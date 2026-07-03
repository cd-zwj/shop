import { useCallback, useEffect, useMemo, useState } from 'react';
import { Edit3, FolderTree, Plus, Tag, Trash2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { merchantProductTaxonomyService } from '../../services/modules/merchantProductTaxonomy';
import type {
  ProductType,
  VirtualProductCategory,
  VirtualProductCategoryPayload,
  VirtualProductType,
  VirtualProductTypePayload,
} from '../../types/merchant';

const TYPE_FORM: VirtualProductTypePayload = {
  typeCode: '',
  typeName: '',
  deliveryStrategy: 'VIRTUAL',
  description: '',
  status: 1,
  sortOrder: 0,
};

const CATEGORY_FORM: VirtualProductCategoryPayload = {
  typeId: 0,
  categoryCode: '',
  categoryName: '',
  parentId: 0,
  description: '',
  status: 1,
  sortOrder: 0,
};

const STRATEGIES: Array<{ value: Exclude<ProductType, 'PHYSICAL'>; label: string }> = [
  { value: 'VIRTUAL', label: '在线资料' },
  { value: 'CARD_KEY', label: '卡密兑换' },
  { value: 'SERVICE', label: '到店服务' },
  { value: 'SUBSCRIPTION', label: '会员权益' },
];

export default function MerchantProductTaxonomy() {
  const { merchantSession } = useAuth();
  const { showToast } = useToast();
  const tenantId = merchantSession?.tenantId;
  const [types, setTypes] = useState<VirtualProductType[]>([]);
  const [categories, setCategories] = useState<VirtualProductCategory[]>([]);
  const [selectedTypeId, setSelectedTypeId] = useState<number | 'ALL'>('ALL');
  const [typeForm, setTypeForm] = useState<VirtualProductTypePayload>(TYPE_FORM);
  const [categoryForm, setCategoryForm] = useState<VirtualProductCategoryPayload>(CATEGORY_FORM);
  const [editingType, setEditingType] = useState<VirtualProductType | null>(null);
  const [editingCategory, setEditingCategory] = useState<VirtualProductCategory | null>(null);
  const [loading, setLoading] = useState(false);

  const loadData = useCallback(async () => {
    if (!tenantId) return;
    setLoading(true);
    try {
      const [typeList, categoryList] = await Promise.all([
        merchantProductTaxonomyService.listTypes(tenantId),
        merchantProductTaxonomyService.listCategories(tenantId),
      ]);
      setTypes(typeList || []);
      setCategories(categoryList || []);
      if (!categoryForm.typeId && typeList?.[0]) {
        setCategoryForm((prev) => ({ ...prev, typeId: typeList[0].id }));
      }
    } catch (err) {
      showToast(err instanceof Error ? err.message : '虚拟商品字典加载失败', 'error');
    } finally {
      setLoading(false);
    }
  }, [categoryForm.typeId, showToast, tenantId]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const visibleCategories = useMemo(
    () => categories.filter((item) => selectedTypeId === 'ALL' || item.typeId === selectedTypeId),
    [categories, selectedTypeId],
  );

  async function saveType() {
    if (!tenantId || !typeForm.typeCode.trim() || !typeForm.typeName.trim()) {
      showToast('请填写类型编码和名称', 'error');
      return;
    }
    try {
      const payload = {
        ...typeForm,
        typeCode: typeForm.typeCode.trim(),
        typeName: typeForm.typeName.trim(),
        description: typeForm.description?.trim() || undefined,
      };
      if (editingType) {
        await merchantProductTaxonomyService.updateType(tenantId, editingType.id, payload);
      } else {
        await merchantProductTaxonomyService.createType(tenantId, payload);
      }
      showToast('虚拟商品类型已保存', 'success');
      setEditingType(null);
      setTypeForm(TYPE_FORM);
      await loadData();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '类型保存失败', 'error');
    }
  }

  async function saveCategory() {
    if (!tenantId || !categoryForm.typeId || !categoryForm.categoryCode.trim() || !categoryForm.categoryName.trim()) {
      showToast('请填写分类所属类型、编码和名称', 'error');
      return;
    }
    try {
      const payload = {
        ...categoryForm,
        categoryCode: categoryForm.categoryCode.trim(),
        categoryName: categoryForm.categoryName.trim(),
        description: categoryForm.description?.trim() || undefined,
      };
      if (editingCategory) {
        await merchantProductTaxonomyService.updateCategory(tenantId, editingCategory.id, payload);
      } else {
        await merchantProductTaxonomyService.createCategory(tenantId, payload);
      }
      showToast('虚拟商品分类已保存', 'success');
      setEditingCategory(null);
      setCategoryForm({ ...CATEGORY_FORM, typeId: types[0]?.id || 0 });
      await loadData();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '分类保存失败', 'error');
    }
  }

  async function deleteType(type: VirtualProductType) {
    if (!tenantId) return;
    try {
      await merchantProductTaxonomyService.deleteType(tenantId, type.id);
      await loadData();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '类型删除失败', 'error');
    }
  }

  async function deleteCategory(category: VirtualProductCategory) {
    if (!tenantId) return;
    try {
      await merchantProductTaxonomyService.deleteCategory(tenantId, category.id);
      await loadData();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '分类删除失败', 'error');
    }
  }

  function editType(type: VirtualProductType) {
    setEditingType(type);
    setTypeForm({
      typeCode: type.typeCode,
      typeName: type.typeName,
      deliveryStrategy: type.deliveryStrategy,
      description: type.description || '',
      status: type.status,
      sortOrder: type.sortOrder,
    });
  }

  function editCategory(category: VirtualProductCategory) {
    setEditingCategory(category);
    setCategoryForm({
      typeId: category.typeId,
      categoryCode: category.categoryCode,
      categoryName: category.categoryName,
      parentId: category.parentId,
      description: category.description || '',
      status: category.status,
      sortOrder: category.sortOrder,
    });
  }

  return (
    <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 p-4 md:p-8">
      <header>
        <h1 className="text-3xl font-black text-slate-900">商品形态字典</h1>
        <p className="mt-1 text-sm font-medium text-slate-500">维护虚拟商品“是哪种”和“有哪些种类”，不替代通用商品分类。</p>
      </header>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <section className="rounded-[28px] border border-slate-100 bg-white p-6 shadow-sm">
          <div className="mb-5 flex items-center justify-between">
            <h2 className="flex items-center gap-2 text-lg font-black text-slate-900"><Tag className="h-5 w-5 text-primary" /> 虚拟商品类型</h2>
            <button onClick={() => { setEditingType(null); setTypeForm(TYPE_FORM); }} className="rounded-xl bg-slate-100 p-2 text-slate-600">
              <Plus className="h-4 w-4" />
            </button>
          </div>
          <div className="grid gap-3">
            <Input label="类型编码" value={typeForm.typeCode} onChange={(value) => setTypeForm({ ...typeForm, typeCode: value })} />
            <Input label="类型名称" value={typeForm.typeName} onChange={(value) => setTypeForm({ ...typeForm, typeName: value })} />
            <select value={typeForm.deliveryStrategy} onChange={(event) => setTypeForm({ ...typeForm, deliveryStrategy: event.target.value as Exclude<ProductType, 'PHYSICAL'> })} className="rounded-2xl bg-slate-50 px-4 py-3 text-sm font-black outline-none">
              {STRATEGIES.map((item) => <option key={item.value} value={item.value}>{item.label} / {item.value}</option>)}
            </select>
            <Input label="描述" value={typeForm.description || ''} onChange={(value) => setTypeForm({ ...typeForm, description: value })} />
            <button onClick={() => void saveType()} className="rounded-2xl bg-slate-900 px-5 py-4 text-sm font-black text-white">
              {editingType ? '保存类型修改' : '新增类型'}
            </button>
          </div>
          <List loading={loading} empty="暂无类型">
            {types.map((type) => (
              <Row
                key={type.id}
                title={`${type.typeName} / ${type.typeCode}`}
                desc={`${type.deliveryStrategy} · ${type.status === 1 ? '启用' : '停用'}`}
                onEdit={() => editType(type)}
                onDelete={() => void deleteType(type)}
              />
            ))}
          </List>
        </section>

        <section className="rounded-[28px] border border-slate-100 bg-white p-6 shadow-sm">
          <div className="mb-5 flex items-center justify-between">
            <h2 className="flex items-center gap-2 text-lg font-black text-slate-900"><FolderTree className="h-5 w-5 text-primary" /> 虚拟商品分类</h2>
            <select value={selectedTypeId} onChange={(event) => setSelectedTypeId(event.target.value === 'ALL' ? 'ALL' : Number(event.target.value))} className="rounded-xl bg-slate-100 px-3 py-2 text-xs font-black outline-none">
              <option value="ALL">全部类型</option>
              {types.map((type) => <option key={type.id} value={type.id}>{type.typeName}</option>)}
            </select>
          </div>
          <div className="grid gap-3">
            <select value={categoryForm.typeId || ''} onChange={(event) => setCategoryForm({ ...categoryForm, typeId: Number(event.target.value) })} className="rounded-2xl bg-slate-50 px-4 py-3 text-sm font-black outline-none">
              <option value="">选择所属类型</option>
              {types.map((type) => <option key={type.id} value={type.id}>{type.typeName}</option>)}
            </select>
            <Input label="分类编码" value={categoryForm.categoryCode} onChange={(value) => setCategoryForm({ ...categoryForm, categoryCode: value })} />
            <Input label="分类名称" value={categoryForm.categoryName} onChange={(value) => setCategoryForm({ ...categoryForm, categoryName: value })} />
            <Input label="描述" value={categoryForm.description || ''} onChange={(value) => setCategoryForm({ ...categoryForm, description: value })} />
            <button onClick={() => void saveCategory()} className="rounded-2xl bg-slate-900 px-5 py-4 text-sm font-black text-white">
              {editingCategory ? '保存分类修改' : '新增分类'}
            </button>
          </div>
          <List loading={loading} empty="暂无分类">
            {visibleCategories.map((category) => (
              <Row
                key={category.id}
                title={`${category.categoryName} / ${category.categoryCode}`}
                desc={`${types.find((type) => type.id === category.typeId)?.typeName || '未知类型'} · ${category.status === 1 ? '启用' : '停用'}`}
                onEdit={() => editCategory(category)}
                onDelete={() => void deleteCategory(category)}
              />
            ))}
          </List>
        </section>
      </div>
    </div>
  );
}

