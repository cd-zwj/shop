export interface CartItem {
  productId: number;
  tenantId: number;
  name: string;
  price: number;
  quantity: number;
  imageUrl?: string | null;
  stock?: number | null;
  category?: string | null;
  productType?: string | null;
  fulfillmentMode?: string | null;
}

export type CheckoutSource = 'APP_CART' | 'APP_BUY_NOW';
