using System;
using System.Threading.Tasks;
using JetBrains.ProjectModel;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface ITransactionManager
    {
        Task ExecuteTransactionAsync(string transactionName, Func<ISolution, Task> action);
        void ExecuteTransaction(string transactionName, Action<ISolution> action);
        void ExecuteWithWriteLock(Action action);
    }
}