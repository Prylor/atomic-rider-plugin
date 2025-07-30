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
        private const string AGRESSIVE_INLINING = "\t\t[MethodImpl(MethodImplOptions.AggressiveInlining)]";
        private const string UNSAFE_SUFFIX = "Unsafe";
        private const string REF_MODIFIER = "ref";

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
            if (config.AggressiveInlining)
            {
                sb.AppendLine("using System.Runtime.CompilerServices;");
            }

            if (interfaceDeclaration.GetContainingFile() is ICSharpFile sharpFile)
            {
                var existingUsings = new HashSet<string> {  };
                if (config.AggressiveInlining) 
                    existingUsings.Add("System.Runtime.CompilerServices");

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
                sb.AppendLine("\t\t///Tags");
                foreach (var tag in tags)
                {
                    GenerateTag(sb, tag);
                }
            }

            if (hasProperties)
            {
                if (hasTags)
                {
                    sb.AppendLine();
                    sb.AppendLine();
                }
                sb.AppendLine("\t\t///Values");
                foreach (var property in properties)
                {
                    GenerateValue(sb, property.Name, property.Type);
                }
            }

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

        private void GenerateValue(StringBuilder sb, string key, IType type)
        {
            int id = GetHashCode(key);
            string typeName = type.GetPresentableName(CSharpLanguage.Instance);
            string typeComment = IsBaseType(typeName) ? "" : $" // {typeName}";
            sb.AppendLine($"\t\tpublic const int {key} = {id};{typeComment}");
        }

        private void GenerateTag(StringBuilder sb, string tag)
        {
            int id = GetHashCode(tag);
            sb.AppendLine($"\t\tpublic const int {tag} = {id};");
        }

        private void GenerateTagExtensions(StringBuilder sb, string tag, string entity, bool useInlining)
        {
            sb.AppendLine();

            //Has:
            if (useInlining) sb.AppendLine(AGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static bool Has{tag}Tag(this {entity} obj) => obj.HasTag({tag});");

            //Add:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static bool Add{tag}Tag(this {entity} obj) => obj.AddTag({tag});");

            //Del:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static bool Del{tag}Tag(this {entity} obj) => obj.DelTag({tag});");
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

            string unsafeSuffix = unsafeAccess ? UNSAFE_SUFFIX : string.Empty;
            string refModifier = unsafeAccess ? REF_MODIFIER : string.Empty;

            //Get:
            if (useInlining) sb.AppendLine(AGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static {typeName} Get{name}(this {entity} obj) => " +
                          $"obj.GetValue{unsafeSuffix}<{typeName}>({name});");
            
            //Get Ref:
            if (unsafeAccess)
            {
                sb.AppendLine();
                sb.AppendLine($"\t\tpublic static {refModifier} {typeName} Ref{name}(this {entity} obj) => " +
                              $"{refModifier} obj.GetValue{unsafeSuffix}<{typeName}>({name});");
            }
            
            //TryGet:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGRESSIVE_INLINING);
            sb.AppendLine(
                $"\t\tpublic static bool TryGet{name}(this {entity} obj, out {typeName} value) =>" +
                $" obj.TryGetValue{unsafeSuffix}({name}, out value);");

            //Add:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static void Add{name}(this {entity} obj, {typeName} value) => " +
                          $"obj.AddValue({name}, value);");

            //Has:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static bool Has{name}(this {entity} obj) => " +
                          $"obj.HasValue({name});");

            //Del:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static bool Del{name}(this {entity} obj) => " +
                          $"obj.DelValue({name});");

            //Set:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(AGRESSIVE_INLINING);
            sb.AppendLine($"\t\tpublic static void Set{name}(this {entity} obj, {typeName} value) => " +
                          $"obj.SetValue({name}, value);");
        }

        private bool IsBaseType(string type)
        {
            return string.IsNullOrEmpty(type) || type is "object" or "Object";
        }

        private int GetHashCode(string value)
        {
            unchecked
            {
                int hash = 17;
                foreach (char c in value)
                {
                    hash = hash * 31 + c;
                }

                return hash;
            }
        }
    }
}