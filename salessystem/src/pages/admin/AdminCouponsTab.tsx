import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Ticket,
  Plus,
  Settings,
  X,
  Layers,
  ShoppingBag,
  FolderTree,
  CheckCircle,
  PowerOff
} from 'lucide-react';
import { useToast } from '../../context/ToastContext';
import { adminMarketingService } from '../../services/modules/adminMarketing';
import type { MerchantCouponTemplate, CouponScope, CouponTemplateCreatePayload, CouponScopeCreatePayload } from '../../types/marketing';
import { formatCurrency } from '../../utils/display';
import { cn } from '../../lib/utils';

interface AdminCouponsTabProps {
  statusFilter: string;
}

export default function AdminCouponsTab({ statusFilter }: AdminCouponsTabProps) {
  const { showToast } = useToast();
  const [coupons, setCoupons] = useState<MerchantCouponTemplate[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Create Coupon Modal
  const [isCouponCreateOpen, setIsCouponCreateOpen] = useState(false);
  const [couponName, setCouponName] = useState('');
  const [couponType, setCouponType] = useState<'FIXED' | 'RATE'>('FIXED');
  const [thresholdAmount, setThresholdAmount] = useState<number | ''>('');
  const [discountAmount, setDiscountAmount] = useState<number | ''>('');
  const [discountRate, setDiscountRate] = useState<number | ''>('');
  const [maxDiscountAmount, setMaxDiscountAmount] = useState<number | ''>('');
  const [totalStock, setTotalStock] = useState<number>(1000);
  const [perUserLimit, setPerUserLimit] = useState<number>(1);
  const [validityType, setValidityType] = useState<'RANGE' | 'DAYS'>('DAYS');
  const [validDaysAfterReceive, setValidDaysAfterReceive] = useState<number | ''>(30);
  const [validStartTime, setValidStartTime] = useState('');
  const [validEndTime, setValidEndTime] = useState('');
  const [receiveStartTime, setReceiveStartTime] = useState('');
  const [receiveEndTime, setReceiveEndTime] = useState('');
  const [couponDescription, setCouponDescription] = useState('');
  const [stackStrategy, setStackStrategy] = useState('NONE');
  const [isCouponSubmitting, setIsCouponSubmitting] = useState(false);

  // Coupon Scope Modal
  const [selectedCoupon, setSelectedCoupon] = useState<MerchantCouponTemplate | null>(null);
  const [scopes, setScopes] = useState<CouponScope[]>([]);
  const [isScopesLoading, setIsScopesLoading] = useState(false);
  const [newScopeType, setNewScopeType] = useState<string>('ALL'); // ALL, PRODUCT, CATEGORY
  const [newScopeId, setNewScopeId] = useState<number | ''>('');
  const [newScopeCode, setNewScopeCode] = useState<string>('');
  const [isAddingScope, setIsAddingScope] = useState(false);

  const loadCoupons = async () => {
    setIsLoading(true);
    try {
      const status = statusFilter === 'ALL' ? undefined : statusFilter;
      const data = await adminMarketingService.getCouponTemplates(status);
      setCoupons(data || []);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '加载平台优惠券失败', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadCoupons();
  }, [statusFilter]);

  const handleActivateCoupon = async (id: number) => {
    try {
      await adminMarketingService.activateCoupon(id);
      showToast('平台优惠券已成功上线', 'success');
      await loadCoupons();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '上线失败', 'error');
    }
  };

  const handleDisableCoupon = async (id: number) => {
    try {
      await adminMarketingService.disableCoupon(id);
      showToast('平台优惠券已成功下线', 'success');
      await loadCoupons();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '下线失败', 'error');
    }
  };

  const handleCreateCoupon = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!couponName.trim()) {
      showToast('请输入优惠券名称', 'error');
      return;
    }
    if (couponType === 'FIXED' && !discountAmount) {
      showToast('请输入满减面额', 'error');
      return;
    }
    if (couponType === 'RATE' && !discountRate) {
      showToast('请输入折扣比例', 'error');
      return;
    }

    setIsCouponSubmitting(true);
    try {
      const payload: CouponTemplateCreatePayload = {
        name: couponName.trim(),
        couponType,
        thresholdAmount: thresholdAmount ? Number(thresholdAmount) : 0,
        totalStock: Number(totalStock),
        perUserLimit: Number(perUserLimit),
        description: couponDescription.trim() || undefined,
        stackStrategy: stackStrategy || undefined,
        ownerType: 'PLATFORM',
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
        if (!validStartTime || !validEndTime) {
          showToast('请选择有效期开始与结束时间', 'error');
          setIsCouponSubmitting(false);
          return;
        }
        payload.validStartTime = new Date(validStartTime).toISOString();
        payload.validEndTime = new Date(validEndTime).toISOString();
      }

      if (receiveStartTime) payload.receiveStartTime = new Date(receiveStartTime).toISOString();
      if (receiveEndTime) payload.receiveEndTime = new Date(receiveEndTime).toISOString();

      await adminMarketingService.createCouponTemplate(payload);
      showToast('新建平台优惠券成功', 'success');
      setIsCouponCreateOpen(false);
      resetCouponForm();
      await loadCoupons();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '创建优惠券失败', 'error');
    } finally {
      setIsCouponSubmitting(false);
    }
  };

  const resetCouponForm = () => {
    setCouponName('');
    setCouponType('FIXED');
    setThresholdAmount('');
    setDiscountAmount('');
    setDiscountRate('');
    setMaxDiscountAmount('');
    setTotalStock(1000);
    setPerUserLimit(1);
    setValidityType('DAYS');
    setValidDaysAfterReceive(30);
    setValidStartTime('');
    setValidEndTime('');
    setReceiveStartTime('');
    setReceiveEndTime('');
    setCouponDescription('');
    setStackStrategy('NONE');
  };

  const handleOpenScopes = async (coupon: MerchantCouponTemplate) => {
    setSelectedCoupon(coupon);
    setIsScopesLoading(true);
    setScopes([]);
    try {
      const data = await adminMarketingService.getCouponScopes(coupon.id);
      setScopes(data || []);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '获取适用范围失败', 'error');
    } finally {
      setIsScopesLoading(false);
    }
  };

  const handleAddScope = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCoupon) return;

    if (newScopeType === 'PRODUCT' && !newScopeId) {
      showToast('请输入适用的商品 ID', 'error');
      return;
    }
    if (newScopeType === 'CATEGORY' && !newScopeCode) {
      showToast('请输入分类编码', 'error');
      return;
    }

    setIsAddingScope(true);
    try {
      const payload: CouponScopeCreatePayload = {
        scopeType: newScopeType,
      };
      if (newScopeType === 'PRODUCT') payload.scopeId = Number(newScopeId);
      if (newScopeType === 'CATEGORY') payload.scopeCode = newScopeCode.trim();

      await adminMarketingService.addCouponScope(selectedCoupon.id, payload);
      showToast('添加适用范围成功', 'success');
      setNewScopeId('');
      setNewScopeCode('');

      // Reload
      const data = await adminMarketingService.getCouponScopes(selectedCoupon.id);
      setScopes(data || []);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '添加适用范围失败', 'error');
    } finally {
      setIsAddingScope(false);
    }
  };

  return (
    <>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-bold text-slate-800">平台优惠券列表</h2>
        <button
          onClick={() => setIsCouponCreateOpen(true)}
          className="flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2.5 text-xs font-bold text-white shadow-md shadow-primary/10 hover:opacity-95"
        >
          <Plus size={14} />
          新建平台优惠券
        </button>
      </div>

      {isLoading ? (
        <div className="flex min-h-[30vh] items-center justify-center text-slate-400">加载数据中...</div>
      ) : coupons.length === 0 ? (
        <div className="flex min-h-[30vh] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white py-12 text-slate-400">
          <Ticket className="mb-2 h-10 w-10 text-slate-300" />
          <p className="text-xs font-bold">暂无平台优惠券模板</p>
        </div>
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {coupons.map((tpl) => (
            <motion.article
              key={tpl.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className={cn(
                "flex flex-col overflow-hidden rounded-[24px] border bg-white shadow-sm transition-all hover:shadow-md",
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
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-black uppercase tracking-wider ${
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
                    <span>发行量 / 每人限领</span>
                    <span className="font-bold text-slate-700">{tpl.totalStock} 张 / 限 {tpl.perUserLimit} 张</span>
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
                    onClick={() => handleActivateCoupon(tpl.id)}
                    className="flex-1 flex items-center justify-center gap-1.5 rounded-xl bg-slate-900 py-2 text-xs font-bold text-white transition-opacity hover:opacity-90"
                  >
                    <CheckCircle size={13} />
                    上线发布
                  </button>
                )}
                {tpl.status === 'ACTIVE' && (
                  <button
                    onClick={() => handleDisableCoupon(tpl.id)}
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

      {/* Coupon Modal */}
      <AnimatePresence>
        {isCouponCreateOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm overflow-y-auto">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="my-8 w-full max-w-lg overflow-hidden rounded-[24px] border border-slate-100 bg-white shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4">
                <h3 className="text-lg font-black text-slate-900">平台通用优惠券</h3>
                <button onClick={() => setIsCouponCreateOpen(false)} className="rounded-xl p-1 text-slate-400 hover:bg-slate-200/50 hover:text-slate-600">
                  <X size={18} />
                </button>
              </div>

              <form onSubmit={handleCreateCoupon} className="max-h-[70vh] overflow-y-auto p-6 space-y-5">
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="copName" className="text-xs font-bold text-slate-700">优惠券名称</label>
                  <input
                    id="copName"
                    type="text"
                    placeholder="如：全场通用端午礼金"
                    value={couponName}
                    onChange={(e) => setCouponName(e.target.value)}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none"
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col gap-1.5">
                    <label className="text-xs font-bold text-slate-700">优惠券类型</label>
                    <select
                      value={couponType}
                      onChange={(e) => setCouponType(e.target.value as 'FIXED' | 'RATE')}
                      className="w-full rounded-2xl border border-slate-200 bg-white p-2.5 text-sm font-semibold outline-none"
                    >
                      <option value="FIXED">满减券</option>
                      <option value="RATE">折扣券</option>
                    </select>
                  </div>

                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="threshold" className="text-xs font-bold text-slate-700">使用门槛金额 (元)</label>
                    <input
                      id="threshold"
                      type="number"
                      min="0"
                      step="0.01"
                      placeholder="0=无门槛"
                      value={thresholdAmount}
                      onChange={(e) => setThresholdAmount(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  {couponType === 'FIXED' ? (
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="amt" className="text-xs font-bold text-slate-700">满减面值 (元)</label>
                      <input
                        id="amt"
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={discountAmount}
                        onChange={(e) => setDiscountAmount(e.target.value ? Number(e.target.value) : '')}
                        className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none"
                        required
                      />
                    </div>
                  ) : (
                    <div className="flex flex-col gap-1.5">
                      <label htmlFor="rate" className="text-xs font-bold text-slate-700">折扣比例 (如0.9=9折)</label>
                      <input
                        id="rate"
                        type="number"
                        min="0.01"
                        max="0.99"
                        step="0.01"
                        value={discountRate}
                        onChange={(e) => setDiscountRate(e.target.value ? Number(e.target.value) : '')}
                        className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none"
                        required
                      />
                    </div>
                  )}

                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="stock" className="text-xs font-bold text-slate-700">发行总量 (张)</label>
                    <input
                      id="stock"
                      type="number"
                      min="1"
                      value={totalStock}
                      onChange={(e) => setTotalStock(Number(e.target.value))}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none"
                      required
                    />
                  </div>
                </div>

                <div className="flex flex-col gap-1.5">
                  <label htmlFor="validDays" className="text-xs font-bold text-slate-700">有效期（领取后天数）</label>
                  <input
                    id="validDays"
                    type="number"
                    min="1"
                    value={validDaysAfterReceive}
                    onChange={(e) => setValidDaysAfterReceive(e.target.value ? Number(e.target.value) : '')}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none"
                    required
                  />
                </div>

                <div className="flex items-center justify-end gap-3 border-t border-slate-50 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setIsCouponCreateOpen(false);
                      resetCouponForm();
                    }}
                    className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-bold text-slate-500 hover:bg-slate-50"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    disabled={isCouponSubmitting}
                    className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white transition-opacity hover:opacity-90"
                  >
                    {isCouponSubmitting ? '提交中...' : '提交'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Scope modal */}
      <AnimatePresence>
        {selectedCoupon && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="w-full max-w-md overflow-hidden rounded-[24px] border border-slate-100 bg-white shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4">
                <div>
                  <h3 className="text-lg font-black text-slate-900">平台优惠券商品范围</h3>
                  <p className="text-xs font-bold text-slate-400">券名：{selectedCoupon.name}</p>
                </div>
                <button onClick={() => setSelectedCoupon(null)} className="rounded-xl p-1 text-slate-400 hover:bg-slate-200/50 hover:text-slate-600">
                  <X size={18} />
                </button>
              </div>

              <div className="p-6 space-y-6">
                <div>
                  <h4 className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">已配置范围</h4>
                  {isScopesLoading ? (
                    <div className="text-center text-xs font-bold text-slate-400 py-3">加载中...</div>
                  ) : scopes.length === 0 ? (
                    <p className="text-xs font-semibold text-slate-400 text-center py-2 bg-slate-50 rounded-xl">默认适用于全部商品</p>
                  ) : (
                    <div className="flex flex-col gap-2">
                      {scopes.map((s) => (
                        <div key={s.id} className="flex justify-between items-center bg-slate-50 p-2.5 rounded-xl text-xs font-bold text-slate-700 border border-slate-100">
                          <span>
                            {s.scopeType === 'ALL' && '全场商品'}
                            {s.scopeType === 'PRODUCT' && `商品 ID: ${s.scopeId}`}
                            {s.scopeType === 'CATEGORY' && `品类编码: ${s.scopeCode}`}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <form onSubmit={handleAddScope} className="border-t border-slate-50 pt-4 space-y-4">
                  <h4 className="text-xs font-black uppercase tracking-widest text-slate-400">新增适用范围限制</h4>
                  <div className="flex flex-col gap-2">
                    <div className="grid grid-cols-3 gap-2">
                      {['ALL', 'PRODUCT', 'CATEGORY'].map((t) => (
                        <button
                          key={t}
                          type="button"
                          onClick={() => setNewScopeType(t)}
                          className={cn(
                            "py-2 rounded-xl text-xs font-bold border transition-all",
                            newScopeType === t ? "border-primary bg-primary/5 text-primary" : "border-slate-100 bg-slate-50/50 text-slate-500"
                          )}
                        >
                          {t === 'ALL' ? '全部商品' : t === 'PRODUCT' ? '按商品ID' : '按品类'}
                        </button>
                      ))}
                    </div>
                  </div>

                  {newScopeType === 'PRODUCT' && (
                    <div className="flex flex-col gap-1">
                      <label htmlFor="scpId" className="text-[10px] font-black text-slate-400 uppercase tracking-widest">商品 ID</label>
                      <input
                        id="scpId"
                        type="number"
                        placeholder="商品 ID"
                        value={newScopeId}
                        onChange={(e) => setNewScopeId(e.target.value ? Number(e.target.value) : '')}
                        className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs font-semibold outline-none"
                        required
                      />
                    </div>
                  )}

                  {newScopeType === 'CATEGORY' && (
                    <div className="flex flex-col gap-1">
                      <label htmlFor="scpCode" className="text-[10px] font-black text-slate-400 uppercase tracking-widest">分类编码</label>
                      <input
                        id="scpCode"
                        type="text"
                        placeholder="分类编码"
                        value={newScopeCode}
                        onChange={(e) => setNewScopeCode(e.target.value)}
                        className="w-full rounded-xl border border-slate-200 px-3 py-2 text-xs font-semibold outline-none"
                        required
                      />
                    </div>
                  )}

                  <button
                    type="submit"
                    disabled={isAddingScope}
                    className="w-full rounded-xl bg-slate-900 py-2.5 text-xs font-bold text-white hover:opacity-90"
                  >
                    添加适用范围
                  </button>
                </form>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </>
  );
}
