using System;
using System.Threading.Tasks;
using JetBrains.Application.Progress;
using JetBrains.Application.Threading;
using JetBrains.DocumentManagers.Transactions;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Util;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class TransactionManager : ITransactionManager
    {
        private readonly ISolution _solution;

        public TransactionManager(ISolution solution)
        {
            _solution = solution;
        }

        public async Task ExecuteTransactionAsync(string transactionName, Func<ISolution, Task> action)
        {
            var tcs = new TaskCompletionSource<bool>();
            
            _solution.Locks.Queue(Lifetime.Eternal, transactionName, () =>
            {
                try
                {
                    using (WriteLockCookie.Create())
                    {
                        using (var cookie = _solution.CreateTransactionCookie(DefaultAction.Commit, transactionName,
                                   NullProgressIndicator.Create()))
                        {
                            
                            
                            action(_solution).GetAwaiter().GetResult();
                        }
                    }
                    tcs.SetResult(true);
                }
                catch (Exception ex)
                {
                    tcs.SetException(ex);
                }
            });
            
            await tcs.Task;
        }

        public void ExecuteTransaction(string transactionName, Action<ISolution> action)
        {
            using (WriteLockCookie.Create())
            {
                using (var cookie = _solution.CreateTransactionCookie(DefaultAction.Commit, transactionName,
                           NullProgressIndicator.Create()))
                {
                    action(_solution);
                }
            }
        }

        public void ExecuteWithWriteLock(Action action)
        {
            using (WriteLockCookie.Create())
            {
                action();
            }
        }
    }
}