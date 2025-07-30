using System.Collections.Generic;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface ICodeGenerator
    {
        string GenerateCode(AtomicEntityApiConfig config);
    }
}