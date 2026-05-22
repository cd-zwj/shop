import { useEffect, useMemo, useState } from 'react';
import { motion } from 'motion/react';
import {
  ArrowRight,
  Minus,
  Plus,
  ShoppingBag,
  Store,
  Trash2,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { appCatalogService } from '../services/modules/appCatalog';
import {
  createOrderForItems,
  getOrderCheckoutPath,
} from '../services/orderCheckout';
import { ApiError } from '../types/api';
import { formatCurrency, getImageUrl } from '../utils/display';

export default function Cart() {
  const navigate = useNavigate();
  const { currentRole } = useAuth();
  const {
    items,
    totalItems,
    updateQuantity,
    removeItem,
    clearTenantItems,
  } = useCart();
  const [tenantNames, setTenantNames] = useState<Record<number, string>>({});
  const [isSubmittingTenantId, setIsSubmittingTenantId] = useState<number | null>(
    null,
  );
  const [error, setError] = useState('');

  useEffect(() => {
    const tenantIds = [...new Set<number>(items.map((item) => item.tenantId))].filter(
      (tenantId) => !tenantNames[tenantId],
    );

    if (tenantIds.length === 0) {
      return;
    }

    let isMounted = true;

    async function loadTenantNames() {
      const tenantEntries = await Promise.all(
        tenantIds.map(async (tenantId) => {
          try {
            const tenant = await appCatalogService.getTenant(tenantId);
            return [tenantId, tenant.name] as const;
          } catch {
            return [tenantId, `商户 #${tenantId}`] as const;
          }
        }),
      );

      if (!isMounted) {
        return;
      }

      setTenantNames((current) => ({
        ...current,
        ...Object.fromEntries(tenantEntries),
      }));
    }

    void loadTenantNames();

    return () => {
      isMounted = false;
    };
  }, [items, tenantNames]);

  const groupedItems = useMemo(() => {
    const groups = new Map<
      number,
      { tenantId: number; storeName: string; items: typeof items; subtotal: number }
    >();

    items.forEach((item) => {
      const existingGroup = groups.get(item.tenantId);
      const storeName = tenantNames[item.tenantId] ?? `商户 #${item.tenantId}`;

      if (existingGroup) {
        existingGroup.items.push(item);
        existingGroup.subtotal += item.price * item.quantity;
        return;
      }

      groups.set(item.tenantId, {
        tenantId: item.tenantId,
        storeName,
        items: [item],
        subtotal: item.price * item.quantity,
      });
    });

    return Array.from(groups.values());
  }, [items, tenantNames]);

  const subtotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0);

  async function handleCheckoutByTenant(tenantId: number) {
    const tenantItems = items.filter((item) => item.tenantId === tenantId);

    if (tenantItems.length === 0) {
      return;
    }

    if (currentRole !== 'user') {
      navigate('/login');
      return;
    }

    setError('');
    setIsSubmittingTenantId(tenantId);

    try {
      const payment = await createOrderForItems(tenantItems, 'APP_CART');
      clearTenantItems(tenantId);

      if (payment.externalPayUrl) {
        window.open(payment.externalPayUrl, '_blank', 'noopener,noreferrer');
      }

      navigate(getOrderCheckoutPath(payment));
    } catch (err) {
      setError(
        err instanceof ApiError || err instanceof Error
          ? err.message
          : '订单创建失败，请稍后重试',
      );
    } finally {
      setIsSubmittingTenantId(null);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 px-4 pb-40 md:mt-8">
      <header className="flex flex-col gap-2">
        <h1 className="text-4xl font-black tracking-tight text-slate-900">购物车</h1>
        <p className="font-medium text-slate-500">
          当前按商户分别结算，提交后会创建真实订单并进入支付状态页。
        </p>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-3xl border border-slate-100 bg-white py-20 shadow-inner">
          <ShoppingBag className="mb-4 h-16 w-16 text-slate-200" />
          <p className="font-bold text-slate-400">购物车空空如也</p>
          <button
            onClick={() => navigate('/discovery')}
            className="mt-4 font-bold text-primary hover:underline"
          >
            去逛逛
          </button>
        </div>
      ) : (
        <div className="flex flex-col gap-8">
          {groupedItems.map((group) => {
            const tenantItemCount = group.items.reduce(
              (sum, item) => sum + item.quantity,
              0,
            );
            const isSubmitting = isSubmittingTenantId === group.tenantId;

            return (
              <motion.section
                key={group.tenantId}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm"
              >
                <div className="flex items-center gap-3 border-b border-slate-200 bg-slate-50/80 p-5">
                  <div className="rounded-xl border border-slate-100 bg-white p-2 shadow-sm">
                    <Store className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <h2 className="text-lg font-black tracking-tight text-slate-900">
                      {group.storeName}
                    </h2>
                    <p className="text-xs font-bold uppercase tracking-widest text-slate-400">
                      {tenantItemCount} 件商品
                    </p>
                  </div>
                </div>

                <div className="flex flex-col divide-y divide-slate-100 p-2">
                  {group.items.map((item) => (
                    <article
                      key={item.productId}
                      className="group flex flex-col gap-5 p-4 sm:flex-row"
                    >
                      <div className="relative h-32 w-full shrink-0 overflow-hidden rounded-2xl border border-slate-100 bg-slate-50 shadow-inner sm:w-32">
                        <img
                          src={getImageUrl(item.imageUrl)}
                          alt={item.name}
                          className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-110"
                        />
                      </div>

                      <div className="flex flex-1 flex-col justify-between">
                        <div className="flex items-start justify-between gap-4">
                          <div>
                            <h3 className="mb-1 text-base font-bold text-slate-900 transition-colors group-hover:text-primary">
                              {item.name}
                            </h3>
                            <p className="text-xs font-semibold uppercase tracking-widest text-slate-400">
                              {item.category || '真实商品'}
                            </p>
                          </div>
                          <button
                            onClick={() => removeItem(item.productId)}
                            className="rounded-xl p-2 text-slate-300 transition-colors hover:bg-red-50 hover:text-red-500"
                          >
                            <Trash2 className="h-5 w-5" />
                          </button>
                        </div>

                        <div className="mt-6 flex items-end justify-between">
                          <div className="flex flex-col gap-1">
                            <span className="text-2xl font-black tracking-tight text-slate-900">
                              {formatCurrency(item.price)}
                            </span>
                            {typeof item.stock === 'number' && (
                              <span className="text-xs font-bold text-slate-400">
                                库存 {item.stock}
                              </span>
                            )}
                          </div>
                          <div className="flex items-center rounded-2xl border border-slate-200 bg-slate-100 p-1">
                            <button
                              onClick={() =>
                                updateQuantity(item.productId, item.quantity - 1)
                              }
                              disabled={item.quantity <= 1}
                              className="flex h-10 w-10 items-center justify-center rounded-xl text-slate-600 transition-all active:scale-90 hover:bg-white hover:shadow-sm disabled:cursor-not-allowed disabled:opacity-40"
                            >
                              <Minus className="h-4 w-4" />
                            </button>
                            <span className="w-12 text-center text-base font-black text-slate-900">
                              {item.quantity}
                            </span>
                            <button
                              onClick={() =>
                                updateQuantity(item.productId, item.quantity + 1)
                              }
                              disabled={
                                typeof item.stock === 'number' &&
                                item.stock > 0 &&
                                item.quantity >= item.stock
                              }
                              className="flex h-10 w-10 items-center justify-center rounded-xl text-slate-600 transition-all active:scale-90 hover:bg-white hover:shadow-sm disabled:cursor-not-allowed disabled:opacity-40"
                            >
                              <Plus className="h-4 w-4" />
                            </button>
                          </div>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>

                <div className="flex flex-col gap-4 border-t border-slate-100 bg-slate-50/60 p-5 md:flex-row md:items-center md:justify-between">
                  <div>
                    <p className="text-xs font-black uppercase tracking-widest text-slate-400">
                      商户小计
                    </p>
                    <p className="mt-1 text-2xl font-black tracking-tight text-slate-900">
                      {formatCurrency(group.subtotal)}
                    </p>
                  </div>
                  <button
                    onClick={() => handleCheckoutByTenant(group.tenantId)}
                    disabled={isSubmitting}
                    className="flex items-center justify-center gap-3 rounded-2xl bg-primary px-8 py-4 text-lg font-black text-white shadow-xl shadow-primary/20 transition-all active:scale-95 hover:scale-[1.02] disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    <span>{isSubmitting ? '创建订单中...' : '结算该商户商品'}</span>
                    <ArrowRight className="h-5 w-5" />
                  </button>
                </div>
              </motion.section>
            );
          })}
        </div>
      )}

      <div className="fixed bottom-20 left-1/2 z-40 w-full max-w-5xl -translate-x-1/2 px-4 md:bottom-6">
        <motion.div
          initial={{ y: 100 }}
          animate={{ y: 0 }}
          className="flex flex-col gap-4 rounded-3xl border border-slate-200 bg-white/95 p-6 shadow-2xl backdrop-blur-2xl sm:flex-row sm:items-center sm:justify-between"
        >
          <div className="flex flex-col items-center sm:items-start">
            <span className="text-sm font-semibold uppercase tracking-widest text-slate-400">
              总计 ({totalItems} 件商品 / {groupedItems.length} 个商户)
            </span>
            <div className="mt-1 flex items-baseline gap-1">
              <span className="text-3xl font-black tracking-tight text-slate-900">
                {formatCurrency(subtotal)}
              </span>
            </div>
          </div>

          <div className="text-center text-sm font-medium text-slate-500 sm:max-w-sm sm:text-right">
            当前不支持跨商户合单，请在上方卡片中分别结算各商户商品。
          </div>
        </motion.div>
      </div>
    </div>
  );
}
