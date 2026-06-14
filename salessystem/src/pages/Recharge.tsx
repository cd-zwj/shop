import { useEffect, useMemo, useState } from 'react';
import { motion } from 'motion/react';
import { Check, Gift, Info, Wallet, Zap } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { appWalletService } from '../services/modules/appWallet';
import type { UnifiedRechargeRule, WalletAccount } from '../types/wallet';
import { ApiError } from '../types/api';
import { cn } from '../lib/utils';
import { formatCurrency } from '../utils/display';
import { openAlipayPaymentWindow, saveAlipayPaymentPayload } from '../utils/alipayPayment';

const PRESET_PACKAGES = [
  { id: 100, label: '基础档', amount: 100 },
  { id: 300, label: '进阶档', amount: 300, popular: true },
  { id: 500, label: '尊享档', amount: 500 },
] as const;

interface RechargePackage {
  id: number;
  label: string;
  amount: number;
  giftAmount: number;
  giftPoints: number;
  popular?: boolean;
}

/** 将后端规则（金额单位：分）转为页面展示用的档位对象。 */
function toPackage(rule: UnifiedRechargeRule, index: number): RechargePackage {
  return {
    id: rule.id,
    label: `档位${index + 1}`,
    amount: rule.rechargeAmount / 100,
    giftAmount: rule.giftAmount / 100,
    giftPoints: rule.giftPoints,
  };
}

/** 当后端规则为空时，使用前端硬编码兜底。 */
function fallbackPackages(): RechargePackage[] {
  return PRESET_PACKAGES.map((p, i) => ({
    id: p.id,
    label: p.label,
    amount: p.amount,
    giftAmount: 0,
    giftPoints: 0,
    popular: 'popular' in p ? p.popular : undefined,
  }));
}

