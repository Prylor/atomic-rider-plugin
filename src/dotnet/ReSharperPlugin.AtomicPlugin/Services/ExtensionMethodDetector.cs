using JetBrains.ReSharper.Psi;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class ExtensionMethodDetector : IExtensionMethodDetector
    {
        public bool IsGeneratedExtensionMethod(IMethod method, string valueName)
        {
            
            if (!method.IsExtensionMethod) return false;
            
            
            var methodName = method.ShortName;
            var expectedPrefixes = new[] { "Get", "Set", "Add", "Has", "Del", "TryGet", "Ref" };
            
            foreach (var prefix in expectedPrefixes)
            {
                if (methodName == $"{prefix}{valueName}")
                {
                    return true;
                }
            }
            
            return false;
        }

        public bool IsGeneratedTagExtensionMethod(IMethod method, string tagName)
        {
            
            if (!method.IsExtensionMethod) return false;
            
            
            var methodName = method.ShortName;
            var expectedSuffixes = new[] { "Tag" };
            var expectedPrefixes = new[] { "Has", "Add", "Del" };
            
            foreach (var prefix in expectedPrefixes)
            {
                foreach (var suffix in expectedSuffixes)
                {
                    if (methodName == $"{prefix}{tagName}{suffix}")
                    {
                        return true;
                    }
                }
            }
            
            return false;
        }
    }
}