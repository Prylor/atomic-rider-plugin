using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Psi.Util;

namespace ReSharperPlugin.AtomicPlugin
{
    public class EntityApiCodeGenerator
    {
        private const string AGGRESSIVE_INLINING = "\t\t[MethodImpl(MethodImplOptions.AggressiveInlining)]";
        private const string UNSAFE_SUFFIX = "Unsafe";
        private const string REF_MODIFIER = "ref";
        private const string PARAM_NAME = "entity";

        public string GenerateCode(IInterfaceDeclaration interfaceDeclaration, EntityApiConfig config)
        {
            var sb = new StringBuilder();

            sb.AppendLine("/**");
            sb.AppendLine("* Code generation. Don't modify! ");
            sb.AppendLine("**/");
            sb.AppendLine();

            GenerateUsingStatements(sb, interfaceDeclaration, config);
            sb.AppendLine();

            sb.AppendLine($"namespace {config.Namespace}");
            sb.AppendLine("{");

            GenerateStaticClass(sb, interfaceDeclaration, config);

            sb.AppendLine("}");

            return sb.ToString();
        }

        private void GenerateUsingStatements(StringBuilder sb, IInterfaceDeclaration interfaceDeclaration, EntityApiConfig config)
        {
            sb.AppendLine("using Atomic.Entities;");
            sb.AppendLine("using static Atomic.Entities.EntityNames;");
            
            if (config.AggressiveInlining)
            {
                sb.AppendLine("using System.Runtime.CompilerServices;");
            }

            sb.AppendLine("#if UNITY_EDITOR");
            sb.AppendLine("using UnityEditor;");
            sb.AppendLine("#endif");

            if (interfaceDeclaration.GetContainingFile() is ICSharpFile sharpFile)
            {
                var existingUsings = new HashSet<string> 
                { 
                    "Atomic.Entities",
                    "Atomic.Entities.EntityNames",
                    "System.Runtime.CompilerServices",
                    "UnityEditor"
                };

                foreach (var usingDirective in sharpFile.ImportsEnumerable)
                {
                    var text = usingDirective.GetText().Trim();
                    if (text.StartsWith("using ") && text.EndsWith(";"))
                    {
                        var ns = text.Substring(6, text.Length - 7).Trim();
                        if (!existingUsings.Contains(ns))
                        {
                            sb.AppendLine($"using {ns};");
                            existingUsings.Add(ns);
                        }
                    }
                }
            }
        }

        private void GenerateStaticClass(StringBuilder sb, IInterfaceDeclaration interfaceDeclaration,
            EntityApiConfig config)
        {
            sb.AppendLine("#if UNITY_EDITOR");
            sb.AppendLine("\t[InitializeOnLoad]");
            sb.AppendLine("#endif");
            
            sb.AppendLine($"\tpublic static class {config.ClassName}");
            sb.AppendLine("\t{");

            var tagsEnum = FindTagsEnum(interfaceDeclaration);
            var tags = tagsEnum?.EnumMemberDeclarations.Select(m => m.DeclaredName).ToList() ?? new List<string>();
            bool hasTags = tags.Any();

            var properties = interfaceDeclaration.PropertyDeclarations
                .Where(p => p.DeclaredElement != null && p.DeclaredElement.IsReadable)
                .Select(p => new { Name = p.DeclaredElement.ShortName, Type = p.DeclaredElement.Type })
                .ToList();
            bool hasProperties = properties.Any();

            if (hasTags)
            {
                sb.AppendLine();
                sb.AppendLine("\t\t///Tags");
                foreach (var tag in tags)
                {
                    sb.AppendLine($"\t\tpublic static readonly int {tag};");
                }
            }

            if (hasProperties)
            {
                if (hasTags) sb.AppendLine();
                
                sb.AppendLine("\t\t///Values");
                foreach (var property in properties)
                {
                    string typeName = property.Type.GetPresentableName(CSharpLanguage.Instance);
                    string typeComment = IsBaseType(typeName) ? string.Empty : $"// {typeName}";
                    sb.AppendLine($"\t\tpublic static readonly int {property.Name}; {typeComment}");
                }
            }

            sb.AppendLine();
            sb.AppendLine($"\t\tstatic {config.ClassName}()");
            sb.AppendLine("\t\t{");
            
            if (hasTags)
            {
                sb.AppendLine("\t\t\t//Tags");
                foreach (var tag in tags)
                {
                    sb.AppendLine($"\t\t\t{tag} = NameToId(nameof({tag}));");
                }
            }
            
            // Initialize values in static constructor
            if (hasProperties)
            {
                if (hasTags) sb.AppendLine();
                
                sb.AppendLine("\t\t\t//Values");
                foreach (var property in properties)
                {
                    sb.AppendLine($"\t\t\t{property.Name} = NameToId(nameof({property.Name}));");
                }
            }
            
            sb.AppendLine("\t\t}");

            if (hasTags)
            {
                sb.AppendLine();
                sb.AppendLine();
                sb.AppendLine("\t\t///Tag Extensions");
                foreach (var tag in tags)
                {
                    GenerateTagExtensions(sb, tag, config.EntityType, config.AggressiveInlining);
                }
            }

            if (hasProperties)
            {
                sb.AppendLine();
                sb.AppendLine();
                sb.AppendLine("\t\t///Value Extensions");
                foreach (var property in properties)
                {
                    GenerateValueExtensions(sb, property.Name, property.Type, 
                        config.EntityType, config.AggressiveInlining, config.UnsafeAccess);
                }
            }

            sb.AppendLine("    }");
        }

