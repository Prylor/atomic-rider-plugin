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
    public class TypeResolver : ITypeResolver
    {
        private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger<TypeResolver>();
        private readonly ISymbolScopeManager _symbolScopeManager;

        public TypeResolver(ISymbolScopeManager symbolScopeManager)
        {
            _symbolScopeManager = symbolScopeManager;
        }

        public async Task<TypeCompletionResponse> GetCompletionsAsync(TypeCompletionRequest request)
        {
            var items = new List<TypeCompletionItem>();
            
            await Task.Run(() =>
            {
                try
                {
                    _symbolScopeManager.ExecuteWithReadLock(() =>
                    {
                            var symbolScope = _symbolScopeManager.GetSymbolScope(LibrarySymbolScope.FULL, caseSensitive: false);
                            
                            var importedNamespaces = new HashSet<string>(request.Imports);
                            importedNamespaces.Add("System");
                            
                            if (!string.IsNullOrEmpty(request.NamespaceFilter))
                            {
                                items.AddRange(GetTypesFromNamespace(symbolScope, request.Prefix, request.NamespaceFilter));
                            }
                            else
                            {
                                items.AddRange(GetTypesWithPrefix(symbolScope, request.Prefix, importedNamespaces));
                                items.AddRange(GetNamespaceSuggestions(symbolScope, request.Prefix));
                            }
                    });
                }
                catch (Exception ex)
                {
                    Logger.Error($"Error getting type completions: {ex.Message}");
                }
            });
            
            return new TypeCompletionResponse(items.ToArray());
        }

        private List<TypeCompletionItem> GetTypesFromNamespace(ISymbolScope symbolScope, string prefix, string namespaceFilter)
        {
            Logger.Info($"[TypeCompletion] Filtering by namespace: {namespaceFilter}");
            
            var items = new List<TypeCompletionItem>();
            var allShortNames = symbolScope.GetAllShortNames();
            
            foreach (var shortName in allShortNames)
            {
                if (!string.IsNullOrEmpty(prefix) && !shortName.StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
                    continue;
                
                try
                {
                    var types = symbolScope.GetElementsByShortName(shortName).OfType<ITypeElement>();
                    foreach (var type in types)
                    {
                        var ns = type.GetContainingNamespace()?.QualifiedName;
                        if (ns == namespaceFilter && IsValidType(type))
                        {
                            items.Add(CreateCompletionItem(type, namespaceFilter));
                        }
                    }
                }
                catch
                {
                    
                }
            }
            
            return items.OrderBy(t => t.TypeName).Take(200).ToList();
        }

        private List<TypeCompletionItem> GetTypesWithPrefix(ISymbolScope symbolScope, string prefix, HashSet<string> importedNamespaces)
        {
            var items = new List<TypeCompletionItem>();
            var matchingTypes = new List<ITypeElement>();
            
            if (!string.IsNullOrEmpty(prefix))
            {
                Logger.Info($"[TypeCompletion] Looking for types with prefix: {prefix}");
                
                var allShortNames = symbolScope.GetAllShortNames();
                var matchingNames = allShortNames
                    .Where(name => name.StartsWith(prefix, StringComparison.OrdinalIgnoreCase));
                
                foreach (var shortName in matchingNames)
                {
                    try
                    {
                        var types = symbolScope.GetElementsByShortName(shortName).OfType<ITypeElement>();
                        matchingTypes.AddRange(types);
                    }
                    catch
                    {
                        
                    }
                }
            }
            else
            {
                
                Logger.Info($"[TypeCompletion] No prefix, showing types from imported namespaces");
                
                foreach (var ns in importedNamespaces)
                {
                    try
                    {
                        var someTypes = symbolScope.GetAllShortNames()
                            .Take(50)
                            .ToList();
                        
                        foreach (var typeName in someTypes)
                        {
                            var types = symbolScope.GetElementsByShortName(typeName)
                                .OfType<ITypeElement>()
                                .Where(t => t.GetContainingNamespace()?.QualifiedName == ns);
                            matchingTypes.AddRange(types);
                        }
                    }
                    catch
                    {
                        
                    }
                }
            }
            
            
            foreach (var typeElement in matchingTypes)
            {
                try
                {
                    if (!IsValidType(typeElement) || !IsAccessible(typeElement))
                        continue;
                    
                    var ns = typeElement.GetContainingNamespace()?.QualifiedName ?? "";
                    items.Add(CreateCompletionItem(typeElement, ns));
                }
                catch
                {
                    
                }
            }
            
            return SortByRelevance(items, prefix, importedNamespaces);
        }

        private List<TypeCompletionItem> GetNamespaceSuggestions(ISymbolScope symbolScope, string prefix)
        {
            if (string.IsNullOrEmpty(prefix))
                return new List<TypeCompletionItem>();
            
            var items = new List<TypeCompletionItem>();
            var namespaceSet = new HashSet<string>();
            
            
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
                                namespaceSet.Add(partialNamespace);
                            }
                        }
                    }
                }
                catch
                {
                    
                }
            }
            
            
            var matchingNamespaces = namespaceSet
                .Where(ns => ns.StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
                .OrderBy(ns => ns);
            
            
            foreach (var ns in matchingNamespaces)
            {
                items.Add(new TypeCompletionItem(
                    typeName: ns,
                    fullTypeName: ns,
                    @namespace: "", 
                    assemblyName: "",
                    isGeneric: false,
                    typeKind: TypeKind.Class 
                ));
            }
            
            return items;
        }

        private bool IsValidType(ITypeElement typeElement)
        {
            var typeKind = GetTypeKind(typeElement);
            if (typeKind == null) return false;
            
            
            if (typeElement.ShortName.StartsWith("<") || typeElement.ShortName.Contains("__"))
                return false;
            
            return true;
        }

        private bool IsAccessible(ITypeElement typeElement)
        {
            try
            {
                
                if (typeElement.GetContainingType() != null)
                {
                    var parent = typeElement.GetContainingType();
                    if (parent != null && parent.ShortName.StartsWith("<"))
                        return false;
                }
                return true;
            }
            catch
            {
                
                return true;
            }
        }

        private TypeCompletionItem CreateCompletionItem(ITypeElement typeElement, string @namespace)
        {
            return new TypeCompletionItem(
                typeName: typeElement.ShortName,
                fullTypeName: typeElement.GetClrName().FullName,
                @namespace: @namespace,
                assemblyName: typeElement.Module?.DisplayName ?? "",
                isGeneric: typeElement.TypeParameters.Count > 0,
                typeKind: GetTypeKind(typeElement)
            );
        }

        private List<TypeCompletionItem> SortByRelevance(List<TypeCompletionItem> items, string prefix, HashSet<string> importedNamespaces)
        {
            return items
                .OrderBy(t => 
                {
                    
                    if (t.TypeName.Equals(prefix, StringComparison.OrdinalIgnoreCase))
                        return 0;
                        
                    
                    var isImported = string.IsNullOrEmpty(t.Namespace) || 
                        importedNamespaces.Any(import => 
                            t.Namespace.Equals(import, StringComparison.Ordinal) || 
                            t.Namespace.StartsWith(import + ".", StringComparison.Ordinal));
                            
                    if (isImported && t.TypeName.StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
                        return 1;
                        
                    
                    if (t.Namespace.StartsWith("System") && t.TypeName.StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
                        return 2;
                        
                    
                    if (t.TypeName.StartsWith(prefix, StringComparison.OrdinalIgnoreCase))
                        return 3;
                        
                    
                    return 4;
                })
                .ThenBy(t => t.TypeName.Length)
                .ThenBy(t => t.TypeName)
                .ToList();
        }

        private TypeKind GetTypeKind(ITypeElement typeElement)
        {
            switch (typeElement)
            {
                case IClass _:
                    return TypeKind.Class;
                case IInterface _:
                    return TypeKind.Interface;
                case IStruct _:
                    return TypeKind.Struct;
                case IEnum _:
                    return TypeKind.Enum;
                case IDelegate _:
                    return TypeKind.Delegate;
                default:
                    return TypeKind.Class;
            }
        }
    }
}