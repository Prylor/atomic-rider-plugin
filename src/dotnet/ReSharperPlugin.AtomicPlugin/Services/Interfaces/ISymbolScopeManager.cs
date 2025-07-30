using System;
using JetBrains.ReSharper.Psi.Caches;

namespace ReSharperPlugin.AtomicPlugin.Services
{
    public interface ISymbolScopeManager
    {
        ISymbolScope GetSymbolScope(LibrarySymbolScope scope, bool caseSensitive);
        void ExecuteWithReadLock(Action action);
        T ExecuteWithReadLock<T>(Func<T> func);
    }
}