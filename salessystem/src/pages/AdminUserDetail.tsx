import { useEffect, useState } from 'react';
import { ArrowLeft, Key, Lock, Wallet } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { adminUserService } from '../services/modules/adminUser';
import { ApiError } from '../types/api';
import type { AdminPlatformUser } from '../types/admin';
import { formatCurrency } from '../utils/display';

export default function AdminUserDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const userId = Number(id);
  const [user, setUser] = useState<AdminPlatformUser | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadUser() {
      if (!Number.isFinite(userId)) {
        setError('用户编号无效');
        return;
      }

      try {
        const detail = await adminUserService.getUserDetail(userId);
        if (!isMounted) return;
        setUser(detail);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('用户详情加载失败，请稍后重试');
      }
    }

    void loadUser();

    return () => {
      isMounted = false;
    };
  }, [userId]);

  async function handleToggleStatus() {
    if (!user) return;
    setIsSubmitting(true);
    setError('');
    setSuccess('');
    try {
      if (user.status === 1) {
        await adminUserService.disableUser(user.id);
      } else {
        await adminUserService.enableUser(user.id);
      }
      const refreshed = await adminUserService.getUserDetail(user.id);
      setUser(refreshed);
      setSuccess(user.status === 1 ? '用户已禁用' : '用户已启用');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '用户状态更新失败');
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
            <ArrowLeft className="h-4 w-4" /> 返回用户列表
          </button>
          <h1 className="text-4xl font-black tracking-tight text-slate-900">
            {user?.username || '用户详情'}
          </h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            当前页展示真实用户基础资料、钱包余额、租户关联数量与状态。
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={() => navigate(`/admin/user/${userId}/permissions`)}
            disabled={!user}
            className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-6 py-3 text-sm font-black text-slate-700 transition-all hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
          >
            <Key className="h-4 w-4" /> 权限配置
          </button>
          <button
            onClick={handleToggleStatus}
            disabled={!user || isSubmitting}
            className="flex items-center gap-2 rounded-[24px] bg-primary px-6 py-3 text-sm font-black text-white shadow-xl shadow-primary/20 transition-all hover:scale-105 disabled:cursor-not-allowed disabled:opacity-70"
          >
            <Lock className="h-4 w-4" />
            {user?.status === 1 ? '禁用用户' : '启用用户'}
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
            <StatCard label="统一钱包余额" value={formatCurrency(user?.unifiedWalletBalance ?? 0)} />
            <StatCard label="会员租户数" value={String(user?.memberTenantCount ?? 0)} />
            <StatCard label="员工租户数" value={String(user?.employeeTenantCount ?? 0)} />
          </div>

          <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
            <h2 className="text-sm font-black uppercase tracking-widest text-slate-900">用户基础资料</h2>
            <div className="mt-8 grid grid-cols-1 gap-6 md:grid-cols-2">
              <DetailItem label="用户名" value={user?.username} />
              <DetailItem label="用户编号" value={user?.userNo || user?.id} />
              <DetailItem label="手机号" value={user?.phone} />
              <DetailItem label="邮箱" value={user?.email} />
              <DetailItem label="注册时间" value={formatDateTime(user?.createTime)} />
              <DetailItem label="当前状态" value={user?.status === 1 ? '启用中' : '已禁用'} />
            </div>
          </section>
        </div>

        <div className="flex flex-col gap-8 lg:col-span-4">
          <section className="rounded-[40px] bg-slate-900 p-8 text-white shadow-xl">
            <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">账户状态</p>
            <p className="mt-4 text-2xl font-black">{user?.status === 1 ? '启用中' : '已禁用'}</p>
            <p className="mt-2 text-sm font-medium text-slate-400">
              用户启停通过 `/v1/admin/users/{'{userId}'}/enable|disable` 真实接口处理。
            </p>
            <div className="mt-6 grid grid-cols-2 gap-4 border-t border-white/5 pt-6">
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">钱包</p>
                <p className="mt-1 text-lg font-black">{formatCurrency(user?.unifiedWalletBalance ?? 0)}</p>
              </div>
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">角色入口</p>
                <p className="mt-1 text-lg font-black">权限治理</p>
              </div>
            </div>
          </section>

          <section className="rounded-[40px] border border-blue-100 bg-blue-50 p-8">
            <div className="flex items-center gap-3 text-primary">
              <Wallet className="h-5 w-5" />
              <span className="text-xs font-black uppercase tracking-widest">接口说明</span>
            </div>
            <p className="mt-3 text-sm font-medium leading-relaxed text-blue-700">
              当前用户详情接口未返回消费日志、积分流水等明细，所以第 6 部分这里聚焦真实的用户主体信息和可治理字段，不再保留原来的 mock 行为轨迹。
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

function DetailItem({ label, value }: { label: string; value?: string | number | null }) {
  return (
    <div>
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
