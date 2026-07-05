import { describe, expect, it } from 'vitest';
import { getRefundProgressPresentation } from './refundProgress';

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
});
