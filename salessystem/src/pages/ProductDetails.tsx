import { useEffect, useState } from 'react';
import {
  ArrowDown,
  ArrowLeft,
  CheckCircle2,
  ChevronRight,
  PackageCheck,
  PlayCircle,
  RotateCcw,
  Search,
  ShieldCheck,
  ShoppingCart,
  Store,
  Truck,
  ZoomIn,
} from 'lucide-react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { appAddressService } from '../services/modules/appAddress';
import { appCatalogService } from '../services/modules/appCatalog';
import { createOrderForItems, getOrderCheckoutPath, requiresShippingAddress } from '../services/orderCheckout';
import type { WalletStrategy } from '../types/order';
import { ApiError } from '../types/api';
import type { Product, Tenant } from '../types/catalog';
import type { CartItem } from '../types/cart';
import { cn } from '../lib/utils';
import { formatCurrency, getImageUrl } from '../utils/display';
import { openAlipayPaymentWindow, saveAlipayPaymentPayload } from '../utils/alipayPayment';
import {
  getProductDetailPresentation,
  getMerchantInfoPresentation,
} from '../utils/productDetail';

export default function ProductDetails() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const { currentRole } = useAuth();
  const { addItem, totalItems } = useCart();
  const productId = Number(id);
  const [product, setProduct] = useState<Product | null>(null);
  const [merchant, setMerchant] = useState<Tenant | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [isSubmittingOrder, setIsSubmittingOrder] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<'ALIPAY_PAGE' | 'UNIFIED_WALLET'>('ALIPAY_PAGE');

  const queryTenantId = searchParams.get('tenantId');
  const tenantId = queryTenantId ? Number(queryTenantId) : undefined;
  const resolvedTenantId = product?.tenantId ?? tenantId;

  const thumbnails = [
    'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=600&q=80',
    'https://images.unsplash.com/photo-1556740749-887f6717d7e4?auto=format&fit=crop&w=600&q=80',
  ];

  useEffect(() => {
    let isMounted = true;

    async function loadProduct() {
      if (!productId) {
        setError('商品参数无效');
        setIsLoading(false);
        return;
      }

      try {
        const detail = await appCatalogService.getProduct(productId);
        if (!isMounted) return;
        setProduct(detail);
        const nextTenantId = detail.tenantId ?? tenantId;
        setMerchant(null);
        if (nextTenantId) {
          try {
            const tenant = await appCatalogService.getTenant(nextTenantId);
            if (!isMounted) return;
            setMerchant(tenant);
          } catch {
            if (!isMounted) return;
            setMerchant(null);
          }
        }
      } catch {
        if (!isMounted) return;
        setError('商品详情加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadProduct();

    return () => {
      isMounted = false;
    };
  }, [productId, tenantId]);

  const productPresentation = getProductDetailPresentation(product);
  const inventory = productPresentation.inventory;
  const fulfillment = productPresentation.fulfillment;
  const deliveryAccess = productPresentation.deliveryAccess;
  const afterSalesNote = productPresentation.afterSalesNote;
  const merchantInfo = getMerchantInfoPresentation(merchant, resolvedTenantId);
  const purchaseLimitNote = productPresentation.purchaseLimitNote;
  const saleStatus = productPresentation.saleStatus;
  const isOutOfStock = inventory.isOutOfStock;
  const isPurchaseBlocked = isOutOfStock || !saleStatus.isPurchasable;

  function toCheckoutItem(detail: Product): CartItem {
    return {
      productId: detail.id,
      tenantId: resolvedTenantId ?? 0,
      name: detail.name,
      price: detail.price,
      quantity: 1,
      imageUrl: detail.imageUrl,
      stock: detail.stock,
      category: detail.category,
      productType: detail.productType,
      fulfillmentMode: detail.fulfillmentMode,
    };
  }

  function handleAddToCart() {
    if (!product) {
      return;
    }
    if (!resolvedTenantId) {
      setActionMessage('当前商品缺少商户信息，请从商户店铺或商品列表重新进入');
      return;
    }

    if (!saleStatus.isPurchasable) {
      setActionMessage(saleStatus.description);
      return;
    }

    if (isOutOfStock) {
      setActionMessage('该商品暂时无库存，暂不能加入购物车');
      return;
    }

    addItem({ ...product, tenantId: resolvedTenantId }, 1);
    setActionMessage('已加入购物车，可以继续选购或前往结算');
  }

  async function handleBuyNow() {
    if (!product) {
      return;
    }
    if (!resolvedTenantId) {
      setActionMessage('当前商品缺少商户信息，请从商户店铺或商品列表重新进入');
      return;
    }

    if (!saleStatus.isPurchasable) {
      setActionMessage(saleStatus.description);
      return;
    }

    if (isOutOfStock) {
      setActionMessage('该商品暂时无库存，暂不能下单');
      return;
    }

    if (currentRole !== 'user') {
      navigate('/login');
      return;
    }

    setActionMessage('');
    setIsSubmittingOrder(true);

    try {
      const checkoutItem = toCheckoutItem(product);
      let addressId: number | undefined;
      if (requiresShippingAddress([checkoutItem])) {
        const addresses = await appAddressService.list();
        const defaultAddress = addresses.find((address) => address.isDefault === 1) ?? addresses[0];
        if (!defaultAddress) {
          setActionMessage('实物商品需要先新增收货地址，请到地址管理维护默认地址后再购买');
          return;
        }
        addressId = defaultAddress.id;
      }
      const walletStrategy: WalletStrategy = paymentMethod === 'UNIFIED_WALLET' ? 'UNIFIED_ONLY' : 'NO_WALLET';
      const channelCode = paymentMethod === 'UNIFIED_WALLET' ? undefined : paymentMethod;
      const payment = await createOrderForItems(
        [checkoutItem], 'APP_BUY_NOW', undefined, walletStrategy, channelCode, addressId);
      if (payment.externalPayUrl) {
        const isOpened = openAlipayPaymentWindow(payment.externalPayUrl);
        if (!isOpened && payment.paymentBillNo) {
          saveAlipayPaymentPayload({
            billNo: payment.paymentBillNo,
            orderNo: payment.orderNo,
            source: 'order',
            payHtml: payment.externalPayUrl,
            amount: payment.totalAmount,
          });
        }
      }

      navigate(getOrderCheckoutPath(payment));
    } catch (err) {
      setActionMessage(
        err instanceof ApiError || err instanceof Error
          ? err.message
          : '订单创建失败，请稍后重试',
      );
    } finally {
      setIsSubmittingOrder(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-50 pb-32 md:bg-white md:pb-12">
      <header className="fixed top-0 z-50 flex h-16 w-full items-center justify-between border-b border-slate-200 bg-white px-4 md:hidden">
        <button onClick={() => navigate(-1)} className="rounded-full p-2 text-slate-600 hover:bg-slate-50">
          <ArrowLeft className="h-6 w-6" />
        </button>
        <span className="text-lg font-bold text-primary">SalesSystem</span>
        <div className="flex items-center gap-1">
          <button className="rounded-full p-2 text-slate-600">
            <Search className="h-5 w-5" />
          </button>
          <button onClick={() => navigate('/cart')} className="relative rounded-full p-2 text-slate-600">
            <ShoppingCart className="h-5 w-5" />
            {totalItems > 0 && (
              <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-error px-1 text-[10px] font-black text-white">
                {Math.min(totalItems, 99)}
              </span>
            )}
          </button>
        </div>
      </header>

      <main className="mx-auto w-full max-w-7xl md:grid md:grid-cols-12 md:gap-12 md:px-8 md:py-12 md:pt-16">
        <nav className="col-span-12 mb-8 hidden items-center gap-2 text-sm text-slate-400 md:flex">
          <span className="hover:text-primary">商品目录</span>
          <ChevronRight className="h-4 w-4" />
          <span className="hover:text-primary">{product?.category || '平台商品'}</span>
          <ChevronRight className="h-4 w-4" />
          <span className="font-medium text-slate-900">{product?.name || '商品详情'}</span>
        </nav>

        <section className="col-span-12 flex flex-col gap-6 pt-16 md:col-span-7 md:pt-0">
          <div className="relative aspect-square w-full overflow-hidden bg-white shadow-sm md:aspect-[4/3] md:rounded-3xl md:border md:border-slate-200">
            <img
              src={getImageUrl(product?.imageUrl)}
              alt={product?.name || 'Product'}
              loading="lazy"
              className="h-full w-full object-cover"
            />
            <div className="absolute left-6 top-6 flex flex-col gap-2">
              <span className="rounded-md bg-primary px-3 py-1 text-[10px] font-black uppercase tracking-[0.1em] text-white">
                实时商品
              </span>
              {product?.category && (
                <span className="rounded-md bg-tertiary px-3 py-1 text-[10px] font-black uppercase tracking-[0.1em] text-white">
                  {product.category}
                </span>
              )}
            </div>
            <button className="absolute bottom-6 right-6 rounded-full bg-white/90 p-3 text-slate-800 shadow-lg backdrop-blur-md transition-all hover:bg-white">
              <ZoomIn className="h-5 w-5" />
            </button>
          </div>

          <div className="flex snap-x gap-4 overflow-x-auto px-4 hide-scrollbar md:px-0">
            <div className="h-24 w-24 shrink-0 snap-start overflow-hidden rounded-2xl border-2 border-primary bg-white shadow-sm">
              <img src={getImageUrl(product?.imageUrl)} alt="" className="h-full w-full object-cover" />
            </div>
            {thumbnails.map((thumb) => (
              <div key={thumb} className="h-24 w-24 shrink-0 snap-start overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm opacity-60 transition-all hover:border-primary/50 hover:opacity-100">
                <img src={thumb} alt="" className="h-full w-full object-cover" />
              </div>
            ))}
            <div className="flex h-24 w-24 shrink-0 snap-start items-center justify-center rounded-2xl border border-slate-200 bg-slate-100 text-slate-400 shadow-sm opacity-60 transition-all hover:opacity-100">
              <PlayCircle className="h-10 w-10" />
            </div>
          </div>
        </section>

        <section className="col-span-12 mt-12 flex flex-col px-4 md:col-span-5 md:mt-0 md:px-0">
          <div className="mb-8">
            <div className="mb-3 flex items-center gap-1.5 text-xs font-bold uppercase tracking-widest text-slate-400">
              <CheckCircle2 className="h-4 w-4 fill-current text-blue-500" />
              <span>{merchantInfo.title}</span>
            </div>
            <h1 className="mb-4 text-3xl font-black leading-tight text-slate-900 md:text-4xl">
              {product?.name || (isLoading ? '商品加载中...' : '未找到商品')}
            </h1>
            <p className="text-lg leading-relaxed text-slate-500">
              {product?.description || error || '这里将展示后端返回的商品描述与展示文案。'}
            </p>
          </div>

          <div className="mb-10 flex items-end gap-3 border-b border-slate-100 pb-8">
            <span className="text-4xl font-black tracking-tight text-slate-900">
              {product ? formatCurrency(product.price) : '...'}
            </span>
            {product?.stock !== undefined && product?.stock !== null && (
              <span className={cn('mb-1.5 rounded-md border px-3 py-1 text-xs font-black', inventory.toneClass)}>
                {inventory.label}
              </span>
            )}
            {product && (
              <span className={cn('mb-1.5 rounded-md border px-3 py-1 text-xs font-black', saleStatus.toneClass)}>
                {saleStatus.label}
              </span>
            )}
          </div>

          {actionMessage && (
            <div className="mb-6 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-600">
              {actionMessage}
            </div>
          )}
          <div className="mb-10 flex flex-col gap-4">
            <h3 className="text-sm font-black uppercase tracking-widest text-slate-800">支付方式</h3>
            <div className='flex gap-3 mb-4'>
              <button
                type='button'
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
                type='button'
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
          </div>

          <div className="mb-12 rounded-2xl border border-slate-100 bg-slate-50 p-6 shadow-inner">
            <h3 className="mb-6 text-sm font-black uppercase tracking-widest text-slate-800">购买与交付</h3>
            <ul className="flex flex-col gap-5">
              {[
                { icon: PackageCheck, title: inventory.label, label: inventory.description },
                { icon: CheckCircle2, title: saleStatus.label, label: saleStatus.description },
                { icon: Truck, title: fulfillment.label, label: fulfillment.description },
                { icon: CheckCircle2, title: deliveryAccess.label, label: deliveryAccess.description },
                { icon: ShieldCheck, title: '购买限制', label: purchaseLimitNote },
                { icon: RotateCcw, title: '售后说明', label: afterSalesNote },
                {
                  icon: Store,
                  title: '商户信息',
                  label: `${merchantInfo.description} ${merchantInfo.contactLine}`,
                },
              ].map((item) => (
                <li key={item.title} className="flex items-start gap-4">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white shadow-sm">
                    <item.icon className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <div className="text-sm font-bold text-slate-900">{item.title}</div>
                    <div className="mt-1 text-[15px] leading-relaxed text-slate-600">{item.label}</div>
                  </div>
                </li>
              ))}
            </ul>
          </div>

          <div className="mb-12 rounded-2xl border border-blue-100 bg-blue-50 p-5">
            <div className="flex items-start gap-4">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white text-blue-600 shadow-sm">
                <PackageCheck className="h-5 w-5" />
              </div>
              <div className="flex-1">
                <h3 className="text-sm font-black text-blue-900">{deliveryAccess.label}</h3>
                <p className="mt-1 text-sm font-semibold leading-relaxed text-blue-700">
                  {deliveryAccess.description}
                </p>
                <button
                  type="button"
                  onClick={() => navigate('/my-purchases')}
                  className="mt-4 inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2 text-xs font-black text-blue-700 shadow-sm transition-all hover:bg-blue-100"
                >
                  {deliveryAccess.actionLabel}
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>

          <div className="mb-12 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
            <div className="flex items-start gap-4">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-50 text-primary">
                <Store className="h-5 w-5" />
              </div>
              <div className="flex-1">
                <h3 className="text-sm font-black text-slate-900">{merchantInfo.title}</h3>
                <p className="mt-1 text-sm font-semibold leading-relaxed text-slate-600">
                  {merchantInfo.description}
                </p>
                <p className="mt-1 text-xs font-medium leading-relaxed text-slate-400">
                  {merchantInfo.contactLine}
                </p>
                <button
                  type="button"
                  onClick={() => merchantInfo.actionPath && navigate(merchantInfo.actionPath)}
                  disabled={!merchantInfo.actionPath}
                  className="mt-4 inline-flex items-center gap-2 rounded-xl bg-slate-900 px-4 py-2 text-xs font-black text-white shadow-sm transition-all hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {merchantInfo.actionLabel}
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>

          <div className="mb-12">
            <h2 className="mb-4 text-xl font-black text-slate-900">商品概览</h2>
            <div className="space-y-6 text-[15px] leading-relaxed text-slate-500">
              <p>{product?.description || '后端真实数据接入后，这里优先展示商品描述、用途、规格和补充说明。'}</p>
              <p>
                {product?.category
                  ? `当前商品分类为「${product.category}」，${fulfillment.description}`
                  : fulfillment.description}
              </p>
            </div>
            <button className="group mt-6 flex items-center gap-1.5 text-sm font-bold text-primary">
              阅读完整规格
              <ArrowDown className="h-4 w-4 transition-transform group-hover:translate-y-1" />
            </button>
          </div>

          <div className="mt-auto hidden flex-col gap-4 md:flex">
            <button
              onClick={handleAddToCart}
              disabled={!product || isPurchaseBlocked || isSubmittingOrder}
              className="flex w-full items-center justify-center gap-3 rounded-2xl bg-primary px-6 py-4 font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary-container hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
            >
              <ShoppingCart className="h-5 w-5" />
              加入购物车
            </button>
            <button
              onClick={handleBuyNow}
              disabled={!product || isPurchaseBlocked || isSubmittingOrder}
              className="w-full rounded-2xl border border-slate-200 bg-white px-6 py-4 font-bold text-slate-900 shadow-sm transition-all hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmittingOrder ? '创建订单中...' : '立即购买'}
            </button>
          </div>
        </section>
      </main>

      <div className="fixed bottom-16 z-50 flex w-full gap-3 border-t border-slate-100 bg-white/95 p-4 pb-10 shadow-[0_-10px_40px_-15px_rgba(0,0,0,0.1)] backdrop-blur-xl md:hidden">
        <button
          onClick={handleBuyNow}
          disabled={!product || isPurchaseBlocked || isSubmittingOrder}
          className="flex-1 rounded-2xl border border-slate-200 bg-white px-2 py-4 font-bold text-slate-900 shadow-sm transition-colors active:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isSubmittingOrder ? '创建订单中...' : '立即购买'}
        </button>
        <button
          onClick={handleAddToCart}
          disabled={!product || isPurchaseBlocked || isSubmittingOrder}
          className="flex-[1.5] rounded-2xl bg-primary px-4 py-4 font-bold text-white shadow-xl shadow-primary/20 transition-all active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
        >
          <div className="flex items-center justify-center gap-2">
            <ShoppingCart className="h-5 w-5" />
            加入购物车
          </div>
        </button>
      </div>
    </div>
  );
}
