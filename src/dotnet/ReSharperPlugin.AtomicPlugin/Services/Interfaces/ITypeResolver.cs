using System.Threading.Tasks;
using ReSharperPlugin.AtomicPlugin.Model;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface ITypeResolver
    {
        Task<TypeCompletionResponse> GetCompletionsAsync(TypeCompletionRequest request);
    }
}