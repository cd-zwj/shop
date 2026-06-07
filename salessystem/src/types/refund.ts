export interface RefundCreateDTO {
  orderNo: string;
  refundType: 'REFUND_ONLY' | 'RETURN_REFUND';
  refundAmount: number;
  reason: string;
  description?: string;
}

export interface Refund {
  id: number;
  refundNo: string;
  orderNo: string;
  refundType: string; // REFUND_ONLY | RETURN_REFUND
  refundStatus: string; // PENDING | APPROVED | REJECTED | COMPLETED | CANCELLED
  refundAmount: number;
  reason: string;
  description: string | null;
  rejectReason: string | null;
  auditTime: string | null;
  completeTime: string | null;
  createTime: string;
}
