export type FulfillmentMode = 'STORE_PICKUP';

export interface Tenant {
  id: number;
  tenantCode?: string | null;
  name: string;
  contact?: string | null;
  phone?: string | null;
  address?: string | null;
  status?: number | null;
  createTime?: string | null;
}

export interface AppStore {
  id: number;
  tenantId: number;
  storeName: string;
  contactPhone?: string | null;
  address?: string | null;
  businessHours?: string | null;
}

export interface Product {
  id: number;
  tenantId?: number | null;
  storeId?: number | null;
  productCode?: string | null;
  name: string;
  price: number;
  unit?: string | null;
  category?: string | null;
  description?: string | null;
  imageUrl?: string | null;
  stock?: number | null;
  fulfillmentMode?: FulfillmentMode | string | null;
  status?: number | string | null;
  inventoryLabel?: string | null;
  inventoryDescription?: string | null;
  fulfillmentLabel?: string | null;
  fulfillmentDescription?: string | null;
  afterSalesNote?: string | null;
  purchaseLimitNote?: string | null;
  deliveryAccessDescription?: string | null;
  deliveryAccessActionLabel?: string | null;
  purchasable?: boolean | null;
  createTime?: string | null;
}
