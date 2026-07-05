import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { useNavigate, useParams } from 'react-router-dom';
import { Coins, ArrowLeft, Clock, ShoppingBag, Sparkles } from 'lucide-react';
import { EmptyState } from '../components/ui/EmptyState';
import { appPointsService } from '../services/modules/appPoints';
import { appCatalogService } from '../services/modules/appCatalog';
import type { PointsBalance, PointsLog, ExchangeProduct } from '../types/points';
import type { Product } from '../types/catalog';
import { useToast } from '../context/ToastContext';
import { cn } from '../lib/utils';
import { getImageUrl } from '../utils/display';

type ProductWithTenant = Product & { tenantId: number };

export default function Points() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { tenantId: tenantIdParam } = useParams<{ tenantId: string }>();
  const tenantId = Number(tenantIdParam);

  const [balance, setBalance] = useState<PointsBalance | null>(null);
  const [logs, setLogs] = useState<PointsLog[]>([]);
  const [exchangeProducts, setExchangeProducts] = useState<ExchangeProduct[]>([]);
  const [productDetails, setProductDetails] = useState<Record<number, Product>>({});
  
  const [activeTab, setActiveTab] = useState<'logs' | 'exchange'>('logs');
  const [isLoading, setIsLoading] = useState(true);
  const [isExchanging, setIsExchanging] = useState<number | null>(null);

  // Pagination for logs

  // Fetch initial data
  const loadPointsData = async () => {
    if (!tenantId || isNaN(tenantId)) {
      showToast('缺少商户参数', 'error');
      setIsLoading(false);
      return;
    }
    try {
      const balanceData = await appPointsService.getPointsBalance(tenantId);
      setBalance(balanceData);

      // Load products for lookup
      let productsList: Product[] = [];
      try {
        const list = await appCatalogService.listTenantProducts(tenantId);
        productsList = list.map((p) => ({ ...p, tenantId }));
      } catch (e) {
        // Silently ignore tenant products load failure
      }

      // Load exchange products
      const exchanges = await appPointsService.getExchangeProducts(tenantId);
      setExchangeProducts(exchanges);

      // Resolve product details
      const resolvedDetails: Record<number, Product> = {};
      productsList.forEach((p) => {
        resolvedDetails[p.id] = p;
      });

      // For any exchange products not in tenant product list, query individually
      const missingProductIds = exchanges
        .map((ep) => ep.productId)
        .filter((id) => !resolvedDetails[id]);

      if (missingProductIds.length > 0) {
        try {
          const missingDetails = await Promise.all(
            missingProductIds.map((id) => appCatalogService.getProduct(id))
          );
          missingDetails.forEach((p) => {
            if (p) resolvedDetails[p.id] = { ...p, tenantId } as ProductWithTenant;
          });
        } catch (e) {
        // Silently ignore missing product details resolution failure
        }
      }

      setProductDetails(resolvedDetails);

      // Load logs
      const logsData = await appPointsService.getPointsLogs(tenantId, 1, 20);
      setLogs(logsData.records ?? []);
    } catch (e) {
      showToast('获取积分中心数据失败', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void loadPointsData();
  }, []);

  const handleExchange = async (ep: ExchangeProduct) => {
    if (!balance || balance.points < ep.pointsRequired) {
      showToast('积分不足，无法兑换该商品', 'error');
      return;
    }

    setIsExchanging(ep.id);
    try {
      await appPointsService.exchangeProduct(tenantId, ep.id);
      showToast('积分兑换成功！已生成兑换订单。', 'success');
      
      // Reload balance, logs and exchange list
      await loadPointsData();
    } catch (e: unknown) {
      const errMsg = e instanceof Error ? e.message : '兑换失败，请稍后重试';
      showToast(errMsg, 'error');
    } finally {
      setIsExchanging(null);
    }
  };

  const formatDate = (isoString: string) => {
    if (!isoString) return '';
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return isoString.split('T')[0] || '';
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).replace(/\//g, '-');
  };

  const groupLogsByDate = (logsList: PointsLog[]) => {
    const groups: Record<string, PointsLog[]> = {};
    logsList.forEach((log) => {
      const dateStr = log.createTime ? log.createTime.split('T')[0] : '其他';
      if (!groups[dateStr]) {
        groups[dateStr] = [];
      }
      groups[dateStr].push(log);
    });
    return Object.entries(groups).sort((a, b) => b[0].localeCompare(a[0]));
  };

  const expiringSoonPoints = balance?.expiringSoonPoints ?? 0;

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 pb-12 md:mt-8">
      {/* Header */}
      <header className="flex items-center justify-between border-b border-slate-100 pb-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="p-2 text-slate-600 hover:bg-slate-50 dark:hover:bg-slate-800 rounded-full transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-black text-slate-900 dark:text-white">积分中心</h1>
            <p className="text-xs font-semibold text-slate-400 mt-0.5">
              消费赚取积分，兑换精美礼品
            </p>
          </div>
        </div>
      </header>

      {/* Points Balance Card */}
      <section className="relative overflow-hidden bg-slate-900 text-white rounded-3xl p-6 sm:p-8 shadow-xl shadow-slate-900/10">
        <div className="pointer-events-none absolute -right-20 -top-20 h-64 w-64 rounded-full bg-primary/20 blur-3xl" />
        <div className="relative z-10 flex items-center justify-between">
          <div>
            <span className="text-[10px] font-black uppercase tracking-widest text-slate-400 flex items-center gap-1.5">
              <Coins className="w-4 h-4 text-yellow-500 fill-yellow-500" />
              当前可用积分
            </span>
            <div className="text-5xl font-black tracking-tight text-white mt-2 flex items-baseline gap-1">
              {isLoading ? '...' : (balance?.points ?? 0).toLocaleString()}
              <span className="text-sm font-semibold text-slate-400 ml-1">分</span>
            </div>
            <p className="text-xs font-semibold text-slate-400 mt-3 flex items-center gap-1">
              <Sparkles className="w-3.5 h-3.5 text-yellow-500" />
              可用积分在兑换商品时可抵扣等值商品
            </p>
            {expiringSoonPoints > 0 && (
              <p className="mt-3 inline-flex items-center gap-1.5 rounded-full bg-yellow-500/15 px-3 py-1 text-xs font-bold text-yellow-200">
                <Clock className="h-3.5 w-3.5" />
                近 30 天将过期 {expiringSoonPoints.toLocaleString()} 分
              </p>
            )}
          </div>
        </div>
      </section>

      {/* Tabs */}
      <div className="flex border-b border-slate-200">
        {[
          { key: 'logs', label: '积分明细' },
          { key: 'exchange', label: '积分兑换' },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key as 'logs' | 'exchange')}
            className={cn(
              'flex-1 text-center py-3.5 text-sm font-bold border-b-2 transition-all',
              activeTab === tab.key
                ? 'border-primary text-primary font-extrabold'
                : 'border-transparent text-slate-500 hover:text-slate-700'
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="min-h-[300px]">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-20 gap-3 text-slate-400">
            <div className="w-8 h-8 border-2 border-primary/20 border-t-primary rounded-full animate-spin" />
            <span className="text-sm font-medium">获取积分中心数据中...</span>
          </div>
        ) : (
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.15 }}
            >
              {/* TAB: LOGS */}
              {activeTab === 'logs' && (
                logs.length === 0 ? (
                  <EmptyState icon={<Clock className="w-12 h-12" />} title="暂无积分明细" subtitle="您最近还没有积分变动，快去消费赚取吧！" />
                ) : (
                  <div className="flex flex-col gap-6">
                    {groupLogsByDate(logs).map(([date, logItems]) => (
                      <div key={date} className="flex flex-col gap-3">
                        <h3 className="text-xs font-black text-slate-400 uppercase tracking-wider ml-1">
                          {date}
                        </h3>
                        <div className="overflow-hidden rounded-3xl border border-slate-100 bg-white dark:bg-slate-900 shadow-sm divide-y divide-slate-50">
                          {logItems.map((log) => {
                            const isGrant = log.type === 'GRANT';
                            return (
                              <div key={log.id} className="flex items-center justify-between p-5 transition-colors hover:bg-slate-50/50">
                                <div className="flex items-center gap-4">
                                  <div className={cn(
                                    'flex h-10 w-10 items-center justify-center rounded-2xl font-black text-sm',
                                    isGrant ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-600'
                                  )}>
                                    {isGrant ? '+' : '-'}
                                  </div>
                                  <div>
                                    <div className="font-extrabold text-slate-800 dark:text-white">
                                      {log.reason}
                                    </div>
                                    <div className="mt-0.5 text-xs font-semibold text-slate-400 flex items-center gap-1.5">
                                      {log.orderNo && <span>订单号: {log.orderNo}</span>}
                                      {log.orderNo && <span className="text-slate-200">•</span>}
                                      <span>{formatDate(log.createTime)}</span>
                                    </div>
                                  </div>
                                </div>
                                <div className="text-right">
                                  <div className={cn(
                                    'text-lg font-black',
                                    isGrant ? 'text-green-600' : 'text-red-600'
                                  )}>
                                    {isGrant ? '+' : ''}{log.points}
                                  </div>
                                  <div className="text-[10px] font-bold text-slate-400 mt-0.5">
                                    余额 {log.balance}
                                  </div>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    ))}
                  </div>
                )
              )}

              {/* TAB: EXCHANGE */}
              {activeTab === 'exchange' && (
                exchangeProducts.length === 0 ? (
                  <EmptyState icon={<ShoppingBag className="w-12 h-12" />} title="暂无可兑换的商品" subtitle="店铺最近没有上架积分兑换商品哦，敬请期待！" />
                ) : (
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                    {exchangeProducts.map((ep) => {
                      const product = productDetails[ep.productId];
                      const isOutOfStock = ep.stock <= 0;
                      const isPointsInsufficient = (balance?.points ?? 0) < ep.pointsRequired;
                      const canExchange = ep.status === 1 && !isOutOfStock && !isPointsInsufficient;

                      return (
                        <div
                          key={ep.id}
                          className="flex flex-col overflow-hidden rounded-3xl border border-slate-100 bg-white dark:bg-slate-900 shadow-sm transition-all duration-300 hover:shadow-md"
                        >
                          <div className="relative aspect-square bg-slate-100 dark:bg-slate-800 overflow-hidden">
                            <img
                              src={getImageUrl(product?.imageUrl)}
                              alt={product?.name || '积分商品'}
                              className="h-full w-full object-cover transition-transform duration-500 hover:scale-105"
                            />
                            {isOutOfStock && (
                              <div className="absolute inset-0 bg-slate-900/60 flex items-center justify-center backdrop-blur-[2px]">
                                <span className="border-2 border-white/60 text-white/90 font-black text-xs px-3 py-1.5 transform -rotate-12 rounded-lg">
                                  已兑完
                                </span>
                              </div>
                            )}
                          </div>
                          
                          <div className="flex flex-col gap-2 p-4 flex-1">
                            <h3 className="line-clamp-2 text-sm font-bold leading-relaxed text-slate-800 dark:text-white flex-1">
                              {product?.name || '积分礼品商品'}
                            </h3>
                            <div className="flex items-baseline gap-0.5 text-yellow-600 dark:text-yellow-500 mt-1">
                              <span className="text-lg font-black">{ep.pointsRequired}</span>
                              <span className="text-[10px] font-bold">积分</span>
                            </div>
                            
                            <div className="mt-2 flex justify-between items-center border-t border-slate-50 dark:border-slate-800/50 pt-3 gap-2">
                              <span className="text-[10px] font-bold text-slate-400">
                                剩 {ep.stock} 件
                              </span>
                              <button
                                disabled={!canExchange || isExchanging !== null}
                                onClick={() => handleExchange(ep)}
                                className={cn(
                                  'px-4 py-1.5 rounded-full text-[10px] font-black tracking-wide transition-all shadow-sm active:scale-95 shrink-0',
                                  canExchange
                                    ? 'bg-yellow-500 text-white hover:bg-yellow-600'
                                    : 'bg-slate-100 text-slate-400 cursor-not-allowed'
                                )}
                              >
                                {isExchanging === ep.id ? (
                                  <div className="w-3.5 h-3.5 border-2 border-slate-300 border-t-slate-600 rounded-full animate-spin" />
                                ) : isOutOfStock ? (
                                  '已兑完'
                                ) : isPointsInsufficient ? (
                                  '积分不足'
                                ) : (
                                  '兑换'
                                )}
                              </button>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )
              )}
            </motion.div>
          </AnimatePresence>
        )}
      </div>
    </div>
  );
}
