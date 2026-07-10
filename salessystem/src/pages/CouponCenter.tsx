import { useCallback, useEffect, useMemo, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AlertCircle, Ticket, ArrowLeft, Store, Clock, RefreshCw } from 'lucide-react';
import { EmptyState } from '../components/ui/EmptyState';
import { appCatalogService } from '../services/modules/appCatalog';
import { appCouponService } from '../services/modules/appCoupon';
import type { Tenant } from '../types/catalog';
import type { CouponTemplate, CouponTimelineEvent, UserCoupon } from '../types/coupon';
import { useToast } from '../context/ToastContext';
import { cn } from '../lib/utils';
import { getCouponTracePresentation } from '../utils/assetTracePresentation';
import { getErrorMessage } from '../utils/errorMessage';

export default function CouponCenter() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { showToast } = useToast();

  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [activeTenant, setActiveTenant] = useState<Tenant | null>(null);
  
  const [activeTab, setActiveTab] = useState<'available' | 'my' | 'expired'>('available');
  const [expandedTimelineCouponId, setExpandedTimelineCouponId] = useState<number | null>(null);
  const [availableCoupons, setAvailableCoupons] = useState<CouponTemplate[]>([]);
  const [myCoupons, setMyCoupons] = useState<UserCoupon[]>([]);
  const [expiredCoupons, setExpiredCoupons] = useState<UserCoupon[]>([]);
  
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Load tenant list
  const loadTenants = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const list = await appCatalogService.listTenants();
      setTenants(list);

      // Determine active tenant from URL or default to first
      const urlTenantId = searchParams.get('tenantId');
      if (urlTenantId) {
        const tenant = list.find((t) => t.id === Number(urlTenantId));
        if (tenant) {
          setActiveTenant(tenant);
          return;
        }
      }
      if (list.length > 0) {
        setActiveTenant(list[0]);
        return;
      }

      setActiveTenant(null);
      setIsLoading(false);
    } catch (e) {
      const message = getErrorMessage(e, '获取商户列表失败');
      setError(message);
      setTenants([]);
      setActiveTenant(null);
      setAvailableCoupons([]);
      setMyCoupons([]);
      setExpiredCoupons([]);
      setIsLoading(false);
      showToast(message, 'error');
    }
  }, [searchParams, showToast]);

  useEffect(() => {
    void loadTenants();
  }, [loadTenants]);

  // Load coupon data for active tenant
  const loadCoupons = useCallback(async (tenant: Tenant) => {
    setIsLoading(true);
    setError(null);
    try {
      const [available, usable, used, expired] = await Promise.all([
        appCouponService.getAvailableCoupons(tenant.id),
        appCouponService.getMyCoupons(tenant.id, 'USABLE'),
        appCouponService.getMyCoupons(tenant.id, 'USED'),
        appCouponService.getMyCoupons(tenant.id, 'EXPIRED'),
      ]);

      setAvailableCoupons(available);
      setMyCoupons(usable);

      // Combine USED and EXPIRED into expiredCoupons tab
      setExpiredCoupons([...used, ...expired]);
    } catch (e) {
      const message = getErrorMessage(e, '获取优惠券列表失败');
      setError(message);
      setAvailableCoupons([]);
      setMyCoupons([]);
      setExpiredCoupons([]);
      showToast(message, 'error');
    } finally {
      setIsLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    if (!activeTenant) return;
    void loadCoupons(activeTenant);
  }, [activeTenant, loadCoupons]);

  const handleRetry = () => {
    if (activeTenant) {
      void loadCoupons(activeTenant);
      return;
    }
    void loadTenants();
  };

  const handleClaim = async (coupon: CouponTemplate) => {
    if (!activeTenant) return;
    setIsSubmitting(coupon.id);
    try {
      await appCouponService.claimCoupon(activeTenant.id, coupon.id);
      showToast('优惠券领取成功！', 'success');
      
      // Refresh coupons
      const [available, usable, used, expired] = await Promise.all([
        appCouponService.getAvailableCoupons(activeTenant.id),
        appCouponService.getMyCoupons(activeTenant.id, 'USABLE'),
        appCouponService.getMyCoupons(activeTenant.id, 'USED'),
        appCouponService.getMyCoupons(activeTenant.id, 'EXPIRED'),
      ]);

      setAvailableCoupons(available);
      setMyCoupons(usable);
      setExpiredCoupons([...used, ...expired]);
      
      // Automatically switch to My Coupons tab
      setActiveTab('my');
    } catch (e: unknown) {
      const errMsg = getErrorMessage(e, '领取失败，请稍后重试');
      showToast(errMsg, 'error');
    } finally {
      setIsSubmitting(null);
    }
  };

  const formatDate = (isoString: string | null | undefined) => {
    if (!isoString) return '';
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return isoString.split('T')[0] || '';
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '.');
  };

  const formatAmount = (value: number | null | undefined) => {
    if (value === null || value === undefined) return '';
    return Number(value).toFixed(0);
  };

  const couponTimeline = useMemo(() => {
    return [...myCoupons, ...expiredCoupons]
      .flatMap((coupon) => (coupon.timeline ?? []).map((event) => ({ coupon, event })))
      .filter(({ event }) => event.occurredAt)
      .sort((left, right) => new Date(right.event.occurredAt || '').getTime() - new Date(left.event.occurredAt || '').getTime());
  }, [myCoupons, expiredCoupons]);

  const couponTimelineSummary = useMemo(() => ({
    totalEvents: couponTimeline.length,
    locked: couponTimeline.filter(({ event }) => event.eventType === 'LOCK').length,
    released: couponTimeline.filter(({ event }) => event.eventType === 'RELEASE').length,
    writeOff: couponTimeline.filter(({ event }) => event.eventType === 'WRITE_OFF').length,
  }), [couponTimeline]);

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 pb-12 md:mt-8">
      {/* Top Header */}
      <header className="flex items-center justify-between border-b border-slate-100 pb-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="p-2 text-slate-600 hover:bg-slate-50 dark:hover:bg-slate-800 rounded-full transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-black text-slate-900 dark:text-white">优惠券中心</h1>
            <p className="text-xs font-semibold text-slate-400 mt-0.5">
              {activeTenant ? `当前店铺：${activeTenant.name}` : '正在获取店铺...'}
            </p>
          </div>
        </div>
      </header>

      {error && (
        <div className="flex flex-col gap-4 rounded-3xl border border-red-100 bg-red-50 px-6 py-5 text-red-700 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <AlertCircle className="h-5 w-5 flex-none" />
            <span className="text-sm font-bold">{error}</span>
          </div>
          <button
            type="button"
            onClick={handleRetry}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-white px-4 py-2 text-sm font-black text-red-700 shadow-sm transition-all hover:bg-red-100"
          >
            <RefreshCw className="h-4 w-4" />
            重试
          </button>
        </div>
      )}

      {/* Multi-Tenant / Shop selector dropdown */}
      {tenants.length > 1 && (
        <section className="flex flex-col gap-2 rounded-2xl bg-slate-50 dark:bg-slate-900/50 p-4 border border-slate-100">
          <span className="text-[10px] font-black uppercase tracking-widest text-slate-400 flex items-center gap-1.5">
            <Store className="w-3.5 h-3.5" />
            切换商户店铺
          </span>
          <div className="flex flex-wrap gap-2 mt-1">
            {tenants.map((t) => (
              <button
                key={t.id}
                onClick={() => setActiveTenant(t)}
                className={cn(
                  'px-4 py-2 rounded-xl text-xs font-bold transition-all border',
                  activeTenant?.id === t.id
                    ? 'bg-primary text-white border-primary shadow-md shadow-primary/10'
                    : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
                )}
              >
                {t.name}
              </button>
            ))}
          </div>
        </section>
      )}

      {!isLoading && couponTimeline.length > 0 && (
        <section className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-lg font-black text-slate-900">优惠券使用时间线</h2>
              <p className="mt-1 text-xs font-bold text-slate-400">
                共 {couponTimelineSummary.totalEvents} 条事件 · 锁定 {couponTimelineSummary.locked} · 释放 {couponTimelineSummary.released} · 核销 {couponTimelineSummary.writeOff}
              </p>
            </div>
            <button
              type="button"
              onClick={() => setActiveTab('expired')}
              className="self-start rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-black text-slate-600 transition-all hover:border-primary/30 hover:bg-primary/5 hover:text-primary sm:self-auto"
            >
              查看失效记录
            </button>
          </div>

          <div className="mt-4 grid gap-3">
            {couponTimeline.slice(0, 5).map(({ coupon, event }, index) => (
              <div
                key={`${coupon.id}-${event.eventType}-${event.occurredAt}-${index}`}
                className="flex gap-3 rounded-xl border border-slate-100 bg-slate-50/70 px-4 py-3"
              >
                <div className={cn('mt-0.5 flex h-8 w-8 flex-none items-center justify-center rounded-full text-xs font-black', timelineToneClass(event.eventType))}>
                  {timelineInitial(event.eventType)}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                    <p className="truncate text-sm font-black text-slate-800">
                      {event.title} · {coupon.name}
                    </p>
                    <span className="text-xs font-bold text-slate-400">{formatDateTime(event.occurredAt)}</span>
                  </div>
                  <p className="mt-1 text-xs font-semibold text-slate-500">
                    {event.description || '优惠券状态已更新'}
                    {event.orderNo ? ` · 订单 ${event.orderNo}` : ''}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Tabs Switcher */}
      <div className="flex border-b border-slate-200">
        {[
          { key: 'available', label: '可领取', count: availableCoupons.length },
          { key: 'my', label: '我的券', count: myCoupons.length },
          { key: 'expired', label: '已失效', count: expiredCoupons.length },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key as 'available' | 'my' | 'expired')}
            className={cn(
              'flex-1 text-center py-3.5 text-sm font-bold border-b-2 transition-all relative',
              activeTab === tab.key
                ? 'border-primary text-primary font-extrabold'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            )}
          >
            {tab.label}
            {tab.count > 0 && (
              <span className="ml-1.5 px-2 py-0.5 text-[10px] rounded-full bg-slate-100 text-slate-600 font-bold">
                {tab.count}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Main Content Area */}
      <div className="mt-4 min-h-[300px]">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-20 gap-3 text-slate-400">
            <div className="w-8 h-8 border-2 border-primary/20 border-t-primary rounded-full animate-spin" />
            <span className="text-sm font-medium">获取优惠券数据中...</span>
          </div>
        ) : (
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.15 }}
              className="flex flex-col gap-4"
            >
              {/* AVAILABLE TAB */}
              {activeTab === 'available' && (
                availableCoupons.length === 0 ? (
                  <EmptyState icon={<Ticket className="w-12 h-12" />} title="没有可领取的优惠券" subtitle="店铺最近暂未发放新券，过阵子再来看看吧~" />
                ) : (
                  availableCoupons.map((coupon) => {
                    const isOutOfStock = coupon.remainingStock <= 0;
                    const isLimitReached = coupon.receivedByCurrentUser >= coupon.perUserLimit;
                    const canClaim = coupon.receivable && !isOutOfStock && !isLimitReached;

                    return (
                      <div
                        key={coupon.id}
                        className={cn(
                          'relative bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-sm overflow-hidden flex flex-row transition-all duration-300 hover:shadow-md',
                          (!canClaim || isSubmitting === coupon.id) && 'opacity-75'
                        )}
                      >
                        {/* Left Side: Value & Conditions */}
                        <div className="w-28 sm:w-32 bg-primary/5 dark:bg-primary/10 flex flex-col justify-center items-center p-4 border-r border-dashed border-slate-100 dark:border-slate-800 relative">
                          <div className="flex items-baseline text-primary font-black">
                            {coupon.couponType === 'FIXED' ? (
                              <>
                                <span className="text-sm sm:text-base mr-0.5">¥</span>
                                <span className="text-3xl sm:text-4xl">{formatAmount(coupon.discountAmount)}</span>
                              </>
                            ) : (
                              <>
                                <span className="text-3xl sm:text-4xl">{((coupon.discountRate || 1) * 10).toFixed(1)}</span>
                                <span className="text-xs sm:text-sm ml-0.5">折</span>
                              </>
                            )}
                          </div>
                          <span className="text-[10px] font-bold text-slate-400 mt-1.5 text-center">
                            {coupon.thresholdAmount > 0 ? `满 ${coupon.thresholdAmount} 元可用` : '无门槛'}
                          </span>
                          
                          {/* Ticket edge decorative circles */}
                          <div className="absolute top-1/2 -translate-y-1/2 -left-2 w-4 h-4 bg-slate-50 dark:bg-slate-950 rounded-full border border-slate-100 dark:border-slate-800" />
                          <div className="absolute top-1/2 -translate-y-1/2 -right-2 w-4 h-4 bg-slate-50 dark:bg-slate-950 rounded-full border border-slate-100 dark:border-slate-800" />
                        </div>

                        {/* Right Side: Details & Actions */}
                        <div className="flex-1 p-5 flex flex-col justify-between gap-4">
                          <div>
                            <div className="flex justify-between items-start gap-2">
                              <span className="px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider rounded-md bg-slate-100 text-slate-500">
                                {coupon.ownerType === 'PLATFORM' ? '平台通用' : '商户专属'}
                              </span>
                              <span className="text-[10px] font-bold text-slate-400">
                                {coupon.perUserLimit > 1 ? `限领 ${coupon.perUserLimit} 张` : '限领 1 张'}
                              </span>
                            </div>
                            <h3 className="text-base font-extrabold text-slate-800 dark:text-white mt-2">
                              {coupon.name}
                            </h3>
                            <p className="text-xs font-semibold text-slate-400 mt-1">
                              有效期: {formatDate(coupon.validStartTime || coupon.receiveStartTime)} - {formatDate(coupon.validEndTime || coupon.receiveEndTime)}
                            </p>
                          </div>

                          <div className="flex justify-between items-center border-t border-slate-50 dark:border-slate-800/50 pt-3">
                            <span className="text-[10px] font-bold text-slate-400">
                              剩 {coupon.remainingStock} 张
                            </span>
                            
                            <button
                              disabled={!canClaim || isSubmitting !== null}
                              onClick={() => handleClaim(coupon)}
                              className={cn(
                                'px-5 py-2 rounded-full text-xs font-black tracking-wide shadow-sm transition-all duration-200 active:scale-95',
                                canClaim
                                  ? 'bg-primary text-white hover:bg-primary/95'
                                  : 'bg-slate-100 text-slate-400 cursor-not-allowed'
                              )}
                            >
                              {isSubmitting === coupon.id ? (
                                <div className="w-4 h-4 border-2 border-slate-300 border-t-slate-600 rounded-full animate-spin" />
                              ) : isOutOfStock ? (
                                '已领完'
                              ) : isLimitReached ? (
                                '已领取'
                              ) : (
                                '立即领取'
                              )}
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })
                )
              )}

              {/* MY ACTIVE COUPONS TAB */}
              {activeTab === 'my' && (
                myCoupons.length === 0 ? (
                  <EmptyState icon={<Ticket className="w-12 h-12 text-slate-300" />} title="暂无可用的优惠券" subtitle="赶快去“可领取”页面挑几张心仪的礼券吧~" />
                ) : (
                  myCoupons.map((coupon) => {
                    const trace = getCouponTracePresentation(coupon);
                    const isTimelineExpanded = expandedTimelineCouponId === coupon.id;
                    return (
                      <div
                        key={coupon.id}
                        className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-sm overflow-hidden flex flex-row"
                      >
                      {/* Left: Value & Conditions */}
                      <div className="w-28 sm:w-32 bg-primary/5 dark:bg-primary/10 flex flex-col justify-center items-center p-4 border-r border-dashed border-slate-100 dark:border-slate-800 relative">
                        <div className="flex items-baseline text-primary font-black">
                          {coupon.couponType === 'FIXED' ? (
                            <>
                              <span className="text-sm sm:text-base mr-0.5">¥</span>
                              <span className="text-3xl sm:text-4xl">{formatAmount(coupon.discountAmount)}</span>
                            </>
                          ) : (
                            <>
                              <span className="text-3xl sm:text-4xl">{((coupon.discountRate || 1) * 10).toFixed(1)}</span>
                              <span className="text-xs sm:text-sm ml-0.5">折</span>
                            </>
                          )}
                        </div>
                        <span className="text-[10px] font-bold text-slate-400 mt-1.5 text-center">
                          {coupon.thresholdAmount > 0 ? `满 ${coupon.thresholdAmount} 元可用` : '无门槛'}
                        </span>
                        <div className="absolute top-1/2 -translate-y-1/2 -left-2 w-4 h-4 bg-slate-50 dark:bg-slate-950 rounded-full border border-slate-100 dark:border-slate-800" />
                        <div className="absolute top-1/2 -translate-y-1/2 -right-2 w-4 h-4 bg-slate-50 dark:bg-slate-950 rounded-full border border-slate-100 dark:border-slate-800" />
                      </div>

                      {/* Right: Info */}
                      <div className="flex-1 p-5 flex flex-col justify-between gap-4">
                        <div>
                          <div className="flex justify-between items-start">
                            <span className="px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider rounded-md bg-green-50 text-green-600 border border-green-100">
                              {trace.status}
                            </span>
                            <span className="text-[10px] font-bold text-slate-400">
                              券码: {coupon.couponNo}
                            </span>
                          </div>
                          <h3 className="text-base font-extrabold text-slate-800 dark:text-white mt-2">
                            {coupon.name}
                          </h3>
                          <p className="mt-1 text-[11px] font-semibold text-slate-400">
                            {trace.source}
                          </p>
                        </div>

                        <div className="flex justify-between items-center border-t border-slate-50 dark:border-slate-800/50 pt-3">
                          <span className="text-[11px] font-semibold text-slate-400 flex items-center gap-1">
                            <Clock className="w-3.5 h-3.5" />
                            {trace.hint}
                          </span>
                          <div className="flex flex-wrap justify-end gap-2">
                            {(coupon.timeline?.length ?? 0) > 0 && (
                              <button
                                type="button"
                                onClick={() => setExpandedTimelineCouponId(isTimelineExpanded ? null : coupon.id)}
                                className="rounded-full border border-slate-200 px-4 py-2 text-xs font-black tracking-wide text-slate-600 transition-all hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                              >
                                {isTimelineExpanded ? '收起时间线' : '查看时间线'}
                              </button>
                            )}
                            <button
                              onClick={() => navigate(trace.actionPath ?? '/')}
                              className="px-5 py-2 rounded-full text-xs font-black tracking-wide bg-primary text-white shadow-sm hover:opacity-95 active:scale-95 transition-all"
                            >
                              {trace.actionLabel}
                            </button>
                          </div>
                        </div>
                        {isTimelineExpanded && (
                          <CouponTimelineDetails events={coupon.timeline ?? []} />
                        )}
                      </div>
                      </div>
                    );
                  })
                )
              )}

              {/* EXPIRED & USED COUPONS TAB */}
              {activeTab === 'expired' && (
                expiredCoupons.length === 0 ? (
                  <EmptyState icon={<Ticket className="w-12 h-12 text-slate-300" />} title="暂无失效记录" subtitle="您的卡包非常健康，没有已失效或已使用的券哦！" />
                ) : (
                  expiredCoupons.map((coupon) => {
                    const trace = getCouponTracePresentation(coupon);
                    const isTimelineExpanded = expandedTimelineCouponId === coupon.id;
                    return (
                      <div
                        key={coupon.id}
                        className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-100 dark:border-slate-800 shadow-sm overflow-hidden flex flex-row opacity-60 grayscale"
                      >
                      {/* Left: Value & Conditions */}
                      <div className="w-28 sm:w-32 bg-slate-100 dark:bg-slate-800 flex flex-col justify-center items-center p-4 border-r border-dashed border-slate-200 dark:border-slate-700 relative">
                        <div className="flex items-baseline text-slate-400 font-black">
                          {coupon.couponType === 'FIXED' ? (
                            <>
                              <span className="text-sm sm:text-base mr-0.5">¥</span>
                              <span className="text-3xl sm:text-4xl">{formatAmount(coupon.discountAmount)}</span>
                            </>
                          ) : (
                            <>
                              <span className="text-3xl sm:text-4xl">{((coupon.discountRate || 1) * 10).toFixed(1)}</span>
                              <span className="text-xs sm:text-sm ml-0.5">折</span>
                            </>
                          )}
                        </div>
                        <span className="text-[10px] font-bold text-slate-400 mt-1.5 text-center">
                          {coupon.thresholdAmount > 0 ? `满 ${coupon.thresholdAmount} 元可用` : '无门槛'}
                        </span>
                        <div className="absolute top-1/2 -translate-y-1/2 -left-2 w-4 h-4 bg-slate-50 dark:bg-slate-950 rounded-full border border-slate-200 dark:border-slate-700" />
                        <div className="absolute top-1/2 -translate-y-1/2 -right-2 w-4 h-4 bg-slate-50 dark:bg-slate-950 rounded-full border border-slate-200 dark:border-slate-700" />
                      </div>

                      {/* Right: Info */}
                      <div className="flex-1 p-5 flex flex-col justify-between gap-4">
                        <div>
                          <div className="flex justify-between items-start">
                            <span className={cn(
                              "px-2.5 py-0.5 text-[10px] font-black uppercase tracking-wider rounded-md border",
                              coupon.status === 'USED'
                                ? 'bg-blue-50 text-blue-400 border-blue-100'
                                : 'bg-red-50 text-red-400 border-red-100'
                            )}>
                              {trace.status}
                            </span>
                            <span className="text-[10px] font-bold text-slate-400">
                              券码: {coupon.couponNo}
                            </span>
                          </div>
                          <h3 className="text-base font-extrabold text-slate-400 mt-2">
                            {coupon.name}
                          </h3>
                          <p className="mt-1 text-[11px] font-semibold text-slate-400">
                            {trace.source}
                          </p>
                        </div>

                        <div className="flex justify-between items-center border-t border-slate-50 dark:border-slate-800/50 pt-3">
                          <span className="text-[11px] font-semibold text-slate-400">
                            {trace.hint}
                          </span>
                          <div className="flex flex-wrap justify-end gap-2">
                            {(coupon.timeline?.length ?? 0) > 0 && (
                              <button
                                type="button"
                                onClick={() => setExpandedTimelineCouponId(isTimelineExpanded ? null : coupon.id)}
                                className="rounded-full border border-slate-200 bg-white px-4 py-2 text-xs font-black tracking-wide text-slate-600 transition-all hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                              >
                                {isTimelineExpanded ? '收起时间线' : '查看时间线'}
                              </button>
                            )}
                            {trace.actionPath ? (
                              <button
                                type="button"
                                onClick={() => navigate(trace.actionPath!)}
                                className="px-5 py-2 rounded-full text-xs font-black tracking-wide bg-white text-primary border border-primary/20 shadow-sm hover:bg-primary/5 active:scale-95 transition-all"
                              >
                                {trace.actionLabel}
                              </button>
                            ) : (
                              <button
                                disabled
                                className="px-5 py-2 rounded-full text-xs font-black tracking-wide bg-slate-100 text-slate-400 cursor-not-allowed border"
                              >
                                {trace.inactiveActionLabel ?? trace.status ?? '已失效'}
                              </button>
                            )}
                          </div>
                        </div>
                        {isTimelineExpanded && (
                          <CouponTimelineDetails events={coupon.timeline ?? []} />
                        )}
                      </div>
                      </div>
                    );
                  })
                )
              )}
            </motion.div>
          </AnimatePresence>
        )}
      </div>
    </div>
  );
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ').slice(0, 16);
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).replace(/\//g, '.');
}

