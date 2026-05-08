import { motion } from 'motion/react';
import { 
  Plus, 
  Search, 
  Filter, 
  MoreHorizontal, 
  Package, 
  Tag, 
  LayoutGrid, 
  List, 
  ChevronRight,
  Eye,
  Edit3,
  Trash2,
  ExternalLink,
  ArrowUpDown,
  History
} from 'lucide-react';
import { cn } from '../lib/utils';

export default function AdminProducts() {
  const products = [
    { id: '1', name: 'Premium Analytics Suite', slug: 'enterprise-analytics', price: '$4,999.00', status: '销售中', stock: '245', category: '软件' },
    { id: '2', name: 'Artisan Mechanical Keyboard', slug: 'custom-board-v2', price: '$299.00', status: '已售罄', stock: '0', category: '硬件' },
    { id: '3', name: 'Cloud Infrastructure Credits', slug: 'aws-credits-gold', price: '$1,500.00', status: '预售', stock: '∞', category: '额度' },
    { id: '4', name: 'Design System Masterclass', slug: 'ui-ux-design-pro', price: '$49.00', status: '销售中', stock: '1,200', category: '教育' },
    { id: '5', name: 'Portable SSD 2TB', slug: 'speed-drive-gen2', price: '$189.00', status: '下架', stock: '56', category: '硬件' },
  ];

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-black text-slate-900 tracking-tight">产品编排</h1>
          <p className="text-slate-500 font-medium font-inter">跨类目动态管理库存、定价与分发策略。</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex bg-slate-100 p-1 rounded-xl">
            <button className="p-2 bg-white text-primary rounded-lg shadow-sm"><LayoutGrid className="w-4 h-4" /></button>
            <button className="p-2 text-slate-400 hover:text-slate-600 transition-colors"><List className="w-4 h-4" /></button>
          </div>
          <button className="flex items-center gap-2 px-5 py-3 bg-primary text-white rounded-xl text-sm font-black shadow-lg shadow-primary/20 hover:bg-primary-container transition-all">
            <Plus className="w-4 h-4" /> 发布新产品
          </button>
        </div>
      </header>

      {/* Main Container */}
      <div className="bg-white rounded-[32px] border border-slate-100 shadow-2xl shadow-slate-100/40 overflow-hidden flex flex-col">
        {/* Table Toolbar */}
        <div className="p-6 border-b border-slate-50 flex flex-col sm:flex-row items-center gap-4">
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
            <input 
              type="text" 
              placeholder="搜索产品名称、SKU 或标签..." 
              className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-transparent rounded-2xl text-sm outline-none focus:bg-white focus:border-primary/20 transition-all font-inter"
            />
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <button className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-xs font-black text-slate-500 uppercase tracking-widest hover:bg-slate-50 transition-all shadow-sm">
              <Filter className="w-3.5 h-3.5" /> 筛选条件
            </button>
            <button className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-xs font-black text-slate-500 uppercase tracking-widest hover:bg-slate-50 transition-all shadow-sm">
              按时间排序 <ArrowUpDown className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Interactive Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-slate-50/50">
                <th className="px-8 py-5">
                   <div className="flex items-center gap-2">
                    <input type="checkbox" className="w-4 h-4 rounded border-slate-300 text-primary focus:ring-primary" />
                    <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest italic">产品目录</span>
                   </div>
                </th>
                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest italic">定价 (USD)</th>
                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest italic text-center">状态</th>
                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest italic">库存</th>
                <th className="px-8 py-5 text-[10px] font-black text-slate-400 uppercase tracking-widest italic">类目</th>
                <th className="px-8 py-5"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {products.map((p) => (
                <motion.tr 
                  key={p.id}
                  whileHover={{ backgroundColor: '#fcfdfe' }}
                  className="group transition-colors cursor-pointer"
                >
                  <td className="px-8 py-6">
                    <div className="flex items-center gap-4">
                      <div className="w-14 h-14 bg-slate-50 border border-slate-100 rounded-2xl flex items-center justify-center p-2 shadow-inner group-hover:scale-105 transition-transform duration-500 overflow-hidden">
                        <img 
                           src={p.id === '1' ? 'https://lh3.googleusercontent.com/aida-public/AB6AXuD7qFvrmqGDR8XNqpySzZEPCekU6EES3bLQSTidMXbagLOpGERWCDQI1JifZXYXRSuHnKAbJcUrhcGXtOoLFMlxzH_WCrfsDE2Xb-eVkefmHtZbwe6UzCM72gvyUf04Z3bT4ochdndGj1-aqtULm7Cf2YxriCkLDnwvWBWW2zLwdVJODnwxyLO5Gl1I0e9YXgizkleiCZM7ikEgV0298qn104jlH05VjrTpn-Zf1C5fQ-8-HNP0t4qKPCb6pStUJmeK-UsEnYa6qx0' : 'https://lh3.googleusercontent.com/aida-public/AB6AXu AjTYcN50633_JIWo12BJkPahQEWOpKR96iK9XJ2XXU6wLOtfSwSyuJqiBn3J7cin6fvPM4Rws-BOUSxhlUfMeLF9dnIh1cW3qQeXYHBhELaki36msvHGdx8fkoICG8Bocv49JgRVA9Raow7sryVGLMEf-ZMUjbxDqvZ0P-jEbp0_UFZNQvn7gVJq-ExctaYqoVfJTX-PNH8HeJrF5pejO9w-IX3I34DuZdYoer_BkYSXpMOhlRnOXttmJOG43ZUZkDlDxqj8YNp2Y'}
                           alt=""
                           className="w-full h-full object-cover rounded-lg"
                        />
                      </div>
                      <div className="flex flex-col gap-0.5">
                        <span className="text-[15px] font-black text-slate-900 tracking-tight group-hover:text-primary transition-colors">{p.name}</span>
                        <span className="text-[11px] font-bold text-slate-400 font-inter font-mono uppercase">/{p.slug}</span>
                      </div>
                    </div>
                  </td>
                  <td className="px-8 py-6 font-black text-slate-900 tracking-tight">{p.price}</td>
                  <td className="px-8 py-6">
                    <div className="flex justify-center">
                      <span className={cn(
                        "px-3 py-1.5 text-[10px] font-black rounded-lg uppercase tracking-widest border shadow-sm",
                        p.status === '销售中' ? "bg-green-50 text-green-600 border-green-100" :
                        p.status === '已售罄' ? "bg-red-50 text-red-600 border-red-100" :
                        p.status === '预售' ? "bg-blue-50 text-blue-600 border-blue-100" :
                        "bg-slate-100 text-slate-500 border-slate-200"
                      )}>
                        {p.status}
                      </span>
                    </div>
                  </td>
                  <td className="px-8 py-6">
                    <span className={cn("text-sm font-bold", p.stock === '0' ? "text-red-500" : "text-slate-500")}>
                      {p.stock} <span className="text-[11px] font-medium opacity-50 ml-1">当前</span>
                    </span>
                  </td>
                  <td className="px-8 py-6">
                    <div className="flex items-center gap-2">
                       <div className="w-2 h-2 rounded-full bg-primary" />
                       <span className="text-xs font-bold text-slate-800">{p.category}</span>
                    </div>
                  </td>
                  <td className="px-8 py-6">
                    <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button className="p-2 text-slate-400 hover:text-primary hover:bg-primary/5 rounded-xl transition-all shadow-sm"><Eye size={18} /></button>
                      <button className="p-2 text-slate-400 hover:text-primary hover:bg-primary/5 rounded-xl transition-all shadow-sm"><Edit3 size={18} /></button>
                      <button className="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-xl transition-all shadow-sm"><Trash2 size={18} /></button>
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="p-8 bg-slate-50/50 border-t border-slate-50 flex items-center justify-between">
          <span className="text-xs font-black text-slate-400 uppercase tracking-widest">显示 1-5 / 共 1,248 件商品</span>
          <div className="flex gap-2">
            <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-white border border-slate-200 text-slate-400 hover:bg-slate-50 transition-all shadow-sm" disabled><ChevronRight className="w-5 h-5 rotate-180" /></button>
            <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-primary text-white font-black text-xs shadow-md">1</button>
            <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-white border border-slate-200 text-slate-900 font-black text-xs hover:bg-slate-50 transition-all shadow-sm">2</button>
            <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-white border border-slate-200 text-slate-900 font-black text-xs hover:bg-slate-50 transition-all shadow-sm">3</button>
            <button className="w-10 h-10 flex items-center justify-center rounded-xl bg-white border border-slate-200 text-slate-400 hover:bg-slate-50 transition-all shadow-sm"><ChevronRight className="w-5 h-5" /></button>
          </div>
        </div>
      </div>
    </div>
  );
}
