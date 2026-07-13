import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { motion } from 'motion/react';
import {
  AlertCircle,
  BadgeCheck,
  ChevronRight,
  CircleDollarSign,
  Coins,
  Plus,
  Receipt,
  RefreshCw,
  Star,
  Store,
  Ticket,
  TrendingUp,
  Wallet as WalletIcon,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { appWalletService } from '../services/modules/appWallet';
import type {
  AssetActivity,
  AssetActivityPage,
  AssetActivityQuery,
  AssetActivityType,
  AssetHold,
  TenantAssetSummary,
  WalletAccount,
  WalletLog,
} from '../types/wallet';
import { cn } from '../lib/utils';
import { formatCurrency } from '../utils/display';
import { getErrorMessage } from '../utils/errorMessage';
import { buildWalletRecentEntries } from '../utils/walletLogPresentation';

const ACTIVITY_PAGE_SIZE = 20;
const ACTIVITY_TYPE_OPTIONS: Array<{ value: AssetActivityType; label: string }> = [
  { value: 'WALLET', label: '钱包' },
  { value: 'POINTS', label: '积分' },
  { value: 'GROWTH', label: '成长值' },
  { value: 'COUPON', label: '优惠券' },
];

export default function UserWallet() {
  const navigate = useNavigate();
  const [wallet, setWallet] = useState<WalletAccount | null>(null);
  const [logs, setLogs] = useState<WalletLog[]>([]);
  const [tenantAssets, setTenantAssets] = useState<TenantAssetSummary[]>([]);
  const [assetActivities, setAssetActivities] = useState<AssetActivity[]>([]);
  const [assetHolds, setAssetHolds] = useState<AssetHold[]>([]);
  const [assetActivitiesUnavailable, setAssetActivitiesUnavailable] = useState(false);
  const [activityQuery, setActivityQuery] = useState<AssetActivityQuery>({ size: ACTIVITY_PAGE_SIZE });
  const [activityTimeRange, setActivityTimeRange] = useState<'ALL' | 'SEVEN_DAYS' | 'THIRTY_DAYS' | 'CUSTOM'>('ALL');
  const [customActivityFrom, setCustomActivityFrom] = useState('');
  const [customActivityTo, setCustomActivityTo] = useState('');
  const [nextActivityCursor, setNextActivityCursor] = useState<string | null>(null);
  const [hasMoreActivities, setHasMoreActivities] = useState(false);
  const [isLoadingMoreActivities, setIsLoadingMoreActivities] = useState(false);
  const [activityLoadError, setActivityLoadError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const activityRequestIdRef = useRef(0);

  const loadWalletData = useCallback(async (isActive: () => boolean = () => true) => {
    setIsLoading(true);
    setError(null);
    const initialActivityRequestId = ++activityRequestIdRef.current;
    const holdsPromise = appWalletService.listAssetHolds().catch(() => [] as AssetHold[]);

    try {
      const [walletInfo, walletLogs, merchantList, activityResult] = await Promise.all([
        appWalletService.getUnifiedWallet(),
        appWalletService.getUnifiedWalletLogs(1, 5),
        appWalletService.listTenantAssetSummaries(),
        appWalletService.listAssetActivities({ size: ACTIVITY_PAGE_SIZE })
          .then((page) => ({ page, unavailable: false }))
          .catch(() => ({ page: emptyActivityPage(), unavailable: true })),
      ]);
      if (!isActive()) return;
      setWallet(walletInfo);
      setLogs(walletLogs.records ?? []);
      setTenantAssets(merchantList);
      if (initialActivityRequestId === activityRequestIdRef.current) {
        setAssetActivities(activityResult.page.records);
        setAssetActivitiesUnavailable(activityResult.unavailable);
        setNextActivityCursor(activityResult.page.nextCursor ?? null);
        setHasMoreActivities(activityResult.page.hasMore);
        setActivityQuery({ size: ACTIVITY_PAGE_SIZE });
        setActivityTimeRange('ALL');
        setCustomActivityFrom('');
        setCustomActivityTo('');
        setActivityLoadError(null);
      }
      void holdsPromise.then((holds) => {
        if (isActive()) {
          setAssetHolds(holds);
        }
      });
    } catch (loadError) {
      if (!isActive()) return;
      setError(getErrorMessage(loadError, '钱包资产加载失败，请稍后重试'));
      setLogs([]);
      setTenantAssets([]);
      setAssetActivities([]);
      setAssetHolds([]);
      setAssetActivitiesUnavailable(false);
      setNextActivityCursor(null);
      setHasMoreActivities(false);
    } finally {
      if (isActive()) {
        setIsLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    let isMounted = true;

    void loadWalletData(() => isMounted);

    return () => {
      isMounted = false;
    };
  }, [loadWalletData]);

  const loadActivityPage = useCallback(async (query: AssetActivityQuery, append: boolean) => {
    const requestId = ++activityRequestIdRef.current;
    if (append) {
      setIsLoadingMoreActivities(true);
    } else {
      setIsLoadingMoreActivities(false);
    }
    setActivityLoadError(null);

    try {
      const page = await appWalletService.listAssetActivities(query);
      if (requestId !== activityRequestIdRef.current) {
        return;
      }
      setAssetActivities((current) => append ? [...current, ...page.records] : page.records);
      setAssetActivitiesUnavailable(false);
      setNextActivityCursor(page.nextCursor ?? null);
      setHasMoreActivities(page.hasMore);
    } catch (loadError) {
      if (requestId !== activityRequestIdRef.current) {
        return;
      }
      const message = getErrorMessage(loadError, '资产动态加载失败，请稍后重试');
      setActivityLoadError(message);
      if (!append) {
        setAssetActivities([]);
        setAssetActivitiesUnavailable(true);
        setNextActivityCursor(null);
        setHasMoreActivities(false);
      }
    } finally {
      if (append && requestId === activityRequestIdRef.current) {
        setIsLoadingMoreActivities(false);
      }
    }
  }, []);

  const resetActivityQuery = useCallback((nextQuery: AssetActivityQuery) => {
    const normalizedQuery = { ...nextQuery, cursor: undefined, size: ACTIVITY_PAGE_SIZE };
    setActivityQuery(normalizedQuery);
    void loadActivityPage(normalizedQuery, false);
  }, [loadActivityPage]);

  const toggleActivityType = useCallback((type: AssetActivityType) => {
    const currentTypes = activityQuery.types ?? [];
    const types = currentTypes.includes(type)
      ? currentTypes.filter((item) => item !== type)
      : [...currentTypes, type];
    resetActivityQuery({ ...activityQuery, types: types.length > 0 ? types : undefined });
  }, [activityQuery, resetActivityQuery]);

  const updateActivityTenant = useCallback((tenantId: number | undefined) => {
    resetActivityQuery({ ...activityQuery, tenantId });
  }, [activityQuery, resetActivityQuery]);

  const updateActivityTimeRange = useCallback((range: 'ALL' | 'SEVEN_DAYS' | 'THIRTY_DAYS' | 'CUSTOM') => {
    setActivityTimeRange(range);
    if (range === 'CUSTOM') {
      setCustomActivityFrom('');
      setCustomActivityTo('');
      resetActivityQuery({
        ...activityQuery,
        from: undefined,
        to: undefined,
      });
      return;
    }
    resetActivityQuery({
      ...activityQuery,
      ...activityRangeBounds(range, customActivityFrom, customActivityTo),
    });
  }, [activityQuery, customActivityFrom, customActivityTo, resetActivityQuery]);

  const applyCustomActivityRange = useCallback(() => {
    resetActivityQuery({
      ...activityQuery,
      ...activityRangeBounds('CUSTOM', customActivityFrom, customActivityTo),
    });
  }, [activityQuery, customActivityFrom, customActivityTo, resetActivityQuery]);

  const loadMoreActivities = useCallback(() => {
    if (!nextActivityCursor || isLoadingMoreActivities) {
      return;
    }
    void loadActivityPage({ ...activityQuery, cursor: nextActivityCursor }, true);
  }, [activityQuery, isLoadingMoreActivities, loadActivityPage, nextActivityCursor]);

  const recentDistribution = useMemo(() => {
    return buildWalletRecentEntries(logs);
  }, [logs]);

  const tenantAssetTotals = useMemo(() => {
    return tenantAssets.reduce(
      (acc, item) => ({
        points: acc.points + Number(item.points || 0),
        wallet: acc.wallet + Number(item.walletAvailableAmount || 0),
        usableCoupons: acc.usableCoupons + Number(item.usableCouponCount || 0),
        lockedCoupons: acc.lockedCoupons + Number(item.lockedCouponCount || 0),
        usedCoupons: acc.usedCoupons + Number(item.usedCouponCount || 0),
        expiredCoupons: acc.expiredCoupons + Number(item.expiredCouponCount || 0),
        expiringCoupons: acc.expiringCoupons + Number(item.expiringSoonCouponCount || 0),
        growth: acc.growth + Number(item.totalGrowth || 0),
      }),
      { points: 0, wallet: 0, usableCoupons: 0, lockedCoupons: 0, usedCoupons: 0, expiredCoupons: 0, expiringCoupons: 0, growth: 0 },
    );
  }, [tenantAssets]);

  const assetRiskItems = useMemo(() => {
    return tenantAssets.flatMap((asset) => {
      const items: Array<{ title: string; detail: string; actionLabel: string; actionPath: string }> = [];
      if (asset.expiringSoonPoints > 0) {
        items.push({
          title: '积分即将过期',
          detail: `${asset.tenantName} 有 ${asset.expiringSoonPoints.toLocaleString()} 分将在 30 天内过期`,
          actionLabel: '查看积分',
          actionPath: `/points/${asset.tenantId}`,
        });
      }
      if (Number(asset.lockedCouponCount || 0) > 0) {
        items.push({
          title: '优惠券锁定中',
          detail: `${asset.tenantName} 有 ${Number(asset.lockedCouponCount).toLocaleString()} 张优惠券处于订单锁定状态`,
          actionLabel: '查看券包',
          actionPath: `/coupons?tenantId=${asset.tenantId}&tab=my`,
        });
      }
      if (Number(asset.expiringSoonCouponCount || 0) > 0) {
        items.push({
          title: '优惠券即将过期',
          detail: `${asset.tenantName} 有 ${Number(asset.expiringSoonCouponCount).toLocaleString()} 张优惠券将在 30 天内过期`,
          actionLabel: '查看券包',
          actionPath: `/coupons?tenantId=${asset.tenantId}&tab=my`,
        });
      }
      if (Number(asset.expiredCouponCount || 0) > 0) {
        items.push({
          title: '存在失效优惠券',
          detail: `${asset.tenantName} 已有 ${Number(asset.expiredCouponCount).toLocaleString()} 张优惠券失效`,
          actionLabel: '查看记录',
          actionPath: `/coupons?tenantId=${asset.tenantId}&tab=expired`,
        });
      }
      return items;
    }).slice(0, 4);
  }, [tenantAssets]);

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 px-4 pb-10 md:mt-8">
      <header>
        <h1 className="text-3xl font-black text-slate-900">我的钱包</h1>
        <p className="mt-1 font-medium text-slate-500">这里的数据已经来自真实钱包、积分和流水接口。</p>
      </header>

      {error && (
        <div className="flex flex-col gap-4 rounded-3xl border border-red-100 bg-red-50 px-6 py-5 text-red-700 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <AlertCircle className="h-5 w-5 flex-none" />
            <span className="text-sm font-bold">{error}</span>
          </div>
          <button
            type="button"
            onClick={() => void loadWalletData()}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-white px-4 py-2 text-sm font-black text-red-700 shadow-sm transition-all hover:bg-red-100"
          >
            <RefreshCw className="h-4 w-4" />
            重试
          </button>
        </div>
      )}

      {!isLoading && assetRiskItems.length > 0 && (
        <section className="rounded-3xl border border-amber-100 bg-amber-50/70 px-6 py-5">
          <div className="mb-4 flex items-center gap-2 text-sm font-black text-amber-700">
            <AlertCircle className="h-5 w-5" />
            资产提醒
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            {assetRiskItems.map((item, index) => (
              <button
                key={`${item.title}-${index}`}
                type="button"
                onClick={() => navigate(item.actionPath)}
                className="flex items-center justify-between gap-4 rounded-2xl border border-amber-100 bg-white px-4 py-3 text-left shadow-sm transition-all hover:border-amber-200 hover:bg-amber-50"
              >
                <div>
                  <div className="text-sm font-black text-slate-900">{item.title}</div>
                  <div className="mt-0.5 text-xs font-bold text-slate-500">{item.detail}</div>
                </div>
                <span className="flex-none text-xs font-black text-amber-700">{item.actionLabel}</span>
              </button>
            ))}
          </div>
        </section>
      )}

      {!isLoading && assetHolds.length > 0 && (
        <section className="border-y border-violet-100 bg-violet-50/70 px-6 py-5">
          <div className="mb-4 flex items-center gap-2 text-sm font-black text-violet-700">
            <AlertCircle className="h-5 w-5" />
            受限资产
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            {assetHolds.map((hold, index) => (
              <button
                key={`${hold.tenantId}-${hold.assetType}-${hold.holdStatus}-${hold.bizNo ?? index}`}
                type="button"
                disabled={!hold.actionPath}
                onClick={() => hold.actionPath && navigate(hold.actionPath)}
                className={cn(
                  'flex items-center justify-between gap-4 border border-violet-100 bg-white px-4 py-3 text-left shadow-sm',
                  hold.actionPath ? 'transition-colors hover:border-violet-300 hover:bg-violet-50' : 'cursor-default',
                )}
              >
                <div className="min-w-0">
                  <div className="text-sm font-black text-slate-900">{assetHoldTitle(hold)}</div>
                  <div className="mt-0.5 truncate text-xs font-bold text-slate-500">
                    {[hold.tenantName, hold.amountText, hold.reason, hold.bizNo].filter(Boolean).join(' · ')}
                  </div>
                </div>
                {hold.actionPath && (
                  <span className="flex-none text-xs font-black text-violet-700">{hold.actionLabel ?? '查看订单'}</span>
                )}
              </button>
            ))}
          </div>
        </section>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="relative col-span-8 flex min-h-[280px] flex-col justify-between overflow-hidden rounded-3xl border border-slate-100 bg-white p-8 shadow-xl shadow-slate-200/50"
        >
          <div className="pointer-events-none absolute -right-20 -top-20 h-80 w-80 rounded-full bg-primary/5 blur-3xl" />

          <div className="relative z-10">
            <div className="mb-4 flex items-center gap-2 text-xs font-black uppercase tracking-widest text-slate-400">
              <CircleDollarSign className="h-5 w-5 text-primary" />
              统一钱包余额
            </div>
            <div className="text-5xl font-black tracking-tight text-slate-900">
              {isLoading ? '...' : formatCurrency(wallet?.availableAmount)}
            </div>
            <div className="mt-6 inline-flex items-center gap-1.5 rounded-full border border-green-100 bg-green-50 px-3 py-1.5 text-xs font-black text-green-600">
              <TrendingUp className="h-4 w-4" />
              已接入真实后端
            </div>
          </div>

          <div className="relative z-10 mt-10 flex flex-wrap gap-12 border-t border-slate-50 pt-8">
            <div className="min-w-[140px] flex-1">
              <div className="text-xs font-black uppercase tracking-widest text-slate-400">可用资金</div>
              <div className="mt-1 text-2xl font-black tracking-tight text-slate-900">
                {isLoading ? '...' : formatCurrency(wallet?.availableAmount)}
              </div>
            </div>
            <div className="min-w-[140px] flex-1">
              <div className="text-xs font-black uppercase tracking-widest text-slate-400">冻结金额</div>
              <div className="mt-1 text-2xl font-black tracking-tight text-slate-900">
                {isLoading ? '...' : formatCurrency(wallet?.frozenAmount)}
              </div>
            </div>
          </div>
        </motion.div>

        <div className="col-span-4 flex flex-col gap-6">
          <MetricCard
            label="商户积分"
            value={isLoading ? '...' : tenantAssetTotals.points.toLocaleString()}
            accent="text-tertiary"
            bg="bg-tertiary/5"
            icon={<Star className="h-6 w-6 fill-current" />}
          />
          <MetricCard
            label="可用优惠券"
            value={isLoading ? '...' : `${tenantAssetTotals.usableCoupons.toLocaleString()} 张`}
            accent="text-amber-600"
            bg="bg-amber-50"
            icon={<Ticket className="h-6 w-6 fill-current" />}
          />
          <MetricCard
            label="成长值"
            value={isLoading ? '...' : tenantAssetTotals.growth.toLocaleString()}
            accent="text-emerald-600"
            bg="bg-emerald-50"
            icon={<TrendingUp className="h-6 w-6" />}
          />
          <MetricCard
            label="关联商户"
            value={isLoading ? '...' : tenantAssets.length.toString()}
            accent="text-primary"
            bg="bg-primary/5"
            icon={<Store className="h-6 w-6" />}
          />
        </div>
      </div>

      <section className="overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-xl shadow-slate-200/30">
        <div className="flex flex-col gap-2 border-b border-slate-50 px-8 py-6 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h3 className="text-xl font-black text-slate-900">商户资产概览</h3>
            <p className="mt-1 text-xs font-bold text-slate-400">
              商户钱包合计 {formatCurrency(tenantAssetTotals.wallet)} · 积分 {tenantAssetTotals.points.toLocaleString()} · 可用券 {tenantAssetTotals.usableCoupons.toLocaleString()} 张 · 成长值 {tenantAssetTotals.growth.toLocaleString()}
            </p>
          </div>
        </div>

        <div className="divide-y divide-slate-50">
          {isLoading && Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="flex items-center justify-between px-8 py-5">
              <div className="h-12 w-12 rounded-2xl bg-slate-100" />
              <div className="ml-4 flex-1">
                <div className="h-4 w-32 rounded-full bg-slate-100" />
                <div className="mt-2 h-3 w-48 rounded-full bg-slate-50" />
              </div>
            </div>
          ))}
          {!isLoading && tenantAssets.length === 0 && (
            <div className="px-8 py-8 text-sm font-bold text-slate-400">
              暂无商户钱包或积分资产。消费、充值或领取会员权益后会在这里汇总。
            </div>
          )}
          {!isLoading && tenantAssets.map((asset) => (
            <div key={asset.tenantId} className="flex flex-col gap-4 px-8 py-5 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-5">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/5 text-primary">
                  <Store className="h-5 w-5" />
                </div>
                <div>
                  <div className="font-black text-slate-900">{asset.tenantName}</div>
                  <div className="mt-0.5 flex flex-wrap gap-2 text-xs font-semibold text-slate-400">
                    <span>商户钱包 {formatCurrency(asset.walletAvailableAmount)}</span>
                    <span>积分 {Number(asset.points || 0).toLocaleString()} 分</span>
                    <span>可用券 {Number(asset.usableCouponCount || 0).toLocaleString()} 张</span>
                    <span>成长值 {Number(asset.totalGrowth || 0).toLocaleString()}</span>
                    {asset.expiringSoonPoints > 0 && (
                      <span className="text-amber-600">30天内过期 {asset.expiringSoonPoints.toLocaleString()} 分</span>
                    )}
                    {Number(asset.expiringSoonCouponCount || 0) > 0 && (
                      <span className="text-amber-600">即将过期券 {Number(asset.expiringSoonCouponCount).toLocaleString()} 张</span>
                    )}
                    {Number(asset.lockedCouponCount || 0) > 0 && (
                      <span className="text-violet-600">锁定券 {Number(asset.lockedCouponCount).toLocaleString()} 张</span>
                    )}
                  </div>
                </div>
              </div>
              <div className="flex flex-wrap gap-2 sm:justify-end">
                <button
                  type="button"
                  onClick={() => navigate(`/wallet/tenants/${asset.tenantId}`)}
                  className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-black text-slate-600 transition-all hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                >
                  钱包明细
                </button>
                <button
                  type="button"
                  onClick={() => navigate(`/points/${asset.tenantId}`)}
                  className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-black text-slate-600 transition-all hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                >
                  积分明细
                </button>
                <button
                  type="button"
                  onClick={() => navigate(`/growth/${asset.tenantId}`)}
                  className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-black text-slate-600 transition-all hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                >
                  成长值
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>

      <div className="grid grid-cols-3 gap-4 md:gap-6">
        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={() => navigate('/recharge')}
          className="flex flex-col items-center justify-center gap-3 rounded-3xl bg-primary px-4 py-6 text-white shadow-xl shadow-primary/20 transition-all hover:bg-primary-container"
        >
          <Plus className="h-6 w-6" />
          <span className="text-sm font-black uppercase tracking-widest">充值</span>
        </motion.button>

        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={() => navigate('/history')}
          className="flex flex-col items-center justify-center gap-3 rounded-3xl border border-slate-200 bg-white px-4 py-6 text-slate-900 shadow-sm transition-all hover:bg-slate-50"
        >
          <Receipt className="h-6 w-6 text-slate-400" />
          <span className="text-sm font-black uppercase tracking-widest">交易记录</span>
        </motion.button>

        <motion.button
          whileTap={{ scale: 0.95 }}
          onClick={() => navigate('/orders')}
          className="flex flex-col items-center justify-center gap-3 rounded-3xl border border-slate-200 bg-white px-4 py-6 text-slate-900 shadow-sm transition-all hover:bg-slate-50"
        >
          <WalletIcon className="h-6 w-6 text-slate-400" />
          <span className="text-sm font-black uppercase tracking-widest">我的订单</span>
        </motion.button>
      </div>

      <section className="overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-xl shadow-slate-200/30">
        <div className="flex flex-col gap-4 border-b border-slate-50 px-8 py-6">
          <div>
            <h3 className="text-xl font-black text-slate-900">统一资产动态</h3>
            <p className="mt-1 text-xs font-bold text-slate-400">
              钱包、积分、成长值、优惠券事件按时间合并展示
            </p>
          </div>
          <div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_180px_160px]">
            <div className="flex flex-wrap gap-2" aria-label="资产类型筛选">
              {ACTIVITY_TYPE_OPTIONS.map((option) => {
                const selected = activityQuery.types?.includes(option.value) ?? false;
                return (
                  <button
                    key={option.value}
                    type="button"
                    aria-pressed={selected}
                    onClick={() => toggleActivityType(option.value)}
                    className={cn(
                      'rounded-lg border px-3 py-1.5 text-xs font-black transition-colors',
                      selected
                        ? 'border-primary bg-primary text-white'
                        : 'border-slate-200 bg-white text-slate-600 hover:border-primary/40 hover:text-primary',
                    )}
                  >
                    {option.label}
                  </button>
                );
              })}
            </div>
            <select
              aria-label="商户筛选"
              value={activityQuery.tenantId ?? ''}
              onChange={(event) => updateActivityTenant(event.target.value ? Number(event.target.value) : undefined)}
              className="h-9 rounded-lg border border-slate-200 bg-white px-3 text-xs font-bold text-slate-700 outline-none focus:border-primary"
            >
              <option value="">全部商户</option>
              {tenantAssets.map((asset) => (
                <option key={asset.tenantId} value={asset.tenantId}>{asset.tenantName}</option>
              ))}
            </select>
            <select
              aria-label="时间范围筛选"
              value={activityTimeRange}
              onChange={(event) => updateActivityTimeRange(event.target.value as 'ALL' | 'SEVEN_DAYS' | 'THIRTY_DAYS' | 'CUSTOM')}
              className="h-9 rounded-lg border border-slate-200 bg-white px-3 text-xs font-bold text-slate-700 outline-none focus:border-primary"
            >
              <option value="ALL">全部时间</option>
              <option value="SEVEN_DAYS">近 7 天</option>
              <option value="THIRTY_DAYS">近 30 天</option>
              <option value="CUSTOM">自定义</option>
            </select>
          </div>
          {activityTimeRange === 'CUSTOM' && (
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
              <input
                aria-label="资产动态开始时间"
                type="datetime-local"
                value={customActivityFrom}
                onChange={(event) => setCustomActivityFrom(event.target.value)}
                className="h-9 min-w-0 rounded-lg border border-slate-200 px-3 text-xs font-bold text-slate-700 outline-none focus:border-primary"
              />
              <input
                aria-label="资产动态结束时间"
                type="datetime-local"
                value={customActivityTo}
                onChange={(event) => setCustomActivityTo(event.target.value)}
                className="h-9 min-w-0 rounded-lg border border-slate-200 px-3 text-xs font-bold text-slate-700 outline-none focus:border-primary"
              />
              <button
                type="button"
                onClick={applyCustomActivityRange}
                className="h-9 rounded-lg border border-primary px-3 text-xs font-black text-primary transition-colors hover:bg-primary/5"
              >
                应用
              </button>
            </div>
          )}
        </div>

        <div className="divide-y divide-slate-50">
          {isLoading && Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="flex items-center gap-4 px-8 py-5">
              <div className="h-11 w-11 rounded-2xl bg-slate-100" />
              <div className="flex-1">
                <div className="h-4 w-32 rounded-full bg-slate-100" />
                <div className="mt-2 h-3 w-48 rounded-full bg-slate-50" />
              </div>
            </div>
          ))}
          {!isLoading && assetActivitiesUnavailable && (
            <div className="px-8 py-8 text-sm font-bold text-slate-400">
              {activityLoadError ?? '资产动态暂时不可用，请稍后重试。'}
            </div>
          )}
          {!isLoading && !assetActivitiesUnavailable && assetActivities.length === 0 && (
            <div className="px-8 py-8 text-sm font-bold text-slate-400">
              暂无资产动态。领取优惠券、下单支付或获得积分后会在这里出现。
            </div>
          )}
          {!isLoading && assetActivities.map((activity) => {
            const content = activityRowContent(activity);
            const key = `${activity.sourceType}-${activity.sourceId}`;
            const className = 'flex w-full items-center justify-between gap-4 px-8 py-5 text-left transition-colors';
            return activity.actionPath ? (
              <motion.button
                key={key}
                whileHover={{ backgroundColor: '#f8fafc' }}
                type="button"
                onClick={() => navigate(activity.actionPath!)}
                className={className}
              >
                {content}
              </motion.button>
            ) : (
              <motion.div key={key} className={className}>
                {content}
              </motion.div>
            );
          })}
        </div>
        {!isLoading && !assetActivitiesUnavailable && assetActivities.length > 0 && (
          <div className="border-t border-slate-50 px-8 py-4">
            {hasMoreActivities ? (
              <button
                type="button"
                onClick={loadMoreActivities}
                disabled={isLoadingMoreActivities}
                className="w-full rounded-lg border border-slate-200 px-4 py-2 text-sm font-black text-slate-700 transition-colors hover:border-primary/40 hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
              >
                {isLoadingMoreActivities ? '正在加载...' : '加载更多'}
              </button>
            ) : (
              <div className="text-center text-xs font-bold text-slate-400">没有更多记录</div>
            )}
            {activityLoadError && (
              <div className="mt-2 text-center text-xs font-bold text-red-600">{activityLoadError}</div>
            )}
          </div>
        )}
      </section>

      <section className="overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-xl shadow-slate-200/30">
        <div className="flex items-center justify-between border-b border-slate-50 px-8 py-6">
          <h3 className="text-xl font-black text-slate-900">最近钱包流水</h3>
          <button onClick={() => navigate('/history')} className="flex items-center gap-1 text-sm font-bold text-primary hover:underline">
            查看全部 <ChevronRight className="h-4 w-4" />
          </button>
        </div>

        <div className="divide-y divide-slate-50">
          {(recentDistribution.length > 0 ? recentDistribution : [{
            title: '暂无流水',
            source: '等待真实数据',
            amountText: '¥0.00',
            percent: '0%',
            color: 'bg-slate-100 text-slate-600',
            initials: 'NA',
          }]).map((entry, index) => (
            <motion.div
              key={`${entry.title}-${index}`}
              whileHover={{ backgroundColor: '#f8fafc' }}
              className="flex items-center justify-between px-8 py-5 transition-colors"
            >
              <div className="flex items-center gap-5">
                <div className={cn('flex h-12 w-12 items-center justify-center rounded-2xl font-black text-sm shadow-sm', entry.color)}>
                  {entry.initials}
                </div>
                <div>
                  <div className="font-black text-slate-900">{entry.title}</div>
                  <div className="mt-0.5 text-xs font-semibold text-slate-400">{entry.source}</div>
                  {'actionPath' in entry && entry.actionPath && (
                    <button
                      type="button"
                      onClick={() => navigate(entry.actionPath!)}
                      className="mt-2 rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-black text-slate-600 transition-all hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                    >
                      {entry.actionLabel}
                    </button>
                  )}
                </div>
              </div>
              <div className="text-right">
                <div className="font-black text-slate-900">{entry.amountText}</div>
                <div className="mt-0.5 text-xs font-semibold text-slate-400">占变动的 {entry.percent}</div>
              </div>
            </motion.div>
          ))}
        </div>
      </section>
    </div>
  );
}

function emptyActivityPage(): AssetActivityPage {
  return { records: [], nextCursor: null, hasMore: false };
}

function activityRangeBounds(
  range: 'ALL' | 'SEVEN_DAYS' | 'THIRTY_DAYS' | 'CUSTOM',
  customFrom: string,
  customTo: string,
): Pick<AssetActivityQuery, 'from' | 'to'> {
  if (range === 'SEVEN_DAYS' || range === 'THIRTY_DAYS') {
    const now = new Date();
    const days = range === 'SEVEN_DAYS' ? 7 : 30;
    return {
      from: toLocalDateTime(new Date(now.getTime() - days * 24 * 60 * 60 * 1000)),
      to: toLocalDateTime(now),
    };
  }
  if (range === 'CUSTOM') {
    return {
      from: customFrom || undefined,
      to: customTo || undefined,
    };
  }
  return { from: undefined, to: undefined };
}

function toLocalDateTime(value: Date) {
  const part = (number: number) => String(number).padStart(2, '0');
  return `${value.getFullYear()}-${part(value.getMonth() + 1)}-${part(value.getDate())}T${part(value.getHours())}:${part(value.getMinutes())}:${part(value.getSeconds())}`;
}

function assetHoldTitle(hold: AssetHold) {
  if (hold.assetType === 'COUPON') {
    return '优惠券锁定中';
  }
  if (hold.assetType === 'POINTS') {
    return '积分预占中';
  }
  return '钱包金额冻结';
}

function activityRowContent(activity: AssetActivity) {
  return (
    <>
      <div className="flex min-w-0 items-center gap-5">
        <div className={cn('flex h-11 w-11 flex-none items-center justify-center rounded-2xl', activityToneClass(activity.tone))}>
          {activityIcon(activity.assetType)}
        </div>
        <div className="min-w-0">
          <div className="truncate font-black text-slate-900">{activity.title}</div>
          <div className="mt-0.5 truncate text-xs font-semibold text-slate-400">
            {[activity.tenantName, activity.description, activity.bizNo].filter(Boolean).join(' · ')}
          </div>
        </div>
      </div>
      <div className="flex-none text-right">
        {activity.amountText && (
          <div className={cn('font-black', activity.tone === 'negative' ? 'text-red-600' : activity.tone === 'positive' ? 'text-emerald-600' : 'text-slate-900')}>
            {activity.amountText}
          </div>
        )}
        <div className="mt-0.5 text-xs font-semibold text-slate-400">{formatActivityTime(activity.occurredAt)}</div>
      </div>
    </>
  );
}

function activityIcon(assetType: string) {
  if (assetType === 'COUPON') {
    return <Ticket className="h-5 w-5" />;
  }
  if (assetType === 'POINTS') {
    return <Coins className="h-5 w-5" />;
  }
  if (assetType === 'GROWTH') {
    return <BadgeCheck className="h-5 w-5" />;
  }
  return <WalletIcon className="h-5 w-5" />;
}

function activityToneClass(tone?: string | null) {
  if (tone === 'negative') {
    return 'bg-red-50 text-red-600';
  }
  if (tone === 'positive') {
    return 'bg-emerald-50 text-emerald-600';
  }
  return 'bg-slate-100 text-slate-600';
}

function formatActivityTime(value?: string | null) {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function MetricCard({
  label,
  value,
  accent,
  bg,
  icon,
}: {
  label: string;
  value: string;
  accent: string;
  bg: string;
  icon: ReactNode;
}) {
  return (
    <motion.div
      whileHover={{ y: -4, boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.05)' }}
      className="flex flex-1 items-center justify-between rounded-3xl border border-slate-100 bg-white p-6 shadow-lg shadow-slate-200/40"
    >
      <div>
        <div className="text-xs font-black uppercase tracking-widest text-slate-400">{label}</div>
        <div className={cn('mt-2 text-3xl font-black tracking-tight', accent)}>{value}</div>
      </div>
      <div className={cn('flex h-14 w-14 items-center justify-center rounded-2xl', bg, accent)}>{icon}</div>
    </motion.div>
  );
}
