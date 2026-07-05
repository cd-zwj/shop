import { useEffect, useState, type ReactNode } from 'react';
import {
  ArrowLeft,
  ChevronRight,
  DollarSign,
  Eye,
  Info,
  KeyRound,
  Layers,
  Package,
  RefreshCw,
  Save,
  Upload,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { fileUploadService } from '../../services/modules/fileUpload';
import { merchantProductService } from '../../services/modules/merchantProduct';
import { merchantProductTaxonomyService } from '../../services/modules/merchantProductTaxonomy';
import { merchantStoreService } from '../../services/modules/merchantStore';
import type {
  FulfillmentMode,
  MerchantCardKey,
  MerchantCardKeySummary,
  MerchantProductUpsertPayload,
  MerchantStore,
  ProductType,
  VirtualProductCategory,
  VirtualProductType,
} from '../../types/merchant';
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
  productType: 'PHYSICAL',
  fulfillmentMode: 'EXPRESS_DELIVERY',
  deliveryConfig: '',
};

const PRODUCT_TYPE_OPTIONS: { value: NonNullable<MerchantProductUpsertPayload['productType']>; label: string; hint: string }[] = [
  { value: 'PHYSICAL', label: '实物', hint: '支付后等待商户发货' },
  { value: 'VIRTUAL', label: '虚拟内容', hint: '需配置 contentUrl / accountInfo' },
  { value: 'CARD_KEY', label: '卡密 / 兑换码', hint: '保存商品后在库存池批量上传' },
  { value: 'SERVICE', label: '服务', hint: '支付后生成核销码,商户侧确认核销' },
  { value: 'SUBSCRIPTION', label: '订阅 / 权益', hint: '可配置 validityDays' },
];

const FULFILLMENT_OPTIONS: { value: FulfillmentMode; label: string; hint: string }[] = [
  { value: 'ONLINE_VIRTUAL', label: '线上虚拟', hint: '资料、卡密、权益即时交付' },
  { value: 'OFFLINE_SERVICE', label: '线下服务', hint: '到店预约或核销' },
  { value: 'EXPRESS_DELIVERY', label: '快递发货', hint: '手工录入发货信息' },
];

