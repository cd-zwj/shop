import type { AssetTracePresentation } from './wallet';

export interface PointsBalance {
  id: number;
  points: number;
  totalEarned: number;
  totalUsed: number;
  expiringSoonPoints: number;
  status: number;
  createTime: string;
  updateTime: string;
}

export interface PointsLog {
  id: number;
  tenantId: number;
  userId: number;
  points: number; // 正数表示获取，负数表示消费
  balance: number;
  type: 'GRANT' | 'DEDUCT';
  reason: string;
  expireTime?: string;
  orderNo: string | null;
  createTime: string;
  trace?: AssetTracePresentation | null;
}