        private IEnumDeclaration FindTagsEnum(IInterfaceDeclaration interfaceDeclaration)
        {
            return interfaceDeclaration.NestedTypeDeclarations
                .OfType<IEnumDeclaration>()
                .FirstOrDefault(e => e.DeclaredName == "Tags");
        }


        private void GenerateTagExtensions(StringBuilder sb, string tag, string entity, bool useInlining)
        {
            sb.AppendLine();
            
            sb.AppendLine($"\t\t#region {tag}");
            sb.AppendLine();

            //Has:
            if (useInlining) sb.AppendLine(AGGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static bool Has{tag}Tag(this {entity} {PARAM_NAME}) => {PARAM_NAME}.HasTag({tag});");

            //Add:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static bool Add{tag}Tag(this {entity} {PARAM_NAME}) => {PARAM_NAME}.AddTag({tag});");

            //Del:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static bool Del{tag}Tag(this {entity} {PARAM_NAME}) => {PARAM_NAME}.DelTag({tag});");
            
            sb.AppendLine();
            sb.AppendLine("\t\t#endregion");
        }

        private void GenerateValueExtensions(
            StringBuilder sb,
            string name,
            IType type,
            string entity,
            bool useInlining,
            bool unsafeAccess
        )
        {
            string typeName = type.GetPresentableName(CSharpLanguage.Instance);
            sb.AppendLine();

            sb.AppendLine($"\t\t#region {name}");
            sb.AppendLine();

            string unsafeSuffix = unsafeAccess ? UNSAFE_SUFFIX : string.Empty;
            string refModifier = unsafeAccess ? REF_MODIFIER : string.Empty;

            //Get:
            if (useInlining) sb.AppendLine(AGGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static {typeName} Get{name}(this {entity} {PARAM_NAME}) => " +
                          $"{PARAM_NAME}.GetValue{unsafeSuffix}<{typeName}>({name});");
            
            //Get Ref:
            if (unsafeAccess)
            {
                sb.AppendLine();
                sb.AppendLine($"\t\tpublic static {refModifier} {typeName} Ref{name}(this {entity} {PARAM_NAME}) => " +
                              $"{refModifier} {PARAM_NAME}.GetValue{unsafeSuffix}<{typeName}>({name});");
            }
            
            //TryGet:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGGRESSIVE_INLINING);
            sb.AppendLine(
                $"\t\tpublic static bool TryGet{name}(this {entity} {PARAM_NAME}, out {typeName} value) =>" +
                $" {PARAM_NAME}.TryGetValue{unsafeSuffix}({name}, out value);");

            //Add:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static void Add{name}(this {entity} {PARAM_NAME}, {typeName} value) => " +
                          $"{PARAM_NAME}.AddValue({name}, value);");

            //Has:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static bool Has{name}(this {entity} {PARAM_NAME}) => " +
                          $"{PARAM_NAME}.HasValue({name});");

            //Del:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static bool Del{name}(this {entity} {PARAM_NAME}) => " +
                          $"{PARAM_NAME}.DelValue({name});");

            //Set:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static void Set{name}(this {entity} {PARAM_NAME}, {typeName} value) => " +
                          $"{PARAM_NAME}.SetValue({name}, value);");
            
            sb.AppendLine();
            sb.AppendLine("\t\t#endregion");
        }

        private bool IsBaseType(string type)
        {
            return string.IsNullOrEmpty(type) || type is "object" or "Object";
        }

    }
}