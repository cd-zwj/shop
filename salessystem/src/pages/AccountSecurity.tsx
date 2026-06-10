import { type FormEvent, useCallback, useEffect, useState } from 'react';
import {
  ArrowLeft,
  CheckCircle2,
  Eye,
  EyeOff,
  Mail,
  Phone,
  ShieldCheck,
  XCircle,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useToast } from '../context/ToastContext';
import { useAuth } from '../context/AuthContext';
import {
  appAccountSecurityService,
  type AccountSecurityVO,
} from '../services/modules/appAccountSecurity';
import { cn } from '../lib/utils';

/* ------------------------------------------------------------------ */
/*  小工具                                                              */
/* ------------------------------------------------------------------ */

function maskFallback(value: string | null | undefined): string {
  if (!value) return '未绑定';
  if (value.length <= 3) return '***';
  return value.slice(0, 3) + '***' + value.slice(-2);
}

function Badge({ ok, label }: { ok: boolean; label: string }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider',
        ok
          ? 'bg-emerald-50 text-emerald-600'
          : 'bg-amber-50 text-amber-600',
      )}
    >
      {ok ? <CheckCircle2 className="h-3 w-3" /> : <XCircle className="h-3 w-3" />}
      {label}
    </span>
  );
}

/* ------------------------------------------------------------------ */
/*  修改密码表单                                                         */
/* ------------------------------------------------------------------ */

