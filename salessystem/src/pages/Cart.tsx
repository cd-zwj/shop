import { useEffect, useMemo, useState, useRef, useCallback } from 'react';
import { motion } from 'motion/react';
import {
  ArrowRight,
  MapPin,
  Minus,
  Plus,
  ShoppingBag,
  Store,
  Trash2,
  Ticket,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useToast } from '../context/ToastContext';
import { appCatalogService } from '../services/modules/appCatalog';
import { appAddressService } from '../services/modules/appAddress';
import { appCouponService } from '../services/modules/appCoupon';
import {
  createOrderForItems,
  getOrderCheckoutPath,
  requiresShippingAddress,
} from '../services/orderCheckout';
import { ApiError } from '../types/api';
import type { Address } from '../types/addressNotification';
import type { CouponTemplate, UserCoupon } from '../types/coupon';
import { formatCurrency, getImageUrl } from '../utils/display';
import { openAlipayPaymentWindow, saveAlipayPaymentPayload } from '../utils/alipayPayment';
import { validateCartItemsAgainstCatalog } from '../utils/cartValidation';

const calculateDiscount = (coupon: { couponType: 'FIXED' | 'RATE'; discountAmount: number | null; discountRate: number | null; maxDiscountAmount: number | null }, subtotal: number) => {
  if (coupon.couponType === 'FIXED') {
    return coupon.discountAmount ?? 0;
  } else {
    const rate = coupon.discountRate ?? 1;
    const discount = subtotal * (1 - rate);
    if (coupon.maxDiscountAmount && discount > coupon.maxDiscountAmount) {
      return coupon.maxDiscountAmount;
    }
    return discount;
  }
};

function getStockValidationMessage(item: { name: string; stock?: number | null; quantity: number }) {
  if (typeof item.stock !== 'number') {
    return '';
  }
  if (item.stock <= 0) {
    return `${item.name} 当前无库存，请先移出购物车`;
  }
  if (item.quantity > item.stock) {
    return `${item.name} 当前库存仅剩 ${item.stock} 件，请调整购买数量`;
  }
  return '';
}

