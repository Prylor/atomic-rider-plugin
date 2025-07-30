using ReSharperPlugin.AtomicPlugin.Model;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface IConfigurationMapper
    {
        AtomicEntityApiConfig MapToConfig(AtomicFileData fileData);
    }
}