import { useCallback, useEffect, useMemo, useState } from 'react';
import { History, Minus, PackagePlus, Warehouse } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { merchantInventoryService } from '../../services/modules/merchantInventory';
import { merchantProductService } from '../../services/modules/merchantProduct';
import { merchantStoreService } from '../../services/modules/merchantStore';
import type {
  MerchantProduct,
  MerchantStore,
  MerchantStoreInventory,
  MerchantStoreInventoryAdjustment,
  MerchantStoreInventoryLog,
} from '../../types/merchant';

const EMPTY_ADJUSTMENT: MerchantStoreInventoryAdjustment = {
  storeId: 0,
  productId: 0,
  delta: 0,
  remark: '',
};

export default function MerchantInventory() {
  const { merchantSession } = useAuth();
  const { showToast } = useToast();
  const tenantId = merchantSession?.tenantId;
  const [stocks, setStocks] = useState<MerchantStoreInventory[]>([]);
  const [logs, setLogs] = useState<MerchantStoreInventoryLog[]>([]);
  const [stores, setStores] = useState<MerchantStore[]>([]);
  const [products, setProducts] = useState<MerchantProduct[]>([]);
  const [storeId, setStoreId] = useState<number | undefined>();
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [selectedStock, setSelectedStock] = useState<MerchantStoreInventory | null>(null);
  const [adjustment, setAdjustment] = useState<MerchantStoreInventoryAdjustment>(EMPTY_ADJUSTMENT);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const loadStocks = useCallback(async () => {
    if (!tenantId) return;
    setLoading(true);
    try {
      const page = await merchantInventoryService.listStocks(tenantId, { storeId, lowStockOnly });
      setStocks(page.records || []);
    } catch (error) {
      showToast(error instanceof Error ? error.message : '门店库存加载失败', 'error');
    } finally {
      setLoading(false);
    }
  }, [lowStockOnly, showToast, storeId, tenantId]);

  const loadLogs = useCallback(async (stock: MerchantStoreInventory | null) => {
    if (!tenantId || !stock) {
      setLogs([]);
      return;
    }
    try {
      const page = await merchantInventoryService.listLogs(tenantId, {
        storeId: stock.storeId,
        productId: stock.productId,
      });
      setLogs(page.records || []);
    } catch (error) {
      showToast(error instanceof Error ? error.message : '库存流水加载失败', 'error');
    }
  }, [showToast, tenantId]);

  useEffect(() => {
    if (!tenantId) return;
    void Promise.all([
      merchantStoreService.listStores(tenantId, { size: 100, status: 1 }),
      merchantProductService.listProducts(tenantId, { size: 100, status: 'active' }),
    ]).then(([storePage, productPage]) => {
      setStores(storePage.records || []);
      setProducts(productPage.records || []);
    }).catch((error) => {
      showToast(error instanceof Error ? error.message : '门店或商品数据加载失败', 'error');
    });
  }, [showToast, tenantId]);

  useEffect(() => {
    void loadStocks();
  }, [loadStocks]);

  useEffect(() => {
    void loadLogs(selectedStock);
  }, [loadLogs, selectedStock]);

  const summary = useMemo(() => ({
    products: stocks.length,
    available: stocks.reduce((sum, stock) => sum + stock.availableQuantity, 0),
    locked: stocks.reduce((sum, stock) => sum + stock.lockedQuantity, 0),
  }), [stocks]);

  function selectStock(stock: MerchantStoreInventory) {
    setSelectedStock(stock);
    setAdjustment({
      storeId: stock.storeId,
      productId: stock.productId,
      delta: 0,
      remark: '',
    });
  }

  async function submitAdjustment() {
    if (!tenantId || !adjustment.storeId || !adjustment.productId || !adjustment.delta) {
      showToast('请选择门店、商品并填写非零调整数量', 'error');
      return;
    }
    setSaving(true);
    try {
      const saved = await merchantInventoryService.adjustStock(tenantId, {
        ...adjustment,
        remark: adjustment.remark?.trim() || undefined,
      });
      setSelectedStock(saved);
      setAdjustment((current) => ({ ...current, delta: 0, remark: '' }));
      showToast('门店库存已调整', 'success');
      await loadStocks();
      await loadLogs(saved);
    } catch (error) {
      showToast(error instanceof Error ? error.message : '库存调整失败', 'error');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 p-4 md:p-8">
      <header className="flex flex-col gap-2">
        <h1 className="text-3xl font-black text-slate-900">门店库存</h1>
        <p className="text-sm font-medium text-slate-500">到店自提订单使用同一份门店库存。</p>
      </header>

      <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
        <Summary label="已配置商品" value={summary.products} />
        <Summary label="可售库存" value={summary.available} />
        <Summary label="已锁定库存" value={summary.locked} />
      </div>

      <section className="flex flex-col gap-3 border-y border-slate-100 bg-white py-4 md:flex-row md:items-center">
        <label className="flex min-w-0 flex-1 items-center gap-3 rounded-lg border border-slate-200 bg-white px-3 py-2">
          <Warehouse className="h-4 w-4 text-slate-400" />
          <select
            value={storeId ?? ''}
            onChange={(event) => setStoreId(event.target.value ? Number(event.target.value) : undefined)}
            className="min-w-0 flex-1 bg-transparent text-sm font-bold outline-none"
          >
            <option value="">全部门店</option>
            {stores.map((store) => <option key={store.id} value={store.id}>{store.storeName}</option>)}
          </select>
        </label>
        <label className="flex items-center gap-2 text-sm font-bold text-slate-600">
          <input
            type="checkbox"
            checked={lowStockOnly}
            onChange={(event) => setLowStockOnly(event.target.checked)}
            className="h-4 w-4 accent-primary"
          />
          仅看低库存
        </label>
      </section>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <section className="overflow-hidden border border-slate-200 bg-white">
          <div className="grid grid-cols-[minmax(0,1fr)_90px_90px_90px] gap-3 border-b border-slate-200 bg-slate-50 px-4 py-3 text-[11px] font-black uppercase tracking-wide text-slate-500">
            <span>商品 / 门店</span><span>实物</span><span>锁定</span><span>可售</span>
          </div>
          {loading ? (
            <div className="p-10 text-center text-sm font-bold text-slate-400">加载中...</div>
          ) : stocks.length === 0 ? (
            <div className="p-10 text-center text-sm font-bold text-slate-400">暂无门店库存，请在右侧调整面板入库。</div>
          ) : stocks.map((stock) => (
            <button
              key={stock.id}
              onClick={() => selectStock(stock)}
              className={`grid w-full grid-cols-[minmax(0,1fr)_90px_90px_90px] gap-3 border-b border-slate-100 px-4 py-4 text-left hover:bg-slate-50 ${
                selectedStock?.id === stock.id ? 'bg-primary/5' : ''
              }`}
            >
              <span className="min-w-0">
                <span className="block truncate text-sm font-black text-slate-900">{stock.productName || `商品 #${stock.productId}`}</span>
                <span className="block truncate text-xs font-bold text-slate-400">{stock.storeName || `门店 #${stock.storeId}`} · {stock.productCode || '--'}</span>
              </span>
              <span className="text-sm font-black text-slate-700">{stock.quantity}</span>
              <span className="text-sm font-black text-amber-600">{stock.lockedQuantity}</span>
              <span className="text-sm font-black text-emerald-600">{stock.availableQuantity}</span>
            </button>
          ))}
        </section>

        <aside className="flex flex-col gap-6">
          <section className="border border-slate-200 bg-white p-5">
            <div className="mb-5 flex items-center gap-2">
              <PackagePlus className="h-5 w-5 text-primary" />
              <h2 className="text-base font-black text-slate-900">库存调整</h2>
            </div>
            <div className="flex flex-col gap-4">
              <SelectField label="门店" value={adjustment.storeId} onChange={(value) => setAdjustment((current) => ({ ...current, storeId: value }))}>
                <option value="0">请选择门店</option>
                {stores.map((store) => <option key={store.id} value={store.id}>{store.storeName}</option>)}
              </SelectField>
              <SelectField label="商品" value={adjustment.productId} onChange={(value) => setAdjustment((current) => ({ ...current, productId: value }))}>
                <option value="0">请选择商品</option>
                {products.map((product) => <option key={product.id} value={product.id}>{product.name} / {product.productCode}</option>)}
              </SelectField>
              <label className="flex flex-col gap-2">
                <span className="text-xs font-black text-slate-500">调整数量</span>
                <input
                  type="number"
                  value={adjustment.delta || ''}
                  onChange={(event) => setAdjustment((current) => ({ ...current, delta: Number(event.target.value) }))}
                  placeholder="正数入库，负数出库"
                  className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-bold outline-none focus:border-primary"
                />
              </label>
              <label className="flex flex-col gap-2">
                <span className="text-xs font-black text-slate-500">备注</span>
                <input
                  value={adjustment.remark || ''}
                  onChange={(event) => setAdjustment((current) => ({ ...current, remark: event.target.value }))}
                  placeholder="如：盘点入库"
                  className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-bold outline-none focus:border-primary"
                />
              </label>
              <button
                onClick={() => void submitAdjustment()}
                disabled={saving}
                className="inline-flex items-center justify-center gap-2 rounded-lg bg-primary px-4 py-3 text-sm font-black text-white disabled:opacity-50"
              >
                {adjustment.delta < 0 ? <Minus className="h-4 w-4" /> : <PackagePlus className="h-4 w-4" />}
                {saving ? '保存中...' : '确认调整'}
              </button>
            </div>
          </section>

          <section className="border border-slate-200 bg-white">
            <div className="flex items-center gap-2 border-b border-slate-200 px-5 py-4">
              <History className="h-4 w-4 text-slate-500" />
              <h2 className="text-sm font-black text-slate-900">库存流水</h2>
            </div>
            {selectedStock ? (
              <div className="divide-y divide-slate-100">
                {logs.length === 0 ? (
                  <p className="p-5 text-sm font-bold text-slate-400">暂无流水</p>
                ) : logs.map((log) => (
                  <div key={log.id} className="px-5 py-3">
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-xs font-black text-slate-700">{log.changeType}</span>
                      <span className={`text-sm font-black ${log.changeQuantity >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
                        {log.changeQuantity >= 0 ? '+' : ''}{log.changeQuantity}
                      </span>
                    </div>
                    <p className="mt-1 truncate text-xs font-medium text-slate-400">{log.remark || log.bizNo || '--'}</p>
                  </div>
                ))}
              </div>
            ) : (
              <p className="p-5 text-sm font-bold text-slate-400">选择左侧库存项后查看流水</p>
            )}
          </section>
        </aside>
      </div>
    </div>
  );
}

function Summary({ label, value }: { label: string; value: number }) {
  return (
    <div className="border border-slate-200 bg-white px-5 py-4">
      <div className="text-xs font-black text-slate-500">{label}</div>
      <div className="mt-2 text-3xl font-black text-slate-900">{value}</div>
    </div>
  );
}

function SelectField({
  label,
  value,
  onChange,
  children,
}: {
  label: string;
  value: number;
  onChange: (value: number) => void;
  children: React.ReactNode;
}) {
  return (
    <label className="flex flex-col gap-2">
      <span className="text-xs font-black text-slate-500">{label}</span>
      <select
        value={value}
        onChange={(event) => onChange(Number(event.target.value))}
        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-bold outline-none focus:border-primary"
      >
        {children}
      </select>
    </label>
  );
}