export default function Recharge() {
  const navigate = useNavigate();
  const [packages, setPackages] = useState<RechargePackage[]>([]);
  const [selectedPackage, setSelectedPackage] = useState<number>(0);
  const [customAmount, setCustomAmount] = useState('');
  const [wallet, setWallet] = useState<WalletAccount | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function load() {
      const [walletResult, rules] = await Promise.allSettled([
        appWalletService.getUnifiedWallet(),
        appWalletService.listUnifiedRechargeRules(),
      ]);

      if (!isMounted) return;

      if (walletResult.status === 'fulfilled') {
        setWallet(walletResult.value);
      }

      const resolved =
        rules.status === 'fulfilled' && rules.value.length > 0
          ? rules.value.map((r, i) => toPackage(r, i))
          : fallbackPackages();

      // 将第一个档位标记为推荐
      if (resolved.length > 0) {
        const popularIdx = Math.min(1, resolved.length - 1);
        resolved[popularIdx] = { ...resolved[popularIdx], popular: true };
      }

      setPackages(resolved);
      setSelectedPackage(resolved[0]?.amount ?? 100);
    }

    void load();
    return () => {
      isMounted = false;
    };
  }, []);

  const finalAmount = useMemo(() => {
    const custom = Number(customAmount);
    if (custom > 0) {
      return custom;
    }
    return selectedPackage;
  }, [customAmount, selectedPackage]);

  async function handleRecharge() {
    if (!finalAmount || finalAmount <= 0) {
      setError('请输入有效的充值金额');
      return;
    }

    setError('');
    setIsSubmitting(true);

    try {
      const payment = await appWalletService.createUnifiedRecharge({
        amount: finalAmount,
        paymentChannelCode: 'ALIPAY_PAGE',
      });

      if (payment.externalPayUrl) {
        // 支付宝返回的是 HTML 表单，需要用 openAlipayPaymentWindow 渲染
        const isOpened = openAlipayPaymentWindow(payment.externalPayUrl);
        if (!isOpened) {
          // 弹窗被阻止，保存 payload 到 sessionStorage，让用户在支付状态页手动触发
          if (payment.paymentBillNo) {
            saveAlipayPaymentPayload({
              billNo: payment.paymentBillNo,
              bizNo: payment.rechargeNo,
              source: 'recharge',
              payHtml: payment.externalPayUrl,
            });
          }
        }
      }

      navigate(
        `/payment/status?billNo=${encodeURIComponent(payment.paymentBillNo)}&bizNo=${encodeURIComponent(payment.rechargeNo)}&source=recharge`,
      );
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '充值创建失败，请稍后重试');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 px-4 pb-10 md:mt-8">
      <header className="text-center md:text-left">
        <h1 className="text-4xl font-black tracking-tight text-slate-900">充值中心</h1>
        <p className="mt-2 text-lg font-medium text-slate-500">已接入统一钱包充值接口，提交后会生成真实支付单。</p>
      </header>

      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-col items-center justify-between gap-6 rounded-3xl border border-slate-100 bg-white p-8 shadow-xl shadow-slate-200/50 md:flex-row"
      >
        <div className="flex items-center gap-5">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/5 text-primary shadow-inner">
            <Wallet className="h-8 w-8" />
          </div>
          <div>
            <p className="text-xs font-black uppercase tracking-widest text-slate-400">当前余额</p>
            <p className="mt-1 text-4xl font-black tracking-tight text-slate-900">
              {formatCurrency(wallet?.availableAmount)}
            </p>
          </div>
        </div>
        <div className="text-right text-sm font-bold text-slate-400">钱包类型：{wallet?.walletType || 'UNIFIED'}</div>
      </motion.div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
        {packages.map((pkg) => (
          <label key={pkg.id} className="group relative cursor-pointer">
            <input
              type="radio"
              name="recharge_package"
              className="sr-only"
              checked={!customAmount && selectedPackage === pkg.amount}
              onChange={() => {
                setCustomAmount('');
                setSelectedPackage(pkg.amount);
              }}
            />
            <motion.div
              whileHover={{ y: -5 }}
              whileTap={{ scale: 0.98 }}
              className={cn(
                'relative h-full overflow-hidden rounded-3xl border-2 bg-white p-8 shadow-lg transition-all duration-300',
                !customAmount && selectedPackage === pkg.amount
                  ? 'border-primary ring-4 ring-primary/5 shadow-primary/10'
                  : 'border-slate-100 shadow-slate-200/40',
              )}
            >
              {pkg.popular && (
                <div className="absolute right-0 top-0 rounded-bl-xl bg-primary px-4 py-1.5 text-[10px] font-black uppercase tracking-widest text-white">
                  推荐
                </div>
              )}

              <div className="mb-10 flex items-start justify-between pt-2">
                <div>
                  <p className="text-xs font-black uppercase tracking-widest text-slate-400">{pkg.label}</p>
                  <div className="mt-2 flex items-baseline gap-1">
                    <span className="text-sm font-black text-slate-900">¥</span>
                    <span className="text-4xl font-black tracking-tight text-slate-900">{pkg.amount}</span>
                  </div>
                </div>
                <div
                  className={cn(
                    'flex h-8 w-8 items-center justify-center rounded-full border-2 transition-all',
                    !customAmount && selectedPackage === pkg.amount
                      ? 'border-primary bg-primary text-white'
                      : 'border-slate-200 text-transparent',
                  )}
                >
                  <Check className="h-5 w-5" />
                </div>
              </div>

              <div className="mt-auto border-t border-slate-50 pt-6">
                <div className="flex items-center gap-2 text-primary">
                  <Gift className="h-5 w-5" />
                  <span className="text-base font-black uppercase tracking-tight">
                    预计到账 {formatCurrency(pkg.amount)}
                    {pkg.giftAmount > 0 && ` + ${formatCurrency(pkg.giftAmount)} 赠送`}
                  </span>
                </div>
                {pkg.giftPoints > 0 && (
                  <p className="mt-1 text-xs font-bold text-slate-400">赠送 {pkg.giftPoints} 积分</p>
                )}
                <p className="mt-2 text-xs font-bold text-slate-400">使用真实支付单流程</p>
              </div>
            </motion.div>
          </label>
        ))}
      </div>

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="flex flex-col gap-8 rounded-3xl border border-slate-100 bg-white p-8 shadow-xl shadow-slate-200/30"
      >
        <h3 className="text-xl font-black text-slate-900">自定义金额</h3>
        <div className="flex flex-col items-end gap-6 md:flex-row">
          <div className="relative w-full">
            <label className="mb-3 block px-1 text-xs font-black uppercase tracking-widest text-slate-400">
              输入金额 (¥)
            </label>
            <div className="group relative">
              <span className="absolute left-5 top-1/2 -translate-y-1/2 text-2xl font-black text-slate-300 transition-colors group-focus-within:text-primary">
                ¥
              </span>
              <input
                type="number"
                placeholder="0.00"
                value={customAmount}
                onChange={(e) => setCustomAmount(e.target.value)}
                className="w-full rounded-2xl border-2 border-slate-100 bg-slate-50/50 py-5 pl-12 pr-6 text-3xl font-black text-slate-900 outline-none transition-all placeholder:text-slate-200 focus:border-primary focus:bg-white focus:ring-4 focus:ring-primary/5"
              />
            </div>
          </div>
          <motion.button
            whileTap={{ scale: 0.98 }}
            disabled={isSubmitting}
            onClick={handleRecharge}
            className="w-full whitespace-nowrap rounded-2xl bg-primary px-12 py-6 text-lg font-black text-white shadow-xl shadow-primary/20 transition-all hover:bg-primary-container disabled:cursor-not-allowed disabled:grayscale md:w-auto"
          >
            {isSubmitting ? '创建支付单...' : '确认充值'}
          </motion.button>
        </div>

        {error && (
          <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
            {error}
          </div>
        )}

        <div className="flex items-start gap-2 rounded-2xl border border-slate-100 bg-slate-50/50 p-4 text-slate-400">
          <Info className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
          <p className="text-sm font-medium leading-relaxed">
            当前统一钱包充值默认走 <span className="font-black text-slate-900">ALIPAY_PAGE</span> 支付通道，创建成功后会跳转到支付状态页并可打开外部支付链接。
          </p>
        </div>
      </motion.div>

      <div className="flex items-center justify-center gap-8 py-4 opacity-20 grayscale">
        <Zap className="h-10 w-10" />
        <Wallet className="h-10 w-10" />
        <Gift className="h-10 w-10" />
      </div>
    </div>
  );
}
