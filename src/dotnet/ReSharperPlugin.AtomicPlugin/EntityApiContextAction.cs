using System;
using System.Linq;
using JetBrains.Application.Progress;
using JetBrains.DocumentManagers;
using JetBrains.DocumentManagers.Transactions;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.ContextActions;
using JetBrains.ReSharper.Feature.Services.CSharp.ContextActions;
using JetBrains.ReSharper.Feature.Services.Util;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.TextControl;
using JetBrains.Util;
using JetBrains.DocumentManagers.impl;

namespace ReSharperPlugin.AtomicPlugin
{
    [ContextAction(
        GroupType = typeof(CSharpContextActions),
        Name = "Generate Entity API",
        Description = "Generates Entity API code for interfaces marked with [EntityAPI] attribute",
        Priority = 0)]
    public class EntityApiContextAction(ICSharpContextActionDataProvider provider) : ContextActionBase
    {
        private IInterfaceDeclaration _interfaceDeclaration;

        public override string Text => "Generate Entity API";

        public override bool IsAvailable(IUserDataHolder cache)
        {
            _interfaceDeclaration = provider.GetSelectedElement<IInterfaceDeclaration>(true, true);
            if (_interfaceDeclaration == null)
                return false;

            var attributes = _interfaceDeclaration.Attributes;
            return attributes.Any(attr =>
            {
                var reference = attr.Name;
                var shortName = reference?.GetText();
                if (shortName is "EntityAPI" or "EntityAPIAttribute")
                    return true;

                var typeElement = reference?.Reference?.Resolve().DeclaredElement as ITypeElement;
                if (typeElement?.ShortName is "EntityAPIAttribute" or "EntityAPI")
                    return true;

                return false;
            });
        }

        protected override Action<ITextControl> ExecutePsiTransaction(ISolution solution, IProgressIndicator progress)
        {
            return textControl =>
            {
                using (ReadLockCookie.Create())
                {
                    var generator = new EntityApiCodeGenerator();
                    var config = ParseEntityApiAttribute(_interfaceDeclaration);
                    var generatedCode = generator.GenerateCode(_interfaceDeclaration, config);

                    if (!string.IsNullOrEmpty(generatedCode))
                    {
                        var sourceFile = _interfaceDeclaration.GetSourceFile();
                        var project = sourceFile?.GetProject();
                        if (project != null)
                        {
                            var outputPath = GetOutputPath(_interfaceDeclaration, config);
                            WriteGeneratedFile(solution, project, outputPath, generatedCode);
                        }
                    }
                }
            };
        }

        private EntityApiConfig ParseEntityApiAttribute(IInterfaceDeclaration interfaceDeclaration)
        {
            var config = new EntityApiConfig
            {
                InterfaceName = interfaceDeclaration.DeclaredName,
                Namespace = interfaceDeclaration.GetContainingNamespaceDeclaration()?.DeclaredName ?? "Generated",
                ClassName = interfaceDeclaration.DeclaredName + "Extensions",
                EntityType = "IEntity",
                AggressiveInlining = false,
                UnsafeAccess = false
            };

            var entityApiAttr = interfaceDeclaration.Attributes.FirstOrDefault(attr =>
            {
                var name = attr.Name?.GetText();
                return name is "EntityAPI" or "EntityAPIAttribute";
            });

            if (entityApiAttr != null)
            {
                foreach (var arg in entityApiAttr.PropertyAssignments)
                {
                    var propertyName = arg.PropertyNameIdentifier?.Name;
                    if (propertyName == null) continue;

                    switch (propertyName)
                    {
                        case "Header":
                            config.Header = GetStringValue(arg.Source);
                            break;
                        case "Namespace":
                            config.Namespace = GetStringValue(arg.Source) ?? config.Namespace;
                            break;
                        case "ClassName":
                            config.ClassName = GetStringValue(arg.Source) ?? config.ClassName;
                            break;
                        case "Directory":
                            config.Directory = GetStringValue(arg.Source);
                            break;
                        case "EntityType":
                            config.EntityType = GetStringValue(arg.Source) ?? config.EntityType;
                            break;
                        case "AggressiveInlining":
                            config.AggressiveInlining = GetBoolValue(arg.Source);
                            break;
                        case "UnsafeAccess":
                            config.UnsafeAccess = GetBoolValue(arg.Source);
                            break;
                    }
                }
            }

            return config;
        }

