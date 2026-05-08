import { useEffect, useState, type ReactNode } from 'react';
import {
  ArrowLeft,
  ChevronRight,
  DollarSign,
  Eye,
  Info,
  Layers,
  Package,
  Save,
  Upload,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { fileUploadService } from '../../services/modules/fileUpload';
import { merchantProductService } from '../../services/modules/merchantProduct';
import type { MerchantProductUpsertPayload } from '../../types/merchant';
import { ApiError } from '../../types/api';

const EMPTY_FORM: MerchantProductUpsertPayload = {
  productCode: '',
  name: '',
  price: 0,
  unit: '',
  category: '',
  description: '',
  imageUrl: '',
  stock: 0,
  status: 'active',
};

export default function MerchantProductEdit() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { merchantSession } = useAuth();
  const tenantId = merchantSession?.tenantId;
  const isEdit = Boolean(id);
  const [formData, setFormData] = useState<MerchantProductUpsertPayload>(EMPTY_FORM);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isLoading, setIsLoading] = useState(Boolean(id));
  const [isSaving, setIsSaving] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    let isMounted = true;

    async function loadProduct() {
      if (!isEdit || !tenantId || !id) {
        return;
      }

      try {
        const product = await merchantProductService.getProduct(tenantId, Number(id));
        if (!isMounted) return;
        setFormData({
          productCode: product.productCode || '',
          name: product.name || '',
          price: Number(product.price || 0),
          unit: product.unit || '',
          category: product.category || '',
          description: product.description || '',
          imageUrl: product.imageUrl || '',
          stock: Number(product.stock || 0),
          status:
            product.status === 'inactive' || product.status === 'out_of_stock'
              ? product.status
              : 'active',
        });
      } catch {
        if (!isMounted) return;
        setError('商品信息加载失败，请稍后重试');
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
  }, [id, isEdit, tenantId]);

  function updateField<K extends keyof MerchantProductUpsertPayload>(
    key: K,
    value: MerchantProductUpsertPayload[K],
  ) {
    setFormData((prev) => ({ ...prev, [key]: value }));
  }

  async function handleFileUpload(file: File | null) {
    if (!file) {
      return;
    }

    setIsUploading(true);
    setError('');
    setSuccess('');

    try {
      const fileUrl = await fileUploadService.uploadFile(file);
      updateField('imageUrl', fileUrl);
      setSuccess('图片上传成功，已自动填入主图地址');
    } catch (err) {
      setError(err instanceof Error ? err.message : '图片上传失败，请稍后重试');
    } finally {
      setIsUploading(false);
    }
  }

  async function handleSave() {
    if (!tenantId) {
      setError('当前商户会话缺少 tenantId，请重新登录');
      return;
    }
    if (!formData.name.trim()) {
      setError('商品名称不能为空');
      return;
    }
    if (Number(formData.price) <= 0) {
      setError('商品价格必须大于 0');
      return;
    }

    setIsSaving(true);
    setError('');
    setSuccess('');

    try {
      const payload: MerchantProductUpsertPayload = {
        productCode: formData.productCode?.trim() || undefined,
        name: formData.name.trim(),
        price: Number(formData.price),
        unit: formData.unit?.trim() || undefined,
        category: formData.category?.trim() || undefined,
        description: formData.description?.trim() || undefined,
        imageUrl: formData.imageUrl?.trim() || undefined,
        stock: Number(formData.stock || 0),
        status: formData.status || 'active',
      };

      const product =
        isEdit && id
          ? await merchantProductService.updateProduct(tenantId, Number(id), payload)
          : await merchantProductService.createProduct(tenantId, payload);

      setSuccess(isEdit ? '商品更新成功' : '商品创建成功');
      setTimeout(() => {
        navigate(`/merchant/product/${product.id}`);
      }, 600);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '商品保存失败，请稍后重试');
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-8 p-4 pb-32 md:p-8">
      <header className="flex flex-col justify-between gap-6 sm:flex-row sm:items-end">
        <div>
          <button
            onClick={() => navigate(-1)}
            className="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-widest text-primary transition-all hover:gap-3"
          >
            <ArrowLeft size={16} /> 返回列表
          </button>
          <h1 className="text-4xl font-black tracking-tight text-slate-900">
            {isEdit ? '编辑商品档案' : '发布新的商品'}
          </h1>
          <p className="mt-1 font-medium text-slate-500">
            当前商户：{merchantSession?.tenantName || '未获取商户会话'}，保存后会写入真实商品接口。
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button className="rounded-2xl border border-slate-100 bg-white p-4 text-slate-400 shadow-sm transition-all hover:text-slate-900">
            <Eye size={20} />
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving || isLoading}
            className="flex items-center gap-2 rounded-[24px] bg-primary px-8 py-4 text-base font-black text-white shadow-2xl shadow-primary/20 transition-all hover:scale-105 active:scale-95 disabled:cursor-not-allowed disabled:opacity-70"
          >
            <Save size={20} /> {isSaving ? '保存中...' : '保存变更'}
          </button>
        </div>
      </header>

      {error && (
        <div className="rounded-2xl border border-red-100 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">
          {error}
        </div>
      )}

      {success && (
        <div className="rounded-2xl border border-green-100 bg-green-50 px-4 py-3 text-sm font-medium text-green-600">
          {success}
        </div>
      )}

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <div className="flex flex-col gap-8 lg:col-span-8">
          <section className="flex flex-col gap-8 rounded-[40px] border border-slate-100 bg-white p-10 shadow-sm">
            <div className="flex flex-col gap-3">
              <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">商品标题</label>
              <input
                type="text"
                value={formData.name}
                onChange={(event) => updateField('name', event.target.value)}
                placeholder="输入商品名称..."
                className="w-full rounded-[20px] border-2 border-slate-50 bg-slate-50 px-6 py-4 text-xl font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white"
              />
            </div>

            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              <Field
                label="商品编码"
                value={formData.productCode || ''}
                onChange={(value) => updateField('productCode', value)}
                placeholder="可留空，由后端自动生成"
              />
              <Field
                label="单位"
                value={formData.unit || ''}
                onChange={(value) => updateField('unit', value)}
                placeholder="如：件 / 瓶 / 份"
              />
              <Field
                label="基础定价 (USD)"
                value={String(formData.price ?? 0)}
                onChange={(value) => updateField('price', Number(value || 0))}
                placeholder="0.00"
                icon={<DollarSign className="h-5 w-5 text-slate-300 transition-colors group-focus-within:text-primary" />}
              />
              <Field
                label="库存数量"
                value={String(formData.stock ?? 0)}
                onChange={(value) => updateField('stock', Number(value || 0))}
                placeholder="0"
                icon={<Package className="h-5 w-5 text-slate-300 transition-colors group-focus-within:text-primary" />}
              />
            </div>

            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              <Field
                label="所属类目"
                value={formData.category || ''}
                onChange={(value) => updateField('category', value)}
                placeholder="如：软件 / 硬件 / 饮品"
              />
              <div className="flex flex-col gap-3">
                <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">销售状态</label>
                <select
                  value={formData.status}
                  onChange={(event) =>
                    updateField('status', event.target.value as MerchantProductUpsertPayload['status'])
                  }
                  className="w-full rounded-[20px] border-2 border-slate-50 bg-slate-50 px-6 py-4 font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white"
                >
                  <option value="active">active</option>
                  <option value="inactive">inactive</option>
                  <option value="out_of_stock">out_of_stock</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              <Field
                label="主图 URL"
                value={formData.imageUrl || ''}
                onChange={(value) => updateField('imageUrl', value)}
                placeholder="https://..."
              />
              <div className="flex flex-col gap-3">
                <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">上传图片</label>
                <label className="flex cursor-pointer items-center justify-center gap-2 rounded-[20px] border-2 border-dashed border-slate-200 bg-slate-50 px-6 py-4 text-sm font-black text-slate-600 transition-all hover:border-primary hover:text-primary">
                  <Upload className="h-4 w-4" />
                  {isUploading ? '上传中...' : '选择图片并上传'}
                  <input
                    type="file"
                    accept="image/*"
                    className="hidden"
                    onChange={(event) => void handleFileUpload(event.target.files?.[0] || null)}
                  />
                </label>
              </div>
            </div>

            <div className="flex flex-col gap-3">
              <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">详细描述</label>
              <textarea
                rows={6}
                value={formData.description || ''}
                onChange={(event) => updateField('description', event.target.value)}
                className="no-scrollbar w-full rounded-[24px] border-2 border-slate-50 bg-slate-50 px-6 py-5 font-medium leading-relaxed text-slate-600 outline-none transition-all focus:border-primary focus:bg-white"
              />
            </div>
          </section>
        </div>

        <div className="flex flex-col gap-8 lg:col-span-4">
          <section className="flex flex-col gap-8 rounded-[40px] bg-slate-900 p-8 text-white shadow-xl">
            <div className="flex flex-col gap-4">
              <label className="text-[10px] font-black uppercase tracking-widest text-slate-500">商品状态摘要</label>
              <div className="grid grid-cols-2 gap-2 rounded-2xl bg-white/5 p-1">
                {['active', 'inactive'].map((status) => (
                  <button
                    key={status}
                    onClick={() => updateField('status', status as MerchantProductUpsertPayload['status'])}
                    className={`rounded-xl py-3 text-xs font-black transition-all ${
                      formData.status === status ? 'bg-white text-slate-900 shadow-lg' : 'text-slate-400 hover:text-white'
                    }`}
                  >
                    {status}
                  </button>
                ))}
              </div>
            </div>

            <div className="border-t border-white/5 pt-4">
              <label className="text-[10px] font-black uppercase tracking-widest text-slate-500">当前类目</label>
              <div className="mt-4 flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 p-4">
                <div className="flex items-center gap-3">
                  <Layers className="h-4 w-4 text-primary" />
                  <span className="text-sm font-bold">{formData.category || '未设置'}</span>
                </div>
                <ChevronRight size={14} className="text-slate-500" />
              </div>
            </div>
          </section>

          <section className="flex flex-col gap-6 rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
            <h3 className="text-[10px] font-black uppercase tracking-widest text-slate-400">保存提示</h3>
            <div className="flex gap-4 rounded-[32px] border border-blue-100 bg-blue-50/50 p-6">
              <Info className="h-5 w-5 shrink-0 text-primary" />
              <p className="text-xs font-medium leading-relaxed text-blue-600">
                当前版本已补上 `/api/file/upload` 的图片上传能力。上传成功后会自动回填主图地址，再随商品表单一起保存到真实商品接口。
              </p>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}

function Field({
  label,
  value,
  onChange,
  placeholder,
  icon,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  icon?: ReactNode;
}) {
  return (
    <div className="flex flex-col gap-3">
      <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</label>
      <div className="group relative">
        {icon && <div className="absolute left-4 top-1/2 -translate-y-1/2">{icon}</div>}
        <input
          type="text"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder}
          className={`w-full rounded-[20px] border-2 border-slate-50 bg-slate-50 py-4 pr-6 font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white ${
            icon ? 'pl-12' : 'px-6'
          }`}
        />
      </div>
    </div>
  );
}
