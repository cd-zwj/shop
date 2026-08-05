export interface AdminPermissionItem {
  permission?: string;
}

export function hasAdminPermission(
  permissions: string[] | undefined,
  requiredPermission: string | undefined,
) {
  return !requiredPermission || permissions?.includes(requiredPermission) === true;
}

export function filterAdminPermissionItems<T extends AdminPermissionItem>(
  permissions: string[] | undefined,
  items: T[],
) {
  return items.filter((item) => hasAdminPermission(permissions, item.permission));
}
