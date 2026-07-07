import type { SalesOrder, SalesOrderDetail, SalesOrderItem } from '../types/order';
import type { Refund } from '../types/refund';

export type OrderLifecycleTone = 'orange' | 'blue' | 'green' | 'red' | 'slate';

export interface OrderLifecycleAction {
  key: 'pay' | 'cancel' | 'refund' | 'repurchase' | 'detail' | 'contact';
  label: string;
}

export interface OrderLifecyclePresentation {
  label: string;
  description: string;
  nextStep: string;
  failureReason?: string;
  tab: 'pending' | 'processing' | 'completed' | 'closed' | 'all';
  tone: OrderLifecycleTone;
  nextActions: OrderLifecycleAction[];
}

export interface OrderLifecycleContext {
  items?: Array<Partial<Pick<SalesOrderItem, 'deliveryStatus'>>>;
  refunds?: Array<Partial<Pick<Refund, 'refundStatus' | 'rejectReason' | 'refundSuggestion'>>>;
  paymentBillStatus?: string | null;
  paymentBillStatusRemark?: string | null;
  backendPresentation?: Partial<Pick<
    SalesOrder,
    'statusLabel' | 'statusDescription' | 'nextStep' | 'failureReason' | 'availableActions'
  >> | null;
}

export interface OrderProgressStep {
  label: '已创建' | '已支付' | '履约中' | '已结束';
  active: boolean;
}

export interface OrderProgressPresentation {
  steps: OrderProgressStep[];
  progressPercent: number;
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
  refunds?: Array<Partial<Refund>>;
}