function ChangePasswordCard() {
  const { showToast } = useToast();
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showOld, setShowOld] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const canSubmit =
    oldPassword.length > 0 &&
    newPassword.length >= 6 &&
    newPassword === confirmPassword &&
    !submitting;

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();

    if (newPassword.length < 6) {
      showToast('新密码长度不能少于 6 位', 'error');
      return;
    }
    if (newPassword !== confirmPassword) {
      showToast('两次输入的新密码不一致', 'error');
      return;
    }
    if (newPassword === oldPassword) {
      showToast('新密码不能与旧密码相同', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await appAccountSecurityService.changePassword({
        oldPassword,
        newPassword,
      });
      showToast('密码修改成功，请使用新密码重新登录', 'success');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : String(err);
      showToast(message || '修改密码失败', 'error');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      {/* 旧密码 */}
      <div className="flex flex-col gap-1.5">
        <label className="text-xs font-bold text-slate-500">当前密码</label>
        <div className="relative">
          <input
            type={showOld ? 'text' : 'password'}
            value={oldPassword}
            onChange={(e) => setOldPassword(e.target.value)}
            placeholder="请输入当前密码"
            className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 pr-12 text-sm text-slate-800 outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20"
          />
          <button
            type="button"
            onClick={() => setShowOld(!showOld)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
          >
            {showOld ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>
      </div>

      {/* 新密码 */}
      <div className="flex flex-col gap-1.5">
        <label className="text-xs font-bold text-slate-500">新密码</label>
        <div className="relative">
          <input
            type={showNew ? 'text' : 'password'}
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="至少 6 位"
            className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 pr-12 text-sm text-slate-800 outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20"
          />
          <button
            type="button"
            onClick={() => setShowNew(!showNew)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
          >
            {showNew ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>
        {newPassword.length > 0 && newPassword.length < 6 && (
          <p className="text-[11px] font-medium text-red-500">密码长度至少 6 位</p>
        )}
      </div>

      {/* 确认新密码 */}
      <div className="flex flex-col gap-1.5">
        <label className="text-xs font-bold text-slate-500">确认新密码</label>
        <input
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          placeholder="再次输入新密码"
          className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-800 outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20"
        />
        {confirmPassword.length > 0 && confirmPassword !== newPassword && (
          <p className="text-[11px] font-medium text-red-500">两次密码不一致</p>
        )}
      </div>

      <button
        type="submit"
        disabled={!canSubmit}
        className={cn(
          'mt-2 w-full rounded-2xl py-3 text-sm font-bold uppercase tracking-wider transition-all',
          canSubmit
            ? 'bg-primary text-white hover:bg-primary/90 active:scale-[0.98]'
            : 'cursor-not-allowed bg-slate-100 text-slate-400',
        )}
      >
        {submitting ? '提交中...' : '确认修改'}
      </button>
    </form>
  );
}

/* ------------------------------------------------------------------ */
/*  绑定信息卡片                                                         */
/* ------------------------------------------------------------------ */

function BindingInfoCard({ security }: { security: AccountSecurityVO | null }) {
  const { currentUser } = useAuth();

  const phoneBound = security?.phone?.bound ?? !!currentUser?.phone;
  const phoneDisplay =
    security?.phone?.bound && security.phone.maskedValue
      ? security.phone.maskedValue
      : maskFallback(currentUser?.phone ?? null);

  const emailBound = security?.email?.bound ?? !!currentUser?.email;
  const emailDisplay =
    security?.email?.bound && security.email.maskedValue
      ? security.email.maskedValue
      : maskFallback(currentUser?.email ?? null);

  return (
    <div className="flex flex-col gap-4">
      {/* 手机号 */}
      <div className="flex items-center gap-4 rounded-2xl border border-slate-100 bg-slate-50/50 p-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-blue-50 text-blue-500">
          <Phone className="h-5 w-5" />
        </div>
        <div className="flex flex-1 flex-col gap-0.5">
          <span className="text-xs font-bold text-slate-400">手机号</span>
          <span className="text-sm font-bold text-slate-800">{phoneDisplay}</span>
        </div>
        <Badge ok={phoneBound} label={phoneBound ? '已绑定' : '未绑定'} />
      </div>

      {/* 邮箱 */}
      <div className="flex items-center gap-4 rounded-2xl border border-slate-100 bg-slate-50/50 p-4">
        <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-violet-50 text-violet-500">
          <Mail className="h-5 w-5" />
        </div>
        <div className="flex flex-1 flex-col gap-0.5">
          <span className="text-xs font-bold text-slate-400">邮箱</span>
          <span className="text-sm font-bold text-slate-800">{emailDisplay}</span>
        </div>
        <Badge ok={emailBound} label={emailBound ? '已绑定' : '未绑定'} />
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  主页面                                                              */
/* ------------------------------------------------------------------ */

export default function AccountSecurity() {
  const navigate = useNavigate();
  const [security, setSecurity] = useState<AccountSecurityVO | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchSecurity = useCallback(async () => {
    try {
      const data = await appAccountSecurityService.getSecuritySummary();
      setSecurity(data);
    } catch {
      setSecurity(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSecurity();
  }, [fetchSecurity]);

  return (
    <div className="flex flex-col gap-6 pb-20">
      {/* 页头 */}
      <header className="sticky top-0 z-40 flex items-center gap-3 border-b border-slate-200 bg-white px-4 py-4">
        <button
          onClick={() => navigate(-1)}
          className="flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-slate-600 hover:bg-slate-200"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <div className="flex items-center gap-2">
          <ShieldCheck className="h-5 w-5 text-primary" />
          <h1 className="text-lg font-black tracking-tight text-slate-900">账号安全</h1>
        </div>
      </header>

      <div className="flex flex-col gap-8 px-4">
        {/* 安全概览卡片 */}
        <section>
          <h2 className="mb-3 ml-1 text-[11px] font-black uppercase tracking-[0.2em] text-slate-400">
            安全概览
          </h2>
          <div className="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm">
            {loading ? (
              <div className="flex items-center justify-center py-6">
                <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
              </div>
            ) : (
              <BindingInfoCard security={security} />
            )}
          </div>
        </section>

        {/* 修改密码卡片 */}
        <section>
          <h2 className="mb-3 ml-1 text-[11px] font-black uppercase tracking-[0.2em] text-slate-400">
            修改密码
          </h2>
          <div className="rounded-3xl border border-slate-100 bg-white p-5 shadow-sm">
            <ChangePasswordCard />
          </div>
        </section>

        {/* 安全提示 */}
        <section>
          <div className="rounded-2xl border border-blue-100 bg-blue-50/40 px-5 py-4">
            <p className="text-[11px] font-medium leading-relaxed text-blue-600">
              安全提示：密码修改成功后，已登录的其他设备将被自动退出。请使用新密码重新登录。切勿向任何人透露您的密码。
            </p>
          </div>
        </section>
      </div>
    </div>
  );
}
