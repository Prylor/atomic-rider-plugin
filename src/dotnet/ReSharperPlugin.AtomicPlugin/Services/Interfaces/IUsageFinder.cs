using System.Threading.Tasks;
using ReSharperPlugin.AtomicPlugin.Model;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface IUsageFinder
    {
        Task<FindMethodUsagesResponse> FindMethodUsagesAsync(FindMethodUsagesRequest request);
        Task<FindTagUsagesResponse> FindTagUsagesAsync(FindTagUsagesRequest request);
    }
}