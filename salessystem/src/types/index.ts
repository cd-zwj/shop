/**
 * 类型统一导出
 * 按模块分类 re-export，方便外部使用
 */

// API 通用类型
export type { ApiResponse, PageResult } from './api';
export { ApiError } from './api';

// 认证类型
export type {
  AuthRole,
  PlatformUser,
  PlatformLoginDTO,
  LoginCaptchaVO,
  PlatformRegisterDTO,
  MerchantTenantSession,
  MerchantSession,
  AdminSession,
} from './auth';

// 用户信息类型
export type {
  UserProfile,
  UpdateUserProfilePayload,
  ChangePasswordPayload,
  UserAddress,
  UserAddressPayload,
} from './user';

// 商户 / 商品类型
export type {
  MerchantProduct,
  MerchantProductUpsertPayload,
  MerchantProductFilters,
  MerchantOrderFilters,
  MerchantWalletSummary,
  MerchantPointsRule,
  MerchantPointsRulePayload,
  MerchantRechargeRule,
  MerchantRechargeRulePayload,
  MerchantWithdrawal,
  MerchantWithdrawalFilters,
  MerchantWithdrawalApplyPayload,
  MerchantOrder,
  MerchantOrderDetail,
} from './merchant';

// 目录类型
export type { Tenant, Product } from './catalog';

// 购物车类型
export type { CartItem, CheckoutSource } from './cart';

// 订单类型
export type {
  WalletStrategy,
  AppCreateOrderItemPayload,
  AppCreateOrderPayload,
  OrderPayment,
  SalesOrder,
  SalesOrderItem,
  SalesOrderDetail,
} from './order';

// 支付类型
export type { PaymentChannelCode, PaymentBill } from './payment';

// 钱包类型
export type {
  WalletAccount,
  WalletLog,
  RechargePayment,
  MerchantRechargeRule as MerchantRechargeRuleFromWallet,
  UnifiedRechargeRule,
  UnifiedWalletRechargePayload,
  MerchantWalletRechargePayload,
} from './wallet';

// 管理后台类型
export type {
  AdminInfo,
  AdminDashboardOverview,
  AdminMerchantListItem,
  AdminMerchantDetail,
  AdminMerchantPayload,
  AdminMerchantRecord,
  AdminPlatformUser,
  AdminPermission,
  AdminPermissionCatalog,
  AdminUserPermissionDetail,
  AdminTradeOverview,
  AdminOrderListItem,
  AdminOrderDetail,
  AdminPaymentBill,
  AdminRechargeOrder,
  AdminWithdrawal,
  FileExistsResult,
} from './admin';

// 优惠券与积分类型
export type { CouponTemplate, UserCoupon, CouponReceiveResult } from './coupon';
export type { PointsBalance, PointsLog } from './points';

// 营销与会员类型
export type {
  MerchantCouponTemplate,
  CouponTemplateCreatePayload,
  CouponScope,
  CouponScopeCreatePayload,
  PromotionActivity,
  ActivityRule,
  ActivityRuleCreatePayload,
  MemberLevel,
  MemberTag,
} from './marketing';

// 退款类型
export type { RefundCreateDTO, Refund } from './refund';

// 收货地址 & 通知类型
export type { Address, AddressPayload, AppNotification } from './addressNotification';

