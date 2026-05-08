import { useEffect, useState } from 'react';
import { ArrowLeft, Save } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { adminMerchantService } from '../services/modules/adminMerchant';
import { ApiError } from '../types/api';
import type { AdminMerchantPayload } from '../types/admin';

const EMPTY_FORM: AdminMerchantPayload = {
  tenantCode: '',
  name: '',
  contact: '',
  phone: '',
  address: '',
};

export default function AdminMerchantEditPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const merchantId = Number(id);
  const isEdit = Number.isFinite(merchantId);
  const [formData, setFormData] = useState<AdminMerchantPayload>(EMPTY_FORM);
  const [isLoading, setIsLoading] = useState(isEdit);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    let isMounted = true;

    async function loadMerchant() {
      if (!isEdit) return;

      try {
        const detail = await adminMerchantService.getMerchantDetail(merchantId);
        if (!isMounted) return;
        setFormData({
          tenantCode: detail.tenantCode || '',
          name: detail.name || '',
          contact: detail.contactName || '',
          phone: detail.contactPhone || '',
          address: detail.address || '',
        });
      } catch {
        if (!isMounted) return;
        setError('商户资料加载失败，请稍后重试');
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    void loadMerchant();

    return () => {
      isMounted = false;
    };
  }, [isEdit, merchantId]);

  function updateField<K extends keyof AdminMerchantPayload>(key: K, value: AdminMerchantPayload[K]) {
    setFormData((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSave() {
    if (!formData.tenantCode.trim() || !formData.name.trim()) {
      setError('商户编码和商户名称不能为空');
      return;
    }

    setIsSaving(true);
    setError('');
    setSuccess('');

    try {
      const payload: AdminMerchantPayload = {
        tenantCode: formData.tenantCode.trim(),
        name: formData.name.trim(),
        contact: formData.contact?.trim() || undefined,
        phone: formData.phone?.trim() || undefined,
        address: formData.address?.trim() || undefined,
      };

      if (isEdit) {
        await adminMerchantService.updateMerchant(merchantId, payload);
        setSuccess('商户资料已更新');
        setTimeout(() => navigate(`/admin/merchant/${merchantId}`), 500);
      } else {
        const created = await adminMerchantService.createMerchant(payload);
        setSuccess('商户创建成功');
        setTimeout(() => navigate(`/admin/merchant/${created.id}`), 500);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '商户保存失败，请稍后重试');
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 p-4 pb-32 md:p-8">
      <header className="flex flex-col justify-between gap-6 sm:flex-row sm:items-end">
        <div>
          <button
            onClick={() => navigate(-1)}
            className="mb-3 flex items-center gap-2 text-xs font-black uppercase tracking-widest text-primary transition-all hover:gap-3"
          >
            <ArrowLeft className="h-4 w-4" /> 返回商户列表
          </button>
          <h1 className="text-4xl font-black tracking-tight text-slate-900">
            {isEdit ? '编辑商户资料' : '创建新商户'}
          </h1>
          <p className="mt-1 text-sm font-medium text-slate-500">
            当前表单与 `/v1/admin/merchants` 的创建和更新接口直接对应。
          </p>
        </div>
        <button
          onClick={handleSave}
          disabled={isLoading || isSaving}
          className="flex items-center gap-2 rounded-[24px] bg-primary px-8 py-4 text-base font-black text-white shadow-2xl shadow-primary/20 transition-all hover:scale-105 disabled:cursor-not-allowed disabled:opacity-70"
        >
          <Save className="h-5 w-5" /> {isSaving ? '保存中...' : '保存商户'}
        </button>
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

      <section className="rounded-[40px] border border-slate-100 bg-white p-8 shadow-sm">
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <Field
            label="商户编码"
            value={formData.tenantCode}
            onChange={(value) => updateField('tenantCode', value)}
            placeholder="例如 tenant-techmart"
            disabled={isLoading}
          />
          <Field
            label="商户名称"
            value={formData.name}
            onChange={(value) => updateField('name', value)}
            placeholder="例如 TechMart"
            disabled={isLoading}
          />
          <Field
            label="联系人"
            value={formData.contact || ''}
            onChange={(value) => updateField('contact', value)}
            placeholder="例如 张经理"
            disabled={isLoading}
          />
          <Field
            label="联系电话"
            value={formData.phone || ''}
            onChange={(value) => updateField('phone', value)}
            placeholder="例如 13800000000"
            disabled={isLoading}
          />
          <Field
            label="联系地址"
            value={formData.address || ''}
            onChange={(value) => updateField('address', value)}
            placeholder="例如 上海市浦东新区..."
            disabled={isLoading}
            className="md:col-span-2"
          />
        </div>
      </section>
    </div>
  );
}

function Field({
  label,
  value,
  onChange,
  placeholder,
  disabled,
  className = '',
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  disabled?: boolean;
  className?: string;
}) {
  return (
    <div className={className}>
      <label className="ml-1 text-[10px] font-black uppercase tracking-widest text-slate-400">
        {label}
      </label>
      <input
        type="text"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        className="mt-3 w-full rounded-[20px] border-2 border-slate-50 bg-slate-50 px-6 py-4 font-black text-slate-900 outline-none transition-all focus:border-primary focus:bg-white disabled:cursor-not-allowed disabled:opacity-70"
      />
    </div>
  );
}
