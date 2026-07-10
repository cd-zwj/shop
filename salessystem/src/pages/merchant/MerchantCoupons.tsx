import React, { useCallback, useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Ticket,
  Plus,
  Layers,
  Settings,
  X,
  ShoppingBag,
  FolderTree,
  CheckCircle,
  PowerOff
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { merchantMarketingService } from '../../services/modules/merchantMarketing';
import type {
  MerchantCouponTemplate,
  CouponScope,
  CouponTemplateCreatePayload,
  CouponScopeCreatePayload,
  MarketingEffectSummary,
  MemberLevel,
  MemberTag,
} from '../../types/marketing';
import { cn } from '../../lib/utils';
import { validateMerchantCouponDraft } from '../../utils/merchantCouponValidation';

export default function MerchantCoupons() {
  const { merchantSession } = useAuth();
  const { showToast } = useToast();
  const tenantId = merchantSession?.tenantId;

  const [templates, setTemplates] = useState<MerchantCouponTemplate[]>([]);
  const [memberLevels, setMemberLevels] = useState<MemberLevel[]>([]);
  const [memberTags, setMemberTags] = useState<MemberTag[]>([]);
  const [effectSummary, setEffectSummary] = useState<MarketingEffectSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<string>('ALL'); // 全部、草稿、已上线、已下线
  
  // 新建优惠券弹窗
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [name, setName] = useState('');
  const [couponType, setCouponType] = useState<'FIXED' | 'RATE'>('FIXED');
  const [thresholdAmount, setThresholdAmount] = useState<number | ''>('');
  const [discountAmount, setDiscountAmount] = useState<number | ''>('');
  const [discountRate, setDiscountRate] = useState<number | ''>('');
  const [maxDiscountAmount, setMaxDiscountAmount] = useState<number | ''>('');
  const [totalStock, setTotalStock] = useState<number>(100);
  const [perUserLimit, setPerUserLimit] = useState<number>(1);
  const [validityType, setValidityType] = useState<'RANGE' | 'DAYS'>('DAYS');
  const [validDaysAfterReceive, setValidDaysAfterReceive] = useState<number | ''>(30);
  const [validStartTime, setValidStartTime] = useState('');
  const [validEndTime, setValidEndTime] = useState('');
  const [receiveStartTime, setReceiveStartTime] = useState('');
  const [receiveEndTime, setReceiveEndTime] = useState('');
  const [description, setDescription] = useState('');
  const [stackStrategy, setStackStrategy] = useState('EXCLUSIVE');
  const [requiredMemberLevel, setRequiredMemberLevel] = useState<number | ''>('');
  const [requiredMemberTagIds, setRequiredMemberTagIds] = useState('');
  const [excludedMemberTagIds, setExcludedMemberTagIds] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 优惠券适用范围弹窗
  const [selectedTemplate, setSelectedTemplate] = useState<MerchantCouponTemplate | null>(null);
  const [scopes, setScopes] = useState<CouponScope[]>([]);
  const [isScopesLoading, setIsScopesLoading] = useState(false);
  const [newScopeType, setNewScopeType] = useState<string>('ALL'); // 全部商品、指定商品、指定分类
  const [newScopeId, setNewScopeId] = useState<number | ''>('');
  const [newScopeCode, setNewScopeCode] = useState<string>('');
  const [isAddingScope, setIsAddingScope] = useState(false);

  const tabs = [
    { id: 'ALL', label: '全部模板' },
    { id: 'DRAFT', label: '草稿' },
    { id: 'ACTIVE', label: '已上线' },
    { id: 'DISABLED', label: '已下线' },
  ];

  const loadTemplates = useCallback(async () => {
    if (!tenantId) return;
    setIsLoading(true);
    try {
      const data = await merchantMarketingService.getCouponTemplates(
        tenantId,
        activeTab === 'ALL' ? undefined : activeTab
      );
      setTemplates(data || []);
      const summary = await merchantMarketingService.getEffectSummary(tenantId);
      setEffectSummary(summary);
      const [levels, tags] = await Promise.all([
        merchantMarketingService.getMemberLevels(tenantId).catch(() => []),
        merchantMarketingService.getMemberTags(tenantId).catch(() => []),
      ]);
      setMemberLevels(levels);
      setMemberTags(tags);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '获取优惠券模板列表失败', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [tenantId, activeTab, showToast]);

  useEffect(() => {
    void loadTemplates();
  }, [loadTemplates]);

  const handleCreateTemplate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tenantId) return;

    const validationIssues = validateMerchantCouponDraft({
      name,
      couponType,
      thresholdAmount,
      discountAmount,
      discountRate,
      maxDiscountAmount,
      totalStock,
      perUserLimit,
      validityType,
      validDaysAfterReceive,
      validStartTime,
      validEndTime,
      receiveStartTime,
      receiveEndTime,
    });

    if (validationIssues.length > 0) {
      showToast(validationIssues.join('；'), 'error');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload: CouponTemplateCreatePayload = {
        name: name.trim(),
        couponType,
        thresholdAmount: thresholdAmount ? Number(thresholdAmount) : 0,
        totalStock: Number(totalStock),
        perUserLimit: Number(perUserLimit),
        description: description.trim() || undefined,
        stackStrategy: stackStrategy || undefined,
        requiredMemberLevel: requiredMemberLevel === '' ? undefined : Number(requiredMemberLevel),
        requiredMemberTagIds: normalizeTagIds(requiredMemberTagIds),
        excludedMemberTagIds: normalizeTagIds(excludedMemberTagIds),
      };

      if (couponType === 'FIXED') {
        payload.discountAmount = Number(discountAmount);
      } else {
        payload.discountRate = Number(discountRate);
        if (maxDiscountAmount) {
          payload.maxDiscountAmount = Number(maxDiscountAmount);
        }
      }

      if (validityType === 'DAYS') {
        payload.validDaysAfterReceive = Number(validDaysAfterReceive);
      } else {
        payload.validStartTime = new Date(validStartTime).toISOString();
        payload.validEndTime = new Date(validEndTime).toISOString();
      }

      if (receiveStartTime) {
        payload.receiveStartTime = new Date(receiveStartTime).toISOString();
      }
      if (receiveEndTime) {
        payload.receiveEndTime = new Date(receiveEndTime).toISOString();
      }

      await merchantMarketingService.createCouponTemplate(tenantId, payload);
      showToast('新建优惠券模板成功', 'success');
      setIsCreateOpen(false);
      resetCreateForm();
      await loadTemplates();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '创建优惠券模板失败', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetCreateForm = () => {
    setName('');
    setCouponType('FIXED');
    setThresholdAmount('');
    setDiscountAmount('');
    setDiscountRate('');
    setMaxDiscountAmount('');
    setTotalStock(100);
    setPerUserLimit(1);
    setValidityType('DAYS');
    setValidDaysAfterReceive(30);
    setValidStartTime('');
    setValidEndTime('');
    setReceiveStartTime('');
    setReceiveEndTime('');
    setDescription('');
    setStackStrategy('EXCLUSIVE');
    setRequiredMemberLevel('');
    setRequiredMemberTagIds('');
    setExcludedMemberTagIds('');
  };

  const handleActivate = async (id: number) => {
    if (!tenantId) return;
    try {
      await merchantMarketingService.activateCoupon(tenantId, id);
      showToast('优惠券已成功上线发布', 'success');
      await loadTemplates();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '上线失败', 'error');
    }
  };

  const handleDisable = async (id: number) => {
    if (!tenantId) return;
    try {
      await merchantMarketingService.disableCoupon(tenantId, id);
      showToast('优惠券模板已下线禁用', 'success');
      await loadTemplates();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '下线失败', 'error');
    }
  };

  const handleOpenScopes = async (template: MerchantCouponTemplate) => {
    if (!tenantId) return;
    setSelectedTemplate(template);
    setIsScopesLoading(true);
    setScopes([]);
    try {
      const data = await merchantMarketingService.getCouponScopes(tenantId, template.id);
      setScopes(data || []);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '获取适用范围失败', 'error');
    } finally {
      setIsScopesLoading(false);
    }
  };

  const handleAddScope = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tenantId || !selectedTemplate) return;

    if (newScopeType === 'PRODUCT' && !newScopeId) {
      showToast('请输入适用的商品 ID', 'error');
      return;
    }

    if (newScopeType === 'CATEGORY' && !newScopeCode) {
      showToast('请输入适用的商品分类编码', 'error');
      return;
    }

    setIsAddingScope(true);
    try {
      const payload: CouponScopeCreatePayload = {
        scopeType: newScopeType,
      };
      if (newScopeType === 'PRODUCT') {
        payload.scopeId = Number(newScopeId);
      } else if (newScopeType === 'CATEGORY') {
        payload.scopeCode = newScopeCode.trim();
      }

      await merchantMarketingService.addCouponScope(tenantId, selectedTemplate.id, payload);
      showToast('添加适用范围成功', 'success');
      setNewScopeId('');
      setNewScopeCode('');
      
      // 重新加载适用范围
      const data = await merchantMarketingService.getCouponScopes(tenantId, selectedTemplate.id);
      setScopes(data || []);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '添加适用范围失败', 'error');
    } finally {
      setIsAddingScope(false);
    }
  };

  return (
    <div className="flex flex-col gap-6 p-6 pb-20">
      <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-black tracking-tight text-slate-900">优惠券模板管理</h1>
          <p className="text-sm font-medium text-slate-500">
            创建及维护店铺优惠券。你可以设置不同的满减或折扣规则，以及限定商品的适用范围。
          </p>
        </div>
        <button
          onClick={() => setIsCreateOpen(true)}
          className="flex items-center justify-center gap-2 rounded-2xl bg-primary px-5 py-3 text-sm font-bold text-white shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
        >
          <Plus size={16} />
          新建优惠券
        </button>
      </header>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <EffectMetric label="模板数" value={effectSummary?.templateCount ?? templates.length} />
        <EffectMetric label="领取数" value={effectSummary?.receivedCount ?? 0} />
        <EffectMetric label="使用数" value={effectSummary?.usedCount ?? 0} />
        <EffectMetric label="核销率" value={`${Math.round((effectSummary?.writeOffRate ?? 0) * 100)}%`} />
        <EffectMetric label="活动数" value={effectSummary?.activityCount ?? 0} />
        <EffectMetric label="生效活动" value={effectSummary?.activeActivityCount ?? 0} />
        <EffectMetric label="活动优惠" value={`¥${Number(effectSummary?.activityDiscountAmount ?? 0).toFixed(2)}`} />
      </div>

      {/* 状态页签 */}
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
      ) : templates.length === 0 ? (
        <div className="flex min-h-[40vh] flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white py-12 text-slate-400">
          <Ticket className="mb-4 h-12 w-12 text-slate-300" />
          <p className="font-bold">暂无优惠券模板数据</p>
        </div>
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {templates.map((tpl) => (
            <motion.article
              key={tpl.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className={cn(
                "flex flex-col overflow-hidden rounded-[32px] border bg-white shadow-sm transition-all hover:shadow-md",
                tpl.status === 'ACTIVE' ? 'border-orange-100' : 'border-slate-100'
              )}
            >
              <div className={cn(
                "flex items-center justify-between border-b px-5 py-4",
                tpl.status === 'ACTIVE' ? 'bg-orange-50/50 border-orange-100/50' : 'bg-slate-50/50 border-slate-50'
              )}>
                <div className="flex items-center gap-2">
                  <Ticket className={tpl.status === 'ACTIVE' ? 'text-orange-500' : 'text-slate-400'} size={18} />
                  <span className="font-bold text-slate-800 text-sm">{tpl.name}</span>
                </div>
                <span className={`rounded-full px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider ${
                  tpl.status === 'DRAFT' ? 'bg-blue-50 text-blue-600 border border-blue-100' :
                  tpl.status === 'ACTIVE' ? 'bg-orange-50 text-orange-600 border border-orange-100' :
                  'bg-slate-100 text-slate-500 border border-slate-200'
                }`}>
                  {tpl.status === 'DRAFT' ? '草稿' : tpl.status === 'ACTIVE' ? '已上线' : '已下线'}
                </span>
              </div>

              <div className="flex-1 p-5 space-y-4">
                <div className="flex items-baseline justify-between">
                  <span className="text-3xl font-black tracking-tight text-slate-900">
                    {tpl.couponType === 'FIXED' ? (
                      <>
                        <span className="text-sm font-bold text-slate-500 mr-0.5">¥</span>
                        {tpl.discountAmount}
                      </>
                    ) : (
                      <>{(tpl.discountRate ?? 1) * 10}折</>
                    )}
                  </span>
                  <span className="text-xs font-bold text-slate-500">
                    满 {tpl.thresholdAmount ? `¥${tpl.thresholdAmount}` : '无门槛'} 可用
                  </span>
                </div>

                {tpl.couponType === 'RATE' && tpl.maxDiscountAmount && (
                  <p className="text-xs font-semibold text-slate-400">
                    折扣上限：¥{tpl.maxDiscountAmount}
                  </p>
                )}

                <div className="space-y-2 text-xs font-medium text-slate-500 border-t border-slate-50 pt-3">
                  <div className="flex justify-between">
                    <span>限额/领用</span>
                    <span className="font-bold text-slate-700">限 {tpl.perUserLimit} 张 / 总 {tpl.totalStock} 张</span>
                  </div>
                  <div className="flex justify-between">
                    <span>领取/使用</span>
                    <span className="font-bold text-slate-700">{tpl.receivedQuantity ?? 0} / {tpl.usedQuantity ?? 0}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>核销率/库存</span>
                    <span className="font-bold text-slate-700">{formatWriteOffRate(tpl)} / 剩 {resolveRemainingStock(tpl)}</span>
                  </div>
                  <div className="flex justify-between items-start">
                    <span>有效期</span>
                    <span className="font-bold text-slate-700 text-right">
                      {tpl.validDaysAfterReceive 
                        ? `领券后 ${tpl.validDaysAfterReceive} 天内有效`
                        : tpl.validStartTime
                          ? `${new Date(tpl.validStartTime).toLocaleDateString()} ~ ${new Date(tpl.validEndTime || '').toLocaleDateString()}`
                          : '无限制'}
                    </span>
                  </div>
                  {tpl.description && (
                    <p className="bg-slate-50 p-2 rounded-xl text-slate-400 mt-2 font-normal">
                      {tpl.description}
                    </p>
                  )}
                  {formatMemberRestriction(tpl) && (
                    <p className="rounded-xl border border-blue-100 bg-blue-50 p-2 text-xs font-bold text-blue-700">
                      {formatMemberRestriction(tpl)}
                    </p>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-2 border-t border-slate-50 bg-slate-50/20 p-4">
                <button
                  onClick={() => handleOpenScopes(tpl)}
                  className="flex-1 flex items-center justify-center gap-1.5 rounded-xl border border-slate-200 bg-white py-2 text-xs font-bold text-slate-600 transition-colors hover:bg-slate-50"
                >
                  <Settings size={13} />
                  适用范围
                </button>
                {tpl.status === 'DRAFT' && (
                  <button
                    onClick={() => handleActivate(tpl.id)}
                    className="flex-1 flex items-center justify-center gap-1.5 rounded-xl bg-slate-900 py-2 text-xs font-bold text-white transition-opacity hover:opacity-90"
                  >
                    <CheckCircle size={13} />
                    上线发布
                  </button>
                )}
                {tpl.status === 'ACTIVE' && (
                  <button
                    onClick={() => handleDisable(tpl.id)}
                    className="flex-1 flex items-center justify-center gap-1.5 rounded-xl border border-red-200 bg-red-50 py-2 text-xs font-bold text-red-600 transition-colors hover:bg-red-100"
                  >
                    <PowerOff size={13} />
                    下线停用
                  </button>
                )}
              </div>
            </motion.article>
          ))}
        </div>
      )}

      {/* 新建优惠券弹窗 */}
      <AnimatePresence>
        {isCreateOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm overflow-y-auto">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="my-8 w-full max-w-lg overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4">
                <h3 className="text-lg font-black text-slate-900">新建优惠券模板</h3>
                <button onClick={() => setIsCreateOpen(false)} className="rounded-xl p-1 text-slate-400 hover:bg-slate-200/50 hover:text-slate-600">
                  <X size={18} />
                </button>
              </div>

              <form onSubmit={handleCreateTemplate} className="max-h-[70vh] overflow-y-auto p-6 space-y-5">
                {/* 优惠券名称 */}
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="name" className="text-xs font-bold text-slate-700">优惠券名称</label>
                  <input
                    id="name"
                    type="text"
                    placeholder="如：年中大促10元满减券"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>

                {/* 优惠类型 */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-700">优惠类型</label>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { value: 'FIXED', label: '满减券 (固定面额)' },
                      { value: 'RATE', label: '折扣券 (打折比例)' }
                    ].map((item) => (
                      <button
                        key={item.value}
                        type="button"
                        onClick={() => setCouponType(item.value as 'FIXED' | 'RATE')}
                        className={`rounded-2xl border-2 py-2.5 text-xs font-bold transition-all ${
                          couponType === item.value
                            ? 'border-primary bg-white text-primary ring-4 ring-primary/5'
                            : 'border-slate-100 bg-slate-50/50 text-slate-500'
                        }`}
                      >
                        {item.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  {/* 门槛金额 */}
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="threshold" className="text-xs font-bold text-slate-700">门槛金额 (元)</label>
                    <input
                      id="threshold"
                      type="number"
                      min="0"
                      step="0.01"
                      placeholder="0=无门槛"
                      value={thresholdAmount}
                      onChange={(e) => setThresholdAmount(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    />
                  </div>

                  {/* 优惠面值 */}
                  {couponType === 'FIXED' ? (
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="discount" className="text-xs font-bold text-slate-700">满减面值 (元)</label>
                      <input
                        id="discount"
                        type="number"
                        min="0.01"
                        step="0.01"
                        placeholder="减免金额"
                        value={discountAmount}
                        onChange={(e) => setDiscountAmount(e.target.value ? Number(e.target.value) : '')}
                        className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                        required={couponType === 'FIXED'}
                      />
                    </div>
                  ) : (
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="rate" className="text-xs font-bold text-slate-700">折扣比例 (如0.8=8折)</label>
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
                        required={couponType === 'RATE'}
                      />
                    </div>
                  )}
                </div>

                {couponType === 'RATE' && (
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="maxDiscount" className="text-xs font-bold text-slate-700">折扣封顶金额 (元，选填)</label>
                    <input
                      id="maxDiscount"
                      type="number"
                      min="0.1"
                      step="0.01"
                      placeholder="不设置则无封顶"
                      value={maxDiscountAmount}
                      onChange={(e) => setMaxDiscountAmount(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    />
                  </div>
                )}

                <div className="grid grid-cols-2 gap-4">
                  {/* 发行库存 */}
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="stock" className="text-xs font-bold text-slate-700">发行总量 (张)</label>
                    <input
                      id="stock"
                      type="number"
                      min="1"
                      placeholder="总量"
                      value={totalStock}
                      onChange={(e) => setTotalStock(Number(e.target.value))}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>

                  {/* 每人限领 */}
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="limit" className="text-xs font-bold text-slate-700">每人限领 (张)</label>
                    <input
                      id="limit"
                      type="number"
                      min="1"
                      value={perUserLimit}
                      onChange={(e) => setPerUserLimit(Number(e.target.value))}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>
                </div>

                {/* 有效期类型 */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs font-bold text-slate-700">有效期类型</label>
                  <div className="grid grid-cols-2 gap-3">
                    {[
                      { value: 'DAYS', label: '相对有效期 (领券后生效)' },
                      { value: 'RANGE', label: '固定时间段' }
                    ].map((item) => (
                      <button
                        key={item.value}
                        type="button"
                        onClick={() => setValidityType(item.value as 'RANGE' | 'DAYS')}
                        className={`rounded-2xl border-2 py-2 text-xs font-bold transition-all ${
                          validityType === item.value
                            ? 'border-primary bg-white text-primary ring-4 ring-primary/5'
                            : 'border-slate-100 bg-slate-50/50 text-slate-500'
                        }`}
                      >
                        {item.label}
                      </button>
                    ))}
                  </div>
                </div>

                {validityType === 'DAYS' ? (
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="validDays" className="text-xs font-bold text-slate-700">领取后有效天数 (天)</label>
                    <input
                      id="validDays"
                      type="number"
                      min="1"
                      placeholder="例如：30"
                      value={validDaysAfterReceive}
                      onChange={(e) => setValidDaysAfterReceive(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required={validityType === 'DAYS'}
                    />
                  </div>
                ) : (
                  <div className="grid grid-cols-2 gap-4">
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="validStart" className="text-xs font-bold text-slate-700">有效期开始</label>
                      <input
                        id="validStart"
                        type="datetime-local"
                        value={validStartTime}
                        onChange={(e) => setValidStartTime(e.target.value)}
                        className="w-full rounded-2xl border border-slate-200 px-4 py-2 text-xs font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                        required={validityType === 'RANGE'}
                      />
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="validEnd" className="text-xs font-bold text-slate-700">有效期结束</label>
                      <input
                        id="validEnd"
                        type="datetime-local"
                        value={validEndTime}
                        onChange={(e) => setValidEndTime(e.target.value)}
                        className="w-full rounded-2xl border border-slate-200 px-4 py-2 text-xs font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                        required={validityType === 'RANGE'}
                      />
                    </div>
                  </div>
                )}

                {/* 可领取时间 */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="receiveStart" className="text-xs font-bold text-slate-700">可领取开始 (选填)</label>
                    <input
                      id="receiveStart"
                      type="datetime-local"
                      value={receiveStartTime}
                      onChange={(e) => setReceiveStartTime(e.target.value)}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2 text-xs font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="receiveEnd" className="text-xs font-bold text-slate-700">可领取结束 (选填)</label>
                    <input
                      id="receiveEnd"
                      type="datetime-local"
                      value={receiveEndTime}
                      onChange={(e) => setReceiveEndTime(e.target.value)}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2 text-xs font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    />
                  </div>
                </div>

                {/* 使用说明 */}
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="description" className="text-xs font-bold text-slate-700">使用说明 (选填)</label>
                  <textarea
                    id="description"
                    rows={2}
                    placeholder="如：仅用于指定商品或分类..."
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="w-full rounded-2xl border border-slate-200 p-3 text-sm font-medium outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                  />
                </div>

                <div className="grid grid-cols-1 gap-4 rounded-2xl border border-slate-100 bg-slate-50/60 p-4">
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="requiredLevel" className="text-xs font-bold text-slate-700">最低会员等级 (选填)</label>
                    <select
                      id="requiredLevel"
                      value={requiredMemberLevel}
                      onChange={(e) => setRequiredMemberLevel(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    >
                      <option value="">不限会员等级</option>
                      {memberLevels.map((level) => (
                        <option key={level.id} value={level.level}>
                          LV.{level.level} {level.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <TagIdInput
                    id="requiredTags"
                    label="必备会员标签 (选填)"
                    value={requiredMemberTagIds}
                    onChange={setRequiredMemberTagIds}
                    tags={memberTags}
                  />
                  <TagIdInput
                    id="excludedTags"
                    label="排除会员标签 (选填)"
                    value={excludedMemberTagIds}
                    onChange={setExcludedMemberTagIds}
                    tags={memberTags}
                  />
                </div>

                <div className="flex items-center justify-end gap-3 border-t border-slate-50 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setIsCreateOpen(false);
                      resetCreateForm();
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

      {/* 适用范围弹窗 */}
      <AnimatePresence>
        {selectedTemplate && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="w-full max-w-md overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4">
                <div>
                  <h3 className="text-lg font-black text-slate-900">适用范围配置</h3>
                  <p className="text-xs font-bold text-slate-400">设置优惠券的商品与品类限制</p>
                </div>
                <button onClick={() => setSelectedTemplate(null)} className="rounded-xl p-1 text-slate-400 hover:bg-slate-200/50 hover:text-slate-600">
                  <X size={18} />
                </button>
              </div>

              <div className="p-6 space-y-6">
                {/* 已配置范围列表 */}
                <div>
                  <h4 className="mb-2.5 text-xs font-black uppercase tracking-widest text-slate-400">已配置规则</h4>
                  {isScopesLoading ? (
                    <div className="py-6 text-center text-xs text-slate-400 font-bold">获取配置中...</div>
                  ) : scopes.length === 0 ? (
                    <div className="rounded-2xl bg-slate-50 p-4 text-center text-xs font-semibold text-slate-400">
                      默认适用于全部商品。添加规则以限制适用范围。
                    </div>
                  ) : (
                    <div className="flex flex-col gap-2">
                      {scopes.map((sc) => (
                        <div key={sc.id} className="flex items-center justify-between rounded-xl bg-slate-50 px-4 py-2.5 text-xs font-bold text-slate-700 border border-slate-100">
                          <span className="flex items-center gap-2">
                            {sc.scopeType === 'ALL' && <Layers size={14} className="text-blue-500" />}
                            {sc.scopeType === 'PRODUCT' && <ShoppingBag size={14} className="text-orange-500" />}
                            {sc.scopeType === 'CATEGORY' && <FolderTree size={14} className="text-green-500" />}
                            
                            {sc.scopeType === 'ALL' && '全场通用'}
                            {sc.scopeType === 'PRODUCT' && `限制商品 ID: ${sc.scopeId}`}
                            {sc.scopeType === 'CATEGORY' && `限制分类编码: ${sc.scopeCode}`}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* 添加范围表单 */}
                <form onSubmit={handleAddScope} className="border-t border-slate-50 pt-5 space-y-4">
                  <h4 className="text-xs font-black uppercase tracking-widest text-slate-400">添加限制规则</h4>
                  
                  <div className="flex flex-col gap-1.5">
                    <label className="text-[10px] font-black uppercase tracking-wider text-slate-400">规则类型</label>
                    <div className="grid grid-cols-3 gap-2">
                      {[
                        { value: 'ALL', label: '全部商品' },
                        { value: 'PRODUCT', label: '按商品ID' },
                        { value: 'CATEGORY', label: '按分类' }
                      ].map((item) => (
                        <button
                          key={item.value}
                          type="button"
                          onClick={() => setNewScopeType(item.value)}
                          className={`rounded-xl border py-2 text-xs font-bold transition-all ${
                            newScopeType === item.value
                              ? 'border-primary bg-primary/5 text-primary'
                              : 'border-slate-100 bg-slate-50/50 text-slate-500'
                          }`}
                        >
                          {item.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  {newScopeType === 'PRODUCT' && (
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="scopeId" className="text-[10px] font-black uppercase tracking-wider text-slate-400">适用商品 ID</label>
                      <input
                        id="scopeId"
                        type="number"
                        placeholder="商品ID，如：12"
                        value={newScopeId}
                        onChange={(e) => setNewScopeId(e.target.value ? Number(e.target.value) : '')}
                        className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                        required
                      />
                    </div>
                  )}

                  {newScopeType === 'CATEGORY' && (
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="scopeCode" className="text-[10px] font-black uppercase tracking-wider text-slate-400">适用分类编码</label>
                      <input
                        id="scopeCode"
                        type="text"
                        placeholder="如：category_shoes"
                        value={newScopeCode}
                        onChange={(e) => setNewScopeCode(e.target.value)}
                        className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                        required
                      />
                    </div>
                  )}

                  <button
                    type="submit"
                    disabled={isAddingScope}
                    className="w-full rounded-xl bg-slate-900 py-2.5 text-xs font-bold text-white transition-opacity hover:opacity-90"
                  >
                    {isAddingScope ? '添加中...' : '确认添加'}
                  </button>
                </form>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}

function EffectMetric({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-[24px] border border-slate-100 bg-white p-5 shadow-sm">
      <div className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</div>
      <div className="mt-2 text-2xl font-black text-slate-900">{value}</div>
    </div>
  );
}

function formatWriteOffRate(template: MerchantCouponTemplate) {
  const received = template.receivedQuantity ?? 0;
  const used = template.usedQuantity ?? 0;
  if (received <= 0) {
    return '0%';
  }
  return `${Math.round((used / received) * 100)}%`;
}

function resolveRemainingStock(template: MerchantCouponTemplate) {
  const total = template.totalStock ?? 0;
  if (total <= 0) {
    return '不限';
  }
  return Math.max(total - (template.receivedQuantity ?? 0), 0);
}

function formatMemberRestriction(template: MerchantCouponTemplate) {
  const parts: string[] = [];
  if (template.requiredMemberLevel) {
    parts.push(`LV.${template.requiredMemberLevel} 及以上`);
  }
  if (template.requiredMemberTagIds) {
    parts.push(`需标签 ${template.requiredMemberTagIds}`);
  }
  if (template.excludedMemberTagIds) {
    parts.push(`排除标签 ${template.excludedMemberTagIds}`);
  }
  return parts.length > 0 ? `会员限制：${parts.join('；')}` : '';
}

function normalizeTagIds(value: string) {
  const normalized = value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .join(',');
  return normalized || undefined;
}

function TagIdInput({
  id,
  label,
  value,
  onChange,
  tags,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  tags: MemberTag[];
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-xs font-bold text-slate-700">{label}</label>
      <input
        id={id}
        type="text"
        placeholder={tags.length > 0 ? tags.map((tag) => `${tag.id}:${tag.name}`).join('，') : '输入标签 ID，多个用逗号分隔'}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
      />
    </div>
  );
}
