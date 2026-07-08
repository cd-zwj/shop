import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  RefreshCw,
  ShieldCheck,
  ToggleLeft,
  ToggleRight,
  UserPlus,
  Users,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { cn } from '../../lib/utils';
import { merchantEmployeeService } from '../../services/modules/merchantEmployee';
import type { MerchantEmployee, MerchantEmployeeRole } from '../../types/merchant';
import { getErrorMessage } from '../../utils/errorMessage';

const ROLE_OPTIONS: Array<{ role: MerchantEmployeeRole; label: string; description: string }> = [
  { role: 'OWNER', label: '店主', description: '拥有全部商户权限，保护最后一个启用店主。' },
  { role: 'ADMIN', label: '管理员', description: '拥有全部商户权限，可协助管理后台。' },
  { role: 'MANAGER', label: '店长', description: '管理门店、商品、订单、售后、营销和规则。' },
  { role: 'OPERATOR', label: '运营', description: '管理商品、订单、售后和营销，不查看财务。' },
  { role: 'CASHIER', label: '收银', description: '查看工作台和订单，适合门店收银处理。' },
  { role: 'FINANCE', label: '财务', description: '查看财务与提现，不操作商品和营销。' },
];

const ROLE_LABEL = Object.fromEntries(ROLE_OPTIONS.map((item) => [item.role, item.label]));

