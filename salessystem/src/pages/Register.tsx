import { useState, type FormEvent } from 'react';
import { motion } from 'motion/react';
import {
  ArrowRight,
  ChevronLeft,
  Github,
  Lock,
  Mail,
  MessageSquare,
  Phone,
  User,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../types/api';

export default function Register() {
  const navigate = useNavigate();
  const { registerUser } = useAuth();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [accepted, setAccepted] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!username.trim() || !password.trim()) {
      setError('用户名和密码为必填项');
      return;
    }
    if (!accepted) {
      setError('请先同意服务协议与隐私政策');
      return;
    }

    setIsSubmitting(true);
    setError('');
    setSuccess('');

    try {
      await registerUser({
        username: username.trim(),
        password: password.trim(),
        email: email.trim() || undefined,
        phone: phone.trim() || undefined,
      });

      setSuccess('注册成功，即将返回登录页');
      setTimeout(() => {
        navigate('/login');
      }, 900);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '注册失败，请稍后重试');
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
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-slate-400 transition-colors hover:text-slate-900"
        >
          <ChevronLeft size={14} /> 返回
        </button>

        <header className="flex flex-col gap-2">
          <h1 className="text-4xl font-black tracking-tighter text-slate-900">创建账户</h1>
          <p className="font-medium text-slate-500">注册成功后即可使用真实用户接口登录系统。</p>
        </header>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <Field
            icon={User}
            type="text"
            placeholder="用户名 / Username"
            value={username}
            onChange={setUsername}
          />
          <Field
            icon={Mail}
            type="email"
            placeholder="电子邮箱 / Email"
            value={email}
            onChange={setEmail}
          />
          <Field
            icon={Phone}
            type="text"
            placeholder="手机号 / Phone"
            value={phone}
            onChange={setPhone}
          />
          <Field
            icon={Lock}
            type="password"
            placeholder="设置密码 / Password"
            value={password}
            onChange={setPassword}
          />

          <div className="mt-2 flex items-start gap-3 px-2">
            <input
              type="checkbox"
              checked={accepted}
              onChange={(event) => setAccepted(event.target.checked)}
              className="mt-1 rounded border-slate-200 text-primary focus:ring-primary"
            />
            <p className="text-[11px] font-medium leading-relaxed text-slate-400">
              我已阅读并同意
              <span className="font-black text-primary"> 服务协议 </span>
              与
              <span className="font-black text-primary"> 隐私政策</span>
            </p>
          </div>

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
            {isSubmitting ? '提交中...' : '立即注册'} <ArrowRight size={20} />
          </button>
        </form>

        <div className="relative flex items-center justify-center py-4">
          <div className="absolute left-0 right-0 h-px bg-slate-100" />
          <span className="relative z-10 bg-white px-4 text-[10px] font-black uppercase tracking-widest text-slate-300">
            第三方快捷注册
          </span>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <button className="flex items-center justify-center gap-2 rounded-2xl border border-slate-100 bg-slate-50 py-4 text-xs font-black uppercase tracking-widest text-slate-900 transition-all hover:bg-slate-100">
            <Github size={16} /> Github
          </button>
          <button className="flex items-center justify-center gap-2 rounded-2xl border border-slate-100 bg-slate-50 py-4 text-xs font-black uppercase tracking-widest text-slate-900 transition-all hover:bg-slate-100">
            <MessageSquare size={16} /> WeChat
          </button>
        </div>

        <p className="mt-4 text-center text-sm font-medium text-slate-400">
          已有账号？
          <button onClick={() => navigate('/login')} className="font-black text-primary transition-all hover:underline">
            立即登录
          </button>
        </p>
      </motion.div>
    </div>
  );
}

function Field({
  icon: Icon,
  type,
  placeholder,
  value,
  onChange,
}: {
  icon: typeof User;
  type: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="flex flex-col gap-2">
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
    </div>
  );
}
