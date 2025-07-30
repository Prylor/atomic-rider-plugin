using System.Collections.Generic;

namespace ReSharperPlugin.AtomicPlugin
{
    public class AtomicEntityApiConfig : EntityApiConfig
    {
        public List<string> Imports { get; set; } = new List<string>();
        public List<string> Tags { get; set; } = new List<string>();
        public List<EntityApiValue> Values { get; set; } = new List<EntityApiValue>();
    }
    
    public class EntityApiValue
    {
        public string Name { get; set; }
        public string Type { get; set; }
    }
}