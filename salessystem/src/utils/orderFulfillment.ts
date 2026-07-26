import type { SalesOrderItem } from '../types/order';

export type FulfillmentTone = 'neutral' | 'warning' | 'success' | 'danger';

export interface OrderItemFulfillmentPresentation {
  label: string;
  description: string;
  tone: FulfillmentTone;
  actionLabel?: string;
  actionPath?: string;
}

export function getOrderItemFulfillmentPresentation(item: SalesOrderItem): OrderItemFulfillmentPresentation {
  switch (item.deliveryStatus) {
    case 'CONFIRMED':
      return { label: '已完成', description: '商家已确认备货完成，可进入售后或评价流程。', tone: 'success' };
    case 'DELIVERED':
      return { label: '待完成', description: '商品已生成取货凭证，等待商家完成备货。', tone: 'warning' };
    case 'DELIVERING':
      return { label: '备货中', description: '商家正在为该订单备货。', tone: 'warning' };
    case 'REVOKED':
      return { label: '已撤销', description: '该订单项已因退款或售后撤销。', tone: 'neutral' };
    case 'FAILED':
    case 'REVOKE_FAILED':
      return { label: '履约异常', description: '该订单项需要商家或平台进一步处理。', tone: 'danger' };
    default:
      return { label: '待备货', description: '支付成功后，商家会开始准备商品。', tone: 'warning' };
  }
}