const ONLINE_PRODUCT_TYPES: ProductType[] = ['VIRTUAL', 'CARD_KEY', 'SUBSCRIPTION'];

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
  const [cardKeySummary, setCardKeySummary] = useState<MerchantCardKeySummary | null>(null);
  const [cardKeys, setCardKeys] = useState<MerchantCardKey[]>([]);
  const [cardKeyInput, setCardKeyInput] = useState('');
  const [isCardKeyLoading, setIsCardKeyLoading] = useState(false);
  const [isCardKeyUploading, setIsCardKeyUploading] = useState(false);
  const [stores, setStores] = useState<MerchantStore[]>([]);
  const [virtualTypes, setVirtualTypes] = useState<VirtualProductType[]>([]);
  const [virtualCategories, setVirtualCategories] = useState<VirtualProductCategory[]>([]);

  useEffect(() => {
    let isMounted = true;

    async function loadProduct() {
      if (!isEdit || !tenantId || !id) {
        return;
      }

      try {
        const product = await merchantProductService.getProduct(tenantId, Number(id));
        if (!isMounted) return;
        const loadedProductType = (product.productType as MerchantProductUpsertPayload['productType']) || 'PHYSICAL';
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
          productType: loadedProductType,
          fulfillmentMode: product.fulfillmentMode || inferFulfillmentMode(loadedProductType),
          storeId: product.storeId || undefined,
          virtualTypeId: product.virtualTypeId || undefined,
          virtualCategoryId: product.virtualCategoryId || undefined,
          deliveryConfig: product.deliveryConfig || '',
        });
        if (loadedProductType === 'CARD_KEY') {
          void loadCardKeys();
        }
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

  useEffect(() => {
    if (!tenantId) return undefined;
    let isMounted = true;

    async function loadOptions() {
      try {
        const [storePage, types, categories] = await Promise.all([
          merchantStoreService.listStores(tenantId, { current: 1, size: 100, status: 1 }),
          merchantProductTaxonomyService.listTypes(tenantId, 1),
          merchantProductTaxonomyService.listCategories(tenantId, { status: 1 }),
        ]);
        if (!isMounted) return;
        setStores(storePage.records || []);
        setVirtualTypes(types || []);
        setVirtualCategories(categories || []);
      } catch {
        if (isMounted) {
          setError('商品履约选项加载失败，请稍后重试');
        }
      }
    }

    void loadOptions();

    return () => {
      isMounted = false;
    };
  }, [tenantId]);

  async function loadCardKeys() {
    if (!tenantId || !id) {
      return;
    }

    setIsCardKeyLoading(true);
    try {
      const [summary, page] = await Promise.all([
        merchantProductService.getCardKeySummary(tenantId, Number(id)),
        merchantProductService.listCardKeys(tenantId, Number(id), { current: 1, size: 10 }),
      ]);
      setCardKeySummary(summary);
      setCardKeys(page.records || []);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '卡密库存加载失败，请稍后重试');
    } finally {
      setIsCardKeyLoading(false);
    }
  }

  function updateField<K extends keyof MerchantProductUpsertPayload>(
    key: K,
    value: MerchantProductUpsertPayload[K],
  ) {
    setFormData((prev) => ({ ...prev, [key]: value }));
  }

  function handleFulfillmentModeChange(mode: FulfillmentMode) {
    setFormData((prev) => {
      const next: MerchantProductUpsertPayload = {
        ...prev,
        fulfillmentMode: mode,
      };
      if (mode === 'EXPRESS_DELIVERY') {
        next.productType = 'PHYSICAL';
        next.virtualTypeId = undefined;
        next.virtualCategoryId = undefined;
      }
      if (mode === 'OFFLINE_SERVICE') {
        next.productType = 'SERVICE';
        next.virtualTypeId = undefined;
        next.virtualCategoryId = undefined;
      }
      if (mode === 'ONLINE_VIRTUAL' && !ONLINE_PRODUCT_TYPES.includes((prev.productType || 'VIRTUAL') as ProductType)) {
        next.productType = 'VIRTUAL';
      }
      return next;
    });
  }

  function handleProductTypeChange(nextType: ProductType) {
    setFormData((prev) => ({
      ...prev,
      productType: nextType,
      fulfillmentMode: inferFulfillmentMode(nextType),
      virtualTypeId: nextType === 'PHYSICAL' ? undefined : prev.virtualTypeId,
      virtualCategoryId: nextType === 'PHYSICAL' ? undefined : prev.virtualCategoryId,
      ...(nextType !== prev.productType ? { virtualTypeId: undefined, virtualCategoryId: undefined } : {}),
    }));
    if (nextType === 'CARD_KEY' && isEdit) {
      void loadCardKeys();
    } else {
      setCardKeySummary(null);
      setCardKeys([]);
    }
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

  async function handleCardKeyUpload() {
    if (!tenantId || !id) {
      setError('请先保存卡密商品，再上传卡密库存');
      return;
    }

    const codes = Array.from(
      new Set(
        cardKeyInput
          .split(/\r?\n/)
          .map((line) => line.trim())
          .filter(Boolean),
      ),
    );
    if (codes.length === 0) {
      setError('请至少输入一条卡密');
      return;
    }

    setIsCardKeyUploading(true);
    setError('');
    setSuccess('');

    try {
      const summary = await merchantProductService.uploadCardKeys(tenantId, Number(id), codes);
      setCardKeySummary(summary);
      setCardKeyInput('');
      setSuccess(`已上传 ${codes.length} 条卡密`);
      await loadCardKeys();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '卡密上传失败，请稍后重试');
    } finally {
      setIsCardKeyUploading(false);
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
        productType: formData.productType || 'PHYSICAL',
        fulfillmentMode: formData.fulfillmentMode || inferFulfillmentMode(formData.productType || 'PHYSICAL'),
        storeId: formData.storeId ? Number(formData.storeId) : undefined,
        virtualTypeId: formData.virtualTypeId ? Number(formData.virtualTypeId) : undefined,
        virtualCategoryId: formData.virtualCategoryId ? Number(formData.virtualCategoryId) : undefined,
        deliveryConfig: formData.deliveryConfig?.trim() || undefined,
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

            <div className="flex flex-col gap-3">
              <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                履约形态
              </label>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                {FULFILLMENT_OPTIONS.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => handleFulfillmentModeChange(option.value)}
                    className={`rounded-[20px] border-2 px-4 py-4 text-left transition-all ${
                      formData.fulfillmentMode === option.value
                        ? 'border-primary bg-primary/5 text-primary'
                        : 'border-slate-100 bg-slate-50 text-slate-600'
                    }`}
                  >
                    <div className="text-sm font-black">{option.label}</div>
                    <div className="mt-1 text-xs font-bold opacity-70">{option.hint}</div>
                  </button>
                ))}
              </div>
            </div>

            {(formData.fulfillmentMode === 'OFFLINE_SERVICE' || formData.fulfillmentMode === 'EXPRESS_DELIVERY') && (
              <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                <div className="flex flex-col gap-3">
                  <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                    关联门店
                  </label>
                  <select
                    value={formData.storeId || ''}
                    onChange={(event) => updateField('storeId', event.target.value ? Number(event.target.value) : undefined)}
                    className="w-full rounded-[20px] border-2 border-slate-50 bg-slate-50 px-6 py-4 font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white"
                  >
                    <option value="">不绑定门店</option>
                    {stores.map((store) => (
                      <option key={store.id} value={store.id}>
                        {store.storeName} / {store.storeNo}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="rounded-[20px] bg-slate-50 px-5 py-4 text-xs font-bold leading-relaxed text-slate-500">
                  {formData.fulfillmentMode === 'OFFLINE_SERVICE'
                    ? '线下服务商品建议绑定门店，支付后继续使用现有核销码和商户订单核销流程。'
                    : '快递商品可绑定发货门店，物流公司和物流单号仍在订单发货流程中手工录入。'}
                </div>
              </div>
            )}

            {formData.fulfillmentMode === 'ONLINE_VIRTUAL' && (
              <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                <div className="flex flex-col gap-3">
                  <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                    虚拟商品类型
                  </label>
                  <select
                    value={formData.virtualTypeId || ''}
                    onChange={(event) => {
                      const value = event.target.value ? Number(event.target.value) : undefined;
                      updateField('virtualTypeId', value);
                      updateField('virtualCategoryId', undefined);
                    }}
                    className="w-full rounded-[20px] border-2 border-slate-50 bg-slate-50 px-6 py-4 font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white"
                  >
                    <option value="">选择虚拟类型</option>
                    {virtualTypes
                      .filter((type) => type.deliveryStrategy === formData.productType)
                      .map((type) => (
                        <option key={type.id} value={type.id}>
                          {type.typeName} / {type.typeCode}
                        </option>
                      ))}
                  </select>
                </div>
                <div className="flex flex-col gap-3">
                  <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                    虚拟商品分类
                  </label>
                  <select
                    value={formData.virtualCategoryId || ''}
                    onChange={(event) => updateField('virtualCategoryId', event.target.value ? Number(event.target.value) : undefined)}
                    className="w-full rounded-[20px] border-2 border-slate-50 bg-slate-50 px-6 py-4 font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white"
                  >
                    <option value="">可不选分类</option>
                    {virtualCategories
                      .filter((category) => category.typeId === formData.virtualTypeId)
                      .map((category) => (
                        <option key={category.id} value={category.id}>
                          {category.categoryName} / {category.categoryCode}
                        </option>
                      ))}
                  </select>
                </div>
              </div>
            )}

            {/* 商品类型 + 交付配置：不同类型走不同 DeliveryStrategy */}
            <div className="flex flex-col gap-3">
              <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                商品类型
              </label>
              <select
                value={formData.productType || 'PHYSICAL'}
                onChange={(event) => {
                  handleProductTypeChange(event.target.value as ProductType);
                }}
                className="w-full rounded-[20px] border-2 border-slate-50 bg-slate-50 px-6 py-4 font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white"
              >
                {PRODUCT_TYPE_OPTIONS.filter((opt) => isProductTypeAllowed(formData.fulfillmentMode || 'EXPRESS_DELIVERY', opt.value)).map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label} — {opt.hint}
                  </option>
                ))}
              </select>
              {formData.productType && formData.productType !== 'PHYSICAL' && formData.productType !== 'CARD_KEY' && (
                <div className="flex flex-col gap-2">
                  <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                    交付配置 (JSON)
                  </label>
                  <textarea
                    rows={3}
                    value={formData.deliveryConfig || ''}
                    onChange={(event) => updateField('deliveryConfig', event.target.value)}
                    placeholder={
                      formData.productType === 'VIRTUAL'
                        ? '{"contentUrl":"https://...","accountInfo":"账号:xxx"}'
                        : formData.productType === 'SERVICE'
                            ? '服务类可留空,支付后系统自动生成核销码'
                        : formData.productType === 'SUBSCRIPTION'
                          ? '{"validityDays":30}'
                          : '按商品类型填写交付配置'
                    }
                    className="no-scrollbar w-full rounded-[20px] border-2 border-slate-50 bg-slate-50 px-6 py-4 font-mono text-xs leading-relaxed text-slate-700 outline-none transition-all focus:border-primary focus:bg-white"
                  />
                </div>
              )}
            </div>

            {formData.productType === 'CARD_KEY' && (
              <div className="flex flex-col gap-5 rounded-[28px] border border-slate-100 bg-slate-50 p-5">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white text-primary shadow-sm">
                      <KeyRound className="h-5 w-5" />
                    </div>
                    <div>
                      <h3 className="text-sm font-black text-slate-900">卡密库存池</h3>
                      <p className="text-xs font-bold text-slate-400">
                        {isEdit ? '每行一条卡密，上传后进入可售库存' : '保存商品后可上传卡密'}
                      </p>
                    </div>
                  </div>
                  {isEdit && (
                    <button
                      type="button"
                      onClick={() => void loadCardKeys()}
                      disabled={isCardKeyLoading}
                      className="flex items-center justify-center gap-2 rounded-2xl bg-white px-4 py-3 text-xs font-black text-slate-600 shadow-sm transition-all hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      <RefreshCw className={`h-4 w-4 ${isCardKeyLoading ? 'animate-spin' : ''}`} />
                      刷新
                    </button>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
                  {buildCardKeyStats(cardKeySummary).map((item) => (
                    <div key={item.label} className="rounded-2xl bg-white p-4 shadow-sm">
                      <div className="text-[10px] font-black uppercase tracking-widest text-slate-400">{item.label}</div>
                      <div className="mt-2 text-2xl font-black text-slate-900">{item.value}</div>
                    </div>
                  ))}
                </div>

                {isEdit ? (
                  <div className="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(300px,0.75fr)]">
                    <div className="flex flex-col gap-3">
                      <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                        批量上传
                      </label>
                      <textarea
                        rows={7}
                        value={cardKeyInput}
                        onChange={(event) => setCardKeyInput(event.target.value)}
                        placeholder={'VIP-2026-0001\nVIP-2026-0002\nVIP-2026-0003'}
                        className="no-scrollbar w-full rounded-[20px] border-2 border-white bg-white px-5 py-4 font-mono text-xs leading-relaxed text-slate-700 outline-none transition-all focus:border-primary"
                      />
                      <button
                        type="button"
                        onClick={() => void handleCardKeyUpload()}
                        disabled={isCardKeyUploading}
                        className="flex items-center justify-center gap-2 rounded-[20px] bg-slate-900 px-5 py-4 text-sm font-black text-white shadow-lg transition-all hover:scale-[1.01] disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        <Upload className="h-4 w-4" />
                        {isCardKeyUploading ? '上传中...' : '上传卡密'}
                      </button>
                    </div>

                    <div className="flex flex-col gap-3">
                      <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
                        最近卡密
                      </label>
                      <div className="overflow-hidden rounded-[20px] bg-white shadow-sm">
                        {cardKeys.length === 0 ? (
                          <div className="px-5 py-8 text-center text-xs font-bold text-slate-400">
                            {isCardKeyLoading ? '加载中...' : '暂无卡密'}
                          </div>
                        ) : (
                          <div className="divide-y divide-slate-100">
                            {cardKeys.map((cardKey) => (
                              <div key={cardKey.id} className="flex items-center justify-between gap-3 px-4 py-3">
                                <div className="min-w-0">
                                  <div className="truncate font-mono text-xs font-black text-slate-800">
                                    {cardKey.cardCode}
                                  </div>
                                  <div className="mt-1 text-[10px] font-bold text-slate-400">
                                    {formatDate(cardKey.createTime)}
                                  </div>
                                </div>
                                <span
                                  className={`shrink-0 rounded-full px-3 py-1 text-[10px] font-black ${getCardKeyStatusClass(cardKey.status)}`}
                                >
                                  {getCardKeyStatusLabel(cardKey.status)}
                                </span>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="rounded-2xl border border-amber-100 bg-amber-50 px-4 py-3 text-xs font-bold text-amber-700">
                    当前商品还未创建，保存后即可上传卡密库存。
                  </div>
                )}
              </div>
            )}

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

function buildCardKeyStats(summary: MerchantCardKeySummary | null) {
  return [
    { label: '全部', value: summary?.totalCount ?? 0 },
    { label: '可售', value: summary?.availableCount ?? 0 },
    { label: '已售', value: summary?.usedCount ?? 0 },
    { label: '已退', value: summary?.returnedCount ?? 0 },
    { label: '停用', value: summary?.disabledCount ?? 0 },
  ];
}

function getCardKeyStatusLabel(status?: string | null) {
  const labels: Record<string, string> = {
    AVAILABLE: '可售',
    USED: '已售',
    RETURNED: '已退',
    DISABLED: '停用',
  };
  return labels[status || ''] || status || '-';
}

function getCardKeyStatusClass(status?: string | null) {
  const classes: Record<string, string> = {
    AVAILABLE: 'bg-emerald-50 text-emerald-600',
    USED: 'bg-blue-50 text-blue-600',
    RETURNED: 'bg-amber-50 text-amber-600',
    DISABLED: 'bg-slate-100 text-slate-500',
  };
  return classes[status || ''] || 'bg-slate-100 text-slate-500';
}

function formatDate(value?: string | null) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 16);
}

function inferFulfillmentMode(productType: ProductType): FulfillmentMode {
  if (productType === 'PHYSICAL') {
    return 'EXPRESS_DELIVERY';
  }
  if (productType === 'SERVICE') {
    return 'OFFLINE_SERVICE';
  }
  return 'ONLINE_VIRTUAL';
}

function isProductTypeAllowed(mode: FulfillmentMode, productType: ProductType) {
  if (mode === 'EXPRESS_DELIVERY') {
    return productType === 'PHYSICAL';
  }
  if (mode === 'OFFLINE_SERVICE') {
    return productType === 'SERVICE';
  }
  return ONLINE_PRODUCT_TYPES.includes(productType);
}
