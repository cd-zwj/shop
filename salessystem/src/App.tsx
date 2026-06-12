import { BrowserRouter as Router, Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Store, 
  ShoppingCart, 
  Bot, 
  Wallet, 
  User, 
  Bell, 
  Search, 
  LayoutDashboard, 
  ShieldCheck, 
  ArrowLeft,
  ChevronRight,
  Plus,
  ArrowUpRight,
  CreditCard,
  History,
  Receipt,
  LogOut,
  HelpCircle,
  Menu,
  X,
  LineChart,
  Package,
  Users,
  ShoppingBag,
  Settings2,
  Sparkles,
  Ticket,
  HeartHandshake
} from 'lucide-react';
import { cn } from './lib/utils';
import { ErrorBoundary } from './components/ErrorBoundary';

import Home from './pages/Home';
import Discovery from './pages/Discovery';
import ProductDetails from './pages/ProductDetails';
import Cart from './pages/Cart';
import UserWallet from './pages/Wallet';
import Recharge from './pages/Recharge';
import ConsumptionHistory from './pages/History';
import AIAssistant from './pages/AIAssistant';
import AdminDashboard from './pages/AdminDashboard';
import AdminMerchants from './pages/AdminMerchants';
import AdminProducts from './pages/AdminProducts';
import AdminAnalytics from './pages/AdminAnalytics';
import AdminUsers from './pages/AdminUsers';
import AdminUserDetail from './pages/AdminUserDetail';
import AdminUserPermissions from './pages/AdminUserPermissions';
import AdminWithdrawals from './pages/AdminWithdrawals';
import AdminTransactions from './pages/AdminTransactions';
import AdminPayments from './pages/AdminPayments';
import AdminRecharges from './pages/AdminRecharges';
import AdminMerchantDetail from './pages/AdminMerchantDetail';
import AdminMerchantEdit from './pages/AdminMerchantEdit';
import AdminPermissions from './pages/AdminPermissions';
import AdminOrderDetail from './pages/AdminOrderDetail';
import MerchantDashboard from './pages/merchant/MerchantDashboard';
import MerchantOrders from './pages/merchant/MerchantOrders';
import MerchantOrderDetail from './pages/merchant/MerchantOrderDetail';
import MerchantFinance from './pages/merchant/MerchantFinance';
import MerchantProducts from './pages/merchant/MerchantProducts';
import MerchantProductDetail from './pages/merchant/MerchantProductDetail';
import MerchantProductEdit from './pages/merchant/MerchantProductEdit';
import MerchantRules from './pages/merchant/MerchantRules';
import MerchantWithdraw from './pages/merchant/MerchantWithdraw';
import Success from './pages/Success';
import Login from './pages/Login';
import Register from './pages/Register';

import Profile from './pages/Profile';
import UserOrders from './pages/UserOrders';
import UserOrderDetail from './pages/UserOrderDetail';
import PaymentStatus from './pages/PaymentStatus';
import PublicMerchantDetail from './pages/PublicMerchantDetail';
import AuthGuard from './components/guards/AuthGuard';
import RoleGuard from './components/guards/RoleGuard';
import GuestGuard from './components/guards/GuestGuard';
import CouponCenter from './pages/CouponCenter';
import Points from './pages/Points';
import GrowthCenter from './pages/GrowthCenter';
import { ToastProvider } from './context/ToastContext';

import ApplyRefund from './pages/ApplyRefund';
import AddressList from './pages/AddressList';
import Notifications from './pages/Notifications';
import MerchantRefunds from './pages/merchant/MerchantRefunds';
import MerchantCoupons from './pages/merchant/MerchantCoupons';
import MerchantActivities from './pages/merchant/MerchantActivities';
import MerchantMembers from './pages/merchant/MerchantMembers';
import AdminMarketing from './pages/AdminMarketing';
import AccountSecurity from './pages/AccountSecurity';

// --- Components ---

const TopNav = ({ title }: { title: string }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const isAdmin = location.pathname.startsWith('/admin');
  const isMerchant = location.pathname.startsWith('/merchant');
  const isLogin = location.pathname === '/login';

  if (isLogin) return null;

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
          onClick={() => navigate('/login')}
          className="p-2 text-primary hover:bg-primary/5 rounded-xl flex items-center gap-1 transition-all mr-2"
        >
          <LogOut className="w-5 h-5" />
          <span className="text-[10px] font-black uppercase hidden sm:inline">切换后台</span>
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
};

