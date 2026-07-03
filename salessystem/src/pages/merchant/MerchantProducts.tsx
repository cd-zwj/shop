import { useEffect, useMemo, useState } from 'react';
import { motion } from 'motion/react';
import {
  AlertTriangle,
  Edit3,
  Filter,
  Package,
  Plus,
  Search,
  Trash2,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { merchantProductService } from '../../services/modules/merchantProduct';
import type { MerchantProduct } from '../../types/merchant';
import { cn } from '../../lib/utils';
import { formatCurrency, getImageUrl } from '../../utils/display';

export default function MerchantProducts() {
  const navigate = useNavigate();
  const { merchantSession } = useAuth();
  const tenantId = merchantSession?.tenantId;
  const [search, setSearch] = useState('');
  const [products, setProducts] = useState<MerchantProduct[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadProducts() {
      if (!tenantId) {
        setError('当前商户会话缺少 tenantId，请重新登录');
        setIsLoading(false);
        return;
      }

      try {
        const result = await merchantProductService.listProducts(tenantId, {
          current: 1,
          size: 100,
          search: search.trim() || undefined,
        });
        if (!isMounted) return;
        setProducts(result.records ?? []);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('商品列表加载失败，请稍后重试');
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
  }, [tenantId, search]);

  const lowStockCount = useMemo(
    () => products.filter((product) => Number(product.stock || 0) <= 5).length,
    [products],
  );
  const activeCount = useMemo(
    () => products.filter((product) => product.status === 'active').length,
    [products],
  );

  async function handleDelete(productId: number) {
    if (!tenantId) return;
    try {
      await merchantProductService.deleteProduct(tenantId, productId);
      setProducts((prev) => prev.filter((product) => product.id !== productId));
    } catch {
      setError('商品删除失败，请稍后重试');
    }
  }

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header className="flex flex-col justify-between gap-6 md:flex-row md:items-center">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-black tracking-tight text-slate-900">我的商品库</h1>
          <p className="font-medium text-slate-500">
            当前商户：{merchantSession?.tenantName || '未获取商户会话'}，以下数据来自真实商品接口。
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <button
            onClick={() => navigate('/merchant/product/new')}
            className="flex items-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-black text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary-container"
          >
            <Plus className="h-4 w-4" /> 发布新商品
          </button>
        </div>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="flex items-start gap-4 rounded-2xl border border-orange-100 bg-orange-50 p-4">
          <div className="rounded-xl bg-orange-100 p-2 text-orange-600">
            <AlertTriangle className="h-5 w-5" />
          </div>
          <div>
            <h4 className="text-sm font-black text-orange-700">库存预警</h4>
            <p className="mt-0.5 text-xs text-orange-600">当前有 {lowStockCount} 个商品库存低于等于 5，请及时补货。</p>
          </div>
        </div>
        <div className="flex items-start gap-4 rounded-2xl border border-blue-100 bg-blue-50 p-4">
          <div className="rounded-xl bg-blue-100 p-2 text-blue-600">
            <Package className="h-5 w-5" />
          </div>
          <div>
            <h4 className="text-sm font-black text-blue-700">上架商品</h4>
            <p className="mt-0.5 text-xs text-blue-600">当前共有 {activeCount} 个商品处于 active 状态。</p>
          </div>
        </div>
      </div>

      <div className="flex flex-col overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-2xl shadow-slate-100/40">
        <div className="flex flex-col items-center gap-4 border-b border-slate-50 p-6 sm:flex-row">
          <div className="relative w-full flex-1">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="搜索商品名称、SKU..."
              className="w-full rounded-2xl border border-transparent bg-slate-50 py-3 pl-11 pr-4 text-sm outline-none transition-all focus:border-primary/20 focus:bg-white"
            />
          </div>
          <div className="shrink-0">
            <button className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-black uppercase tracking-widest text-slate-500 shadow-sm transition-all hover:bg-slate-50">
              <Filter className="h-3.5 w-3.5" /> 筛选
            </button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-slate-50/50">
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest italic text-slate-400">
                  商品详情
                </th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest italic text-slate-400">
                  单价
                </th>
                <th className="px-8 py-5 text-center text-[10px] font-black uppercase tracking-widest italic text-slate-400">
                  状态
                </th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest italic text-slate-400">
                  剩余库存
                </th>
                <th className="px-8 py-5"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {(isLoading ? Array.from<MerchantProduct | undefined>({ length: 3 }) : products).map((product, index) => {                return (
                  <motion.tr
                    key={product ? product.id : index}
                    whileHover={{ backgroundColor: '#fcfdfe' }}
                    className="group cursor-pointer transition-colors"
                    onClick={() => product && navigate(`/merchant/product/${product.id}`)}
                  >
                    <td className="px-8 py-6">
                      <div className="flex items-center gap-4">
                        <div className="flex h-14 w-14 items-center justify-center overflow-hidden rounded-2xl border border-slate-100 bg-slate-50">
                          {product ? (
                            <img src={getImageUrl(product.imageUrl)} alt={product.name} className="h-full w-full object-cover" />
                          ) : null}
                        </div>
                        <div className="flex flex-col">
                          <span className="text-[15px] font-black tracking-tight text-slate-900 transition-colors group-hover:text-primary">
                            {product ? product.name : '加载中...'}
                          </span>
                          <span className="font-mono text-[11px] font-bold uppercase text-slate-400">
                            SKU: {product ? product.productCode : '--'}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="px-8 py-6 font-black tracking-tight text-slate-900">
                      {product ? formatCurrency(product.price) : '...'}
                    </td>
                    <td className="px-8 py-6">
                      <div className="flex justify-center">
                        <span
                          className={cn(
                            'rounded-lg border px-3 py-1.5 text-[10px] font-black uppercase tracking-widest',
                            product && product.status === 'active'
                              ? 'border-green-100 bg-green-50 text-green-600'
                              : product && product.status === 'inactive'
                                ? 'border-slate-200 bg-slate-100 text-slate-500'
                                : 'border-red-100 bg-red-50 text-red-600',
                          )}
                        >
                          {product ? product.status : '...'}
                        </span>
                      </div>
                    </td>
                    <td className="px-8 py-6">
                      <span
                        className={cn(
                          'text-sm font-bold',
                          product && Number(product.stock) === 0 ? 'text-red-500' : 'text-slate-500',
                        )}
                      >
                        {product ? product.stock : '--'}
                      </span>
                    </td>
                    <td className="px-8 py-6">
                      {product && (
                        <div className="flex items-center justify-end gap-2 opacity-0 transition-opacity group-hover:opacity-100">
                          <button
                            onClick={(event) => {
                              event.stopPropagation();
                              navigate(`/merchant/product/edit/${product.id}`);
                            }}
                            className="rounded-xl p-2 text-slate-400 shadow-sm transition-all hover:bg-primary/5 hover:text-primary"
                          >
                            <Edit3 size={18} />
                          </button>
                          <button
                            onClick={(event) => {
                              event.stopPropagation();
                              void handleDelete(product.id);
                            }}
                            className="rounded-xl p-2 text-slate-400 shadow-sm transition-all hover:bg-red-50 hover:text-red-500"
                          >
                            <Trash2 size={18} />
                          </button>
                        </div>
                      )}
                    </td>
                  </motion.tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
