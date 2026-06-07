import { request } from '../request';

export interface OpenPaymentBillStatus {
  billNo: string;
  payStatus: string;
}

export const openPaymentService = {
  getBillStatus(billNo: string) {
    return request<OpenPaymentBillStatus>({
      url: `/v1/open/payments/bills/${billNo}/status`,
      method: 'get',
      authRole: false,
    });
  },
};
