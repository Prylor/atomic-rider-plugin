using System.Collections.Generic;
using System.Threading.Tasks;
using JetBrains.ProjectModel;
using JetBrains.Util;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface IProjectManager
    {
        ISolution Solution { get; }
        Task<string[]> GetAvailableProjectsAsync();
        IProject FindProjectForDirectory(FileSystemPath directory);
        IProject FindSuitableProject(FileSystemPath atomicPath, AtomicEntityApiConfig config);
        bool IsCSharpProject(IProject project);
        
        
        IEnumerable<IProject> GetCSharpProjects();
        IProject FindProjectByName(string projectName);
        IProject FindClosestProjectToDirectory(FileSystemPath targetDirectory);
        bool IsUnityProject();
        IProject FindUnityProjectForDirectory(FileSystemPath directory);
        IProject GetDefaultUnityProject();
        
        
        Task<bool> AddFileToProjectAsync(FileSystemPath filePath);
    }
}