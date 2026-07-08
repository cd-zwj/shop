import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  Clock,
  Copy,
  ExternalLink,
  Package,
  RefreshCw,
  Truck,
} from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useToast } from '../context/ToastContext';
import { appPurchasesService, type DeliveryStatus, type ProductType, type PurchaseRecord } from '../services/modules/appPurchases';
import { EmptyState } from '../components/ui/EmptyState';
import { cn } from '../lib/utils';
import {
  getPurchaseDeliveryPresentation,
  parseDeliveryPayload,
} from '../utils/purchaseDelivery';
import { getErrorMessage } from '../utils/errorMessage';
import { getPageTotalPages } from '../utils/pageResult';

/* ------------------------------------------------------------------ */
/*  Constants                                                          */
/* ------------------------------------------------------------------ */

const TABS: { key: 'ALL' | DeliveryStatus; label: string }[] = [
  { key: 'ALL', label: '全部' },
  { key: 'PENDING', label: '待交付' },
  { key: 'DELIVERING', label: '交付中' },
  { key: 'DELIVERED', label: '已交付' },
  { key: 'CONFIRMED', label: '已确认' },
  { key: 'FAILED', label: '交付失败' },
  { key: 'REVOKED', label: '已撤销' },
  { key: 'REVOKE_FAILED', label: '撤销失败' },
];

const PAGE_SIZE = 50;

