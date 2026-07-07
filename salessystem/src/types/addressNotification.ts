/**
 * 用户端 -- 收货地址 & 通知相关类型
 */

/** 用户收货地址 */
export interface Address {
  id: number;
  platformUserId: number;
  receiverName: string;
  phone: string;
  province: string | null;
  city: string | null;
  district: string | null;
  detail: string;
  isDefault: number;
  deleted: number;
  createTime: string | null;
  updateTime: string | null;
}

/** 新增/修改地址 payload */
export interface AddressPayload {
  receiverName: string;
  phone: string;
  province?: string;
  city?: string;
  district?: string;
  detail: string;
  isDefault?: boolean;
}

export type AppNotificationActionType =
  | 'ORDER_DETAIL'
  | 'REFUND_DETAIL'
  | 'ORDER_LIST'
  | 'COUPON_CENTER'
  | 'WALLET'
  | 'RECHARGE_STATUS';

/** 用户通知 */
export interface AppNotification {
  id: number;
  platformUserId?: number;
  title: string;
  content: string;
  category: string | null;
  readStatus: number;
  deleted?: number;
  readTime: string | null;
  createTime: string | null;
  updateTime?: string | null;
  actionType?: AppNotificationActionType | null;
  actionLabel?: string | null;
  actionUrl?: string | null;
}
