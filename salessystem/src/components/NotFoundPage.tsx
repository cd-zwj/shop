import { Home, LogIn } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function NotFoundPage() {
  const navigate = useNavigate();
  const { currentRole } = useAuth();
  const loggedIn = Boolean(currentRole);

  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center gap-6 px-6 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-100 text-slate-500">
        {loggedIn ? <Home className="h-8 w-8" /> : <LogIn className="h-8 w-8" />}
      </div>
      <div>
        <h1 className="text-2xl font-black text-slate-900">页面不存在</h1>
        <p className="mt-2 max-w-md text-sm font-medium leading-6 text-slate-500">
          当前地址没有可用页面，请返回可访问的入口继续操作。
        </p>
      </div>
      <button
        type="button"
        onClick={() => navigate(loggedIn ? '/' : '/login', { replace: true })}
        className="rounded-2xl bg-primary px-6 py-3 text-sm font-black text-white shadow-xl shadow-primary/20 transition-all hover:scale-105 active:scale-95"
      >
        {loggedIn ? '返回首页' : '返回登录'}
      </button>
    </div>
  );
}
