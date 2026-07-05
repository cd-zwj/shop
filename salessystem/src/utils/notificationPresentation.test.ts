import { describe, expect, it } from 'vitest';
import { getNotificationPresentation } from './notificationPresentation';

describe('notificationPresentation', () => {
  it('labels refund notifications as actionable after-sales messages', () => {
    expect(getNotificationPresentation('REFUND').label).toBe('售后');
  });

  it('labels coupon notifications consistently with the backend category', () => {
    expect(getNotificationPresentation('COUPON').label).toBe('优惠券');
  });

  it('labels wallet notifications so balance changes are not shown as generic messages', () => {
    expect(getNotificationPresentation('WALLET').label).toBe('钱包');
  });

  it('falls back for unknown categories', () => {
    expect(getNotificationPresentation('UNKNOWN').label).toBe('通知');
    expect(getNotificationPresentation(null).label).toBe('通知');
  });
});
