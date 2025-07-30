using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.ReSharper.Feature.Services;
using JetBrains.ReSharper.Psi.CSharp;

namespace ReSharperPlugin.AtomicPlugin
{
    [ZoneMarker]
    public class ZoneMarker : IRequire<IAtomicPluginZone>, IRequire<ICodeEditingZone>, IRequire<ILanguageCSharpZone>
    {
    }
}