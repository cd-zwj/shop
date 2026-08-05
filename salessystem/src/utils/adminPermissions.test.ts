import { describe, expect, it } from 'vitest';
import { filterAdminPermissionItems, hasAdminPermission } from './adminPermissions';

describe('adminPermissions', () => {
  it('requires an exact server-confirmed permission', () => {
    expect(hasAdminPermission(['admin:after-sale:list'], 'admin:after-sale:list')).toBe(true);
    expect(hasAdminPermission(['admin:after-sale:list'], 'admin:after-sale:manage')).toBe(false);
    expect(hasAdminPermission(undefined, 'admin:after-sale:list')).toBe(false);
  });

  it('keeps unrestricted items and filters protected items', () => {
    const items = [
      { path: '/admin', permission: undefined },
      { path: '/admin/after-sales', permission: 'admin:after-sale:list' },
    ];

    expect(filterAdminPermissionItems([], items)).toEqual([items[0]]);
    expect(filterAdminPermissionItems(['admin:after-sale:list'], items)).toEqual(items);
  });
});
