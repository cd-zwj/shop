import { describe, expect, it } from 'vitest';
import {
  getRefundProgressPresentation,
  getRefundStatusLabel,
  isRefundApplicationActive,
} from './refundProgress';

describe('refundProgress', () => {
  it('describes pending refund review node', () => {
    const presentation = getRefundProgressPresentation({ refundStatus: 'PENDING' });

    expect(presentation.label).toBe('待商家审核');
    expect(presentation.nextStep).toContain('商家审核');
  });

  it('shows reject reason for rejected refunds', () => {
    const presentation = getRefundProgressPresentation({
      refundStatus: 'REJECTED',
      rejectReason: '超过售后期',
    });

    expect(presentation.label).toBe('已驳回');
    expect(presentation.description).toContain('超过售后期');
    expect(presentation.tone).toBe('red');
  });

  it('shows processing node for approved refunds', () => {
    const presentation = getRefundProgressPresentation({
      refundStatus: 'APPROVED',
      refundSuggestion: '同意后需先撤销交付再退款',
    });

    expect(presentation.label).toBe('退款处理中');
    expect(presentation.nextStep).toContain('内部退款单');
    expect(presentation.description).toContain('撤销交付');
  });

  it('treats processing refunds as active so users cannot submit duplicate applications', () => {
    expect(isRefundApplicationActive({ refundStatus: 'PROCESSING' })).toBe(true);
    expect(isRefundApplicationActive({ refundStatus: 'APPROVED' })).toBe(true);
    expect(isRefundApplicationActive({ refundStatus: 'COMPLETED' })).toBe(true);
  });

  it('allows retrying after failed or rejected refunds while preserving clear labels', () => {
    expect(isRefundApplicationActive({ refundStatus: 'FAILED' })).toBe(false);
    expect(isRefundApplicationActive({ refundStatus: 'REJECTED' })).toBe(false);
    expect(getRefundStatusLabel('FAILED')).toBe('退款失败');
    expect(getRefundStatusLabel('PROCESSING')).toBe('退款处理中');
  });

  it('shows failure reason and merchant follow-up for failed refunds', () => {
    const presentation = getRefundProgressPresentation({
      refundStatus: 'FAILED',
      rejectReason: '交付撤销失败',
    });

    expect(presentation.label).toBe('退款失败');
    expect(presentation.description).toContain('交付撤销失败');
    expect(presentation.nextStep).toContain('联系商户');
    expect(presentation.tone).toBe('red');
  });
});
