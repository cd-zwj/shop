import { describe, expect, it } from 'vitest';
import type { AppNotification } from '../types/addressNotification';
import { getNotificationAction } from './notificationAction';

describe('notificationAction', () => {
  it('uses backend action fields when present', () => {
    const action = getNotificationAction(notification({
      actionType: 'ORDER_DETAIL',
      actionLabel: '查看订单',
      actionUrl: '/order/SO001',
    }));

    expect(action).toEqual({
      type: 'ORDER_DETAIL',
      label: '查看订单',
      path: '/order/SO001',
    });
  });

  it('derives order action from notification content', () => {
    const action = getNotificationAction(notification({
      category: 'ORDER',
      content: '您的订单 SO202607050001 已支付成功',
    }));

    expect(action?.path).toBe('/order/SO202607050001');
    expect(action?.label).toBe('查看订单');
  });

  it('derives refund action from content when order number exists', () => {
    const action = getNotificationAction(notification({
      category: 'REFUND',
      content: '订单 SO202607050001 的退款申请 RA202607050001 已提交',
    }));

    expect(action?.path).toBe('/orders/SO202607050001/refund');
    expect(action?.label).toBe('查看售后');
  });

  it('routes coupon notifications to coupon center', () => {
    const action = getNotificationAction(notification({
      category: 'COUPON',
      content: '你有一张优惠券即将过期',
    }));

    expect(action?.path).toBe('/coupons');
  });

  it('returns null when no action can be derived', () => {
    expect(getNotificationAction(notification({ category: 'SYSTEM', content: '系统维护' }))).toBeNull();
  });
});

function notification(overrides: Partial<AppNotification>): AppNotification {
  return {
    id: 1,
    platformUserId: 2,
    title: '通知',
    content: '内容',
    category: 'SYSTEM',
    readStatus: 0,
    deleted: 0,
    readTime: null,
    createTime: null,
    updateTime: null,
    ...overrides,
  };
}