export default function MerchantEmployees() {
  const { merchantSession } = useAuth();
  const { showToast } = useToast();
  const tenantId = merchantSession?.tenantId;
  const [employees, setEmployees] = useState<MerchantEmployee[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [platformUserIdDraft, setPlatformUserIdDraft] = useState('');
  const [roleDraft, setRoleDraft] = useState<MerchantEmployeeRole>('OPERATOR');
  const [editingEmployee, setEditingEmployee] = useState<MerchantEmployee | null>(null);
  const [editingRole, setEditingRole] = useState<MerchantEmployeeRole>('OPERATOR');

  const loadEmployees = useCallback(async () => {
    if (!tenantId) {
      setError('当前商户会话缺少 tenantId，请重新登录');
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError('');
    try {
      const result = await merchantEmployeeService.listEmployees(tenantId);
      setEmployees(result || []);
    } catch (err) {
      setEmployees([]);
      setError(getErrorMessage(err, '员工列表加载失败，请稍后重试'));
    } finally {
      setIsLoading(false);
    }
  }, [tenantId]);

  useEffect(() => {
    void loadEmployees();
  }, [loadEmployees]);

  const activeCount = useMemo(() => employees.filter((item) => item.status === 1).length, [employees]);
  const ownerAdminCount = useMemo(
    () => employees.filter((item) => item.status === 1 && ['OWNER', 'ADMIN'].includes(item.employeeRole)).length,
    [employees],
  );
  const ownerCount = useMemo(
    () => employees.filter((item) => item.status === 1 && item.employeeRole === 'OWNER').length,
    [employees],
  );

  async function handleCreateEmployee(event: FormEvent) {
    event.preventDefault();
    if (!tenantId) return;
    const platformUserId = Number(platformUserIdDraft);
    if (!Number.isInteger(platformUserId) || platformUserId <= 0) {
      showToast('平台用户ID必须是大于0的整数', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      await merchantEmployeeService.addEmployee(tenantId, platformUserId, roleDraft);
      showToast('员工已添加或重新启用', 'success');
      setPlatformUserIdDraft('');
      setRoleDraft('OPERATOR');
      setIsCreateOpen(false);
      await loadEmployees();
    } catch (err) {
      showToast(getErrorMessage(err, '添加员工失败，请稍后重试'), 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleUpdateRole(event: FormEvent) {
    event.preventDefault();
    if (!tenantId || !editingEmployee) return;

    setIsSubmitting(true);
    try {
      await merchantEmployeeService.updateRole(tenantId, editingEmployee.id, editingRole);
      showToast('员工角色已更新', 'success');
      setEditingEmployee(null);
      await loadEmployees();
    } catch (err) {
      showToast(getErrorMessage(err, '角色更新失败，请稍后重试'), 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleToggleStatus(employee: MerchantEmployee) {
    if (!tenantId) return;
    const nextStatus = employee.status === 1 ? 0 : 1;
    setIsSubmitting(true);
    try {
      await merchantEmployeeService.updateStatus(tenantId, employee.id, nextStatus);
      showToast(nextStatus === 1 ? '员工已启用' : '员工已禁用', 'success');
      await loadEmployees();
    } catch (err) {
      showToast(getErrorMessage(err, '员工状态更新失败，请稍后重试'), 'error');
    } finally {
      setIsSubmitting(false);
    }
  }

  function openRoleEditor(employee: MerchantEmployee) {
    setEditingEmployee(employee);
    setEditingRole(normalizeRole(employee.employeeRole));
  }

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 p-4 pb-24 md:p-8">
      <header className="flex flex-col justify-between gap-6 md:flex-row md:items-end">
        <div>
          <h1 className="text-3xl font-black tracking-tight text-slate-900">员工与权限</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            当前商户：{merchantSession?.tenantName || '未获取商户会话'}，本页只开放给 OWNER / ADMIN。
          </p>
        </div>
        <button
          type="button"
          onClick={() => setIsCreateOpen(true)}
          className="inline-flex items-center justify-center gap-2 rounded-2xl bg-primary px-5 py-3 text-sm font-black text-white shadow-xl shadow-primary/20"
        >
          <UserPlus className="h-4 w-4" />
          添加员工
        </button>
      </header>

      {error && (
        <div role="alert" className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          <div className="flex items-center justify-between gap-4">
            <span>{error}</span>
            <button
              type="button"
              onClick={() => void loadEmployees()}
              className="inline-flex shrink-0 items-center gap-1.5 font-black text-red-700"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              重试
            </button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <SummaryCard icon={Users} label="员工总数" value={employees.length} hint="包含启用和禁用员工" />
        <SummaryCard icon={CheckCircle2} label="启用员工" value={activeCount} hint="可登录商户后台" />
        <SummaryCard icon={ShieldCheck} label="管理角色" value={ownerAdminCount} hint={`启用 OWNER ${ownerCount} 个`} />
      </div>

      <section className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <div className="overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-sm lg:col-span-8">
          <div className="border-b border-slate-50 px-6 py-5">
            <h2 className="text-sm font-black uppercase tracking-widest text-slate-900">员工列表</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50/70">
                <tr>
                  <th className="px-6 py-4 text-[10px] font-black uppercase tracking-widest text-slate-400">员工</th>
                  <th className="px-6 py-4 text-[10px] font-black uppercase tracking-widest text-slate-400">角色</th>
                  <th className="px-6 py-4 text-[10px] font-black uppercase tracking-widest text-slate-400">状态</th>
                  <th className="px-6 py-4 text-[10px] font-black uppercase tracking-widest text-slate-400">加入时间</th>
                  <th className="px-6 py-4"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {isLoading ? (
                  Array.from({ length: 4 }).map((_, index) => (
                    <tr key={index}>
                      <td className="px-6 py-5 text-slate-400">加载中...</td>
                      <td className="px-6 py-5 text-slate-400">--</td>
                      <td className="px-6 py-5 text-slate-400">--</td>
                      <td className="px-6 py-5 text-slate-400">--</td>
                      <td className="px-6 py-5"></td>
                    </tr>
                  ))
                ) : employees.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-6 py-12 text-center text-sm font-bold text-slate-400">
                      暂无员工数据。
                    </td>
                  </tr>
                ) : (
                  employees.map((employee) => (
                    <tr key={employee.id} className="hover:bg-slate-50/50">
                      <td className="px-6 py-5">
                        <div className="flex flex-col">
                          <span className="font-black text-slate-900">
                            {employee.username || `平台用户 ${employee.platformUserId}`}
                          </span>
                          <span className="mt-1 font-mono text-[11px] font-bold text-slate-400">
                            #{employee.platformUserId} · {employee.employeeNo || '--'}
                          </span>
                          {(employee.phone || employee.email) && (
                            <span className="mt-1 text-xs font-medium text-slate-400">
                              {employee.phone || employee.email}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-5">
                        <button
                          type="button"
                          onClick={() => openRoleEditor(employee)}
                          className="rounded-xl border border-slate-100 bg-slate-50 px-3 py-1.5 text-xs font-black text-slate-700 transition-colors hover:border-primary/20 hover:text-primary"
                        >
                          {ROLE_LABEL[employee.employeeRole] || employee.employeeRole}
                        </button>
                      </td>
                      <td className="px-6 py-5">
                        <span
                          className={cn(
                            'rounded-xl px-3 py-1.5 text-[10px] font-black uppercase tracking-widest',
                            employee.status === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500',
                          )}
                        >
                          {employee.status === 1 ? '启用' : '禁用'}
                        </span>
                      </td>
                      <td className="px-6 py-5 text-xs font-bold text-slate-400">
                        {formatDateTime(employee.createTime)}
                      </td>
                      <td className="px-6 py-5 text-right">
                        <button
                          type="button"
                          disabled={isSubmitting}
                          onClick={() => void handleToggleStatus(employee)}
                          className={cn(
                            'inline-flex items-center gap-1.5 rounded-xl px-3 py-2 text-xs font-black transition-colors disabled:cursor-not-allowed disabled:opacity-50',
                            employee.status === 1
                              ? 'bg-red-50 text-red-600 hover:bg-red-100'
                              : 'bg-emerald-50 text-emerald-600 hover:bg-emerald-100',
                          )}
                        >
                          {employee.status === 1 ? <ToggleLeft className="h-4 w-4" /> : <ToggleRight className="h-4 w-4" />}
                          {employee.status === 1 ? '禁用' : '启用'}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        <aside className="flex flex-col gap-4 lg:col-span-4">
          <div className="rounded-[32px] border border-amber-100 bg-amber-50 p-6 text-amber-800">
            <div className="mb-3 flex items-center gap-2">
              <AlertTriangle className="h-5 w-5" />
              <span className="text-sm font-black">安全约束</span>
            </div>
            <p className="text-xs font-bold leading-relaxed">
              后端会阻止禁用最后一个启用 OWNER，也会阻止当前账号把自己降权或禁用到失去员工管理入口。
            </p>
          </div>

          <div className="rounded-[32px] border border-slate-100 bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-sm font-black uppercase tracking-widest text-slate-900">角色权限说明</h2>
            <div className="flex flex-col gap-3">
              {ROLE_OPTIONS.map((item) => (
                <div key={item.role} className="rounded-2xl bg-slate-50 px-4 py-3">
                  <div className="text-xs font-black text-slate-900">
                    {item.label} <span className="font-mono text-slate-400">{item.role}</span>
                  </div>
                  <p className="mt-1 text-xs font-medium leading-relaxed text-slate-500">{item.description}</p>
                </div>
              ))}
            </div>
          </div>
        </aside>
      </section>

      {isCreateOpen && (
        <EmployeeDialog
          title="添加员工"
          submitLabel={isSubmitting ? '提交中...' : '确认添加'}
          platformUserIdDraft={platformUserIdDraft}
          roleDraft={roleDraft}
          showPlatformUserInput
          isSubmitting={isSubmitting}
          onPlatformUserIdChange={setPlatformUserIdDraft}
          onRoleChange={setRoleDraft}
          onCancel={() => setIsCreateOpen(false)}
          onSubmit={handleCreateEmployee}
        />
      )}

      {editingEmployee && (
        <EmployeeDialog
          title="调整员工角色"
          submitLabel={isSubmitting ? '保存中...' : '保存角色'}
          platformUserIdDraft={String(editingEmployee.platformUserId)}
          roleDraft={editingRole}
          showPlatformUserInput={false}
          isSubmitting={isSubmitting}
          onPlatformUserIdChange={setPlatformUserIdDraft}
          onRoleChange={setEditingRole}
          onCancel={() => setEditingEmployee(null)}
          onSubmit={handleUpdateRole}
        />
      )}
    </div>
  );
}

function SummaryCard({
  icon: Icon,
  label,
  value,
  hint,
}: {
  icon: typeof Users;
  label: string;
  value: number;
  hint: string;
}) {
  return (
    <div className="rounded-[28px] border border-slate-100 bg-white p-6 shadow-sm">
      <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-2xl bg-primary/5 text-primary">
        <Icon className="h-5 w-5" />
      </div>
      <p className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</p>
      <p className="mt-2 text-3xl font-black tracking-tight text-slate-900">{value}</p>
      <p className="mt-1 text-xs font-medium text-slate-500">{hint}</p>
    </div>
  );
}

function EmployeeDialog({
  title,
  submitLabel,
  platformUserIdDraft,
  roleDraft,
  showPlatformUserInput,
  isSubmitting,
  onPlatformUserIdChange,
  onRoleChange,
  onCancel,
  onSubmit,
}: {
  title: string;
  submitLabel: string;
  platformUserIdDraft: string;
  roleDraft: MerchantEmployeeRole;
  showPlatformUserInput: boolean;
  isSubmitting: boolean;
  onPlatformUserIdChange: (value: string) => void;
  onRoleChange: (value: MerchantEmployeeRole) => void;
  onCancel: () => void;
  onSubmit: (event: FormEvent) => void;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4 backdrop-blur-sm">
      <form onSubmit={onSubmit} className="w-full max-w-md rounded-[32px] border border-slate-100 bg-white p-6 shadow-2xl">
        <h3 className="text-lg font-black text-slate-900">{title}</h3>
        <div className="mt-6 flex flex-col gap-4">
          {showPlatformUserInput && (
            <div>
              <label htmlFor="platformUserId" className="mb-2 block text-[10px] font-black uppercase tracking-widest text-slate-400">
                平台用户ID
              </label>
              <input
                id="platformUserId"
                type="number"
                min={1}
                value={platformUserIdDraft}
                onChange={(event) => onPlatformUserIdChange(event.target.value)}
                className="w-full rounded-2xl border-2 border-slate-100 bg-slate-50 px-4 py-3 text-sm font-black text-slate-900 outline-none focus:border-primary focus:bg-white"
                required
              />
            </div>
          )}

          <div>
            <label htmlFor="employeeRole" className="mb-2 block text-[10px] font-black uppercase tracking-widest text-slate-400">
              员工角色
            </label>
            <select
              id="employeeRole"
              value={roleDraft}
              onChange={(event) => onRoleChange(event.target.value as MerchantEmployeeRole)}
              className="w-full rounded-2xl border-2 border-slate-100 bg-slate-50 px-4 py-3 text-sm font-black text-slate-900 outline-none focus:border-primary focus:bg-white"
            >
              {ROLE_OPTIONS.map((item) => (
                <option key={item.role} value={item.role}>
                  {item.label} / {item.role}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="mt-6 rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-xs font-bold leading-relaxed text-amber-700">
          OWNER 和 ADMIN 可以管理员工；FINANCE 只能看财务与提现；普通运营角色不能访问员工管理。
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-xl border border-slate-200 px-5 py-3 text-sm font-black text-slate-500 hover:bg-slate-50"
          >
            取消
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-xl bg-slate-900 px-5 py-3 text-sm font-black text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitLabel}
          </button>
        </div>
      </form>
    </div>
  );
}

function normalizeRole(role: string): MerchantEmployeeRole {
  return ROLE_OPTIONS.some((item) => item.role === role) ? role as MerchantEmployeeRole : 'OPERATOR';
}

function formatDateTime(value?: string | null) {
  if (!value) return '--';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-');
}
