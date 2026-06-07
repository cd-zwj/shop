import { type ReactNode, useEffect, useState } from 'react';
import { motion } from 'motion/react';
import {
  Filter,
  Heart,
  MapPin,
  Search,
  Smartphone,
  Star,
} from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { appCatalogService } from '../services/modules/appCatalog';
import type { Tenant } from '../types/catalog';
import { cn } from '../lib/utils';
import { getImageUrl } from '../utils/display';

export default function Discovery() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [stores, setStores] = useState<Tenant[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState('全部分类');
  const [searchQuery, setSearchQuery] = useState(() => searchParams.get('keyword') || '');
  const [debouncedQuery, setDebouncedQuery] = useState(() => searchParams.get('keyword') || '');

  const categories = ['全部分类', '餐饮', '零售', '服务', '娱乐'];

  // Helper to map query param to category tab
  const mapQueryToCategory = (queryVal: string | null) => {
    if (!queryVal) return '全部分类';
    switch (queryVal.toLowerCase()) {
      case 'waimaimeishi':
        return '餐饮';
      case 'jiankanggouyao':
        return '零售';
      case 'lvyouchuxing':
        return '服务';
      case 'dianyingyanchu':
        return '娱乐';
      default:
        return '全部分类';
    }
  };

  // Helper to assign mock category to tenants
  const getStoreCategory = (storeId: number) => {
    const cats = ['餐饮', '零售', '服务', '娱乐'];
    return cats[storeId % cats.length];
  };

  // Load stores once on mount
  useEffect(() => {
    let isMounted = true;

    async function loadStores() {
      try {
        const tenants = await appCatalogService.listTenants();
        if (!isMounted) return;
        setStores(tenants);
      } catch {
        if (!isMounted) return;
        setStores([]);
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadStores();

    return () => {
      isMounted = false;
    };
  }, []);

  // Sync category and search query from URL search parameters
  useEffect(() => {
    const categoryParam = searchParams.get('category');
    setActiveCategory(mapQueryToCategory(categoryParam));

    const keywordParam = searchParams.get('keyword') || '';
    if (keywordParam !== searchQuery) {
      setSearchQuery(keywordParam);
      setDebouncedQuery(keywordParam);
    }
  }, [searchParams]);

  // Debounce search query and sync with URL
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(searchQuery);

      const currentKeyword = searchParams.get('keyword') || '';
      if (searchQuery !== currentKeyword) {
        if (searchQuery) {
          searchParams.set('keyword', searchQuery);
        } else {
          searchParams.delete('keyword');
        }
        setSearchParams(searchParams);
      }
    }, 300);

    return () => {
      clearTimeout(handler);
    };
  }, [searchQuery, searchParams, setSearchParams]);

  // Filter stores based on search query and active category tab
  const filteredStores = stores.filter((store) => {
    const matchesSearch = store.name.toLowerCase().includes(debouncedQuery.toLowerCase()) || 
                          (store.address || '').toLowerCase().includes(debouncedQuery.toLowerCase());
    
    const matchesCategory = activeCategory === '全部分类' || getStoreCategory(store.id) === activeCategory;

    return matchesSearch && matchesCategory;
  });

  const displayStores = isLoading ? Array.from({ length: 6 }) : filteredStores;

  const handleCategoryChange = (cat: string) => {
    setActiveCategory(cat);
    // Sync with URL query parameter
    if (cat === '全部分类') {
      searchParams.delete('category');
    } else {
      const pinyinMap: Record<string, string> = {
        '餐饮': 'waimaimeishi',
        '零售': 'jiankanggouyao',
        '服务': 'lvyouchuxing',
        '娱乐': 'dianyingyanchu',
      };
      searchParams.set('category', pinyinMap[cat] || cat);
    }
    setSearchParams(searchParams);
  };

  return (
    <div className="flex flex-col gap-6 pb-10 md:gap-8">
      <section className="sticky top-16 z-40 border-b border-slate-100 bg-white px-4 py-3 shadow-sm md:hidden">
        <div className="flex items-center gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="搜索门店..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-full border-none bg-slate-100 py-2 pl-10 pr-4 text-sm outline-none transition-all focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <button className="rounded-full p-2 text-slate-500 transition-colors hover:bg-slate-100">
            <Filter className="h-5 w-5" />
          </button>
        </div>
      </section>

      <section className="mx-auto flex w-full max-w-6xl flex-col gap-8 px-4 md:mt-8 lg:px-8">
        <div className="hidden flex-col gap-6 md:flex">
          <h1 className="text-3xl font-bold text-slate-900">探索门店</h1>
          <div className="flex items-center gap-6">
            <div className="relative max-w-md flex-1">
              <Search className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                placeholder="搜索餐饮、零售、服务..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full rounded-xl border border-slate-200 bg-white py-3 pl-12 pr-4 text-base shadow-sm outline-none transition-all focus:border-primary focus:ring-4 focus:ring-primary/10"
              />
            </div>
            <div className="flex gap-2 overflow-x-auto pb-1 hide-scrollbar">
              {categories.map((cat) => (
                <button
                  key={cat}
                  onClick={() => handleCategoryChange(cat)}
                  className={cn(
                    'whitespace-nowrap rounded-full px-5 py-2.5 text-sm font-semibold shadow-sm transition-all',
                    activeCategory === cat
                      ? 'bg-primary text-white shadow-md shadow-primary/10'
                      : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50',
                  )}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className="flex gap-2 overflow-x-auto pb-4 hide-scrollbar md:hidden">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => handleCategoryChange(cat)}
              className={cn(
                'whitespace-nowrap rounded-full px-5 py-2 text-sm font-semibold shadow-sm',
                activeCategory === cat ? 'bg-primary text-white shadow-md shadow-primary/10' : 'border border-slate-200 bg-white text-slate-600',
              )}
            >
              {cat}
            </button>
          ))}
        </div>

        <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
          {!isLoading && filteredStores.length === 0 ? (
            <EmptyState
              icon={<Search className="w-12 h-12" />}
              title="没有找到匹配的门店"
              subtitle="尝试更换其他分类，或调整你的搜索关键词再试一次吧~"
            />
          ) : (
            displayStores.map((store, index) => {
              const isData = typeof store === 'object';
              return (
                <motion.article
                  key={isData ? store.id : index}
                  whileHover={{ y: -8 }}
                  onClick={() => isData && navigate(`/merchant-store/${store.id}`)}
                  className="group flex cursor-pointer flex-col overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm transition-all duration-500 hover:shadow-xl"
                >
                  <div className="relative h-48 w-full overflow-hidden bg-slate-100">
                    {isData ? (
                      <img
                        src={getImageUrl(undefined, 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=900&q=80')}
                        alt={store.name}
                        className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-110"
                      />
                    ) : (
                      <div className="h-full w-full animate-pulse bg-slate-200" />
                    )}
                    <div className="absolute right-4 top-4 flex items-center gap-1 rounded-full bg-white/90 px-2.5 py-1 shadow-md backdrop-blur-md">
                      <Star className="h-4 w-4 fill-current text-primary" />
                      <span className="text-sm font-bold text-slate-800">{isData ? getStoreCategory(store.id) : '加载中'}</span>
                    </div>
                    <button className="absolute left-4 top-4 rounded-full bg-white/20 p-2 text-white transition-colors hover:bg-white/40">
                      <Heart className="h-5 w-5" />
                    </button>
                  </div>

                  <div className="flex flex-1 flex-col p-6">
                    <div className="mb-2 flex justify-between">
                      <h3 className="line-clamp-1 text-xl font-bold text-slate-900 transition-colors group-hover:text-primary">
                        {isData ? store.name : '加载商户中...'}
                      </h3>
                    </div>
                    <div className="mb-4 flex items-center gap-1 text-sm text-slate-500">
                      <MapPin className="h-4 w-4" />
                      <span>{isData ? store.address || `商户 ID: ${store.id}` : '请稍候'}</span>
                    </div>
                    <p className="mb-6 flex-1 line-clamp-2 text-[15px] leading-relaxed text-slate-600">
                      {isData
                        ? [store.contact, store.phone].filter(Boolean).join(' · ') || '该商户已接入平台，可点击查看在售商品。'
                        : '正在同步商户资料...'}
                    </p>

                    <div className="flex flex-wrap gap-2 border-t border-slate-50 pt-4">
                      <span className="inline-flex items-center gap-1.5 rounded-full border border-slate-100 bg-slate-50 px-3 py-1.5 text-xs font-bold text-slate-700">
                        <Smartphone className="h-3.5 w-3.5 text-slate-500" />
                        平台商户
                      </span>
                    </div>
                  </div>
                </motion.article>
              );
            })
          )}
        </div>
      </section>
    </div>
  );
}

function EmptyState({ icon, title, subtitle }: { icon: ReactNode; title: string; subtitle: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-20 px-4 text-center rounded-3xl border border-slate-100 bg-white shadow-sm w-full col-span-full">
      <div className="p-4 rounded-full bg-slate-50 text-slate-400 mb-4">
        {icon}
      </div>
      <h3 className="text-base font-extrabold text-slate-800">{title}</h3>
      <p className="text-xs font-semibold text-slate-400 mt-1.5 max-w-xs leading-relaxed">{subtitle}</p>
    </div>
  );
}
