import { useEffect, useState } from 'react';
import {
  ArrowLeft,
  CheckCircle2,
  Edit3,
  Eye,
  ExternalLink,
  Package,
  ShieldCheck,
  ShoppingBag,
  Trash2,
  TrendingUp,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { merchantProductService } from '../../services/modules/merchantProduct';
import type { MerchantProduct } from '../../types/merchant';
import { formatCurrency, getImageUrl } from '../../utils/display';

export default function MerchantProductDetail() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { merchantSession } = useAuth();
  const tenantId = merchantSession?.tenantId;
  const [product, setProduct] = useState<MerchantProduct | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadProduct() {
      if (!tenantId || !id) {
        setError('商品参数缺失，请重新进入页面');
        setIsLoading(false);
        return;
      }

      try {
        const result = await merchantProductService.getProduct(tenantId, Number(id));
        if (!isMounted) return;
        setProduct(result);
        setError('');
      } catch {
        if (!isMounted) return;
        setError('商品详情加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadProduct();
    return () => {
      isMounted = false;
    };
  }, [id, tenantId]);

  async function handleDelete() {
    if (!tenantId || !product) return;
    try {
      await merchantProductService.deleteProduct(tenantId, product.id);
      navigate('/merchant/products');
    } catch {
      setError('商品删除失败，请稍后重试');
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 p-4 pb-32 md:gap-8 md:p-8">
      <header className="flex flex-col justify-between gap-6 sm:flex-row sm:items-end">
        <div className="flex flex-col gap-3">
          <button
            onClick={() => navigate(-1)}
            className="flex w-fit items-center gap-2 text-xs font-black uppercase tracking-widest text-primary transition-all hover:gap-3"
          >
            <ArrowLeft size={16} /> 返回列表
          </button>
          <div className="flex flex-col gap-4 md:flex-row md:items-center">
            <h1 className="text-3xl font-black tracking-tight text-slate-900 md:text-4xl">
              {product?.name || (isLoading ? '商品加载中...' : '未找到商品')}
            </h1>
            {product && (
              <span className="flex w-fit items-center gap-1 rounded-lg border border-green-100 bg-green-50 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-green-600">
                <CheckCircle2 size={10} /> {product.status}
              </span>
            )}
          </div>
          <p className="text-sm font-medium text-slate-500">
            SPU ID: {product?.id || '--'} · 商品编码: {product?.productCode || '--'} · 类目: {product?.category || '--'}
          </p>
        </div>
        <div className="flex w-full items-center gap-3 sm:w-auto">
          <button
            onClick={() => navigate(`/merchant/product/edit/${id}`)}
            className="flex flex-1 items-center justify-center gap-2 rounded-2xl border border-slate-100 bg-white p-4 text-slate-400 shadow-sm transition-all hover:bg-primary/5 hover:text-primary sm:flex-none"
          >
            <Edit3 size={20} /> <span className="font-bold sm:hidden">编辑商品</span>
          </button>
          <button
            onClick={handleDelete}
            className="flex flex-1 items-center justify-center gap-2 rounded-2xl border border-slate-100 bg-white p-4 text-slate-400 shadow-sm transition-all hover:bg-red-50 hover:text-red-500 sm:flex-none"
          >
            <Trash2 size={20} /> <span className="font-bold sm:hidden">下架移除</span>
          </button>
        </div>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <div className="flex flex-col gap-8 lg:col-span-8">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {Array.from({ length: 2 }).map((_, index) => (
              <div key={index} className="aspect-square overflow-hidden rounded-[32px] border border-slate-100 shadow-sm">
                <img
                  src={getImageUrl(product?.imageUrl)}
                  alt={product?.name || 'product'}
                  className="h-full w-full object-cover"
                />
              </div>
            ))}
          </div>

          <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm md:p-10">
            <h3 className="mb-6 text-sm font-black uppercase tracking-widest text-slate-900">商品详细描述</h3>
            <p className="font-medium leading-relaxed text-slate-600">
              {product?.description || '该商品暂无详细描述。'}
            </p>
          </section>

          <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm md:p-10">
            <div className="mb-8 flex items-center justify-between">
              <h3 className="text-sm font-black uppercase tracking-widest text-slate-900">商品数据概览</h3>
              <TrendingUp className="h-6 w-6 text-primary" />
            </div>
            <div className="grid grid-cols-1 gap-8 sm:grid-cols-2">
              <div className="flex flex-col gap-1 rounded-3xl bg-slate-50 p-6">
                <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">当前售价</span>
                <span className="text-2xl font-black text-slate-900">{formatCurrency(product?.price)}</span>
              </div>
              <div className="flex flex-col gap-1 rounded-3xl bg-slate-50 p-6">
                <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">库存数量</span>
                <span className="text-2xl font-black text-slate-900">{product?.stock ?? '--'}</span>
              </div>
            </div>
          </section>
        </div>

        <div className="flex flex-col gap-8 lg:col-span-4">
          <section className="rounded-[40px] bg-slate-900 p-8 text-white shadow-xl">
            <h3 className="mb-6 text-[10px] font-black uppercase tracking-widest text-slate-500">库存状态</h3>
            <div className="flex flex-col gap-6">
              <div>
                <span className="text-xs font-bold text-slate-400">当前剩余库存</span>
                <div className="mt-1 text-4xl font-black">
                  {product?.stock ?? '--'} <span className="text-base font-medium text-slate-500">PCS</span>
                </div>
              </div>
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/10">
                <div
                  className="h-full bg-primary"
                  style={{ width: `${Math.min(100, Math.max(5, Number(product?.stock || 0)))}%` }}
                />
              </div>
              <button
                onClick={() => navigate(`/merchant/product/edit/${id}`)}
                className="w-full rounded-2xl border border-white/10 bg-white/10 py-4 text-sm font-black transition-all hover:bg-white hover:text-slate-900"
              >
                快捷调整库存
              </button>
            </div>
          </section>

          <div className="flex flex-col gap-6 rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
            <h3 className="text-[10px] font-black uppercase tracking-widest text-slate-400">关联操作</h3>
            <div className="flex flex-col gap-4">
              <button
                onClick={() => navigate('/merchant/orders')}
                className="group flex items-center justify-between text-left"
              >
                <div className="flex items-center gap-3">
                  <div className="rounded-xl bg-slate-50 p-2 transition-colors group-hover:bg-primary/10">
                    <ShoppingBag size={14} className="text-slate-400 group-hover:text-primary" />
                  </div>
                  <span className="text-xs font-black text-slate-600 transition-colors group-hover:text-primary">
                    查看关联订单
                  </span>
                </div>
                <ExternalLink size={14} className="text-slate-300" />
              </button>
              <div className="group flex items-center justify-between text-left">
                <div className="flex items-center gap-3">
                  <div className="rounded-xl bg-slate-50 p-2 transition-colors group-hover:bg-primary/10">
                    <ShieldCheck size={14} className="text-slate-400 group-hover:text-primary" />
                  </div>
                  <span className="text-xs font-black text-slate-600 transition-colors group-hover:text-primary">
                    商品审核信息
                  </span>
                </div>
                <ExternalLink size={14} className="text-slate-300" />
              </div>
            </div>
          </div>

          <div className="flex flex-col items-center gap-4 rounded-[40px] border border-primary/10 bg-primary/5 p-8 text-center">
            <div className="rounded-full bg-primary/10 p-4 text-primary">
              <Eye size={24} />
            </div>
            <h4 className="font-black text-slate-900">预览用户视图</h4>
            <p className="text-xs font-medium text-slate-500">
              查看该商品在客户端商城中的真实展示效果。
            </p>
            <button
              onClick={() => navigate(`/product/${id}`)}
              className="mt-2 w-full rounded-2xl bg-primary py-3 text-xs font-black uppercase tracking-widest text-white"
            >
              立即前往预览
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
