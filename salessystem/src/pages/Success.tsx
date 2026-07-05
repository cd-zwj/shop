import { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import { CheckCircle2, ChevronRight, Home, Loader2 } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { appOrderService } from '../services/modules/appOrder';
import { formatCurrency } from '../utils/display';

/** 从 URL 参数或订单详情接口获取的订单摘要 */
interface OrderSummary {
  orderNo: string;
  totalAmount: number;
}

export default function Success() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const orderNo = searchParams.get('orderNo');
  const amountParam = searchParams.get('amount');

  const [summary, setSummary] = useState<OrderSummary | null>(null);
  const [isLoading, setIsLoading] = useState(!!orderNo);
  const [error, setError] = useState('');

  useEffect(() => {
    // 没有 orderNo 时直接展示通用成功页，不调用接口
    if (!orderNo) {
      return undefined;
    }

    let isMounted = true;

    async function loadOrder() {
      try {
        // 优先使用 URL 中显式传入的金额参数，避免额外请求
        if (amountParam) {
          const parsed = Number(amountParam);
          if (!Number.isNaN(parsed) && isMounted) {
            setSummary({ orderNo, totalAmount: parsed });
            setIsLoading(false);
            return;
          }
        }

        // URL 中无金额时，调用订单详情接口获取真实数据
        const detail = await appOrderService.getOrder(orderNo);
        if (!isMounted) return;
        setSummary({
          orderNo: detail.order.orderNo,
          totalAmount: detail.order.totalAmount,
        });
        setError('');
      } catch {
        if (!isMounted) return;
        // 接口失败时降级为通用提示，不阻断用户操作
        setError('订单详情加载失败');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadOrder();

    return () => {
      isMounted = false;
    };
  }, [orderNo, amountParam]);

  /** 是否有有效订单数据可展示 */
  const hasOrderData = summary !== null && !error;

  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-144px)] px-4">
      <motion.div
        initial={{ opacity: 0, scale: 0.8, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        transition={{ type: 'spring', damping: 20, stiffness: 300 }}
        className="w-24 h-24 bg-green-50 rounded-full flex items-center justify-center mb-10 relative shadow-2xl shadow-green-100 border border-green-100"
      >
        <div className="absolute inset-0 bg-green-500/10 rounded-full animate-ping" />
        <CheckCircle2 className="w-12 h-12 text-green-500 fill-white" />
      </motion.div>

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.2 }}
        className="text-center max-w-md flex flex-col items-center gap-4"
      >
        <h1 className="text-4xl font-black text-slate-900 tracking-tight">操作成功！</h1>
        <p className="text-lg text-slate-500 font-medium leading-relaxed">您的交易已完成处理。订单详情已发送至您的电子邮箱，电子凭证已存入钱包中心。</p>

        {/* 加载中状态 */}
        {isLoading && (
          <div className="flex items-center gap-2 text-slate-400 mt-4">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span className="text-sm font-medium">正在获取订单信息...</span>
          </div>
        )}

        {/* 订单摘要卡片：仅有数据时展示 */}
        {!isLoading && hasOrderData && (
          <div className="bg-slate-50 rounded-2xl p-6 border border-slate-100 w-full mt-6 shadow-inner">
            <div className="flex justify-between items-center mb-4 pb-4 border-b border-slate-200">
              <span className="text-xs font-black text-slate-400 uppercase tracking-widest">订单编号</span>
              <span className="text-sm font-black text-slate-900">{summary.orderNo}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-xs font-black text-slate-400 uppercase tracking-widest">交易金额</span>
              <span className="text-xl font-black text-primary tracking-tight">{formatCurrency(summary.totalAmount)}</span>
            </div>
          </div>
        )}

        {/* 接口失败时的错误提示（不影响主流程） */}
        {!isLoading && error && (
          <div className="bg-amber-50 rounded-2xl p-4 border border-amber-100 w-full mt-6">
            <p className="text-sm text-amber-700 font-medium text-center">{error}，请前往订单记录查看详情。</p>
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 w-full mt-12">
          <button
            onClick={() => navigate('/')}
            className="flex items-center justify-center gap-2 px-8 py-5 bg-slate-900 text-white rounded-2xl font-black text-sm hover:bg-slate-800 transition-all shadow-xl shadow-slate-900/10 active:scale-95"
          >
            <Home className="w-4 h-4" />
            返回首页
          </button>
          <button
             onClick={() => navigate('/history')}
            className="flex items-center justify-center gap-2 px-8 py-5 bg-white border-2 border-slate-100 text-slate-900 rounded-2xl font-black text-sm hover:border-primary hover:text-primary transition-all shadow-sm active:scale-95 group"
          >
            查看记录
            <ChevronRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
          </button>
        </div>
      </motion.div>
    </div>
  );
}
