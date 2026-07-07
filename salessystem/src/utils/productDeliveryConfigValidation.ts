import type { ProductType } from '../types/catalog';

export interface DeliveryConfigValidationResult {
  valid: boolean;
  message?: string;
}

export function validateProductDeliveryConfig(
  productType: ProductType | string | null | undefined,
  deliveryConfig: string | null | undefined,
): DeliveryConfigValidationResult {
  if (productType !== 'VIRTUAL' && productType !== 'SUBSCRIPTION') {
    return { valid: true };
  }

  const trimmed = deliveryConfig?.trim() || '';
  if (!trimmed) {
    if (productType === 'SUBSCRIPTION') {
      return { valid: true };
    }

    return {
      valid: false,
      message: '虚拟内容商品必须配置 contentUrl 或 accountInfo',
    };
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(trimmed);
  } catch {
    return { valid: false, message: '交付配置必须是合法 JSON' };
  }

  if (!isRecord(parsed)) {
    return { valid: false, message: '交付配置必须是 JSON 对象' };
  }

  if (productType === 'VIRTUAL') {
    const contentUrl = stringField(parsed.contentUrl);
    const accountInfo = stringField(parsed.accountInfo);
    if (!contentUrl && !accountInfo) {
      return { valid: false, message: '虚拟内容商品必须配置 contentUrl 或 accountInfo' };
    }
  }

  if (productType === 'SUBSCRIPTION') {
    const validityDays = Number(parsed.validityDays);
    if (!Number.isInteger(validityDays) || validityDays <= 0) {
      return { valid: false, message: '订阅商品 validityDays 必须是大于 0 的整数' };
    }
  }

  return { valid: true };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function stringField(value: unknown) {
  return typeof value === 'string' && value.trim() ? value.trim() : '';
}
