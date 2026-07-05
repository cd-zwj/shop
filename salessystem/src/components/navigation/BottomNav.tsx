import { Bot, ShoppingCart, Store, User, Wallet } from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import { cn } from '../../lib/utils';

export function BottomNav() {
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { icon: Store, label: '首页', path: '/' },
    { icon: Bot, label: 'AI', path: '/ai' },
    { icon: ShoppingCart, label: '购物车', path: '/cart', badge: true },
    { icon: Wallet, label: '钱包', path: '/wallet' },
    { icon: User, label: '我的', path: '/profile' },
  ];

  if (
    location.pathname.startsWith('/admin')
    || location.pathname.startsWith('/merchant')
    || location.pathname === '/login'
    || location.pathname === '/reset-password'
  ) {
    return null;
  }

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 h-20 bg-white/95 backdrop-blur-md border-t border-slate-200 md:hidden flex justify-around items-center px-2 pb-safe">
      {navItems.map((item) => {
        const isActive = location.pathname === item.path;
        return (
          <button
            key={item.path}
            onClick={() => navigate(item.path)}
            className={cn(
              'flex flex-col items-center justify-center w-16 transition-all duration-200',
              isActive ? 'text-primary scale-110' : 'text-slate-500 scale-100',
            )}
          >
            <div className={cn('p-2 rounded-2xl relative', isActive && 'bg-primary/10')}>
              <item.icon className={cn('w-6 h-6', isActive && 'fill-current')} />
              {item.badge && (
                <span className="absolute top-1 right-1 w-2 h-2 bg-error rounded-full" />
              )}
            </div>
            <span className="text-[10px] font-medium mt-1">{item.label}</span>
          </button>
        );
      })}
    </nav>
  );
}
