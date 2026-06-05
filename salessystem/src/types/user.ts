/**
 * 用户端 -- 用户信息相关类型
 */

/** 用户基本信息（对应 PlatformUser 的补充字段） */
export interface UserProfile {
  id: number;
  username: string;
  phone?: string | null;
  email?: string | null;
  nickname?: string | null;
  avatar?: string | null;
  gender?: number | null;
  birthday?: string | null;
  status?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/** 修改用户资料 payload */
export interface UpdateUserProfilePayload {
  nickname?: string;
  avatar?: string;
  phone?: string;
  email?: string;
  gender?: number;
  birthday?: string;
}

/** 修改密码 payload */
export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

/** 用户地址 */
export interface UserAddress {
  id: number;
  platformUserId: number;
  receiverName: string;
  receiverPhone: string;
  province?: string | null;
  city?: string | null;
  district?: string | null;
  detailAddress: string;
  isDefault?: boolean | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/** 新增/修改地址 payload */
export interface UserAddressPayload {
  receiverName: string;
  receiverPhone: string;
  province?: string;
  city?: string;
  district?: string;
  detailAddress: string;
  isDefault?: boolean;
}
