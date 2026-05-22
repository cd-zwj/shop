export type AuthRole = 'user' | 'merchant' | 'admin';

export interface PlatformUser {
  id: number;
  username: string;
  phone?: string | null;
  email?: string | null;
  status?: number | null;
  avatar?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface PlatformLoginDTO {
  username: string;
  password: string;
  captchaKey: string;
  captchaCode: string;
}

export interface LoginCaptchaVO {
  captchaKey: string;
  captchaImage: string;
}

export interface PlatformRegisterDTO {
  username: string;
  password: string;
  phone?: string;
  email?: string;
}

export interface MerchantTenantSession {
  tenantId: number;
  tenantName: string;
  employeeRole: string;
}

export interface MerchantSession {
  token: string;
  expiresIn: number;
  platformUserId: number;
  username: string;
  tenantId: number;
  tenantName: string;
  employeeRole: string;
  tenants: MerchantTenantSession[];
}

export interface AdminSession {
  userId: number;
  username: string;
  nickname?: string | null;
  userType?: number | null;
  role: string;
  scope: string;
  permissions: string[];
  roles: string[];
}
