import { useEffect, useMemo, useState } from 'react';
import { ArrowUpDown, Plus, Search, Store } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { adminMerchantService } from '../services/modules/adminMerchant';
import type { AdminMerchantListItem } from '../types/admin';

export default function AdminMerchants() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined);
  const [merchants, setMerchants] = useState<AdminMerchantListItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadMerchants() {
      try {
        const result = await adminMerchantService.listMerchants({
          current: 1,
          size: 50,
          name: keyword.trim() || undefined,
          status: statusFilter,
        });
        if (!isMounted) return;
        setMerchants(result.records ?? []);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('商户列表加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadMerchants();

    return () => {
      isMounted = false;
    };
  }, [keyword, statusFilter]);

  const activeCount = useMemo(
    () => merchants.filter((merchant) => merchant.status === 1).length,
    [merchants],
  );

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header className="flex flex-col justify-between gap-6 md:flex-row md:items-end">
        <div>
          <h1 className="text-3xl font-black tracking-tight text-slate-900">商户治理中心</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            商户列表、详情、启停用和编辑能力都已接入真实接口。
          </p>
        </div>
        <button
          onClick={() => navigate('/admin/merchant/new')}
          className="flex items-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-black text-white shadow-lg shadow-primary/20 transition-all hover:scale-105"
        >
          <Plus className="h-4 w-4" /> 新建商户
        </button>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard label="当前列表商户" value={String(merchants.length)} />
        <SummaryCard label="启用中商户" value={String(activeCount)} />
        <SummaryCard label="禁用中商户" value={String(merchants.length - activeCount)} />
      </div>

      <div className="rounded-[24px] border border-slate-100 bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-4 md:flex-row md:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="按商户名称搜索..."
              className="w-full rounded-2xl border border-slate-100 bg-slate-50 py-3 pl-11 pr-4 text-sm font-medium outline-none transition-all focus:border-primary focus:bg-white"
            />
          </div>
          <div className="flex gap-2">
            {[
              { label: '全部', value: undefined },
              { label: '启用', value: 1 },
              { label: '禁用', value: 0 },
            ].map((item) => (
              <button
                key={item.label}
                onClick={() => setStatusFilter(item.value)}
                className={`rounded-xl px-4 py-2 text-xs font-black uppercase tracking-widest transition-all ${
                  statusFilter === item.value
                    ? 'bg-primary text-white shadow-lg shadow-primary/20'
                    : 'bg-slate-100 text-slate-500 hover:text-slate-700'
                }`}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-xl shadow-slate-100/40">
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead className="bg-slate-50/50">
              <tr>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  商户主体
                </th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  联系方式
                </th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  状态
                </th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  创建时间
                </th>
                <th className="px-8 py-5"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {(isLoading ? Array.from({ length: 5 }) : merchants).map((merchant, index) => {
                const isData = typeof merchant === 'object';
                return (
                  <tr
                    key={isData ? merchant.id : index}
                    className="cursor-pointer transition-colors hover:bg-slate-50/50"
                    onClick={() => isData && navigate(`/admin/merchant/${merchant.id}`)}
                  >
                    <td className="px-8 py-6">
                      <div className="flex items-center gap-4">
                        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 text-slate-500">
                          <Store className="h-5 w-5" />
                        </div>
                        <div>
                          <p className="text-sm font-black text-slate-900">
                            {isData ? merchant.name : '加载中...'}
                          </p>
                          <p className="mt-1 text-xs font-medium text-slate-400">
                            {isData ? merchant.tenantCode : '--'}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-8 py-6">
                      <p className="text-sm font-bold text-slate-700">
                        {isData ? merchant.contactName || '--' : '--'}
                      </p>
                      <p className="mt-1 text-xs font-medium text-slate-400">
                        {isData ? merchant.contactPhone || '--' : '--'}
                      </p>
                    </td>
                    <td className="px-8 py-6">
                      <span
                        className={`rounded-lg px-3 py-1 text-[10px] font-black uppercase tracking-widest ${
                          isData && merchant.status === 1
                            ? 'bg-green-100 text-green-700'
                            : 'bg-slate-200 text-slate-500'
                        }`}
                      >
                        {isData ? (merchant.status === 1 ? '启用中' : '已禁用') : '...'}
                      </span>
                    </td>
                    <td className="px-8 py-6 text-sm font-medium text-slate-500">
                      {isData ? formatDateTime(merchant.createTime) : '--'}
                    </td>
                    <td className="px-8 py-6 text-right">
                      {isData && (
                        <button
                          onClick={(event) => {
                            event.stopPropagation();
                            navigate(`/admin/merchant/edit/${merchant.id}`);
                          }}
                          className="rounded-xl border border-slate-200 px-4 py-2 text-xs font-black text-slate-600 transition-all hover:border-primary hover:text-primary"
                        >
                          编辑
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
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

function formatDateTime(value?: string | null) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date);
}
