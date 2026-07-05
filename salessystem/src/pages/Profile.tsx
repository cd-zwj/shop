import type { ComponentProps } from 'react';
import {
  ChevronRight,
  Clock,
  LogOut,
  MapPin,
  Package,
  Shield,
  ShoppingBag,
  Wallet,
  Bell,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { cn } from '../lib/utils';

export default function Profile() {
  const navigate = useNavigate();
  const { currentUser, logout } = useAuth();
  const { showToast } = useToast();

  const menuGroups = [
    {
      title: '交易与资产',
      items: [
        { icon: ShoppingBag, label: '我的订单', desc: '查看订单状态与物流', path: '/orders' },
        { icon: Package, label: '我的已购', desc: '虚拟商品/卡密/服务交付明细', path: '/my-purchases' },
        { icon: Wallet, label: '我的钱包', desc: '查看余额、积分与充值', path: '/wallet' },
        { icon: Clock, label: '消费明细', desc: '历史交易流水记录', path: '/history' },
      ],
    },
    {
      title: '账户设置',
      items: [
        { icon: MapPin, label: '收货地址', desc: '管理配送地址', path: '/addresses' },
        { icon: Shield, label: '账号安全', desc: '修改密码、实名认证', path: '/account-security' },
        { icon: BellIcon, label: '消息通知', desc: '系统动态与福利提醒', path: '/notifications' },
      ],
    },
  ] as const;

  const displayName = currentUser?.username || '访客用户';
  const accountId = currentUser?.id ? `账户 ID: ${currentUser.id}` : '账户信息待同步';
  const email = currentUser?.email || '未绑定邮箱';

  async function handleLogout() {
    await logout();
    navigate('/login');
  }

  return (
    <div className="flex flex-col gap-6 pb-20">
      <section className="relative overflow-hidden bg-slate-900 px-6 pb-24 pt-12">
        <div className="absolute right-0 top-0 -mr-32 -mt-32 h-64 w-64 rounded-full bg-primary/20 blur-[100px]" />
        <div className="relative z-10 flex items-center gap-6">
          <div className="flex h-20 w-20 items-center justify-center rounded-3xl border-4 border-white/10 bg-white/10 text-2xl font-black text-white shadow-2xl">
            {displayName.slice(0, 1).toUpperCase()}
          </div>
          <div className="flex flex-col gap-1">
            <h1 className="text-2xl font-black text-white">{displayName}</h1>
            <p className="text-sm font-medium text-slate-400">{accountId}</p>
            <p className="text-xs font-medium text-slate-500">{email}</p>
          </div>
        </div>
      </section>

      <section className="relative z-20 -mt-12 px-4">
        <div className="grid grid-cols-3 divide-x divide-slate-100 rounded-3xl border border-slate-100 bg-white p-6 shadow-xl shadow-slate-200/50">
          <div className="flex flex-col items-center gap-0.5" onClick={() => navigate('/wallet')}>
            <span className="text-xl font-black text-slate-900">实时</span>
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">钱包数据</span>
          </div>
          <div className="flex flex-col items-center gap-0.5">
            <span className="text-xl font-black text-slate-900">{currentUser?.phone || '--'}</span>
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">绑定手机</span>
          </div>
          <div className="flex flex-col items-center gap-0.5">
            <span className="text-xl font-black text-slate-900">
              {currentUser?.status !== undefined && currentUser?.status !== null
                ? currentUser.status === 1
                  ? '正常'
                  : '异常'
                : '--'}
            </span>
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">账号状态</span>
          </div>
        </div>
      </section>

      <div className="mt-4 flex flex-col gap-8 px-4">
        {menuGroups.map((group) => (
          <section key={group.title} className="flex flex-col gap-4">
            <h3 className="ml-2 text-[11px] font-black uppercase tracking-[0.2em] text-slate-400">{group.title}</h3>
            <div className="flex flex-col overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-sm">
              {group.items.map((item) => (
                <button
                  key={item.label}
                  onClick={() => {
                    if (item.path === '#') {
                      showToast('该功能即将上线，敬请期待！', 'info');
                    } else {
                      navigate(item.path);
                    }
                  }}
                  className={cn(
                    'group flex items-center gap-4 border-b border-slate-50 p-5 text-left transition-colors last:border-0 hover:bg-slate-50',
                    item.path === '#' && 'cursor-not-allowed opacity-50',
                  )}
                >
                  <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-slate-50 text-slate-400 transition-all group-hover:bg-primary/5 group-hover:text-primary">
                    <item.icon className="h-5 w-5" />
                  </div>
                  <div className="flex flex-1 flex-col">
                    <span className="text-[15px] font-black tracking-tight text-slate-900">{item.label}</span>
                    <span className="text-xs font-medium text-slate-400">{item.desc}</span>
                  </div>
                  <ChevronRight className="h-4 w-4 text-slate-300 transition-colors group-hover:text-slate-900" />
                </button>
              ))}
            </div>
          </section>
        ))}

        <button
          onClick={handleLogout}
          className="mb-10 flex w-full items-center justify-center gap-3 rounded-3xl bg-red-50 py-5 text-sm font-black uppercase tracking-widest text-red-500 transition-all hover:bg-red-100"
        >
          <LogOut size={18} /> 退出登录
        </button>
      </div>
    </div>
  );
}

function BellIcon(props: ComponentProps<typeof Bell>) {
  return <Bell {...props} />;
}
