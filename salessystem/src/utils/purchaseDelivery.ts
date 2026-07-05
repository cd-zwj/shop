import type { ProductType, PurchaseRecord } from '../services/modules/appPurchases';

const PRODUCT_TYPE_LABEL: Record<ProductType, string> = {
  PHYSICAL: '实物',
  VIRTUAL: '虚拟内容',
  CARD_KEY: '兑换码',
  SERVICE: '服务',
  SUBSCRIPTION: '订阅',
};

export interface PurchaseDeliveryAction {
  kind: 'open' | 'copy' | 'confirm' | 'contact';
  label: string;
  value?: string;
}

export interface PurchaseDeliveryPresentation {
  title: string;
  subtitle: string;
  guidance: string;
  primaryAction?: PurchaseDeliveryAction;
}

export function parseDeliveryPayload(payload?: string | null): Record<string, unknown> | null {
  if (!payload) return null;
  try {
    const value = JSON.parse(payload);
    return typeof value === 'object' && value !== null ? (value as Record<string, unknown>) : null;
  } catch {
    return null;
  }
}

export function getProductTypeLabel(productType: ProductType) {
  return PRODUCT_TYPE_LABEL[productType] ?? productType;
}

export function getPurchaseDeliveryPresentation(record: PurchaseRecord): PurchaseDeliveryPresentation {
  const payload = parseDeliveryPayload(record.payload);
  const typeLabel = getProductTypeLabel(record.productType);
  const title = record.productName?.trim() || `${typeLabel} #${record.productId}`;
  const subtitle = `${typeLabel} · 订单 ${record.orderNo}`;

  if (record.status === 'FAILED' || record.status === 'REVOKE_FAILED') {
    return {
      title,
      subtitle,
      guidance: record.failReason ? `处理失败：${record.failReason}` : '交付或撤销处理失败，请联系商户处理。',
      primaryAction: { kind: 'contact', label: '联系商户' },
    };
  }

  if (record.status === 'PENDING' || record.status === 'DELIVERING') {
    return {
      title,
      subtitle,
      guidance: record.productType === 'PHYSICAL' ? '等待商户填写物流信息。' : '系统正在准备交付内容，请稍后刷新查看。',
    };
  }

  if (record.status === 'REVOKED') {
    return {
      title,
      subtitle,
      guidance: '交付内容已因退款或售后被撤销。',
    };
  }

  if (record.productType === 'VIRTUAL') {
    const url = stringValue(payload?.contentUrl);
    return {
      title,
      subtitle,
      guidance: url ? '内容已交付，可随时重新打开查看。' : '内容已交付，请查看下方账号或补充信息。',
      primaryAction: url ? { kind: 'open', label: '重新查看内容', value: url } : undefined,
    };
  }

  if (record.productType === 'CARD_KEY') {
    const code = stringValue(payload?.code);
    return {
      title,
      subtitle,
      guidance: code ? '兑换码已交付，可重新复制使用。' : '兑换码交付记录缺少可复制内容，请联系商户。',
      primaryAction: code ? { kind: 'copy', label: '复制兑换码', value: code } : undefined,
    };
  }

  if (record.productType === 'SERVICE') {
    const code = stringValue(payload?.verifyCode);
    return {
      title,
      subtitle,
      guidance: code ? '到店或服务履约时向商户出示核销码。' : '服务交付记录缺少核销码，请联系商户。',
      primaryAction: code ? { kind: 'copy', label: '复制核销码', value: code } : undefined,
    };
  }

  if (record.productType === 'SUBSCRIPTION') {
    return {
      title,
      subtitle,
      guidance: record.expireTime ? '订阅权益已激活，可在到期前持续使用。' : '订阅权益已激活。',
    };
  }

  return {
    title,
    subtitle,
    guidance: record.status === 'DELIVERED' ? '物流信息已更新，请关注签收。' : '该实物商品正在履约。',
    primaryAction: record.status === 'DELIVERED' ? { kind: 'confirm', label: '确认收货' } : undefined,
  };
}

function stringValue(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}
