export interface RefundCreateDTO {
  orderNo: string;
  orderItemId?: number | null;
  refundType: 'REFUND_ONLY' | 'RETURN_REFUND';
  refundAmount: number;
  reason: string;
  description?: string;
}

export interface Refund {
  id: number;
  refundNo: string;
  orderNo: string;
  orderItemId: number | null;
  refundType: string; // REFUND_ONLY | RETURN_REFUND
  refundStatus: string; // PENDING | APPROVED | PROCESSING | COMPLETED | FAILED | REJECTED | CANCELLED
  refundAmount: number;
  deliveryStatus: string | null;
  refundableAmount: number | null;
  quickRefundSuggested: boolean | null;
  refundSuggestion: string | null;
  statusLabel?: string | null;
  statusDescription?: string | null;
  nextStep?: string | null;
  failureReason?: string | null;
  availableActions?: string[] | null;
  reason: string;
  description: string | null;
  rejectReason: string | null;
  auditTime: string | null;
  completeTime: string | null;
  createTime: string;
}
