using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class CodeGenerator : ICodeGenerator
    {
        private const string AGGRESSIVE_INLINING = "\t\t[MethodImpl(MethodImplOptions.AggressiveInlining)]";
        private const string UNSAFE_SUFFIX = "Unsafe";
        private const string REF_MODIFIER = "ref";
        private const string PARAM_NAME = "entity";

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
            // Core Atomic Framework usings - ALWAYS included
            sb.AppendLine("using Atomic.Entities;");
            sb.AppendLine("using static Atomic.Entities.EntityNames;");
            
            if (config.AggressiveInlining)
            {
                sb.AppendLine("using System.Runtime.CompilerServices;");
            }

            // Unity Editor using
            sb.AppendLine("#if UNITY_EDITOR");
            sb.AppendLine("using UnityEditor;");
            sb.AppendLine("#endif");

            var existingUsings = new HashSet<string> 
            { 
                "Atomic.Entities",
                "Atomic.Entities.EntityNames",
                "System.Runtime.CompilerServices",
                "UnityEditor"
            };

            foreach (var import in config.Imports)
            {
                // Skip Atomic.Entities if it's explicitly in imports since we always add it
                if (!existingUsings.Contains(import))
                {
                    sb.AppendLine($"using {import};");
                    existingUsings.Add(import);
                }
            }
        }

        private void GenerateStaticClass(StringBuilder sb, AtomicEntityApiConfig config)
        {
            // Add Unity Editor attribute
            sb.AppendLine("#if UNITY_EDITOR");
            sb.AppendLine("\t[InitializeOnLoad]");
            sb.AppendLine("#endif");
            
            sb.AppendLine($"\tpublic static class {config.ClassName}");
            sb.AppendLine("\t{");

            bool hasTags = config.Tags.Any();
            bool hasValues = config.Values.Any();

            // Generate tag fields
            if (hasTags)
            {
                sb.AppendLine();
                sb.AppendLine("\t\t///Tags");
                foreach (var tag in config.Tags)
                {
                    sb.AppendLine($"\t\tpublic static readonly int {tag};");
                }
            }

            // Generate value fields
            if (hasValues)
            {
                if (hasTags) sb.AppendLine();
                
                sb.AppendLine("\t\t///Values");
                foreach (var value in config.Values)
                {
                    string typeComment = IsBaseType(value.Type) ? string.Empty : $"// {value.Type}";
                    sb.AppendLine($"\t\tpublic static readonly int {value.Name}; {typeComment}");
                }
            }

            // Generate static constructor
            sb.AppendLine();
            sb.AppendLine($"\t\tstatic {config.ClassName}()");
            sb.AppendLine("\t\t{");
            
            // Initialize tags in static constructor
            if (hasTags)
            {
                sb.AppendLine("\t\t\t//Tags");
                foreach (var tag in config.Tags)
                {
                    sb.AppendLine($"\t\t\t{tag} = NameToId(nameof({tag}));");
                }
            }
            
            // Initialize values in static constructor
            if (hasValues)
            {
                if (hasTags) sb.AppendLine();
                
                sb.AppendLine("\t\t\t//Values");
                foreach (var value in config.Values)
                {
                    sb.AppendLine($"\t\t\t{value.Name} = NameToId(nameof({value.Name}));");
                }
            }
            
            sb.AppendLine("\t\t}");

            // Generate tag extensions
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

            // Generate value extensions
            if (hasValues)
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
            string typeName,
            string entity,
            bool useInlining,
            bool unsafeAccess
        )
        {
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