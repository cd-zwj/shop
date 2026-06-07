import { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import {
  ChevronRight,
  Film,
  FastForward,
  Grid2X2,
  HeartPulse,
  MapPin,
  Plane,
  Plus,
  Search,
  Smartphone,
  Sparkles,
  Star,
  Store as StoreIcon,
  Ticket,
  Utensils,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { appCatalogService } from '../services/modules/appCatalog';
import type { Product, Tenant } from '../types/catalog';

type ProductWithTenant = Product & { tenantId: number };
import { cn } from '../lib/utils';
import { formatCurrency, getImageUrl } from '../utils/display';

export default function Home() {
  const navigate = useNavigate();
  const [featuredProducts, setFeaturedProducts] = useState<ProductWithTenant[]>([]);
  const [featuredMerchants, setFeaturedMerchants] = useState<Tenant[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const categories = [
    { icon: Smartphone, label: '手机充值', color: 'bg-blue-50 text-blue-600', path: '/recharge' },
    { icon: Ticket, label: '领券中心', color: 'bg-orange-50 text-orange-500', path: '/coupons' },
    { icon: StoreIcon, label: '附近门店', color: 'bg-green-50 text-green-600', path: '/discovery?category=fujinmendian' },
    { icon: FastForward, label: '外卖美食', color: 'bg-red-50 text-red-500', path: '/discovery?category=waimaimeishi' },
    { icon: Plane, label: '旅游出行', color: 'bg-sky-50 text-sky-500', path: '/discovery?category=lvyouchuxing' },
    { icon: Film, label: '电影演出', color: 'bg-purple-50 text-purple-500', path: '/discovery?category=dianyingyanchu' },
    { icon: HeartPulse, label: '健康购药', color: 'bg-teal-50 text-teal-500', path: '/discovery?category=jiankanggouyao' },
    { icon: Grid2X2, label: '全部分类', color: 'bg-slate-100 text-slate-600', path: '/discovery' },
  ];

  useEffect(() => {
    let isMounted = true;

    async function loadHomeData() {
      try {
        const tenants = await appCatalogService.listTenants();
        if (!isMounted) return;
        setFeaturedMerchants(tenants.slice(0, 2));

        const productGroups = await Promise.all(
          tenants.slice(0, 2).map(async (tenant) => {
            const list = await appCatalogService.listTenantProducts(tenant.id);
            return list.map((p) => ({ ...p, tenantId: tenant.id }));
          }),
        );
        if (!isMounted) return;
        setFeaturedProducts(productGroups.flat().slice(0, 4));
      } catch {
        if (!isMounted) return;
        setFeaturedMerchants([]);
        setFeaturedProducts([]);
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadHomeData();

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <div className="flex flex-col gap-6 pb-10 md:gap-8">
      <section className="mt-4 px-4">
        <div className="flex flex-col gap-3 rounded-2xl border border-slate-100 bg-white p-4 shadow-sm">
          <div className="flex items-center gap-1 text-slate-600">
            <MapPin className="h-4 w-4" />
            <span className="flex-1 truncate text-sm font-medium">定位中，默认展示平台推荐商户</span>
            <ChevronRight className="h-4 w-4 rotate-90" />
          </div>
          <div className="relative flex items-center">
            <Search className="absolute left-3 h-5 w-5 text-slate-400" />
            <input
              type="text"
              placeholder="搜索商户、商品或分类..."
              className="w-full rounded-full border border-slate-200 bg-slate-50 py-2.5 pl-10 pr-4 text-sm transition-all placeholder:text-slate-400 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
            <button className="absolute right-1.5 rounded-full bg-primary px-4 py-1.5 text-xs font-medium text-white shadow-sm transition-opacity hover:opacity-90">
              搜索
            </button>
          </div>
        </div>
      </section>

      <section className="pl-4">
        <div className="flex snap-x snap-mandatory gap-4 overflow-x-auto pb-2 pr-4 hide-scrollbar">
          {[
            { title: '真实商户数据', subtitle: '首页推荐已切换到后端返回结果', img: 'https://images.unsplash.com/photo-1520607162513-77705c0f0d4a?auto=format&fit=crop&w=1200&q=80' },
            { title: '统一接口底座', subtitle: '登录、商户、商品浏览都已开始联调', img: 'https://images.unsplash.com/photo-1556740749-887f6717d7e4?auto=format&fit=crop&w=1200&q=80' },
          ].map((banner, i) => (
            <motion.div
              key={i}
              whileHover={{ scale: 1.02 }}
              className="relative aspect-[2.2/1] w-[88%] shrink-0 snap-center cursor-pointer overflow-hidden rounded-2xl shadow-md md:w-[400px]"
            >
              <img src={banner.img} alt={banner.title} className="h-full w-full object-cover transition-transform duration-700" />
              <div className="absolute inset-0 flex flex-col justify-end bg-gradient-to-t from-black/80 via-black/20 to-transparent p-5">
                <span className="text-xl font-bold tracking-wide text-white md:text-2xl">{banner.title}</span>
                <span className="mt-1 text-sm text-white/90">{banner.subtitle}</span>
              </div>
            </motion.div>
          ))}
        </div>
      </section>

      <section className="mx-4 rounded-3xl border border-slate-50 bg-white px-4 py-6 shadow-sm">
        <div className="grid grid-cols-4 gap-x-2 gap-y-6">
          {categories.map((cat) => (
            <motion.button
              key={cat.label}
              whileTap={{ scale: 0.95 }}
              onClick={() => navigate(cat.path)}
              className="group flex flex-col items-center gap-2"
            >
              <div className={cn('flex h-12 w-12 items-center justify-center rounded-[18px] transition-transform duration-200 group-hover:scale-110', cat.color)}>
                <cat.icon className="h-6 w-6 fill-current" />
              </div>
              <span className="text-[12px] font-semibold text-slate-700">{cat.label}</span>
            </motion.button>
          ))}
        </div>
      </section>

      <section className="flex flex-col gap-4 px-4">
        <div className="flex items-center justify-between">
          <h2 className="flex items-center gap-2 text-xl font-bold text-slate-900">
            <Sparkles className="h-5 w-5 fill-current text-primary" />
            后端商品推荐
          </h2>
          <button onClick={() => navigate('/discovery')} className="flex items-center text-[13px] text-slate-500 transition-colors hover:text-primary">
            查看全部 <ChevronRight className="h-4 w-4" />
          </button>
        </div>

        <div className="grid grid-cols-2 gap-4">
          {(isLoading ? Array.from({ length: 4 }) : featuredProducts).map((product, index) => {
            const isData = typeof product === 'object';
            return (
              <motion.div
                key={isData ? product.id : index}
                whileHover={{ y: -4 }}
                onClick={() => isData && navigate(`/product/${product.id}?tenantId=${product.tenantId}`)}
                className="flex cursor-pointer flex-col overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm"
              >
                <div className="relative h-40 bg-slate-100">
                  {isData ? (
                    <img src={getImageUrl(product.imageUrl)} alt={product.name} className="h-full w-full object-cover" />
                  ) : (
                    <div className="h-full w-full animate-pulse bg-slate-200" />
                  )}
                  <div className="absolute right-2 top-2 flex items-center gap-1 rounded-full bg-white/90 px-2.5 py-1 shadow-sm backdrop-blur">
                    <Star className="h-3 w-3 fill-current text-orange-500" />
                    <span className="text-[11px] font-bold text-slate-700">{isData ? '实时' : '加载中'}</span>
                  </div>
                </div>
                <div className="flex flex-col gap-2 p-3">
                  <span className="h-10 line-clamp-2 text-sm font-semibold leading-relaxed text-slate-900">
                    {isData ? product.name : '加载商品中...'}
                  </span>
                  <div className="flex items-center gap-1">
                    <span className="rounded border border-red-100 bg-red-50 px-2 py-0.5 text-[10px] font-bold text-red-500">
                      {isData ? product.category || '平台商品' : '同步中'}
                    </span>
                  </div>
                  <div className="mt-1 flex items-center justify-between">
                    <div className="text-red-500">
                      <span className="text-sm font-black">{isData ? formatCurrency(product.price) : '...'}</span>
                    </div>
                    <button className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-white shadow-sm transition-opacity hover:opacity-90">
                      <Plus className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </motion.div>
            );
          })}
        </div>

        <motion.button
          whileHover={{ scale: 1.01 }}
          onClick={() => featuredMerchants[0] && navigate(`/merchant-store/${featuredMerchants[0].id}`)}
          className="mt-2 flex cursor-pointer items-center gap-4 rounded-2xl border border-blue-100 bg-gradient-to-r from-blue-50 to-indigo-50 p-5 text-left"
        >
          <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-white shadow-sm">
            <Utensils className="h-7 w-7 text-primary" />
          </div>
          <div className="flex flex-col gap-1">
            <span className="text-lg font-bold text-slate-900">
              {featuredMerchants[0]?.name || '正在同步热门商户'}
            </span>
            <span className="text-[13px] leading-snug text-slate-600">
              {featuredMerchants[0]?.address ||
                featuredMerchants[0]?.contact ||
                '接入真实商户数据后，这里会展示商户简介与地址。'}
            </span>
          </div>
        </motion.button>
      </section>
    </div>
  );
}
