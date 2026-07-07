import type { MerchantOrder } from '../types/merchant';
import type { Refund } from '../types/refund';

export interface MerchantOperationsAnalyticsInput {
  orders: MerchantOrder[];
  refunds: Refund[];
}

export interface MerchantOperationsSummary {
  orderCount: number;
  paidOrderCount: number;
  paidAmount: number;
  averageOrderValue: number;
  refundCaseCount: number;
  refundAmount: number;
  refundRate: number;
  uniqueCustomerCount: number;
  repeatCustomerCount: number;
  repeatCustomerRate: number;
  paidConversionRate: number;
}

const EFFECTIVE_REFUND_STATUSES = new Set(['PENDING', 'APPROVED', 'PROCESSING', 'COMPLETED', 'FAILED']);

export function summarizeMerchantOperations({
  orders,
  refunds,
}: MerchantOperationsAnalyticsInput): MerchantOperationsSummary {
  const paidOrders = orders.filter((order) => order.payStatus === 'SUCCESS');
  const paidOrderNos = new Set(paidOrders.map((order) => order.orderNo));
  const paidAmount = paidOrders.reduce((sum, order) => sum + normalizeAmount(order.totalAmount), 0);
  const effectiveRefunds = refunds.filter((refund) => EFFECTIVE_REFUND_STATUSES.has(refund.refundStatus));
  const refundedPaidOrderNos = new Set(
    effectiveRefunds
      .filter((refund) => paidOrderNos.has(refund.orderNo))
      .map((refund) => refund.orderNo),
  );
  const customerOrderCounts = orders.reduce<Map<number, number>>((counts, order) => {
    const current = counts.get(order.platformUserId) ?? 0;
    return new Map(counts).set(order.platformUserId, current + 1);
  }, new Map());
  const uniqueCustomerCount = customerOrderCounts.size;
  const repeatCustomerCount = Array.from(customerOrderCounts.values()).filter((count) => count > 1).length;

  return {
    orderCount: orders.length,
    paidOrderCount: paidOrders.length,
    paidAmount,
    averageOrderValue: paidOrders.length > 0 ? paidAmount / paidOrders.length : 0,
    refundCaseCount: refundedPaidOrderNos.size,
    refundAmount: effectiveRefunds.reduce((sum, refund) => sum + normalizeAmount(refund.refundAmount), 0),
    refundRate: paidOrders.length > 0 ? refundedPaidOrderNos.size / paidOrders.length : 0,
    uniqueCustomerCount,
    repeatCustomerCount,
    repeatCustomerRate: uniqueCustomerCount > 0 ? repeatCustomerCount / uniqueCustomerCount : 0,
    paidConversionRate: orders.length > 0 ? paidOrders.length / orders.length : 0,
  };
}

function normalizeAmount(value: number | string | null | undefined) {
  const amount = Number(value ?? 0);
  return Number.isFinite(amount) ? amount : 0;
}
