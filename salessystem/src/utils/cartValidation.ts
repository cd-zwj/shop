import type { CartItem } from '../types/cart';
import type { Product } from '../types/catalog';
import { formatCurrency } from './display';

export interface CartValidationIssue {
  productId: number;
  severity: 'warning' | 'blocking';
  message: string;
}

export interface CartValidationResult {
  refreshedItems: CartItem[];
  issues: CartValidationIssue[];
  hasIssues: boolean;
  hasBlockingIssues: boolean;
}

export async function validateCartItemsAgainstCatalog(
  items: CartItem[],
  loadProduct: (productId: number) => Promise<Product | null | undefined>,
): Promise<CartValidationResult> {
  const refreshedItems: CartItem[] = [];
  const issues: CartValidationIssue[] = [];

  for (const item of items) {
    let product: Product | null | undefined;
    try {
      product = await loadProduct(item.productId);
    } catch {
      product = null;
    }

    if (!product || !isActiveProduct(product)) {
      issues.push({
        productId: item.productId,
        severity: 'blocking',
        message: `${item.name} 已下架或不存在，已从购物车移除`,
      });
      continue;
    }

    if (typeof product.tenantId === 'number' && product.tenantId !== item.tenantId) {
      issues.push({
        productId: item.productId,
        severity: 'blocking',
        message: `${item.name} 商户归属已变化，已从购物车移除`,
      });
      continue;
    }

    if (typeof product.stock === 'number' && product.stock <= 0) {
      issues.push({
        productId: item.productId,
        severity: 'blocking',
        message: `${product.name} 当前无库存，已从购物车移除`,
      });
      continue;
    }

    if (product.price !== item.price) {
      issues.push({
        productId: item.productId,
        severity: 'warning',
        message: `${product.name} 价格已从 ${formatCurrency(item.price / 100)} 调整为 ${formatCurrency(product.price / 100)}`,
      });
    }

    let quantity = item.quantity;
    if (typeof product.stock === 'number' && quantity > product.stock) {
      quantity = product.stock;
      issues.push({
        productId: item.productId,
        severity: 'warning',
        message: `${product.name} 当前库存仅剩 ${product.stock} 件，已自动调整购买数量`,
      });
    }

    refreshedItems.push({
      ...item,
      name: product.name,
      price: product.price,
      quantity,
      imageUrl: product.imageUrl,
      stock: product.stock,
      category: product.category,
    });
  }

  return {
    refreshedItems,
    issues,
    hasIssues: issues.length > 0,
    hasBlockingIssues: issues.some((issue) => issue.severity === 'blocking'),
  };
}

function isActiveProduct(product: Product) {
  return product.status === undefined
    || product.status === null
    || product.status === 1
    || product.status === '1'
    || product.status === 'active';
}
