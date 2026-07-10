import type { Refund } from '../types/refund';

const CSV_HEADERS = ['退款单号', '订单号', '订单项', '退款类型', '退款状态', '退款金额', '交付状态', '失败原因', '申请原因', '创建时间'];

export function buildMerchantRefundsCsv(refunds: Refund[]) {
  const rows = refunds.map((refund) => [
    refund.refundNo,
    refund.orderNo,
    refund.orderItemId == null ? '' : String(refund.orderItemId),
    refund.refundType,
    refund.refundStatus,
    Number(refund.refundAmount || 0).toFixed(2),
    refund.deliveryStatus || '',
    refund.failureReason || refund.rejectReason || '',
    refund.reason || '',
    refund.createTime || '',
  ]);

  const lines = [CSV_HEADERS, ...rows].map((row) => row.map(formatCsvCell).join(','));
  return `\ufeff${lines.join('\n')}`;
}

export function downloadMerchantRefundsCsv(refunds: Refund[], filename = defaultExportFilename()) {
  if (refunds.length === 0 || typeof document === 'undefined') {
    return false;
  }

  const blob = new Blob([buildMerchantRefundsCsv(refunds)], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
  return true;
}

function formatCsvCell(value: string) {
  const safeValue = escapeSpreadsheetFormula(value ?? '');
  return `"${safeValue.replace(/"/g, '""')}"`;
}

function escapeSpreadsheetFormula(value: string) {
  return /^[=+\-@]/.test(value) ? `'${value}` : value;
}

function defaultExportFilename() {
  return `merchant-refunds-${new Date().toISOString().slice(0, 10)}.csv`;
}
