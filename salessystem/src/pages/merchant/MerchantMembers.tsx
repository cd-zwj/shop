import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Users,
  Plus,
  Crown,
  Tag,
  X,
  AlertCircle,
  TrendingUp,
  Percent,
  CheckCircle2,
  Trash2
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { merchantMarketingService } from '../../services/modules/merchantMarketing';
import type { MemberLevel, MemberTag } from '../../types/marketing';
import { formatCurrency } from '../../utils/display';
import { cn } from '../../lib/utils';

export default function MerchantMembers() {
  const { merchantSession } = useAuth();
  const { showToast } = useToast();
  const tenantId = merchantSession?.tenantId;

  const [activeTab, setActiveTab] = useState<'LEVELS' | 'TAGS'>('LEVELS');
  const [levels, setLevels] = useState<MemberLevel[]>([]);
  const [tags, setTags] = useState<MemberTag[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Level Modal
  const [isLevelOpen, setIsLevelOpen] = useState(false);
  const [levelVal, setLevelVal] = useState<number | ''>('');
  const [levelName, setLevelName] = useState('');
  const [thresholdAmount, setThresholdAmount] = useState<number | ''>('');
  const [discountRate, setDiscountRate] = useState<number | ''>('');
  const [isLevelSubmitting, setIsLevelSubmitting] = useState(false);

  // Tag Modal
  const [isTagOpen, setIsTagOpen] = useState(false);
  const [tagName, setTagName] = useState('');
  const [isTagSubmitting, setIsTagSubmitting] = useState(false);

  const loadData = async () => {
    if (!tenantId) return;
    setIsLoading(true);
    try {
      if (activeTab === 'LEVELS') {
        const data = await merchantMarketingService.getMemberLevels(tenantId);
        setLevels(data || []);
      } else {
        const data = await merchantMarketingService.getMemberTags(tenantId);
        setTags(data || []);
      }
    } catch (err) {
      showToast(err instanceof Error ? err.message : '加载数据失败', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, [tenantId, activeTab]);

  const handleCreateLevel = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tenantId) return;

    if (levelVal === '' || !levelName.trim() || thresholdAmount === '' || discountRate === '') {
      showToast('请完整填写等级信息', 'error');
      return;
    }

    if (Number(discountRate) <= 0 || Number(discountRate) > 1) {
      showToast('折扣率应在 0 到 1 之间（如 0.9 代表九折）', 'error');
      return;
    }

    setIsLevelSubmitting(true);
    try {
      await merchantMarketingService.createMemberLevel(tenantId, {
        level: Number(levelVal),
        name: levelName.trim(),
        thresholdAmount: Number(thresholdAmount),
        discountRate: Number(discountRate),
      });
      showToast('等级创建成功', 'success');
      setIsLevelOpen(false);
      resetLevelForm();
      await loadData();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '创建会员等级失败', 'error');
    } finally {
      setIsLevelSubmitting(false);
    }
  };

  const resetLevelForm = () => {
    setLevelVal('');
    setLevelName('');
    setThresholdAmount('');
    setDiscountRate('');
  };

  const handleCreateTag = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!tenantId) return;

    if (!tagName.trim()) {
      showToast('请输入标签名称', 'error');
      return;
    }

    setIsTagSubmitting(true);
    try {
      await merchantMarketingService.createMemberTag(tenantId, tagName.trim());
      showToast('标签创建成功', 'success');
      setIsTagOpen(false);
      setTagName('');
      await loadData();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '创建会员标签失败', 'error');
    } finally {
      setIsTagSubmitting(false);
    }
  };

  return (
    <div className="flex flex-col gap-6 p-6 pb-20">
      <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-black tracking-tight text-slate-900">会员及客户管理</h1>
          <p className="text-sm font-medium text-slate-500">
            自定义客户的成长等级及会员标签，用于后续实施定向优惠策略。
          </p>
        </div>
        <button
          onClick={() => {
            if (activeTab === 'LEVELS') {
              setIsLevelOpen(true);
            } else {
              setIsTagOpen(true);
            }
          }}
          className="flex items-center justify-center gap-2 rounded-2xl bg-primary px-5 py-3 text-sm font-bold text-white shadow-xl shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
        >
          <Plus size={16} />
          {activeTab === 'LEVELS' ? '新建会员等级' : '新建会员标签'}
        </button>
      </header>

      {/* Tabs */}
      <div className="flex border-b border-slate-100 overflow-x-auto hide-scrollbar">
        <button
          onClick={() => setActiveTab('LEVELS')}
          className={`flex items-center gap-2 whitespace-nowrap px-6 pb-4 text-sm font-bold border-b-2 transition-all ${
            activeTab === 'LEVELS'
              ? 'border-primary text-primary font-black scale-[1.02]'
              : 'border-transparent text-slate-400 hover:text-slate-600'
          }`}
        >
          <Crown size={16} />
          等级管理
        </button>
        <button
          onClick={() => setActiveTab('TAGS')}
          className={`flex items-center gap-2 whitespace-nowrap px-6 pb-4 text-sm font-bold border-b-2 transition-all ${
            activeTab === 'TAGS'
              ? 'border-primary text-primary font-black scale-[1.02]'
              : 'border-transparent text-slate-400 hover:text-slate-600'
          }`}
        >
          <Tag size={16} />
          标签管理
        </button>
      </div>

      {isLoading ? (
        <div className="flex min-h-[40vh] flex-col items-center justify-center gap-2 text-slate-500">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary/30 border-t-primary" />
          <span className="text-sm font-semibold">加载数据中...</span>
        </div>
      ) : activeTab === 'LEVELS' ? (
        /* Levels View */
        levels.length === 0 ? (
          <div className="flex min-h-[40vh] flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white py-12 text-slate-400">
            <Crown className="mb-4 h-12 w-12 text-slate-300" />
            <p className="font-bold">暂无等级数据</p>
          </div>
        ) : (
          <div className="overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="bg-slate-50/50">
                    <th className="px-6 py-4 text-xs font-black uppercase tracking-widest text-slate-400">等级值</th>
                    <th className="px-6 py-4 text-xs font-black uppercase tracking-widest text-slate-400">等级名称</th>
                    <th className="px-6 py-4 text-xs font-black uppercase tracking-widest text-slate-400">消费门槛</th>
                    <th className="px-6 py-4 text-xs font-black uppercase tracking-widest text-slate-400">折扣率</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {levels.map((lvl) => (
                    <tr key={lvl.id} className="transition-colors hover:bg-slate-50/50">
                      <td className="px-6 py-4 font-mono font-bold text-slate-900">LV.{lvl.level}</td>
                      <td className="px-6 py-4">
                        <span className="inline-flex items-center gap-1.5 rounded-xl bg-orange-50 px-3 py-1 text-xs font-bold text-orange-700">
                          <Crown size={12} />
                          {lvl.name}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm font-bold text-slate-800">
                        {formatCurrency(lvl.thresholdAmount)}
                      </td>
                      <td className="px-6 py-4">
                        <span className="inline-flex items-center gap-1 text-xs font-black text-indigo-600">
                          <Percent size={12} />
                          {lvl.discountRate === 1 ? '不打折' : `${lvl.discountRate * 10} 折`}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )
      ) : (
        /* Tags View */
        tags.length === 0 ? (
          <div className="flex min-h-[40vh] flex-col items-center justify-center rounded-3xl border border-dashed border-slate-200 bg-white py-12 text-slate-400">
            <Tag className="mb-4 h-12 w-12 text-slate-300" />
            <p className="font-bold">暂无标签数据</p>
          </div>
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4">
            {tags.map((tag) => (
              <motion.div
                key={tag.id}
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex items-center justify-between overflow-hidden rounded-2xl border border-slate-100 bg-white p-4 shadow-sm"
              >
                <div className="flex items-center gap-3">
                  <div className="rounded-xl bg-slate-50 p-2.5 text-slate-500">
                    <Tag size={16} />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-slate-800">{tag.name}</h4>
                    <p className="text-[10px] font-bold text-slate-400 mt-0.5">
                      关联会员：{tag.memberCount ?? 0} 人
                    </p>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        )
      )}

      {/* Level Create Modal */}
      <AnimatePresence>
        {isLevelOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="w-full max-w-md overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4">
                <h3 className="text-lg font-black text-slate-900">新建会员等级</h3>
                <button onClick={() => setIsLevelOpen(false)} className="rounded-xl p-1 text-slate-400 hover:bg-slate-200/50 hover:text-slate-600">
                  <X size={18} />
                </button>
              </div>

              <form onSubmit={handleCreateLevel} className="p-6 space-y-5">
                <div className="grid grid-cols-2 gap-4">
                  {/* Level value */}
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="levelVal" className="text-xs font-bold text-slate-700">等级权重值 (数字)</label>
                    <input
                      id="levelVal"
                      type="number"
                      min="1"
                      placeholder="如：1"
                      value={levelVal}
                      onChange={(e) => setLevelVal(e.target.value ? Number(e.target.value) : '')}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>

                  {/* Level Name */}
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="levelName" className="text-xs font-bold text-slate-700">等级名称</label>
                    <input
                      id="levelName"
                      type="text"
                      placeholder="如：黄金会员"
                      value={levelName}
                      onChange={(e) => setLevelName(e.target.value)}
                      className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                      required
                    />
                  </div>
                </div>

                {/* Threshold Amount */}
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="threshold" className="text-xs font-bold text-slate-700">累计消费金额门槛 (元)</label>
                  <input
                    id="threshold"
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="消费满多少升级"
                    value={thresholdAmount}
                    onChange={(e) => setThresholdAmount(e.target.value ? Number(e.target.value) : '')}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>

                {/* Discount Rate */}
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="rate" className="text-xs font-bold text-slate-700">专享会员折扣率 (如0.9=9折，1.0=不打折)</label>
                  <input
                    id="rate"
                    type="number"
                    min="0.1"
                    max="1.0"
                    step="0.01"
                    placeholder="请输入0.1 ~ 1.0 之间的数值"
                    value={discountRate}
                    onChange={(e) => setDiscountRate(e.target.value ? Number(e.target.value) : '')}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>

                <div className="flex items-center justify-end gap-3 border-t border-slate-50 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setIsLevelOpen(false);
                      resetLevelForm();
                    }}
                    className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-bold text-slate-500 hover:bg-slate-50"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    disabled={isLevelSubmitting}
                    className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white transition-opacity hover:opacity-90 disabled:opacity-50"
                  >
                    {isLevelSubmitting ? '保存中...' : '确认新建'}
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Tag Create Modal */}
      <AnimatePresence>
        {isTagOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-sm">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="w-full max-w-md overflow-hidden rounded-[32px] border border-slate-100 bg-white shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 px-6 py-4">
                <h3 className="text-lg font-black text-slate-900">新建会员标签</h3>
                <button onClick={() => setIsTagOpen(false)} className="rounded-xl p-1 text-slate-400 hover:bg-slate-200/50 hover:text-slate-600">
                  <X size={18} />
                </button>
              </div>

              <form onSubmit={handleCreateTag} className="p-6 space-y-5">
                <div className="flex flex-col gap-1.5">
                  <label htmlFor="tagName" className="text-xs font-bold text-slate-700">标签名称</label>
                  <input
                    id="tagName"
                    type="text"
                    placeholder="如：高频消费客"
                    value={tagName}
                    onChange={(e) => setTagName(e.target.value)}
                    className="w-full rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-semibold outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                    required
                  />
                </div>

                <div className="flex items-center justify-end gap-3 border-t border-slate-50 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setIsTagOpen(false);
                      setTagName('');
                    }}
                    className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-bold text-slate-500 hover:bg-slate-50"
                  >
                    取消
                  </button>
                  <button
                    type="submit"
                    disabled={isTagSubmitting}
                    className="rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white transition-opacity hover:opacity-90 disabled:opacity-50"
                  >
                    {isTagSubmitting ? '保存中...' : '确认新建'}
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
