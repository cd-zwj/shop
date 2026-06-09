import { describe, it, expect } from 'vitest';
import { formatCurrency, getImageUrl } from './display';

describe('formatCurrency', () => {
  it('正整数应返回正确的人民币格式', () => {
    // Arrange
    const value = 100;

    // Act
    const result = formatCurrency(value);

    // Assert
    expect(result).toBe('¥100.00');
  });

  it('小数应正确格式化', () => {
    // Arrange
    const value = 123.45;

    // Act
    const result = formatCurrency(value);

    // Assert
    expect(result).toBe('¥123.45');
  });

  it('零应返回 ¥0.00', () => {
    // Arrange
    const value = 0;

    // Act
    const result = formatCurrency(value);

    // Assert
    expect(result).toBe('¥0.00');
  });

  it('null 应视为 0', () => {
    // Arrange
    const value = null;

    // Act
    const result = formatCurrency(value);

    // Assert
    expect(result).toBe('¥0.00');
  });

  it('undefined 应视为 0', () => {
    // Arrange
    const value = undefined;

    // Act
    const result = formatCurrency(value);

    // Assert
    expect(result).toBe('¥0.00');
  });

  it('NaN 字符串应返回 ¥0.00', () => {
    // Arrange
    const value = 'abc';

    // Act
    const result = formatCurrency(value);

    // Assert
    expect(result).toBe('¥0.00');
  });

  it('数字字符串应正确解析', () => {
    // Arrange
    const value = '99.9';

    // Act
    const result = formatCurrency(value);

    // Assert
    expect(result).toBe('¥99.90');
  });

  it('大数应正确格式化（含千分位）', () => {
    // Arrange
    const value = 1234567.89;

    // Act
    const result = formatCurrency(value);

    // Assert
    expect(result).toContain('1,234,567.89');
  });
});

describe('getImageUrl', () => {
  it('有值时应返回原始 URL', () => {
    // Arrange
    const url = 'https://example.com/image.png';

    // Act
    const result = getImageUrl(url);

    // Assert
    expect(result).toBe(url);
  });

  it('空字符串应返回默认 fallback', () => {
    // Arrange
    const url = '';

    // Act
    const result = getImageUrl(url);

    // Assert
    expect(result).toBe(
      'https://images.unsplash.com/photo-1556740749-887f6717d7e4?auto=format&fit=crop&w=900&q=80',
    );
  });

  it('null 应返回默认 fallback', () => {
    // Arrange
    const url = null;

    // Act
    const result = getImageUrl(url);

    // Assert
    expect(result).toContain('unsplash.com');
  });

  it('undefined 应返回默认 fallback', () => {
    // Arrange
    const url = undefined;

    // Act
    const result = getImageUrl(url);

    // Assert
    expect(result).toContain('unsplash.com');
  });

  it('空白字符串应返回默认 fallback', () => {
    // Arrange
    const url = '   ';

    // Act
    const result = getImageUrl(url);

    // Assert
    expect(result).toContain('unsplash.com');
  });

  it('有自定义 fallback 时应返回自定义 fallback', () => {
    // Arrange
    const customFallback = 'https://example.com/fallback.png';

    // Act
    const result = getImageUrl(undefined, customFallback);

    // Assert
    expect(result).toBe(customFallback);
  });
});