function CouponTimelineDetails({ events }: { events: CouponTimelineEvent[] }) {
  const orderedEvents = [...events]
    .filter((event) => event.occurredAt)
    .sort((left, right) => new Date(left.occurredAt || '').getTime() - new Date(right.occurredAt || '').getTime());

  if (orderedEvents.length === 0) {
    return null;
  }

  return (
    <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-4">
      <div className="mb-3 text-xs font-black uppercase tracking-widest text-slate-400">生命周期明细</div>
      <div className="space-y-3">
        {orderedEvents.map((event, index) => (
          <div key={`${event.eventType}-${event.occurredAt}-${index}`} className="flex gap-3">
            <div className={cn('flex h-7 w-7 flex-none items-center justify-center rounded-full text-[10px] font-black', timelineToneClass(event.eventType))}>
              {timelineInitial(event.eventType)}
            </div>
            <div className="min-w-0 flex-1 border-b border-slate-100 pb-3 last:border-b-0 last:pb-0">
              <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                <span className="text-xs font-black text-slate-800">{event.title}</span>
                <span className="text-[11px] font-bold text-slate-400">{formatDateTime(event.occurredAt)}</span>
              </div>
              <p className="mt-1 text-xs font-semibold text-slate-500">{event.description || '优惠券状态已更新'}</p>
              {(event.orderNo || event.bizNo) && (
                <p className="mt-1 text-[11px] font-bold text-slate-400">
                  {event.orderNo ? `订单 ${event.orderNo}` : ''}
                  {event.orderNo && event.bizNo ? ' · ' : ''}
                  {event.bizNo ? `流水 ${event.bizNo}` : ''}
                </p>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function timelineInitial(eventType: string) {
  if (eventType === 'WRITE_OFF') return '核';
  if (eventType === 'RELEASE') return '释';
  if (eventType === 'LOCK') return '锁';
  if (eventType === 'EXPIRE') return '期';
  return '领';
}

function timelineToneClass(eventType: string) {
  if (eventType === 'WRITE_OFF') return 'bg-blue-50 text-blue-600';
  if (eventType === 'RELEASE') return 'bg-amber-50 text-amber-600';
  if (eventType === 'LOCK') return 'bg-violet-50 text-violet-600';
  if (eventType === 'EXPIRE') return 'bg-red-50 text-red-600';
  return 'bg-green-50 text-green-600';
}
