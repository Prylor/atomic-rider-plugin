using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Caches;
using JetBrains.Util;
using ReSharperPlugin.AtomicPlugin.Model;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class NamespaceResolver : INamespaceResolver
    {
        private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger<NamespaceResolver>();
        private readonly ISymbolScopeManager _symbolScopeManager;

        public NamespaceResolver(ISymbolScopeManager symbolScopeManager)
        {
            _symbolScopeManager = symbolScopeManager;
        }

        public async Task<NamespaceCompletionResponse> GetCompletionsAsync(NamespaceCompletionRequest request)
        {
            return await Task.Run(() =>
            {
                var namespaces = new HashSet<string>();
                
                try
                {
                    _symbolScopeManager.ExecuteWithReadLock(() =>
                    {
                            var symbolScope = _symbolScopeManager.GetSymbolScope(LibrarySymbolScope.FULL, caseSensitive: false);
                        
                        
                        var allShortNames = symbolScope.GetAllShortNames();
                        
                        foreach (var shortName in allShortNames)
                        {
                            try
                            {
                                var types = symbolScope.GetElementsByShortName(shortName).OfType<ITypeElement>();
                                foreach (var type in types)
                                {
                                    var ns = type.GetContainingNamespace()?.QualifiedName;
                                    if (!string.IsNullOrEmpty(ns))
                                    {
                                        
                                        var parts = ns.Split('.');
                                        for (int i = 1; i <= parts.Length; i++)
                                        {
                                            var partialNamespace = string.Join(".", parts.Take(i));
                                            if (string.IsNullOrEmpty(request.Prefix) || 
                                                partialNamespace.StartsWith(request.Prefix, StringComparison.OrdinalIgnoreCase))
                                            {
                                                namespaces.Add(partialNamespace);
                                            }
                                        }
                                    }
                                }
                            }
                            catch
                            {
                                
                            }
                        }
                    });
                }
                catch (Exception ex)
                {
                    Logger.Error($"Error getting namespace completions: {ex.Message}");
                }
                
                var sortedNamespaces = namespaces
                    .OrderBy(ns => 
                    {
                        
                        if (ns.Equals(request.Prefix, StringComparison.OrdinalIgnoreCase))
                            return 0;
                        
                        if (ns.StartsWith("System"))
                            return 1;
                        
                        return 2;
                    })
                    .ThenBy(ns => ns.Length)
                    .ThenBy(ns => ns)
                    .ToArray();
                
                return new NamespaceCompletionResponse(sortedNamespaces);
            });
        }

        public async Task<NamespaceValidationResponse> ValidateAsync(NamespaceValidationRequest request)
        {
            return await Task.Run(() =>
            {
                return _symbolScopeManager.ExecuteWithReadLock(() =>
                {
                        var symbolScope = _symbolScopeManager.GetSymbolScope(LibrarySymbolScope.FULL, caseSensitive: true);
                    
                    
                    var namespaceExists = false;
                    var hasTypes = false;
                    
                    
                    var allNamespaces = new HashSet<string>();
                    var allShortNames = symbolScope.GetAllShortNames();
                    
                    foreach (var shortName in allShortNames)
                    {
                        try
                        {
                            var types = symbolScope.GetElementsByShortName(shortName).OfType<ITypeElement>();
                            foreach (var type in types)
                            {
                                var ns = type.GetContainingNamespace()?.QualifiedName;
                                if (!string.IsNullOrEmpty(ns))
                                {
                                    allNamespaces.Add(ns);
                                    
                                    
                                    if (ns == request.Namespace)
                                    {
                                        hasTypes = true;
                                        namespaceExists = true;
                                    }
                                    
                                    else if (ns.StartsWith(request.Namespace + ".") || request.Namespace.StartsWith(ns + "."))
                                    {
                                        namespaceExists = true;
                                    }
                                }
                            }
                        }
                        catch
                        {
                            
                        }
                    }
                    
                    
                    
                    return new NamespaceValidationResponse(
                        isValid: namespaceExists,
                        hasTypes: hasTypes
                    );
                });
            });
        }
    }
}