const STATUS_STYLE: Record<DeliveryStatus, { label: string; cls: string }> = {
  PENDING: { label: '待交付', cls: 'bg-amber-50 text-amber-700 border-amber-200' },
  DELIVERING: { label: '交付中', cls: 'bg-sky-50 text-sky-700 border-sky-200' },
  DELIVERED: { label: '已交付', cls: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  CONFIRMED: { label: '已确认', cls: 'bg-slate-100 text-slate-600 border-slate-200' },
  FAILED: { label: '交付失败', cls: 'bg-rose-50 text-rose-700 border-rose-200' },
  REVOKED: { label: '已撤销', cls: 'bg-slate-100 text-slate-500 border-slate-200' },
  REVOKE_FAILED: { label: '撤销失败', cls: 'bg-rose-50 text-rose-700 border-rose-200' },
};

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */

function formatTime(t?: string | null): string {
  if (!t) return '';
  const d = new Date(t);
  if (Number.isNaN(d.getTime())) return t;
  return d.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-');
}

/* ------------------------------------------------------------------ */
/*  Page                                                               */
/* ------------------------------------------------------------------ */

export default function MyPurchases() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { showToast } = useToast();
  const [tab, setTab] = useState<'ALL' | DeliveryStatus>('ALL');
  const [items, setItems] = useState<PurchaseRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const orderNoFilter = searchParams.get('orderNo')?.trim() || '';

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    try {
      const res = await appPurchasesService.list(
        tab === 'ALL' ? undefined : tab,
        currentPage,
        PAGE_SIZE,
        orderNoFilter || undefined,
      );
      setItems(res.records || []);
      setTotalPages(Math.max(1, getPageTotalPages(res)));
    } catch (err) {
      const msg = getErrorMessage(err, '加载失败');
      setItems([]);
      setTotalPages(1);
      setLoadError(msg);
      showToast(msg, 'error');
    } finally {
      setLoading(false);
    }
  }, [currentPage, orderNoFilter, tab, showToast]);

  const visibleItems = useMemo(
    () => orderNoFilter ? items.filter((item) => item.orderNo === orderNoFilter) : items,
    [items, orderNoFilter],
  );

  useEffect(() => {
    void load();
  }, [load]);

  const handleTabChange = (nextTab: 'ALL' | DeliveryStatus) => {
    setTab(nextTab);
    setCurrentPage(1);
  };

  const handlePageChange = (nextPage: number) => {
    if (nextPage < 1 || nextPage > totalPages || nextPage === currentPage) return;
    setCurrentPage(nextPage);
  };

  async function handleConfirm(id: number) {
    try {
      await appPurchasesService.confirm(id);
      showToast('已确认', 'success');
      void load();
    } catch (err) {
      const msg = err instanceof Error ? err.message : '操作失败';
      showToast(msg, 'error');
    }
  }

  async function handleCopy(text: string) {
    try {
      await navigator.clipboard.writeText(text);
      showToast('已复制到剪贴板', 'success');
    } catch {
      showToast('复制失败', 'error');
    }
  }

  return (
    <div className="flex flex-col gap-4 pb-20">
      {/* Header */}
      <section className="flex items-center gap-3 bg-white px-4 py-4 shadow-sm">
        <button
          onClick={() => navigate(-1)}
          className="flex h-9 w-9 items-center justify-center rounded-full bg-slate-50 text-slate-600 hover:bg-slate-100"
          aria-label="返回"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <h1 className="text-lg font-black tracking-tight text-slate-900">我的已购</h1>
      </section>

      {/* Tabs */}
      <section className="px-4">
        <div className="flex gap-2 overflow-x-auto rounded-2xl bg-white p-2 shadow-sm">
          {TABS.map((t) => (
            <button
              key={t.key}
              onClick={() => handleTabChange(t.key)}
              className={cn(
                'flex-shrink-0 rounded-xl px-4 py-2 text-sm font-bold transition-all',
                tab === t.key ? 'bg-slate-900 text-white' : 'text-slate-500 hover:bg-slate-50',
              )}
            >
              {t.label}
            </button>
          ))}
        </div>
        {orderNoFilter && (
          <div className="mt-3 rounded-2xl border border-blue-100 bg-blue-50 px-4 py-3 text-xs font-semibold text-blue-700">
            正在查看订单 {orderNoFilter} 的履约记录。
          </div>
        )}
      </section>

      {/* List */}
      <section className="flex flex-col gap-3 px-4">
        {loading && items.length === 0 ? (
          <div className="py-12 text-center text-sm text-slate-400">加载中...</div>
        ) : loadError ? (
          <div className="flex flex-col items-center justify-center rounded-3xl border border-red-100 bg-red-50/70 px-4 py-14 text-center shadow-sm">
            <div className="mb-4 rounded-full bg-white p-4 text-red-500 shadow-sm">
              <AlertCircle className="h-10 w-10" />
            </div>
            <h2 className="text-base font-black text-slate-900">交付记录加载失败</h2>
            <p className="mt-1.5 max-w-xs text-xs font-semibold leading-relaxed text-slate-500">
              {loadError}
            </p>
            <button
              type="button"
              onClick={() => void load()}
              className="mt-5 inline-flex items-center gap-2 rounded-2xl bg-slate-900 px-4 py-2.5 text-xs font-black text-white transition-all hover:bg-slate-800 active:scale-95"
            >
              <RefreshCw className="h-4 w-4" />
              重试
            </button>
          </div>
        ) : visibleItems.length === 0 ? (
          <EmptyState
            icon={<Package className="h-6 w-6" />}
            title={orderNoFilter ? '暂无该订单的履约记录' : '还没有已购商品'}
            subtitle={orderNoFilter ? '如果订单刚完成支付，请稍后刷新查看交付状态。' : '支付完成后,商品交付信息会在这里展示'}
          />
        ) : (
          visibleItems.map((item) => (
            <PurchaseCard
              key={item.id}
              record={item}
              onConfirm={() => handleConfirm(item.id)}
              onCopy={handleCopy}
              onOpenOrder={() => navigate(`/order/${encodeURIComponent(item.orderNo)}`)}
              onContactMerchant={(path) => navigate(path)}
            />
          ))
        )}
      </section>

      {!loadError && totalPages > 1 && (
        <section className="flex items-center justify-center gap-3 px-4">
          <button
            type="button"
            aria-label="上一页"
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage <= 1 || loading}
            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-black text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            上一页
          </button>
          <span className="min-w-16 text-center text-sm font-black text-slate-600">{currentPage} / {totalPages}</span>
          <button
            type="button"
            aria-label="下一页"
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage >= totalPages || loading}
            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-black text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            下一页
          </button>
        </section>
      )}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Card                                                               */
/* ------------------------------------------------------------------ */

