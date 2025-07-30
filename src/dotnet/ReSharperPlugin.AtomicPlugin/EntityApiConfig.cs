namespace ReSharperPlugin.AtomicPlugin
{
    public class EntityApiConfig
    {
        public string InterfaceName { get; set; }
        public string Header { get; set; }
        public string Namespace { get; set; }
        public string ClassName { get; set; }
        public string Directory { get; set; }
        public string Solution { get; set; }
        public string EntityType { get; set; } = "IEntity";
        public bool AggressiveInlining { get; set; }
        public bool UnsafeAccess { get; set; }
    }
}