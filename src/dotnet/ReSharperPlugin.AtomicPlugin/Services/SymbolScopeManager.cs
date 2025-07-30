using System;
using JetBrains.Application.Threading;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Caches;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public class SymbolScopeManager : ISymbolScopeManager
    {
        private readonly ISolution _solution;

        public SymbolScopeManager(ISolution solution)
        {
            _solution = solution;
        }

        public ISymbolScope GetSymbolScope(LibrarySymbolScope scope, bool caseSensitive)
        {
            var psiServices = _solution.GetPsiServices();
            var symbolCache = psiServices.Symbols;
            return symbolCache.GetSymbolScope(scope, caseSensitive);
        }

        public void ExecuteWithReadLock(Action action)
        {
            var psiServices = _solution.GetPsiServices();
            using (psiServices.Locks.UsingReadLock())
            {
                action();
            }
        }

        public T ExecuteWithReadLock<T>(Func<T> func)
        {
            var psiServices = _solution.GetPsiServices();
            using (psiServices.Locks.UsingReadLock())
            {
                return func();
            }
        }
    }
}