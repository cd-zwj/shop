import { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import {
  ArrowDown,
  ArrowLeft,
  CheckCircle2,
  ChevronRight,
  Database,
  Headphones,
  LineChart,
  PlayCircle,
  Search,
  ShoppingCart,
  ZoomIn,
} from 'lucide-react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { appCatalogService } from '../services/modules/appCatalog';
import { createOrderForItems, getOrderCheckoutPath } from '../services/orderCheckout';
import { ApiError } from '../types/api';
import type { Product } from '../types/catalog';
import type { CartItem } from '../types/cart';
import { cn } from '../lib/utils';
import { formatCurrency, getImageUrl } from '../utils/display';
import { openAlipayPaymentWindow, saveAlipayPaymentPayload } from '../utils/alipayPayment';

export default function ProductDetails() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const { currentRole } = useAuth();
  const { addItem, totalItems } = useCart();
  const productId = Number(id);
  const [selectedTier, setSelectedTier] = useState('pro');
  const [product, setProduct] = useState<Product | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');
  const [isSubmittingOrder, setIsSubmittingOrder] = useState(false);

  const queryTenantId = searchParams.get('tenantId');
  const tenantId = queryTenantId ? Number(queryTenantId) : undefined;

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
  }, [productId]);

  const isOutOfStock = typeof product?.stock === 'number' && product.stock <= 0;

  function toCheckoutItem(detail: Product): CartItem {
    return {
      productId: detail.id,
      tenantId: tenantId ?? 0,
      name: detail.name,
      price: detail.price,
      quantity: 1,
      imageUrl: detail.imageUrl,
      stock: detail.stock,
      category: detail.category,
    };
  }

  function handleAddToCart() {
    if (!product) {
      return;
    }

    if (isOutOfStock) {
      setActionMessage('该商品暂时无库存，暂不能加入购物车');
      return;
    }

    addItem({ ...product, tenantId }, 1);
    setActionMessage('已加入购物车，可以继续选购或前往结算');
  }

  async function handleBuyNow() {
    if (!product) {
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
      const payment = await createOrderForItems([toCheckoutItem(product)], 'APP_BUY_NOW');

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
              <span>SalesSystem 认证</span>
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
              <span className="mb-1.5 rounded-md border border-red-100 bg-red-50 px-3 py-1 text-xs font-black text-red-600">
                库存 {product.stock}
              </span>
            )}
          </div>

          {actionMessage && (
            <div className="mb-6 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-600">
              {actionMessage}
            </div>
          )}

          <div className="mb-10 flex flex-col gap-4">
            <h3 className="text-sm font-black uppercase tracking-widest text-slate-800">许可等级</h3>
            <div className="grid grid-cols-2 gap-4">
              {[
                { id: 'pro', label: '标准版', desc: '适合基础展示' },
                { id: 'enterprise', label: '扩展版', desc: '适合企业场景' },
              ].map((tier) => (
                <label
                  key={tier.id}
                  className={cn(
                    'relative flex cursor-pointer flex-col rounded-2xl border-2 bg-slate-50/50 p-5 shadow-sm transition-all hover:border-slate-300',
                    selectedTier === tier.id && 'border-primary bg-white ring-4 ring-primary/5',
                  )}
                  onClick={() => setSelectedTier(tier.id)}
                >
                  <input type="radio" checked={selectedTier === tier.id} className="sr-only" readOnly />
                  <span className="font-bold text-slate-900">{tier.label}</span>
                  <span className="mt-1 text-xs text-slate-500">{tier.desc}</span>
                  {selectedTier === tier.id && (
                    <CheckCircle2 className="absolute right-5 top-5 h-5 w-5 fill-current text-primary" />
                  )}
                </label>
              ))}
            </div>
          </div>

          <div className="mb-12 rounded-2xl border border-slate-100 bg-slate-50 p-6 shadow-inner">
            <h3 className="mb-6 text-sm font-black uppercase tracking-widest text-slate-800">包含服务</h3>
            <ul className="flex flex-col gap-5">
              {[
                { icon: Database, label: '商品基础信息、库存、分类等都来自真实后端接口。' },
                { icon: LineChart, label: '后续会继续接入订单、支付状态和用户钱包等真实数据。' },
                { icon: Headphones, label: '当前页面结构已为后续接单、支付和详情联动预留。' },
              ].map((item) => (
                <li key={item.label} className="flex items-start gap-4">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 bg-white shadow-sm">
                    <item.icon className="h-5 w-5 text-primary" />
                  </div>
                  <span className="text-[15px] leading-relaxed text-slate-600">{item.label}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="mb-12">
            <h2 className="mb-4 text-xl font-black text-slate-900">商品概览</h2>
            <div className="space-y-6 text-[15px] leading-relaxed text-slate-500">
              <p>{product?.description || '后端真实数据接入后，这里优先展示商品描述、用途、规格和补充说明。'}</p>
              <p>
                {product?.category
                  ? `当前商品分类为「${product.category}」，后续还可以继续扩展商户、订单、支付、钱包等完整链路。`
                  : '当前页面已经切换到真实商品接口，后续会继续接入订单与支付链路。'}
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
              disabled={!product || isOutOfStock || isSubmittingOrder}
              className="flex w-full items-center justify-center gap-3 rounded-2xl bg-primary px-6 py-4 font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary-container hover:shadow-xl disabled:cursor-not-allowed disabled:opacity-60"
            >
              <ShoppingCart className="h-5 w-5" />
              加入购物车
            </button>
            <button
              onClick={handleBuyNow}
              disabled={!product || isOutOfStock || isSubmittingOrder}
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
          disabled={!product || isOutOfStock || isSubmittingOrder}
          className="flex-1 rounded-2xl border border-slate-200 bg-white px-2 py-4 font-bold text-slate-900 shadow-sm transition-colors active:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isSubmittingOrder ? '创建订单中...' : '立即购买'}
        </button>
        <button
          onClick={handleAddToCart}
          disabled={!product || isOutOfStock || isSubmittingOrder}
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
