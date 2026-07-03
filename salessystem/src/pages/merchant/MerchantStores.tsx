import { useCallback, useEffect, useMemo, useState } from 'react';
import { Building2, Edit3, Plus, Search, Trash2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { merchantStoreService } from '../../services/modules/merchantStore';
import type { MerchantStore, MerchantStorePayload } from '../../types/merchant';
import { cn } from '../../lib/utils';

const EMPTY_FORM: MerchantStorePayload = {
  storeName: '',
  storeNo: '',
  storeType: 'DIRECT',
  contactName: '',
  contactPhone: '',
  province: '',
  city: '',
  district: '',
  address: '',
  businessHours: '',
  serviceTags: '',
  status: 1,
};

export default function MerchantStores() {
  const { merchantSession } = useAuth();
  const { showToast } = useToast();
  const tenantId = merchantSession?.tenantId;
  const [stores, setStores] = useState<MerchantStore[]>([]);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<string>('ALL');
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState<MerchantStore | null>(null);
  const [form, setForm] = useState<MerchantStorePayload>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const loadStores = useCallback(async () => {
    if (!tenantId) return;
    setLoading(true);
    try {
      const page = await merchantStoreService.listStores(tenantId, {
        current: 1,
        size: 100,
        keyword: keyword.trim() || undefined,
        status: status === 'ALL' ? undefined : Number(status),
      });
      setStores(page.records || []);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '门店列表加载失败', 'error');
    } finally {
      setLoading(false);
    }
  }, [keyword, showToast, status, tenantId]);

  useEffect(() => {
    void loadStores();
  }, [loadStores]);

  const activeCount = useMemo(() => stores.filter((store) => store.status === 1).length, [stores]);

  function openCreate() {
    setEditing(null);
    setForm(EMPTY_FORM);
  }

  function openEdit(store: MerchantStore) {
    setEditing(store);
    setForm({
      storeNo: store.storeNo || '',
      storeName: store.storeName || '',
      storeType: store.storeType || 'DIRECT',
      contactName: store.contactName || '',
      contactPhone: store.contactPhone || '',
      province: store.province || '',
      city: store.city || '',
      district: store.district || '',
      address: store.address || '',
      businessHours: store.businessHours || '',
      serviceTags: store.serviceTags || '',
      status: store.status,
    });
  }

  async function handleSave() {
    if (!tenantId || !form.storeName?.trim()) {
      showToast('请输入门店名称', 'error');
      return;
    }
    setSaving(true);
    try {
      const payload: MerchantStorePayload = {
        ...form,
        storeNo: form.storeNo?.trim() || undefined,
        storeName: form.storeName.trim(),
        storeType: form.storeType?.trim() || 'DIRECT',
        status: Number(form.status ?? 1),
      };
      if (editing) {
        await merchantStoreService.updateStore(tenantId, editing.id, payload);
        showToast('门店已更新', 'success');
      } else {
        await merchantStoreService.createStore(tenantId, payload);
        showToast('门店已创建', 'success');
      }
      setEditing(null);
      setForm(EMPTY_FORM);
      await loadStores();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '门店保存失败', 'error');
    } finally {
      setSaving(false);
    }
  }

  async function handleToggle(store: MerchantStore) {
    if (!tenantId) return;
    try {
      await merchantStoreService.updateStoreStatus(tenantId, store.id, store.status === 1 ? 0 : 1);
      await loadStores();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '状态更新失败', 'error');
    }
  }

  async function handleDelete(store: MerchantStore) {
    if (!tenantId) return;
    try {
      await merchantStoreService.deleteStore(tenantId, store.id);
      showToast('门店已删除', 'success');
      await loadStores();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '门店删除失败', 'error');
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 p-4 md:p-8">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-3xl font-black text-slate-900">门店管理</h1>
          <p className="mt-1 text-sm font-medium text-slate-500">维护线下服务、快递发货和核销相关门店。</p>
        </div>
        <button
          onClick={openCreate}
          className="inline-flex items-center justify-center gap-2 rounded-2xl bg-primary px-5 py-3 text-sm font-black text-white shadow-lg shadow-primary/20"
        >
          <Plus className="h-4 w-4" /> 新建门店
        </button>
      </header>

      <section className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <Metric label="门店总数" value={stores.length} />
        <Metric label="启用门店" value={activeCount} />
        <Metric label="停用门店" value={stores.length - activeCount} />
      </section>

      <section className="flex flex-col gap-3 rounded-[28px] border border-slate-100 bg-white p-4 shadow-sm md:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索门店名称、编号或电话"
            className="w-full rounded-2xl bg-slate-50 py-3 pl-11 pr-4 text-sm font-bold outline-none focus:ring-2 focus:ring-primary/20"
          />
        </div>
        <select
          value={status}
          onChange={(event) => setStatus(event.target.value)}
          className="rounded-2xl bg-slate-50 px-4 py-3 text-sm font-black outline-none"
        >
          <option value="ALL">全部状态</option>
          <option value="1">启用</option>
          <option value="0">停用</option>
        </select>
      </section>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_380px]">
        <section className="overflow-hidden rounded-[28px] border border-slate-100 bg-white shadow-sm">
          {loading ? (
            <div className="p-10 text-center text-sm font-bold text-slate-400">加载中...</div>
          ) : stores.length === 0 ? (
            <div className="p-10 text-center text-sm font-bold text-slate-400">暂无门店</div>
          ) : (
            <div className="divide-y divide-slate-100">
              {stores.map((store) => (
                <div key={store.id} className="grid gap-4 p-5 lg:grid-cols-[minmax(0,1fr)_220px] lg:items-center">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-3">
                      <Building2 className="h-5 w-5 text-primary" />
                      <h3 className="font-black text-slate-900">{store.storeName}</h3>
                      <span className={cn('rounded-full px-3 py-1 text-[10px] font-black', store.status === 1 ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500')}>
                        {store.status === 1 ? '启用' : '停用'}
                      </span>
                    </div>
                    <p className="mt-2 text-xs font-bold text-slate-400">
                      {store.storeNo} · {store.contactPhone || '未填电话'} · {[store.province, store.city, store.district, store.address].filter(Boolean).join('') || '未填地址'}
                    </p>
                  </div>
                  <div className="flex justify-start gap-2 lg:justify-end">
                    <button onClick={() => openEdit(store)} className="rounded-xl bg-slate-100 p-3 text-slate-600 hover:text-primary" title="编辑">
                      <Edit3 className="h-4 w-4" />
                    </button>
                    <button onClick={() => void handleToggle(store)} className="rounded-xl bg-slate-100 px-4 py-3 text-xs font-black text-slate-600">
                      {store.status === 1 ? '停用' : '启用'}
                    </button>
                    <button onClick={() => void handleDelete(store)} className="rounded-xl bg-red-50 p-3 text-red-500" title="删除">
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <StoreForm form={form} editing={editing} saving={saving} onChange={setForm} onSave={() => void handleSave()} />
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-[24px] border border-slate-100 bg-white p-5 shadow-sm">
      <div className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</div>
      <div className="mt-2 text-3xl font-black text-slate-900">{value}</div>
    </div>
  );
}

function StoreForm({
  form,
  editing,
  saving,
  onChange,
  onSave,
}: {
  form: MerchantStorePayload;
  editing: MerchantStore | null;
  saving: boolean;
  onChange: (form: MerchantStorePayload) => void;
  onSave: () => void;
}) {
  const set = (key: keyof MerchantStorePayload, value: string | number) => onChange({ ...form, [key]: value });
  return (
    <aside className="rounded-[28px] border border-slate-100 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-black text-slate-900">{editing ? '编辑门店' : '新建门店'}</h2>
      <div className="mt-5 grid gap-4">
        <Input label="门店名称" value={form.storeName || ''} onChange={(value) => set('storeName', value)} />
        <Input label="门店编号" value={form.storeNo || ''} onChange={(value) => set('storeNo', value)} placeholder="可留空自动生成" />
        <Input label="联系电话" value={form.contactPhone || ''} onChange={(value) => set('contactPhone', value)} />
        <Input label="联系人" value={form.contactName || ''} onChange={(value) => set('contactName', value)} />
        <Input label="地址" value={form.address || ''} onChange={(value) => set('address', value)} />
        <Input label="营业时间" value={form.businessHours || ''} onChange={(value) => set('businessHours', value)} />
        <Input label="服务标签" value={form.serviceTags || ''} onChange={(value) => set('serviceTags', value)} placeholder="核销,自提,售后" />
        <select
          value={form.status ?? 1}
          onChange={(event) => set('status', Number(event.target.value))}
          className="rounded-2xl bg-slate-50 px-4 py-3 text-sm font-black outline-none"
        >
          <option value={1}>启用</option>
          <option value={0}>停用</option>
        </select>
        <button
          onClick={onSave}
          disabled={saving}
          className="rounded-2xl bg-slate-900 px-5 py-4 text-sm font-black text-white disabled:opacity-60"
        >
          {saving ? '保存中...' : '保存门店'}
        </button>
      </div>
    </aside>
  );
}

function Input({
  label,
  value,
  onChange,
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}) {
  return (
    <label className="grid gap-2">
      <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">{label}</span>
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className="rounded-2xl bg-slate-50 px-4 py-3 text-sm font-bold outline-none focus:ring-2 focus:ring-primary/20"
      />
    </label>
  );
}
