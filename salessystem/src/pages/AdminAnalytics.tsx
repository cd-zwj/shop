import { motion } from 'motion/react';
import { 
  LineChart, 
  BarChart as BarChartIcon, 
  PieChart, 
  TrendingUp, 
  Zap, 
  Database, 
  ShieldAlert, 
  Activity, 
  ChevronRight, 
  Search, 
  ArrowUpRight,
  Brain,
  Globe,
  Cpu
} from 'lucide-react';
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer, 
  Cell,
  LineChart as ReLineChart,
  Line
} from 'recharts';
import { cn } from '../lib/utils';

export default function AdminAnalytics() {
  const anomaliesData = [
    { name: 'Mon', count: 12 },
    { name: 'Tue', count: 45, alert: true },
    { name: 'Wed', count: 15 },
    { name: 'Thu', count: 22 },
    { name: 'Fri', count: 86, alert: true },
    { name: 'Sat', count: 32 },
    { name: 'Sun', count: 18 },
  ];

  const trendData = [
    { date: '01/10', current: 4000, previous: 2400 },
    { date: '05/10', current: 3000, previous: 1398 },
    { date: '10/10', current: 2000, previous: 9800 },
    { date: '15/10', current: 2780, previous: 3908 },
    { date: '20/10', current: 1890, previous: 4800 },
    { date: '25/10', current: 2390, previous: 3800 },
    { date: '30/10', current: 3490, previous: 4300 },
  ];

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8">
      <header className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="flex flex-col gap-1">
          <h1 className="text-3xl font-black text-slate-900 tracking-tight">智能引擎与深度分析</h1>
          <p className="text-slate-500 font-medium font-inter">基于 Gemini Pro 驱动的预测性治理与全局流量分析系统。</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="px-4 py-2 bg-primary/10 text-primary border border-primary/20 rounded-xl flex items-center gap-2">
            <div className="w-2 h-2 bg-primary rounded-full animate-pulse" />
            <span className="text-[10px] font-black uppercase tracking-widest font-inter">神经网络监听中</span>
          </div>
        </div>
      </header>

      {/* Hero Analytics Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <motion.div 
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          className="lg:col-span-2 bg-slate-900 rounded-[40px] p-10 text-white shadow-2xl relative overflow-hidden"
        >
          {/* Background visuals */}
          <div className="absolute inset-0 opacity-10">
            <div className="absolute left-0 top-0 w-full h-full border-[1px] border-white/20 translate-x-12 translate-y-12 rounded-full" />
            <div className="absolute left-0 top-0 w-full h-full border-[1px] border-white/20 translate-x-24 translate-y-24 rounded-full" />
          </div>

          <div className="relative z-10">
            <div className="flex items-center gap-4 mb-8">
              <div className="w-14 h-14 bg-white/10 backdrop-blur-xl rounded-2xl flex items-center justify-center border border-white/20 shadow-xl">
                <Globe className="w-8 h-8 text-primary" />
              </div>
              <div>
                <h3 className="text-2xl font-black tracking-tight">全球流量交互趋势</h3>
                <p className="text-slate-400 font-medium text-sm font-inter">跨地区商户实时连接热力图</p>
              </div>
            </div>

            <div className="h-[320px] w-full">
              <ResponsiveContainer width="100%" height="100%">
                 <ReLineChart data={trendData}>
                  <defs>
                    <filter id="shadow" height="200%">
                      <feGaussianBlur in="SourceAlpha" stdDeviation="3" />
                      <feOffset dx="0" dy="4" result="offsetblur" />
                      <feComponentTransfer><feFuncA type="linear" slope="0.5"/></feComponentTransfer>
                      <feMerge><feMergeNode/><feMergeNode in="SourceGraphic"/></feMerge>
                    </filter>
                  </defs>
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#0f172a', border: '1px solid #1e293b', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold' }}
                    itemStyle={{ color: '#fff' }}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="current" 
                    stroke="#0ea5e9" 
                    strokeWidth={4} 
                    dot={false} 
                    activeDot={{ r: 8, stroke: '#fff', strokeWidth: 4 }}
                    filter="url(#shadow)"
                  />
                  <Line 
                    type="monotone" 
                    dataKey="previous" 
                    stroke="#1e293b" 
                    strokeWidth={2} 
                    strokeDasharray="5 5"
                    dot={false} 
                  />
                </ReLineChart>
              </ResponsiveContainer>
            </div>

            <div className="flex gap-10 mt-10 border-t border-white/5 pt-8">
              <div>
                <div className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-1">主节点延迟</div>
                <div className="text-3xl font-black text-white tracking-tight">18<span className="text-sm ml-1 text-slate-400">ms</span></div>
              </div>
              <div>
                <div className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-1">活跃连接</div>
                <div className="text-3xl font-black text-white tracking-tight">248.5<span className="text-sm ml-1 text-slate-400">k</span></div>
              </div>
            </div>
          </div>
        </motion.div>

        <motion.div 
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          className="bg-white rounded-[40px] border border-slate-100 p-10 shadow-xl shadow-slate-200/40 flex flex-col gap-10"
        >
          <div className="flex flex-col gap-3">
            <div className="w-12 h-12 bg-orange-50 text-orange-500 rounded-2xl flex items-center justify-center border border-orange-100">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <h3 className="text-xl font-black text-slate-900">异常检测分析</h3>
            <p className="text-sm text-slate-500 leading-relaxed font-inter font-medium">模型自动标记疑似欺诈或合规风险的交易模式。</p>
          </div>

          <div className="h-[200px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={anomaliesData}>
                <Bar dataKey="count" radius={[8, 8, 0, 0]}>
                  {anomaliesData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.alert ? '#f43f5e' : '#cbd5e1'} />
                  ))}
                </Bar>
                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fontSize: 11, fontWeight: 800, fill: '#94a3b8' }} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="flex flex-col gap-4">
            <div className="p-4 bg-red-50 rounded-2xl flex items-center justify-between group cursor-pointer hover:bg-red-100 transition-colors">
              <div className="flex items-center gap-3">
                <ShieldAlert className="w-5 h-5 text-red-500" />
                <span className="text-sm font-black text-red-700">关键异常: 未经授权的提现</span>
              </div>
              <ChevronRight className="w-4 h-4 text-red-400 group-hover:translate-x-1 transition-transform" />
            </div>
            <button className="w-full py-4 border-2 border-slate-100 text-slate-500 font-black text-sm rounded-2xl hover:bg-slate-50 transition-all">
              配置检测神经网络
            </button>
          </div>
        </motion.div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 mb-10">
        {[
          { label: '处理器负荷', value: '14%', icon: Cpu, color: 'text-primary' },
          { label: '训练数据集', value: '4.2 TB', icon: Database, color: 'text-tertiary' },
          { label: '决策延迟', value: '240ms', icon: Zap, color: 'text-orange-500' },
          { label: 'AI 可信度', value: '98.2%', icon: Brain, color: 'text-indigo-500' }
        ].map((item, i) => (
          <motion.div 
            key={i}
            whileHover={{ y: -4, shadow: '0 20px 25px -5px rgba(0, 0, 0, 0.05)' }}
            className="bg-white border border-slate-100 p-6 rounded-3xl shadow-sm flex items-center gap-6 group cursor-pointer"
          >
            <div className={cn("w-14 h-14 rounded-2xl bg-slate-50 flex items-center justify-center transition-all group-hover:scale-110 shadow-inner", item.color)}>
              <item.icon className="w-7 h-7" />
            </div>
            <div>
              <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{item.label}</p>
              <p className="text-2xl font-black text-slate-900 mt-1">{item.value}</p>
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