export interface MerchantWorkItem {
  key: 'payment' | 'fulfillment' | 'abnormalOrder' | 'refund' | 'refundFailed' | 'stock';
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

export function getOrderLifecyclePresentation(
  order?: Partial<SalesOrder> | null,
  context: OrderLifecycleContext = {},
): OrderLifecyclePresentation {
  const backendPresentation = normalizeBackendOrderPresentation(context.backendPresentation ?? order);
  if (backendPresentation) {
    return backendPresentation;
  }

  if (!order) {
    return {
      label: '加载中',
      description: '正在同步订单状态。',
      nextStep: '请稍候，系统正在读取最新订单信息。',
      tab: 'all',
      tone: 'slate',
      nextActions: [{ key: 'detail', label: '查看详情' }],
    };
  }

  const refundLifecycle = resolveRefundLifecycle(context.refunds);
  if (refundLifecycle) {
    return refundLifecycle;
  }

  if (order.payStatus === 'FAILED' || context.paymentBillStatus === 'FAILED') {
    const failureReason = context.paymentBillStatusRemark?.trim() || '支付渠道返回失败或本地支付单处理失败';
    return {
      label: '支付失败',
      description: `失败原因：${failureReason}`,
      nextStep: '可返回订单详情重新发起支付；如已扣款，请联系商户并保留支付单号。',
      failureReason,
      tab: 'closed',
      tone: 'red',
      nextActions: [
        { key: 'pay', label: '重新支付' },
        { key: 'contact', label: '联系商户' },
        { key: 'repurchase', label: '重新购买' },
      ],
    };
  }

  if (order.payStatus === 'PAYING' || context.paymentBillStatus === 'PAYING') {
    return {
      label: '支付中',
      description: '支付单已创建并进入支付确认阶段，正在等待本地同步支付结果。',
      nextStep: '请保持支付状态页打开或手动刷新；长时间无结果可返回订单重新发起支付。',
      tab: 'pending',
      tone: 'orange',
      nextActions: [
        { key: 'pay', label: '查看支付状态' },
        { key: 'contact', label: '联系商户' },
      ],
    };
  }

  if (isPendingPayment(order)) {
    return {
      label: '待支付',
      description: '订单已创建，请在支付关闭前完成付款；如支付页丢失，可继续支付。',
      nextStep: '下一步：继续支付或取消订单。',
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
      nextStep: '下一步：重新购买同款商品，或进入详情查看结束原因。',
      tab: 'closed',
      tone: order.payStatus === 'FAILED' ? 'red' : 'slate',
      nextActions: [
        { key: 'repurchase', label: '重新购买' },
        { key: 'detail', label: '查看详情' },
      ],
    };
  }

  if (isPaidOrder(order)) {
    return resolveDeliveryLifecycle(context.items);
  }

  return {
    label: `${order.orderStatus || '--'} / ${order.payStatus || '--'}`,
    description: '该订单处于非常规状态，请进入详情确认后续处理方式。',
    nextStep: '下一步：查看详情，或联系商户核对订单状态。',
    tab: 'all',
    tone: 'slate',
    nextActions: [{ key: 'detail', label: '查看详情' }],
  };
}

function normalizeBackendOrderPresentation(
  input?: Partial<Pick<SalesOrder, 'statusLabel' | 'statusDescription' | 'nextStep' | 'failureReason' | 'availableActions'>> | null,
): OrderLifecyclePresentation | null {
  if (!input?.statusLabel || !input.statusDescription || !input.nextStep) {
    return null;
  }

  return {
    label: input.statusLabel,
    description: input.statusDescription,
    nextStep: input.nextStep,
    failureReason: input.failureReason ?? undefined,
    tab: resolveTabFromBackendLabel(input.statusLabel),
    tone: resolveToneFromBackendLabel(input.statusLabel, input.failureReason),
    nextActions: (input.availableActions ?? ['DETAIL']).map((action) => ({
      key: mapBackendOrderAction(action),
      label: action,
    })),
  };
}

function mapBackendOrderAction(action: string): OrderLifecycleAction['key'] {
  const normalized = action.toUpperCase();
  if (normalized === 'PAY') return 'pay';
  if (normalized === 'CANCEL') return 'cancel';
  if (normalized === 'REFUND' || normalized === 'APPLY_REFUND') return 'refund';
  if (normalized === 'REPURCHASE') return 'repurchase';
  if (normalized === 'CONTACT_MERCHANT') return 'contact';
  return 'detail';
}

function resolveTabFromBackendLabel(label: string): OrderLifecyclePresentation['tab'] {
  if (label.includes('待支付') || label.includes('支付中')) return 'pending';
  if (label.includes('完成') || label.includes('已退款')) return 'completed';
  if (label.includes('取消') || label.includes('关闭') || label.includes('支付失败')) return 'closed';
  if (label.includes('退款') || label.includes('发货') || label.includes('履约') || label.includes('已支付')) return 'processing';
  return 'all';
}

function resolveToneFromBackendLabel(label: string, failureReason?: string | null): OrderLifecycleTone {
  if (failureReason || label.includes('失败') || label.includes('驳回')) return 'red';
  if (label.includes('完成') || label.includes('已退款') || label.includes('已发货')) return 'green';
  if (label.includes('待支付') || label.includes('待审核')) return 'orange';
  if (label.includes('取消') || label.includes('关闭')) return 'slate';
  return 'blue';
}

function resolveRefundLifecycle(refunds?: OrderLifecycleContext['refunds']): OrderLifecyclePresentation | null {
  const activeRefund = (refunds ?? []).find((refund) =>
    refund.refundStatus === 'PENDING'
    || refund.refundStatus === 'APPROVED'
    || refund.refundStatus === 'PROCESSING'
    || refund.refundStatus === 'FAILED'
    || refund.refundStatus === 'COMPLETED'
    || refund.refundStatus === 'REJECTED'
  );
  if (!activeRefund) {
    return null;
  }

  const suggestion = activeRefund.refundSuggestion?.trim();
  if (activeRefund.refundStatus === 'COMPLETED') {
    return {
      label: '已退款',
      description: '退款流程已完成，可在资产明细或订单售后记录中追溯。',
      nextStep: '后续无需操作；如金额未变化，请联系商户核对本地账务记录。',
      tab: 'completed',
      tone: 'green',
      nextActions: [
        { key: 'detail', label: '查看详情' },
        { key: 'contact', label: '联系商户' },
      ],
    };
  }

  if (activeRefund.refundStatus === 'FAILED' || activeRefund.refundStatus === 'REJECTED') {
    const failureReason = activeRefund.rejectReason?.trim()
      || (activeRefund.refundStatus === 'FAILED' ? '内部退款处理失败' : '商家已驳回退款申请');
    return {
      label: activeRefund.refundStatus === 'FAILED' ? '退款失败' : '退款驳回',
      description: `失败原因：${failureReason}`,
      nextStep: '下一步：联系商户处理，或补充信息后重新提交售后申请。',
      failureReason,
      tab: 'processing',
      tone: 'red',
      nextActions: [
        { key: 'contact', label: '联系商户' },
        { key: 'refund', label: '重新申请售后' },
      ],
    };
  }

  return {
    label: '退款中',
    description: suggestion || '退款申请已进入审核或内部退款处理流程。',
    nextStep: '预计节点：商家审核后进入内部退款单处理，完成或失败都会继续更新状态。',
    tab: 'processing',
    tone: 'blue',
    nextActions: [
      { key: 'detail', label: '查看退款进度' },
      { key: 'contact', label: '联系商户' },
    ],
  };
}

function resolveDeliveryLifecycle(items?: OrderLifecycleContext['items']): OrderLifecyclePresentation {
  const statuses = (items ?? [])
    .map((item) => item.deliveryStatus)
    .filter((status): status is string => Boolean(status));
  const baseActions: OrderLifecycleAction[] = [
    { key: 'refund', label: '申请售后' },
    { key: 'contact', label: '联系商户' },
  ];

  if (statuses.length === 0) {
    return {
      label: '已支付',
      description: '支付已完成，系统正在等待商家或交付任务接管订单。',
      nextStep: '下一步：等待商家发货、卡密交付或服务核销。',
      tab: 'processing',
      tone: 'blue',
      nextActions: baseActions,
    };
  }

  if (statuses.some((status) => status === 'FAILED' || status === 'REVOKE_FAILED')) {
    return {
      label: '履约失败',
      description: '订单已支付，但交付或撤销流程出现异常。',
      nextStep: '下一步：联系商户处理；商家可在待办中心查看异常订单或重试交付。',
      failureReason: '交付任务失败或资源撤销失败',
      tab: 'processing',
      tone: 'red',
      nextActions: [
        { key: 'contact', label: '联系商户' },
        { key: 'refund', label: '申请售后' },
      ],
    };
  }

  if (statuses.every((status) => status === 'CONFIRMED')) {
    return {
      label: '已完成',
      description: '商品或服务已确认完成，订单履约结束。',
      nextStep: '可继续查看已购内容、再次购买，或在售后期内申请售后。',
      tab: 'completed',
      tone: 'green',
      nextActions: [
        { key: 'repurchase', label: '重新购买' },
        { key: 'refund', label: '申请售后' },
      ],
    };
  }

  if (statuses.some((status) => status === 'DELIVERED' || status === 'CONFIRMED')) {
    return {
      label: '已发货',
      description: '商家已发货或虚拟内容已交付，可在订单或已购内容中查看。',
      nextStep: '下一步：确认收货、查看卡密/文件/核销码，或按需申请售后。',
      tab: 'processing',
      tone: 'green',
      nextActions: [
        { key: 'detail', label: '查看交付' },
        { key: 'refund', label: '申请售后' },
      ],
    };
  }

  if (statuses.some((status) => status === 'DELIVERING')) {
    return {
      label: '发货中',
      description: '商家或系统正在处理发货、卡密发放或服务凭证生成。',
      nextStep: '预计节点：交付完成后会更新为已发货，并在已购内容中开放查看。',
      tab: 'processing',
      tone: 'blue',
      nextActions: baseActions,
    };
  }

  return {
    label: '待发货',
    description: '支付已完成，订单正在等待商家发货或系统自动交付。',
    nextStep: '下一步：等待商家处理；长时间无进展可联系商户。',
    tab: 'processing',
    tone: 'blue',
    nextActions: baseActions,
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

export function getOrderProgressPresentation(
  order: Partial<SalesOrder> | null | undefined,
  lifecycle: OrderLifecyclePresentation,
): OrderProgressPresentation {
  const hasOrder = Boolean(order);
  const isPaid = hasOrder && isPaidOrder(order);
  const isTerminal = isTerminalLifecycle(lifecycle.label) || Boolean(order && isClosedOrder(order));
  const isFulfillment = isFulfillmentLifecycle(lifecycle.label)
    || lifecycle.label === '已完成'
    || lifecycle.label === '已退款';
  const activeFlags = [
    hasOrder,
    isPaid || isFulfillment || isTerminalLifecycleAfterPayment(lifecycle.label),
    isFulfillment,
    isTerminal,
  ];

  const lastActiveIndex = activeFlags.reduce(
    (lastIndex, isActive, index) => isActive ? index : lastIndex,
    -1,
  );

  return {
    steps: (['已创建', '已支付', '履约中', '已结束'] as const).map((label, index) => ({
      label,
      active: activeFlags[index],
    })),
    progressPercent: lastActiveIndex <= 0 ? 0 : (lastActiveIndex / 3) * 100,
  };
}

function isFulfillmentLifecycle(label: string) {
  return label === '待发货'
    || label === '发货中'
    || label === '已发货'
    || label === '履约失败'
    || label === '退款中';
}

function isTerminalLifecycle(label: string) {
  return label === '已完成'
    || label === '已退款'
    || label === '支付失败'
    || label === '退款失败'
    || label === '退款驳回'
    || label === '已取消'
    || label === '已关闭';
}

function isTerminalLifecycleAfterPayment(label: string) {
  return label === '已完成'
    || label === '已退款'
    || label === '退款失败'
    || label === '退款驳回';
}

export function buildMerchantWorkItems(input: MerchantWorkInput): MerchantWorkItem[] {
  const unpaidOrders = input.orders.filter(isPendingPayment).length;
  const fulfillmentOrders = input.orders.filter((order) => isPaidOrder(order) && !isClosedOrder(order)).length;
  const abnormalOrders = input.orders.filter((order) => order.payStatus === 'FAILED').length;
  const pendingRefunds = (input.refunds ?? []).filter((refund) => refund.refundStatus === 'PENDING').length;
  const failedRefunds = (input.refunds ?? []).filter((refund) => refund.refundStatus === 'FAILED').length;
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
      path: '/merchant/orders?tab=pending',
      tone: 'orange',
    },
    {
      key: 'fulfillment',
      label: '待履约订单',
      description: '用户已支付，需要商家发货、卡密交付或服务核销。',
      count: fulfillmentOrders,
      path: '/merchant/orders?tab=shipping',
      tone: 'blue',
    },
    {
      key: 'abnormalOrder',
      label: '异常订单',
      description: '支付失败或内部状态异常的订单，需要确认失败原因并协助用户重试。',
      count: abnormalOrders,
      path: '/merchant/orders?tab=abnormal',
      tone: 'red',
    },
    {
      key: 'refund',
      label: '待审核退款',
      description: '用户已提交售后申请，需要商家审核通过或给出驳回原因。',
      count: pendingRefunds,
      path: '/merchant/refunds?status=PENDING',
      tone: 'orange',
    },
    {
      key: 'refundFailed',
      label: '退款失败单',
      description: '内部退款处理失败，需要检查失败原因并继续跟进用户。',
      count: failedRefunds,
      path: '/merchant/refunds?status=FAILED',
      tone: 'red',
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

export function prioritizeMerchantWorkItems(items: MerchantWorkItem[]) {
  const tonePriority: Record<OrderLifecycleTone, number> = {
    red: 0,
    orange: 1,
    blue: 2,
    green: 3,
    slate: 4,
  };

  return [...items].sort((left, right) => {
    const activeDiff = Number(right.count > 0) - Number(left.count > 0);
    if (activeDiff !== 0) {
      return activeDiff;
    }

    const toneDiff = tonePriority[left.tone] - tonePriority[right.tone];
    if (toneDiff !== 0) {
      return toneDiff;
    }

    return right.count - left.count;
  });
}
