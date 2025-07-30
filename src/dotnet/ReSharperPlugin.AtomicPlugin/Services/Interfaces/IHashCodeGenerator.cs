namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface IHashCodeGenerator
    {
        int GetHashCode(string value);
    }
}