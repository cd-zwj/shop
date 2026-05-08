export interface Tenant {
  id: number;
  tenantCode?: string | null;
  name: string;
  contact?: string | null;
  phone?: string | null;
  address?: string | null;
  status?: number | null;
  deleted?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface Product {
  id: number;
  tenantId: number;
  productCode?: string | null;
  name: string;
  price: number;
  unit?: string | null;
  category?: string | null;
  description?: string | null;
  imageUrl?: string | null;
  stock?: number | null;
  status?: number | string | null;
  deleted?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

