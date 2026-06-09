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

export function getImageUrl(imageUrl?: string | null, fallback?: string) {
  if (imageUrl?.trim()) {
    return imageUrl;
  }

  return (
    fallback ??
    'https://images.unsplash.com/photo-1556740749-887f6717d7e4?auto=format&fit=crop&w=900&q=80'
  );
}

