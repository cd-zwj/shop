import { request } from '../request';
import type { PaymentBill } from '../../types/payment';

export const appPaymentBillService = {
  getPaymentBill(billNo: string) {
    return request<PaymentBill>({
      url: `/v1/app/payment-bills/${billNo}`,
      method: 'get',
      authRole: 'user',
    });
  },

  syncPaymentBill(billNo: string) {
    return request<PaymentBill>({
      url: `/v1/app/payment-bills/${billNo}/sync`,
      method: 'post',
      authRole: 'user',
    });
  },
};
