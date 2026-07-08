import { AnimatePresence, motion } from 'motion/react';
import {
  ArrowUpRight,
  Bot,
  CreditCard,
  Database,
  FolderTree,
  HeartHandshake,
  LayoutDashboard,
  LineChart,
  LogOut,
  Menu,
  Package,
  Receipt,
  Settings2,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
  Store,
  Ticket,
  Users,
  Wallet,
  X,
} from 'lucide-react';
import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { cn } from '../../lib/utils';
import { filterMerchantPermissionItems, type MerchantPermission } from '../../utils/merchantPermissions';

export function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout, merchantSession } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const isAdmin = location.pathname.startsWith('/admin');
  const isMerchant = location.pathname.startsWith('/merchant');

  const adminMenu = [
    { icon: LayoutDashboard, label: '工作仪表盘', path: '/admin' },
    { icon: Users, label: '商户入驻', path: '/admin/merchants' },
    { icon: Package, label: '全量商品库', path: '/admin/products' },
    { icon: LineChart, label: '全盘分析', path: '/admin/analytics' },
    { icon: Receipt, label: '流水审计', path: '/admin/transactions' },
    { icon: CreditCard, label: '支付账单', path: '/admin/payments' },
    { icon: Wallet, label: '充值监管', path: '/admin/recharges' },
    { icon: ShieldCheck, label: '用户安全治理', path: '/admin/users' },
    { icon: ArrowUpRight, label: '提现审批中心', path: '/admin/withdrawals' },
    { icon: Sparkles, label: '平台营销运营', path: '/admin/marketing' },
    { icon: Database, label: '知识库管理', path: '/admin/documents' },
    { icon: Bot, label: 'AI 治理助手', path: '/admin/ai' },
  ];

  const merchantMenu: Array<{
    icon: typeof LayoutDashboard;
    label: string;
    path: string;
    permission: MerchantPermission;
  }> = [
    { icon: LayoutDashboard, label: '商户工作台', path: '/merchant', permission: 'dashboard:view' },
    { icon: Store, label: '门店管理', path: '/merchant/stores', permission: 'store:manage' },
    { icon: Package, label: '我的商品', path: '/merchant/products', permission: 'product:manage' },
    { icon: FolderTree, label: '商品形态字典', path: '/merchant/product-taxonomy', permission: 'product:manage' },
    { icon: ShoppingBag, label: '订单管理', path: '/merchant/orders', permission: 'order:manage' },
    { icon: Wallet, label: '财务结算', path: '/merchant/finance', permission: 'finance:view' },
    { icon: Ticket, label: '优惠券管理', path: '/merchant/marketing/coupons', permission: 'marketing:manage' },
    { icon: Sparkles, label: '促销活动管理', path: '/merchant/marketing/activities', permission: 'marketing:manage' },
    { icon: Users, label: '会员等级标签', path: '/merchant/marketing/members', permission: 'marketing:manage' },
    { icon: HeartHandshake, label: '售后退款审核', path: '/merchant/refunds', permission: 'refund:manage' },
    { icon: Settings2, label: '规则配置', path: '/merchant/rules', permission: 'rule:manage' },
    { icon: ShieldCheck, label: '员工与权限', path: '/merchant/employees', permission: 'employee:manage' },
    { icon: ArrowUpRight, label: '提现中心', path: '/merchant/withdrawals', permission: 'withdrawal:manage' },
    { icon: Bot, label: 'AI 经营助手', path: '/merchant/ai', permission: 'ai:use' },
  ];

  if (!isAdmin && !isMerchant) return null;

  const currentMenu = isAdmin
    ? adminMenu
    : filterMerchantPermissionItems(merchantSession?.employeeRole, merchantMenu);
  const title = isAdmin ? '管理控制台' : '商户中心';
  const subtitle = isAdmin ? '全局治理' : '店铺管理';

  async function handleLogout() {
    await logout();
    setIsOpen(false);
    navigate('/login', { replace: true });
  }

  return (
    <>
      <div className="fixed top-4 left-4 z-[60] md:hidden">
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="p-3 bg-slate-900 text-white rounded-2xl shadow-xl border border-white/10"
        >
          {isOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </div>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setIsOpen(false)}
            className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-[55] md:hidden"
          />
        )}
      </AnimatePresence>

      <aside className={cn(
        'fixed left-0 top-0 bottom-0 w-64 bg-slate-900 border-r border-white/5 flex flex-col z-[58] py-4 text-white transition-transform duration-300 md:translate-x-0',
        isOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0',
      )}>
        <div className="px-6 mb-8 flex flex-col gap-2 mt-16 md:mt-0">
          <h1 className="text-2xl font-black text-primary">{title}</h1>
          <p className="text-[10px] text-slate-500 font-black tracking-[0.2em] uppercase">{subtitle}</p>
        </div>

        <div className="flex-1 px-4 space-y-1.5 overflow-y-auto no-scrollbar">
          {currentMenu.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <button
                key={item.path}
                onClick={() => {
                  navigate(item.path);
                  setIsOpen(false);
                }}
                className={cn(
                  'w-full flex items-center px-4 py-4 gap-3 rounded-2xl transition-all text-sm font-black tracking-tight',
                  isActive
                    ? 'bg-primary text-white shadow-xl shadow-primary/20 scale-[1.02]'
                    : 'text-slate-400 hover:bg-white/5 hover:text-white',
                )}
              >
                <item.icon className="w-5 h-5 transition-transform group-active:scale-90" />
                {item.label}
              </button>
            );
          })}
        </div>

        <div className="mt-auto px-4 pt-4 border-t border-white/5 space-y-1">
          <button onClick={handleLogout} className="w-full flex items-center px-4 py-3 gap-3 rounded-xl text-slate-400 hover:bg-white/5 transition-all text-sm font-bold">
            <LogOut className="w-5 h-5" /> 退出登录
          </button>
        </div>
      </aside>
    </>
  );
}
