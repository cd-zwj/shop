import { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Bot, 
  Send, 
  Sparkles, 
  User, 
  ShoppingBag, 
  TrendingUp, 
  BrainCircuit, 
  MessageCircle,
  PlusCircle,
  ThumbsUp,
  ThumbsDown,
  RefreshCw,
  Terminal,
  Zap,
  Info
} from 'lucide-react';
import { cn } from '../lib/utils';

interface Message {
  id: string;
  role: 'user' | 'ai';
  content: string;
  type?: 'text' | 'recommendation' | 'insight';
  data?: any;
}

export default function AIAssistant() {
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<Message[]>([
    { 
      id: '1', 
      role: 'ai', 
      content: '你好！我是你的 AI 消费助手。我可以帮你分析支出、推荐商品或回答关于 SalesSystem 的任何问题。',
      type: 'text'
    },
    {
      id: '2',
      role: 'ai',
      type: 'insight',
      content: '基于您上周的消费数据，我们发现您的“餐饮”支出比平时高出 12%，建议您可以关注部分餐饮店的限时优惠券。',
      data: { trend: '+12%', category: '餐饮' }
    }
  ]);
  const [isTyping, setIsTyping] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, isTyping]);

  const handleSend = () => {
    if (!input.trim()) return;
    
    const userMsg: Message = { id: Date.now().toString(), role: 'user', content: input };
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setIsTyping(true);

    // Simulate AI response
    setTimeout(() => {
      setIsTyping(false);
      const aiMsg: Message = { 
        id: (Date.now() + 1).toString(), 
        role: 'ai', 
        content: '我正在分析你的请求并连接实时数据源。基于您的偏好，以下是一些为您精选的商品：',
        type: 'recommendation'
      };
      setMessages(prev => [...prev, aiMsg]);
    }, 1500);
  };

  return (
    <div className="flex flex-col h-[calc(100vh-144px)] md:h-[calc(100vh-100px)] max-w-4xl mx-auto w-full px-4 pt-4 md:mt-4">
      {/* AI Stats / Info Bar */}
      <div className="bg-gradient-to-r from-primary to-indigo-600 rounded-2xl p-4 flex items-center justify-between shadow-lg shadow-primary/20 mb-6 text-white shrink-0">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 bg-white/20 backdrop-blur-md rounded-xl flex items-center justify-center border border-white/20">
            <BrainCircuit className="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 className="text-lg font-black tracking-tight leading-none mb-1">Gemini Pro 引擎</h2>
            <div className="flex items-center gap-1.5 opacity-80">
              <span className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
              <span className="text-[10px] font-bold uppercase tracking-wider">实时分析活跃中</span>
            </div>
          </div>
        </div>
        <div className="hidden sm:flex gap-4">
          <div className="text-right">
            <p className="text-[10px] font-bold opacity-60 uppercase tracking-widest">今日处理请求</p>
            <p className="text-xl font-black">1.2k</p>
          </div>
          <div className="text-right border-l border-white/10 pl-4">
            <p className="text-[10px] font-bold opacity-60 uppercase tracking-widest">平均响应时间</p>
            <p className="text-xl font-black">0.8s</p>
          </div>
        </div>
      </div>

      {/* Chat Area */}
      <div 
        ref={scrollRef}
        className="flex-1 overflow-y-auto mb-6 flex flex-col gap-8 pr-2 hide-scrollbar scroll-smooth"
      >
        <AnimatePresence>
          {messages.map((msg) => (
            <motion.div 
              key={msg.id}
              initial={{ opacity: 0, y: 10, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              className={cn(
                "flex flex-col max-w-[85%] md:max-w-[75%]",
                msg.role === 'user' ? "ml-auto items-end text-right" : "mr-auto items-start text-left"
              )}
            >
              <div className="flex items-center gap-2 mb-2">
                {msg.role === 'ai' && <div className="w-6 h-6 bg-primary/10 rounded-lg flex items-center justify-center text-primary"><Bot size={14} /></div>}
                <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">
                  {msg.role === 'ai' ? '销售助手' : '你自己'}
                </span>
                {msg.role === 'user' && <div className="w-6 h-6 bg-slate-100 rounded-lg flex items-center justify-center text-slate-400"><User size={14} /></div>}
              </div>

              {msg.type === 'insight' ? (
                <div className="bg-blue-50 border border-blue-100 p-5 rounded-2xl shadow-sm">
                   <div className="flex items-center gap-2 mb-3">
                    <TrendingUp className="w-5 h-5 text-blue-500" />
                    <span className="text-sm font-black text-blue-900 tracking-tight">消费趋势分析</span>
                  </div>
                  <p className="text-[15px] text-blue-700 leading-relaxed font-medium">{msg.content}</p>
                </div>
              ) : msg.type === 'recommendation' ? (
                <div className="flex flex-col gap-4">
                  <div className="bg-primary text-white p-5 rounded-2xl shadow-lg shadow-primary/10">
                    <p className="text-[15px] leading-relaxed font-inter">{msg.content}</p>
                  </div>
                  <div className="flex gap-4 overflow-x-auto pb-2 -mx-2 px-2 hide-scrollbar">
                    {[1, 2].map((i) => (
                      <div key={i} className="min-w-[200px] bg-white border border-slate-200 rounded-2xl p-3 shadow-sm hover:shadow-md transition-shadow cursor-pointer flex flex-col gap-2">
                        <div className="h-28 bg-slate-50 rounded-xl overflow-hidden relative group">
                          <img 
                            src={i === 1 
                              ? 'https://lh3.googleusercontent.com/aida-public/AB6AXuAjTYcN50633_JIWo12BJkPahQEWOpKR96iK9XJ2XXU6wLOtfSwSyuJqiBn3J7cin6fvPM4Rws-BOUSxhlUfMeLF9dnIh1cW3qQeXYHBhELaki36msvHGdx8fkoICG8Bocv49JgRVA9Raow7sryVGLMEf-ZMUjbxDqvZ0P-jEbp0_UFZNQvn7gVJq-ExctaYqoVfJTX-PNH8HeJrF5pejO9w-IX3I34DuZdYoer_BkYSXpMOhlRnOXttmJOG43ZUZkDlDxqj8YNp2Y'
                              : 'https://lh3.googleusercontent.com/aida-public/AB6AXuD_K_GBILzj7qZzoUteeioYylo3xjQe_Mx3ljpBitRG7PmetuKr9mTQ8xUqicJKYJ07u0YtnLDkBKXIHDlJ55X-BthnVEC8_HzP1KalQEzmKIpiFM-jDLKxlcBbcpEA5WZ8uMVMLusRJBViRV18e8Q3hsMwetdSq2ZKqtWFXTXJdOQupiTg9GmiYaNPVwRWIZSFxPtdb5SsRqyc2o76Q7EqxmruPKoWj8LSUrsozvHxt-raCxCPnVRyZhW0uKJ9adLWu4MhZbLoaWk'} 
                            alt="" 
                            className="w-full h-full object-cover group-hover:scale-110 transition-transform" 
                          />
                          <div className="absolute top-2 right-2 px-2 py-0.5 bg-white/90 backdrop-blur-md rounded-lg text-[9px] font-black text-primary border border-primary/10">AI 优选</div>
                        </div>
                        <div>
                          <p className="text-xs font-bold text-slate-800 line-clamp-1">{i === 1 ? '办公桌搭套装' : '精品烘焙咖啡豆'}</p>
                          <div className="flex items-center justify-between mt-2">
                            <span className="text-sm font-black text-primary">¥{i === 1 ? '299' : '24.5'}</span>
                            <PlusCircle className="w-5 h-5 text-slate-300 hover:text-primary transition-colors" />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className={cn(
                  "p-5 rounded-[24px] shadow-sm font-inter text-[15px] leading-relaxed border",
                  msg.role === 'user' 
                    ? "bg-white border-slate-200 text-slate-800 rounded-tr-none" 
                    : "bg-slate-900 border-slate-800 text-slate-100 rounded-tl-none"
                )}>
                  {msg.content}
                </div>
              )}
              
              {msg.role === 'ai' && (
                <div className="flex gap-2 mt-3 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button className="p-2 text-slate-400 hover:text-primary hover:bg-primary/5 rounded-full"><ThumbsUp size={14} /></button>
                  <button className="p-2 text-slate-400 hover:text-primary hover:bg-primary/5 rounded-full"><ThumbsDown size={14} /></button>
                  <button className="p-2 text-slate-400 hover:text-primary hover:bg-primary/5 rounded-full"><RefreshCw size={14} /></button>
                </div>
              )}
            </motion.div>
          ))}

          {isTyping && (
            <motion.div 
              initial={{ opacity: 0 }} 
              animate={{ opacity: 1 }}
              className="flex flex-col items-start gap-4"
            >
              <div className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-900/5 rounded-full border border-slate-100">
                <span className="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce [animation-delay:-0.3s]" />
                <span className="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce [animation-delay:-0.15s]" />
                <span className="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" />
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Input Bar */}
      <div className="bg-white border border-slate-200 rounded-[32px] p-2 pr-3 shadow-2xl shadow-slate-200/50 mb-10 flex items-center gap-4 group focus-within:ring-4 focus-within:ring-primary/5 transition-all">
        <div className="w-14 h-14 bg-slate-50 border border-slate-100 rounded-full flex items-center justify-center text-slate-400 shrink-0 shadow-inner group-focus-within:bg-primary/5 group-focus-within:text-primary transition-colors">
          <MessageCircle className="w-7 h-7" />
        </div>
        <input 
          type="text" 
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
          placeholder="给 AI 助手发送消息..." 
          className="flex-1 bg-transparent border-none outline-none font-bold text-slate-900 text-lg placeholder:text-slate-300 placeholder:font-medium"
        />
        <button 
          onClick={handleSend}
          disabled={!input.trim()}
          className="w-14 h-14 bg-slate-900 text-white rounded-full flex items-center justify-center shadow-lg shadow-slate-900/10 hover:bg-primary transition-all disabled:opacity-20 disabled:grayscale"
        >
          <Zap className="w-7 h-7 fill-current" />
        </button>
      </div>
    </div>
  );
}
