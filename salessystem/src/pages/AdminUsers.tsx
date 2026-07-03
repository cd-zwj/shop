import { useEffect, useMemo, useState } from 'react';
import { Search, Shield, UserCheck, UserX, Users } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { adminUserService } from '../services/modules/adminUser';
import { ApiError } from '../types/api';
import type { AdminPlatformUser } from '../types/admin';
import { formatCurrency } from '../utils/display';

export default function AdminUsersPage() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined);
  const [users, setUsers] = useState<AdminPlatformUser[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadUsers() {
      try {
        const result = await adminUserService.listUsers({
          current: 1,
          size: 50,
          keyword: keyword.trim() || undefined,
          status: statusFilter,
        });
        if (!isMounted) return;
        setUsers(result.records ?? []);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('平台用户列表加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadUsers();

    return () => {
      isMounted = false;
    };
  }, [keyword, statusFilter]);

  const activeCount = useMemo(() => users.filter((user) => user.status === 1).length, [users]);

  async function toggleUserStatus(user: AdminPlatformUser) {
    setError('');
    try {
      if (user.status === 1) {
        await adminUserService.disableUser(user.id);
      } else {
        await adminUserService.enableUser(user.id);
      }
      const result = await adminUserService.listUsers({
        current: 1,
        size: 50,
        keyword: keyword.trim() || undefined,
        status: statusFilter,
      });
      setUsers(result.records ?? []);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '用户状态更新失败');
    }
  }

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header className="flex flex-col justify-between gap-6 md:flex-row md:items-end">
        <div>
          <h1 className="text-3xl font-black tracking-tight text-slate-900">平台用户治理</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            用户列表、详情、启停用与权限入口都已接到真实接口。
          </p>
        </div>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard label="当前列表用户" value={String(users.length)} />
        <SummaryCard label="启用中用户" value={String(activeCount)} />
        <SummaryCard label="已禁用用户" value={String(users.length - activeCount)} />
      </div>

      <div className="rounded-[24px] border border-slate-100 bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-4 md:flex-row md:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="按用户名、手机号或邮箱搜索..."
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
                  用户主体
                </th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  联系方式
                </th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  钱包余额
                </th>
                <th className="px-8 py-5 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  状态
                </th>
                <th className="px-8 py-5"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {(isLoading ? Array.from<AdminPlatformUser | undefined>({ length: 5 }) : users).map((user, index) => {                return (
                  <tr
                    key={user ? user.id : index}
                    className="cursor-pointer transition-colors hover:bg-slate-50/50"
                    onClick={() => user && navigate(`/admin/user/${user.id}`)}
                  >
                    <td className="px-8 py-6">
                      <div className="flex items-center gap-4">
                        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-slate-600">
                          <Users className="h-4 w-4" />
                        </div>
                        <div>
                          <p className="text-sm font-black text-slate-900">
                            {user ? user.username : '加载中...'}
                          </p>
                          <p className="mt-1 text-xs font-medium text-slate-400">
                            {user ? user.userNo || `ID ${user.id}` : '--'}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-8 py-6">
                      <p className="text-sm font-medium text-slate-700">{user ? user.phone || '--' : '--'}</p>
                      <p className="mt-1 text-xs font-medium text-slate-400">{user ? user.email || '--' : '--'}</p>
                    </td>
                    <td className="px-8 py-6 text-sm font-black text-slate-900">
                      {user ? formatCurrency(user.unifiedWalletBalance) : '--'}
                    </td>
                    <td className="px-8 py-6">
                      <span
                        className={`rounded-lg px-3 py-1 text-[10px] font-black uppercase tracking-widest ${
                          user && user.status === 1
                            ? 'bg-green-100 text-green-700'
                            : 'bg-slate-200 text-slate-500'
                        }`}
                      >
                        {user ? (user.status === 1 ? '启用中' : '已禁用') : '...'}
                      </span>
                    </td>
                    <td className="px-8 py-6">
                      {user && (
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={(event) => {
                              event.stopPropagation();
                              void toggleUserStatus(user);
                            }}
                            className="rounded-xl border border-slate-200 p-2 text-slate-500 transition-all hover:border-primary hover:text-primary"
                          >
                            {user.status === 1 ? (
                              <UserX className="h-4 w-4" />
                            ) : (
                              <UserCheck className="h-4 w-4" />
                            )}
                          </button>
                          <button
                            onClick={(event) => {
                              event.stopPropagation();
                              navigate(`/admin/user/${user.id}/permissions`);
                            }}
                            className="rounded-xl border border-slate-200 p-2 text-slate-500 transition-all hover:border-primary hover:text-primary"
                          >
                            <Shield className="h-4 w-4" />
                          </button>
                        </div>
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