const BottomNav = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { icon: Store, label: '首页', path: '/' },
    { icon: ShoppingCart, label: '购物车', path: '/cart', badge: true },
    { icon: Wallet, label: '钱包', path: '/wallet' },
    { icon: User, label: '我的', path: '/profile' },
  ];

  if (location.pathname.startsWith('/admin') || location.pathname.startsWith('/merchant') || location.pathname === '/login') return null;

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 h-20 bg-white/95 backdrop-blur-md border-t border-slate-200 md:hidden flex justify-around items-center px-2 pb-safe">
      {navItems.map((item) => {
        const isActive = location.pathname === item.path;
        return (
          <button
            key={item.path}
            onClick={() => navigate(item.path)}
            className={cn(
              "flex flex-col items-center justify-center w-16 transition-all duration-200",
              isActive ? "text-primary scale-110" : "text-slate-500 scale-100"
            )}
          >
            <div className={cn(
              "p-2 rounded-2xl relative",
              isActive && "bg-primary/10"
            )}>
              <item.icon className={cn("w-6 h-6", isActive && "fill-current")} />
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
};

const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();
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
  ];

  const merchantMenu = [
    { icon: LayoutDashboard, label: '商户工作台', path: '/merchant' },
    { icon: Package, label: '我的商品', path: '/merchant/products' },
    { icon: ShoppingBag, label: '订单管理', path: '/merchant/orders' },
    { icon: Wallet, label: '财务结算', path: '/merchant/finance' },
    { icon: Ticket, label: '优惠券管理', path: '/merchant/marketing/coupons' },
    { icon: Sparkles, label: '促销活动管理', path: '/merchant/marketing/activities' },
    { icon: Users, label: '会员等级标签', path: '/merchant/marketing/members' },
    { icon: HeartHandshake, label: '售后退款审核', path: '/merchant/refunds' },
    { icon: Settings2, label: '规则配置', path: '/merchant/rules' },
    { icon: ArrowUpRight, label: '提现中心', path: '/merchant/withdrawals' },
  ];

  if (!isAdmin && !isMerchant) return null;

  const currentMenu = isAdmin ? adminMenu : merchantMenu;
  const title = isAdmin ? '管理控制台' : '商户中心';
  const subtitle = isAdmin ? '全局治理' : '店铺管理';

  return (
    <>
      {/* Mobile Toggle */}
      <div className="fixed top-4 left-4 z-[60] md:hidden">
        <button 
          onClick={() => setIsOpen(!isOpen)}
          className="p-3 bg-slate-900 text-white rounded-2xl shadow-xl border border-white/10"
        >
          {isOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </div>

      {/* Backdrop */}
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
        "fixed left-0 top-0 bottom-0 w-64 bg-slate-900 border-r border-white/5 flex flex-col z-[58] py-4 text-white transition-transform duration-300 md:translate-x-0",
        isOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
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
                  "w-full flex items-center px-4 py-4 gap-3 rounded-2xl transition-all text-sm font-black tracking-tight",
                  isActive 
                    ? "bg-primary text-white shadow-xl shadow-primary/20 scale-[1.02]" 
                    : "text-slate-400 hover:bg-white/5 hover:text-white"
                )}
              >
                <item.icon className="w-5 h-5 transition-transform group-active:scale-90" />
                {item.label}
              </button>
            );
          })}
        </div>

        <div className="mt-auto px-4 pt-4 border-t border-white/5 space-y-1">
          <button onClick={() => navigate('/login')} className="w-full flex items-center px-4 py-3 gap-3 rounded-xl text-slate-400 hover:bg-white/5 transition-all text-sm font-bold">
            <LogOut className="w-5 h-5" /> 切换角色
          </button>
        </div>
      </aside>
    </>
  );
};

