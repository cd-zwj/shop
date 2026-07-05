import type { SalesOrder, SalesOrderDetail, SalesOrderItem } from '../types/order';

export type OrderLifecycleTone = 'orange' | 'blue' | 'green' | 'red' | 'slate';

export interface OrderLifecycleAction {
  key: 'pay' | 'cancel' | 'refund' | 'repurchase' | 'detail' | 'contact';
  label: string;
}

export interface OrderLifecyclePresentation {
  label: string;
  description: string;
  tab: 'pending' | 'processing' | 'completed' | 'closed' | 'all';
  tone: OrderLifecycleTone;
  nextActions: OrderLifecycleAction[];
}

type FlatSalesOrderDetail = SalesOrder & {
  items?: SalesOrderItem[];
  paymentBillNo?: string | null;
  paymentBillStatus?: string | null;
  paymentBillStatusRemark?: string | null;
  paymentBillExpireTime?: string | null;
};

type RawSalesOrderDetail = SalesOrderDetail | FlatSalesOrderDetail;

interface MerchantWorkInput {
  orders: Array<Partial<SalesOrder>>;
  products: Array<{ stock?: number | null; status?: string | number | null }>;
}

export interface MerchantWorkItem {
  key: 'payment' | 'fulfillment' | 'stock';
  label: string;
  description: string;
  count: number;
  path: string;
  tone: OrderLifecycleTone;
}

export function normalizeSalesOrderDetail(raw: RawSalesOrderDetail): SalesOrderDetail {
  if ('order' in raw && raw.order) {
    return raw;
  }

  const {
    items = [],
    paymentBillNo = null,
    paymentBillStatus = null,
    paymentBillStatusRemark = null,
    paymentBillExpireTime = null,
    ...order
  } = raw;
  return {
    order: order as SalesOrder,
    items,
    paymentBillNo,
    paymentBillStatus,
    paymentBillStatusRemark,
    paymentBillExpireTime,
  };
}

export function isPendingPayment(order?: Partial<SalesOrder> | null) {
  if (order?.orderStatus === 'CANCELLED'
    || order?.orderStatus === 'CLOSED'
    || order?.payStatus === 'CLOSED'
    || order?.payStatus === 'FAILED') {
    return false;
  }

  return order?.payStatus === 'WAIT_PAY'
    || order?.payStatus === 'PAYING'
    || order?.orderStatus === 'CREATED';
}

export function isClosedOrder(order?: Partial<SalesOrder> | null) {
  return order?.orderStatus === 'CANCELLED'
    || order?.orderStatus === 'CLOSED'
    || order?.payStatus === 'CLOSED'
    || order?.payStatus === 'FAILED';
}

export function isPaidOrder(order?: Partial<SalesOrder> | null) {
  return order?.payStatus === 'SUCCESS' || order?.orderStatus === 'PAID';
}

export function getOrderLifecyclePresentation(order?: Partial<SalesOrder> | null): OrderLifecyclePresentation {
  if (!order) {
    return {
      label: '加载中',
      description: '正在同步订单状态。',
      tab: 'all',
      tone: 'slate',
      nextActions: [{ key: 'detail', label: '查看详情' }],
    };
  }

  if (isPendingPayment(order)) {
    return {
      label: '待支付',
      description: '订单已创建，请在支付关闭前完成付款；如支付页丢失，可继续支付。',
      tab: 'pending',
      tone: 'orange',
      nextActions: [
        { key: 'pay', label: '继续支付' },
        { key: 'cancel', label: '取消订单' },
      ],
    };
  }

  if (isClosedOrder(order)) {
    return {
      label: order.orderStatus === 'CANCELLED' ? '已取消' : '已关闭',
      description: '当前订单已结束，如仍需购买可重新加入购物车。',
      tab: 'closed',
      tone: order.payStatus === 'FAILED' ? 'red' : 'slate',
      nextActions: [
        { key: 'repurchase', label: '重新购买' },
        { key: 'detail', label: '查看详情' },
      ],
    };
  }

  if (isPaidOrder(order)) {
    return {
      label: '待履约',
      description: '支付已完成，商家正在处理发货、卡密交付或服务核销。',
      tab: 'processing',
      tone: 'blue',
      nextActions: [
        { key: 'refund', label: '申请售后' },
        { key: 'contact', label: '联系商户' },
      ],
    };
  }

  return {
    label: `${order.orderStatus || '--'} / ${order.payStatus || '--'}`,
    description: '该订单处于非常规状态，请进入详情确认后续处理方式。',
    tab: 'all',
    tone: 'slate',
    nextActions: [{ key: 'detail', label: '查看详情' }],
  };
}

export function getOrderToneClass(tone: OrderLifecycleTone) {
  const classes: Record<OrderLifecycleTone, string> = {
    orange: 'border-orange-100 bg-orange-50 text-orange-600',
    blue: 'border-blue-100 bg-blue-50 text-blue-600',
    green: 'border-green-100 bg-green-50 text-green-600',
    red: 'border-red-100 bg-red-50 text-red-600',
    slate: 'border-slate-200 bg-slate-100 text-slate-500',
  };
  return classes[tone];
}

export function buildMerchantWorkItems(input: MerchantWorkInput): MerchantWorkItem[] {
  const unpaidOrders = input.orders.filter(isPendingPayment).length;
  const fulfillmentOrders = input.orders.filter((order) => isPaidOrder(order) && !isClosedOrder(order)).length;
  const lowStockProducts = input.products.filter((product) => {
    const stock = Number(product.stock ?? 0);
    return product.status !== 'inactive' && stock <= 5;
  }).length;

  return [
    {
      key: 'payment',
      label: '待付款订单',
      description: '用户已下单但尚未完成支付，可关注是否需要催付或备货。',
      count: unpaidOrders,
      path: '/merchant/orders',
      tone: 'orange',
    },
    {
      key: 'fulfillment',
      label: '待履约订单',
      description: '用户已支付，需要商家发货、卡密交付或服务核销。',
      count: fulfillmentOrders,
      path: '/merchant/orders',
      tone: 'blue',
    },
    {
      key: 'stock',
      label: '低库存商品',
      description: '库存低于或等于 5 的上架商品，建议补货或下架。',
      count: lowStockProducts,
      path: '/merchant/products',
      tone: 'red',
    },
  ];
}
