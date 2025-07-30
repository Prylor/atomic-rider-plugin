using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class CodeGenerator : ICodeGenerator
    {
        private readonly IHashCodeGenerator _hashCodeGenerator;

        public CodeGenerator(IHashCodeGenerator hashCodeGenerator)
        {
            _hashCodeGenerator = hashCodeGenerator;
        }

        public string GenerateCode(AtomicEntityApiConfig config)
        {
            var sb = new StringBuilder();

            sb.AppendLine("/**");
            sb.AppendLine("* Code generation. Don't modify! ");
            sb.AppendLine("**/");
            sb.AppendLine();

            GenerateUsingStatements(sb, config);
            sb.AppendLine();

            sb.AppendLine($"namespace {config.Namespace}");
            sb.AppendLine("{");

            GenerateStaticClass(sb, config);

            sb.AppendLine("}");

            return sb.ToString();
        }

        private void GenerateUsingStatements(StringBuilder sb, AtomicEntityApiConfig config)
        {

            if (config.AggressiveInlining)
            {
                sb.AppendLine("using System.Runtime.CompilerServices;");
            }

            var existingUsings = new HashSet<string>();
            if (config.AggressiveInlining) 
                existingUsings.Add("System.Runtime.CompilerServices");

            foreach (var import in config.Imports)
            {
                if (!existingUsings.Contains(import))
                {
                    sb.AppendLine($"using {import};");
                    existingUsings.Add(import);
                }
            }
        }

        private void GenerateStaticClass(StringBuilder sb, AtomicEntityApiConfig config)
        {
            sb.AppendLine($"\tpublic static class {config.ClassName}");
            sb.AppendLine("\t{");

            bool hasTags = config.Tags.Any();
            bool hasProperties = config.Values.Any();

            if (hasTags)
            {
                sb.AppendLine("\t\t///Tags");
                foreach (var tag in config.Tags)
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
                foreach (var value in config.Values)
                {
                    GenerateValue(sb, value.Name, value.Type);
                }
            }

            if (hasTags)
            {
                sb.AppendLine();
                sb.AppendLine();
                sb.AppendLine("\t\t///Tag Extensions");
                foreach (var tag in config.Tags)
                {
                    GenerateTagExtensions(sb, tag, config.EntityType, config.AggressiveInlining);
                }
            }

            if (hasProperties)
            {
                sb.AppendLine();
                sb.AppendLine();
                sb.AppendLine("\t\t///Value Extensions");
                foreach (var value in config.Values)
                {
                    GenerateValueExtensions(sb, value.Name, value.Type, 
                        config.EntityType, config.AggressiveInlining, config.UnsafeAccess);
                }
            }

            sb.AppendLine("    }");
        }

        private void GenerateValue(StringBuilder sb, string key, string type)
        {
            int id = _hashCodeGenerator.GetHashCode(key);
            string typeComment = IsBaseType(type) ? "" : $" // {type}";
            sb.AppendLine($"\t\tpublic const int {key} = {id};{typeComment}");
        }

        private void GenerateTag(StringBuilder sb, string tag)
        {
            int id = _hashCodeGenerator.GetHashCode(tag);
            sb.AppendLine($"\t\tpublic const int {tag} = {id};");
        }

        private void GenerateTagExtensions(StringBuilder sb, string tag, string entity, bool useInlining)
        {
            const string aggressiveInlining = "\t\t[MethodImpl(MethodImplOptions.AggressiveInlining)]";
            sb.AppendLine();

            //Has:
            if (useInlining) sb.AppendLine(aggressiveInlining);
            sb.AppendLine($"\t\tpublic static bool Has{tag}Tag(this {entity} obj) => obj.HasTag({tag});");

            //Add:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(aggressiveInlining);
            sb.AppendLine($"\t\tpublic static bool Add{tag}Tag(this {entity} obj) => obj.AddTag({tag});");

            //Del:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(aggressiveInlining);
            sb.AppendLine($"\t\tpublic static bool Del{tag}Tag(this {entity} obj) => obj.DelTag({tag});");
        }

        private void GenerateValueExtensions(
            StringBuilder sb,
            string name,
            string typeName,
            string entity,
            bool useInlining,
            bool unsafeAccess
        )
        {
            const string aggressiveInlining = "\t\t[MethodImpl(MethodImplOptions.AggressiveInlining)]";
            const string unsafeSuffix = "Unsafe";
            const string refModifier = "ref";
            
            sb.AppendLine();

            string unsafeSuffixStr = unsafeAccess ? unsafeSuffix : string.Empty;
            string refModifierStr = unsafeAccess ? refModifier : string.Empty;

            //Get:
            if (useInlining) sb.AppendLine(aggressiveInlining);
            sb.AppendLine($"\t\tpublic static {typeName} Get{name}(this {entity} obj) => " +
                          $"obj.GetValue{unsafeSuffixStr}<{typeName}>({name});");
            
            //Get Ref:
            if (unsafeAccess)
            {
                sb.AppendLine();
                sb.AppendLine($"\t\tpublic static {refModifierStr} {typeName} Ref{name}(this {entity} obj) => " +
                              $"{refModifierStr} obj.GetValue{unsafeSuffixStr}<{typeName}>({name});");
            }
            
            //TryGet:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(aggressiveInlining);
            sb.AppendLine(
                $"\t\tpublic static bool TryGet{name}(this {entity} obj, out {typeName} value) =>" +
                $" obj.TryGetValue{unsafeSuffixStr}({name}, out value);");

            //Add:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(aggressiveInlining);
            sb.AppendLine($"\t\tpublic static void Add{name}(this {entity} obj, {typeName} value) => " +
                          $"obj.AddValue({name}, value);");

            //Has:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(aggressiveInlining);
            sb.AppendLine($"\t\tpublic static bool Has{name}(this {entity} obj) => " +
                          $"obj.HasValue({name});");

            //Del:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(aggressiveInlining);
            sb.AppendLine($"\t\tpublic static bool Del{name}(this {entity} obj) => " +
                          $"obj.DelValue({name});");

            //Set:
            sb.AppendLine();
            if (useInlining) sb.AppendLine(aggressiveInlining);
            sb.AppendLine($"\t\tpublic static void Set{name}(this {entity} obj, {typeName} value) => " +
                          $"obj.SetValue({name}, value);");
        }

        private bool IsBaseType(string type)
        {
            return string.IsNullOrEmpty(type) || type is "object" or "Object";
        }
    }
}