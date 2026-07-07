import { describe, expect, it } from 'vitest';
import { validateProductDeliveryConfig } from './productDeliveryConfigValidation';

describe('productDeliveryConfigValidation', () => {
  it('requires virtual products to provide contentUrl or accountInfo', () => {
    const result = validateProductDeliveryConfig('VIRTUAL', '{"note":"missing"}');

    expect(result).toEqual({
      valid: false,
      message: '虚拟内容商品必须配置 contentUrl 或 accountInfo',
    });
  });

  it('rejects invalid delivery config JSON', () => {
    const result = validateProductDeliveryConfig('VIRTUAL', '{bad json');

    expect(result).toEqual({
      valid: false,
      message: '交付配置必须是合法 JSON',
    });
  });

  it('accepts virtual contentUrl delivery config', () => {
    const result = validateProductDeliveryConfig('VIRTUAL', '{"contentUrl":"https://example.com/file.zip"}');

    expect(result.valid).toBe(true);
  });

  it('requires positive integer validityDays for subscriptions', () => {
    const result = validateProductDeliveryConfig('SUBSCRIPTION', '{"validityDays":0}');

    expect(result).toEqual({
      valid: false,
      message: '订阅商品 validityDays 必须是大于 0 的整数',
    });
  });

  it('allows blank subscription delivery config because backend applies a default validity period', () => {
    const result = validateProductDeliveryConfig('SUBSCRIPTION', '');

    expect(result.valid).toBe(true);
  });

  it('does not require delivery config for card-key products', () => {
    const result = validateProductDeliveryConfig('CARD_KEY', '');

    expect(result.valid).toBe(true);
  });
});
