import { type FormEvent, useCallback, useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  ArrowLeft,
  Plus,
  MapPin,
  Star,
  Trash2,
  Edit3,
  X,
  Check,
  Phone,
  User,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useToast } from '../context/ToastContext';
import { appAddressService } from '../services/modules/appAddress';
import type { Address, AddressPayload } from '../types/addressNotification';
import { EmptyState } from '../components/ui/EmptyState';
import { cn } from '../lib/utils';

/* ------------------------------------------------------------------ */
/*  Address Form Modal                                                 */
/* ------------------------------------------------------------------ */

interface AddressFormModalProps {
  open: boolean;
  initial?: Address | null;
  onClose: () => void;
  onSaved: () => void;
}

function AddressFormModal({ open, initial, onClose, onSaved }: AddressFormModalProps) {
  const { showToast } = useToast();
  const isEdit = Boolean(initial);

  const [receiverName, setReceiverName] = useState('');
  const [phone, setPhone] = useState('');
  const [province, setProvince] = useState('');
  const [city, setCity] = useState('');
  const [district, setDistrict] = useState('');
  const [detail, setDetail] = useState('');
  const [isDefault, setIsDefault] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  /* Sync form when initial changes */
  useEffect(() => {
    if (initial) {
      setReceiverName(initial.receiverName);
      setPhone(initial.phone);
      setProvince(initial.province ?? '');
      setCity(initial.city ?? '');
      setDistrict(initial.district ?? '');
      setDetail(initial.detail);
      setIsDefault(initial.isDefault === 1);
    } else {
      setReceiverName('');
      setPhone('');
      setProvince('');
      setCity('');
      setDistrict('');
      setDetail('');
      setIsDefault(false);
    }
  }, [initial, open]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!receiverName.trim()) { showToast('请输入收货人姓名', 'error'); return; }
    if (!phone.trim()) { showToast('请输入手机号', 'error'); return; }
    if (!detail.trim()) { showToast('请输入详细地址', 'error'); return; }

    const payload: AddressPayload = {
      receiverName: receiverName.trim(),
      phone: phone.trim(),
      province: province.trim() || undefined,
      city: city.trim() || undefined,
      district: district.trim() || undefined,
      detail: detail.trim(),
      isDefault: isDefault ? 1 : 0,
    };

    setSubmitting(true);
    try {
      if (isEdit && initial) {
        await appAddressService.update(initial.id, payload);
        showToast('地址已更新', 'success');
      } else {
        await appAddressService.create(payload);
        showToast('地址已添加', 'success');
      }
      onSaved();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '操作失败', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (!open) return null;

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[80] flex items-end justify-center sm:items-center"
        >
          <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" onClick={onClose} />
          <motion.div
            initial={{ y: '100%' }}
            animate={{ y: 0 }}
            exit={{ y: '100%' }}
            transition={{ type: 'spring', damping: 30, stiffness: 300 }}
            className="relative z-10 w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-t-3xl sm:rounded-3xl bg-white shadow-2xl"
          >
            {/* Header */}
            <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-100 bg-white px-6 py-4">
              <h2 className="text-lg font-black text-slate-900">
                {isEdit ? '编辑地址' : '新增地址'}
              </h2>
              <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-50 transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="flex flex-col gap-4 p-6">
              {/* Receiver Name */}
              <label className="flex flex-col gap-1.5">
                <span className="text-xs font-bold text-slate-500">收货人姓名 *</span>
                <div className="relative">
                  <User className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                  <input
                    value={receiverName}
                    onChange={(e) => setReceiverName(e.target.value)}
                    placeholder="请输入收货人姓名"
                    className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-10 pr-4 text-sm font-medium text-slate-800 outline-none transition-all focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/10"
                  />
                </div>
              </label>

              {/* Phone */}
              <label className="flex flex-col gap-1.5">
                <span className="text-xs font-bold text-slate-500">手机号 *</span>
                <div className="relative">
                  <Phone className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
                  <input
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="请输入手机号"
                    className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-10 pr-4 text-sm font-medium text-slate-800 outline-none transition-all focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/10"
                  />
                </div>
              </label>

              {/* Province / City / District */}
              <div className="grid grid-cols-3 gap-3">
                {[
                  { label: '省份', value: province, set: setProvince, placeholder: '省份' },
                  { label: '城市', value: city, set: setCity, placeholder: '城市' },
                  { label: '区/县', value: district, set: setDistrict, placeholder: '区/县' },
                ].map((f) => (
                  <label key={f.label} className="flex flex-col gap-1.5">
                    <span className="text-xs font-bold text-slate-500">{f.label}</span>
                    <input
                      value={f.value}
                      onChange={(e) => f.set(e.target.value)}
                      placeholder={f.placeholder}
                      className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 px-3.5 text-sm font-medium text-slate-800 outline-none transition-all focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/10"
                    />
                  </label>
                ))}
              </div>

              {/* Detail Address */}
              <label className="flex flex-col gap-1.5">
                <span className="text-xs font-bold text-slate-500">详细地址 *</span>
                <textarea
                  value={detail}
                  onChange={(e) => setDetail(e.target.value)}
                  placeholder="街道、门牌号、小区等详细信息"
                  rows={3}
                  className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 px-4 text-sm font-medium text-slate-800 outline-none transition-all focus:border-primary focus:bg-white focus:ring-2 focus:ring-primary/10 resize-none"
                />
              </label>

              {/* Default Switch */}
              <button
                type="button"
                onClick={() => setIsDefault(!isDefault)}
                className="flex items-center gap-3 py-3 px-1"
              >
                <div
                  className={cn(
                    'w-5 h-5 rounded-lg border-2 flex items-center justify-center transition-all',
                    isDefault
                      ? 'bg-primary border-primary text-white'
                      : 'border-slate-300 text-transparent'
                  )}
                >
                  <Check className="w-3.5 h-3.5" />
                </div>
                <span className="text-sm font-bold text-slate-700">设为默认地址</span>
              </button>

              {/* Submit */}
              <button
                type="submit"
                disabled={submitting}
                className="w-full rounded-2xl bg-primary py-4 text-sm font-black text-white shadow-xl shadow-primary/20 transition-all hover:scale-[1.01] active:scale-[0.98] disabled:opacity-60"
              >
                {submitting ? (
                  <span className="flex items-center justify-center gap-2">
                    <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    保存中...
                  </span>
                ) : isEdit ? (
                  '保存修改'
                ) : (
                  '添加地址'
                )}
              </button>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

/* ------------------------------------------------------------------ */
/*  Address List Page                                                  */
/* ------------------------------------------------------------------ */

export default function AddressList() {
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [addresses, setAddresses] = useState<Address[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Modal state
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Address | null>(null);

  const loadAddresses = useCallback(async () => {
    try {
      const data = await appAddressService.list();
      setAddresses(data ?? []);
    } catch (err) {
      showToast(err instanceof Error ? err.message : '加载地址失败', 'error');
    } finally {
      setIsLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    void loadAddresses();
  }, [loadAddresses]);

  const handleDelete = async (id: number) => {
    if (!window.confirm('确定删除该地址吗？')) return;
    try {
      await appAddressService.remove(id);
      showToast('地址已删除', 'success');
      await loadAddresses();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '删除失败', 'error');
    }
  };

  const handleSetDefault = async (id: number) => {
    try {
      await appAddressService.setDefault(id);
      showToast('已设为默认地址', 'success');
      await loadAddresses();
    } catch (err) {
      showToast(err instanceof Error ? err.message : '设置失败', 'error');
    }
  };

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };

  const openEdit = (addr: Address) => {
    setEditing(addr);
    setModalOpen(true);
  };

  const formatFullAddress = (addr: Address) => {
    const parts = [addr.province, addr.city, addr.district, addr.detail].filter(Boolean);
    return parts.join(' ');
  };

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 pb-12 md:mt-8">
      {/* Header */}
      <header className="flex items-center justify-between border-b border-slate-100 pb-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="p-2 text-slate-600 hover:bg-slate-50 dark:hover:bg-slate-800 rounded-full transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-black text-slate-900 dark:text-white">收货地址</h1>
            <p className="text-xs font-semibold text-slate-400 mt-0.5">
              管理您的配送地址信息
            </p>
          </div>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center gap-2 rounded-2xl bg-primary px-4 py-2.5 text-xs font-bold text-white shadow-lg shadow-primary/20 transition-all hover:scale-[1.02] active:scale-95"
        >
          <Plus className="w-4 h-4" />
          新增
        </button>
      </header>

      {/* Content */}
      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-20 gap-3 text-slate-400">
          <div className="w-8 h-8 border-2 border-primary/20 border-t-primary rounded-full animate-spin" />
          <span className="text-sm font-medium">加载地址中...</span>
        </div>
      ) : addresses.length === 0 ? (
        <EmptyState
          icon={<MapPin className="w-12 h-12" />}
          title="暂无收货地址"
          subtitle="点击右上角「新增」按钮添加您的第一个收货地址"
        />
      ) : (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col gap-3"
        >
          {addresses.map((addr) => (
            <motion.div
              key={addr.id}
              layout
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              className="group relative overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-sm transition-all hover:shadow-md"
            >
              <div className="flex items-start gap-4 p-5">
                {/* Icon */}
                <div
                  className={cn(
                    'flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl',
                    addr.isDefault === 1
                      ? 'bg-primary/10 text-primary'
                      : 'bg-slate-50 text-slate-400'
                  )}
                >
                  <MapPin className="h-5 w-5" />
                </div>

                {/* Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-sm font-black text-slate-900">{addr.receiverName}</span>
                    <span className="text-xs font-medium text-slate-400">{addr.phone}</span>
                    {addr.isDefault === 1 && (
                      <span className="inline-flex items-center gap-1 rounded-lg bg-primary/10 px-2 py-0.5 text-[10px] font-black text-primary">
                        <Star className="w-3 h-3 fill-primary" />
                        默认
                      </span>
                    )}
                  </div>
                  <p className="text-xs font-medium text-slate-500 leading-relaxed">
                    {formatFullAddress(addr)}
                  </p>
                </div>

                {/* Actions */}
                <div className="flex items-center gap-1 shrink-0">
                  <button
                    onClick={() => openEdit(addr)}
                    className="p-2 text-slate-400 hover:text-primary hover:bg-primary/5 rounded-xl transition-colors"
                    title="编辑"
                  >
                    <Edit3 className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDelete(addr.id)}
                    className="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-xl transition-colors"
                    title="删除"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Set Default Button (only when not default) */}
              {addr.isDefault !== 1 && (
                <div className="border-t border-slate-50 px-5 py-3">
                  <button
                    onClick={() => handleSetDefault(addr.id)}
                    className="text-xs font-bold text-slate-400 hover:text-primary transition-colors"
                  >
                    设为默认地址
                  </button>
                </div>
              )}
            </motion.div>
          ))}
        </motion.div>
      )}

      {/* Address Form Modal */}
      <AddressFormModal
        open={modalOpen}
        initial={editing}
        onClose={() => setModalOpen(false)}
        onSaved={() => {
          setModalOpen(false);
          void loadAddresses();
        }}
      />
    </div>
  );
}
