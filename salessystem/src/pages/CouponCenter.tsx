import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Ticket, ArrowLeft, Store, Clock } from 'lucide-react';
import { EmptyState } from '../components/ui/EmptyState';
import { appCatalogService } from '../services/modules/appCatalog';
import { appCouponService } from '../services/modules/appCoupon';
import type { Tenant } from '../types/catalog';
import type { CouponTemplate, UserCoupon } from '../types/coupon';
import { useToast } from '../context/ToastContext';
import { cn } from '../lib/utils';

export default function CouponCenter() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { showToast } = useToast();

  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [activeTenant, setActiveTenant] = useState<Tenant | null>(null);
  
  const [activeTab, setActiveTab] = useState<'available' | 'my' | 'expired'>('available');
  const [availableCoupons, setAvailableCoupons] = useState<CouponTemplate[]>([]);
  const [myCoupons, setMyCoupons] = useState<UserCoupon[]>([]);
  const [expiredCoupons, setExpiredCoupons] = useState<UserCoupon[]>([]);
  
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState<number | null>(null);

  // Load tenant list
  useEffect(() => {
    let isMounted = true;
    async function loadTenants() {
      try {
        const list = await appCatalogService.listTenants();
        if (!isMounted) return;
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
        }
      } catch (e) {
        if (isMounted) {
          showToast('获取商户列表失败', 'error');
        }
      }
    }
    void loadTenants();
    return () => {
      isMounted = false;
    };
  }, [searchParams]);

  // Load coupon data for active tenant
  useEffect(() => {
    if (!activeTenant) return undefined;

    let isMounted = true;
    async function loadCoupons() {
      setIsLoading(true);
      try {
        const [available, usable, used, expired] = await Promise.all([
          appCouponService.getAvailableCoupons(activeTenant!.id),
          appCouponService.getMyCoupons(activeTenant!.id, 'USABLE'),
          appCouponService.getMyCoupons(activeTenant!.id, 'USED'),
          appCouponService.getMyCoupons(activeTenant!.id, 'EXPIRED'),
        ]);

        if (!isMounted) return;
        setAvailableCoupons(available);
        setMyCoupons(usable);
        
        // Combine USED and EXPIRED into expiredCoupons tab
        setExpiredCoupons([...used, ...expired]);
      } catch (e) {
        if (isMounted) {
          showToast('获取优惠券列表失败', 'error');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadCoupons();
    return () => {
      isMounted = false;
    };
  }, [activeTenant]);

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
      const errMsg = e instanceof Error ? e.message : '领取失败，请稍后重试';
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
                  myCoupons.map((coupon) => (
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
                              可使用
                            </span>
                            <span className="text-[10px] font-bold text-slate-400">
                              券码: {coupon.couponNo}
                            </span>
                          </div>
                          <h3 className="text-base font-extrabold text-slate-800 dark:text-white mt-2">
                            {coupon.name}
                          </h3>
                        </div>

                        <div className="flex justify-between items-center border-t border-slate-50 dark:border-slate-800/50 pt-3">
                          <span className="text-[11px] font-semibold text-slate-400 flex items-center gap-1">
                            <Clock className="w-3.5 h-3.5" />
                            有效期至 {formatDate(coupon.expireTime)}
                          </span>
                          <button
                            onClick={() => navigate('/')}
                            className="px-5 py-2 rounded-full text-xs font-black tracking-wide bg-primary text-white shadow-sm hover:opacity-95 active:scale-95 transition-all"
                          >
                            去使用
                          </button>
                        </div>
                      </div>
                    </div>
                  ))
                )
              )}

              {/* EXPIRED & USED COUPONS TAB */}
              {activeTab === 'expired' && (
                expiredCoupons.length === 0 ? (
                  <EmptyState icon={<Ticket className="w-12 h-12 text-slate-300" />} title="暂无失效记录" subtitle="您的卡包非常健康，没有已失效或已使用的券哦！" />
                ) : (
                  expiredCoupons.map((coupon) => (
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
                              {coupon.status === 'USED' ? '已使用' : '已过期'}
                            </span>
                            <span className="text-[10px] font-bold text-slate-400">
                              券码: {coupon.couponNo}
                            </span>
                          </div>
                          <h3 className="text-base font-extrabold text-slate-400 mt-2">
                            {coupon.name}
                          </h3>
                        </div>

                        <div className="flex justify-between items-center border-t border-slate-50 dark:border-slate-800/50 pt-3">
                          <span className="text-[11px] font-semibold text-slate-400">
                            {coupon.status === 'USED'
                              ? `使用时间: ${formatDate(coupon.usedTime)}`
                              : `过期时间: ${formatDate(coupon.expireTime)}`}
                          </span>
                          <button
                            disabled
                            className="px-5 py-2 rounded-full text-xs font-black tracking-wide bg-slate-100 text-slate-400 cursor-not-allowed border"
                          >
                            已失效
                          </button>
                        </div>
                      </div>
                    </div>
                  ))
                )
              )}
            </motion.div>
          </AnimatePresence>
        )}
      </div>
    </div>
  );
}
