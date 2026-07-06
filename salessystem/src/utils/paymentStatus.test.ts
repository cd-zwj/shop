import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  resolvePaymentFlowState,
  getPaymentStatusPresentation,
  getPaymentBillReuseHint,
  resolvePaymentBizTypeFromSource,
} from './paymentStatus';

describe('resolvePaymentFlowState', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-06-09T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('payStatus 为 SUCCESS 时应返回 success', () => {
    // Arrange
    const bill = { payStatus: 'SUCCESS', expireTime: null };

    // Act
    const state = resolvePaymentFlowState(bill);

    // Assert
    expect(state).toBe('success');
  });

  it('payStatus 为 FAILED 时应返回 failed', () => {
    // Arrange
    const bill = { payStatus: 'FAILED', expireTime: null };

    // Act
    const state = resolvePaymentFlowState(bill);

    // Assert
    expect(state).toBe('failed');
  });

  it('payStatus 为 CLOSED 且未过期时应返回 closed', () => {
    // Arrange — expireTime 在未来
    const bill = { payStatus: 'CLOSED', expireTime: '2026-06-10T00:00:00Z' };

    // Act
    const state = resolvePaymentFlowState(bill);

    // Assert
    expect(state).toBe('closed');
  });

  it('payStatus 为 CLOSED 且已过期时应返回 expired', () => {
    // Arrange — expireTime 在过去
    const bill = { payStatus: 'CLOSED', expireTime: '2026-06-01T00:00:00Z' };

    // Act
    const state = resolvePaymentFlowState(bill);

    // Assert
    expect(state).toBe('expired');
  });

  it('payStatus 为 WAIT_PAY 且已过期时应返回 expired', () => {
    // Arrange
    const bill = { payStatus: 'WAIT_PAY', expireTime: '2026-06-01T00:00:00Z' };

    // Act
    const state = resolvePaymentFlowState(bill);

    // Assert
    expect(state).toBe('expired');
  });

  it('payStatus 为 PAYING 且已过期时应返回 expired', () => {
    // Arrange
    const bill = { payStatus: 'PAYING', expireTime: '2026-06-01T00:00:00Z' };

    // Act
    const state = resolvePaymentFlowState(bill);

    // Assert
    expect(state).toBe('expired');
  });

  it('payStatus 为 WAIT_PAY 且未过期时应返回 pending', () => {
    // Arrange
    const bill = { payStatus: 'WAIT_PAY', expireTime: '2026-06-10T00:00:00Z' };

    // Act
    const state = resolvePaymentFlowState(bill);

    // Assert
    expect(state).toBe('pending');
  });

  it('null 或 undefined bill 应返回 pending', () => {
    // Arrange & Act & Assert
    expect(resolvePaymentFlowState(null)).toBe('pending');
    expect(resolvePaymentFlowState(undefined)).toBe('pending');
  });

  it('expireTime 为无效日期字符串时应返回 pending（不视为过期）', () => {
    // Arrange
    const bill = { payStatus: 'WAIT_PAY', expireTime: 'invalid-date' };

    // Act
    const state = resolvePaymentFlowState(bill);

    // Assert
    expect(state).toBe('pending');
  });
});

describe('getPaymentStatusPresentation', () => {
  it('success 状态应返回正确的标题和标签', () => {
    // Arrange
    const bill = { payStatus: 'SUCCESS', expireTime: null, statusRemark: null };

    // Act
    const presentation = getPaymentStatusPresentation(bill);

    // Assert
    expect(presentation.state).toBe('success');
    expect(presentation.title).toBe('支付已确认');
    expect(presentation.badgeLabel).toBe('支付成功');
  });

  it('failed 状态应返回正确的标题和标签', () => {
    // Arrange
    const bill = { payStatus: 'FAILED', expireTime: null, statusRemark: null };

    // Act
    const presentation = getPaymentStatusPresentation(bill);

    // Assert
    expect(presentation.state).toBe('failed');
    expect(presentation.title).toBe('支付失败');
    expect(presentation.badgeLabel).toBe('支付失败');
  });

  it('failed 状态应优先展示后端失败原因和重新支付提示', () => {
    const presentation = getPaymentStatusPresentation({
      payStatus: 'FAILED',
      expireTime: null,
      statusRemark: '渠道返回：余额不足',
    });

    expect(presentation.description).toContain('渠道返回：余额不足');
    expect(presentation.nextStep).toContain('重新发起支付');
  });

  it('pending 状态应返回等待中的描述', () => {
    // Arrange
    const bill = { payStatus: 'WAIT_PAY', expireTime: '2026-12-31T00:00:00Z', statusRemark: null };

    // Act
    const presentation = getPaymentStatusPresentation(bill);

    // Assert
    expect(presentation.state).toBe('pending');
    expect(presentation.badgeLabel).toBe('等待支付结果');
  });

  it('null bill 应返回 pending 状态的展示', () => {
    // Arrange & Act
    const presentation = getPaymentStatusPresentation(null);

    // Assert
    expect(presentation.state).toBe('pending');
    expect(presentation.title).toBe('交易确认中');
  });
});

describe('getPaymentBillReuseHint', () => {
  it('复用时应返回复用提示', () => {
    // Arrange & Act
    const hint = getPaymentBillReuseHint(true);

    // Assert
    expect(hint).toContain('复用');
  });

  it('未复用时应返回新建提示', () => {
    // Arrange & Act
    const hint = getPaymentBillReuseHint(false);

    // Assert
    expect(hint).toContain('新建支付单');
  });

  it('null 或 undefined 时应返回空字符串', () => {
    // Arrange & Act & Assert
    expect(getPaymentBillReuseHint(null)).toBe('');
    expect(getPaymentBillReuseHint(undefined)).toBe('');
  });
});

describe('resolvePaymentBizTypeFromSource', () => {
  it('maps recharge status links to RECHARGE payment bills', () => {
    expect(resolvePaymentBizTypeFromSource('recharge')).toBe('RECHARGE');
    expect(resolvePaymentBizTypeFromSource('merchant-recharge')).toBe('RECHARGE');
  });

  it('defaults order and unknown links to ORDER payment bills', () => {
    expect(resolvePaymentBizTypeFromSource('order')).toBe('ORDER');
    expect(resolvePaymentBizTypeFromSource(null)).toBe('ORDER');
    expect(resolvePaymentBizTypeFromSource('unknown')).toBe('ORDER');
  });
});
