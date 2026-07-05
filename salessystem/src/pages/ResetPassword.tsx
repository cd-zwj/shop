import { useEffect, useRef, useState, type FormEvent } from 'react';
import { motion } from 'motion/react';
import {
  ArrowRight,
  CheckCircle2,
  ChevronLeft,
  Lock,
  Mail,
  RefreshCcw,
  ShieldCheck,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cn } from '../lib/utils';
import { appAuthService } from '../services/modules/appAuth';
import { ApiError } from '../types/api';

const EMAIL_CODE_COOLDOWN_SECONDS = 60;

export default function ResetPassword() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [emailCode, setEmailCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [captchaCode, setCaptchaCode] = useState('');
  const [captchaKey, setCaptchaKey] = useState('');
  const [captchaImage, setCaptchaImage] = useState('');
  const [cooldown, setCooldown] = useState(0);
  const [isCaptchaLoading, setIsCaptchaLoading] = useState(false);
  const [isSendingCode, setIsSendingCode] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const redirectTimerRef = useRef<number>(null);

  useEffect(() => {
    void loadCaptcha();
    return () => {
      if (redirectTimerRef.current) {
        clearTimeout(redirectTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = window.setInterval(() => {
      setCooldown((value) => (value <= 1 ? 0 : value - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  async function loadCaptcha() {
    setIsCaptchaLoading(true);
    try {
      const data = await appAuthService.getCaptcha();
      setCaptchaKey(data.captchaKey);
      setCaptchaImage(data.captchaImage);
      setCaptchaCode('');
    } catch {
      setError('验证码加载失败，请稍后重试');
    } finally {
      setIsCaptchaLoading(false);
    }
  }

  async function handleSendCode() {
    const normalizedEmail = email.trim();

    if (!isValidEmail(normalizedEmail)) {
      setError('请输入正确的邮箱地址');
      return;
    }
    if (!captchaCode.trim()) {
      setError('请输入图形验证码');
      return;
    }
    if (!captchaKey) {
      setError('验证码未准备好，请刷新后重试');
      return;
    }

    setIsSendingCode(true);
    setError('');
    setSuccess('');

    try {
      await appAuthService.sendPasswordResetCode({
        email: normalizedEmail,
        captchaKey,
        captchaCode: captchaCode.trim(),
      });
      setCooldown(EMAIL_CODE_COOLDOWN_SECONDS);
      setSuccess('邮箱验证码已发送');
      void loadCaptcha();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '邮箱验证码发送失败，请稍后重试');
      void loadCaptcha();
    } finally {
      setIsSendingCode(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedEmail = email.trim();

    if (!isValidEmail(normalizedEmail)) {
      setError('请输入正确的邮箱地址');
      return;
    }
    if (!emailCode.trim()) {
      setError('请输入邮箱验证码');
      return;
    }
    if (newPassword.length < 6 || newPassword.length > 64) {
      setError('新密码长度需在6-64位之间');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('两次输入的新密码不一致');
      return;
    }

    setIsSubmitting(true);
    setError('');
    setSuccess('');

    try {
      await appAuthService.resetPassword({
        email: normalizedEmail,
        emailCode: emailCode.trim(),
        newPassword,
      });
      setSuccess('密码已重置，即将返回登录页');
      redirectTimerRef.current = window.setTimeout(() => {
        navigate('/login');
      }, 900);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '密码重置失败，请稍后重试');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-white p-6">
      <div className="absolute left-0 top-0 h-1 w-full bg-primary/10" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative z-10 flex w-full max-w-md flex-col gap-8"
      >
        <button
          type="button"
          onClick={() => navigate('/login')}
          className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-slate-400 transition-colors hover:text-slate-900"
        >
          <ChevronLeft size={14} /> 返回登录
        </button>

        <header className="flex flex-col gap-2">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <ShieldCheck size={24} />
          </div>
          <h1 className="text-4xl font-black tracking-tighter text-slate-900">重置密码</h1>
          <p className="font-medium text-slate-500">通过已绑定邮箱完成身份校验并设置新密码。</p>
        </header>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <Field
            icon={Mail}
            type="email"
            placeholder="绑定邮箱 / Email"
            value={email}
            onChange={setEmail}
          />

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_148px]">
            <input
              type="text"
              value={captchaCode}
              onChange={(event) => setCaptchaCode(event.target.value.toUpperCase())}
              placeholder="图形验证码"
              className="w-full rounded-2xl border-2 border-slate-50 bg-slate-50 px-5 py-4 font-black uppercase tracking-[0.2em] text-slate-900 outline-none transition-all placeholder:tracking-normal placeholder:text-slate-400 focus:border-primary focus:bg-white"
            />
            <button
              type="button"
              onClick={() => void loadCaptcha()}
              disabled={isCaptchaLoading}
              className="flex h-[58px] items-center justify-center gap-2 rounded-2xl border-2 border-slate-50 bg-slate-50 px-3 transition-all hover:border-primary hover:bg-white disabled:cursor-not-allowed disabled:opacity-70"
            >
              {captchaImage ? (
                <img src={captchaImage} alt="重置密码验证码" className="h-10 w-full rounded-lg object-cover" />
              ) : (
                <span className="text-xs font-bold text-slate-400">加载中...</span>
              )}
              <RefreshCcw className={cn('h-4 w-4 text-slate-400', isCaptchaLoading && 'animate-spin')} />
            </button>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_148px]">
            <Field
              icon={CheckCircle2}
              type="text"
              placeholder="邮箱验证码"
              value={emailCode}
              onChange={setEmailCode}
            />
            <button
              type="button"
              onClick={() => void handleSendCode()}
              disabled={isSendingCode || cooldown > 0}
              className="flex h-[58px] items-center justify-center rounded-2xl border-2 border-slate-50 bg-slate-50 px-4 text-sm font-black transition-all hover:border-primary hover:bg-white disabled:cursor-not-allowed disabled:opacity-70"
            >
              {isSendingCode ? '发送中...' : cooldown > 0 ? `${cooldown}s` : '获取验证码'}
            </button>
          </div>

          <Field
            icon={Lock}
            type="password"
            placeholder="新密码 / New password"
            value={newPassword}
            onChange={setNewPassword}
          />
          <Field
            icon={Lock}
            type="password"
            placeholder="确认新密码 / Confirm password"
            value={confirmPassword}
            onChange={setConfirmPassword}
          />

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

          <button
            type="submit"
            disabled={isSubmitting}
            className="mt-4 flex w-full items-center justify-center gap-3 rounded-[24px] bg-primary py-5 text-lg font-black text-white shadow-2xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-70"
          >
            {isSubmitting ? '提交中...' : '确认重置'} <ArrowRight size={20} />
          </button>
        </form>
      </motion.div>
    </div>
  );
}

function isValidEmail(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function Field({
  icon: Icon,
  type,
  placeholder,
  value,
  onChange,
}: {
  icon: typeof Mail;
  type: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="group relative">
      <Icon className="absolute left-4 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-slate-300 transition-colors group-focus-within:text-primary" />
      <input
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="w-full rounded-2xl border-2 border-slate-50 bg-slate-50 py-4 pl-12 pr-6 font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white"
      />
    </div>
  );
}
