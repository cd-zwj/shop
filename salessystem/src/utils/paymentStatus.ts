import type { PaymentBill } from '../types/payment';

export type PaymentFlowState =
  | 'pending'
  | 'success'
  | 'failed'
  | 'closed'
  | 'expired';

export interface PaymentStatusPresentation {
  state: PaymentFlowState;
  title: string;
  description: string;
  badgeLabel: string;
}

function isExpired(expireTime?: string | null) {
  if (!expireTime) {
    return false;
  }

  const expireAt = new Date(expireTime).getTime();
  if (Number.isNaN(expireAt)) {
    return false;
  }

  return expireAt <= Date.now();
}

export function resolvePaymentFlowState(paymentBill?: Pick<PaymentBill, 'payStatus' | 'expireTime'> | null): PaymentFlowState {
  const payStatus = paymentBill?.payStatus;

  if (payStatus === 'SUCCESS') {
    return 'success';
  }

  if (payStatus === 'FAILED') {
    return 'failed';
  }

  if (payStatus === 'CLOSED') {
    return isExpired(paymentBill?.expireTime) ? 'expired' : 'closed';
  }

  if ((payStatus === 'WAIT_PAY' || payStatus === 'PAYING') && isExpired(paymentBill?.expireTime)) {
    return 'expired';
  }

  return 'pending';
}

export function getPaymentStatusPresentation(paymentBill?: Pick<PaymentBill, 'payStatus' | 'expireTime' | 'statusRemark'> | null): PaymentStatusPresentation {
  const state = resolvePaymentFlowState(paymentBill);

  switch (state) {
    case 'success':
      return {
        state,
        title: '支付已确认',
        description: '本次支付已经成功确认，可以继续查看订单或钱包结果。',
        badgeLabel: '支付成功',
      };
    case 'failed':
      return {
        state,
        title: '支付失败',
        description: '本次支付未成功完成，请回到订单重新发起支付。',
        badgeLabel: '支付失败',
      };
    case 'closed':
      return {
        state,
        title: '支付单已关闭',
        description: '当前支付单已经关闭，原链接通常不可继续使用，请重新发起支付。',
        badgeLabel: '支付已关闭',
      };
    case 'expired':
      return {
        state,
        title: '支付已过期',
        description: '当前支付单已超过可支付时间，请重新发起支付以生成新的支付单。',
        badgeLabel: '支付已过期',
      };
    default:
      return {
        state,
        title: '交易确认中',
        description: '支付结果仍在同步中，页面会自动刷新，也可以手动刷新状态。',
        badgeLabel: '等待支付结果',
      };
  }
}

export function getPaymentBillReuseHint(reusedPaymentBill?: boolean | null) {
  if (reusedPaymentBill === true) {
    return '本次继续支付复用了之前仍有效的支付单，可直接继续当前支付链接。';
  }

  if (reusedPaymentBill === false) {
    return '之前的支付单已失效或不可复用，本次已为你新建支付单。';
  }

  return '';
}
