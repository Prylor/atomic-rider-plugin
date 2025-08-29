using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Application.Threading;
using JetBrains.Application.Threading.Tasks;
using JetBrains.DataFlow;
using JetBrains.DocumentManagers.impl;
using JetBrains.DocumentModel;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.Rd.Tasks;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Util;
using JetBrains.Util.dataStructures.TypedIntrinsics;
using ReSharperPlugin.AtomicPlugin.Model;
using ReSharperPlugin.AtomicPlugin.Services;
using ProjectManager = NuGet.ProjectManager;

namespace ReSharperPlugin.AtomicPlugin.Rider
{
    [SolutionComponent(Instantiation.ContainerAsyncAnyThreadSafe)]
    public class AtomicGenerationComponent
    {
        private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger<AtomicGenerationComponent>();
        
        private readonly ISolution _solution;
        private readonly ICodeGenerator _codeGenerator;
        private readonly ITypeResolver _typeResolver;
        private readonly ITypeValidator _typeValidator;
        private readonly INamespaceResolver _namespaceResolver;
        private readonly IProjectManager _projectManager;
        private readonly IFileSystemManager _fileSystemManager;
        private readonly IConfigurationMapper _configMapper;
        private readonly IUsageFinder _usageFinder;
        
        public AtomicGenerationComponent(ISolution solution)
        {
            _solution = solution;
            
            
            var symbolScopeManager = new SymbolScopeManager(solution);
            var hashGenerator = new HashCodeGenerator();
            var transactionManager = new TransactionManager(solution);
            var extensionMethodDetector = new ExtensionMethodDetector();
            
            _codeGenerator = new CodeGenerator(hashGenerator);
            _typeResolver = new TypeResolver(symbolScopeManager);
            _typeValidator = new TypeValidator(symbolScopeManager);
            _namespaceResolver = new NamespaceResolver(symbolScopeManager); 
            _projectManager = new Services.ProjectManager(solution);
            _fileSystemManager = new FileSystemManagerAsync(_projectManager, solution);
            _configMapper = new ConfigurationMapper();
            _usageFinder = new UsageFinder(solution, extensionMethodDetector);
            
            
            var model = solution.GetProtocolSolution().GetAtomicGenerationModel();
            SetupHandlers(model);
            
            Logger.Info("AtomicGenerationComponentRefactored initialized");
        }
        
        private void SetupHandlers(AtomicGenerationModel model)
        {
            
            model.GenerateApi.SetAsync(async (lt, fileData) =>
            {
                Logger.Info($"Backend received generation request for: {fileData.FilePath}");
                return await GenerateApiAsync(fileData);
            });
            
            
            model.GetTypeCompletions.SetAsync(async (lt, request) =>
            {
                Logger.Info($"Backend received type completion request for prefix: {request.Prefix}");
                return await _typeResolver.GetCompletionsAsync(request);
            });
            
            
            model.ValidateType.SetAsync(async (lt, request) =>
            {
                Logger.Info($"Backend received type validation request for: {request.TypeName}");
                return await _typeValidator.ValidateAsync(request);
            });
            
            
            model.GetAvailableProjects.SetAsync(async (lt, _) =>
            {
                Logger.Info("Backend received available projects request");
                return await _projectManager.GetAvailableProjectsAsync();
            });
            
            
            model.GetNamespaceCompletions.SetAsync(async (lt, request) =>
            {
                Logger.Info($"Backend received namespace completion request for prefix: {request.Prefix}");
                return await _namespaceResolver.GetCompletionsAsync(request);
            });
            
            
            model.ValidateNamespace.SetAsync(async (lt, request) =>
            {
                Logger.Info($"Backend received namespace validation request for: {request.Namespace}");
                return await _namespaceResolver.ValidateAsync(request);
            });
            
            
            model.FindMethodUsages.SetAsync(async (lt, request) =>
            {
                Logger.Info($"Backend received find method usages request for value: {request.ValueName}");
                return await _usageFinder.FindMethodUsagesAsync(request);
            });
            
            
            model.FindTagUsages.SetAsync(async (lt, request) =>
            {
                Logger.Info($"Backend received find tag usages request for tag: {request.TagName}");
                return await _usageFinder.FindTagUsagesAsync(request);
            });
            
            
            model.RenameValue.SetAsync(async (lt, request) =>
            {
                Logger.Info($"Backend received rename value request: {request.OldName} -> {request.NewName}");
                return await HandleRenameValueAsync(request);
            });
            
            
            model.RenameTag.SetAsync(async (lt, request) =>
            {
                Logger.Info($"Backend received rename tag request: {request.OldName} -> {request.NewName}");
                return await HandleRenameTagAsync(request);
            });
            
            
            model.AddAtomicFileToProject.SetAsync(async (lt, atomicFilePath) =>
            {
                Logger.Info($"Backend received add atomic file to project request: {atomicFilePath}");
                return await AddAtomicFileToProjectAsync(atomicFilePath);
            });
        }
        
