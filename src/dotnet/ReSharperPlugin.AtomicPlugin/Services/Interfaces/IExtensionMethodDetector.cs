using JetBrains.ReSharper.Psi;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface IExtensionMethodDetector
    {
        bool IsGeneratedExtensionMethod(IMethod method, string valueName);
        bool IsGeneratedTagExtensionMethod(IMethod method, string tagName);
    }
}