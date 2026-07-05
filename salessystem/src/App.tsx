import { BrowserRouter as Router, useLocation } from 'react-router-dom';
import { ErrorBoundary } from './components/ErrorBoundary';
import { BottomNav } from './components/navigation/BottomNav';
import { Sidebar } from './components/navigation/Sidebar';
import { TopNav } from './components/navigation/TopNav';
import { ToastProvider } from './context/ToastContext';
import { cn } from './lib/utils';
import { AppRoutes } from './routes/AppRoutes';

function AppContent() {
  const location = useLocation();
  const isAdmin = location.pathname.startsWith('/admin');
  const isMerchant = location.pathname.startsWith('/merchant');
  const isLogin = location.pathname === '/login';
  const isResetPassword = location.pathname === '/reset-password';

  return (
    <div className="min-h-screen bg-surface">
      <TopNav title="SalesSystem" />
      <Sidebar />

      <main className={cn(
        'transition-all duration-300',
        (isAdmin || isMerchant) ? 'pt-16 md:pl-64' : (isLogin || isResetPassword) ? 'p-0' : 'pt-16 pb-20 md:pb-0',
      )}>
        <div className="max-w-7xl mx-auto w-full h-full">
          <ErrorBoundary>
            <AppRoutes />
          </ErrorBoundary>
        </div>
      </main>

      <BottomNav />
    </div>
  );
}

export default function App() {
  return (
    <ErrorBoundary variant="fullscreen">
      <ToastProvider>
        <Router>
          <AppContent />
        </Router>
      </ToastProvider>
    </ErrorBoundary>
  );
}