        private async Task<string> GenerateApiAsync(AtomicFileData fileData)
        {
            Logger.Info($"[GenerateApiAsync] Started for file: {fileData.FilePath}");
            Logger.Info($"[GenerateApiAsync] Raw header properties count: {fileData.HeaderProperties.Length}");
            
            foreach (var prop in fileData.HeaderProperties)
            {
                Logger.Info($"[GenerateApiAsync] Header property: key='{prop.Key}', value='{prop.Value}'");
            }
            
            try
            {
                
                var config = _configMapper.MapToConfig(fileData);
                Logger.Info($"[AtomicGeneration] Config - Directory: '{config.Directory}', ClassName: '{config.ClassName}'");
                
                
                var generatedCode = _codeGenerator.GenerateCode(config);
                
                
                await _fileSystemManager.CreateOrUpdateFile(fileData.FilePath, config, generatedCode);
                
                
                _solution.Locks.Tasks.StartNew(Lifetime.Eternal, Scheduling.MainGuard, () =>
                {
                    var model = _solution.GetProtocolSolution().GetAtomicGenerationModel();
                    model.GenerationStatus($"Generated API for {config.Values.Count} values");
                });
                
                return generatedCode;
            }
            catch (Exception ex)
            {
                Logger.Error(ex, "Failed to generate API");
                throw;
            }
        }
        
        private async Task<RenameResponse> HandleRenameValueAsync(RenameValueRequest request)
        {
            try
            {
                
                var atomicFile = System.IO.File.ReadAllText(request.AtomicFilePath);
                var lines = atomicFile.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
                
                
                var oldMethodNames = GetGeneratedMethodNames(request.OldName);
                var newMethodNames = GetGeneratedMethodNames(request.NewName);
                
                
                var findRequest = new FindMethodUsagesRequest(
                    valueName: request.OldName,
                    methodNames: oldMethodNames.ToArray(),
                    projectPath: request.ProjectPath,
                    generatedFilePath: CalculateGeneratedFilePath(request.AtomicFilePath)
                );
                
                var usagesResponse = await _usageFinder.FindMethodUsagesAsync(findRequest);
                var usages = usagesResponse.Usages.ToList();
                
                
                var updatedUsages = new List<MethodUsageLocation>();
                
                foreach (var usage in usages)
                {
                    var updated = await UpdateMethodUsage(usage, request.OldName, request.NewName);
                    if (updated != null)
                    {
                        updatedUsages.Add(updated);
                    }
                }
                
                
                
                Logger.Info($"[HandleRenameValueAsync] About to regenerate API file for: {request.AtomicFilePath}");
                var regeneratedPath = await RegenerateApiFile(request.AtomicFilePath);
                Logger.Info($"[HandleRenameValueAsync] Regenerated file at: {regeneratedPath}");
                
                return new RenameResponse(
                    success: true,
                    regeneratedFilePath: regeneratedPath,
                    updatedUsages: updatedUsages.ToArray(),
                    errorMessage: null
                );
            }
            catch (Exception ex)
            {
                Logger.Error($"Failed to rename value: {ex.Message}", ex);
                return new RenameResponse(
                    success: false,
                    regeneratedFilePath: null,
                    updatedUsages: new MethodUsageLocation[0],
                    errorMessage: ex.Message
                );
            }
        }
        
