export function formatCurrency(value: number | string | null | undefined) {
  const amount = Number(value ?? 0);
  if (Number.isNaN(amount)) {
    return '¥0.00';
  }

  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
  }).format(amount);
}

/**
 * 格式化以“分”为单位的金额（商品目录价 ProductVO.price、购物车 CartItem.price、
 * 购物车小计均为分）。订单、钱包、退款等后端 BigDecimal 金额单位是元，
 * 请使用 formatCurrency。
 */
export function formatCurrencyFen(value: number | string | null | undefined) {
  const fen = Number(value ?? 0);
  if (Number.isNaN(fen)) {
    return '¥0.00';
  }
  return formatCurrency(fen / 100);
}

export function getImageUrl(imageUrl?: string | null, fallback?: string) {
  if (imageUrl?.trim()) {
    return imageUrl;
  }

  return (
    fallback ??
    'https://images.unsplash.com/photo-1556740749-887f6717d7e4?auto=format&fit=crop&w=900&q=80'
  );
}

