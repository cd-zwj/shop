import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, Package, Receipt } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { adminTradeService } from '../services/modules/adminTrade';
import type { AdminOrderDetail } from '../types/admin';
import { formatCurrency } from '../utils/display';

export default function AdminOrderDetail() {
  const navigate = useNavigate();
  const { id } = useParams();
  const orderNo = id || '';
  const [detail, setDetail] = useState<AdminOrderDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadDetail() {
      if (!orderNo) {
        setError('订单号为空');
        setIsLoading(false);
        return;
      }

      try {
        const result = await adminTradeService.getOrderDetail(orderNo);
        if (!isMounted) return;
        setDetail(result);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('订单详情加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadDetail();

    return () => {
      isMounted = false;
    };
  }, [orderNo]);

  const totalQuantity = useMemo(
    () => (detail?.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0),
    [detail?.items],
  );

  return (
    <div className="mx-auto flex w-full max-w-7xl flex-col gap-8 p-4 pb-32 md:p-8">
      <header>
        <button
          onClick={() => navigate(-1)}
          className="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-widest text-primary transition-all hover:gap-3"
        >
          <ArrowLeft className="h-4 w-4" /> 返回订单列表
        </button>
        <h1 className="text-4xl font-black tracking-tight text-slate-900">
          订单详情 {detail?.order?.orderNo ? `#${detail.order.orderNo}` : ''}
        </h1>
        <p className="mt-1 text-sm font-medium text-slate-500">
          当前页对接 `/v1/admin/orders/{'{orderNo}'}`，展示真实订单主体和订单项。
        </p>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
        <SummaryCard label="订单总额" value={formatCurrency(detail?.order?.totalAmount || 0)} />
        <SummaryCard label="外部支付" value={formatCurrency(detail?.order?.externalPayAmount || 0)} />
        <SummaryCard label="订单项数" value={String(detail?.items?.length ?? 0)} />
        <SummaryCard label="商品总数" value={String(totalQuantity)} />
      </div>

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm lg:col-span-7">
          <h2 className="text-sm font-black uppercase tracking-widest text-slate-900">订单主体</h2>
          <div className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-2">
            <DetailItem label="订单号" value={detail?.order?.orderNo} />
            <DetailItem label="租户 ID" value={detail?.order?.tenantId} />
            <DetailItem label="平台用户 ID" value={detail?.order?.platformUserId} />
            <DetailItem label="订单主题" value={detail?.order?.subject} />
            <DetailItem label="订单状态" value={detail?.order?.orderStatus} />
            <DetailItem label="支付状态" value={detail?.order?.payStatus} />
            <DetailItem label="钱包策略" value={detail?.order?.walletStrategy} />
            <DetailItem label="订单来源" value={detail?.order?.source} />
            <DetailItem label="统一钱包抵扣" value={formatCurrency(detail?.order?.unifiedWalletDeductAmount || 0)} />
            <DetailItem label="商户钱包抵扣" value={formatCurrency(detail?.order?.merchantWalletDeductAmount || 0)} />
            <DetailItem label="创建时间" value={formatDateTime(detail?.order?.createTime)} />
            <DetailItem label="更新时间" value={formatDateTime(detail?.order?.updateTime)} />
          </div>
        </section>

        <section className="flex flex-col gap-8 lg:col-span-5">
          <div className="rounded-[40px] bg-slate-900 p-8 text-white shadow-xl">
            <div className="flex items-center gap-3">
              <Receipt className="h-5 w-5 text-primary" />
              <span className="text-xs font-black uppercase tracking-widest">金额拆分</span>
            </div>
            <div className="mt-6 space-y-4">
              <MoneyRow label="总金额" value={formatCurrency(detail?.order?.totalAmount || 0)} />
              <MoneyRow label="折扣金额" value={formatCurrency(detail?.order?.discountAmount || 0)} />
              <MoneyRow label="钱包抵扣" value={formatCurrency(detail?.order?.walletDeductAmount || 0)} />
              <MoneyRow label="应付金额" value={formatCurrency(detail?.order?.payableAmount || 0)} />
            </div>
          </div>

          <div className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
            <div className="mb-5 flex items-center gap-3 text-primary">
              <Package className="h-5 w-5" />
              <span className="text-xs font-black uppercase tracking-widest">订单项列表</span>
            </div>
            <div className="flex flex-col gap-3">
              {(detail?.items || []).length === 0 ? (
                <div className="rounded-[28px] border border-dashed border-slate-200 px-6 py-10 text-center text-sm font-medium text-slate-400">
                  当前订单没有返回订单项。
                </div>
              ) : (
                detail?.items.map((item) => (
                  <div key={item.id} className="rounded-[24px] bg-slate-50 px-5 py-4">
                    <p className="text-sm font-black text-slate-900">{item.productName}</p>
                    <p className="mt-1 text-xs font-medium text-slate-500">
                      商品 ID {item.productId} · 数量 {item.quantity} · 单价 {formatCurrency(item.price)}
                    </p>
                    <p className="mt-2 text-sm font-black text-primary">小计 {formatCurrency(item.subtotal)}</p>
                  </div>
                ))
              )}
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[28px] border border-slate-100 bg-white p-6 shadow-sm">
      <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</p>
      <p className="mt-2 text-2xl font-black tracking-tight text-slate-900">{value}</p>
    </div>
  );
}

function DetailItem({ label, value }: { label: string; value?: string | number | null }) {
  return (
    <div>
      <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</p>
      <p className="mt-2 text-lg font-black text-slate-900">{value || '--'}</p>
    </div>
  );
}

function MoneyRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-sm font-medium text-slate-400">{label}</span>
      <span className="text-sm font-black text-white">{value}</span>
    </div>
  );
}

function formatDateTime(value?: string | null) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}