function Input({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="grid gap-2">
      <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</span>
      <input value={value} onChange={(event) => onChange(event.target.value)} className="rounded-2xl bg-slate-50 px-4 py-3 text-sm font-bold outline-none focus:ring-2 focus:ring-primary/20" />
    </label>
  );
}

function List({ loading, empty, children }: { loading: boolean; empty: string; children: React.ReactNode }) {
  const hasChildren = Array.isArray(children) ? children.length > 0 : Boolean(children);
  return (
    <div className="mt-6 divide-y divide-slate-100 overflow-hidden rounded-2xl border border-slate-100">
      {loading ? <div className="p-6 text-center text-sm font-bold text-slate-400">加载中...</div> : hasChildren ? children : <div className="p-6 text-center text-sm font-bold text-slate-400">{empty}</div>}
    </div>
  );
}

function Row({ title, desc, onEdit, onDelete }: { title: string; desc: string; onEdit: () => void; onDelete: () => void }) {
  return (
    <div className="flex items-center justify-between gap-4 p-4">
      <div className="min-w-0">
        <div className="truncate text-sm font-black text-slate-900">{title}</div>
        <div className="mt-1 text-xs font-bold text-slate-400">{desc}</div>
      </div>
      <div className="flex shrink-0 gap-2">
        <button onClick={onEdit} className="rounded-xl bg-slate-100 p-2 text-slate-600"><Edit3 className="h-4 w-4" /></button>
        <button onClick={onDelete} className="rounded-xl bg-red-50 p-2 text-red-500"><Trash2 className="h-4 w-4" /></button>
      </div>
    </div>
  );
}
