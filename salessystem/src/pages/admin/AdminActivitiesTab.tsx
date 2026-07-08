import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Sparkles,
  Plus,
  Calendar,
  Layers,
  X,
  ChevronDown,
  ChevronUp,
  Percent,
  Gift,
  FolderTree
} from 'lucide-react';
import { useToast } from '../../context/ToastContext';
import { adminMarketingService } from '../../services/modules/adminMarketing';
import type { PromotionActivity, ActivityRule, ActivityRuleCreatePayload, ActivityRuleType } from '../../types/marketing';
import { cn } from '../../lib/utils';
import {
  detectMerchantActivityRuleConflicts,
  normalizeRuleType,
  validateMerchantActivityDraft,
  validateMerchantActivityRuleDraft,
} from '../../utils/merchantActivityValidation';

interface AdminActivitiesTabProps {
  statusFilter: string;
}

export default function AdminActivitiesTab({ statusFilter }: AdminActivitiesTabProps) {
  const { showToast } = useToast();
  const [activities, setActivities] = useState<PromotionActivity[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Expanded Activities
  const [expandedActivityId, setExpandedActivityId] = useState<number | null>(null);
  const [activityRules, setActivityRules] = useState<Record<number, ActivityRule[]>>({});
  const [isRulesLoading, setIsRulesLoading] = useState(false);

  // Create Activity Modal
  const [isActivityCreateOpen, setIsActivityCreateOpen] = useState(false);
  const [activityName, setActivityName] = useState('');
  const [activityType, setActivityType] = useState<ActivityRuleType>('FULL_REDUCTION');
  const [actStartTime, setActStartTime] = useState('');
  const [actEndTime, setActEndTime] = useState('');
  const [actDescription, setActDescription] = useState('');
  const [isActivitySubmitting, setIsActivitySubmitting] = useState(false);

  // Create Activity Rule Modal
  const [isRuleCreateOpen, setIsRuleCreateOpen] = useState(false);
  const [targetActivity, setTargetActivity] = useState<PromotionActivity | null>(null);
  const [ruleType, setRuleType] = useState<ActivityRuleType>('FULL_REDUCTION');
  const [ruleThreshold, setRuleThreshold] = useState<number | ''>('');
  const [ruleDiscountAmount, setRuleDiscountAmount] = useState<number | ''>('');
  const [ruleDiscountRate, setRuleDiscountRate] = useState<number | ''>('');
  const [ruleConfigJson, setRuleConfigJson] = useState('');
  const [rulePriority, setRulePriority] = useState<number>(0);
  const [isRuleSubmitting, setIsRuleSubmitting] = useState(false);

  const loadActivities = async () => {
    setIsLoading(true);
    try {
      const status = statusFilter === 'ALL' ? undefined : statusFilter;
      const data = await adminMarketingService.getActivities(status);
      setActivities(data || []);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '加载平台活动失败', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadActivities();
  }, [statusFilter]);

  const loadRules = async (activityId: number) => {
    setIsRulesLoading(true);
    try {
      const data = await adminMarketingService.getActivityRules(activityId);
      setActivityRules((prev) => ({
        ...prev,
        [activityId]: data || [],
      }));
    } catch (err) {
      showToast(err instanceof Error ? err.message : '获取活动规则失败', 'error');
    } finally {
      setIsRulesLoading(false);
    }
  };

  const toggleExpandActivity = async (activityId: number) => {
    if (expandedActivityId === activityId) {
      setExpandedActivityId(null);
    } else {
      setExpandedActivityId(activityId);
      await loadRules(activityId);
    }
  };

  const handleActivateActivity = async (id: number) => {
    try {
      await adminMarketingService.activateActivity(id);
      showToast('平台活动已成功上线', 'success');
      await loadActivities();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '上线活动失败', 'error');
    }
  };

  const handleDisableActivity = async (id: number) => {
    try {
      await adminMarketingService.disableActivity(id);
      showToast('平台活动已成功下线', 'success');
      await loadActivities();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '下线活动失败', 'error');
    }
  };

  const handleCreateActivity = async (e: React.FormEvent) => {
    e.preventDefault();

    const validationIssues = validateMerchantActivityDraft({
      name: activityName,
      startTime: actStartTime,
      endTime: actEndTime,
    });

    if (validationIssues.length > 0) {
      showToast(validationIssues.join('；'), 'error');
      return;
    }

    setIsActivitySubmitting(true);
    try {
      await adminMarketingService.createActivity({
        name: activityName.trim(),
        activityType,
        startTime: new Date(actStartTime).toISOString(),
        endTime: new Date(actEndTime).toISOString(),
        description: actDescription.trim() || undefined,
      });
      showToast('平台活动创建成功', 'success');
      setIsActivityCreateOpen(false);
      resetActivityForm();
      await loadActivities();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '创建活动失败', 'error');
    } finally {
      setIsActivitySubmitting(false);
    }
  };

  const resetActivityForm = () => {
    setActivityName('');
    setActivityType('FULL_REDUCTION');
    setActStartTime('');
    setActEndTime('');
    setActDescription('');
  };

  const handleCreateRule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetActivity) return;

    const validationIssues = validateMerchantActivityRuleDraft({
      ruleType,
      thresholdAmount: ruleThreshold,
      discountAmount: ruleDiscountAmount,
      discountRate: ruleDiscountRate,
      productId: '',
      categoryCode: '',
      ruleConfigJson,
      priority: rulePriority,
    });

    if (validationIssues.length > 0) {
      showToast(validationIssues.join('；'), 'error');
      return;
    }

    const conflictIssues = detectMerchantActivityRuleConflicts(activityRules[targetActivity.id] || [], {
      ruleType,
      thresholdAmount: ruleThreshold,
      discountAmount: ruleDiscountAmount,
      discountRate: ruleDiscountRate,
      productId: '',
      categoryCode: '',
      ruleConfigJson,
      priority: rulePriority,
    });

    if (conflictIssues.length > 0) {
      showToast(conflictIssues.join('；'), 'error');
      return;
    }

    setIsRuleSubmitting(true);
    try {
      const payload: ActivityRuleCreatePayload = {
        ruleType,
        priority: Number(rulePriority),
      };

      if (ruleThreshold) payload.thresholdAmount = Number(ruleThreshold);
      if (ruleDiscountAmount) payload.discountAmount = Number(ruleDiscountAmount);
      if (ruleDiscountRate) payload.discountRate = Number(ruleDiscountRate);
      if (ruleConfigJson) payload.ruleConfigJson = ruleConfigJson.trim();

      await adminMarketingService.addActivityRule(targetActivity.id, payload);
      showToast('添加活动规则成功', 'success');
      setIsRuleCreateOpen(false);
      resetRuleForm();
      await loadRules(targetActivity.id);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '创建规则失败', 'error');
    } finally {
      setIsRuleSubmitting(false);
    }
  };

  const resetRuleForm = (keepType?: ActivityRuleType) => {
    setRuleType(keepType || 'FULL_REDUCTION');
    setRuleThreshold('');
    setRuleDiscountAmount('');
    setRuleDiscountRate('');
    setRuleConfigJson('');
    setRulePriority(0);
  };

  return (
    <>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-bold text-slate-800">平台活动列表</h2>
        <button
          onClick={() => setIsActivityCreateOpen(true)}
          className="flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2.5 text-xs font-bold text-white shadow-md shadow-primary/10 hover:opacity-95"
        >
          <Plus size={14} />
          新建平台活动
        </button>
      </div>

      {isLoading ? (
        <div className="flex min-h-[30vh] items-center justify-center text-slate-400">加载数据中...</div>
      ) : activities.length === 0 ? (
        <div className="flex min-h-[30vh] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white py-12 text-slate-400">
          <Sparkles className="mb-2 h-10 w-10 text-slate-300" />
          <p className="text-xs font-bold">暂无平台活动</p>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {activities.map((act) => {
            const isExpanded = expandedActivityId === act.id;
            const rules = activityRules[act.id] || [];

            return (
              <motion.div
                key={act.id}
                className={cn(
                  "overflow-hidden rounded-[24px] border bg-white shadow-sm transition-all",
                  act.status === 'ACTIVE' ? 'border-indigo-100' : 'border-slate-100'
                )}
              >
                <div className="flex flex-col gap-4 p-6 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-start gap-4">
                    <div className={cn(
                      "rounded-2xl border p-3.5",
                      act.status === 'ACTIVE' ? 'border-indigo-100 bg-indigo-50/50 text-indigo-600' : 'border-slate-100 bg-slate-50/50 text-slate-400'
                    )}>
                      <Sparkles size={20} />
                    </div>
                    <div>
                      <h3 className="font-black text-slate-800 text-lg flex items-center gap-2">
                        {act.name}
                        <span className={`rounded-full px-2 py-0.5 text-[9px] font-black uppercase tracking-wider ${
                          act.status === 'DRAFT' ? 'bg-blue-50 text-blue-600 border border-blue-100' :
                          act.status === 'ACTIVE' ? 'bg-indigo-50 text-indigo-600 border border-indigo-100' :
                          'bg-slate-100 text-slate-500 border border-slate-200'
                        }`}>
                          {act.status === 'DRAFT' ? '草稿' : act.status === 'ACTIVE' ? '已上线' : '已下线'}
                        </span>
                      </h3>
                      <p className="text-xs font-semibold text-slate-400 mt-1 flex items-center gap-1.5">
                        <Calendar size={13} />
                        活动时间：{new Date(act.startTime).toLocaleString()} ~ {new Date(act.endTime).toLocaleString()}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    {act.status === 'DRAFT' && (
                      <button
                        onClick={() => handleActivateActivity(act.id)}
                        className="rounded-xl bg-slate-900 px-4 py-2 text-xs font-bold text-white transition-opacity hover:opacity-90"
                      >
                        上线活动
                      </button>
                    )}
                    {act.status === 'ACTIVE' && (
                      <button
                        onClick={() => handleDisableActivity(act.id)}
                        className="rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-xs font-bold text-red-600 transition-colors hover:bg-red-100"
                      >
                        暂停下线
                      </button>
                    )}
                    <button
                      onClick={() => toggleExpandActivity(act.id)}
                      className="rounded-xl border border-slate-100 bg-slate-50/50 p-2 text-slate-500 hover:text-slate-800"
                    >
                      {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                    </button>
                  </div>
                </div>

                <AnimatePresence>
                  {isExpanded && (
                    <motion.div
                      initial={{ height: 0 }}
                      animate={{ height: 'auto' }}
                      exit={{ height: 0 }}
                      className="border-t border-slate-50 bg-slate-50/30 overflow-hidden"
                    >
                      <div className="p-6 space-y-6">
                        {act.description && (
                          <div>
                            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">活动描述</span>
                            <p className="text-xs font-medium text-slate-600 mt-1">{act.description}</p>
                          </div>
                        )}

                        <div>
                          <div className="flex justify-between items-center mb-3">
                            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">活动规则</span>
                            <button
                              onClick={() => {
                                setTargetActivity(act);
                                resetRuleForm(resolveRuleTypeForActivity(act.activityType));
                                setIsRuleCreateOpen(true);
                              }}
                              className="flex items-center gap-1 text-xs text-primary font-bold hover:underline"
                            >
                              <Plus size={13} />
                              添加规则
                            </button>
                          </div>

                          {isRulesLoading ? (
                            <div className="text-center text-xs font-bold text-slate-400 py-4">读取规则中...</div>
                          ) : rules.length === 0 ? (
                            <div className="rounded-2xl border border-slate-100 bg-white p-6 text-center text-xs font-semibold text-slate-400">
                              尚未配置规则。
                            </div>
                          ) : (
                            <div className="flex flex-col gap-3">
                              {rules.map((rule) => (
                                <div key={rule.id} className="flex flex-col gap-2 rounded-2xl border border-slate-100 bg-white p-4 sm:flex-row sm:items-center sm:justify-between">
                                  <div className="flex items-center gap-3">
                                    <div className="rounded-xl bg-indigo-50 p-2 text-indigo-600">
                                      {rule.ruleType === 'FULL_REDUCTION' && <Layers size={16} />}
                                      {isDiscountRule(rule.ruleType) && <Percent size={16} />}
                                      {rule.ruleType === 'BUY_X_GET_Y' && <Gift size={16} />}
                                      {rule.ruleType === 'CATEGORY_DISCOUNT' && <FolderTree size={16} />}
                                    </div>
                                    <div>
                                      <p className="text-xs font-black text-slate-800">
                                        {rule.ruleType === 'FULL_REDUCTION' && '满减规则'}
                                        {isDiscountRule(rule.ruleType) && '满折规则'}
                                        {rule.ruleType === 'BUY_X_GET_Y' && '买赠规则'}
                                        {rule.ruleType === 'CATEGORY_DISCOUNT' && '品类折扣'}
                                      </p>
                                      <p className="text-[11px] font-bold text-slate-400 mt-0.5">
                                        {rule.ruleType === 'FULL_REDUCTION' && `满 ¥${rule.thresholdAmount} 减 ¥${rule.discountAmount}`}
                                        {isDiscountRule(rule.ruleType) && `满 ¥${rule.thresholdAmount} 打 ${(rule.discountRate ?? 1) * 10} 折`}
                                        {rule.ruleType === 'BUY_X_GET_Y' && `购买商品 ID: ${rule.productId}`}
                                        {rule.ruleType === 'CATEGORY_DISCOUNT' && `品类: ${rule.categoryCode}，折扣: ${(rule.discountRate ?? 1) * 10} 折`}
                                      </p>
                                    </div>
                                  </div>
                                  <span className="text-[10px] font-black tracking-widest text-slate-400">优先级: {rule.priority}</span>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            );
          })}
        </div>
      )}

      {/* Activity Modal */}
      <AnimatePresence>
        {isActivityCreateOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="w-full max-w-md overflow-hidden rounded-[24px] border border-slate-100 bg-white shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4">
                <h3 className="text-lg font-black text-slate-900">新建平台活动</h3>
                <button onClick={() => setIsActivityCreateOpen(false)} className="rounded-xl p-1 text-slate-400 hover:bg-slate-200/50 hover:text-slate-600">
                  <X size={18} />
                </button>
              </div>

              <form onSubmit={handleCreateActivity} className="p-6 space-y-5">
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="actName" className="text-xs font-bold text-slate-700">活动名称</label>
                  <input
                    id="actName"
                    type="text"
                    placeholder="如：全平台国庆大聚惠"
                    value={activityName}
                    onChange={(e) => setActivityName(e.target.value)}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none"
                    required
                  />
                </div>

                <div className="flex flex-col gap-1.5">
                  <label htmlFor="actType" className="text-xs font-bold text-slate-700">活动类型</label>
                  <select
                    id="actType"
                    value={activityType}
                    onChange={(event) => setActivityType(event.target.value as ActivityRuleType)}
                    className="w-full rounded-2xl border border-slate-200 bg-white p-3 text-sm font-bold text-slate-800 outline-none"
                  >
                    <option value="FULL_REDUCTION">满减活动</option>
                    <option value="DISCOUNT_RATE">折扣活动</option>
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="actStart" className="text-xs font-bold text-slate-700">开始时间</label>
                    <input
                      id="actStart"
                      type="datetime-local"
                      value={actStartTime}
                      onChange={(e) => setActStartTime(e.target.value)}
                      className="w-full rounded-2xl border border-slate-200 px-3 py-2 text-xs font-semibold outline-none"
                      required
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="actEnd" className="text-xs font-bold text-slate-700">结束时间</label>
                    <input
                      id="actEnd"
                      type="datetime-local"
                      value={actEndTime}
                      onChange={(e) => setActEndTime(e.target.value)}
                      className="w-full rounded-2xl border border-slate-200 px-3 py-2 text-xs font-semibold outline-none"
                      required
                    />
                  </div>
                </div>

                <div className="flex flex-col gap-1.5">
                  <label htmlFor="actDesc" className="text-xs font-bold text-slate-700">活动描述</label>
                  <textarea
                    id="actDesc"
                    rows={3}
                    placeholder="请输入活动规则、说明等...详情"
                    value={actDescription}
                    onChange={(e) => setActDescription(e.target.value)}
                    className="w-full rounded-2xl border border-slate-200 p-3 text-sm font-medium outline-none"
                  />
                </div>

                <div className="flex items-center justify-end gap-3 border-t border-slate-50 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setIsActivityCreateOpen(false);
                      resetActivityForm();
                    }}
                    className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-bold text-slate-500 hover:bg-slate-50"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    disabled={isActivitySubmitting}
                    className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white hover:opacity-90"
                  >
                    {isActivitySubmitting ? '保存中...' : '保存为草稿'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Rule Modal */}
      <AnimatePresence>
        {isRuleCreateOpen && targetActivity && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm overflow-y-auto">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="my-8 w-full max-w-md overflow-hidden rounded-[24px] border border-slate-100 bg-white shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4">
                <div>
                  <h3 className="text-lg font-black text-slate-900">配置平台优惠规则</h3>
                  <p className="text-xs font-bold text-slate-400">活动名称：{targetActivity.name}</p>
                </div>
                <button onClick={() => setIsRuleCreateOpen(false)} className="rounded-xl p-1 text-slate-400 hover:bg-slate-200/50 hover:text-slate-600">
                  <X size={18} />
                </button>
              </div>

              <form onSubmit={handleCreateRule} className="max-h-[70vh] overflow-y-auto p-6 space-y-5">
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-700">规则类型</label>
                  <select
                    value={ruleType}
                    onChange={(e) => {
                      const val = e.target.value as ActivityRuleType;
                      resetRuleForm(val);
                    }}
                    className="w-full rounded-2xl border border-slate-200 bg-white p-3 text-sm font-bold text-slate-800 outline-none"
                  >
                    {getRuleTypeOptions(targetActivity.activityType).map((option) => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </div>

                {(ruleType === 'FULL_REDUCTION' || isDiscountRule(ruleType)) && (
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="ruleThreshold" className="text-xs font-bold text-slate-700">消费门槛金额 (元)</label>
                    <input
                      id="ruleThreshold"
                      type="number"
                      min="0"
                      step="0.01"
                      value={ruleThreshold}
                      onChange={(e) => setRuleThreshold(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none"
                      required
                    />
                  </div>
                )}

                {ruleType === 'FULL_REDUCTION' && (
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="ruleDiscount" className="text-xs font-bold text-slate-700">减免金额 (元)</label>
                    <input
                      id="ruleDiscount"
                      type="number"
                      min="0.01"
                      step="0.01"
                      value={ruleDiscountAmount}
                      onChange={(e) => setRuleDiscountAmount(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none"
                      required
                    />
                  </div>
                )}

                {isDiscountRule(ruleType) && (
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="ruleRate" className="text-xs font-bold text-slate-700">打折比例 (如0.85 = 85折)</label>
                    <input
                      id="ruleRate"
                      type="number"
                      min="0.01"
                      max="0.99"
                      step="0.01"
                      value={ruleDiscountRate}
                      onChange={(e) => setRuleDiscountRate(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none"
                      required
                    />
                  </div>
                )}

                <div className="flex flex-col gap-1.5">
                  <label htmlFor="rulePri" className="text-xs font-bold text-slate-700">优先级 (数字越大越优先)</label>
                  <input
                    id="rulePri"
                    type="number"
                    min="0"
                    value={rulePriority}
                    onChange={(e) => setRulePriority(Number(e.target.value))}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none"
                    required
                  />
                </div>

                <div className="flex flex-col gap-1.5">
                  <label htmlFor="configJson" className="text-xs font-bold text-slate-700">规则高级配置 JSON (选填)</label>
                  <textarea
                    id="configJson"
                    rows={2}
                    placeholder='如: {"buyX": 2, "getY": 1}'
                    value={ruleConfigJson}
                    onChange={(e) => setRuleConfigJson(e.target.value)}
                    className="w-full rounded-2xl border border-slate-200 p-3 text-sm font-medium outline-none"
                  />
                </div>

                <div className="flex items-center justify-end gap-3 border-t border-slate-50 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setIsRuleCreateOpen(false);
                      resetRuleForm();
                    }}
                    className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-bold text-slate-500 hover:bg-slate-50"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    disabled={isRuleSubmitting}
                    className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white hover:opacity-90"
                  >
                    {isRuleSubmitting ? '保存中...' : '保存'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </>
  );
}

function isDiscountRule(ruleType: ActivityRuleType | string) {
  return normalizeRuleType(ruleType) === 'DISCOUNT_RATE';
}

function resolveRuleTypeForActivity(activityType: ActivityRuleType | string): ActivityRuleType {
  const normalized = normalizeRuleType(activityType);
  return normalized === 'DISCOUNT_RATE' ? 'DISCOUNT_RATE' : 'FULL_REDUCTION';
}

function getRuleTypeOptions(activityType: ActivityRuleType | string) {
  const ruleType = resolveRuleTypeForActivity(activityType);
  if (ruleType === 'DISCOUNT_RATE') {
    return [{ value: 'DISCOUNT_RATE' as const, label: '满折规则 (DISCOUNT_RATE)' }];
  }
  return [{ value: 'FULL_REDUCTION' as const, label: '满减规则 (FULL_REDUCTION)' }];
}
