using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using JetBrains.Application.Progress;
using JetBrains.Application.Threading;
using JetBrains.Application.Threading.Tasks;
using JetBrains.DocumentManagers;
using JetBrains.DocumentManagers.impl;
using JetBrains.DocumentManagers.Transactions;

using JetBrains.DocumentModel;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Util;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Util;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class FileSystemManager : IFileSystemManager
    {
        private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger<FileSystemManager>();
        private readonly ITransactionManager _transactionManager;
        private readonly IProjectManager _projectManager;

        public FileSystemManager(ITransactionManager transactionManager, IProjectManager projectManager)
        {
            _transactionManager = transactionManager;
            _projectManager = projectManager;
        }

        public async Task CreateOrUpdateFile(string atomicFilePath, AtomicEntityApiConfig config, string generatedCode)
        {
            var atomicPath = FileSystemPath.TryParse(atomicFilePath);
            if (atomicPath.IsEmpty) 
                throw new ArgumentException($"Invalid file path: {atomicFilePath}");

            
            var solution = _projectManager.Solution;
            
            
            var tcs = new TaskCompletionSource<bool>();
            
            solution.Locks.Tasks.StartNew(Lifetime.Eternal, Scheduling.MainGuard, () =>
            {
                try
                {
                    using (WriteLockCookie.Create())
                    {
                        IProject targetProject = null;
                        
                        
                        var targetDirectory = GetOutputPath(atomicPath, config, null).Directory;
                        Logger.Info($"Target directory for generated file: {targetDirectory}");
                        
                        
                        if (!string.IsNullOrEmpty(config.Solution))
                        {
                            Logger.Info($"Looking for specified project: {config.Solution}");
                            targetProject = _projectManager.FindProjectByName(config.Solution);
                            
                            if (targetProject != null)
                            {
                                Logger.Info($"Found specified project: {targetProject.Name}");
                            }
                            else
                            {
                                Logger.Warn($"Specified project '{config.Solution}' not found, will search for closest project");
                            }
                        }
                        
                        
                        if (targetProject == null)
                        {
                            Logger.Info($"Searching for closest project to directory: {targetDirectory}");
                            
                            
                            if (_projectManager.IsUnityProject())
                            {
                                Logger.Info("Detected Unity project structure");
                                
                                
                                targetProject = _projectManager.FindUnityProjectForDirectory(targetDirectory);
                                
                                if (targetProject == null)
                                {
                                    
                                    targetProject = _projectManager.GetDefaultUnityProject();
                                    
                                    if (targetProject != null)
                                    {
                                        Logger.Info("No .asmdef found, defaulting to Assembly-CSharp");
                                    }
                                }
                            }
                            else
                            {
                                
                                
                                targetProject = _projectManager.FindProjectForDirectory(targetDirectory);
                                
                                if (targetProject != null)
                                {
                                    Logger.Info($"Found project that owns the directory: {targetProject.Name}");
                                }
                                else
                                {
                                    
                                    targetProject = _projectManager.FindClosestProjectToDirectory(targetDirectory);
                                }
                            }
                            
                            
                            if (targetProject == null)
                            {
                                targetProject = _projectManager.FindProjectForDirectory(atomicPath.Parent);
                                if (targetProject != null)
                                {
                                    Logger.Info($"Found project containing atomic file: {targetProject.Name}");
                                }
                            }
                            
                            
                            if (targetProject == null)
                            {
                                targetProject = _projectManager.FindSuitableProject(targetDirectory, config);
                            }
                        }
                        
                        if (targetProject == null)
                        {
                            Logger.Error("Could not find any C# project in solution to add generated file.");
                            throw new InvalidOperationException("No suitable C# project found in solution");
                        }
                        
                        
                        var outputPath = GetOutputPath(atomicPath, config, targetProject);
                        Logger.Info($"[AtomicGeneration] Output path determined: {outputPath}");
                        
                        
                        var existingProjectFile = targetProject.GetAllProjectFiles()
                            .FirstOrDefault(pf => pf.Location.Equals(outputPath));
                        
                        if (existingProjectFile != null)
                        {
                            Logger.Info($"File already exists at: {existingProjectFile.Location}");
                            
                            
                            var document = existingProjectFile.GetDocument();
                            if (document != null)
                            {
                                
                                var normalizedCode = NormalizeLineEndings(generatedCode, document);
                                document.ReplaceText(document.DocumentRange, normalizedCode);
                                Logger.Info($"Updated existing file content");
                            }
                        }
                        else
                        {
                            
                            if (targetProject.IsMiscFilesProject())
                            {
                                Logger.Error("Target is misc files project. Cannot add generated file.");
                                throw new InvalidOperationException("Cannot add generated file to misc files project");
                            }
                            
                            
                            Logger.Info($"Attempting to get/create folder for directory: {outputPath.Directory}");
                            var parentFolder = GetOrCreateProjectFolder(targetProject, outputPath.Directory);
                            if (parentFolder == null)
                            {
                                Logger.Error($"Could not create folder structure for path: {outputPath.Directory}");
                                throw new InvalidOperationException($"Failed to create folder structure: {outputPath.Directory}");
                            }
                            
                            Logger.Info($"Got parent folder: {parentFolder.Name} at location: {parentFolder.Location}");
                            
                            AddNewItemHelper.AddFile(parentFolder, outputPath.Name, generatedCode);
                            
                            Logger.Info($"Successfully added file to project: {outputPath}");
                        }
                        
                        
                        var psiServices = solution.GetPsiServices();
                        psiServices.Files.CommitAllDocuments();
                    }
                    
                    tcs.SetResult(true);
                }
                catch (Exception ex)
                {
                    tcs.SetException(ex);
                }
            });
            
            await tcs.Task;
        }

        public FileSystemPath GetOutputPath(FileSystemPath atomicFilePath, AtomicEntityApiConfig config, IProject project)
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
                
                if (project != null && !project.IsMiscFilesProject())
                {
                    var projectDir = project.Location.ToNativeFileSystemPath();
                    return projectDir.Combine(cleanDir).Combine(outputFileName);
                }
                
                
                
                var currentPath = atomicFilePath.Parent;
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

                return atomicFilePath.Parent.Combine(cleanDir).Combine(outputFileName);
            }
            
            if (project != null && !project.IsMiscFilesProject())
            {
                return project.Location.ToNativeFileSystemPath().Combine(outputFileName);
            }
            
            return atomicFilePath.Parent.Combine(outputFileName);
        }

        private IProjectFolder GetOrCreateProjectFolder(IProject project, FileSystemPath directory)
        {
            var projectLocation = project.Location.ToNativeFileSystemPath();
            
            Logger.Info($"[GetOrCreateProjectFolder] Project location: {projectLocation}");
            Logger.Info($"[GetOrCreateProjectFolder] Target directory: {directory}");
            
            if (!projectLocation.IsPrefixOf(directory))
            {
                Logger.Error($"Directory {directory} is not within project {projectLocation}");
                return null;
            }
            
            
            if (project.IsSolutionFolder())
            {
                Logger.Error("Cannot create folders in solution folder projects");
                return null;
            }
            
            var relativePath = directory.MakeRelativeTo(projectLocation);
            var folders = relativePath.Components.ToArray();
            Logger.Info($"[GetOrCreateProjectFolder] Relative path components: {string.Join("/", folders.Select(f => f.ToString()))}");
            
            IProjectFolder currentFolder = project;
            foreach (var folderName in folders)
            {
                if (string.IsNullOrEmpty(folderName.ToString()) || folderName == ".")
                    continue;
                
                var path = currentFolder.Location.Combine(folderName.ToString());
                
                try
                {
                    currentFolder = currentFolder.GetOrCreateProjectFolder(path);
                    
                    if (currentFolder == null)
                    {
                        Logger.Error($"Failed to create folder: {path}");
                        return null;
                    }
                }
                catch (ProjectModelEditorException ex)
                {
                    Logger.Error($"Cannot create folder {path}: {ex.Message}");
                    
                    var existingFolder = currentFolder.GetSubFolders()
                        .FirstOrDefault(f => f.Name.Equals(folderName.ToString(), StringComparison.OrdinalIgnoreCase));
                    
                    if (existingFolder != null)
                    {
                        currentFolder = existingFolder;
                    }
                    else
                    {
                        
                        return currentFolder;
                    }
                }
            }
            
            return currentFolder;
        }

        private string NormalizeLineEndings(string text, IDocument document)
        {
            var documentText = document.GetText();
            bool usesCRLF = documentText.Contains("\r\n");
            
            if (usesCRLF)
            {
                return text.Replace("\r\n", "\n").Replace("\n", "\r\n");
            }

            return text.Replace("\r\n", "\n");
        }
    }
}