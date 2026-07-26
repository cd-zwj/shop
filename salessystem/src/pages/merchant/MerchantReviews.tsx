import { useEffect, useState } from 'react';
import { MessageSquare, Star } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { merchantReviewService } from '../../services/modules/merchantReview';
import type { StoreReview } from '../../types/review';

export default function MerchantReviews() {
  const { merchantSession } = useAuth();
  const { showToast } = useToast();
  const tenantId = merchantSession?.tenantId;
  const [reviews, setReviews] = useState<StoreReview[]>([]);
  const [replying, setReplying] = useState<number | null>(null);
  const [reply, setReply] = useState('');

  async function load() {
    if (!tenantId) return;
    try {
      const page = await merchantReviewService.list(tenantId);
      setReviews(page.records ?? []);
    } catch (error) {
      showToast(error instanceof Error ? error.message : '评价加载失败', 'error');
    }
  }
  useEffect(() => { void load(); }, [tenantId]);

  async function submitReply(reviewId: number) {
    if (!tenantId || !reply.trim()) return;
    try {
      await merchantReviewService.reply(tenantId, reviewId, reply.trim());
      setReplying(null); setReply(''); await load(); showToast('回复已发布', 'success');
    } catch (error) {
      showToast(error instanceof Error ? error.message : '回复失败', 'error');
    }
  }

  return <main className="mx-auto w-full max-w-5xl p-4 md:p-8">
    <header className="mb-6"><h1 className="text-2xl font-bold text-slate-900">门店评价</h1><p className="mt-1 text-sm text-slate-500">查看用户对已完成自提订单的反馈并公开回复。</p></header>
    <div className="space-y-3">
      {reviews.map((review) => <article key={review.id} className="border border-slate-200 bg-white p-5">
        <div className="flex items-center gap-1 text-amber-500">{Array.from({ length: review.rating }, (_, index) => <Star key={index} size={16} fill="currentColor" />)}</div>
        <p className="mt-3 whitespace-pre-wrap text-sm text-slate-700">{review.content || '用户未填写文字评价'}</p>
        <p className="mt-3 text-xs text-slate-400">订单 {review.orderNo} · {review.createTime}</p>
        {review.merchantReply ? <div className="mt-4 border-l-2 border-primary pl-3 text-sm text-slate-600">我的回复：{review.merchantReply}</div> : replying === review.id ? <div className="mt-4 flex gap-2"><input autoFocus value={reply} onChange={(event) => setReply(event.target.value)} maxLength={1000} className="min-w-0 flex-1 border border-slate-300 px-3 py-2 text-sm" placeholder="输入公开回复" /><button onClick={() => void submitReply(review.id)} className="bg-primary px-3 text-sm font-semibold text-white">发布</button></div> : <button onClick={() => setReplying(review.id)} className="mt-4 flex items-center gap-2 text-sm font-semibold text-primary"><MessageSquare size={15} /> 回复评价</button>}
      </article>)}
      {reviews.length === 0 && <p className="border border-dashed border-slate-300 p-8 text-center text-sm text-slate-500">暂无门店评价</p>}
    </div>
  </main>;
}
