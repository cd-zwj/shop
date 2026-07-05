import { useEffect, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Lock,
  ShieldCheck,
  Store,
  Wallet,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { adminMerchantService } from '../services/modules/adminMerchant';
import { ApiError } from '../types/api';
import type { AdminMerchantDetail } from '../types/admin';
import { formatCurrency } from '../utils/display';

export default function AdminMerchantDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const merchantId = Number(id);
  const [merchant, setMerchant] = useState<AdminMerchantDetail | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadDetail() {
      if (!Number.isFinite(merchantId)) {
        setError('商户编号无效');
        return;
      }

      try {
        const result = await adminMerchantService.getMerchantDetail(merchantId);
        if (!isMounted) return;
        setMerchant(result);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('商户详情加载失败，请稍后重试');
      }
    }

    void loadDetail();

    return () => {
      isMounted = false;
    };
  }, [merchantId]);

  async function handleToggleStatus() {
    if (!merchant) return;
    setIsSubmitting(true);
    setError('');
    setSuccess('');
    try {
      if (merchant.status === 1) {
        await adminMerchantService.disableMerchant(merchant.id);
      } else {
        await adminMerchantService.enableMerchant(merchant.id);
      }
      const refreshed = await adminMerchantService.getMerchantDetail(merchant.id);
      setMerchant(refreshed);
      setSuccess(merchant.status === 1 ? '商户已禁用' : '商户已启用');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '商户状态更新失败');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 p-4 pb-32 md:p-8">
      <header className="flex flex-col justify-between gap-6 sm:flex-row sm:items-end">
        <div>
          <button
            onClick={() => navigate(-1)}
            className="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-widest text-primary transition-all hover:gap-3"
          >
            <ArrowLeft className="h-4 w-4" /> 返回商户列表
          </button>
          <h1 className="text-4xl font-black tracking-tight text-slate-900">
            {merchant?.name || '商户详情'}
          </h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            真实接口字段主要包括基础信息、状态、商品数、订单数和累计销售额。
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={() => merchant && navigate(`/admin/merchant/edit/${merchant.id}`)}
            disabled={!merchant}
            className="rounded-2xl border border-slate-200 bg-white px-6 py-3 text-sm font-black text-slate-700 transition-all hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
          >
            编辑资料
          </button>
          <button
            onClick={handleToggleStatus}
            disabled={!merchant || isSubmitting}
            className="flex items-center gap-2 rounded-[24px] bg-primary px-6 py-3 text-sm font-black text-white shadow-xl shadow-primary/20 transition-all hover:scale-105 disabled:cursor-not-allowed disabled:opacity-70"
          >
            <Lock className="h-4 w-4" />
            {merchant?.status === 1 ? '禁用商户' : '启用商户'}
          </button>
        </div>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-green-100 bg-green-50 px-4 py-3 text-sm font-medium text-green-600">
          {success}
        </div>
      )}

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <div className="flex flex-col gap-8 lg:col-span-8">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <StatCard label="商品数量" value={String(merchant?.productCount ?? 0)} />
            <StatCard label="订单数量" value={String(merchant?.orderCount ?? 0)} />
            <StatCard label="累计销售额" value={formatCurrency(merchant?.totalSales ?? 0)} />
          </div>

          <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
            <h2 className="text-sm font-black uppercase tracking-widest text-slate-900">
              商户基础资料
            </h2>
            <div className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-2">
              <DetailItem label="商户名称" value={merchant?.name} />
              <DetailItem label="商户编码" value={merchant?.tenantCode} />
              <DetailItem label="联系人" value={merchant?.contactName} />
              <DetailItem label="联系电话" value={merchant?.contactPhone} />
              <DetailItem label="创建时间" value={formatDateTime(merchant?.createTime)} />
              <DetailItem label="当前状态" value={merchant?.status === 1 ? '启用中' : '已禁用'} />
              <DetailItem
                label="联系地址"
                value={merchant?.address}
                className="md:col-span-2"
              />
            </div>
          </section>
        </div>

        <div className="flex flex-col gap-8 lg:col-span-4">
          <section className="rounded-[40px] bg-slate-900 p-8 text-white shadow-xl">
            <div className="flex items-center gap-4">
              <div className="rounded-3xl bg-white/10 p-4">
                <Store className="h-8 w-8 text-primary" />
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">商户状态</p>
                <p className="mt-1 text-2xl font-black">
                  {merchant?.status === 1 ? '启用中' : '已禁用'}
                </p>
              </div>
            </div>
            <div className="mt-6 grid grid-cols-2 gap-4 border-t border-white/5 pt-6">
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">订单数</p>
                <p className="mt-1 text-lg font-black">{merchant?.orderCount ?? 0}</p>
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">商品数</p>
                <p className="mt-1 text-lg font-black">{merchant?.productCount ?? 0}</p>
              </div>
            </div>
          </section>

          <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
            <div className="mb-5 flex items-center gap-3 text-primary">
              <ShieldCheck className="h-5 w-5" />
              <span className="text-xs font-black uppercase tracking-widest">管理入口</span>
            </div>
            <div className="flex flex-col gap-3">
              {[
                { label: '编辑商户资料', action: () => merchant && navigate(`/admin/merchant/edit/${merchant.id}`) },
                { label: '查看提现审核', action: () => navigate('/admin/withdrawals') },
                { label: '查看交易总览', action: () => navigate('/admin/transactions') },
              ].map((item) => (
                <button
                  key={item.label}
                  onClick={item.action}
                  className="flex items-center justify-between rounded-[24px] bg-slate-50 px-5 py-4 text-left transition-all hover:bg-slate-100"
                >
                  <span className="text-sm font-black text-slate-800">{item.label}</span>
                  <ArrowRight className="h-4 w-4 text-slate-400" />
                </button>
              ))}
            </div>
          </section>

          <section className="rounded-[40px] border border-blue-100 bg-blue-50 p-8">
            <div className="flex items-center gap-3 text-primary">
              <Wallet className="h-5 w-5" />
              <span className="text-xs font-black uppercase tracking-widest">接口说明</span>
            </div>
            <p className="mt-3 text-sm font-medium leading-relaxed text-blue-700">
              当前详情接口没有直接返回商户钱包余额，所以管理端第 6 部分这里只展示商户基础档案和业务统计，不再继续沿用 mock 的结算卡片数值。
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[28px] border border-slate-100 bg-white p-6 shadow-sm">
      <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</p>
      <p className="mt-2 text-2xl font-black tracking-tight text-slate-900">{value}</p>
    </div>
  );
}

function DetailItem({
  label,
  value,
  className = '',
}: {
  label: string;
  value?: string | number | null;
  className?: string;
}) {
  return (
    <div className={className}>
      <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</p>
      <p className="mt-2 text-lg font-black text-slate-900">{value || '--'}</p>
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
