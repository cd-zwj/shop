import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, Lock, Save, ShieldCheck, Trash2 } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { adminUserService } from '../services/modules/adminUser';
import { ApiError } from '../types/api';
import type {
  AdminPermission,
  AdminPermissionCatalog,
  AdminPlatformUser,
  AdminUserPermissionDetail,
} from '../types/admin';

export default function AdminUserPermissionsPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const userId = Number(id);

  const [user, setUser] = useState<AdminPlatformUser | null>(null);
  const [permissionDetail, setPermissionDetail] = useState<AdminUserPermissionDetail | null>(null);
  const [catalog, setCatalog] = useState<AdminPermissionCatalog>({});
  const [draftExtraCodes, setDraftExtraCodes] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [removingCode, setRemovingCode] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const permissionCodeMap = useMemo(() => {
    const map = new Map<string, AdminPermission>();
    (Object.values(catalog) as AdminPermission[][]).forEach((permissions) => {
      permissions.forEach((permission) => {
        map.set(permission.permissionCode, permission);
      });
    });
    return map;
  }, [catalog]);

  const modules = useMemo(() => Object.entries(catalog) as Array<[string, AdminPermission[]]>, [catalog]);

  async function loadPage(targetUserId: number) {
    const [nextUser, nextPermissionDetail, nextCatalog] = await Promise.all([
      adminUserService.getUserDetail(targetUserId),
      adminUserService.getUserPermissions(targetUserId),
      adminUserService.listPermissions(),
    ]);

    setUser(nextUser);
    setPermissionDetail(nextPermissionDetail);
    setCatalog(nextCatalog);
    setDraftExtraCodes(nextPermissionDetail.extraPermissions || []);
  }

  useEffect(() => {
    let isMounted = true;

    async function run() {
      if (!Number.isFinite(userId)) {
        setError('用户编号无效');
        setIsLoading(false);
        return;
      }

      try {
        await loadPage(userId);
        if (!isMounted) return;
        setError('');
      } catch {
        if (!isMounted) return;
        setError('用户权限数据加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void run();

    return () => {
      isMounted = false;
    };
  }, [userId]);

  function togglePermission(permissionCode: string) {
    if (permissionDetail?.rolePermissions.includes(permissionCode)) {
      return;
    }

    setDraftExtraCodes((prev) =>
      prev.includes(permissionCode)
        ? prev.filter((code) => code !== permissionCode)
        : [...prev, permissionCode],
    );
  }

  async function handleSave() {
    setIsSaving(true);
    setError('');
    setSuccess('');

    try {
      const permissionIds = draftExtraCodes
        .map((code) => permissionCodeMap.get(code)?.id)
        .filter((value): value is number => Number.isFinite(value));

      await adminUserService.setUserPermissions(userId, permissionIds);
      await loadPage(userId);
      setSuccess('用户额外权限已保存');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '权限保存失败，请稍后重试');
    } finally {
      setIsSaving(false);
    }
  }

  async function handleRemoveExtraPermission(permissionCode: string) {
    const permission = permissionCodeMap.get(permissionCode);
    if (!permission) {
      setError(`未找到权限 ${permissionCode} 的编号，无法删除`);
      return;
    }

    setRemovingCode(permissionCode);
    setError('');
    setSuccess('');

    try {
      await adminUserService.removeUserPermission(userId, permission.id);
      await loadPage(userId);
      setSuccess(`已移除额外权限：${permission.permissionName}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '移除额外权限失败，请稍后重试');
    } finally {
      setRemovingCode(null);
    }
  }

  const extraPermissionCards = useMemo(
    () =>
      draftExtraCodes
        .map((code) => {
          const permission = permissionCodeMap.get(code);
          return permission
            ? {
                code,
                permission,
                persisted: permissionDetail?.extraPermissions.includes(code) ?? false,
              }
            : null;
        })
        .filter((item): item is { code: string; permission: AdminPermission; persisted: boolean } => Boolean(item)),
    [draftExtraCodes, permissionCodeMap, permissionDetail],
  );

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 p-4 pb-32 md:p-8">
      <header className="flex flex-col justify-between gap-6 sm:flex-row sm:items-end">
        <div>
          <button
            onClick={() => navigate(-1)}
            className="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-widest text-primary transition-all hover:gap-3"
          >
            <ArrowLeft className="h-4 w-4" /> 返回用户详情
          </button>
          <h1 className="text-4xl font-black tracking-tight text-slate-900">
            {user?.username || permissionDetail?.username || '用户权限配置'}
          </h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            角色继承权限保持锁定展示，额外权限支持整组保存，也支持按条直接删除。
          </p>
        </div>
        <button
          onClick={() => void handleSave()}
          disabled={isLoading || isSaving}
          className="flex items-center gap-2 rounded-[24px] bg-primary px-8 py-4 text-base font-black text-white shadow-2xl shadow-primary/20 transition-all hover:scale-105 disabled:cursor-not-allowed disabled:opacity-70"
        >
          <Save className="h-5 w-5" /> {isSaving ? '保存中...' : '保存权限'}
        </button>
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

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard label="角色继承权限" value={String(permissionDetail?.rolePermissions.length ?? 0)} />
        <SummaryCard label="额外授权权限" value={String(draftExtraCodes.length)} />
        <SummaryCard
          label="全部生效权限"
          value={String((permissionDetail?.rolePermissions.length ?? 0) + draftExtraCodes.length)}
        />
      </div>

      <section className="rounded-[40px] bg-slate-900 p-8 text-white shadow-xl">
        <div className="flex items-center gap-3">
          <ShieldCheck className="h-5 w-5 text-primary" />
          <span className="text-xs font-black uppercase tracking-widest">权限说明</span>
        </div>
        <p className="mt-4 text-sm font-medium leading-relaxed text-slate-300">
          带锁图标的权限来自角色继承，当前页不能取消。新增或批量调整额外权限时，保存按钮会调用
          `/v1/admin/users/{'{userId}'}/permissions`；对已存在的额外权限执行“移除”时，会直接调用
          `DELETE /v1/admin/users/{'{userId}'}/permissions/{'{permissionId}'}`。
        </p>
      </section>

      <section className="rounded-[32px] border border-slate-100 bg-white p-6 shadow-sm">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-black tracking-tight text-slate-900">已授予额外权限</h2>
            <p className="mt-1 text-sm font-medium text-slate-500">
              这里的“移除”会直接走删除接口，适合单条精确回收授权。
            </p>
          </div>
          <span className="rounded-xl bg-slate-100 px-3 py-2 text-xs font-black uppercase tracking-widest text-slate-500">
            {extraPermissionCards.length} items
          </span>
        </div>

        <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
          {extraPermissionCards.length === 0 && (
            <div className="rounded-[24px] border border-dashed border-slate-200 bg-slate-50 px-5 py-6 text-sm font-medium text-slate-500">
              当前没有额外授权的权限。
            </div>
          )}

          {extraPermissionCards.map(({ code, permission, persisted }) => (
            <div key={code} className="rounded-[24px] border border-slate-100 bg-slate-50 p-5">
              <p className="text-sm font-black text-slate-900">{permission.permissionName}</p>
              <p className="mt-1 break-all text-xs font-black text-primary">{permission.permissionCode}</p>
              <p className="mt-2 text-sm font-medium leading-relaxed text-slate-500">
                {permission.description || '当前权限点没有附带说明。'}
              </p>
              <div className="mt-4 flex items-center justify-between gap-3">
                <span className="text-xs font-black uppercase tracking-widest text-slate-400">
                  {persisted ? '已生效' : '待保存'}
                </span>
                {persisted ? (
                  <button
                    type="button"
                    onClick={() => void handleRemoveExtraPermission(code)}
                    disabled={removingCode === code}
                    className="inline-flex items-center gap-2 rounded-xl border border-red-200 px-3 py-2 text-xs font-black text-red-600 transition-all hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                    {removingCode === code ? '移除中...' : '移除'}
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => togglePermission(code)}
                    className="rounded-xl border border-slate-200 px-3 py-2 text-xs font-black text-slate-600 transition-all hover:bg-white"
                  >
                    取消待保存
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      </section>

      <div className="grid grid-cols-1 gap-6">
        {modules.map(([moduleName, permissions]) => (
          <section
            key={moduleName}
            className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm"
          >
            <div className="mb-6">
              <h2 className="text-xl font-black tracking-tight text-slate-900">
                {moduleName || '未分组模块'}
              </h2>
              <p className="mt-1 text-sm font-medium text-slate-500">按模块配置额外授权权限</p>
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              {permissions.map((permission) => {
                const inherited = permissionDetail?.rolePermissions.includes(permission.permissionCode) ?? false;
                const extraGranted = draftExtraCodes.includes(permission.permissionCode);
                const active = inherited || extraGranted;

                return (
                  <button
                    key={permission.id}
                    type="button"
                    onClick={() => togglePermission(permission.permissionCode)}
                    className={`rounded-[28px] border p-5 text-left transition-all ${
                      active
                        ? 'border-primary bg-primary/5'
                        : 'border-slate-100 bg-slate-50 hover:border-slate-200'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <p className="text-sm font-black text-slate-900">{permission.permissionName}</p>
                        <p className="mt-1 break-all text-xs font-black text-primary">
                          {permission.permissionCode}
                        </p>
                        <p className="mt-2 text-sm font-medium leading-relaxed text-slate-500">
                          {permission.description || '当前权限点没有附带说明。'}
                        </p>
                      </div>
                      {inherited && (
                        <span className="inline-flex items-center gap-1 rounded-xl bg-slate-900 px-2 py-1 text-[10px] font-black uppercase tracking-widest text-white">
                          <Lock className="h-3 w-3" /> 继承
                        </span>
                      )}
                    </div>
                    <div className="mt-4 text-xs font-black uppercase tracking-widest text-slate-500">
                      {inherited ? '角色继承' : extraGranted ? '额外授予' : '未授予'}
                    </div>
                  </button>
                );
              })}
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
