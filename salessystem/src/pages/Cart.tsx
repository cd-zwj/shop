import { useState } from 'react';
import { motion } from 'motion/react';
import { 
  Trash2, 
  Plus, 
  Minus, 
  Store, 
  ArrowRight,
  ShoppingBag
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cn } from '../lib/utils';

export default function Cart() {
  const navigate = useNavigate();
  const [items, setItems] = useState([
    {
      id: 1,
      name: '专业无线降噪耳机',
      store: '科技电子城',
      options: '哑光黑 • 标准版',
      price: 299,
      quantity: 1,
      image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuC_-tdO6zIEAZ5ngASindCeQeaZa60kMTgNa4zX2_R8JM0cNL-7M6z1_jTgYuZ2ICkCckhPQukuFDo770EdxiKdNi2cSsgmBW2JGLtYN9MpDUCke4NJvJg02H2q9kJFIMCUWffYfYNp3flDlyXr7M_anZIfLsKlCbyZV98Xa-qYi06TwmoT0_IVC6aqt7ZrG156YuUsbj1N0m5CmKTDpoF7R9lK0jgs-tzYHy9owqS2cdoJwXERCGSW-D3484BB69Guw-jD_0-GarM'
    },
    {
      id: 2,
      name: 'Series 8 智能手表',
      store: '科技电子城',
      options: '石墨不锈钢 • 午夜色表带',
      price: 399,
      quantity: 2,
      image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuC6FwUC_ai_4Iz9Kdt7wpsSvERAjxgXJBErFHLhXigfTvuIAMHpRgAGzd-qKtLTp0S31ZJXt0PxAwda8DgM_53Zr3_HYjI-Edzl4uiQ4JGmu58rndhfZKw6pQZaSw0FGAvl9-kKAyhzyan0sFzZBCfLqReXaDyQ1tnRhHjItXMLQtV7Dxj0ZcaFTubtLJ8QS6d_ICz2-E8rAx_F9dX61OomGXE-A_ZkLCxdKuyzFvzqxPJ88r8-HHdKyNorMMn9_fR0bMb7G_2s06Y'
    },
    {
      id: 3,
      name: '商务真皮笔记本',
      store: '办公用品精选',
      options: 'A5 尺寸 • 横线内页',
      price: 45,
      quantity: 3,
      image: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBQWURNhYrJ8jFlhOQvL98w85PlVeMWyagyEeViTvKCyGg7yYKQ7Pl5YI0sswk1E5w7m2PK72eYCUvOysMPCErqIubURma43yyoSBvfXT-sucnSpc5qTJEk6e8IcYV6ZkeKQjJnXMvSBhSfcS7d3O1wB2bV0JmWsSHBowTd76dfyoyObE6EislOCZ1WoZsMZP8GaoR41YzG66EghP1jHYZ6Hg7b0iovHR3BXeG7cCKGKR3doz_5xpGbLMziiAyaAocR5ZjiPzsXpoU'
    }
  ]);

  const updateQuantity = (id: number, delta: number) => {
    setItems(items.map(item => 
      item.id === id ? { ...item, quantity: Math.max(1, item.quantity + delta) } : item
    ));
  };

  const removeItem = (id: number) => {
    setItems(items.filter(item => item.id !== id));
  };

  const subtotal = items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
  const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);

  // Group items by store
  const stores = [...new Set(items.map(item => item.store))];

  return (
    <div className="flex flex-col gap-8 pb-40 px-4 md:mt-8 max-w-4xl mx-auto w-full">
      <header className="flex flex-col gap-2">
        <h1 className="text-4xl font-black text-slate-900 tracking-tight">购物车</h1>
        <p className="text-slate-500 font-medium font-inter">在去结算前核对您的商品。</p>
      </header>

      {items.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-slate-100 shadow-inner">
          <ShoppingBag className="w-16 h-16 text-slate-200 mb-4" />
          <p className="text-slate-400 font-bold">购物车空空如也</p>
          <button onClick={() => navigate('/discovery')} className="text-primary font-bold mt-4 hover:underline">去逛逛</button>
        </div>
      ) : (
        <div className="flex flex-col gap-10">
          {stores.map(storeName => (
            <motion.section 
              key={storeName}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden"
            >
              <div className="bg-slate-50/80 p-5 border-b border-slate-200 flex items-center gap-3">
                <div className="p-2 bg-white rounded-xl shadow-sm border border-slate-100">
                  <Store className="w-5 h-5 text-primary" />
                </div>
                <h2 className="text-lg font-black text-slate-900 tracking-tight">{storeName}</h2>
              </div>
              
              <div className="p-2 flex flex-col divide-y divide-slate-100">
                {items.filter(item => item.store === storeName).map(item => (
                  <article key={item.id} className="p-4 flex flex-col sm:flex-row gap-5 group">
                    <div className="w-full sm:w-32 h-32 rounded-2xl overflow-hidden shrink-0 bg-slate-50 border border-slate-100 shadow-inner relative">
                      <img src={item.image} alt={item.name} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-700" />
                    </div>
                    
                    <div className="flex-1 flex flex-col justify-between">
                      <div className="flex justify-between items-start gap-4">
                        <div>
                          <h3 className="text-base font-bold text-slate-900 mb-1 group-hover:text-primary transition-colors cursor-pointer">{item.name}</h3>
                          <p className="text-xs font-semibold text-slate-400 uppercase tracking-widest">{item.options}</p>
                        </div>
                        <button 
                          onClick={() => removeItem(item.id)}
                          className="text-slate-300 hover:text-red-500 transition-colors p-2 hover:bg-red-50 rounded-xl"
                        >
                          <Trash2 className="w-5 h-5" />
                        </button>
                      </div>
                      
                      <div className="flex items-end justify-between mt-6">
                        <span className="text-2xl font-black text-slate-900 tracking-tight">${item.price}</span>
                        <div className="flex items-center bg-slate-100 rounded-2xl border border-slate-200 p-1">
                          <button 
                            onClick={() => updateQuantity(item.id, -1)}
                            className="w-10 h-10 flex items-center justify-center text-slate-600 hover:bg-white hover:shadow-sm rounded-xl transition-all active:scale-90"
                          >
                            <Minus className="w-4 h-4" />
                          </button>
                          <span className="text-base font-black text-slate-900 w-12 text-center">{item.quantity}</span>
                          <button 
                            onClick={() => updateQuantity(item.id, 1)}
                            className="w-10 h-10 flex items-center justify-center text-slate-600 hover:bg-white hover:shadow-sm rounded-xl transition-all active:scale-90"
                          >
                            <Plus className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            </motion.section>
          ))}
        </div>
      )}

      {/* Sticky Bottom Summary */}
      <div className="fixed bottom-20 md:bottom-6 left-1/2 -translate-x-1/2 w-full max-w-4xl px-4 z-40">
        <motion.div 
          initial={{ y: 100 }}
          animate={{ y: 0 }}
          className="bg-white/95 backdrop-blur-2xl border border-slate-200 shadow-2xl rounded-3xl p-6 flex flex-col sm:flex-row items-center justify-between gap-6"
        >
          <div className="flex flex-col items-center sm:items-start w-full sm:w-auto">
            <span className="text-sm font-semibold text-slate-400 uppercase tracking-widest">总计 ({totalItems} 件商品)</span>
            <div className="flex items-baseline gap-1 mt-1">
              <span className="text-3xl font-black text-slate-900 tracking-tight">${subtotal.toLocaleString()}</span>
            </div>
          </div>
          
          <button 
            disabled={items.length === 0}
            onClick={() => navigate('/success')}
            className="w-full sm:w-auto px-12 py-4 bg-primary text-white rounded-2xl font-black text-lg shadow-xl shadow-primary/20 hover:shadow-2xl hover:scale-[1.02] active:scale-95 transition-all flex items-center justify-center gap-3 disabled:opacity-50 disabled:grayscale"
          >
            <span>去结算</span>
            <ArrowRight className="w-5 h-5" />
          </button>
        </motion.div>
      </div>
    </div>
  );
}
