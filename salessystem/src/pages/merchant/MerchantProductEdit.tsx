import { useEffect, useState, type ReactNode } from 'react';
import { ArrowLeft, Eye, ImagePlus, Save } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { fileUploadService } from '../../services/modules/fileUpload';
import { merchantProductService } from '../../services/modules/merchantProduct';
import { merchantStoreService } from '../../services/modules/merchantStore';
import type { MerchantProductUpsertPayload, MerchantStore } from '../../types/merchant';
import { ApiError } from '../../types/api';

const EMPTY_FORM: MerchantProductUpsertPayload = {
  productCode: '',
  name: '',
  price: 0,
  unit: '件',
  category: '',
  description: '',
  imageUrl: '',
  stock: 0,
  status: 'active',
  fulfillmentMode: 'STORE_PICKUP',
};

export default function MerchantProductEdit() {
  const navigate = useNavigate();
  const { id } = useParams();
  const { merchantSession } = useAuth();
  const tenantId = merchantSession?.tenantId;
  const isEdit = Boolean(id);
  const [formData, setFormData] = useState<MerchantProductUpsertPayload>(EMPTY_FORM);
  const [stores, setStores] = useState<MerchantStore[]>([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isLoading, setIsLoading] = useState(Boolean(id));
  const [isSaving, setIsSaving] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    if (!tenantId) {
      return undefined;
    }
    let active = true;

    async function load() {
      try {
        const storesPage = await merchantStoreService.listStores(tenantId, { current: 1, size: 100, status: 1 });
        if (!active) return;
        setStores(storesPage.records || []);
        if (!isEdit || !id) return;

        const product = await merchantProductService.getProduct(tenantId, Number(id));
        if (!active) return;
        setFormData({
          productCode: product.productCode || '',
          name: product.name || '',
          price: Number(product.price || 0),
          unit: product.unit || '件',
          category: product.category || '',
          description: product.description || '',
          imageUrl: product.imageUrl || '',
          stock: Number(product.stock || 0),
          status: product.status === 'inactive' ? 'inactive' : 'active',
          fulfillmentMode: 'STORE_PICKUP',
          storeId: product.storeId || undefined,
        });
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof ApiError ? loadError.message : '商品信息加载失败，请稍后重试');
        }
      } finally {
        if (active) setIsLoading(false);
      }
    }

    void load();
    return () => {
      active = false;
    };
  }, [id, isEdit, tenantId]);

  function updateField<K extends keyof MerchantProductUpsertPayload>(key: K, value: MerchantProductUpsertPayload[K]) {
    setFormData((current) => ({ ...current, [key]: value }));
  }

  async function handleImageUpload(file: File | null) {
    if (!file) return;
    setIsUploading(true);
    setError('');
    try {
      updateField('imageUrl', await fileUploadService.uploadFile(file));
    } catch (uploadError) {
      setError(uploadError instanceof Error ? uploadError.message : '图片上传失败，请稍后重试');
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
    if (!formData.storeId) {
      setError('请为商品选择可售门店');
      return;
    }
    if (!Number.isInteger(Number(formData.stock)) || Number(formData.stock) < 0) {
      setError('库存必须是大于等于 0 的整数');
      return;
    }

    setIsSaving(true);
    setError('');
    setSuccess('');
    const payload: MerchantProductUpsertPayload = {
      productCode: formData.productCode?.trim() || undefined,
      name: formData.name.trim(),
      price: Number(formData.price),
      unit: formData.unit?.trim() || undefined,
      category: formData.category?.trim() || undefined,
      description: formData.description?.trim() || undefined,
      imageUrl: formData.imageUrl?.trim() || undefined,
      storeId: Number(formData.storeId),
      stock: Number(formData.stock),
      status: formData.status || 'active',
      fulfillmentMode: 'STORE_PICKUP',
    };

    try {
      const product = isEdit && id
        ? await merchantProductService.updateProduct(tenantId, Number(id), payload)
        : await merchantProductService.createProduct(tenantId, payload);
      setSuccess(isEdit ? '商品已更新' : '商品已创建');
      navigate(`/merchant/product/${product.id}`);
    } catch (saveError) {
      setError(saveError instanceof ApiError ? saveError.message : '商品保存失败，请稍后重试');
    } finally {
      setIsSaving(false);
    }
  }

  if (isLoading) {
    return <div className="p-8 text-sm text-slate-500">加载商品信息...</div>;
  }

  return (
    <main className="mx-auto w-full max-w-5xl p-4 pb-12 md:p-8">
      <header className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <button type="button" onClick={() => navigate(-1)} className="mb-3 inline-flex items-center gap-2 text-sm text-slate-600 hover:text-slate-950">
            <ArrowLeft size={16} /> 返回商品列表
          </button>
          <h1 className="text-2xl font-bold text-slate-950">{isEdit ? '编辑商品' : '新增商品'}</h1>
          <p className="mt-1 text-sm text-slate-500">仅支持实体商品和到店自提。</p>
        </div>
        <div className="flex gap-2">
          {isEdit && id && (
            <button type="button" title="预览用户视图" onClick={() => navigate(`/product/${id}`)} className="inline-flex h-10 w-10 items-center justify-center border border-slate-300 text-slate-700 hover:bg-slate-50">
              <Eye size={18} />
            </button>
          )}
          <button type="button" onClick={handleSave} disabled={isSaving} className="inline-flex h-10 items-center gap-2 bg-slate-900 px-4 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-60">
            <Save size={17} /> {isSaving ? '保存中' : '保存'}
          </button>
        </div>
      </header>

      {error && <p role="alert" className="mb-5 border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>}
      {success && <p className="mb-5 border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{success}</p>}

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
        <section className="space-y-5 border border-slate-200 bg-white p-5">
          <h2 className="text-base font-semibold text-slate-900">商品信息</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="商品名称" required>
              <input value={formData.name} onChange={(event) => updateField('name', event.target.value)} className="field" maxLength={100} />
            </Field>
            <Field label="商品编码">
              <input value={formData.productCode || ''} onChange={(event) => updateField('productCode', event.target.value)} className="field" maxLength={64} placeholder="不填则自动生成" />
            </Field>
            <Field label="售价" required>
              <input type="number" min="0.01" step="0.01" value={formData.price} onChange={(event) => updateField('price', Number(event.target.value))} className="field" />
            </Field>
            <Field label="计量单位">
              <input value={formData.unit || ''} onChange={(event) => updateField('unit', event.target.value)} className="field" maxLength={20} />
            </Field>
            <Field label="商品分类">
              <input value={formData.category || ''} onChange={(event) => updateField('category', event.target.value)} className="field" maxLength={50} />
            </Field>
            <Field label="状态" required>
              <select value={formData.status || 'active'} onChange={(event) => updateField('status', event.target.value as 'active' | 'inactive')} className="field">
                <option value="active">上架</option>
                <option value="inactive">下架</option>
              </select>
            </Field>
          </div>
          <Field label="商品描述">
            <textarea value={formData.description || ''} onChange={(event) => updateField('description', event.target.value)} className="field min-h-28 resize-y" maxLength={2000} />
          </Field>
        </section>

        <aside className="space-y-5">
          <section className="border border-slate-200 bg-white p-5">
            <h2 className="mb-4 text-base font-semibold text-slate-900">门店与库存</h2>
            <div className="space-y-4">
              <Field label="可售门店" required>
                <select value={formData.storeId || ''} onChange={(event) => updateField('storeId', event.target.value ? Number(event.target.value) : undefined)} className="field">
                  <option value="">请选择门店</option>
                  {stores.map((store) => <option key={store.id} value={store.id}>{store.storeName}</option>)}
                </select>
              </Field>
              <Field label="初始库存" required>
                <input type="number" min="0" step="1" value={formData.stock} onChange={(event) => updateField('stock', Number(event.target.value))} className="field" />
              </Field>
              <div className="border-l-2 border-slate-900 bg-slate-50 px-3 py-2 text-sm text-slate-600">履约方式固定为到店自提。商品保存后可在门店库存工作台调整库存。</div>
            </div>
          </section>
          <section className="border border-slate-200 bg-white p-5">
            <h2 className="mb-4 text-base font-semibold text-slate-900">商品主图</h2>
            {formData.imageUrl ? <img src={formData.imageUrl} alt="商品主图预览" className="mb-3 aspect-square w-full object-cover" /> : <div className="mb-3 flex aspect-square items-center justify-center bg-slate-100 text-sm text-slate-400">暂无图片</div>}
            <label className="inline-flex h-10 w-full cursor-pointer items-center justify-center gap-2 border border-slate-300 text-sm text-slate-700 hover:bg-slate-50">
              <ImagePlus size={17} /> {isUploading ? '上传中' : '上传图片'}
              <input type="file" accept="image/*" className="sr-only" disabled={isUploading} onChange={(event) => void handleImageUpload(event.target.files?.[0] || null)} />
            </label>
            <input value={formData.imageUrl || ''} onChange={(event) => updateField('imageUrl', event.target.value)} className="field mt-3" placeholder="或填写图片地址" />
          </section>
        </aside>
      </div>
    </main>
  );
}

function Field({ label, required, children }: { label: string; required?: boolean; children: ReactNode }) {
  return <label className="block text-sm font-medium text-slate-700"><span className="mb-1.5 block">{label}{required && <span className="ml-1 text-red-600">*</span>}</span>{children}</label>;
}
