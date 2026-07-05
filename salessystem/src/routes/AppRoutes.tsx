import { lazy, Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import AuthGuard from '../components/guards/AuthGuard';
import GuestGuard from '../components/guards/GuestGuard';
import RoleGuard from '../components/guards/RoleGuard';
import NotFoundPage from '../components/NotFoundPage';

const Home = lazy(() => import('../pages/Home'));
const Discovery = lazy(() => import('../pages/Discovery'));
const ProductDetails = lazy(() => import('../pages/ProductDetails'));
const Cart = lazy(() => import('../pages/Cart'));
const UserWallet = lazy(() => import('../pages/Wallet'));
const Recharge = lazy(() => import('../pages/Recharge'));
const ConsumptionHistory = lazy(() => import('../pages/History'));
const Login = lazy(() => import('../pages/Login'));
const Register = lazy(() => import('../pages/Register'));
const ResetPassword = lazy(() => import('../pages/ResetPassword'));
const Profile = lazy(() => import('../pages/Profile'));
const UserOrders = lazy(() => import('../pages/UserOrders'));
const UserOrderDetail = lazy(() => import('../pages/UserOrderDetail'));
const MyPurchases = lazy(() => import('../pages/MyPurchases'));
const PaymentStatus = lazy(() => import('../pages/PaymentStatus'));
const PublicMerchantDetail = lazy(() => import('../pages/PublicMerchantDetail'));
const CouponCenter = lazy(() => import('../pages/CouponCenter'));
const Points = lazy(() => import('../pages/Points'));
const GrowthCenter = lazy(() => import('../pages/GrowthCenter'));
const ApplyRefund = lazy(() => import('../pages/ApplyRefund'));
const AddressList = lazy(() => import('../pages/AddressList'));
const Notifications = lazy(() => import('../pages/Notifications'));
const AccountSecurity = lazy(() => import('../pages/AccountSecurity'));
const AIAssistant = lazy(() => import('../pages/AIAssistant'));
const Success = lazy(() => import('../pages/Success'));

const AdminDashboard = lazy(() => import('../pages/AdminDashboard'));
const AdminMerchants = lazy(() => import('../pages/AdminMerchants'));
const AdminProducts = lazy(() => import('../pages/AdminProducts'));
const AdminAnalytics = lazy(() => import('../pages/AdminAnalytics'));
const AdminUsers = lazy(() => import('../pages/AdminUsers'));
const AdminUserDetail = lazy(() => import('../pages/AdminUserDetail'));
const AdminUserPermissions = lazy(() => import('../pages/AdminUserPermissions'));
const AdminWithdrawals = lazy(() => import('../pages/AdminWithdrawals'));
const AdminTransactions = lazy(() => import('../pages/AdminTransactions'));
const AdminPayments = lazy(() => import('../pages/AdminPayments'));
const AdminRecharges = lazy(() => import('../pages/AdminRecharges'));
const AdminMerchantDetail = lazy(() => import('../pages/AdminMerchantDetail'));
const AdminMerchantEdit = lazy(() => import('../pages/AdminMerchantEdit'));
const AdminPermissions = lazy(() => import('../pages/AdminPermissions'));
const AdminOrderDetail = lazy(() => import('../pages/AdminOrderDetail'));
const AdminMarketing = lazy(() => import('../pages/AdminMarketing'));
const AdminDocuments = lazy(() => import('../pages/AdminDocuments'));

const MerchantDashboard = lazy(() => import('../pages/merchant/MerchantDashboard'));
const MerchantOrders = lazy(() => import('../pages/merchant/MerchantOrders'));
const MerchantOrderDetail = lazy(() => import('../pages/merchant/MerchantOrderDetail'));
const MerchantFinance = lazy(() => import('../pages/merchant/MerchantFinance'));
const MerchantProducts = lazy(() => import('../pages/merchant/MerchantProducts'));
const MerchantProductDetail = lazy(() => import('../pages/merchant/MerchantProductDetail'));
const MerchantProductEdit = lazy(() => import('../pages/merchant/MerchantProductEdit'));
const MerchantStores = lazy(() => import('../pages/merchant/MerchantStores'));
const MerchantProductTaxonomy = lazy(() => import('../pages/merchant/MerchantProductTaxonomy'));
const MerchantRules = lazy(() => import('../pages/merchant/MerchantRules'));
const MerchantWithdraw = lazy(() => import('../pages/merchant/MerchantWithdraw'));
const MerchantRefunds = lazy(() => import('../pages/merchant/MerchantRefunds'));
const MerchantCoupons = lazy(() => import('../pages/merchant/MerchantCoupons'));
const MerchantActivities = lazy(() => import('../pages/merchant/MerchantActivities'));
const MerchantMembers = lazy(() => import('../pages/merchant/MerchantMembers'));

function PageLoader() {
  return (
    <div className="flex items-center justify-center min-h-[60vh]">
      <div className="flex flex-col items-center gap-3">
        <div className="w-8 h-8 border-3 border-primary/30 border-t-primary rounded-full animate-spin" />
        <span className="text-sm text-slate-400">加载中...</span>
      </div>
    </div>
  );
}

export function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/login" element={<GuestGuard><Login /></GuestGuard>} />
        <Route path="/register" element={<GuestGuard><Register /></GuestGuard>} />
        <Route path="/reset-password" element={<GuestGuard><ResetPassword /></GuestGuard>} />
        <Route path="/" element={<AuthGuard><Home /></AuthGuard>} />
        <Route path="/discovery" element={<AuthGuard><Discovery /></AuthGuard>} />
        <Route path="/product/:id" element={<AuthGuard><ProductDetails /></AuthGuard>} />
        <Route path="/cart" element={<AuthGuard><Cart /></AuthGuard>} />
        <Route path="/wallet" element={<AuthGuard><UserWallet /></AuthGuard>} />
        <Route path="/recharge" element={<AuthGuard><Recharge /></AuthGuard>} />
        <Route path="/history" element={<AuthGuard><ConsumptionHistory /></AuthGuard>} />
        <Route path="/orders" element={<AuthGuard><UserOrders /></AuthGuard>} />
        <Route path="/order/:id" element={<AuthGuard><UserOrderDetail /></AuthGuard>} />
        <Route path="/my-purchases" element={<AuthGuard><MyPurchases /></AuthGuard>} />
        <Route path="/orders/:orderNo/refund" element={<AuthGuard><ApplyRefund /></AuthGuard>} />
        <Route path="/payment/status" element={<AuthGuard><PaymentStatus /></AuthGuard>} />
        <Route path="/merchant-store/:id" element={<AuthGuard><PublicMerchantDetail /></AuthGuard>} />
        <Route path="/ai" element={<AuthGuard><RoleGuard allowedRoles={['user']}><AIAssistant /></RoleGuard></AuthGuard>} />
        <Route path="/profile" element={<AuthGuard><Profile /></AuthGuard>} />
        <Route path="/success" element={<AuthGuard><Success /></AuthGuard>} />
        <Route path="/coupons" element={<AuthGuard><CouponCenter /></AuthGuard>} />
        <Route path="/points/:tenantId" element={<AuthGuard><Points /></AuthGuard>} />
        <Route path="/growth/:tenantId" element={<AuthGuard><GrowthCenter /></AuthGuard>} />
        <Route path="/addresses" element={<AuthGuard><AddressList /></AuthGuard>} />
        <Route path="/notifications" element={<AuthGuard><Notifications /></AuthGuard>} />
        <Route path="/account-security" element={<AuthGuard><AccountSecurity /></AuthGuard>} />

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
        <Route path="/admin/documents" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AdminDocuments /></RoleGuard></AuthGuard>} />
        <Route path="/admin/ai" element={<AuthGuard><RoleGuard allowedRoles={['admin']}><AIAssistant /></RoleGuard></AuthGuard>} />

        <Route path="/merchant" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantDashboard /></RoleGuard></AuthGuard>} />
        <Route path="/merchant/stores" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantStores /></RoleGuard></AuthGuard>} />
        <Route path="/merchant/products" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantProducts /></RoleGuard></AuthGuard>} />
        <Route path="/merchant/product-taxonomy" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><MerchantProductTaxonomy /></RoleGuard></AuthGuard>} />
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
        <Route path="/merchant/ai" element={<AuthGuard><RoleGuard allowedRoles={['merchant']}><AIAssistant /></RoleGuard></AuthGuard>} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