        private async Task<RenameResponse> HandleRenameTagAsync(RenameTagRequest request)
        {
            try
            {
                
                var oldMethodNames = GetGeneratedTagMethodNames(request.OldName);
                var newMethodNames = GetGeneratedTagMethodNames(request.NewName);
                
                
                var findRequest = new FindTagUsagesRequest(
                    tagName: request.OldName,
                    methodNames: oldMethodNames.ToArray(),
                    projectPath: request.ProjectPath,
                    generatedFilePath: CalculateGeneratedFilePath(request.AtomicFilePath)
                );
                
                var usagesResponse = await _usageFinder.FindTagUsagesAsync(findRequest);
                var usages = usagesResponse.Usages.ToList();
                
                
                var updatedUsages = new List<MethodUsageLocation>();
                
                foreach (var usage in usages)
                {
                    var updated = await UpdateTagMethodUsage(usage, request.OldName, request.NewName);
                    if (updated != null)
                    {
                        updatedUsages.Add(updated);
                    }
                }
                
                
                var regeneratedPath = await RegenerateApiFile(request.AtomicFilePath);
                
                return new RenameResponse(
                    success: true,
                    regeneratedFilePath: regeneratedPath,
                    updatedUsages: updatedUsages.ToArray(),
                    errorMessage: null
                );
            }
            catch (Exception ex)
            {
                Logger.Error($"Failed to rename tag: {ex.Message}", ex);
                return new RenameResponse(
                    success: false,
                    regeneratedFilePath: null,
                    updatedUsages: new MethodUsageLocation[0],
                    errorMessage: ex.Message
                );
            }
        }
        
        private List<string> GetGeneratedMethodNames(string valueName)
        {
            return new List<string>
            {
                $"Get{valueName}",
                $"Set{valueName}",
                $"Add{valueName}",
                $"Has{valueName}",
                $"Del{valueName}",
                $"TryGet{valueName}",
                $"Ref{valueName}"
            };
        }
        
        private List<string> GetGeneratedTagMethodNames(string tagName)
        {
            return new List<string>
            {
                $"Has{tagName}Tag",
                $"Add{tagName}Tag",
                $"Del{tagName}Tag"
            };
        }
        
        private string CalculateGeneratedFilePath(string atomicFilePath)
        {
            try
            {
                
                var fileContent = System.IO.File.ReadAllText(atomicFilePath);
                var fileData = ParseAtomicFileContent(atomicFilePath, fileContent);
                var config = _configMapper.MapToConfig(fileData);
                
                
                if (!string.IsNullOrEmpty(config.Directory))
                {
                    
                    var cleanDir = config.Directory.Replace('/', System.IO.Path.DirectorySeparatorChar)
                        .Replace('\\', System.IO.Path.DirectorySeparatorChar)
                        .Trim(System.IO.Path.DirectorySeparatorChar);
                    
                    
                    var atomicFileInfo = new System.IO.FileInfo(atomicFilePath);
                    var currentDir = atomicFileInfo.Directory;
                    
                    
                    if (currentDir.FullName.EndsWith(cleanDir.Replace('/', System.IO.Path.DirectorySeparatorChar)))
                    {
                        
                        return System.IO.Path.Combine(currentDir.FullName, config.ClassName + ".cs");
                    }
                    
                    
                    while (currentDir != null)
                    {
                        if (currentDir.Name == "Assets" || 
                            System.IO.Directory.Exists(System.IO.Path.Combine(currentDir.FullName, "Assets")))
                        {
                            var projectRoot = currentDir.Name == "Assets" ? currentDir.Parent : currentDir;
                            var outputPath = System.IO.Path.Combine(projectRoot.FullName, cleanDir, config.ClassName + ".cs");
                            return outputPath;
                        }
                        currentDir = currentDir.Parent;
                    }
                }
                
                
                var fallbackDir = System.IO.Path.GetDirectoryName(atomicFilePath);
                return System.IO.Path.Combine(fallbackDir, config.ClassName + ".cs");
            }
            catch (Exception ex)
            {
                Logger.Error($"Failed to calculate generated file path: {ex.Message}", ex);
                
                
                var atomicFileInfo = new System.IO.FileInfo(atomicFilePath);
                var atomicFileName = System.IO.Path.GetFileNameWithoutExtension(atomicFileInfo.Name);
                return System.IO.Path.Combine(atomicFileInfo.DirectoryName, $"{atomicFileName}.cs");
            }
        }
        
