import { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import { 
  Plus, 
  Search, 
  Filter, 
  Package, 
  LayoutGrid, 
  List, 
  ChevronRight,
  Eye,
  Edit3,
  Trash2,
  ArrowUpDown,
} from 'lucide-react';
import { cn } from '../lib/utils';
import { appCatalogService } from '../services/modules/appCatalog';
import type { Product } from '../types/catalog';
import { formatCurrency, getImageUrl } from '../utils/display';

export default function AdminProducts() {
  const [products, setProducts] = useState<Array<Product & { tenantName: string }>>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadProducts() {
      setIsLoading(true);
      try {
        const tenants = await appCatalogService.listTenants();
        const allProducts = (await Promise.all(
          tenants.map(async (t) => {
            try {
              const stores = await appCatalogService.listTenantStores(t.id);
              const tenantProducts = stores[0]
                ? await appCatalogService.listTenantProducts(t.id, stores[0].id)
                : [];
              return tenantProducts.map((p) => ({ ...p, tenantName: t.name }));
            } catch {
              return [];
            }
          })
        )).flat();

        if (isMounted) {
          setProducts(allProducts);
        }
      } catch (err) {
        if (isMounted) {
          setProducts([]);
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadProducts();

    return () => {
      isMounted = false;
    };
  }, []);

  const getProductStatusText = (p: Product) => {
    if (p.stock === 0) return '已售罄';
    if (p.status === 1 || p.status === '1' || p.status === 'ACTIVE' || p.status === '销售中') return '销售中';
    if (p.status === 0 || p.status === '0' || p.status === 'DISABLED' || p.status === '下架') return '下架';
    return '销售中'; // default fallback
  };

  const filteredProducts = products.filter((p) => {
    const query = searchQuery.toLowerCase().trim();
    if (!query) return true;
    return (
      p.name.toLowerCase().includes(query) ||
      p.tenantName.toLowerCase().includes(query) ||
      (p.productCode || '').toLowerCase().includes(query)
    );
  });

  const skeletonRows = Array.from({ length: 5 }).map((_, index) => (
    <tr key={`skeleton-${index}`} className="animate-pulse border-b border-slate-50">
      <td className="px-8 py-6">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 bg-slate-100 rounded-2xl" />
          <div className="flex flex-col gap-2">
            <div className="w-32 h-4 bg-slate-100 rounded" />
            <div className="w-20 h-3 bg-slate-50 rounded" />
          </div>
        </div>
      </td>
      <td className="px-8 py-6"><div className="w-20 h-4 bg-slate-100 rounded mx-auto" /></td>
      <td className="px-8 py-6"><div className="w-16 h-4 bg-slate-100 rounded" /></td>
      <td className="px-8 py-6"><div className="w-16 h-4 bg-slate-100 rounded mx-auto" /></td>
      <td className="px-8 py-6"><div className="w-12 h-4 bg-slate-100 rounded" /></td>
      <td className="px-8 py-6"><div className="w-16 h-4 bg-slate-100 rounded" /></td>
      <td className="px-8 py-6"></td>
    </tr>
  ));

  const emptyStateRow = (
    <tr>
      <td colSpan={7} className="py-20 text-center">
        <div className="flex flex-col items-center justify-center text-slate-400">
          <Package className="w-12 h-12 mb-3 text-slate-300" />
          <p className="text-base font-extrabold text-slate-800">暂无产品数据</p>
          <p className="text-xs font-semibold text-slate-400 mt-1">没有找到符合条件的商品，请调整搜索过滤词试一下~</p>
        </div>
      </td>
    </tr>
  );

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-black text-slate-900 tracking-tight">产品编排</h1>
          <p className="text-slate-500 font-medium font-inter">跨类目动态管理库存、定价与分发策略。</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex bg-slate-100 p-1 rounded-xl">
            <button className="p-2 bg-white text-primary rounded-lg shadow-sm"><LayoutGrid className="w-4 h-4" /></button>
            <button className="p-2 text-slate-400 hover:text-slate-600 transition-colors"><List className="w-4 h-4" /></button>
          </div>
          <button className="flex items-center gap-2 px-5 py-3 bg-primary text-white rounded-xl text-sm font-black shadow-lg shadow-primary/20 hover:bg-opacity-90 transition-all">
            <Plus className="w-4 h-4" /> 发布新产品
          </button>
        </div>
      </header>

      {/* Main Container */}
      <div className="bg-white rounded-[32px] border border-slate-100 shadow-2xl shadow-slate-100/40 overflow-hidden flex flex-col">
        {/* Table Toolbar */}
        <div className="p-6 border-b border-slate-50 flex flex-col sm:flex-row items-center gap-4">
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
            <input 
              type="text" 
              placeholder="搜索产品名称、编码或商户..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-transparent rounded-2xl text-sm outline-none focus:bg-white focus:border-primary/20 transition-all font-inter"
            />
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <button className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-xs font-black text-slate-500 uppercase tracking-widest hover:bg-slate-50 transition-all shadow-sm">
              <Filter className="w-3.5 h-3.5" /> 筛选条件
            </button>
            <button className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-xs font-black text-slate-500 uppercase tracking-widest hover:bg-slate-50 transition-all shadow-sm">
              按时间排序 <ArrowUpDown className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Interactive Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-slate-50/50">
                <th className="px-8 py-5">
                   <div className="flex items-center gap-2">
                    <input type="checkbox" className="w-4 h-4 rounded border-slate-300 text-primary focus:ring-primary" />
                    <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest italic">产品目录</span>
                   </div>
                </th>
                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest italic text-center">商户</th>
                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest italic">定价</th>
                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest italic text-center">状态</th>
                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest italic">库存</th>
                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest italic">类目</th>
                <th className="px-8 py-5"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {isLoading ? (
                skeletonRows
              ) : filteredProducts.length === 0 ? (
                emptyStateRow
              ) : (
                filteredProducts.map((p) => {
                  const statusText = getProductStatusText(p);
                  return (
                    <motion.tr 
                      key={p.id}
                      whileHover={{ backgroundColor: '#fcfdfe' }}
                      className="group transition-colors cursor-pointer"
                    >
                      <td className="px-8 py-6">
                        <div className="flex items-center gap-4">
                          <div className="w-14 h-14 bg-slate-50 border border-slate-100 rounded-2xl flex items-center justify-center p-2 shadow-inner group-hover:scale-105 transition-transform duration-500 overflow-hidden">
                            <img 
                               src={getImageUrl(p.imageUrl)}
                               alt={p.name}
                               className="w-full h-full object-cover rounded-lg"
                            />
                          </div>
                          <div className="flex flex-col gap-0.5">
                            <span className="text-[15px] font-black text-slate-900 tracking-tight group-hover:text-primary transition-colors">{p.name}</span>
                            <span className="text-[11px] font-bold text-slate-400 font-inter font-mono uppercase">/{p.productCode || `ID: ${p.id}`}</span>
                          </div>
                        </div>
                      </td>
                      <td className="px-8 py-6 text-center">
                        <span className="text-xs font-bold text-slate-600">{p.tenantName}</span>
                      </td>
                      <td className="px-8 py-6 font-black text-slate-900 tracking-tight">{formatCurrency(p.price)}</td>
                      <td className="px-8 py-6">
                        <div className="flex justify-center">
                          <span className={cn(
                            "px-3 py-1.5 text-[10px] font-black rounded-lg uppercase tracking-widest border shadow-sm",
                            statusText === '销售中' ? "bg-green-50 text-green-600 border-green-100" :
                            statusText === '已售罄' ? "bg-red-50 text-red-600 border-red-100" :
                            statusText === '下架' ? "bg-slate-100 text-slate-500 border-slate-200" :
                            "bg-slate-100 text-slate-500 border-slate-200"
                          )}>
                            {statusText}
                          </span>
                        </div>
                      </td>
                      <td className="px-8 py-6">
                        <span className={cn("text-sm font-bold", p.stock === 0 ? "text-red-500" : "text-slate-500")}>
                          {p.stock !== null && p.stock !== undefined ? p.stock : '∞'} <span className="text-[11px] font-medium opacity-50 ml-1">当前</span>
                        </span>
                      </td>
                      <td className="px-8 py-6">
                        <div className="flex items-center gap-2">
                           <div className="w-2 h-2 rounded-full bg-primary" />
                           <span className="text-xs font-bold text-slate-800">{p.category || '未分类'}</span>
                        </div>
                      </td>
                      <td className="px-8 py-6">
                        <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                          <button className="p-2 text-slate-400 hover:text-primary hover:bg-primary/5 rounded-xl transition-all shadow-sm"><Eye size={18} /></button>
                          <button className="p-2 text-slate-400 hover:text-primary hover:bg-primary/5 rounded-xl transition-all shadow-sm"><Edit3 size={18} /></button>
                          <button className="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-xl transition-all shadow-sm"><Trash2 size={18} /></button>
                        </div>
                      </td>
                    </motion.tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <div className="p-8 bg-slate-50/50 border-t border-slate-50 flex items-center justify-between">
          <span className="text-xs font-black text-slate-400 uppercase tracking-widest">
            {isLoading ? '正在加载商品...' : `显示 ${filteredProducts.length} 件商品 / 共 ${products.length} 件`}
          </span>
          <div className="flex gap-2">
            <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-white border border-slate-200 text-slate-400 hover:bg-slate-50 transition-all shadow-sm" disabled><ChevronRight className="w-5 h-5 rotate-180" /></button>
            <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-primary text-white font-black text-xs shadow-md">1</button>
            <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-white border border-slate-200 text-slate-400 hover:bg-slate-50 transition-all shadow-sm" disabled><ChevronRight className="w-5 h-5" /></button>
          </div>
        </div>
      </div>
    </div>
  );
}
