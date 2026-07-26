import { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import { ArrowLeft, MapPin, Search, Share2, ShoppingBag, Star } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { appCatalogService } from '../services/modules/appCatalog';
import type { AppStore, Product, Tenant } from '../types/catalog';
import { formatCurrency, getImageUrl } from '../utils/display';

export default function PublicMerchantDetail() {
  const navigate = useNavigate();
  const { id } = useParams();
  const tenantId = Number(id);
  const [merchant, setMerchant] = useState<Tenant | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [stores, setStores] = useState<AppStore[]>([]);
  const [selectedStoreId, setSelectedStoreId] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadMerchantDetail() {
      if (!tenantId) {
        setError('商户参数无效');
        setIsLoading(false);
        return;
      }

      try {
        const [tenant, tenantStores] = await Promise.all([
          appCatalogService.getTenant(tenantId),
          appCatalogService.listTenantStores(tenantId),
        ]);
        if (!isMounted) return;
        setMerchant(tenant);
        setStores(tenantStores);
        const initialStoreId = tenantStores[0]?.id;
        setSelectedStoreId(initialStoreId ?? null);
        setProducts([]);
      } catch {
        if (!isMounted) return;
        setError('商户信息加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadMerchantDetail();

    return () => {
      isMounted = false;
    };
  }, [tenantId]);

  useEffect(() => {
    if (!tenantId || !selectedStoreId) {
      return;
    }
    void appCatalogService.listTenantProducts(tenantId, selectedStoreId)
      .then(setProducts)
      .catch(() => setProducts([]));
  }, [selectedStoreId, tenantId]);

  return (
    <div className="flex flex-col gap-0 pb-32">
      <section className="relative overflow-hidden bg-slate-900 px-4 pb-24 pt-16 md:px-6 md:pb-40 md:pt-20">
        <button
          onClick={() => navigate(-1)}
          className="absolute left-4 top-6 z-20 rounded-2xl bg-white/10 p-3 text-white backdrop-blur transition-all hover:bg-white/20 md:left-6 md:top-12"
        >
          <ArrowLeft size={20} />
        </button>
        <div className="absolute right-0 top-0 -mr-40 -mt-40 h-[400px] w-[400px] rounded-full bg-primary/20 blur-[80px] md:h-[600px] md:w-[600px] md:blur-[120px]" />

        <div className="relative z-10 mx-auto flex w-full max-w-7xl flex-col items-center gap-6 md:flex-row md:items-end md:gap-8">
          <div className="h-24 w-24 shrink-0 overflow-hidden rounded-[32px] border-4 border-white/5 bg-white shadow-2xl md:h-40 md:w-40 md:rounded-[48px] md:border-8">
            <img
              src={getImageUrl(undefined, 'https://images.unsplash.com/photo-1520607162513-77705c0f0d4a?auto=format&fit=crop&w=900&q=80')}
              alt={merchant?.name || 'Merchant'}
              className="h-full w-full object-cover"
            />
          </div>
          <div className="flex flex-1 flex-col gap-3 text-center md:gap-4 md:text-left">
            <div className="flex flex-col items-center gap-3 md:flex-row md:justify-start md:gap-4">
              <h1 className="text-3xl font-black tracking-tighter text-white md:text-5xl">
                {merchant?.name || (isLoading ? '商户加载中...' : '未找到商户')}
              </h1>
              <span className="w-fit rounded-lg bg-primary px-3 py-1 text-[10px] font-black uppercase tracking-widest text-white">
                官方认证
              </span>
            </div>
            <div className="flex flex-wrap items-center justify-center gap-4 text-sm font-medium text-slate-400 md:justify-start md:gap-6">
              <span className="flex items-center gap-1.5 md:gap-2">
                <Star className="h-4 w-4 fill-current text-yellow-400" /> 已接入
              </span>
              <span className="flex items-center gap-1.5 md:gap-2">
                <MapPin className="h-4 w-4" /> {merchant?.address || '暂无地址信息'}
              </span>
              <span className="flex items-center gap-1.5 md:gap-2">
                <ShoppingBag className="h-4 w-4" /> {products.length} 件商品
              </span>
            </div>
          </div>
          <div className="flex w-full items-center gap-3 md:w-auto">
            <button className="flex-1 rounded-[20px] bg-white px-8 py-4 text-sm font-black text-slate-900 shadow-xl transition-all hover:scale-105 active:scale-95 md:flex-none md:rounded-[24px] md:text-base">
              收藏店铺
            </button>
            <button className="rounded-[20px] bg-white/10 p-4 text-white backdrop-blur transition-all hover:bg-white/20 md:rounded-[24px]">
              <Share2 size={20} />
            </button>
          </div>
        </div>
      </section>

      <div className="relative z-20 mx-auto -mt-8 w-full max-w-7xl px-4 md:-mt-20 md:px-6">
        <div className="rounded-[32px] bg-white p-6 shadow-2xl shadow-slate-900/10 md:rounded-[48px] md:p-10">
          <header className="mb-8 flex flex-col gap-6 md:mb-12 lg:flex-row lg:items-center lg:justify-between lg:gap-8">
            <div className="flex-1 overflow-x-auto border-b border-slate-50 hide-scrollbar">
              <div className="flex gap-6 md:gap-8">
                {['全部商品', '商户信息', '联系方式'].map((tab, index) => (
                  <button
                    key={tab}
                    className={`whitespace-nowrap pb-4 text-xs font-black uppercase tracking-widest transition-all md:pb-6 md:text-sm ${
                      index === 0 ? 'border-b-4 border-primary text-primary' : 'text-slate-400 hover:text-slate-900'
                    }`}
                  >
                    {tab}
                  </button>
                ))}
              </div>
            </div>
            <div className="relative w-full lg:w-80">
              <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-300" />
              <input
                type="text"
                placeholder="在店内搜索商品..."
                className="w-full rounded-[16px] bg-slate-50 py-3 pl-11 pr-4 text-sm font-bold outline-none transition-all focus:ring-4 focus:ring-primary/5 md:rounded-[20px] md:py-4"
              />
            </div>
          </header>

          <div className="mb-6 flex gap-2 overflow-x-auto">
            {stores.map((store) => (
              <button
                key={store.id}
                type="button"
                onClick={() => setSelectedStoreId(store.id)}
                className={`shrink-0 rounded-lg border px-3 py-2 text-sm font-semibold ${selectedStoreId === store.id ? 'border-primary bg-primary text-white' : 'border-slate-200 text-slate-700'}`}
              >
                {store.storeName}
              </button>
            ))}
          </div>

          {error && (
            <div className="mb-6 rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
              {error}
            </div>
          )}

          <div className="grid grid-cols-1 gap-6 md:gap-8 lg:grid-cols-3 sm:grid-cols-2">
            {products.map((product) => (
              <motion.div
                key={product.id}
                whileHover={{ y: -8 }}
                onClick={() => navigate(`/product/${product.id}?tenantId=${tenantId}&storeId=${selectedStoreId}`)}
                className="group flex cursor-pointer flex-col gap-4 rounded-[32px] border border-slate-50 p-3 transition-all hover:shadow-2xl hover:shadow-slate-200/50 md:gap-5 md:rounded-[40px] md:p-4"
              >
                <div className="aspect-square overflow-hidden rounded-[24px] bg-slate-100 md:rounded-[32px]">
                  <img
                    src={getImageUrl(product.imageUrl)}
                    alt={product.name}
                    className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-110"
                  />
                </div>
                <div className="flex flex-col gap-1 px-2 pb-2 md:gap-2">
                  <h3 className="line-clamp-1 text-lg font-black text-slate-900 md:text-xl">{product.name}</h3>
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xl font-black tracking-tighter text-primary md:text-2xl">
                      {formatCurrency(product.price)}
                    </span>
                    <button className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-900 text-white shadow-lg transition-all hover:bg-primary active:scale-95 md:rounded-2xl">
                      <ShoppingBag size={18} />
                    </button>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>

          <button className="mt-12 w-full rounded-2xl bg-slate-50 py-4 text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 transition-all hover:bg-slate-100 hover:text-slate-900 md:rounded-3xl md:py-5 md:text-xs">
            {products.length > 0 ? `已加载 ${products.length} 件商品` : '暂无更多商品'}
          </button>
        </div>
      </div>
    </div>
  );
}