        private async Task<MethodUsageLocation> UpdateMethodUsage(MethodUsageLocation usage, string oldName, string newName)
        {
            return await Task.Run(() =>
            {
                _solution.Locks.Tasks.StartNew(Lifetime.Eternal, Scheduling.MainGuard, () =>
                {
                    try
                    {
                        var psiServices = _solution.GetPsiServices();
                        var filePath = FileSystemPath.Parse(usage.FilePath);
                        IPsiSourceFile sourceFile = null;
                        
                        
                        foreach (var project in _solution.GetAllProjects())
                        {
                            var projectFile = project.GetAllProjectFiles()
                                .FirstOrDefault(pf => pf.Location.Equals(filePath));
                            
                            if (projectFile != null)
                            {
                                sourceFile = projectFile.ToSourceFiles().FirstOrDefault();
                                break;
                            }
                        }
                        
                        if (sourceFile != null)
                        {
                            var document = sourceFile.Document;
                            if (document != null)
                            {
                                
                                var oldMethodName = usage.MethodName;
                                var newMethodName = oldMethodName.Replace(oldName, newName);
                                
                                
                                _solution.Locks.ExecuteWithWriteLock(() =>
                                {
                                    
                                    var text = document.GetText();
                                    
                                    
                                    var lineIndex = (int)usage.Line - 1;
                                    var lineNum = (Int32<DocLine>)lineIndex;
                                    var lineStartOffset = document.GetLineStartOffset(lineNum);
                                    
                                    
                                    var lineEndOffset = text.Length;
                                    
                                    
                                    var newlineIndex = text.IndexOf('\n', lineStartOffset);
                                    if (newlineIndex >= 0)
                                    {
                                        lineEndOffset = newlineIndex;
                                        
                                        if (newlineIndex > 0 && text[newlineIndex - 1] == '\r')
                                        {
                                            lineEndOffset = newlineIndex - 1;
                                        }
                                    }
                                    var lineText = text.Substring(lineStartOffset, lineEndOffset - lineStartOffset);
                                    
                                    
                                    var methodIndex = lineText.IndexOf(oldMethodName);
                                    if (methodIndex >= 0)
                                    {
                                        
                                        var absoluteOffset = lineStartOffset + methodIndex;
                                        var range = new TextRange(absoluteOffset, absoluteOffset + oldMethodName.Length);
                                        
                                        
                                        var textToReplace = text.Substring(absoluteOffset, oldMethodName.Length);
                                        if (textToReplace == oldMethodName)
                                        {
                                            document.ReplaceText(range, newMethodName);
                                        }
                                        else
                                        {
                                            Logger.Warn($"Text mismatch at offset {absoluteOffset}: expected '{oldMethodName}', found '{textToReplace}'");
                                        }
                                    }
                                    else
                                    {
                                        Logger.Warn($"Method name '{oldMethodName}' not found in line: {lineText}");
                                    }
                                    
                                    psiServices.Files.CommitAllDocuments();
                                });
                                
                                
                                
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        Logger.Error($"Failed to update usage at {usage.FilePath}:{usage.Line}", ex);
                    }
                });
                
                
                return new MethodUsageLocation(
                    filePath: usage.FilePath,
                    line: usage.Line,
                    column: usage.Column,
                    methodName: usage.MethodName.Replace(oldName, newName),
                    usageText: usage.UsageText.Replace(usage.MethodName, usage.MethodName.Replace(oldName, newName))
                );
            });
        }
        
        private async Task<MethodUsageLocation> UpdateTagMethodUsage(MethodUsageLocation usage, string oldName, string newName)
        {
            
            return await UpdateMethodUsage(usage, oldName, newName);
        }
        
        private async Task<string> RegenerateApiFile(string atomicFilePath)
        {
            try
            {
                Logger.Info($"[RegenerateApiFile] Reading atomic file: {atomicFilePath}");
                
                
                await Task.Run(() =>
                {
                    _solution.Locks.Tasks.StartNew(Lifetime.Eternal, Scheduling.MainGuard, () =>
                    {
                        var filePath = FileSystemPath.Parse(atomicFilePath);
                        var fileInfo = new System.IO.FileInfo(atomicFilePath);
                        fileInfo.Refresh();
                        
                        
                        foreach (var project in _solution.GetAllProjects())
                        {
                            var projectFile = project.GetAllProjectFiles()
                                .FirstOrDefault(pf => pf.Location.Equals(filePath));
                            
                            if (projectFile != null)
                            {
                                projectFile.ToSourceFiles().FirstOrDefault()?.Document?.GetText();
                                break;
                            }
                        }
                    });
                });
                
                
                await Task.Delay(100);
                
                
                var fileContent = System.IO.File.ReadAllText(atomicFilePath);
                Logger.Info($"[RegenerateApiFile] Full file content:");
                var contentLines = fileContent.Split(new[] { '\r', '\n' }, StringSplitOptions.None);
                for (int i = 0; i < Math.Min(contentLines.Length, 25); i++)
                {
                    Logger.Info($"[RegenerateApiFile] Line {i + 1}: {contentLines[i]}");
                }
                
                
                
                var fileData = ParseAtomicFileContent(atomicFilePath, fileContent);
                Logger.Info($"[RegenerateApiFile] Parsed data - Values: {string.Join(", ", fileData.Values.Select(v => $"{v.Name}:{v.Type}"))}");
                
                
                var generatedCode = await GenerateApiAsync(fileData);
                Logger.Info($"[RegenerateApiFile] Generated code length: {generatedCode.Length}");
                
                
                var config = _configMapper.MapToConfig(fileData);
                var atomicPath = FileSystemPath.TryParse(atomicFilePath);
                var outputPath = _fileSystemManager.GetOutputPath(atomicPath, config, null);
                
                Logger.Info($"[RegenerateApiFile] Output path: {outputPath.FullPath}");
                return outputPath.FullPath;
            }
            catch (Exception ex)
            {
                Logger.Error($"Failed to regenerate API file: {ex.Message}", ex);
                throw;
            }
        }
        
        private AtomicFileData ParseAtomicFileContent(string filePath, string content)
        {
            Logger.Info($"[ParseAtomicFileContent] Starting parse of file: {filePath}");
            Logger.Info($"[ParseAtomicFileContent] Content length: {content.Length}");
            
            
            var headerProperties = new List<HeaderProperty>();
            var imports = new List<string>();
            var tags = new List<string>();
            var values = new List<AtomicValueData>();
            
            var lines = content.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
            var currentSection = "";
            
            Logger.Info($"[ParseAtomicFileContent] Total lines: {lines.Length}");
            
            foreach (var line in lines)
            {
                var trimmedLine = line.Trim();
                
                if (trimmedLine.EndsWith(":"))
                {
                    currentSection = trimmedLine.TrimEnd(':');
                    Logger.Info($"[ParseAtomicFileContent] Entering section: {currentSection}");
                    continue;
                }
                
                if (trimmedLine.StartsWith("-"))
                {
                    var item = trimmedLine.Substring(1).Trim();
                    
                    switch (currentSection)
                    {
                        case "imports":
                            imports.Add(item);
                            Logger.Info($"[ParseAtomicFileContent] Added import: {item}");
                            break;
                        case "tags":
                            tags.Add(item);
                            Logger.Info($"[ParseAtomicFileContent] Added tag: {item}");
                            break;
                        case "values":
                            var colonIndex = item.IndexOf(':');
                            if (colonIndex > 0)
                            {
                                var name = item.Substring(0, colonIndex).Trim();
                                var type = item.Substring(colonIndex + 1).Trim();
                                values.Add(new AtomicValueData(name, type));
                                Logger.Info($"[ParseAtomicFileContent] Added value: {name} of type {type}");
                            }
                            break;
                    }
                }
                else if (trimmedLine.Contains(":") && currentSection == "")
                {
                    
                    var colonIndex = trimmedLine.IndexOf(':');
                    var key = trimmedLine.Substring(0, colonIndex).Trim();
                    var value = trimmedLine.Substring(colonIndex + 1).Trim().Trim('"');
                    headerProperties.Add(new HeaderProperty(key, value));
                    Logger.Info($"[ParseAtomicFileContent] Added header property: {key}={value}");
                }
            }
            
            return new AtomicFileData(
                headerProperties: headerProperties.ToArray(),
                imports: imports.ToArray(),
                tags: tags.ToArray(),
                values: values.ToArray(),
                filePath: filePath
            );
        }
        
        private async Task<bool> AddAtomicFileToProjectAsync(string atomicFilePath)
        {
            try
            {
                var filePath = FileSystemPath.TryParse(atomicFilePath);
                if (filePath.IsEmpty)
                {
                    Logger.Error($"Invalid file path: {atomicFilePath}");
                    return false;
                }
                
                
                return await _projectManager.AddFileToProjectAsync(filePath);
            }
            catch (Exception ex)
            {
                Logger.Error($"Error adding atomic file to project: {ex.Message}", ex);
                return false;
            }
        }
    }
}