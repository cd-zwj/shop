import { useState } from 'react';
import { Ticket, Sparkles, ShieldAlert } from 'lucide-react';
import AdminCouponsTab from './admin/AdminCouponsTab';
import AdminActivitiesTab from './admin/AdminActivitiesTab';
import { cn } from '../lib/utils';

export default function AdminMarketing() {
  const [activeMode, setActiveMode] = useState<'COUPONS' | 'ACTIVITIES'>('COUPONS');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const filterTabs = [
    { id: 'ALL', label: '全部' },
    { id: 'DRAFT', label: '草稿' },
    { id: 'ACTIVE', label: '已上线' },
    { id: 'DISABLED', label: '已下线' },
  ];

  return (
    <div className="flex flex-col gap-6 p-6 pb-20">
      <header className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-black tracking-tight text-slate-900 flex items-center gap-2">
            <ShieldAlert className="text-primary" />
            平台营销中心
          </h1>
          <p className="text-sm font-medium text-slate-500">
            管理员后台：在此管理全平台级别的通用优惠券模板及大促促销活动。
          </p>
        </div>
      </header>

      {/* Main Tab Options */}
      <div className="flex bg-slate-100 p-1.5 rounded-2xl w-fit">
        <button
          onClick={() => {
            setActiveMode('COUPONS');
            setStatusFilter('ALL');
          }}
          className={cn(
            "flex items-center gap-2 px-6 py-2.5 rounded-xl text-xs font-black uppercase tracking-widest transition-all",
            activeMode === 'COUPONS' ? "bg-white text-primary shadow-sm" : "text-slate-400 hover:text-slate-600"
          )}
        >
          <Ticket size={14} />
          平台优惠券
        </button>
        <button
          onClick={() => {
            setActiveMode('ACTIVITIES');
            setStatusFilter('ALL');
          }}
          className={cn(
            "flex items-center gap-2 px-6 py-2.5 rounded-xl text-xs font-black uppercase tracking-widest transition-all",
            activeMode === 'ACTIVITIES' ? "bg-white text-primary shadow-sm" : "text-slate-400 hover:text-slate-600"
          )}
        >
          <Sparkles size={14} />
          平台活动
        </button>
      </div>

      {/* Status Filters */}
      <div className="flex border-b border-slate-100 overflow-x-auto hide-scrollbar">
        {filterTabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setStatusFilter(tab.id)}
            className={`whitespace-nowrap px-6 pb-4 text-sm font-bold border-b-2 transition-all ${
              statusFilter === tab.id
                ? 'border-primary text-primary font-black scale-[1.02]'
                : 'border-transparent text-slate-400 hover:text-slate-600'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="mt-4">
        {activeMode === 'COUPONS' ? (
          <AdminCouponsTab statusFilter={statusFilter} />
        ) : (
          <AdminActivitiesTab statusFilter={statusFilter} />
        )}
      </div>
    </div>
  );
}
