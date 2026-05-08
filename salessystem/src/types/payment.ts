export type PaymentChannelCode = 'ALIPAY_PAGE' | 'EXT_PROVIDER';

export interface PaymentBill {
  id: number;
  billNo: string;
  bizType?: string | null;
  bizNo?: string | null;
  tenantId?: number | null;
  platformUserId?: number | null;
  channelCode?: string | null;
  channelMode?: string | null;
  payAmount?: number | null;
  payStatus?: string | null;
  thirdPartyBillNo?: string | null;
  callbackStatus?: string | null;
  statusRemark?: string | null;
  expireTime?: string | null;
  extensionJson?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