interface CardProps {
  record: PurchaseRecord;
  onConfirm: () => void;
  onCopy: (text: string) => void;
  onOpenOrder: () => void;
  onContactMerchant: (path: string) => void;
}

function PurchaseCard({ record, onConfirm, onCopy, onOpenOrder, onContactMerchant }: CardProps) {
  const payload = useMemo(() => parseDeliveryPayload(record.payload), [record.payload]);
  const presentation = useMemo(() => getPurchaseDeliveryPresentation(record), [record]);
  const statusStyle = STATUS_STYLE[record.status] ?? STATUS_STYLE.PENDING;

  return (
    <article className="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm">
      <header className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-xs font-bold text-slate-400">
          <span>订单 {record.orderNo}</span>
        </div>
        <span className={cn('rounded-full border px-2.5 py-0.5 text-[11px] font-black', statusStyle.cls)}>
          {statusStyle.label}
        </span>
      </header>

      <div className="mt-3 flex items-center gap-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-50 text-slate-400">
          <IconForType type={record.productType} />
        </div>
        <div className="flex flex-1 flex-col">
          <span className="text-sm font-black text-slate-900">{presentation.title}</span>
          <span className="text-xs text-slate-400">{presentation.subtitle}</span>
          <span className="mt-0.5 text-[11px] text-slate-400">下单时间 {formatTime(record.createTime)}</span>
        </div>
      </div>

      <p className="mt-3 rounded-xl bg-slate-50 px-3 py-2 text-xs font-medium text-slate-600">
        {presentation.guidance}
      </p>

      {/* Payload 渲染：按类型展示对应字段 */}
      <DeliveryDetail record={record} payload={payload} onCopy={onCopy} />

      {/* 操作区 */}
      {(presentation.primaryAction || record.status === 'DELIVERED') && (
        <div className="mt-4 flex justify-end">
          <PrimaryActionButton
            action={presentation.primaryAction}
            onConfirm={onConfirm}
            onCopy={onCopy}
            onOpenOrder={onOpenOrder}
            onContactMerchant={onContactMerchant}
          />
        </div>
      )}
    </article>
  );
}

function IconForType({ type }: { type: ProductType }) {
  switch (type) {
    case 'PHYSICAL':
      return <Truck className="h-5 w-5" />;
    case 'VIRTUAL':
      return <ExternalLink className="h-5 w-5" />;
    case 'CARD_KEY':
      return <Copy className="h-5 w-5" />;
    case 'SERVICE':
      return <CheckCircle2 className="h-5 w-5" />;
    case 'SUBSCRIPTION':
      return <Clock className="h-5 w-5" />;
    default:
      return <Package className="h-5 w-5" />;
  }
}

interface DetailProps {
  record: PurchaseRecord;
  payload: Record<string, unknown> | null;
  onCopy: (text: string) => void;
}

