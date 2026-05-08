import { useEffect, useMemo, useState } from 'react';
import { KeyRound, Search, ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { adminUserService } from '../services/modules/adminUser';
import type { AdminPermission, AdminPermissionCatalog } from '../types/admin';

export default function AdminPermissionsPage() {
  const { adminSession } = useAuth();
  const [catalog, setCatalog] = useState<AdminPermissionCatalog>({});
  const [keyword, setKeyword] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadPermissions() {
      try {
        const result = await adminUserService.listPermissions();
        if (!isMounted) return;
        setCatalog(result);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('权限目录加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadPermissions();

    return () => {
      isMounted = false;
    };
  }, []);

  const moduleEntries = useMemo(() => {
    const lowered = keyword.trim().toLowerCase();
    return (Object.entries(catalog) as Array<[string, AdminPermission[]]>)
      .map(([moduleName, permissions]) => {
        const filtered = permissions.filter((permission) => {
          if (!lowered) return true;
          return (
            permission.permissionCode.toLowerCase().includes(lowered) ||
            permission.permissionName.toLowerCase().includes(lowered) ||
            (permission.description || '').toLowerCase().includes(lowered)
          );
        });
        return [moduleName, filtered] as const;
      })
      .filter(([, permissions]) => permissions.length > 0);
  }, [catalog, keyword]);

  const totalPermissions = useMemo(
    () =>
      (Object.values(catalog) as AdminPermission[][]).reduce(
        (sum, permissions) => sum + permissions.length,
        0,
      ),
    [catalog],
  );

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header>
        <h1 className="text-3xl font-black tracking-tight text-slate-900">权限目录与访问治理</h1>
        <p className="mt-1 text-sm font-medium text-slate-500">
          当前页展示真实权限目录，以及管理员会话上已拥有的权限集合。
        </p>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard label="权限模块数" value={String(Object.keys(catalog).length)} />
        <SummaryCard label="权限总数" value={String(totalPermissions)} />
        <SummaryCard label="当前管理员权限" value={String(adminSession?.permissions?.length ?? 0)} />
      </div>

      <div className="rounded-[24px] border border-slate-100 bg-white p-4 shadow-sm">
        <div className="relative">
          <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="按权限码、权限名或描述搜索..."
            className="w-full rounded-2xl border border-slate-100 bg-slate-50 py-3 pl-11 pr-4 text-sm font-medium outline-none transition-all focus:border-primary focus:bg-white"
          />
        </div>
      </div>

      <section className="rounded-[40px] bg-slate-900 p-8 text-white shadow-xl">
        <div className="flex items-center gap-3">
          <ShieldCheck className="h-5 w-5 text-primary" />
          <span className="text-xs font-black uppercase tracking-widest">当前管理员权限快照</span>
        </div>
        <div className="mt-5 flex flex-wrap gap-2">
          {(adminSession?.permissions || []).length === 0 ? (
            <span className="text-sm font-medium text-slate-400">当前会话未返回权限码。</span>
          ) : (
            adminSession?.permissions.map((permissionCode) => (
              <span
                key={permissionCode}
                className="rounded-xl bg-white/10 px-3 py-2 text-xs font-black tracking-widest text-white"
              >
                {permissionCode}
              </span>
            ))
          )}
        </div>
      </section>

      <div className="grid grid-cols-1 gap-6">
        {moduleEntries.map(([moduleName, permissions]) => (
          <section
            key={moduleName}
            className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm"
          >
            <div className="mb-6 flex items-center justify-between">
              <div>
                <h2 className="text-xl font-black tracking-tight text-slate-900">
                  {moduleName || '未分组模块'}
                </h2>
                <p className="mt-1 text-sm font-medium text-slate-500">
                  共 {permissions.length} 个权限点
                </p>
              </div>
              {isLoading && (
                <span className="rounded-xl bg-slate-100 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  loading
                </span>
              )}
            </div>
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {permissions.map((permission) => (
                <div key={permission.id} className="rounded-[28px] bg-slate-50 p-5">
                  <div className="flex items-start gap-3">
                    <div className="rounded-2xl bg-white p-3 text-primary shadow-sm">
                      <KeyRound className="h-4 w-4" />
                    </div>
                    <div className="min-w-0">
                      <p className="text-sm font-black text-slate-900">{permission.permissionName}</p>
                      <p className="mt-1 break-all text-xs font-black text-primary">
                        {permission.permissionCode}
                      </p>
                      <p className="mt-2 text-sm font-medium leading-relaxed text-slate-500">
                        {permission.description || '当前权限点没有附带描述。'}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        ))}
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
