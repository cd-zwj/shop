export interface PointsBalance {
  points: number;
  userId: number;
  tenantId: number;
}

export interface PointsLog {
  id: number;
  tenantId: number;
  userId: number;
  points: number; // 正数表示获取，负数表示消费
  balance: number;
  type: 'GRANT' | 'DEDUCT';
  reason: string;
  orderNo: string | null;
  createTime: string;
}

export interface ExchangeProduct {
  id: number;
  tenantId: number;
  productId: number;
  pointsRequired: number;
  stock: number;
  status: number; // 1=上架 0=下架
  createTime: string;
  updateTime: string;
}
