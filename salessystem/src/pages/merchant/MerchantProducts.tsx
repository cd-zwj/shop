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
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { merchantProductService } from '../../services/modules/merchantProduct';
import type { MerchantProduct } from '../../types/merchant';
import { cn } from '../../lib/utils';
import { formatCurrency, getImageUrl } from '../../utils/display';
import {
  buildProductStockUpdatePayload,
  buildProductStatusUpdatePayload,
  normalizeProductStatusFilter,
  PRODUCT_STATUS_FILTERS,
  type ProductEditableStatus,
  type ProductStatusFilter,
} from '../../utils/productBulkEdit';

export default function MerchantProducts() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { merchantSession } = useAuth();
  const tenantId = merchantSession?.tenantId;
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<ProductStatusFilter>(() => normalizeProductStatusFilter(searchParams.get('status')));
  const [products, setProducts] = useState<MerchantProduct[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(() => new Set());
  const [isLoading, setIsLoading] = useState(true);
  const [isBatchUpdating, setIsBatchUpdating] = useState(false);
  const [isStockUpdating, setIsStockUpdating] = useState(false);
  const [stockEditingProduct, setStockEditingProduct] = useState<MerchantProduct | null>(null);
  const [stockDraft, setStockDraft] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    const nextStatus = normalizeProductStatusFilter(searchParams.get('status'));
    setStatusFilter((current) => current === nextStatus ? current : nextStatus);
  }, [searchParams]);

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
          status: statusFilter === 'ALL' ? undefined : statusFilter,
        });
        if (!isMounted) return;
        setProducts(result.records ?? []);
        setSelectedIds(new Set());
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
  }, [tenantId, search, statusFilter]);

  const lowStockCount = useMemo(
    () => products.filter((product) => Number(product.stock || 0) <= 5).length,
    [products],
  );
  const activeCount = useMemo(
    () => products.filter((product) => product.status === 'active').length,
    [products],
  );
  const selectedProducts = useMemo(
    () => products.filter((product) => selectedIds.has(product.id)),
    [products, selectedIds],
  );
  const allVisibleSelected = products.length > 0 && products.every((product) => selectedIds.has(product.id));

  function handleStatusFilterChange(nextStatus: ProductStatusFilter) {
    setStatusFilter(nextStatus);
    if (nextStatus === 'ALL') {
      setSearchParams({});
      return;
    }
    setSearchParams({ status: nextStatus });
  }

  function toggleProductSelection(productId: number) {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(productId)) {
        next.delete(productId);
      } else {
        next.add(productId);
      }
      return next;
    });
  }

  function toggleAllVisible() {
    setSelectedIds((prev) => {
      if (products.length === 0) {
        return prev;
      }
      if (products.every((product) => prev.has(product.id))) {
        return new Set();
      }
      return new Set(products.map((product) => product.id));
    });
  }

  async function handleBatchStatusUpdate(status: ProductEditableStatus) {
    if (!tenantId || selectedProducts.length === 0) return;

    setIsBatchUpdating(true);
    setError('');
    setSuccess('');
    try {
      await Promise.all(
        selectedProducts.map((product) =>
          merchantProductService.updateProduct(
            tenantId,
            product.id,
            buildProductStatusUpdatePayload(product, status),
          ),
        ),
      );
      const selectedIdSet = new Set(selectedProducts.map((product) => product.id));
      setProducts((prev) => prev.map((product) => selectedIdSet.has(product.id) ? { ...product, status } : product));
      setSelectedIds(new Set());
      setSuccess(`已批量${status === 'active' ? '上架' : status === 'inactive' ? '下架' : '标记售罄'} ${selectedProducts.length} 个商品`);
    } catch {
      setError('批量更新商品状态失败，请稍后重试');
    } finally {
      setIsBatchUpdating(false);
    }
  }

  function openStockEditor(product: MerchantProduct) {
    setStockEditingProduct(product);
    setStockDraft(String(product.stock ?? 0));
    setError('');
    setSuccess('');
  }

  async function handleStockUpdate() {
    if (!tenantId || !stockEditingProduct) return;

    const nextStock = Number(stockDraft);
    if (!Number.isInteger(nextStock) || nextStock < 0) {
      setError('库存必须是大于等于 0 的整数');
      return;
    }

    setIsStockUpdating(true);
    setError('');
    setSuccess('');
    try {
      await merchantProductService.updateProduct(
        tenantId,
        stockEditingProduct.id,
        buildProductStockUpdatePayload(stockEditingProduct, nextStock),
      );
      setProducts((prev) => prev.map((product) =>
        product.id === stockEditingProduct.id ? { ...product, stock: nextStock } : product,
      ));
      setStockEditingProduct(null);
      setStockDraft('');
      setSuccess(`已调整「${stockEditingProduct.name}」库存为 ${nextStock}`);
    } catch {
      setError('库存调整失败，请稍后重试');
    } finally {
      setIsStockUpdating(false);
    }
  }

  async function handleDelete(productId: number) {
    if (!tenantId) return;
    try {
      await merchantProductService.deleteProduct(tenantId, productId);
      setProducts((prev) => prev.filter((product) => product.id !== productId));
      setSelectedIds((prev) => {
        const next = new Set(prev);
        next.delete(productId);
        return next;
      });
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
      {success && (
        <div className="rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
          {success}
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
          <div className="flex w-full flex-wrap items-center gap-2 sm:w-auto">
            <span className="flex items-center gap-1.5 px-2 text-[10px] font-black uppercase tracking-widest text-slate-400">
              <Filter className="h-3.5 w-3.5" /> 状态
            </span>
            {PRODUCT_STATUS_FILTERS.map((filter) => (
              <button
                key={filter.id}
                type="button"
                onClick={() => handleStatusFilterChange(filter.id)}
                className={cn(
                  'rounded-xl border px-3 py-2 text-xs font-black transition-all',
                  statusFilter === filter.id
                    ? 'border-primary bg-primary text-white shadow-sm'
                    : 'border-slate-100 bg-white text-slate-500 hover:border-primary/30 hover:text-primary',
                )}
              >
                {filter.label}
              </button>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-3 border-b border-slate-50 bg-slate-50/50 px-6 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="text-xs font-bold text-slate-500">
            已选择 <span className="font-black text-slate-900">{selectedProducts.length}</span> 个商品
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => void handleBatchStatusUpdate('active')}
              disabled={selectedProducts.length === 0 || isBatchUpdating}
              className="rounded-xl bg-emerald-600 px-4 py-2 text-xs font-black text-white transition-all hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              批量上架
            </button>
            <button
              type="button"
              onClick={() => void handleBatchStatusUpdate('inactive')}
              disabled={selectedProducts.length === 0 || isBatchUpdating}
              className="rounded-xl bg-slate-900 px-4 py-2 text-xs font-black text-white transition-all hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              批量下架
            </button>
            <button
              type="button"
              onClick={() => void handleBatchStatusUpdate('out_of_stock')}
              disabled={selectedProducts.length === 0 || isBatchUpdating}
              className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-2 text-xs font-black text-amber-700 transition-all hover:bg-amber-100 disabled:cursor-not-allowed disabled:opacity-50"
            >
              标记售罄
            </button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-slate-50/50">
                <th className="px-8 py-5">
                  <input
                    type="checkbox"
                    checked={allVisibleSelected}
                    onChange={toggleAllVisible}
                    className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary"
                    aria-label="选择当前页全部商品"
                  />
                </th>
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
                      {product && (
                        <input
                          type="checkbox"
                          checked={selectedIds.has(product.id)}
                          onChange={(event) => {
                            event.stopPropagation();
                            toggleProductSelection(product.id);
                          }}
                          onClick={(event) => event.stopPropagation()}
                          className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary"
                          aria-label={`选择商品 ${product.name}`}
                        />
                      )}
                    </td>
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
                              openStockEditor(product);
                            }}
                            className="rounded-xl p-2 text-slate-400 shadow-sm transition-all hover:bg-amber-50 hover:text-amber-600"
                            title="调整库存"
                          >
                            <Package size={18} />
                          </button>
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

      {stockEditingProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-[32px] border border-slate-100 bg-white p-6 shadow-2xl">
            <div className="mb-5 flex items-start justify-between gap-4">
              <div>
                <h3 className="text-lg font-black text-slate-900">调整库存</h3>
                <p className="mt-1 text-xs font-bold text-slate-400">
                  {stockEditingProduct.name} / 当前 {stockEditingProduct.stock}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setStockEditingProduct(null)}
                className="rounded-xl bg-slate-50 px-3 py-2 text-xs font-black text-slate-500 hover:bg-slate-100"
              >
                关闭
              </button>
            </div>

            <label className="mb-2 block text-[10px] font-black uppercase tracking-widest text-slate-400">
              新库存数量
            </label>
            <input
              type="number"
              min={0}
              step={1}
              value={stockDraft}
              onChange={(event) => setStockDraft(event.target.value)}
              className="w-full rounded-2xl border-2 border-slate-100 bg-slate-50 px-5 py-4 text-xl font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white"
            />
            <p className="mt-3 rounded-2xl bg-amber-50 px-4 py-3 text-xs font-bold leading-relaxed text-amber-700">
              库存会参与用户下单前校验。调低库存后，购物车结算可能提示用户刷新库存。
            </p>

            <div className="mt-6 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setStockEditingProduct(null)}
                className="rounded-xl border border-slate-200 px-5 py-3 text-sm font-black text-slate-500 hover:bg-slate-50"
              >
                取消
              </button>
              <button
                type="button"
                onClick={() => void handleStockUpdate()}
                disabled={isStockUpdating}
                className="rounded-xl bg-slate-900 px-5 py-3 text-sm font-black text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isStockUpdating ? '保存中...' : '保存库存'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
