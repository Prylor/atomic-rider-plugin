using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using JetBrains.Application.Progress;
using JetBrains.Application.Threading;
using JetBrains.Application.Threading.Tasks;
using JetBrains.DocumentModel;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Caches;
using JetBrains.ReSharper.Psi.Resolve;
using JetBrains.ReSharper.Psi.Search;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Util;
using ReSharperPlugin.AtomicPlugin.Model;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class UsageFinder : IUsageFinder
    {
        private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger<UsageFinder>();
        private readonly ISolution _solution;
        private readonly IExtensionMethodDetector _extensionMethodDetector;

        public UsageFinder(ISolution solution, IExtensionMethodDetector extensionMethodDetector)
        {
            _solution = solution;
            _extensionMethodDetector = extensionMethodDetector;
        }

        public async Task<FindMethodUsagesResponse> FindMethodUsagesAsync(FindMethodUsagesRequest request)
        {
            var usages = new List<MethodUsageLocation>();
            
            try
            {
                var tcs = new TaskCompletionSource<List<MethodUsageLocation>>();
                
                _solution.Locks.Tasks.StartNew(Lifetime.Eternal, Scheduling.MainGuard, () =>
                {
                    try
                    {
                        var psiServices = _solution.GetPsiServices();
                        var searchDomain = SearchDomainFactory.Instance.CreateSearchDomain(_solution, false);
                        var symbolCache = psiServices.Symbols;
                        var symbolScope = symbolCache.GetSymbolScope(LibrarySymbolScope.FULL, caseSensitive: true);
                        
                        foreach (var methodName in request.MethodNames)
                        {
                            Logger.Info($"[FindMethodUsages] Searching for method: {methodName}");
                            var sourceMembers = symbolScope.GetSourceMembers(methodName);
                            var methods = new List<IMethod>();

                            var typeMembers = sourceMembers as ITypeMember[] ?? sourceMembers.ToArray();
                            
                            foreach (var member in typeMembers.Where(x => x.ShortName == methodName))
                            {
                                if (member is IMethod method)
                                {
                                    methods.Add(method);
                                }
                            }
                            
                            Logger.Info($"[FindMethodUsages] Found {methods.Count} methods named {methodName}");
                            
                            foreach (var method in methods)
                            {
                                if (method == null) continue;
                                
                                var containingType = method.ContainingType;
                                Logger.Info($"[FindMethodUsages] Checking method {method.ShortName} in type {containingType?.ShortName}, is extension: {method.IsExtensionMethod}");
                                
                                
                                var methodSourceFile = method.GetSourceFiles().FirstOrDefault();
                                if (methodSourceFile != null)
                                {
                                    var methodFilePath = methodSourceFile.GetLocation().FullPath;
                                    Logger.Info($"[FindMethodUsages] Method source file: {methodFilePath}");
                                    
                                    
                                    var normalizedMethodPath = System.IO.Path.GetFullPath(methodFilePath).ToLowerInvariant();
                                    var normalizedGeneratedPath = System.IO.Path.GetFullPath(request.GeneratedFilePath).ToLowerInvariant();
                                    
                                    if (normalizedMethodPath != normalizedGeneratedPath)
                                    {
                                        Logger.Info($"[FindMethodUsages] Method is not from the expected generated file");
                                        Logger.Info($"[FindMethodUsages] Expected: {normalizedGeneratedPath}");
                                        Logger.Info($"[FindMethodUsages] Actual: {normalizedMethodPath}");
                                        continue;
                                    }
                                }
                                    
                                if (!_extensionMethodDetector.IsGeneratedExtensionMethod(method, request.ValueName))
                                {
                                    Logger.Info($"[FindMethodUsages] Method {method.ShortName} is not a generated extension method for {request.ValueName}");
                                    continue;
                                }
                                
                                Logger.Info($"[FindMethodUsages] Method {method.ShortName} IS a generated extension method for {request.ValueName}");
                                    
                                var finder = psiServices.SingleThreadedFinder;
                                var referencesList = new List<IReference>();
                                var consumer = new SimpleReferenceConsumer(referencesList);
                                
                                finder.FindReferences(method, searchDomain, consumer, NullProgressIndicator.Create());
                                
                                Logger.Info($"[FindMethodUsages] Found {referencesList.Count} references to {method.ShortName}");
                                    
                                foreach (var reference in referencesList)
                                {
                                    var sourceFile = reference.GetTreeNode()?.GetSourceFile();
                                    if (sourceFile == null) continue;
                                        
                                    var document = sourceFile.Document;
                                    if (document == null) continue;
                                    
                                    var range = reference.GetTreeNode().GetDocumentRange();
                                    var startOffset = range.TextRange.StartOffset;
                                    var docCoords = new DocumentOffset(document, startOffset).ToDocumentCoords();
                                    
                                    
                                    var line = document.GetLineText(docCoords.Line);
                                    
                                    usages.Add(new MethodUsageLocation(
                                        filePath: sourceFile.GetLocation().FullPath,
                                        line: (int)docCoords.Line.Plus1(), 
                                        column: (int)docCoords.Column.Plus1(), 
                                        methodName: methodName,
                                        usageText: line.Trim()
                                    ));
                                }
                            }
                        }
                        
                        Logger.Info($"[FindMethodUsages] Found {usages.Count} usages");
                        
                        tcs.SetResult(usages);
                    }
                    catch (Exception ex)
                    {
                        tcs.SetException(ex);
                    }
                });
                
                
                usages = await tcs.Task;
            }
            catch (Exception ex)
            {
                Logger.Error($"Error finding method usages: {ex.Message}", ex);
            }
            
            return new FindMethodUsagesResponse(usages.ToArray());
        }

        public async Task<FindTagUsagesResponse> FindTagUsagesAsync(FindTagUsagesRequest request)
        {
            var usages = new List<MethodUsageLocation>();
            
            try
            {
                var tcs = new TaskCompletionSource<List<MethodUsageLocation>>();
                
                _solution.Locks.Tasks.StartNew(Lifetime.Eternal, Scheduling.MainGuard, () =>
                {
                    try
                    {
                        var psiServices = _solution.GetPsiServices();
                        var searchDomain = SearchDomainFactory.Instance.CreateSearchDomain(_solution, false);
                        var symbolCache = psiServices.Symbols;
                        var symbolScope = symbolCache.GetSymbolScope(LibrarySymbolScope.FULL, caseSensitive: true);
                        
                        foreach (var methodName in request.MethodNames)
                        {
                            Logger.Info($"[FindTagUsages] Searching for method: {methodName}");
                            var sourceMembers = symbolScope.GetSourceMembers(methodName);
                            var methods = new List<IMethod>();

                            var typeMembers = sourceMembers as ITypeMember[] ?? sourceMembers.ToArray();
                            
                            foreach (var member in typeMembers.Where(x => x.ShortName == methodName))
                            {
                                if (member is IMethod method)
                                {
                                    methods.Add(method);
                                }
                            }
                            
                            Logger.Info($"[FindTagUsages] Found {methods.Count} methods named {methodName}");
                            
                            foreach (var method in methods)
                            {
                                if (method == null) continue;
                                
                                var containingType = method.ContainingType;
                                Logger.Info($"[FindTagUsages] Checking method {method.ShortName} in type {containingType?.ShortName}, is extension: {method.IsExtensionMethod}");
                                
                                
                                var methodSourceFile = method.GetSourceFiles().FirstOrDefault();
                                if (methodSourceFile != null)
                                {
                                    var methodFilePath = methodSourceFile.GetLocation().FullPath;
                                    Logger.Info($"[FindTagUsages] Method source file: {methodFilePath}");
                                    
                                    
                                    var normalizedMethodPath = System.IO.Path.GetFullPath(methodFilePath).ToLowerInvariant();
                                    var normalizedGeneratedPath = System.IO.Path.GetFullPath(request.GeneratedFilePath).ToLowerInvariant();
                                    
                                    if (normalizedMethodPath != normalizedGeneratedPath)
                                    {
                                        Logger.Info($"[FindTagUsages] Method is not from the expected generated file");
                                        Logger.Info($"[FindTagUsages] Expected: {normalizedGeneratedPath}");
                                        Logger.Info($"[FindTagUsages] Actual: {normalizedMethodPath}");
                                        continue;
                                    }
                                }
                                    
                                if (!_extensionMethodDetector.IsGeneratedTagExtensionMethod(method, request.TagName))
                                {
                                    Logger.Info($"[FindTagUsages] Method {method.ShortName} is not a generated extension method for tag {request.TagName}");
                                    continue;
                                }
                                
                                Logger.Info($"[FindTagUsages] Method {method.ShortName} IS a generated extension method for tag {request.TagName}");
                                    
                                var finder = psiServices.SingleThreadedFinder;
                                var referencesList = new List<IReference>();
                                var consumer = new SimpleReferenceConsumer(referencesList);
                                
                                finder.FindReferences(method, searchDomain, consumer, NullProgressIndicator.Create());
                                
                                Logger.Info($"[FindTagUsages] Found {referencesList.Count} references to {method.ShortName}");
                                    
                                foreach (var reference in referencesList)
                                {
                                    var sourceFile = reference.GetTreeNode()?.GetSourceFile();
                                    if (sourceFile == null) continue;
                                        
                                    var document = sourceFile.Document;
                                    if (document == null) continue;
                                    
                                    var range = reference.GetTreeNode().GetDocumentRange();
                                    var startOffset = range.TextRange.StartOffset;
                                    var docCoords = new DocumentOffset(document, startOffset).ToDocumentCoords();
                                    
                                    
                                    var line = document.GetLineText(docCoords.Line);
                                    
                                    usages.Add(new MethodUsageLocation(
                                        filePath: sourceFile.GetLocation().FullPath,
                                        line: (int)docCoords.Line.Plus1(), 
                                        column: (int)docCoords.Column.Plus1(), 
                                        methodName: methodName,
                                        usageText: line.Trim()
                                    ));
                                }
                            }
                        }
                        
                        Logger.Info($"[FindTagUsages] Found {usages.Count} usages");
                        
                        tcs.SetResult(usages);
                    }
                    catch (Exception ex)
                    {
                        tcs.SetException(ex);
                    }
                });
                
                
                usages = await tcs.Task;
            }
            catch (Exception ex)
            {
                Logger.Error($"Error finding tag usages: {ex.Message}", ex);
            }
            
            return new FindTagUsagesResponse(usages.ToArray());
        }

        private class SimpleReferenceConsumer : IFindResultConsumer<IReference>
        {
            private readonly List<IReference> _references;
            
            public SimpleReferenceConsumer(List<IReference> references)
            {
                _references = references;
            }
            
            public IReference Build(FindResult result)
            {
                
                if (result is FindResultReference findResultReference)
                {
                    return findResultReference.Reference;
                }
                return null;
            }
            
            public FindExecution Merge(IReference reference)
            {
                if (reference != null)
                {
                    _references.Add(reference);
                }
                return FindExecution.Continue;
            }
        }
    }
}