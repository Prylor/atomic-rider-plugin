namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class HashCodeGenerator : IHashCodeGenerator
    {
        public int GetHashCode(string value)
        {
            unchecked
            {
                int hash = 17;
                foreach (char c in value)
                {
                    hash = hash * 31 + c;
                }
                return hash;
            }
        }
    }
}