export default function Cart() {
  const navigate = useNavigate();
  const { currentRole } = useAuth();
  const { showToast } = useToast();
  const {
    items,
    totalItems,
    updateQuantity,
    removeItem,
    clearTenantItems,
    replaceTenantItems,
  } = useCart();
  const [tenantNames, setTenantNames] = useState<Record<number, string>>({});
  const [isSubmittingTenantId, setIsSubmittingTenantId] = useState<number | null>(
    null,
  );
  const [error, setError] = useState('');
  const [addressError, setAddressError] = useState('');
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [isLoadingAddresses, setIsLoadingAddresses] = useState(false);
  const [selectedAddressIdByTenant, setSelectedAddressIdByTenant] = useState<Record<number, number | undefined>>({});
  const [paymentMethod, setPaymentMethod] = useState<'ALIPAY_PAGE' | 'UNIFIED_WALLET'>('ALIPAY_PAGE');

  // Coupon states
  const [couponsByTenant, setCouponsByTenant] = useState<Record<number, {
    availableTemplates: CouponTemplate[];
    myUsableCoupons: UserCoupon[];
    isLoading: boolean;
  }>>({});

  const [selectedCouponByTenant, setSelectedCouponByTenant] = useState<Record<number, {
    key: string;
    type: 'TEMPLATE' | 'OWNED';
    id: number;
    name: string;
  } | null>>({});

  const loadedTenantsRef = useRef<Set<number>>(new Set());

  useEffect(() => {
    const tenantIds = [...new Set<number>(items.map((item) => item.tenantId))].filter(
      (tenantId) => !tenantNames[tenantId],
    );

    if (tenantIds.length === 0) {
      return undefined;
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

  // Fetch coupons for all merchants in the cart
  useEffect(() => {
    const activeTenantIds = [...new Set<number>(items.map((item) => item.tenantId))];
    
    // Sync loadedTenantsRef with active tenants to allow refetching if removed and re-added
    const activeSet = new Set(activeTenantIds);
    Array.from(loadedTenantsRef.current).forEach((id: number) => {
      if (!activeSet.has(id)) {
        loadedTenantsRef.current.delete(id);
      }
    });

    const tenantsToLoad = activeTenantIds.filter((id) => !loadedTenantsRef.current.has(id));

    if (tenantsToLoad.length === 0) return undefined;

    tenantsToLoad.forEach((id) => loadedTenantsRef.current.add(id));

    setCouponsByTenant((prev) => {
      const next = { ...prev };
      tenantsToLoad.forEach((id) => {
        next[id] = { availableTemplates: [], myUsableCoupons: [], isLoading: true };
      });
      return next;
    });

    let isMounted = true;

    async function loadCoupons() {
      await Promise.all(
        tenantsToLoad.map(async (tenantId) => {
          try {
            const [available, myCoupons] = await Promise.all([
              appCouponService.getAvailableCoupons(tenantId),
              appCouponService.getMyCoupons(tenantId, 'USABLE'),
            ]);
            if (!isMounted) return;
            setCouponsByTenant((prev) => ({
              ...prev,
              [tenantId]: { availableTemplates: available, myUsableCoupons: myCoupons, isLoading: false },
            }));
          } catch (e) {
            loadedTenantsRef.current.delete(tenantId);
            if (!isMounted) return;
            setCouponsByTenant((prev) => ({
              ...prev,
              [tenantId]: { availableTemplates: [], myUsableCoupons: [], isLoading: false },
            }));
          }
        })
      );
    }

    void loadCoupons();

    return () => {
      isMounted = false;
    };
  }, [items]);

  const getSelectableCoupons = useCallback((tenantId: number, subtotal: number) => {
    const data = couponsByTenant[tenantId];
    if (!data) return [];

    const options: Array<{
      key: string;
      id: number;
      type: 'OWNED' | 'TEMPLATE';
      name: string;
      desc: string;
      discountAmount: number;
      thresholdAmount: number;
      isUsable: boolean;
      reason?: string;
    }> = [];

    // 1. Add owned coupons
    data.myUsableCoupons.forEach((coupon) => {
      const isUsable = subtotal >= coupon.thresholdAmount;
      const discount = calculateDiscount(coupon, subtotal);
      
      options.push({
        key: `owned-${coupon.id}`,
        id: coupon.id,
        type: 'OWNED',
        name: coupon.name,
        desc: coupon.couponType === 'FIXED' 
          ? `满 ¥${coupon.thresholdAmount} 减 ¥${coupon.discountAmount}`
          : `满 ¥${coupon.thresholdAmount} 打 ${(coupon.discountRate ?? 1) * 10} 折`,
        discountAmount: discount,
        thresholdAmount: coupon.thresholdAmount,
        isUsable,
        reason: isUsable ? undefined : `还差 ¥${(coupon.thresholdAmount - subtotal).toFixed(2)}`
      });
    });

    // 2. Add available templates to claim
    data.availableTemplates.forEach((template) => {
      const hasOwnedUsable = data.myUsableCoupons.some((c) => c.couponTemplateId === template.id);
      if (hasOwnedUsable) return;

      const isUsable = subtotal >= template.thresholdAmount && template.receivable && template.remainingStock > 0;
      const discount = calculateDiscount(template, subtotal);

      let reason = undefined;
      if (subtotal < template.thresholdAmount) {
        reason = `还差 ¥${(template.thresholdAmount - subtotal).toFixed(2)}`;
      } else if (!template.receivable) {
        reason = '已领超限';
      } else if (template.remainingStock <= 0) {
        reason = '无库存';
      }

      options.push({
        key: `template-${template.id}`,
        id: template.id,
        type: 'TEMPLATE',
        name: `${template.name} (可领用)`,
        desc: template.couponType === 'FIXED' 
          ? `满 ¥${template.thresholdAmount} 减 ¥${template.discountAmount}`
          : `满 ¥${template.thresholdAmount} 打 ${(template.discountRate ?? 1) * 10} 折`,
        discountAmount: discount,
        thresholdAmount: template.thresholdAmount,
        isUsable,
        reason
      });
    });

    return options.sort((a, b) => {
      if (a.isUsable && !b.isUsable) return -1;
      if (!a.isUsable && b.isUsable) return 1;
      return b.discountAmount - a.discountAmount;
    });
  }, [couponsByTenant]);

  const getSelectedCouponDiscount = useCallback((tenantId: number, subtotal: number) => {
    const sel = selectedCouponByTenant[tenantId];
    if (!sel) return 0;
    const data = couponsByTenant[tenantId];
    if (!data) return 0;

    if (sel.type === 'OWNED') {
      const coupon = data.myUsableCoupons.find((c) => c.id === sel.id);
      if (!coupon || subtotal < coupon.thresholdAmount) return 0;
      return calculateDiscount(coupon, subtotal);
    } else {
      const template = data.availableTemplates.find((t) => t.id === sel.id);
      if (!template || subtotal < template.thresholdAmount) return 0;
      return calculateDiscount(template, subtotal);
    }
  }, [selectedCouponByTenant, couponsByTenant]);

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

  const shippingTenantIds = useMemo(
    () => groupedItems
      .filter((group) => requiresShippingAddress(group.items))
      .map((group) => group.tenantId),
    [groupedItems],
  );

  useEffect(() => {
    if (currentRole !== 'user' || shippingTenantIds.length === 0) {
      setAddressError('');
      return undefined;
    }

    let isMounted = true;
    setIsLoadingAddresses(true);

    async function loadAddresses() {
      try {
        const result = await appAddressService.list();
        if (!isMounted) return;

        setAddresses(result);
        setAddressError('');
        const defaultAddress = result.find((address) => address.isDefault === 1) ?? result[0];
        if (!defaultAddress) {
          return;
        }

        setSelectedAddressIdByTenant((prev) => {
          const next = { ...prev };
          shippingTenantIds.forEach((tenantId) => {
            if (!next[tenantId]) {
              next[tenantId] = defaultAddress.id;
            }
          });
          return next;
        });
      } catch {
        if (!isMounted) return;
        setAddressError('收货地址加载失败，请稍后重试或前往地址管理确认。');
      } finally {
        if (isMounted) {
          setIsLoadingAddresses(false);
        }
      }
    }

    void loadAddresses();

    return () => {
      isMounted = false;
    };
  }, [currentRole, shippingTenantIds.join(',')]);

  const subtotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0);

  const totalDiscount = useMemo(() => {
    return groupedItems.reduce((sum, group) => {
      return sum + getSelectedCouponDiscount(group.tenantId, group.subtotal);
    }, 0);
  }, [groupedItems, getSelectedCouponDiscount]);

  async function handleCheckoutByTenant(tenantId: number) {
    const tenantItems = items.filter((item) => item.tenantId === tenantId);

    if (tenantItems.length === 0) {
      return;
    }

    const stockError = tenantItems
      .map(getStockValidationMessage)
      .find((message) => message.length > 0);
    if (stockError) {
      setError(stockError);
      return;
    }

    const validation = await validateCartItemsAgainstCatalog(
      tenantItems,
      (productId) => appCatalogService.getProduct(productId),
    );
    if (validation.hasIssues) {
      replaceTenantItems(tenantId, validation.refreshedItems);
      setSelectedCouponByTenant((prev) => ({ ...prev, [tenantId]: null }));
      setError(`购物车已刷新：${validation.issues.map((issue) => issue.message).join('；')}。请确认后重新结算。`);
      return;
    }

    if (currentRole !== 'user') {
      navigate('/login');
      return;
    }

    const needsShippingAddress = requiresShippingAddress(validation.refreshedItems);
    const selectedAddressId = selectedAddressIdByTenant[tenantId];
    if (needsShippingAddress && !selectedAddressId) {
      setError('实物商品需要先选择收货地址，请新增或选择默认地址后再结算。');
      return;
    }

    setError('');
    setIsSubmittingTenantId(tenantId);

    try {
      let finalCouponId: number | undefined = undefined;
      const selectedCoupon = selectedCouponByTenant[tenantId];
      
      if (selectedCoupon) {
        const tenantSubtotal = tenantItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
        const discount = getSelectedCouponDiscount(tenantId, tenantSubtotal);
        
        if (discount > 0) {
          if (selectedCoupon.type === 'OWNED') {
            finalCouponId = selectedCoupon.id;
          } else if (selectedCoupon.type === 'TEMPLATE') {
            showToast('正在为您领取并绑定优惠券...', 'info');
            const claimResult = await appCouponService.claimCoupon(tenantId, selectedCoupon.id);
            finalCouponId = claimResult.userCouponId;
            showToast('优惠券领用成功！', 'success');
          }
        } else {
          showToast('所选优惠券不满足门槛条件或已失效', 'error');
          setIsSubmittingTenantId(null);
          return;
        }
      }

      const walletStrategy = paymentMethod === 'UNIFIED_WALLET' ? 'UNIFIED_ONLY' : 'NO_WALLET';
      const channelCode = paymentMethod === 'UNIFIED_WALLET' ? undefined : paymentMethod;
      const payment = await createOrderForItems(
        tenantItems,
        'APP_CART',
        finalCouponId,
        walletStrategy,
        channelCode,
        needsShippingAddress ? selectedAddressId : undefined,
      );
      clearTenantItems(tenantId);

      if (payment.externalPayUrl) {
        // 支付宝返回的是 HTML 表单，需要用 openAlipayPaymentWindow 渲染
        const isOpened = openAlipayPaymentWindow(payment.externalPayUrl);
        if (!isOpened) {
          // 弹窗被阻止，保存 payload 到 sessionStorage，让用户在支付状态页手动触发
          if (payment.paymentBillNo) {
            saveAlipayPaymentPayload({
              billNo: payment.paymentBillNo,
              orderNo: payment.orderNo,
              source: 'order',
              payHtml: payment.externalPayUrl,
              amount: payment.totalAmount,
            });
          }
        }
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

      {(error || addressError) && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error || addressError}
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
            const groupNeedsShipping = requiresShippingAddress(group.items);
            const selectedAddressId = selectedAddressIdByTenant[group.tenantId];
            const selectedAddress = addresses.find((address) => address.id === selectedAddressId);

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
                                item.stock >= 0 &&
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

                {groupNeedsShipping && (
                  <div className="flex flex-col gap-3 border-t border-slate-100 bg-white px-5 py-4">
                    <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                      <span className="flex items-center gap-1.5 text-sm font-bold text-slate-700">
                        <MapPin className="h-4 w-4 text-primary" />
                        收货地址
                      </span>
                      {addresses.length > 0 ? (
                        <select
                          value={selectedAddressId ?? ''}
                          disabled={isLoadingAddresses}
                          onChange={(event) => {
                            const rawValue = event.target.value;
                            setSelectedAddressIdByTenant((prev) => ({
                              ...prev,
                              [group.tenantId]: rawValue ? Number(rawValue) : undefined,
                            }));
                          }}
                          className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-slate-800 outline-none focus:border-primary focus:ring-1 focus:ring-primary sm:w-96"
                        >
                          <option value="">请选择收货地址</option>
                          {addresses.map((address) => (
                            <option key={address.id} value={address.id}>
                              {address.isDefault === 1 ? '默认 · ' : ''}
                              {address.receiverName} {address.phone} · {[address.province, address.city, address.district, address.detail].filter(Boolean).join('')}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <button
                          type="button"
                          onClick={() => navigate('/addresses')}
                          className="rounded-xl border border-primary/20 px-4 py-2 text-xs font-black text-primary transition-colors hover:bg-primary/5"
                        >
                          去新增地址
                        </button>
                      )}
                    </div>
                    <p className="text-xs font-medium leading-relaxed text-slate-500">
                      {selectedAddress
                        ? `${selectedAddress.receiverName} ${selectedAddress.phone} · ${[selectedAddress.province, selectedAddress.city, selectedAddress.district, selectedAddress.detail].filter(Boolean).join('')}`
                        : '实物商品会在下单时生成地址快照，后续修改地址不会影响该订单。'}
                    </p>
                  </div>
                )}

                {/* Coupon Selector Section */}
                <div className="flex flex-col gap-3 border-t border-slate-100 bg-slate-50/20 px-5 py-4">
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <span className="flex items-center gap-1.5 text-sm font-bold text-slate-600">
                      <Ticket className="h-4 w-4 text-orange-500" />
                      优惠券
                    </span>
                    <select
                      value={selectedCouponByTenant[group.tenantId]?.key || ''}
                      onChange={(e) => {
                        const key = e.target.value;
                        if (!key) {
                          setSelectedCouponByTenant((prev) => ({ ...prev, [group.tenantId]: null }));
                          return;
                        }
                        const coupon = getSelectableCoupons(group.tenantId, group.subtotal).find(c => c.key === key);
                        if (coupon) {
                          setSelectedCouponByTenant((prev) => ({
                            ...prev,
                            [group.tenantId]: {
                              key,
                              type: coupon.type,
                              id: coupon.id,
                              name: coupon.name,
                            }
                          }));
                        }
                      }}
                      className="w-full sm:w-64 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-bold text-slate-800 outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    >
                      <option value="">不使用优惠券</option>
                      {getSelectableCoupons(group.tenantId, group.subtotal).map((opt) => (
                        <option
                          key={opt.key}
                          value={opt.key}
                          disabled={!opt.isUsable}
                        >
                          {opt.name} - {opt.desc} {opt.reason ? `(${opt.reason})` : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                  {selectedCouponByTenant[group.tenantId] && getSelectedCouponDiscount(group.tenantId, group.subtotal) > 0 && (
                    <div className="flex items-center justify-between text-xs text-orange-600 font-bold bg-orange-50/50 p-2.5 rounded-xl border border-orange-100">
                      <span>已选: {selectedCouponByTenant[group.tenantId]?.name}</span>
                      <span>优惠: -{formatCurrency(getSelectedCouponDiscount(group.tenantId, group.subtotal))}</span>
                    </div>
                  )}
                </div>

                <div className="flex flex-col gap-4 border-t border-slate-100 bg-slate-50/60 p-5 md:flex-row md:items-center md:justify-between">
                  <div className="flex-1">
                    <div className="flex gap-3 mb-4">
                      <button
                        type="button"
                        onClick={() => setPaymentMethod('ALIPAY_PAGE')}
                        className={`flex-1 rounded-xl border-2 py-2.5 text-sm font-bold transition-all ${
                          paymentMethod === 'ALIPAY_PAGE'
                            ? 'border-primary bg-primary/5 text-primary'
                            : 'border-slate-200 text-slate-500 hover:border-slate-300'
                        }`}
                      >
                        支付宝支付
                      </button>
                      <button
                        type="button"
                        onClick={() => setPaymentMethod('UNIFIED_WALLET')}
                        className={`flex-1 rounded-xl border-2 py-2.5 text-sm font-bold transition-all ${
                          paymentMethod === 'UNIFIED_WALLET'
                            ? 'border-primary bg-primary/5 text-primary'
                            : 'border-slate-200 text-slate-500 hover:border-slate-300'
                        }`}
                      >
                        钱包余额
                      </button>
                    </div>
                    <div>
                    <p className="text-xs font-black uppercase tracking-widest text-slate-400">
                      商户小计
                    </p>
                    <p className="mt-1 text-2xl font-black tracking-tight text-slate-900">
                      {selectedCouponByTenant[group.tenantId] && getSelectedCouponDiscount(group.tenantId, group.subtotal) > 0 ? (
                        <span className="flex items-center gap-2">
                          <span className="text-sm font-semibold text-slate-400 line-through">
                            {formatCurrency(group.subtotal)}
                          </span>
                          <span className="text-primary">
                            {formatCurrency(Math.max(0, group.subtotal - getSelectedCouponDiscount(group.tenantId, group.subtotal)))}
                          </span>
                        </span>
                      ) : (
                        formatCurrency(group.subtotal)
                      )}
                    </p>
                    </div>
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
              {totalDiscount > 0 ? (
                <div className="flex items-baseline gap-2">
                  <span className="text-sm font-semibold text-slate-400 line-through">
                    {formatCurrency(subtotal)}
                  </span>
                  <span className="text-3xl font-black tracking-tight text-primary">
                    {formatCurrency(Math.max(0, subtotal - totalDiscount))}
                  </span>
                  <span className="text-xs font-bold text-orange-600 bg-orange-50 px-2 py-0.5 rounded-md ml-1">
                    已省 {formatCurrency(totalDiscount)}
                  </span>
                </div>
              ) : (
                <span className="text-3xl font-black tracking-tight text-slate-900">
                  {formatCurrency(subtotal)}
                </span>
              )}
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
