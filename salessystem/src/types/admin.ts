import type { SalesOrderDetail } from './order';

export interface AdminInfo {
  userId: number;
  username: string;
  nickname?: string | null;
  role: string;
  scope: string;
  roles: string[];
  permissions: string[];
}

export interface AdminDashboardOverview {
  totalPlatformUsers: number;
  totalMerchants: number;
  activeMerchants: number;
  totalOrders: number;
  paidOrders: number;
  totalOrderAmount: number;
  totalPaymentBills: number;
  totalPaymentAmount: number;
  totalRechargeOrders: number;
  totalRechargeAmount: number;
  pendingWithdrawals: number;
}

export interface AdminMerchantListItem {
  id: number;
  tenantCode: string;
  name: string;
  contactName?: string | null;
  contactPhone?: string | null;
  status: number;
  createTime?: string | null;
}

export interface AdminMerchantDetail {
  id: number;
  tenantCode: string;
  name: string;
  contactName?: string | null;
  contactPhone?: string | null;
  address?: string | null;
  status: number;
  createTime?: string | null;
  productCount: number;
  orderCount: number;
  totalSales: number;
}

export interface AdminMerchantPayload {
  tenantCode: string;
  name: string;
  contact?: string;
  phone?: string;
  address?: string;
}

export interface AdminMerchantRecord {
  id: number;
  tenantCode?: string | null;
  name?: string | null;
  contact?: string | null;
  phone?: string | null;
  address?: string | null;
  status?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface AdminPlatformUser {
  id: number;
  userNo?: string | null;
  username: string;
  phone?: string | null;
  email?: string | null;
  status: number;
  createTime?: string | null;
  memberTenantCount: number;
  employeeTenantCount: number;
  unifiedWalletBalance: number;
}

export interface AdminPermission {
  id: number;
  permissionCode: string;
  permissionName: string;
  module?: string | null;
  description?: string | null;
  createTime?: string | null;
}

export type AdminPermissionCatalog = Record<string, AdminPermission[]>;

export interface AdminUserPermissionDetail {
  userId: number;
  username: string;
  rolePermissions: string[];
  extraPermissions: string[];
  allPermissions: string[];
}

export interface AdminTradeOverview {
  totalOrders: number;
  paidOrders: number;
  pendingOrders: number;
  totalOrderAmount: number;
  totalExternalPayAmount: number;
  totalPaymentBills: number;
  paidPaymentBills: number;
  totalPaymentAmount: number;
  totalRechargeOrders: number;
  successRechargeOrders: number;
  totalRechargeAmount: number;
}

export interface AdminOrderListItem {
  id: number;
  orderNo: string;
  tenantId: number;
  platformUserId: number;
  subject?: string | null;
  orderStatus: string;
  payStatus: string;
  totalAmount: number;
  externalPayAmount?: number | null;
  createTime?: string | null;
}

export type AdminOrderDetail = SalesOrderDetail;

export interface AdminPaymentBill {
  id: number;
  billNo: string;
  bizType?: string | null;
  bizNo?: string | null;
  tenantId?: number | null;
  platformUserId?: number | null;
  channelCode?: string | null;
  payStatus?: string | null;
  payAmount: number;
  callbackStatus?: string | null;
  thirdPartyBillNo?: string | null;
  createTime?: string | null;
}

export interface AdminRechargeOrder {
  id: number;
  rechargeNo: string;
  walletType?: string | null;
  tenantId?: number | null;
  platformUserId?: number | null;
  rechargeAmount: number;
  giftAmount: number;
  giftPoints: number;
  actualCreditAmount: number;
  bizStatus?: string | null;
  createTime?: string | null;
}

export interface AdminWithdrawal {
  id: number;
  tenantId: number;
  merchantName?: string | null;
  amount: number;
  bankName?: string | null;
  bankAccount?: string | null;
  accountName?: string | null;
  status: number;
  rejectReason?: string | null;
  applyTime?: string | null;
  approveTime?: string | null;
  approverId?: number | null;
  approverName?: string | null;
  createTime?: string | null;
}

export interface AdminMerchantBalance {
  id: number;
  tenantId: number;
  balance: number;
  frozenBalance: number;
  totalIncome: number;
  totalWithdrawal: number;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface FileExistsResult {
  exists: boolean;
  fileUrl?: string | null;
  message?: string | null;
}
