import type { SalesOrder, SalesOrderDetail } from './order';
import type { FulfillmentMode } from './catalog';
import type { MerchantWorkItem, MerchantWorkItemKey, OrderLifecycleTone } from '../utils/orderLifecycle';

export type { FulfillmentMode };

export interface MerchantProduct {
  id: number;
  tenantId: number;
  productCode: string;
  name: string;
  price: number;
  unit?: string | null;
  category?: string | null;
  description?: string | null;
  imageUrl?: string | null;
  storeId?: number | null;
  fulfillmentMode?: FulfillmentMode | null;
  stock: number;
  status: 'active' | 'inactive' | 'out_of_stock' | string;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface MerchantProductUpsertPayload {
  productCode?: string;
  name: string;
  price: number;
  unit?: string;
  category?: string;
  description?: string;
  imageUrl?: string;
  storeId?: number;
  fulfillmentMode?: FulfillmentMode;
  stock: number;
  status?: 'active' | 'inactive' | 'out_of_stock';
}

export interface MerchantProductFilters {
  current?: number;
  size?: number;
  search?: string;
  category?: string;
  status?: string;
}

export interface MerchantProductChangeLog {
  id: number;
  tenantId: number;
  productId: number;
  changeType: 'PRICE' | 'STOCK' | string;
  fieldName: 'price' | 'stock' | string;
  oldValue?: string | null;
  newValue?: string | null;
 operatorId?: number | null;
remark?: string | null;
createTime?: string | null;
}

export interface MerchantOrderFilters {
  current?: number;
  size?: number;
  orderStatus?: string;
  payStatus?: string;
  fulfillmentStatus?: 'PENDING' | 'COMPLETED' | 'ABNORMAL' | string;
  deliveryStatus?: string;
  keyword?: string;
}

export type MerchantWorkbenchTodoKey = MerchantWorkItemKey;
export type MerchantWorkbenchTodoTone = OrderLifecycleTone;
export type MerchantWorkbenchTodoItem = MerchantWorkItem;

export interface MerchantWorkbenchTodoSummary {
  totalCount: number;
  items: MerchantWorkbenchTodoItem[];
}

export type MerchantWorkbenchTaskSource = 'compensation' | 'retry';

export interface MerchantWorkbenchTask {
  taskSource: MerchantWorkbenchTaskSource | string;
  id: number;
  taskNo: string;
  taskType?: string | null;
  bizType?: string | null;
  bizNo?: string | null;
  taskStatus: string;
  retryCount?: number | null;
  maxRetryCount?: number | null;
  nextRetryTime?: string | null;
  lastError?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  actionLabel?: string | null;
  actionPath?: string | null;
}

export type MerchantEmployeeRole = 'OWNER' | 'ADMIN' | 'MANAGER' | 'OPERATOR' | 'PICKUP_CLERK' | 'FINANCE';

export interface MerchantEmployee {
  id: number;
  tenantId: number;
  platformUserId: number;
  employeeNo?: string | null;
  employeeRole: MerchantEmployeeRole | string;
  status: 0 | 1 | number;
  username?: string | null;
  phone?: string | null;
  email?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface MerchantProductSalesRankItem {
  productId: number;
  productCode?: string | null;
  productName: string;
  productImage?: string | null;
  salesQuantity: number;
  salesAmount: number;
}

export interface MerchantAnalyticsFilters {
  startDate?: string;
  endDate?: string;
  limit?: number;
}

export interface MerchantWalletSummary {
  tenantId: number;
  availableBalance: number;
  frozenBalance: number;
  totalIncome: number;
 totalWithdrawal: number;
  totalPlatformFee: number;
}

export interface MerchantPointsRule {
  pointsRatio: number;
  enabled: boolean;
}

export interface MerchantPointsRulePayload {
  pointsRatio: number;
  enabled: boolean;
}

export interface MerchantRechargeRule {
  id?: number;
  tenantId?: number;
  rechargeAmount: number;
  giftAmount: number;
  giftPoints: number;
  status: number;
  sortOrder?: number | null;
}

export interface MerchantRechargeRulePayload {
  id?: number;
  rechargeAmount: number;
  giftAmount: number;
  giftPoints: number;
  enabled: boolean;
  sortOrder?: number;
}

export interface MerchantWithdrawal {
  id: number;
  tenantId: number;
  amount: number;
  bankName: string;
  bankAccount: string;
  accountName: string;
  status: number;
  rejectReason?: string | null;
  applyTime?: string | null;
  approveTime?: string | null;
  approverId?: number | null;
  createTime?: string | null;
}

export interface MerchantWithdrawalFilters {
  current?: number;
  size?: number;
  status?: number;
}

export interface MerchantWithdrawalApplyPayload {
  amount: number;
  bankName: string;
  bankAccount: string;
  accountName: string;
}

export interface MerchantTransaction {
  id: number;
  bizType: string;
  bizNo?: string | null;
  changeAmount: number;
 balanceBefore?: number | null;
 balanceAfter?: number | null;
  feeAmount?: number | null;
 remark?: string | null;
 createTime?: string | null;
}

export interface MerchantTransactionFilters {
  current?: number;
  size?: number;
  type?: string;
  startDate?: string;
  endDate?: string;
}

export type MerchantOrder = SalesOrder;
export type MerchantOrderDetail = SalesOrderDetail;

export interface MerchantStore {
  id: number;
  tenantId: number;
  storeNo: string;
  storeName: string;
  storeType?: string | null;
  contactName?: string | null;
  contactPhone?: string | null;
  province?: string | null;
  city?: string | null;
  district?: string | null;
  address?: string | null;
  longitude?: number | null;
  latitude?: number | null;
  rating?: number | null;
  businessHours?: string | null;
  serviceTags?: string | null;
  status: number;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface MerchantStorePayload {
  storeNo?: string;
  storeName: string;
  storeType?: string;
  contactName?: string;
  contactPhone?: string;
  province?: string;
  city?: string;
  district?: string;
  address?: string;
  longitude?: number;
  latitude?: number;
  businessHours?: string;
  serviceTags?: string;
  status?: number;
}

export interface MerchantStoreInventory {
  id: number;
  tenantId: number;
  storeId: number;
  storeName?: string | null;
  productId: number;
  productCode?: string | null;
  productName?: string | null;
  quantity: number;
  lockedQuantity: number;
  availableQuantity: number;
  updateTime?: string | null;
}

export interface MerchantStoreInventoryAdjustment {
  storeId: number;
  productId: number;
  delta: number;
  remark?: string;
}

export interface MerchantStoreInventoryLog {
  id: number;
  storeId: number;
  productId: number;
  changeType: string;
  changeQuantity: number;
  quantityBefore: number;
  quantityAfter: number;
  lockedBefore: number;
  lockedAfter: number;
  bizType?: string | null;
  bizNo?: string | null;
  operatorId?: number | null;
  remark?: string | null;
  createTime?: string | null;
}
