using System.Threading.Tasks;
using ReSharperPlugin.AtomicPlugin.Model;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface INamespaceResolver
    {
        Task<NamespaceCompletionResponse> GetCompletionsAsync(NamespaceCompletionRequest request);
        Task<NamespaceValidationResponse> ValidateAsync(NamespaceValidationRequest request);
    }
}