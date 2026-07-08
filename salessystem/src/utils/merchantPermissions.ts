export type MerchantPermission =
  | 'dashboard:view'
  | 'store:manage'
  | 'product:manage'
  | 'order:manage'
  | 'refund:manage'
  | 'finance:view'
  | 'withdrawal:manage'
  | 'marketing:manage'
  | 'rule:manage'
  | 'employee:manage'
  | 'ai:use';

export interface MerchantMenuPermissionItem {
  permission?: MerchantPermission;
}

const ALL_PERMISSIONS: MerchantPermission[] = [
  'dashboard:view',
  'store:manage',
  'product:manage',
  'order:manage',
  'refund:manage',
  'finance:view',
  'withdrawal:manage',
  'marketing:manage',
  'rule:manage',
  'employee:manage',
  'ai:use',
];

const ROLE_PERMISSIONS: Record<string, MerchantPermission[]> = {
  OWNER: ALL_PERMISSIONS,
  ADMIN: ALL_PERMISSIONS,
  MANAGER: [
    'dashboard:view',
    'store:manage',
    'product:manage',
    'order:manage',
    'refund:manage',
    'marketing:manage',
    'rule:manage',
    'ai:use',
  ],
  OPERATOR: [
    'dashboard:view',
    'product:manage',
    'order:manage',
    'refund:manage',
    'marketing:manage',
    'ai:use',
  ],
  CASHIER: [
    'dashboard:view',
    'order:manage',
    'ai:use',
  ],
  FINANCE: [
    'dashboard:view',
    'finance:view',
    'withdrawal:manage',
    'ai:use',
  ],
};

export function normalizeMerchantRole(role?: string | null) {
  return role?.trim().toUpperCase() || '';
}

export function hasMerchantPermission(role: string | null | undefined, permission?: MerchantPermission) {
  if (!permission) {
    return true;
  }

  const normalizedRole = normalizeMerchantRole(role);
  const permissions = ROLE_PERMISSIONS[normalizedRole];
  return permissions ? permissions.includes(permission) : permission === 'dashboard:view';
}

export function filterMerchantPermissionItems<T extends MerchantMenuPermissionItem>(
  role: string | null | undefined,
  items: T[],
) {
  return items.filter((item) => hasMerchantPermission(role, item.permission));
}
