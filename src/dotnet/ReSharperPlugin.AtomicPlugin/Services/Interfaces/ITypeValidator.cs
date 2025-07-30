using System.Threading.Tasks;
using ReSharperPlugin.AtomicPlugin.Model;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface ITypeValidator
    {
        Task<TypeValidationResponse> ValidateAsync(TypeValidationRequest request);
    }
}