function DeliveryDetail({ record, payload, onCopy }: DetailProps) {
  if (record.status === 'PENDING' || record.status === 'DELIVERING') {
    return (
      <p className="mt-3 rounded-xl bg-amber-50 px-3 py-2 text-xs font-medium text-amber-700">
        {record.productType === 'PHYSICAL' ? '等待商户发货...' : '正在准备交付内容...'}
      </p>
    );
  }
  if (record.status === 'REVOKED') {
    return (
      <p className="mt-3 rounded-xl bg-slate-50 px-3 py-2 text-xs font-medium text-slate-500">该交付已撤销(退款)</p>
    );
  }
  if (!payload) {
    return null;
  }

  if (record.productType === 'VIRTUAL') {
    const url = payload.contentUrl as string | undefined;
    const account = payload.accountInfo as string | undefined;
    return (
      <div className="mt-3 flex flex-col gap-2 rounded-xl bg-emerald-50 px-3 py-2.5">
        {url && (
          <a
            href={url}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 text-xs font-black text-emerald-700 hover:underline"
          >
            <ExternalLink className="h-3.5 w-3.5" />
            打开交付内容
          </a>
        )}
        {account && (
          <div className="flex items-center justify-between gap-2">
            <span className="text-xs font-medium text-emerald-700">账号: {account}</span>
            <button onClick={() => onCopy(account)} className="text-emerald-600 hover:text-emerald-800">
              <Copy className="h-3.5 w-3.5" />
            </button>
          </div>
        )}
      </div>
    );
  }
  if (record.productType === 'CARD_KEY') {
    const code = (payload.code as string) || '';
    return (
      <div className="mt-3 flex items-center justify-between gap-3 rounded-xl bg-slate-900 px-3 py-2.5">
        <span className="font-mono text-sm font-black text-white">{code}</span>
        {code && (
          <button onClick={() => onCopy(code)} className="text-slate-300 hover:text-white">
            <Copy className="h-3.5 w-3.5" />
          </button>
        )}
      </div>
    );
  }
  if (record.productType === 'SERVICE') {
    const code = (payload.verifyCode as string) || '';
    return (
      <div className="mt-3 rounded-xl bg-sky-50 px-3 py-2.5">
        <p className="text-xs font-medium text-sky-700">到店出示核销码</p>
        <p className="mt-1 font-mono text-lg font-black tracking-widest text-sky-900">{code}</p>
      </div>
    );
  }
  if (record.productType === 'SUBSCRIPTION') {
    const days = payload.validityDays as number | undefined;
    return (
      <div className="mt-3 rounded-xl bg-violet-50 px-3 py-2.5 text-xs font-medium text-violet-700">
        权益已激活{days ? `,有效期 ${days} 天` : ''}
        {record.expireTime ? `,到期 ${formatTime(record.expireTime)}` : ''}
      </div>
    );
  }
  if (record.productType === 'PHYSICAL') {
    const sn = payload.shippingNo as string | undefined;
    const company = payload.logisticsCompany as string | undefined;
    if (!sn) return null;
    return (
      <div className="mt-3 rounded-xl bg-slate-50 px-3 py-2.5 text-xs">
        <p className="font-bold text-slate-700">物流单号: {sn}</p>
        {company && <p className="mt-0.5 text-slate-500">承运商: {company}</p>}
      </div>
    );
  }
  return null;
}

function PrimaryActionButton({
  action,
  onConfirm,
  onCopy,
  onOpenOrder,
  onContactMerchant,
}: {
  action?: ReturnType<typeof getPurchaseDeliveryPresentation>['primaryAction'];
  onConfirm: () => void;
  onCopy: (text: string) => void;
  onOpenOrder: () => void;
  onContactMerchant: (path: string) => void;
}) {
  if (!action) {
    return (
      <button
        onClick={onConfirm}
        className="flex items-center gap-1.5 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-black text-white hover:bg-slate-800"
      >
        <CheckCircle2 className="h-3.5 w-3.5" />
        确认收货
      </button>
    );
  }

  if (action.kind === 'open' && action.value) {
    return (
      <a
        href={action.value}
        target="_blank"
        rel="noreferrer"
        className="flex items-center gap-1.5 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-black text-white hover:bg-slate-800"
      >
        <ExternalLink className="h-3.5 w-3.5" />
        {action.label}
      </a>
    );
  }

  if (action.kind === 'copy' && action.value) {
    return (
      <button
        onClick={() => onCopy(action.value ?? '')}
        className="flex items-center gap-1.5 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-black text-white hover:bg-slate-800"
      >
        <Copy className="h-3.5 w-3.5" />
        {action.label}
      </button>
    );
  }

  if (action.kind === 'confirm') {
    return (
      <button
        onClick={onConfirm}
        className="flex items-center gap-1.5 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-black text-white hover:bg-slate-800"
      >
        <CheckCircle2 className="h-3.5 w-3.5" />
        {action.label}
      </button>
    );
  }

  if (action.kind === 'contact') {
    return (
      <button
        onClick={() => action.value ? onContactMerchant(action.value) : onOpenOrder()}
        className="flex items-center gap-1.5 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-black text-white hover:bg-slate-800"
      >
        <ExternalLink className="h-3.5 w-3.5" />
        {action.label}
      </button>
    );
  }

  return (
    <button
      onClick={onOpenOrder}
      className="flex items-center gap-1.5 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-black text-white hover:bg-slate-800"
    >
      <ExternalLink className="h-3.5 w-3.5" />
      {action.label}
    </button>
  );
}
