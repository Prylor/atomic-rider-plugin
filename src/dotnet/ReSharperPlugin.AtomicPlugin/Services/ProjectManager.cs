using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using JetBrains.Application.Threading;
using JetBrains.Application.Threading.Tasks;
using JetBrains.DocumentManagers.impl;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Util;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Util;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class ProjectManager : IProjectManager
    {
        private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger<ProjectManager>();
        private readonly ISolution _solution;

        public ProjectManager(ISolution solution)
        {
            _solution = solution;
        }

        public ISolution Solution => _solution;

        public async Task<string[]> GetAvailableProjectsAsync()
        {
            return await Task.Run(() =>
            {
                using (ReadLockCookie.Create())
                {
                    var projects = GetCSharpProjects()
                        .Select(p => p.Name)
                        .OrderBy(name => name)
                        .ToArray();

                    Logger.Info($"Found {projects.Length} available C# projects");
                    return projects;
                }
            });
        }

        public IProject FindProjectForDirectory(FileSystemPath directory)
        {
            Logger.Info($"Finding project for directory: {directory}");

            var allProjects = GetCSharpProjects().ToList();

            Logger.Info($"Found {allProjects.Count} C# projects in solution:");
            foreach (var proj in allProjects)
            {
                Logger.Info($"  - {proj.Name} at {proj.Location}");
            }

            
            IProject bestMatch = null;
            int bestMatchDepth = -1;

            foreach (var project in allProjects)
            {
                var projectPath = project.Location.ToNativeFileSystemPath();

                
                if (projectPath.IsPrefixOf(directory))
                {
                    
                    var relativePath = directory.MakeRelativeTo(projectPath);
                    var depth = relativePath.Components.Count();

                    
                    
                    if (depth >= 0 && (bestMatch == null || projectPath.FullPath.Length >
                            bestMatch.Location.ToNativeFileSystemPath().FullPath.Length))
                    {
                        bestMatch = project;
                        bestMatchDepth = depth;
                        Logger.Info($"Found potential project: {project.Name} at depth {depth}");
                    }
                }
            }

            if (bestMatch != null)
            {
                Logger.Info($"Best matching project for directory: {bestMatch.Name}");
            }
            else
            {
                Logger.Info("No project found that contains the target directory");
            }

            return bestMatch;
        }

        public IProject FindSuitableProject(FileSystemPath atomicPath, AtomicEntityApiConfig config)
        {
            var allProjects = GetCSharpProjects().ToList();

            if (!allProjects.Any())
                return null;

            if (allProjects.Count == 1)
                return allProjects.First();

            if (!string.IsNullOrEmpty(config.Namespace))
            {
                var namespaceRoot = config.Namespace.Split('.').First();
                var projectWithMatchingNamespace = allProjects
                    .FirstOrDefault(p => p.Name.IndexOf(namespaceRoot, StringComparison.OrdinalIgnoreCase) >= 0);

                if (projectWithMatchingNamespace != null)
                    return projectWithMatchingNamespace;
            }

            IProject closestProject = null;
            int shortestDistance = int.MaxValue;

            foreach (var project in allProjects)
            {
                var projectPath = project.Location.ToNativeFileSystemPath();
                var distance = GetPathDistance(projectPath, atomicPath);

                if (distance < shortestDistance)
                {
                    shortestDistance = distance;
                    closestProject = project;
                }
            }

            return closestProject ?? allProjects.First();
        }

        public bool IsCSharpProject(IProject project)
        {
            var projectFile = project.ProjectFile;
            if (projectFile != null)
            {
                var extension = projectFile.Location.ExtensionNoDot;
                if (extension.Equals("csproj", StringComparison.OrdinalIgnoreCase))
                    return true;
            }

            
            return project.GetAllProjectFiles().Any(f =>
                f.LanguageType.Is<CSharpProjectFileType>());
        }

        private int GetPathDistance(FileSystemPath path1, FileSystemPath path2)
        {
            var components1 = path1.Components.ToList();
            var components2 = path2.Components.ToList();

            int commonLength = 0;
            for (int i = 0; i < Math.Min(components1.Count, components2.Count); i++)
            {
                if (components1[i].Equals(components2[i]))
                    commonLength++;
                else
                    break;
            }

            return (components1.Count - commonLength) + (components2.Count - commonLength);
        }

        public IEnumerable<IProject> GetCSharpProjects()
        {
            return _solution.GetAllProjects()
                .Where(p => !p.IsMiscFilesProject() && !p.IsSolutionFolder() && IsCSharpProject(p));
        }

        public IProject FindProjectByName(string projectName)
        {
            if (string.IsNullOrEmpty(projectName))
                return null;

            return GetCSharpProjects()
                .FirstOrDefault(p => p.Name.Equals(projectName, StringComparison.OrdinalIgnoreCase));
        }

        public IProject FindClosestProjectToDirectory(FileSystemPath targetDirectory)
        {
            Logger.Info($"Finding closest project to directory: {targetDirectory}");

            IProject closestProject = null;
            int longestMatchLength = 0;

            foreach (var project in GetCSharpProjects())
            {
                var projectPath = project.Location.ToNativeFileSystemPath();

                if (projectPath.IsPrefixOf(targetDirectory))
                {
                    
                    if (projectPath.FullPath.Length > longestMatchLength)
                    {
                        closestProject = project;
                        longestMatchLength = projectPath.FullPath.Length;
                    }
                }
                else
                {
                    
                    var commonParent = GetCommonParentPath(projectPath, targetDirectory);
                    if (commonParent != null && commonParent.FullPath.Length > longestMatchLength)
                    {
                        closestProject = project;
                        longestMatchLength = commonParent.FullPath.Length;
                    }
                }
            }

            if (closestProject != null)
            {
                Logger.Info($"Found closest project: {closestProject.Name} (match length: {longestMatchLength})");
            }

            return closestProject;
        }

        public bool IsUnityProject()
        {
            
            var hasAssemblyCSharp = _solution.GetAllProjects()
                .Any(p => p.Name == "Assembly-CSharp" || p.Name == "Assembly-CSharp-Editor");

            if (hasAssemblyCSharp)
            {
                return true;
            }

            
            var solutionPath = _solution.SolutionFilePath.Directory;
            var assetsPath = solutionPath.Combine("Assets");

            return assetsPath.ExistsDirectory;
        }

        public IProject FindUnityProjectForDirectory(FileSystemPath directory)
        {
            Logger.Info($"Looking for Unity project for directory: {directory}");

            
            var asmdefFiles = new List<(FileSystemPath path, IProject project)>();

            foreach (var project in GetCSharpProjects())
            {
                
                
                var projectFiles = project.GetAllProjectFiles();
                foreach (var file in projectFiles)
                {
                    if (file.Name.EndsWith(".asmdef", StringComparison.OrdinalIgnoreCase))
                    {
                        asmdefFiles.Add((file.Location.ToNativeFileSystemPath(), project));
                        Logger.Info($"Found .asmdef: {file.Location} in project {project.Name}");
                    }
                }
            }

            
            IProject closestProject = null;
            var longestMatchLength = 0;

            foreach (var (asmdefPath, project) in asmdefFiles)
            {
                var asmdefDir = asmdefPath.Directory;

                
                if (asmdefDir.IsPrefixOf(directory))
                {
                    var pathLength = asmdefDir.FullPath.Length;
                    if (pathLength > longestMatchLength)
                    {
                        closestProject = project;
                        longestMatchLength = pathLength;
                    }
                }
            }

            if (closestProject != null)
            {
                Logger.Info($"Found Unity project based on .asmdef: {closestProject.Name}");
            }

            return closestProject;
        }

        public IProject GetDefaultUnityProject()
        {
            return FindProjectByName("Assembly-CSharp");
        }

        private FileSystemPath GetCommonParentPath(FileSystemPath path1, FileSystemPath path2)
        {
            if (path1.IsEmpty || path2.IsEmpty)
                return null;

            var components1 = path1.Components.ToList();
            var components2 = path2.Components.ToList();

            var commonComponents = new List<string>();
            var minCount = Math.Min(components1.Count, components2.Count);

            for (int i = 0; i < minCount; i++)
            {
                if (string.Equals(components1[i].ToString(), components2[i].ToString(),
                        StringComparison.OrdinalIgnoreCase))
                {
                    commonComponents.Add(components1[i].ToString());
                }
                else
                {
                    break;
                }
            }

            if (commonComponents.Count == 0)
                return null;

            
            var result = FileSystemPath.Empty;
            foreach (var component in commonComponents)
            {
                result = result.Combine(component);
            }

            return result;
        }

        public async Task<bool> AddFileToProjectAsync(FileSystemPath filePath)
        {
            try
            {
                if (filePath.IsEmpty || !filePath.ExistsFile)
                {
                    Logger.Error($"Invalid or non-existent file path: {filePath}");
                    return false;
                }

                
                IProject targetProject = null;
                IProjectFolder parentFolder = null;

                await Task.Run(() =>
                {
                    using (ReadLockCookie.Create())
                    {
                        if (!IsUnityProject())
                        {
                            targetProject = FindProjectForDirectory(filePath.Parent);
                        }
                        else
                        {
                            targetProject = FindUnityProjectForDirectory(filePath.Parent);

                            if (targetProject == null)
                            {
                                
                                targetProject = GetDefaultUnityProject();
                            }
                        }
                        

                        if (targetProject == null)
                        {
                            
                            targetProject = FindClosestProjectToDirectory(filePath.Parent);
                        }

                        if (targetProject == null)
                        {
                            Logger.Error($"Could not find a project to add the file to");
                            return;
                        }

                        Logger.Info($"Adding file to project: {targetProject.Name}");

                        
                        var existingFile = targetProject.GetAllProjectFiles()
                            .FirstOrDefault(f => f.Location.Equals(filePath));

                        if (existingFile != null)
                        {
                            Logger.Info($"File already exists in project {targetProject.Name}");
                            targetProject = null; 
                            return;
                        }
                    }
                });

                if (targetProject == null)
                {
                    return true; 
                }

                
                var shellLocks = _solution.GetComponent<IShellLocks>();
                var result = false;

                shellLocks.Tasks.StartNew(_solution.GetSolutionLifetimes().UntilSolutionCloseLifetime, Scheduling.MainGuard, () =>
                {
                    using (WriteLockCookie.Create())
                    {
                        
                        parentFolder = GetOrCreateProjectFolder(targetProject, filePath.Parent);
                        if (parentFolder == null)
                        {
                            Logger.Error($"Could not get or create folder for path: {filePath.Parent}");
                            return;
                        }

                        
                        var fileContent = "";
                        if (filePath.ExistsFile)
                        {
                            try
                            {
                                fileContent = System.IO.File.ReadAllText(filePath.FullPath);
                            }
                            catch (Exception ex)
                            {
                                Logger.Warn($"Could not read file content: {ex.Message}");
                            }
                        }

                        
                        var projectFile = AddNewItemHelper.AddFile(
                            parentFolder,
                            filePath.Name,
                            fileContent
                        );

                        if (projectFile != null)
                        {
                            Logger.Info($"Successfully added file to project {targetProject.Name}");
                            result = true;
                        }
                        else
                        {
                            Logger.Error($"Failed to add file to project");
                            result = false;
                        }
                    }
                });

                return result;
            }
            catch (Exception ex)
            {
                Logger.Error($"Error adding file to project: {ex.Message}", ex);
                return false;
            }
        }

        private IProjectFolder GetOrCreateProjectFolder(IProject project, FileSystemPath directory)
        {
            var projectLocation = project.Location.ToNativeFileSystemPath();

            if (!projectLocation.IsPrefixOf(directory))
            {
                
                return project;
            }

            var relativePath = directory.MakeRelativeTo(projectLocation);
            var folders = relativePath.Components.ToArray();

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
                catch (Exception ex)
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
    }
}