        private string GetStringValue(ICSharpExpression expression)
        {
            if (expression is ICSharpLiteralExpression literal &&
                literal.Literal?.GetTokenType().IsStringLiteral == true)
            {
                return literal.Literal.GetText().Trim('"');
            }

            return null;
        }

        private bool GetBoolValue(ICSharpExpression expression)
        {
            if (expression is ICSharpLiteralExpression literal)
            {
                return literal.Literal?.GetText() == "true";
            }

            return false;
        }

        private FileSystemPath GetOutputPath(IInterfaceDeclaration interfaceDeclaration, EntityApiConfig config)
        {
            var sourceFile = interfaceDeclaration.GetSourceFile();
            if (sourceFile == null)
                return FileSystemPath.Empty;

            string outputFileName = config.ClassName + ".cs";
            var sourceFilePath = sourceFile.GetLocation();
            var nativePath = sourceFilePath.ToNativeFileSystemPath();

            if (!string.IsNullOrEmpty(config.Directory))
            {
                var dirPath = FileSystemPath.TryParse(config.Directory);

                if (dirPath.IsAbsolute)
                {
                    return dirPath.Combine(outputFileName);
                }

                var currentPath = nativePath;
                while (currentPath != null && !currentPath.IsEmpty)
                {
                    if (currentPath.Name == "Assets")
                    {
                        var projectRoot = currentPath.Parent;
                        return projectRoot.Combine(config.Directory).Combine(outputFileName);
                    }

                    currentPath = currentPath.Parent;
                }

                var project = sourceFile.GetProject();
                if (project?.Location != null)
                {
                    var projectDir = project.Location.ToNativeFileSystemPath();
                    return projectDir.Directory.Combine(config.Directory).Combine(outputFileName);
                }
            }

            return nativePath.Parent.Combine(outputFileName);
        }

        private void WriteGeneratedFile(ISolution solution, IProject project, FileSystemPath outputPath, string content)
        {
            var normalizedContent = content.Replace("\r\n", "\n");

            using (var cookie = solution.CreateTransactionCookie(DefaultAction.Commit, "Generate Entity API",
                       NullProgressIndicator.Create()))
            {
                var existingProjectFile = project.GetAllProjectFiles()
                    .FirstOrDefault(pf => pf.Location.Equals(outputPath));

                if (existingProjectFile != null)
                {
                    var document = existingProjectFile.GetDocument();
                    if (document != null)
                    {
                        document.ReplaceText(document.DocumentRange, normalizedContent);
                    }
                }
                else
                {
                    var parentFolder = GetOrCreateProjectFolder(project, outputPath.Directory);
                    if (parentFolder != null)
                    {
                        AddNewItemHelper.AddFile(parentFolder, outputPath.Name, normalizedContent);
                    }
                }

                var psiServices = solution.GetPsiServices();
                psiServices.Files.CommitAllDocuments();
            }
        }

        private IProjectFolder GetOrCreateProjectFolder(IProject project, FileSystemPath directory)
        {
            var projectLocation = project.Location.ToNativeFileSystemPath();

            if (projectLocation.IsPrefixOf(directory))
            {
                var relativePath = directory.MakeRelativeTo(projectLocation);
                var folders = relativePath.Components.ToArray();

                IProjectFolder currentFolder = project;
                foreach (var folderName in folders)
                {
                    if (string.IsNullOrEmpty(folderName.ToString()) || folderName == ".")
                        continue;

                    var path = currentFolder.Location.Combine(folderName.ToString());
                    currentFolder = currentFolder.GetOrCreateProjectFolder(path);

                    if (currentFolder == null)
                        return null;
                }

                return currentFolder;
            }

            var existingFolder = project.GetSubFolders()
                .FirstOrDefault(f => f.Location.Equals(directory.Parent));

            if (existingFolder != null)
                return existingFolder;

            return project;
        }
    }
}