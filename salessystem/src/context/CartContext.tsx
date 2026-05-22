import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import type { Product } from '../types/catalog';
import type { CartItem } from '../types/cart';

const CART_STORAGE_KEY = 'sales_system_cart_items';

interface CartContextValue {
  items: CartItem[];
  totalItems: number;
  addItem: (product: Product, quantity?: number) => void;
  addCartItems: (nextItems: CartItem[]) => void;
  updateQuantity: (productId: number, quantity: number) => void;
  removeItem: (productId: number) => void;
  clearCart: () => void;
  clearTenantItems: (tenantId: number) => void;
}

const CartContext = createContext<CartContextValue | null>(null);

function getStorage() {
  if (typeof window === 'undefined') {
    return null;
  }

  return window.localStorage;
}

function clampQuantity(quantity: number, stock?: number | null) {
  const safeQuantity = Math.max(1, Math.floor(quantity));

  if (typeof stock === 'number' && stock > 0) {
    return Math.min(safeQuantity, stock);
  }

  return safeQuantity;
}

function toCartItem(product: Product, quantity: number): CartItem {
  return {
    productId: product.id,
    tenantId: product.tenantId,
    name: product.name,
    price: product.price,
    quantity: clampQuantity(quantity, product.stock),
    imageUrl: product.imageUrl,
    stock: product.stock,
    category: product.category,
  };
}

function readCartItems(): CartItem[] {
  const storage = getStorage();
  const raw = storage?.getItem(CART_STORAGE_KEY);

  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return [];
    }

    return parsed
      .filter((item) => typeof item?.productId === 'number' && typeof item?.tenantId === 'number')
      .map((item) => ({
        productId: item.productId,
        tenantId: item.tenantId,
        name: typeof item.name === 'string' ? item.name : '未命名商品',
        price: Number(item.price) || 0,
        quantity: clampQuantity(Number(item.quantity) || 1, item.stock),
        imageUrl: typeof item.imageUrl === 'string' ? item.imageUrl : null,
        stock: typeof item.stock === 'number' ? item.stock : null,
        category: typeof item.category === 'string' ? item.category : null,
      }));
  } catch {
    return [];
  }
}

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>(() => readCartItems());

  useEffect(() => {
    const storage = getStorage();
    storage?.setItem(CART_STORAGE_KEY, JSON.stringify(items));
  }, [items]);

  function addItem(product: Product, quantity = 1) {
    setItems((currentItems) => {
      const existingItem = currentItems.find((item) => item.productId === product.id);

      if (!existingItem) {
        return [...currentItems, toCartItem(product, quantity)];
      }

      return currentItems.map((item) =>
        item.productId === product.id
          ? {
              ...item,
              name: product.name,
              price: product.price,
              imageUrl: product.imageUrl,
              stock: product.stock,
              category: product.category,
              quantity: clampQuantity(item.quantity + quantity, product.stock),
            }
          : item,
      );
    });
  }

  function addCartItems(nextItems: CartItem[]) {
    setItems((currentItems) => {
      const mergedItems = [...currentItems];

      nextItems.forEach((nextItem) => {
        const existingIndex = mergedItems.findIndex(
          (item) => item.productId === nextItem.productId,
        );

        if (existingIndex === -1) {
          mergedItems.push({
            ...nextItem,
            quantity: clampQuantity(nextItem.quantity, nextItem.stock),
          });
          return;
        }

        const existingItem = mergedItems[existingIndex];
        mergedItems[existingIndex] = {
          ...existingItem,
          ...nextItem,
          quantity: clampQuantity(
            existingItem.quantity + nextItem.quantity,
            nextItem.stock ?? existingItem.stock,
          ),
        };
      });

      return mergedItems;
    });
  }

  function updateQuantity(productId: number, quantity: number) {
    setItems((currentItems) =>
      currentItems.map((item) =>
        item.productId === productId
          ? {
              ...item,
              quantity: clampQuantity(quantity, item.stock),
            }
          : item,
      ),
    );
  }

  function removeItem(productId: number) {
    setItems((currentItems) => currentItems.filter((item) => item.productId !== productId));
  }

  function clearCart() {
    setItems([]);
  }

  function clearTenantItems(tenantId: number) {
    setItems((currentItems) => currentItems.filter((item) => item.tenantId !== tenantId));
  }

  const value = useMemo<CartContextValue>(
    () => ({
      items,
      totalItems: items.reduce((sum, item) => sum + item.quantity, 0),
      addItem,
      addCartItems,
      updateQuantity,
      removeItem,
      clearCart,
      clearTenantItems,
    }),
    [items],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }

  return context;
}
