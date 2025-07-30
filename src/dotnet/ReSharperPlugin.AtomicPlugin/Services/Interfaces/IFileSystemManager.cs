using System.Threading.Tasks;
using JetBrains.ProjectModel;
using JetBrains.Util;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface IFileSystemManager
    {
        Task CreateOrUpdateFile(string atomicFilePath, AtomicEntityApiConfig config, string generatedCode);
        FileSystemPath GetOutputPath(FileSystemPath atomicFilePath, AtomicEntityApiConfig config, IProject project);
    }
}