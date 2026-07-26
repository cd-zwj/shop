import { useEffect, useState } from 'react';
import { ArrowLeft, Star } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { appOrderService } from '../services/modules/appOrder';
import { appReviewService } from '../services/modules/appReview';
import { useToast } from '../context/ToastContext';
import type { SalesOrderDetail } from '../types/order';
import type { StoreReview as StoreReviewType } from '../types/review';

export default function StoreReview() {
  const { orderNo } = useParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [detail, setDetail] = useState<SalesOrderDetail | null>(null);
  const [existing, setExisting] = useState<StoreReviewType | null>(null);
  const [rating, setRating] = useState(5);
  const [content, setContent] = useState('');
  const [imageUrls, setImageUrls] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    async function load() {
      if (!orderNo) return;
      try {
        const order = await appOrderService.getOrder(orderNo);
        if (!active) return;
        setDetail(order);
        const review = await appReviewService.getMine(order.order.tenantId, orderNo);
        if (active) setExisting(review);
      } catch (error) {
        if (active) showToast(error instanceof Error ? error.message : '评价信息加载失败', 'error');
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => { active = false; };
  }, [orderNo, showToast]);

  async function submit() {
    if (!detail || !orderNo) return;
    if (detail.order.orderStatus !== 'COMPLETED') {
      showToast('订单完成后才能评价门店', 'error');
      return;
    }
    setSubmitting(true);
    try {
      const urls = imageUrls.split(/\n|,/).map((value) => value.trim()).filter(Boolean);
      const review = await appReviewService.create(detail.order.tenantId, orderNo, { rating, content: content.trim() || undefined, imageUrls: urls });
      setExisting(review);
      showToast('评价已提交', 'success');
    } catch (error) {
      showToast(error instanceof Error ? error.message : '评价提交失败', 'error');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <div className="p-8 text-sm text-slate-500">加载评价信息...</div>;

  return (
    <main className="mx-auto w-full max-w-2xl p-4 md:p-8">
      <button onClick={() => navigate(-1)} className="mb-6 flex items-center gap-2 text-sm font-semibold text-slate-600">
        <ArrowLeft size={16} /> 返回订单
      </button>
      <h1 className="text-2xl font-bold text-slate-900">门店评价</h1>
      <p className="mt-2 text-sm text-slate-500">订单 {detail?.order.orderNo ?? '--'}</p>
      {existing ? (
        <section className="mt-6 border border-slate-200 bg-white p-5">
          <div className="flex items-center gap-1 text-amber-500">{Array.from({ length: existing.rating }, (_, index) => <Star key={index} size={18} fill="currentColor" />)}</div>
          {existing.content && <p className="mt-3 whitespace-pre-wrap text-sm text-slate-700">{existing.content}</p>}
          {existing.merchantReply && <div className="mt-4 border-l-2 border-slate-300 pl-3 text-sm text-slate-600">商家回复：{existing.merchantReply}</div>}
          {existing.status === 'HIDDEN' && <p className="mt-4 text-sm text-red-600">该评价已被平台隐藏：{existing.moderationRemark}</p>}
        </section>
      ) : (
        <section className="mt-6 space-y-6 border border-slate-200 bg-white p-5">
          <div>
            <label className="block text-sm font-semibold text-slate-700">评分</label>
            <div className="mt-2 flex gap-1">
              {Array.from({ length: 5 }, (_, index) => {
                const value = index + 1;
                return <button key={value} aria-label={`${value}星`} onClick={() => setRating(value)} className="p-1 text-amber-500"><Star size={28} fill={value <= rating ? 'currentColor' : 'none'} /></button>;
              })}
            </div>
          </div>
          <label className="block text-sm font-semibold text-slate-700">评价内容
            <textarea value={content} onChange={(event) => setContent(event.target.value)} maxLength={1000} rows={5} className="mt-2 w-full border border-slate-300 p-3 text-sm font-normal outline-none focus:border-primary" placeholder="分享本次到店自提体验" />
          </label>
          <label className="block text-sm font-semibold text-slate-700">凭证图片地址
            <textarea value={imageUrls} onChange={(event) => setImageUrls(event.target.value)} rows={3} className="mt-2 w-full border border-slate-300 p-3 text-sm font-normal outline-none focus:border-primary" placeholder="每行一条已上传图片地址，最多6条" />
          </label>
          <button onClick={() => void submit()} disabled={submitting} className="bg-primary px-5 py-3 text-sm font-semibold text-white disabled:opacity-50">{submitting ? '提交中...' : '提交评价'}</button>
        </section>
      )}
    </main>
  );
}