function AppContent() {
  const location = useLocation();
  const isAdmin = location.pathname.startsWith('/admin');
  const isMerchant = location.pathname.startsWith('/merchant');
  const isLogin = location.pathname === '/login';

  return (
    <div className="min-h-screen bg-surface">
      <TopNav title="SalesSystem" />
      <Sidebar />
      
      <main className={cn(
        "transition-all duration-300",
        (isAdmin || isMerchant) ? "md:pl-64 pt-0" : isLogin ? "p-0" : "pt-16 pb-20 md:pb-0"
      )}>
        <div className="max-w-7xl mx-auto w-full h-full">
          <Routes>
            <Route path="/login" element={<GuestGuard><Login /></GuestGuard>} />
            <Route path="/register" element={<GuestGuard><Register /></GuestGuard>} />
            <Route path="/" element={<AuthGuard><Home /></AuthGuard>} />
            <Route path="/discovery" element={<AuthGuard><Discovery /></AuthGuard>} />
            <Route path="/product/:id" element={<AuthGuard><ProductDetails /></AuthGuard>} />
            {/* --- Auth-required user routes --- */}
            <Route path="/cart" element={<AuthGuard><Cart /></AuthGuard>} />
            <Route path="/wallet" element={<AuthGuard><UserWallet /></AuthGuard>} />
            <Route path="/recharge" element={<AuthGuard><Recharge /></AuthGuard>} />
            <Route path="/history" element={<AuthGuard><ConsumptionHistory /></AuthGuard>} />
            <Route path="/orders" element={<AuthGuard><UserOrders /></AuthGuard>} />
            <Route path="/order/:id" element={<AuthGuard><UserOrderDetail /></AuthGuard>} />
            <Route path="/orders/:orderNo/refund" element={<AuthGuard><ApplyRefund /></AuthGuard>} />
            <Route path="/payment/status" element={<AuthGuard><PaymentStatus /></AuthGuard>} />
            <Route path="/merchant-store/:id" element={<AuthGuard><PublicMerchantDetail /></AuthGuard>} />
            {/* AI 助手已隐藏：后端功能未联调，待后续需要时恢复 */}
            {/* <Route path="/ai" element={<AuthGuard><AIAssistant /></AuthGuard>} /> */}
            <Route path="/profile" element={<AuthGuard><Profile /></AuthGuard>} />
            <Route path="/success" element={<AuthGuard><Success /></AuthGuard>} />
            <Route path="/coupons" element={<AuthGuard><CouponCenter /></AuthGuard>} />
            <Route path="/points/:tenantId" element={<AuthGuard><Points /></AuthGuard>} />
            <Route path="/growth/:tenantId" element={<AuthGuard><GrowthCenter /></AuthGuard>} />
            <Route path="/addresses" element={<AuthGuard><AddressList /></AuthGuard>} />
            <Route path="/notifications" element={<AuthGuard><Notifications /></AuthGuard>} />
            <Route path="/account-security" element={<AuthGuard><AccountSecurity /></AuthGuard>} />
            
            {/* --- Admin routes (admin role required) --- */}
            <Route path="/admin" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminDashboard /></RoleGuard></AuthGuard>} />
            <Route path="/admin/merchants" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminMerchants /></RoleGuard></AuthGuard>} />
            <Route path="/admin/merchant/new" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminMerchantEdit /></RoleGuard></AuthGuard>} />
            <Route path="/admin/merchant/edit/:id" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminMerchantEdit /></RoleGuard></AuthGuard>} />
            <Route path="/admin/merchant/:id" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminMerchantDetail /></RoleGuard></AuthGuard>} />
            <Route path="/admin/products" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminProducts /></RoleGuard></AuthGuard>} />
            <Route path="/admin/analytics" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminAnalytics /></RoleGuard></AuthGuard>} />
            <Route path="/admin/transactions" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminTransactions /></RoleGuard></AuthGuard>} />
            <Route path="/admin/payments" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminPayments /></RoleGuard></AuthGuard>} />
            <Route path="/admin/order/:id" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminOrderDetail /></RoleGuard></AuthGuard>} />
            <Route path="/admin/recharges" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminRecharges /></RoleGuard></AuthGuard>} />
            <Route path="/admin/users" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminUsers /></RoleGuard></AuthGuard>} />
            <Route path="/admin/user/:id" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminUserDetail /></RoleGuard></AuthGuard>} />
            <Route path="/admin/user/:id/permissions" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminUserPermissions /></RoleGuard></AuthGuard>} />
            <Route path="/admin/permissions" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminPermissions /></RoleGuard></AuthGuard>} />
            <Route path="/admin/withdrawals" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminWithdrawals /></RoleGuard></AuthGuard>} />
            <Route path="/admin/marketing" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminMarketing /></RoleGuard></AuthGuard>} />
            
            {/* --- Merchant routes (merchant role required) --- */}
            <Route path="/merchant" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantDashboard /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/products" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantProducts /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/product/:id" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantProductDetail /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/product/new" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantProductEdit /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/product/edit/:id" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantProductEdit /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/orders" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantOrders /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/order/:id" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantOrderDetail /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/finance" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantFinance /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/marketing/coupons" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantCoupons /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/marketing/activities" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantActivities /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/marketing/members" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantMembers /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/refunds" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantRefunds /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/rules" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantRules /></RoleGuard></AuthGuard>} />
            <Route path="/merchant/withdrawals" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantWithdraw /></RoleGuard></AuthGuard>} />
          </Routes>
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
