import { ArrowLeft, Bell, LogOut, Search } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export function TopNav({ title }: { title: string }) {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout } = useAuth();
  const isAdmin = location.pathname.startsWith('/admin');
  const isMerchant = location.pathname.startsWith('/merchant');
  const isLogin = location.pathname === '/login';
  const isResetPassword = location.pathname === '/reset-password';

  if (isLogin || isResetPassword) return null;

  async function handleAuthAction() {
    if (isAdmin || isMerchant) {
      await logout();
      navigate('/login', { replace: true });
      return;
    }
    navigate('/login');
  }

  return (
    <header className="fixed top-0 left-0 right-0 z-50 h-16 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 shadow-sm flex items-center justify-between px-4">
      <div className="flex items-center gap-3">
        {location.pathname !== '/' && !isAdmin && !isMerchant && (
          <button onClick={() => navigate(-1)} className="p-2 hover:bg-slate-100 rounded-full md:hidden">
            <ArrowLeft className="w-5 h-5" />
          </button>
        )}
        <div className="flex items-center gap-2 cursor-pointer" onClick={() => navigate('/')}>
          <div className="w-8 h-8 rounded-full overflow-hidden bg-primary/10 flex items-center justify-center">
            <img
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuCySrAMILh3_qitDxUnnGkFdVjlC7RSMxnc_vNOzqFQn_3SXd-vJmW2eNvhTwNC53M5Z97LnmR8Jg0_78KnJ3j5KxI91h67Uc75gBtZqkqlgPusvW5OOUPUu4uet5Hyi4GjlU83UDHd5eu0YiGKtyxJj2qZQHg37tsVOG9jWNY1jsF_KYrghL9ljOD1NsO5ZnDhtHDZs7W1IRNZTT1mY7S-3dDivoWNzXtRLYONLeQcSlSSj9al58KVvBJ_DI8FvQHnbYPGp6KmPbo"
              alt="Avatar"
              className="w-full h-full object-cover"
            />
          </div>
          <span className="text-xl font-black tracking-tight text-primary">{title}</span>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={handleAuthAction}
          className="p-2 text-primary hover:bg-primary/5 rounded-xl flex items-center gap-1 transition-all mr-2"
        >
          <LogOut className="w-5 h-5" />
          <span className="text-[10px] font-black uppercase hidden sm:inline">
            {isAdmin || isMerchant ? '退出登录' : '切换后台'}
          </span>
        </button>
        <button className="p-2 text-slate-600 hover:bg-slate-50 rounded-full relative">
          <Search className="w-5 h-5 transition-opacity" />
        </button>
        <button className="p-2 text-slate-600 hover:bg-slate-50 rounded-full relative">
          <Bell className="w-5 h-5" />
          <span className="absolute top-2 right-2 w-2 h-2 bg-error rounded-full border border-white" />
        </button>
      </div>
    </header>
  );
}
