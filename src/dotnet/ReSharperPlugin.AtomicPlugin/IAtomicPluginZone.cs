using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;

namespace ReSharperPlugin.AtomicPlugin
{
    [ZoneDefinition]
    [ZoneDefinitionConfigurableFeature("Entity API Generator", "Provides code generation for Entity API interfaces", IsInProductSection: false)]
    public interface IAtomicPluginZone : IPsiLanguageZone,
        IRequire<ILanguageCSharpZone>
    {
    }
}
