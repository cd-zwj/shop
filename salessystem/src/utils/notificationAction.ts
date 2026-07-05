import type { AppNotification } from '../types/addressNotification';

export type NotificationActionType =
  | 'ORDER_DETAIL'
  | 'REFUND_DETAIL'
  | 'ORDER_LIST'
  | 'COUPON_CENTER';

export interface NotificationAction {
  type: NotificationActionType;
  label: string;
  path: string;
}

const ORDER_NO_PATTERN = /\b(?:SO|ORD|EX)[A-Z0-9_-]{2,}\b/;

export function getNotificationAction(notification: AppNotification): NotificationAction | null {
  if (isValidBackendAction(notification)) {
    return {
      type: notification.actionType,
      label: notification.actionLabel,
      path: notification.actionUrl,
    };
  }

  const category = notification.category;
  const orderNo = extractOrderNo(notification.content);

  if ((category === 'ORDER' || category === 'PAYMENT') && orderNo) {
    return {
      type: 'ORDER_DETAIL',
      label: '查看订单',
      path: `/order/${encodeURIComponent(orderNo)}`,
    };
  }

  if (category === 'REFUND') {
    if (orderNo) {
      return {
        type: 'REFUND_DETAIL',
        label: '查看售后',
        path: `/orders/${encodeURIComponent(orderNo)}/refund`,
      };
    }

    return {
      type: 'ORDER_LIST',
      label: '查看订单',
      path: '/orders',
    };
  }

  if (category === 'COUPON' || category === 'PROMOTION') {
    return {
      type: 'COUPON_CENTER',
      label: '查看优惠券',
      path: '/coupons',
    };
  }

  return null;
}

function isValidBackendAction(
  notification: AppNotification,
): notification is AppNotification & {
  actionType: NotificationActionType;
  actionLabel: string;
  actionUrl: string;
} {
  return Boolean(notification.actionType && notification.actionLabel && notification.actionUrl);
}

function extractOrderNo(content?: string | null) {
  if (!content) {
    return null;
  }

  return content.match(ORDER_NO_PATTERN)?.[0] ?? null;
}
