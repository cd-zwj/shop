import { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import { ChevronRight, ShoppingBag } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { appOrderService } from '../services/modules/appOrder';
import type { SalesOrder } from '../types/order';
import { cn } from '../lib/utils';
import { formatCurrency } from '../utils/display';

export default function UserOrders() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<SalesOrder[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadOrders() {
      try {
        const result = await appOrderService.listOrders(1, 20);
        if (!isMounted) return;
        setOrders(result.records ?? []);
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadOrders();
    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <div className="min-h-screen bg-slate-50 pb-32">
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-6 p-4 md:p-8">
        <header className="flex flex-col gap-2">
          <h1 className="text-3xl font-black tracking-tight text-slate-900">我的订单</h1>
          <p className="font-medium text-slate-500">这里展示后端真实订单列表。</p>
        </header>

        <div className="flex gap-2 overflow-x-auto py-2 hide-scrollbar">
          {['全部', '待支付', '处理中', '已完成', '已关闭'].map((tab, i) => (
            <button
              key={tab}
              className={cn(
                'whitespace-nowrap rounded-2xl px-6 py-2.5 text-xs font-black uppercase tracking-widest transition-all',
                i === 0 ? 'bg-primary text-white shadow-lg shadow-primary/20' : 'border border-slate-100 bg-white text-slate-400',
              )}
            >
              {tab}
            </button>
          ))}
        </div>

        <div className="flex flex-col gap-4">
          {(isLoading ? Array.from({ length: 3 }) : orders).map((order, index) => {
            const isData = typeof order === 'object';
            return (
              <motion.div
                key={isData ? order.orderNo : index}
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                onClick={() => isData && navigate(`/order/${order.orderNo}`)}
                className="group cursor-pointer rounded-[32px] border border-slate-100 bg-white p-6 shadow-sm transition-all hover:shadow-xl hover:shadow-slate-200/50"
              >
                <div className="mb-4 flex items-center justify-between border-b border-slate-50 pb-4">
                  <div className="flex items-center gap-2">
                    <ShoppingBag className="h-4 w-4 text-primary" />
                    <span className="text-sm font-black text-slate-900">订单号 {isData ? order.orderNo : '加载中...'}</span>
                  </div>
                  <span className="rounded-lg bg-slate-100 px-2.5 py-1 text-[10px] font-black uppercase tracking-wider text-slate-500">
                    {isData ? `${order.orderStatus} / ${order.payStatus}` : '同步中'}
                  </span>
                </div>

                <div className="flex gap-4">
                  <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl border border-slate-100 bg-slate-50">
                    <ShoppingBag className="h-8 w-8 text-slate-300" />
                  </div>
                  <div className="flex flex-1 flex-col justify-between">
                    <div>
                      <h3 className="text-[10px] font-black uppercase tracking-widest text-slate-400">
                        商户 ID / 订单主题
                      </h3>
                      <p className="mt-0.5 text-xl font-black text-slate-900">
                        {isData ? formatCurrency(order.totalAmount) : '...'}
                      </p>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-medium text-slate-400">
                        {isData ? `${order.createTime || '--'} · tenant ${order.tenantId}` : '--'}
                      </span>
                      <ChevronRight className="h-5 w-5 text-slate-300 transition-colors group-hover:text-primary" />
                    </div>
                  </div>
                </div>
              </motion.div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
