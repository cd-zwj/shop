import { describe, expect, it } from 'vitest';
import {
  filterMerchantPermissionItems,
  hasMerchantPermission,
  normalizeMerchantRole,
} from './merchantPermissions';

describe('merchantPermissions', () => {
  it('normalizes backend employee role values', () => {
    expect(normalizeMerchantRole(' owner ')).toBe('OWNER');
    expect(normalizeMerchantRole(null)).toBe('');
  });

  it('allows owner to access every merchant module', () => {
    expect(hasMerchantPermission('OWNER', 'finance:view')).toBe(true);
    expect(hasMerchantPermission('OWNER', 'marketing:manage')).toBe(true);
    expect(hasMerchantPermission('OWNER', 'withdrawal:manage')).toBe(true);
  });

  it('keeps cashier focused on orders and refunds', () => {
    expect(hasMerchantPermission('CASHIER', 'order:manage')).toBe(true);
    expect(hasMerchantPermission('CASHIER', 'refund:manage')).toBe(true);
    expect(hasMerchantPermission('CASHIER', 'finance:view')).toBe(false);
    expect(hasMerchantPermission('CASHIER', 'product:manage')).toBe(false);
  });

  it('keeps finance staff out of product and marketing modules', () => {
    expect(hasMerchantPermission('FINANCE', 'finance:view')).toBe(true);
    expect(hasMerchantPermission('FINANCE', 'withdrawal:manage')).toBe(true);
    expect(hasMerchantPermission('FINANCE', 'product:manage')).toBe(false);
    expect(hasMerchantPermission('FINANCE', 'marketing:manage')).toBe(false);
  });

  it('filters menu items according to role permissions', () => {
    const items = [
      { label: '工作台', permission: 'dashboard:view' as const },
      { label: '订单', permission: 'order:manage' as const },
      { label: '财务', permission: 'finance:view' as const },
      { label: '优惠券', permission: 'marketing:manage' as const },
    ];

    expect(filterMerchantPermissionItems('CASHIER', items).map((item) => item.label)).toEqual([
      '工作台',
      '订单',
    ]);
  });
});
