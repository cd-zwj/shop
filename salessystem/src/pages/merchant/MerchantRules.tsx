import { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  Gift,
  Plus,
  Save,
  Settings2,
  ShieldCheck,
  Trash2,
  Trophy,
  Zap,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { merchantFinanceService } from '../../services/modules/merchantFinance';
import { ApiError } from '../../types/api';
import type { MerchantRechargeRulePayload } from '../../types/merchant';
import { cn } from '../../lib/utils';
import { formatCurrency } from '../../utils/display';

interface RechargeRuleFormItem {
  id?: number;
  rechargeAmount: string;
  giftAmount: string;
  giftPoints: string;
  enabled: boolean;
}

const EMPTY_RULE: RechargeRuleFormItem = {
  rechargeAmount: '',
  giftAmount: '',
  giftPoints: '',
  enabled: true,
};

export default function MerchantRules() {
  const navigate = useNavigate();
  const { merchantSession } = useAuth();
  const tenantId = merchantSession?.tenantId;
  const [pointsRatio, setPointsRatio] = useState('0');
  const [pointsEnabled, setPointsEnabled] = useState(false);
  const [rechargeRules, setRechargeRules] = useState<RechargeRuleFormItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadRules() {
      if (!tenantId) {
        setError('当前商户会话缺少 tenantId，请重新登录');
        setIsLoading(false);
        return;
      }

      try {
        const [pointsRule, rechargeRuleList] = await Promise.all([
          merchantFinanceService.getPointsRule(tenantId),
          merchantFinanceService.listRechargeRules(tenantId),
        ]);

        if (!isMounted) return;
        setPointsRatio(String(pointsRule.pointsRatio ?? 0));
        setPointsEnabled(Boolean(pointsRule.enabled));
        setRechargeRules(
          (rechargeRuleList ?? []).map((rule) => ({
            id: rule.id,
            rechargeAmount: String(rule.rechargeAmount ?? 0),
            giftAmount: String(rule.giftAmount ?? 0),
            giftPoints: String(rule.giftPoints ?? 0),
            enabled: Number(rule.status) === 1,
          })),
        );
        setError('');
      } catch {
        if (!isMounted) return;
        setError('商户运营规则加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadRules();

    return () => {
      isMounted = false;
    };
  }, [tenantId]);

  const enabledRulesCount = useMemo(
    () => rechargeRules.filter((rule) => rule.enabled).length,
    [rechargeRules],
  );

  function updateRule(index: number, patch: Partial<RechargeRuleFormItem>) {
    setRechargeRules((prev) =>
      prev.map((rule, ruleIndex) => (ruleIndex === index ? { ...rule, ...patch } : rule)),
    );
  }

  function addRule() {
    setRechargeRules((prev) => [...prev, { ...EMPTY_RULE }]);
  }

  function removeRule(index: number) {
    setRechargeRules((prev) => prev.filter((_, ruleIndex) => ruleIndex !== index));
  }

  async function handleSave() {
    if (!tenantId) {
      setError('当前商户会话缺少 tenantId，请重新登录');
      return;
    }

    const ratio = Number(pointsRatio);
    if (!Number.isFinite(ratio) || ratio < 0) {
      setError('积分比例必须是大于等于 0 的数字');
      return;
    }

    let payload: MerchantRechargeRulePayload[];
    try {
      payload = rechargeRules.map((rule, index) => {
        const rechargeAmount = Number(rule.rechargeAmount);
        const giftAmount = Number(rule.giftAmount);
        const giftPoints = Number(rule.giftPoints);

        if (!Number.isFinite(rechargeAmount) || rechargeAmount <= 0) {
          throw new Error(`第 ${index + 1} 条充值规则的充值金额必须大于 0`);
        }
        if (!Number.isFinite(giftAmount) || giftAmount < 0) {
          throw new Error(`第 ${index + 1} 条充值规则的赠送余额不能小于 0`);
        }
        if (!Number.isInteger(giftPoints) || giftPoints < 0) {
          throw new Error(`第 ${index + 1} 条充值规则的赠送积分必须是大于等于 0 的整数`);
        }

        return {
          id: rule.id,
          rechargeAmount,
          giftAmount,
          giftPoints,
          enabled: rule.enabled,
          sortOrder: index + 1,
        };
      });
    } catch (validationError) {
      setError(validationError instanceof Error ? validationError.message : '充值规则校验失败');
      return;
    }

    setIsSaving(true);
    setError('');
    setSuccess('');

    try {
      await Promise.all([
        merchantFinanceService.updatePointsRule(tenantId, {
          pointsRatio: Math.trunc(ratio),
          enabled: pointsEnabled,
        }),
        merchantFinanceService.replaceRechargeRules(tenantId, payload),
      ]);
      setSuccess('商户运营规则已保存到真实接口');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '规则保存失败，请稍后重试');
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 p-4 pb-32 md:p-8">
      <header className="flex flex-col justify-between gap-6 sm:flex-row sm:items-end">
        <div>
          <button
            onClick={() => navigate('/merchant/finance')}
            className="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-widest text-primary transition-all hover:gap-3"
          >
            <ArrowLeft className="h-4 w-4" /> 返回财务总览
          </button>
          <h1 className="text-4xl font-black tracking-tight text-slate-900">商户运营规则配置</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            积分规则和充值规则都已切到真实接口，保存会直接覆盖当前租户的后端配置。
          </p>
        </div>
        <button
          onClick={handleSave}
          disabled={isLoading || isSaving}
          className="flex items-center justify-center gap-2 rounded-[24px] bg-primary px-8 py-4 text-base font-black text-white shadow-2xl shadow-primary/20 transition-all hover:scale-105 active:scale-95 disabled:cursor-not-allowed disabled:opacity-70"
        >
          <Save className="h-5 w-5" /> {isSaving ? '保存中...' : '保存当前配置'}
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

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <div className="flex flex-col gap-8 lg:col-span-8">
          <section className="rounded-[40px] border border-slate-100 bg-white p-10 shadow-sm">
            <div className="mb-8 flex items-center gap-4">
              <div className="rounded-2xl bg-yellow-50 p-3 text-yellow-600">
                <Trophy className="h-6 w-6" />
              </div>
              <div>
                <h2 className="text-xl font-black tracking-tight text-slate-900">积分返利规则</h2>
                <p className="mt-1 text-sm font-medium text-slate-500">
                  对应接口：`GET/PUT /v1/merchant/tenants/{tenantId}/points-rule`
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
              <div className="flex flex-col gap-3">
                <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  每消费 1 元返积分
                </label>
                <input
                  type="number"
                  min="0"
                  step="1"
                  value={pointsRatio}
                  onChange={(event) => setPointsRatio(event.target.value)}
                  disabled={isLoading}
                  className="w-full rounded-[20px] border-2 border-slate-50 bg-slate-50 px-6 py-4 text-xl font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white disabled:cursor-not-allowed disabled:opacity-70"
                />
              </div>

              <div className="flex flex-col gap-3">
                <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                  启用状态
                </label>
                <button
                  type="button"
                  onClick={() => setPointsEnabled((prev) => !prev)}
                  disabled={isLoading}
                  className={cn(
                    'flex items-center justify-between rounded-[20px] border-2 px-6 py-4 text-left transition-all disabled:cursor-not-allowed disabled:opacity-70',
                    pointsEnabled
                      ? 'border-primary bg-primary/5 text-primary'
                      : 'border-slate-100 bg-slate-50 text-slate-500',
                  )}
                >
                  <span className="font-black">{pointsEnabled ? '已启用' : '已停用'}</span>
                  <span className="text-xs font-bold">
                    {pointsEnabled ? '前台消费会返积分' : '前台不再返积分'}
                  </span>
                </button>
              </div>
            </div>
          </section>

          <section className="rounded-[40px] border border-slate-100 bg-white p-10 shadow-sm">
            <div className="mb-8 flex items-center justify-between gap-4">
              <div className="flex items-center gap-4">
                <div className="rounded-2xl bg-blue-50 p-3 text-blue-600">
                  <Zap className="h-6 w-6" />
                </div>
                <div>
                  <h2 className="text-xl font-black tracking-tight text-slate-900">充值梯度规则</h2>
                  <p className="mt-1 text-sm font-medium text-slate-500">
                    保存时会整组替换后端规则列表。
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={addRule}
                className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm font-black text-slate-700 transition-all hover:border-primary hover:text-primary"
              >
                <Plus className="h-4 w-4" /> 添加规则
              </button>
            </div>

            <div className="flex flex-col gap-5">
              {rechargeRules.length === 0 ? (
                <div className="rounded-[28px] border border-dashed border-slate-200 px-6 py-10 text-center text-sm font-medium text-slate-400">
                  当前还没有充值规则，点击右上角可以新增第一条规则。
                </div>
              ) : (
                rechargeRules.map((rule, index) => (
                  <div
                    key={`${rule.id ?? 'new'}-${index}`}
                    className="rounded-[32px] border border-slate-100 bg-slate-50 p-6"
                  >
                    <div className="mb-5 flex items-center justify-between gap-4">
                      <div className="flex items-center gap-3">
                        <div className="rounded-2xl bg-white p-3 text-primary shadow-sm">
                          <Gift className="h-5 w-5" />
                        </div>
                        <div>
                          <p className="text-sm font-black text-slate-900">第 {index + 1} 条规则</p>
                          <p className="text-xs font-medium text-slate-500">
                            前台会展示启用状态的充值赠送梯度。
                          </p>
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => removeRule(index)}
                        className="rounded-2xl border border-red-100 bg-white p-3 text-red-500 transition-all hover:bg-red-50"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>

                    <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
                      <RuleField
                        label="充值金额"
                        value={rule.rechargeAmount}
                        onChange={(value) => updateRule(index, { rechargeAmount: value })}
                        placeholder="100"
                      />
                      <RuleField
                        label="赠送余额"
                        value={rule.giftAmount}
                        onChange={(value) => updateRule(index, { giftAmount: value })}
                        placeholder="10"
                      />
                      <RuleField
                        label="赠送积分"
                        value={rule.giftPoints}
                        onChange={(value) => updateRule(index, { giftPoints: value })}
                        placeholder="100"
                      />
                      <div className="flex flex-col gap-3">
                        <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                          当前状态
                        </label>
                        <button
                          type="button"
                          onClick={() => updateRule(index, { enabled: !rule.enabled })}
                          className={cn(
                            'rounded-[20px] border-2 px-4 py-4 text-sm font-black transition-all',
                            rule.enabled
                              ? 'border-primary bg-primary/5 text-primary'
                              : 'border-slate-100 bg-white text-slate-500',
                          )}
                        >
                          {rule.enabled ? '启用中' : '已停用'}
                        </button>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </section>
        </div>

        <div className="flex flex-col gap-8 lg:col-span-4">
          <section className="rounded-[40px] bg-slate-900 p-8 text-white shadow-xl">
            <p className="text-[10px] font-black uppercase tracking-widest text-slate-500">规则摘要</p>
            <div className="mt-6 flex flex-col gap-6">
              <div>
                <p className="text-xs font-bold text-slate-400">积分规则</p>
                <p className="mt-1 text-2xl font-black">
                  {pointsEnabled ? `${Number(pointsRatio || 0)} / 元` : '未启用'}
                </p>
              </div>
              <div className="border-t border-white/5 pt-6">
                <p className="text-xs font-bold text-slate-400">充值梯度</p>
                <p className="mt-1 text-2xl font-black">{enabledRulesCount} 条启用</p>
                <p className="mt-2 text-sm font-medium text-slate-400">
                  共 {rechargeRules.length} 条规则会在保存时整体提交。
                </p>
              </div>
            </div>
          </section>

          <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
            <div className="mb-5 flex items-center gap-3 text-primary">
              <Settings2 className="h-5 w-5" />
              <span className="text-xs font-black uppercase tracking-widest">前台展示预览</span>
            </div>
            <div className="flex flex-col gap-4">
              {rechargeRules.length === 0 ? (
                <p className="text-sm font-medium text-slate-400">暂无可预览的充值规则。</p>
              ) : (
                rechargeRules.slice(0, 4).map((rule, index) => (
                  <div
                    key={`${rule.id ?? 'preview'}-${index}`}
                    className="rounded-[24px] bg-slate-50 px-5 py-4"
                  >
                    <p className="text-sm font-black text-slate-900">
                      充 {formatCurrency(Number(rule.rechargeAmount || 0))}
                    </p>
                    <p className="mt-1 text-xs font-medium text-slate-500">
                      送 {formatCurrency(Number(rule.giftAmount || 0))} +{' '}
                      {Number(rule.giftPoints || 0)} 积分
                    </p>
                  </div>
                ))
              )}
            </div>
          </section>

          <section className="rounded-[40px] border border-blue-100 bg-blue-50 p-8">
            <div className="flex items-center gap-3 text-primary">
              <ShieldCheck className="h-5 w-5" />
              <span className="text-xs font-black uppercase tracking-widest">接口约束</span>
            </div>
            <p className="mt-3 text-sm font-medium leading-relaxed text-blue-700">
              积分比例要求大于等于 0。充值规则要求充值金额大于 0，赠送余额大于等于 0，赠送积分为整数。保存时后端会根据提交顺序重建规则排序。
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}

function RuleField({
  label,
  value,
  onChange,
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
}) {
  return (
    <div className="flex flex-col gap-3">
      <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
        {label}
      </label>
      <input
        type="number"
        min="0"
        step="0.01"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className="w-full rounded-[20px] border-2 border-white bg-white px-5 py-4 font-black text-slate-900 outline-none transition-all focus:border-primary"
      />
    </div>
  );
}
