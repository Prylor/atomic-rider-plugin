using System;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using JetBrains.Application.Threading;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Psi.Modules;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Util;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class DirectFileWriter(ISolution solution)
    {
        private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger<DirectFileWriter>();

        public async Task<bool> WriteFileDirectlyAsync(IProject project, FileSystemPath outputPath, string content)
        {
            try
            {
                var lfContent = content.Replace("\r\n", "\n");
                
                var directory = outputPath.Directory;
                if (!directory.ExistsDirectory)
                {
                    Logger.Info($"Creating directory: {directory}");
                    directory.CreateDirectory();
                }
                
                Logger.Info($"Writing file directly to: {outputPath}");
                await Task.Run(() => File.WriteAllText(outputPath.FullPath, lfContent)).ConfigureAwait(false);
                
                ScheduleProjectModelUpdate(project, outputPath);
                
                return true;
            }
            catch (Exception ex)
            {
                Logger.Error($"Failed to write file directly: {ex.Message}", ex);
                return false;
            }
        }
        
        private void ScheduleProjectModelUpdate(IProject project, FileSystemPath filePath)
        {
            Task.Run(async () =>
            {
                try
                {
                    await Task.Delay(500).ConfigureAwait(false);
                    
                    solution.Locks.ExecuteOrQueue(
                        Lifetime.Eternal,
                        "RefreshProject",
                        () =>
                        {
                            try
                            {
                                using (WriteLockCookie.Create())
                                {
                                    project.GetPsiModules();
                                    Logger.Info($"Triggered project refresh for: {filePath}");
                                }
                            }
                            catch (Exception ex)
                            {
                                Logger.Warn($"Could not refresh project model: {ex.Message}");
                            }
                        });
                }
                catch (Exception ex)
                {
                    Logger.Warn($"Project model update failed: {ex.Message}");
                }
            });
        }
        
        private IProjectFolder GetProjectFolder(IProject project, FileSystemPath directory)
        {
            if (project == null || directory.IsEmpty)
                return null;
            
            var folders = project.GetSubFolders();
            foreach (var folder in folders)
            {
                if (folder.Location.Equals(directory))
                    return folder;
            }
            
            return project;
        }
    }
}