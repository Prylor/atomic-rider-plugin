using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Concurrent;
using JetBrains.Application.Progress;
using JetBrains.Application.Threading;
using JetBrains.DocumentManagers;
using JetBrains.DocumentManagers.impl;
using JetBrains.DocumentModel;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Util;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Util;
using System.Collections.Generic;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    /// <summary>
    /// Async-optimized version of FileSystemManager for Rider 2025.2 compatibility
    /// Prevents RPC timeout issues by using proper async patterns
    /// </summary>
    public class FileSystemManagerAsync : IFileSystemManager
    {
        private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger<FileSystemManagerAsync>();
        private readonly IProjectManager _projectManager;
        private readonly ISolution _solution;
        private readonly DirectFileWriter _directFileWriter;
        private readonly SemaphoreSlim _fileOperationSemaphore = new SemaphoreSlim(1, 1);
        private readonly ConcurrentDictionary<string, DateTime> _lastGenerationTime = new ConcurrentDictionary<string, DateTime>();

        public FileSystemManagerAsync(IProjectManager projectManager, ISolution solution)
        {
            _projectManager = projectManager;
            _solution = solution;
            _directFileWriter = new DirectFileWriter(solution);
        }

        public async Task CreateOrUpdateFile(string atomicFilePath, AtomicEntityApiConfig config, string generatedCode)
        {
            var atomicPath = FileSystemPath.TryParse(atomicFilePath);
            if (atomicPath.IsEmpty) 
                throw new ArgumentException($"Invalid file path: {atomicFilePath}");

            Logger.Info($"[AtomicGeneration] Starting file generation for: {atomicFilePath}");
            
            await _fileOperationSemaphore.WaitAsync().ConfigureAwait(false);
            try
            {
                if (_lastGenerationTime.TryGetValue(atomicFilePath, out var lastTime))
                {
                    var timeSinceLastGen = DateTime.Now - lastTime;
                    if (timeSinceLastGen.TotalMilliseconds < 500)
                    {
                        Logger.Info($"[AtomicGeneration] Skipping - too soon after last generation ({timeSinceLastGen.TotalMilliseconds}ms)");
                        return;
                    }
                }
                _lastGenerationTime[atomicFilePath] = DateTime.Now;

            IProject targetProject = null;
            FileSystemPath outputPath = FileSystemPath.Empty;
            
            await Task.Run(() =>
            {
                using (ReadLockCookie.Create())
                {
                    targetProject = FindTargetProject(atomicPath, config);
                    if (targetProject == null)
                    {
                        throw new InvalidOperationException("No suitable C# project found in solution");
                    }
                    
                    outputPath = GetOutputPath(atomicPath, config, targetProject);
                    Logger.Info($"[AtomicGeneration] Target project: {targetProject.Name}, Output path: {outputPath}");
                }
            }).ConfigureAwait(false);

            IProjectFile existingFile = null;
            await Task.Run(() =>
            {
                using (ReadLockCookie.Create())
                {
                    existingFile = targetProject.GetAllProjectFiles()
                        .FirstOrDefault(pf => pf.Location.Equals(outputPath));
                }
            }).ConfigureAwait(false);

            if (existingFile != null)
            {
                await UpdateExistingFileAsync(existingFile, generatedCode, outputPath).ConfigureAwait(false);
            }
            else
            {
                await CreateNewFileAsync(targetProject, outputPath, generatedCode, atomicPath, config).ConfigureAwait(false);
            }

                await CommitDocumentsAsync().ConfigureAwait(false);
                
                Logger.Info($"[AtomicGeneration] Successfully completed file generation for: {outputPath}");
            }
            finally
            {
                _fileOperationSemaphore.Release();
            }
        }

        private async Task UpdateExistingFileAsync(IProjectFile existingFile, string generatedCode, FileSystemPath outputPath)
        {
            await Task.Run(() =>
            {
                _solution.Locks.ExecuteOrQueueEx(
                    Lifetime.Eternal,
                    "UpdateAtomicFile",
                    () =>
                    {
                        using (WriteLockCookie.Create())
                        {
                            Logger.Info($"Updating existing file: {existingFile.Location}");
                            
                            var document = existingFile.GetDocument();
                            if (document != null)
                            {
                                var lfCode = generatedCode.Replace("\r\n", "\n");
                                document.ReplaceText(document.DocumentRange, lfCode);
                                Logger.Info($"Updated existing file content with LF line endings");
                            }
                        }
                    });
            }).ConfigureAwait(false);
        }

        private async Task CreateNewFileAsync(IProject targetProject, FileSystemPath outputPath, 
            string generatedCode, FileSystemPath atomicPath, AtomicEntityApiConfig config)
        {
            if (targetProject.IsMiscFilesProject())
            {
                throw new InvalidOperationException("Cannot add generated file to misc files project");
            }

            Logger.Info($"Creating new file at: {outputPath}");
            
            var success = await _directFileWriter.WriteFileDirectlyAsync(targetProject, outputPath, generatedCode)
                .ConfigureAwait(false);
            
            if (success)
            {
                Logger.Info($"Successfully created file: {outputPath}");
            }
            else
            {
                throw new InvalidOperationException($"Failed to create file: {outputPath}");
            }
        }

        private async Task CommitDocumentsAsync()
        {
            await Task.Run(() =>
            {
                _solution.Locks.ExecuteOrQueueEx(
                    Lifetime.Eternal,
                    "CommitDocuments",
                    () =>
                    {
                        using (ReadLockCookie.Create())
                        {
                            var psiServices = _solution.GetPsiServices();
                            psiServices.Files.CommitAllDocuments();
                        }
                    });
            }).ConfigureAwait(false);
        }

        private IProject FindTargetProject(FileSystemPath atomicPath, AtomicEntityApiConfig config)
        {
            IProject targetProject = null;
            var targetDirectory = GetOutputPath(atomicPath, config, null).Directory;
            
            Logger.Info($"Target directory for generated file: {targetDirectory}");
            
            if (!string.IsNullOrEmpty(config.Solution))
            {
                Logger.Info($"Looking for specified project: {config.Solution}");
                targetProject = _projectManager.FindProjectByName(config.Solution);
            }
            
            if (targetProject == null)
            {
                if (_projectManager.IsUnityProject())
                {
                    Logger.Info("Detected Unity project structure");
                    targetProject = _projectManager.FindUnityProjectForDirectory(targetDirectory) 
                                 ?? _projectManager.GetDefaultUnityProject();
                }
                else
                {
                    targetProject = _projectManager.FindProjectForDirectory(targetDirectory)
                                 ?? _projectManager.FindClosestProjectToDirectory(targetDirectory);
                }
            }
            
            if (targetProject == null)
            {
                targetProject = _projectManager.FindProjectForDirectory(atomicPath.Parent);
            }
            
            if (targetProject == null)
            {
                targetProject = _projectManager.FindSuitableProject(targetDirectory, config);
            }
            
            return targetProject;
        }

        public FileSystemPath GetOutputPath(FileSystemPath atomicPath, AtomicEntityApiConfig config, IProject targetProject)
        {
            string outputFileName = config.ClassName + ".cs";
            Logger.Info($"[GetOutputPath] Directory from config: '{config.Directory}'");
            Logger.Info($"[GetOutputPath] ClassName: '{config.ClassName}'");
            
            if (!string.IsNullOrEmpty(config.Directory))
            {
                var cleanDir = config.Directory.Replace("./", "").Replace(".\\", "").Trim('/', '\\');
                Logger.Info($"[GetOutputPath] Cleaned directory: '{cleanDir}'");
                
                if (FileSystemPath.TryParse(config.Directory) is { IsAbsolute: true } absolutePath)
                {
                    return absolutePath.Combine(outputFileName);
                }
                
                if (targetProject != null && !targetProject.IsMiscFilesProject())
                {
                    var projectDir = targetProject.Location.ToNativeFileSystemPath();
                    return projectDir.Combine(cleanDir).Combine(outputFileName);
                }
                
                var currentPath = atomicPath.Parent;
                while (currentPath != null && !currentPath.IsEmpty)
                {
                    var assetsPath = currentPath.Combine("Assets");
                    if (assetsPath.ExistsDirectory)
                    {
                        if (cleanDir.StartsWith("Assets", StringComparison.OrdinalIgnoreCase))
                        {
                            return currentPath.Combine(cleanDir).Combine(outputFileName);
                        }
                        else
                        {
                            return assetsPath.Combine(cleanDir).Combine(outputFileName);
                        }
                    }
                    currentPath = currentPath.Parent;
                }

                return atomicPath.Parent.Combine(cleanDir).Combine(outputFileName);
            }
            
            if (targetProject != null && !targetProject.IsMiscFilesProject())
            {
                return targetProject.Location.ToNativeFileSystemPath().Combine(outputFileName);
            }
            
            return atomicPath.Parent.Combine(outputFileName);
        }

        private IProjectFolder GetOrCreateProjectFolder(IProject project, FileSystemPath directory)
        {
            if (project == null || directory.IsEmpty)
                return null;
            
            var projectLocation = project.ProjectFileLocation.Directory;
            
            return project;
        }

        private string NormalizeLineEndings(string text, IDocument document)
        {
            return text.Replace("\r\n", "\n");
        }
    }
}