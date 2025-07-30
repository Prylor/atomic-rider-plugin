using System;
using System.Linq;
using System.Threading.Tasks;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Caches;
using JetBrains.Util;
using ReSharperPlugin.AtomicPlugin.Model;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class TypeValidator : ITypeValidator
    {
        private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger<TypeValidator>();
        private readonly ISymbolScopeManager _symbolScopeManager;

        public TypeValidator(ISymbolScopeManager symbolScopeManager)
        {
            _symbolScopeManager = symbolScopeManager;
        }

        public async Task<TypeValidationResponse> ValidateAsync(TypeValidationRequest request)
        {
            return await Task.Run(() =>
            {
                return _symbolScopeManager.ExecuteWithReadLock(() =>
                {
                        var symbolScope = _symbolScopeManager.GetSymbolScope(LibrarySymbolScope.FULL, caseSensitive: true);
                        
                        Logger.Info($"[ValidateType] Validating type '{request.TypeName}' with imports: {string.Join(", ", request.Imports)}");
                        
                        
                        if (request.TypeName.Contains("."))
                        {
                            return ValidateFullyQualifiedType(request.TypeName, symbolScope);
                        }
                        
                        
                        return ValidateSimpleType(request.TypeName, request.Imports, symbolScope);
                });
            });
        }

        private TypeValidationResponse ValidateFullyQualifiedType(string typeName, ISymbolScope symbolScope)
        {
            var lastDotIndex = typeName.LastIndexOf('.');
            var namespacePart = typeName.Substring(0, lastDotIndex);
            var typePart = typeName.Substring(lastDotIndex + 1);
            
            Logger.Info($"[ValidateType] Fully qualified type detected. Namespace: '{namespacePart}', Type: '{typePart}'");
            
            
            var typeElements = symbolScope.GetElementsByShortName(typePart)
                .OfType<ITypeElement>()
                .Where(t => t.GetContainingNamespace()?.QualifiedName == namespacePart)
                .ToList();
            
            if (typeElements.Any())
            {
                var firstType = typeElements.First();
                Logger.Info($"[ValidateType] Found fully qualified type: {firstType.GetClrName().FullName}");
                
                return new TypeValidationResponse(
                    isValid: true,
                    fullTypeName: firstType.GetClrName().FullName,
                    suggestedImport: null,
                    suggestedImports: new string[0],
                    isAmbiguous: false,
                    ambiguousNamespaces: new string[0]
                );
            }
            
            Logger.Info($"[ValidateType] Fully qualified type '{typeName}' not found");
            
            return new TypeValidationResponse(
                isValid: false,
                fullTypeName: null,
                suggestedImport: null,
                suggestedImports: new string[0],
                isAmbiguous: false,
                ambiguousNamespaces: new string[0]
            );
        }

        private TypeValidationResponse ValidateSimpleType(string typeName, string[] imports, ISymbolScope symbolScope)
        {
            
            var accessibleTypes = symbolScope.GetElementsByShortName(typeName)
                .OfType<ITypeElement>()
                .Where(t => IsTypeAccessible(t, imports))
                .ToList();
            
            foreach (var type in accessibleTypes)
            {
                var ns = type.GetContainingNamespace()?.QualifiedName ?? "<global>";
                Logger.Info($"[ValidateType] Found accessible type: {type.GetClrName().FullName} in namespace {ns}");
            }
            
            
            if (accessibleTypes.Count > 1)
            {
                var ambiguousNamespaces = accessibleTypes
                    .Select(t => t.GetContainingNamespace()?.QualifiedName)
                    .Where(ns => !string.IsNullOrEmpty(ns))
                    .Distinct()
                    .OrderBy(ns => ns)
                    .ToArray();
                
                Logger.Info($"[ValidateType] Type is ambiguous! Found in namespaces: {string.Join(", ", ambiguousNamespaces)}");
                
                
                if (ambiguousNamespaces.Length > 1)
                {
                    var firstType = accessibleTypes.First();
                    return new TypeValidationResponse(
                        isValid: false,
                        fullTypeName: firstType.GetClrName().FullName,
                        suggestedImport: null,
                        suggestedImports: new string[0],
                        isAmbiguous: true,
                        ambiguousNamespaces: ambiguousNamespaces
                    );
                }
            }
            
            var typeElement = accessibleTypes.FirstOrDefault();
            
            if (typeElement != null)
            {
                var resultNs = typeElement.GetContainingNamespace()?.QualifiedName ?? "<global>";
                Logger.Info($"[ValidateType] Type is valid: {typeElement.GetClrName().FullName} from namespace {resultNs}");
                
                return new TypeValidationResponse(
                    isValid: true,
                    fullTypeName: typeElement.GetClrName().FullName,
                    suggestedImport: null,
                    suggestedImports: new string[0],
                    isAmbiguous: false,
                    ambiguousNamespaces: new string[0]
                );
            }
            
            
            var allTypesWithName = symbolScope.GetElementsByShortName(typeName)
                .OfType<ITypeElement>()
                .Where(t => t.GetContainingNamespace() != null)
                .ToList();
            
            Logger.Info($"[ValidateType] Found {allTypesWithName.Count} types named '{typeName}' in other namespaces");
            
            if (allTypesWithName.Any())
            {
                var namespaces = allTypesWithName
                    .Select(t => t.GetContainingNamespace()?.QualifiedName)
                    .Where(ns => !string.IsNullOrEmpty(ns))
                    .Distinct()
                    .OrderBy(ns => ns)
                    .ToArray();
                
                Logger.Info($"[ValidateType] Suggested namespaces: {string.Join(", ", namespaces)}");
                
                var firstType = allTypesWithName.First();
                return new TypeValidationResponse(
                    isValid: false,
                    fullTypeName: firstType.GetClrName().FullName,
                    suggestedImport: namespaces.FirstOrDefault(), 
                    suggestedImports: namespaces,
                    isAmbiguous: false,
                    ambiguousNamespaces: new string[0]
                );
            }
            
            Logger.Info($"[ValidateType] Type '{typeName}' not found anywhere");
            
            return new TypeValidationResponse(
                isValid: false,
                fullTypeName: null,
                suggestedImport: null,
                suggestedImports: new string[0],
                isAmbiguous: false,
                ambiguousNamespaces: new string[0]
            );
        }

        private bool IsTypeAccessible(ITypeElement typeElement, string[] imports)
        {
            var typeNamespace = typeElement.GetContainingNamespace()?.QualifiedName;
            if (string.IsNullOrEmpty(typeNamespace))
                return true;
            
            
            
            return imports.Any(import => import == typeNamespace);
        }
    }
}