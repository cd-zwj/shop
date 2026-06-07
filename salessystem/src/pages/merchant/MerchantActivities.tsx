import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Sparkles,
  Plus,
  Calendar,
  Layers,
  Settings,
  X,
  AlertCircle,
  ShoppingBag,
  FolderTree,
  Eye,
  CheckCircle,
  PowerOff,
  ChevronDown,
  ChevronUp,
  Percent,
  Gift,
  ArrowRight
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { merchantMarketingService } from '../../services/modules/merchantMarketing';
import type { PromotionActivity, ActivityRule, ActivityRuleCreatePayload } from '../../types/marketing';
import { formatCurrency } from '../../utils/display';
import { cn } from '../../lib/utils';

export default function MerchantActivities() {
  const { merchantSession } = useAuth();
  const { showToast } = useToast();
  const tenantId = merchantSession?.tenantId;

  const [activities, setActivities] = useState<PromotionActivity[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<string>('ALL'); // ALL, DRAFT, ACTIVE, DISABLED
  
  // Expanded Activities to show rules
  const [expandedActivityId, setExpandedActivityId] = useState<number | null>(null);
  const [activityRules, setActivityRules] = useState<Record<number, ActivityRule[]>>({});
  const [isRulesLoading, setIsRulesLoading] = useState(false);

  // Create Activity Modal
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [name, setName] = useState('');
  const [activityType, setActivityType] = useState('PROMOTION');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [description, setDescription] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Create Rule Modal
  const [isCreateRuleOpen, setIsCreateRuleOpen] = useState(false);
  const [targetActivity, setTargetActivity] = useState<PromotionActivity | null>(null);
  const [ruleType, setRuleType] = useState<'FULL_REDUCTION' | 'FULL_DISCOUNT' | 'BUY_X_GET_Y' | 'CATEGORY_DISCOUNT'>('FULL_REDUCTION');
  const [thresholdAmount, setThresholdAmount] = useState<number | ''>('');
  const [discountAmount, setDiscountAmount] = useState<number | ''>('');
  const [discountRate, setDiscountRate] = useState<number | ''>('');
  const [productId, setProductId] = useState<number | ''>('');
  const [categoryCode, setCategoryCode] = useState('');
  const [ruleConfigJson, setRuleConfigJson] = useState('');
  const [priority, setPriority] = useState<number>(0);
  const [isRuleSubmitting, setIsRuleSubmitting] = useState(false);

  const tabs = [
    { id: 'ALL', label: '全部活动' },
    { id: 'DRAFT', label: '草稿' },
    { id: 'ACTIVE', label: '已上线' },
    { id: 'DISABLED', label: '已下线' },
  ];

  const loadActivities = async () => {
    if (!tenantId) return;
    setIsLoading(true);
    try {
      const data = await merchantMarketingService.getActivities(
        tenantId,
        activeTab === 'ALL' ? undefined : activeTab
      );
      setActivities(data || []);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '获取促销活动列表失败', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadActivities();
  }, [tenantId, activeTab]);

  const loadRules = async (activityId: number) => {
    if (!tenantId) return;
    setIsRulesLoading(true);
    try {
      const data = await merchantMarketingService.getActivityRules(tenantId, activityId);
      setActivityRules((prev) => ({
        ...prev,
        [activityId]: data || [],
      }));
    } catch (err) {
      showToast(err instanceof Error ? err.message : '获取规则列表失败', 'error');
    } finally {
      setIsRulesLoading(false);
    }
  };

  const toggleExpand = async (activityId: number) => {
    if (expandedActivityId === activityId) {
      setExpandedActivityId(null);
    } else {
      setExpandedActivityId(activityId);
      await loadRules(activityId);
    }
  };

  const handleCreateActivity = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tenantId) return;

    if (!name.trim()) {
      showToast('请输入活动名称', 'error');
      return;
    }
    if (!startTime || !endTime) {
      showToast('请选择活动开始与结束时间', 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      await merchantMarketingService.createActivity(tenantId, {
        name: name.trim(),
        activityType,
        startTime: new Date(startTime).toISOString(),
        endTime: new Date(endTime).toISOString(),
        description: description.trim() || undefined,
      });
      showToast('新建促销活动成功', 'success');
      setIsCreateOpen(false);
      resetActivityForm();
      await loadActivities();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '创建活动失败', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetActivityForm = () => {
    setName('');
    setActivityType('PROMOTION');
    setStartTime('');
    setEndTime('');
    setDescription('');
  };

  const handleCreateRule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tenantId || !targetActivity) return;

    // Validation
    if (ruleType === 'FULL_REDUCTION' && (!thresholdAmount || !discountAmount)) {
      showToast('请输入满减门槛与优惠金额', 'error');
      return;
    }
    if (ruleType === 'FULL_DISCOUNT' && (!thresholdAmount || !discountRate)) {
      showToast('请输入满折门槛与折扣比例', 'error');
      return;
    }
    if (ruleType === 'BUY_X_GET_Y' && !productId) {
      showToast('请输入买赠的商品 ID', 'error');
      return;
    }
    if (ruleType === 'CATEGORY_DISCOUNT' && (!categoryCode || !discountRate)) {
      showToast('请输入分类编码与折扣比例', 'error');
      return;
    }

    setIsRuleSubmitting(true);
    try {
      const payload: ActivityRuleCreatePayload = {
        ruleType,
        priority: Number(priority),
      };

      if (thresholdAmount) payload.thresholdAmount = Number(thresholdAmount);
      if (discountAmount) payload.discountAmount = Number(discountAmount);
      if (discountRate) payload.discountRate = Number(discountRate);
      if (productId) payload.productId = Number(productId);
      if (categoryCode) payload.categoryCode = categoryCode.trim();
      if (ruleConfigJson) payload.ruleConfigJson = ruleConfigJson.trim();

      await merchantMarketingService.addActivityRule(tenantId, targetActivity.id, payload);
      showToast('添加活动规则成功', 'success');
      setIsCreateRuleOpen(false);
      resetRuleForm();
      await loadRules(targetActivity.id);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '创建规则失败', 'error');
    } finally {
      setIsRuleSubmitting(false);
    }
  };

  const resetRuleForm = (keepType?: 'FULL_REDUCTION' | 'FULL_DISCOUNT' | 'BUY_X_GET_Y' | 'CATEGORY_DISCOUNT') => {
    setRuleType(keepType || 'FULL_REDUCTION');
    setThresholdAmount('');
    setDiscountAmount('');
    setDiscountRate('');
    setProductId('');
    setCategoryCode('');
    setRuleConfigJson('');
    setPriority(0);
  };

  const handleActivate = async (id: number) => {
    if (!tenantId) return;
    try {
      await merchantMarketingService.activateActivity(tenantId, id);
      showToast('促销活动已成功上线', 'success');
      await loadActivities();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '上线活动失败', 'error');
    }
  };

  const handleDisable = async (id: number) => {
    if (!tenantId) return;
    try {
      await merchantMarketingService.disableActivity(tenantId, id);
      showToast('促销活动已暂停下线', 'success');
      await loadActivities();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '下线活动失败', 'error');
    }
  };

  return (
    <div className="flex flex-col gap-6 p-6 pb-20">
      <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-black tracking-tight text-slate-900">促销活动管理</h1>
          <p className="text-sm font-medium text-slate-500">
            创建及维护店铺促销。包含满减、满折、买赠等多样化规则配置，助力营收增长。
          </p>
        </div>
        <button
          onClick={() => setIsCreateOpen(true)}
          className="flex items-center justify-center gap-2 rounded-2xl bg-primary px-5 py-3 text-sm font-bold text-white shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
        >
          <Plus size={16} />
          新建活动
        </button>
      </header>

      {/* Tabs */}
      <div className="flex border-b border-slate-100 overflow-x-auto hide-scrollbar">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`whitespace-nowrap px-6 pb-4 text-sm font-bold border-b-2 transition-all ${
              activeTab === tab.id
                ? 'border-primary text-primary font-black scale-[1.02]'
                : 'border-transparent text-slate-400 hover:text-slate-600'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="flex min-h-[40vh] flex-col items-center justify-center gap-2 text-slate-500">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary/30 border-t-primary" />
          <span className="text-sm font-semibold">加载数据中...</span>
        </div>
      ) : activities.length === 0 ? (
        <div className="flex min-h-[40vh] flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white py-12 text-slate-400">
          <Sparkles className="mb-4 h-12 w-12 text-slate-300" />
          <p className="font-bold">暂无活动数据</p>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {activities.map((act) => {
            const isExpanded = expandedActivityId === act.id;
            const rules = activityRules[act.id] || [];

            return (
              <motion.div
                key={act.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className={cn(
                  "overflow-hidden rounded-[32px] border bg-white shadow-sm transition-all",
                  act.status === 'ACTIVE' ? 'border-indigo-100' : 'border-slate-100'
                )}
              >
                {/* Header row */}
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
                        onClick={() => handleActivate(act.id)}
                        className="rounded-xl bg-slate-900 px-4 py-2 text-xs font-bold text-white transition-opacity hover:opacity-90"
                      >
                        上线活动
                      </button>
                    )}
                    {act.status === 'ACTIVE' && (
                      <button
                        onClick={() => handleDisable(act.id)}
                        className="rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-xs font-bold text-red-600 transition-colors hover:bg-red-100"
                      >
                        暂停下线
                      </button>
                    )}
                    <button
                      onClick={() => toggleExpand(act.id)}
                      className="rounded-xl border border-slate-100 bg-slate-50/50 p-2 text-slate-500 hover:text-slate-800"
                    >
                      {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                    </button>
                  </div>
                </div>

                {/* Expanded Details / Rules */}
                <AnimatePresence>
                  {isExpanded && (
                    <motion.div
                      initial={{ height: 0 }}
                      animate={{ height: 'auto' }}
                      exit={{ height: 0 }}
                      className="border-t border-slate-50 bg-slate-50/30 overflow-hidden"
                    >
                      <div className="p-6 space-y-6">
                        {/* Description */}
                        {act.description && (
                          <div>
                            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">活动描述</span>
                            <p className="text-xs font-medium text-slate-600 mt-1">{act.description}</p>
                          </div>
                        )}

                        {/* Rules table/list */}
                        <div>
                          <div className="flex justify-between items-center mb-3">
                            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">活动优惠规则</span>
                            <button
                              onClick={() => {
                                setTargetActivity(act);
                                setIsCreateRuleOpen(true);
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
                              尚未配置规则。点击右上角“添加规则”配置此活动的优惠方式。
                            </div>
                          ) : (
                            <div className="flex flex-col gap-3">
                              {rules.map((rule) => (
                                <div key={rule.id} className="flex flex-col gap-2 rounded-2xl border border-slate-100 bg-white p-4 sm:flex-row sm:items-center sm:justify-between">
                                  <div className="flex items-center gap-3">
                                    <div className="rounded-xl bg-indigo-50 p-2 text-indigo-600">
                                      {rule.ruleType === 'FULL_REDUCTION' && <Layers size={16} />}
                                      {rule.ruleType === 'FULL_DISCOUNT' && <Percent size={16} />}
                                      {rule.ruleType === 'BUY_X_GET_Y' && <Gift size={16} />}
                                      {rule.ruleType === 'CATEGORY_DISCOUNT' && <FolderTree size={16} />}
                                    </div>
                                    <div>
                                      <p className="text-xs font-black text-slate-800">
                                        {rule.ruleType === 'FULL_REDUCTION' && '阶梯满减规则'}
                                        {rule.ruleType === 'FULL_DISCOUNT' && '阶梯满折规则'}
                                        {rule.ruleType === 'BUY_X_GET_Y' && '买赠规则'}
                                        {rule.ruleType === 'CATEGORY_DISCOUNT' && '品类折扣规则'}
                                      </p>
                                      <p className="text-[11px] font-bold text-slate-400 mt-0.5">
                                        {rule.ruleType === 'FULL_REDUCTION' && `消费满 ¥${rule.thresholdAmount} 减免 ¥${rule.discountAmount}`}
                                        {rule.ruleType === 'FULL_DISCOUNT' && `消费满 ¥${rule.thresholdAmount} 享受 ${(rule.discountRate ?? 1) * 10} 折`}
                                        {rule.ruleType === 'BUY_X_GET_Y' && `购买商品 ID #${rule.productId}，配置: ${rule.ruleConfigJson || '买X赠Y'}`}
                                        {rule.ruleType === 'CATEGORY_DISCOUNT' && `限制商品品类 [${rule.categoryCode}]，折扣 ${(rule.discountRate ?? 1) * 10} 折`}
                                      </p>
                                    </div>
                                  </div>

                                  <div className="text-right">
                                    <span className="text-[10px] font-black tracking-widest uppercase text-slate-400">优先级: {rule.priority}</span>
                                  </div>
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

      {/* Create Activity Modal */}
      <AnimatePresence>
        {isCreateOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="w-full max-w-md overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4">
                <h3 className="text-lg font-black text-slate-900">新建促销活动</h3>
                <button onClick={() => setIsCreateOpen(false)} className="rounded-xl p-1 text-slate-400 hover:bg-slate-200/50 hover:text-slate-600">
                  <X size={18} />
                </button>
              </div>

              <form onSubmit={handleCreateActivity} className="p-6 space-y-5">
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="actName" className="text-xs font-bold text-slate-700">活动名称</label>
                  <input
                    id="actName"
                    type="text"
                    placeholder="如：618全店年中大促"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="actStart" className="text-xs font-bold text-slate-700">开始时间</label>
                    <input
                      id="actStart"
                      type="datetime-local"
                      value={startTime}
                      onChange={(e) => setStartTime(e.target.value)}
                      className="w-full rounded-2xl border border-slate-200 px-3 py-2 text-xs font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="actEnd" className="text-xs font-bold text-slate-700">结束时间</label>
                    <input
                      id="actEnd"
                      type="datetime-local"
                      value={endTime}
                      onChange={(e) => setEndTime(e.target.value)}
                      className="w-full rounded-2xl border border-slate-200 px-3 py-2 text-xs font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>
                </div>

                <div className="flex flex-col gap-1.5">
                  <label htmlFor="actDesc" className="text-xs font-bold text-slate-700">活动说明 (选填)</label>
                  <textarea
                    id="actDesc"
                    rows={3}
                    placeholder="请输入活动的促销政策、参与指南等..."
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="w-full rounded-2xl border border-slate-200 p-3 text-sm font-medium outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                  />
                </div>

                <div className="flex items-center justify-end gap-3 border-t border-slate-50 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setIsCreateOpen(false);
                      resetActivityForm();
                    }}
                    className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-bold text-slate-500 hover:bg-slate-50"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white transition-opacity hover:opacity-90 disabled:opacity-50"
                  >
                    {isSubmitting ? '保存中...' : '保存为草稿'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Create Rule Modal */}
      <AnimatePresence>
        {isCreateRuleOpen && targetActivity && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm overflow-y-auto">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="my-8 w-full max-w-md overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4">
                <div>
                  <h3 className="text-lg font-black text-slate-900">配置优惠规则</h3>
                  <p className="text-xs font-bold text-slate-400">活动名称：{targetActivity.name}</p>
                </div>
                <button onClick={() => setIsCreateRuleOpen(false)} className="rounded-xl p-1 text-slate-400 hover:bg-slate-200/50 hover:text-slate-600">
                  <X size={18} />
                </button>
              </div>

              <form onSubmit={handleCreateRule} className="max-h-[70vh] overflow-y-auto p-6 space-y-5">
                {/* Rule Type */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-700">规则类型</label>
                  <select
                    value={ruleType}
                    onChange={(e) => {
                      const val = e.target.value as 'FULL_REDUCTION' | 'FULL_DISCOUNT' | 'BUY_X_GET_Y' | 'CATEGORY_DISCOUNT';
                      resetRuleForm(val);
                    }}
                    className="w-full rounded-2xl border border-slate-200 bg-white p-3 text-sm font-bold text-slate-800 outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                  >
                    <option value="FULL_REDUCTION">满减规则 (FULL_REDUCTION)</option>
                    <option value="FULL_DISCOUNT">满折规则 (FULL_DISCOUNT)</option>
                    <option value="BUY_X_GET_Y">买赠规则 (BUY_X_GET_Y)</option>
                    <option value="CATEGORY_DISCOUNT">分类折扣 (CATEGORY_DISCOUNT)</option>
                  </select>
                </div>

                {/* Conditional Fields based on ruleType */}
                {(ruleType === 'FULL_REDUCTION' || ruleType === 'FULL_DISCOUNT') && (
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="threshold" className="text-xs font-bold text-slate-700">消费门槛金额 (元)</label>
                    <input
                      id="threshold"
                      type="number"
                      min="0"
                      step="0.01"
                      placeholder="例如满 100 元"
                      value={thresholdAmount}
                      onChange={(e) => setThresholdAmount(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>
                )}

                {ruleType === 'FULL_REDUCTION' && (
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="discount" className="text-xs font-bold text-slate-700">减免金额 (元)</label>
                    <input
                      id="discount"
                      type="number"
                      min="0.01"
                      step="0.01"
                      placeholder="例如减免 10 元"
                      value={discountAmount}
                      onChange={(e) => setDiscountAmount(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>
                )}

                {(ruleType === 'FULL_DISCOUNT' || ruleType === 'CATEGORY_DISCOUNT') && (
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="rate" className="text-xs font-bold text-slate-700">打折比例 (如0.85 = 85折)</label>
                    <input
                      id="rate"
                      type="number"
                      min="0.01"
                      max="0.99"
                      step="0.01"
                      placeholder="0.01 - 0.99"
                      value={discountRate}
                      onChange={(e) => setDiscountRate(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>
                )}

                {ruleType === 'BUY_X_GET_Y' && (
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="product" className="text-xs font-bold text-slate-700">赠送商品的关联主商品 ID</label>
                    <input
                      id="product"
                      type="number"
                      min="1"
                      placeholder="商品 ID"
                      value={productId}
                      onChange={(e) => setProductId(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>
                )}

                {ruleType === 'CATEGORY_DISCOUNT' && (
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="category" className="text-xs font-bold text-slate-700">限制分类编码</label>
                    <input
                      id="category"
                      type="text"
                      placeholder="如：category_shoes"
                      value={categoryCode}
                      onChange={(e) => setCategoryCode(e.target.value)}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>
                )}

                {/* Priority */}
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="priority" className="text-xs font-bold text-slate-700">规则优先级 (数字越大优先级越高)</label>
                  <input
                    id="priority"
                    type="number"
                    min="0"
                    value={priority}
                    onChange={(e) => setPriority(Number(e.target.value))}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>

                {/* ruleConfigJson for advanced config */}
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="configJson" className="text-xs font-bold text-slate-700">规则高级配置 JSON (选填)</label>
                  <textarea
                    id="configJson"
                    rows={2}
                    placeholder='如: {"buyX": 2, "getY": 1}'
                    value={ruleConfigJson}
                    onChange={(e) => setRuleConfigJson(e.target.value)}
                    className="w-full rounded-2xl border border-slate-200 p-3 text-sm font-medium outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                  />
                </div>

                <div className="flex items-center justify-end gap-3 border-t border-slate-50 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setIsCreateRuleOpen(false);
                      resetRuleForm();
                    }}
                    className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-bold text-slate-500 hover:bg-slate-50"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    disabled={isRuleSubmitting}
                    className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white transition-opacity hover:opacity-90"
                  >
                    {isRuleSubmitting ? '保存中...' : '保存规则'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
