using System.Linq;
using JetBrains.Util;
using ReSharperPlugin.AtomicPlugin.Model;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class ConfigurationMapper : IConfigurationMapper
    {
        private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger<ConfigurationMapper>();

        public AtomicEntityApiConfig MapToConfig(AtomicFileData fileData)
        {
            var config = new AtomicEntityApiConfig();
            
            
            foreach (var prop in fileData.HeaderProperties)
            {
                switch (prop.Key.ToLower())
                {
                    case "entitytype":
                        config.EntityType = prop.Value;
                        break;
                    case "aggressiveinlining":
                        config.AggressiveInlining = bool.Parse(prop.Value);
                        break;
                    case "unsafe":
                        config.UnsafeAccess = bool.Parse(prop.Value);
                        break;
                    case "namespace":
                        config.Namespace = prop.Value;
                        break;
                    case "classname":
                        config.ClassName = prop.Value;
                        break;
                    case "directory":
                        config.Directory = prop.Value;
                        break;
                    case "solution":
                        config.Solution = prop.Value;
                        break;
                }
            }
            
            
            config.Imports = fileData.Imports.ToList();
            
            
            config.Tags = fileData.Tags.ToList();
            
            
            config.Values = fileData.Values
                .Select(v => 
                {
                    Logger.Info($"[ConfigurationMapper] Value: name='{v.Name}', type='{v.Type}'");
                    return new EntityApiValue { Name = v.Name, Type = v.Type };
                })
                .ToList();
            
            
            if (string.IsNullOrEmpty(config.ClassName))
            {
                config.ClassName = "AtomicExtensions";
            }
            if (string.IsNullOrEmpty(config.Namespace))
            {
                config.Namespace = "Generated";
            }
            
            return config;
        }
    }
}