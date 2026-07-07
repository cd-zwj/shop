import type { MerchantOrder } from '../types/merchant';

const CSV_HEADERS = ['订单号', '订单主题', '用户ID', '订单状态', '支付状态', '订单金额', '来源', '创建时间'];
const DANGEROUS_SPREADSHEET_PREFIX = /^[=+\-@]/;

export function buildMerchantOrdersCsv(orders: MerchantOrder[]) {
  const rows = orders.map((order) => [
    order.orderNo,
    order.subject,
    order.platformUserId,
    order.orderStatus,
    order.payStatus,
    typeof order.totalAmount === 'number' ? order.totalAmount.toFixed(2) : order.totalAmount,
    order.source,
    order.createTime,
  ]);

  const lines = [CSV_HEADERS, ...rows].map((row) => row.map(formatCsvCell).join(','));
  return `\ufeff${lines.join('\r\n')}\r\n`;
}

export function downloadMerchantOrdersCsv(orders: MerchantOrder[], filename = defaultExportFilename()) {
  if (orders.length === 0) {
    return false;
  }

  const blob = new Blob([buildMerchantOrdersCsv(orders)], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  return true;
}

function defaultExportFilename() {
  return `merchant-orders-${new Date().toISOString().slice(0, 10)}.csv`;
}

function formatCsvCell(value: unknown) {
  const rawValue = value == null ? '' : String(value);
  const safeValue = DANGEROUS_SPREADSHEET_PREFIX.test(rawValue) ? `'${rawValue}` : rawValue;
  return `"${safeValue.replaceAll('"', '""')}"`;
}
