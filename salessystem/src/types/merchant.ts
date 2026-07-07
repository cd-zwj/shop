import type { SalesOrder, SalesOrderDetail } from './order';
import type { FulfillmentMode, ProductType } from './catalog';
import type { MerchantWorkItem, MerchantWorkItemKey, OrderLifecycleTone } from '../utils/orderLifecycle';

export type { FulfillmentMode, ProductType };

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
  virtualTypeId?: number | null;
  virtualCategoryId?: number | null;
  stock: number;
  status: 'active' | 'inactive' | 'out_of_stock' | string;
  productType?: ProductType | null;
  /** JSON 字符串,按 productType 解读 */
  deliveryConfig?: string | null;
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
  virtualTypeId?: number;
  virtualCategoryId?: number;
  stock: number;
  status?: 'active' | 'inactive' | 'out_of_stock';
  productType?: ProductType;
  /** JSON 字符串,提交时按需序列化 */
  deliveryConfig?: string;
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

export type MerchantCardKeyStatus = 'AVAILABLE' | 'USED' | 'RETURNED' | 'DISABLED';

export interface MerchantCardKey {
  id: number;
  tenantId: number;
  productId: number;
  cardCode: string;
  status: MerchantCardKeyStatus | string;
  orderNo?: string | null;
  orderItemId?: number | null;
  usedTime?: string | null;
  returnedTime?: string | null;
  returnReason?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface MerchantCardKeySummary {
  productId: number;
  availableCount: number;
  usedCount: number;
  returnedCount: number;
  disabledCount: number;
  totalCount: number;
}

export interface MerchantOrderFilters {
  current?: number;
  size?: number;
  orderStatus?: string;
  payStatus?: string;
  keyword?: string;
}

export type MerchantWorkbenchTodoKey = MerchantWorkItemKey;
export type MerchantWorkbenchTodoTone = OrderLifecycleTone;
export type MerchantWorkbenchTodoItem = MerchantWorkItem;

export interface MerchantWorkbenchTodoSummary {
  totalCount: number;
  items: MerchantWorkbenchTodoItem[];
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

export interface VirtualProductType {
  id: number;
  tenantId: number;
  typeCode: string;
  typeName: string;
  deliveryStrategy: Exclude<ProductType, 'PHYSICAL'>;
  description?: string | null;
  status: number;
  sortOrder: number;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface VirtualProductTypePayload {
  typeCode: string;
  typeName: string;
  deliveryStrategy: Exclude<ProductType, 'PHYSICAL'>;
  description?: string;
  status?: number;
  sortOrder?: number;
}

export interface VirtualProductCategory {
  id: number;
  tenantId: number;
  typeId: number;
  categoryCode: string;
  categoryName: string;
  parentId: number;
  description?: string | null;
  status: number;
  sortOrder: number;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface VirtualProductCategoryPayload {
  typeId: number;
  categoryCode: string;
  categoryName: string;
  parentId?: number;
  description?: string;
  status?: number;
  sortOrder?: number;
}
