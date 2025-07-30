using System.Threading;
using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.ReSharper.Feature.Services;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.TestFramework;
using JetBrains.TestFramework;
using JetBrains.TestFramework.Application.Zones;
using NUnit.Framework;

[assembly: Apartment(ApartmentState.STA)]

namespace ReSharperPlugin.AtomicPlugin.Tests
{
    [ZoneDefinition]
    public class AtomicPluginTestEnvironmentZone : ITestsEnvZone, IRequire<PsiFeatureTestZone>, IRequire<IAtomicPluginZone> { }

    [ZoneMarker]
    public class ZoneMarker : IRequire<ICodeEditingZone>, IRequire<ILanguageCSharpZone>, IRequire<AtomicPluginTestEnvironmentZone> { }

    [SetUpFixture]
    public class AtomicPluginTestsAssembly : ExtensionTestEnvironmentAssembly<AtomicPluginTestEnvironmentZone> { }
}
