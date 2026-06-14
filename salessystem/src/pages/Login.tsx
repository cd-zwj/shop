import { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import {
  ArrowRight,
  Bot,
  Github,
  LayoutDashboard,
  Lock,
  Mail,
  MessageSquare,
  RefreshCcw,
  Smartphone,
  Store,
  Zap,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { cn } from '../lib/utils';
import { appAuthService } from '../services/modules/appAuth';
import { ApiError } from '../types/api';
import type { AuthRole } from '../types/auth';

const SMS_COOLDOWN_SECONDS = 60;

export default function Login() {
  const navigate = useNavigate();
  const { loginAdmin, loginMerchant, loginUser } = useAuth();
 const { showToast } = useToast();
  // 从 URL 读取被 401 踢出的角色，预选对应登录标签
  function getInitialRole(): AuthRole {
    const params = new URLSearchParams(window.location.search);
    const roleParam = params.get('role');
    if (roleParam === 'merchant' || roleParam === 'admin') return roleParam;
    return 'user';
  }
  const [selectedRole, setSelectedRole] = useState<AuthRole>(getInitialRole);
 const [loginMethod, setLoginMethod] = useState<'password' | 'sms'>('password');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [smsCode, setSmsCode] = useState('');
  const [smsCooldown, setSmsCooldown] = useState(0);
  const [isSendingSms, setIsSendingSms] = useState(false);
  const [captchaCode, setCaptchaCode] = useState('');
  const [captchaKey, setCaptchaKey] = useState('');
  const [captchaImage, setCaptchaImage] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCaptchaLoading, setIsCaptchaLoading] = useState(false);

  const roles = [
    { id: 'user', title: '普通用户', desc: '浏览商品、充值钱包、查看消费', icon: Smartphone, color: 'bg-blue-50 text-blue-600' },
    { id: 'merchant', title: '商户中心', desc: '管理商品、订单与财务', icon: Store, color: 'bg-orange-50 text-orange-600' },
    { id: 'admin', title: '管理后台', desc: '治理平台商户、用户与数据', icon: LayoutDashboard, color: 'bg-slate-100 text-slate-600' },
  ] as const;

  useEffect(() => {
    if (selectedRole !== 'user') {
      setLoginMethod('password');
    }
    setError('');
  }, [selectedRole]);

  useEffect(() => {
    void loadCaptcha();
  }, []);

  // SMS cooldown countdown
  useEffect(() => {
    if (smsCooldown <= 0) return;
    const timer = setInterval(() => {
      setSmsCooldown((prev) => (prev <= 1 ? 0 : prev - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [smsCooldown]);

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

  async function handleSendSmsCode() {
    if (!phone.trim()) {
      setError('请输入手机号');
      return;
    }
    if (!/^1[3-9]\d{9}$/.test(phone.trim())) {
      setError('请输入正确的手机号');
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

    setIsSendingSms(true);
    setError('');

    try {
      await appAuthService.sendSmsCode({
        phone: phone.trim(),
        captchaKey,
        captchaCode: captchaCode.trim(),
      });
      setSmsCooldown(SMS_COOLDOWN_SECONDS);
      void loadCaptcha();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '验证码发送失败，请稍后重试');
      void loadCaptcha();
    } finally {
      setIsSendingSms(false);
    }
  }

  async function handleLogin() {
    if (selectedRole === 'user' && loginMethod === 'sms') {
      await handleSmsLogin();
      return;
    }

    if (!username.trim() || !password.trim() || !captchaCode.trim()) {
      setError('请输入完整的登录信息');
      return;
    }
    if (!captchaKey) {
      setError('验证码未准备好，请刷新后重试');
      return;
    }

    setIsSubmitting(true);
    setError('');

    try {
      const payload = {
        username: username.trim(),
        password: password.trim(),
        captchaKey,
        captchaCode: captchaCode.trim(),
      };

      if (selectedRole === 'user') {
        await loginUser(loginMethod, payload);
        navigate('/');
      } else if (selectedRole === 'merchant') {
        await loginMerchant(payload);
        navigate('/merchant');
      } else {
        await loginAdmin(payload);
        navigate('/admin');
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '登录失败，请稍后重试');
      void loadCaptcha();
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleSmsLogin() {
    if (!phone.trim()) {
      setError('请输入手机号');
      return;
    }
    if (!smsCode.trim()) {
      setError('请输入短信验证码');
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

    setIsSubmitting(true);
    setError('');

    try {
      await loginUser('sms', {
        phone: phone.trim(),
        smsCode: smsCode.trim(),
        captchaKey,
        captchaCode: captchaCode.trim(),
      });
      navigate('/');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '登录失败，请稍后重试');
      void loadCaptcha();
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleThirdPartyLogin() {
    if (!username.trim() || !password.trim() || !captchaCode.trim()) {
      setError('请先填写账号、凭证和图形验证码，再触发第三方登录');
      return;
    }
    if (!captchaKey) {
      setError('验证码未准备好，请刷新后重试');
      return;
    }

    setIsSubmitting(true);
    setError('');

    try {
      await loginUser('third-party', {
        username: username.trim(),
        password: password.trim(),
        captchaKey,
        captchaCode: captchaCode.trim(),
      });
      navigate('/');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '第三方登录失败，请稍后重试');
      void loadCaptcha();
    } finally {
      setIsSubmitting(false);
    }
  }

  const isSmsMode = selectedRole === 'user' && loginMethod === 'sms';

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-50 p-4">
      <div className="absolute right-0 top-0 h-[600px] w-[600px] -mr-40 -mt-40 rounded-full bg-primary/5 blur-[120px]" />
      <div className="absolute bottom-0 left-0 h-[500px] w-[500px] -mb-40 -ml-40 rounded-full bg-indigo-500/5 blur-[100px]" />

      <motion.div
        initial={{ opacity: 0, scale: 0.98 }}
        animate={{ opacity: 1, scale: 1 }}
        className="relative z-10 grid w-full max-w-5xl grid-cols-1 overflow-hidden rounded-[48px] border border-slate-100 bg-white shadow-2xl lg:grid-cols-2"
      >
        <div className="relative hidden flex-col justify-between overflow-hidden bg-slate-900 p-16 text-white lg:flex">
          <div className="absolute inset-0 opacity-10">
            <div className="grid scale-150 grid-cols-6 gap-8 -rotate-12">
              {Array.from({ length: 36 }).map((_, i) => (
                <div key={i} className="h-12 w-12 rounded-lg bg-white" />
              ))}
            </div>
          </div>

          <div className="relative z-10">
            <div className="mb-10 flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary shadow-lg shadow-primary/20">
                <Zap className="h-6 w-6 fill-current text-white" />
              </div>
              <span className="text-2xl font-black tracking-tight text-white">SalesSystem</span>
            </div>
            <h2 className="mb-8 text-5xl font-black leading-tight">
              多角色统一接入
              <br />
              电商支付系统
            </h2>
            <p className="max-w-sm text-lg font-medium leading-relaxed text-slate-400">
              当前登录页已经接入真实认证接口，登录成功后会保存真实 token 和对应会话。
            </p>
          </div>

          <div className="relative z-10 flex flex-col gap-6">
            <div className="flex items-center gap-1.5 text-xs font-black uppercase tracking-widest text-slate-500">
              <Bot className="h-4 w-4 text-primary" />
              <span>API Connected</span>
            </div>
          </div>
        </div>

        <div className="flex flex-col justify-center p-8 md:p-16">
          <header className="mb-12">
            <h1 className="mb-3 text-3xl font-black tracking-tight text-slate-900">登录你的账户</h1>
            <p className="font-medium text-slate-500">选择访问角色并填写真实后端登录信息。</p>
          </header>

          <div className="flex flex-col gap-8">
            {selectedRole === 'user' && (
              <div className="flex w-fit rounded-2xl bg-slate-100 p-1">
                <button
                  onClick={() => setLoginMethod('password')}
                  className={cn(
                    'rounded-xl px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all',
                    loginMethod === 'password' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-400',
                  )}
                >
                  密码登录
                </button>
                <button
                  onClick={() => setLoginMethod('sms')}
                  className={cn(
                    'rounded-xl px-4 py-2 text-[10px] font-black uppercase tracking-widest transition-all',
                    loginMethod === 'sms' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-400',
                  )}
                >
                  短信登录
                </button>
              </div>
            )}

            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              {roles.map((role) => {
                const isActive = selectedRole === role.id;
                return (
                  <button
                    key={role.id}
                    onClick={() => setSelectedRole(role.id)}
                    className={cn(
                      'group relative flex flex-col items-center gap-3 rounded-[24px] border-2 p-5 transition-all',
                      isActive
                        ? 'border-primary bg-white shadow-xl shadow-primary/5 ring-4 ring-primary/5'
                        : 'border-transparent bg-slate-50 hover:bg-slate-100',
                    )}
                  >
                    <div
                      className={cn(
                        'flex h-12 w-12 items-center justify-center rounded-2xl transition-transform group-hover:scale-110',
                        role.color,
                      )}
                    >
                      <role.icon size={24} />
                    </div>
                    <span className="text-sm font-black text-slate-900">{role.title}</span>
                    {isActive && <div className="absolute right-2 top-2 h-2 w-2 rounded-full bg-primary" />}
                  </button>
                );
              })}
            </div>

            <div className="flex flex-col gap-5">
              {isSmsMode ? (
                <>
                  {/* Phone number input */}
                  <div className="flex flex-col gap-2">
                    <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                      手机号
                    </label>
                    <div className="group relative">
                      <Smartphone className="absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-300 transition-colors group-focus-within:text-primary" />
                      <input
                        type="tel"
                        value={phone}
                        onChange={(event) => setPhone(event.target.value)}
                        placeholder="请输入手机号"
                        maxLength={11}
                        className="w-full rounded-[20px] border-2 border-slate-100 bg-slate-50/50 py-4 pl-14 pr-6 font-bold text-slate-900 outline-none transition-all placeholder:text-slate-400 focus:border-primary focus:bg-white"
                      />
                    </div>
                  </div>

                  {/* SMS code input with send button */}
                  <div className="flex flex-col gap-2">
                    <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                      短信验证码
                    </label>
                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_148px]">
                      <div className="group relative">
                        <MessageSquare className="absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-300 transition-colors group-focus-within:text-primary" />
                        <input
                          type="text"
                          value={smsCode}
                          onChange={(event) => setSmsCode(event.target.value)}
                          placeholder="请输入短信验证码"
                          maxLength={6}
                          className="w-full rounded-[20px] border-2 border-slate-100 bg-slate-50/50 py-4 pl-14 pr-6 font-bold text-slate-900 outline-none transition-all placeholder:text-slate-400 focus:border-primary focus:bg-white"
                        />
                      </div>
                      <button
                        type="button"
                        onClick={() => void handleSendSmsCode()}
                        disabled={isSendingSms || smsCooldown > 0}
                        className="flex h-[58px] items-center justify-center rounded-[20px] border-2 border-slate-100 bg-slate-50/70 px-4 text-sm font-black transition-all hover:border-primary hover:bg-white disabled:cursor-not-allowed disabled:opacity-70"
                      >
                        {isSendingSms
                          ? '发送中...'
                          : smsCooldown > 0
                            ? `${smsCooldown}s`
                            : '获取验证码'}
                      </button>
                    </div>
                  </div>
                </>
              ) : (
                <>
                  {/* Username / email input */}
                  <div className="flex flex-col gap-2">
                    <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                      登录账号
                    </label>
                    <div className="group relative">
                      <Mail className="absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-300 transition-colors group-focus-within:text-primary" />
                      <input
                        type="text"
                        value={username}
                        onChange={(event) => setUsername(event.target.value)}
                        placeholder="用户名 / Email"
                        className="w-full rounded-[20px] border-2 border-slate-100 bg-slate-50/50 py-4 pl-14 pr-6 font-bold text-slate-900 outline-none transition-all placeholder:text-slate-400 focus:border-primary focus:bg-white"
                      />
                    </div>
                  </div>

                  {/* Password input */}
                  <div className="flex flex-col gap-2">
                    <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                      密码
                    </label>
                    <div className="group relative">
                      <Lock className="absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-300 transition-colors group-focus-within:text-primary" />
                      <input
                        type="password"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        placeholder="请输入密码"
                        className="w-full rounded-[20px] border-2 border-slate-100 bg-slate-50/50 py-4 pl-14 pr-6 font-bold text-slate-900 outline-none transition-all placeholder:text-slate-400 focus:border-primary focus:bg-white"
                      />
                    </div>
                  </div>
                </>
              )}

              {/* Captcha (shared by both modes) */}
              <div className="flex flex-col gap-2">
                <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  图形验证码
                </label>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_148px]">
                  <input
                    type="text"
                    value={captchaCode}
                    onChange={(event) => setCaptchaCode(event.target.value.toUpperCase())}
                    placeholder="请输入图片中的字符"
                    className="w-full rounded-[20px] border-2 border-slate-100 bg-slate-50/50 px-6 py-4 font-bold uppercase tracking-[0.2em] text-slate-900 outline-none transition-all placeholder:tracking-normal placeholder:text-slate-400 focus:border-primary focus:bg-white"
                  />
                  <button
                    type="button"
                    onClick={() => void loadCaptcha()}
                    disabled={isCaptchaLoading}
                    className="flex h-[58px] items-center justify-center gap-2 rounded-[20px] border-2 border-slate-100 bg-slate-50/70 px-3 transition-all hover:border-primary hover:bg-white disabled:cursor-not-allowed disabled:opacity-70"
                  >
                    {captchaImage ? (
                      <img src={captchaImage} alt="登录验证码" className="h-10 w-full rounded-lg object-cover" />
                    ) : (
                      <span className="text-xs font-bold text-slate-400">加载中...</span>
                    )}
                    <RefreshCcw className={cn('h-4 w-4 text-slate-400', isCaptchaLoading && 'animate-spin')} />
                  </button>
                </div>
              </div>
            </div>

            <div className="flex flex-col gap-6 pt-4">
              {error && (
                <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
                  {error}
                </div>
              )}

              <button
                onClick={() => void handleLogin()}
                disabled={isSubmitting}
                className="flex w-full items-center justify-center gap-3 rounded-[24px] bg-primary py-5 text-lg font-black text-white shadow-2xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95 disabled:cursor-not-allowed disabled:opacity-70"
              >
                <span>{isSubmitting ? '登录中...' : '确认登录'}</span>
                <ArrowRight className="h-6 w-6" />
              </button>

              {selectedRole === 'user' && (
                <div className="flex items-center justify-center gap-4 py-2">
                  <button
                    onClick={() => void handleThirdPartyLogin()}
                    disabled={isSubmitting}
                    className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-50 text-slate-400 shadow-sm transition-all hover:bg-slate-100"
                  >
                    <Github size={20} />
                  </button>
                  <button
                    onClick={() => void handleThirdPartyLogin()}
                    disabled={isSubmitting}
                    className="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-50 text-slate-400 shadow-sm transition-all hover:bg-slate-100"
                  >
                    <MessageSquare size={20} />
                  </button>
                </div>
              )}

              <div className="flex items-center justify-between px-2">
                <button
                  onClick={() => showToast('密码重置功能开发中，请联系管理员', 'info')}
                  className="text-xs font-black text-slate-400 transition-colors hover:text-primary"
                >
                  忘记密码？
                </button>
                <div className="text-xs font-medium text-slate-400">
                  没有账号？
                  <button onClick={() => navigate('/register')} className="font-black text-primary hover:underline">
                    立即注册
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  );
}

