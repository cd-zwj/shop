import { useState, useRef, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Bot,
  User,
  TrendingUp,
  BrainCircuit,
  MessageCircle,
  ThumbsUp,
  ThumbsDown,
  RefreshCw,
  Zap,
  ShieldCheck,
  Store,
  Wallet,
  CheckCircle,
  FileText,
  Plus,
  List,
  Trash2,
  Mic,
  MicOff,
} from 'lucide-react';
import { cn } from '../lib/utils';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../utils/errorMessage';
import {
  ragScenarioChatStream,
  ragSessionCreate,
  ragSessionHistory,
  ragSessionList,
  ragSessionDelete,
  ragSubmitFeedback,
  ragAsr,
  type AiScenario,
  type AgentMode,
} from '../services/rag';

interface Citation {
  sourceName?: string;
  docTitle?: string;
  sectionTitle?: string;
  label?: string;
  text?: string;
  score?: number;
  minioUrl?: string;
  chunkIndex?: number;
}

interface PlanData {
  planId?: string;
  planText?: string;
  message?: string;
  mode?: string;
  status?: string;
}

interface Message {
  id: string;
  role: 'user' | 'ai';
  content: string;
  type?: 'text' | 'insight' | 'plan';
  data?: unknown;
  citations?: Citation[];
  planData?: PlanData;
  feedback?: 'UP' | 'DOWN';
  messageIndex?: number;
}

type ActorRole = 'user' | 'merchant' | 'admin';

interface ScenarioOption {
  scenario: AiScenario;
  label: string;
  summary: string;
}

const roleScenarios: Record<ActorRole, ScenarioOption[]> = {
  user: [
    { scenario: 'USER_SHOPPING_ASSISTANT', label: '购物咨询', summary: '商品、商户、优惠活动、平台规则' },
    { scenario: 'USER_WALLET_ADVISOR', label: '钱包权益', summary: '钱包、积分、优惠券、成长等级' },
    { scenario: 'USER_ORDER_AFTERSALE', label: '订单售后', summary: '订单进度、履约、退款、通知' },
  ],
  merchant: [
    { scenario: 'MERCHANT_OPERATION_ASSISTANT', label: '经营概览', summary: '商品、订单、会员、财务概览' },
    { scenario: 'MERCHANT_ORDER_ASSISTANT', label: '订单履约', summary: '订单处理、发货、退款审核' },
    { scenario: 'MERCHANT_MARKETING_ASSISTANT', label: '营销会员', summary: '优惠券、活动、会员等级、标签' },
  ],
  admin: [
    { scenario: 'ADMIN_GOVERNANCE_ASSISTANT', label: '平台治理', summary: '商户、用户、交易、支付、提现、权限' },
    { scenario: 'ADMIN_RISK_ASSISTANT', label: '风险审查', summary: '风控、退款、提现、安全异常' },
  ],
};

const roleTitle: Record<ActorRole, string> = {
  user: '用户消费助手',
  merchant: '商家经营助手',
  admin: '平台治理助手',
};

const roleIntro: Record<ActorRole, string> = {
  user: '我可以协助处理购物咨询、钱包权益、订单售后等用户场景，并只访问你自己的数据。',
  merchant: '我可以协助处理经营概览、订单履约、营销会员等商家场景，并限制在当前商家租户。',
  admin: '我可以协助处理平台治理、风险审查等管理场景，并按管理员权限提供依据。',
};

const roleIcon = {
  user: Wallet,
  merchant: Store,
  admin: ShieldCheck,
};

function buildWelcomeMessages(role: ActorRole): Message[] {
  return [
    { id: 'welcome', role: 'ai', content: roleIntro[role], type: 'text' },
    {
      id: 'scope',
      role: 'ai',
      type: 'insight',
      content: roleScenarios[role].map(item => `${item.label}: ${item.summary}`).join('\n'),
    },
  ];
}

export default function AIAssistant() {
  const { currentRole } = useAuth();
  const { showToast } = useToast();
  const safeRole = currentRole === 'merchant' || currentRole === 'admin' ? currentRole : 'user';
  const RoleIcon = roleIcon[safeRole];
  const [selectedScenario, setSelectedScenario] = useState<AiScenario>(roleScenarios[safeRole][0].scenario);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [turnCount, setTurnCount] = useState(0);
  const [modeHint, setModeHint] = useState<AgentMode>('AUTO');
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<Message[]>(() => buildWelcomeMessages(safeRole));
  const [isTyping, setIsTyping] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  // 会话管理
  const [showSessionList, setShowSessionList] = useState(false);
  const [sessionIds, setSessionIds] = useState<string[]>([]);

  // ASR 录音
  const [isRecording, setIsRecording] = useState(false);
  const [recordingSecs, setRecordingSecs] = useState(0);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const recordingTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, isTyping]);

  useEffect(() => {
    setMessages(buildWelcomeMessages(safeRole));
    setSelectedScenario(roleScenarios[safeRole][0].scenario);
    setSessionId(null);
    setTurnCount(0);
  }, [safeRole]);

  useEffect(() => {
    if (!sessionId) return undefined;
    let cancelled = false;
    (async () => {
      try {
        const history = await ragSessionHistory(sessionId);
        if (cancelled || !Array.isArray(history) || history.length === 0) return;
        const historyMessages: Message[] = history.map((entry, idx) => ({
          id: `h-${idx}`,
          role: (entry.role === 'user' ? 'user' : 'ai') as 'user' | 'ai',
          content: String(entry.content ?? entry.message ?? ''),
          type: 'text',
        }));
        setMessages([...buildWelcomeMessages(safeRole), ...historyMessages]);
        setTurnCount(historyMessages.filter(m => m.role === 'user').length);
      } catch (error) {
        showToast(getErrorMessage(error, '历史会话加载失败'), 'error');
      }
    })();
    return () => { cancelled = true; };
  }, [sessionId]); // eslint-disable-line react-hooks/exhaustive-deps

  async function ensureSession() {
    if (sessionId) return sessionId;
    const session = await ragSessionCreate('');
    setSessionId(session.sessionId);
    return session.sessionId;
  }

  // ==================== 会话管理 ====================

  const loadSessionList = useCallback(async () => {
    try {
      const result = await ragSessionList('');
      setSessionIds(result.sessions ?? []);
    } catch (error) {
      showToast(getErrorMessage(error, '历史会话列表加载失败'), 'error');
    }
  }, []);

  const handleNewSession = useCallback(() => {
    setSessionId(null);
    setTurnCount(0);
    setMessages(buildWelcomeMessages(safeRole));
    setShowSessionList(false);
  }, [safeRole]);

  const handleSwitchSession = useCallback(async (sid: string) => {
    setSessionId(sid);
    setShowSessionList(false);
  }, []);

  const handleDeleteSession = useCallback(async (sid: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await ragSessionDelete(sid, '');
      setSessionIds(prev => prev.filter(id => id !== sid));
      if (sessionId === sid) handleNewSession();
    } catch (error) {
      showToast(getErrorMessage(error, '删除会话失败'), 'error');
    }
  }, [sessionId, handleNewSession]);

  // ==================== 反馈 ====================

  const handleFeedback = useCallback(async (msgId: string, feedbackType: 'UP' | 'DOWN') => {
    setMessages(prev => prev.map(msg =>
      msg.id === msgId ? { ...msg, feedback: feedbackType } : msg
    ));
    try {
      await ragSubmitFeedback({
        sessionId: sessionId ?? '',
        messageIndex: messages.find(m => m.id === msgId)?.messageIndex ?? 0,
        feedbackType,
      });
    } catch (error) {
      showToast(getErrorMessage(error, '反馈提交失败'), 'error');
    }
  }, [sessionId, messages]);

  const handleRegenerate = useCallback(async () => {
    if (isTyping) return;
    const lastUserMsg = [...messages].reverse().find(m => m.role === 'user');
    if (!lastUserMsg) return;

    const aiMsgId = `a-${Date.now()}`;
    setMessages(prev => [...prev, { id: aiMsgId, role: 'ai', content: '', type: 'text' }]);
    setIsTyping(true);

    try {
      const activeSessionId = await ensureSession();
      await ragScenarioChatStream(
        { scenario: selectedScenario, sessionId: activeSessionId, message: lastUserMsg.content, turnCount, modeHint },
        ({ event, data }) => {
          if (event === 'token') appendAiToken(aiMsgId, data);
          if (event === 'error') replaceAiMessage(aiMsgId, { content: data || 'AI 服务暂时不可用' });
          if (event === 'citations' && data && data !== '[]') {
            try {
              const parsed = JSON.parse(data) as Citation[];
              if (Array.isArray(parsed) && parsed.length > 0) replaceAiMessage(aiMsgId, { citations: parsed });
            } catch { /* ignore */ }
          }
        },
      );
      setTurnCount(prev => prev + 1);
    } catch (error) {
      replaceAiMessage(aiMsgId, { content: error instanceof Error ? error.message : '重新生成失败' });
    } finally {
      setIsTyping(false);
    }
  }, [isTyping, messages, selectedScenario, turnCount, modeHint]); // eslint-disable-line react-hooks/exhaustive-deps

  // ==================== ASR 语音识别 ====================

  const toggleRecording = useCallback(async () => {
    if (isRecording) {
      mediaRecorderRef.current?.stop();
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: { sampleRate: 16000, channelCount: 1 } });
      const recorder = new MediaRecorder(stream, { mimeType: 'audio/webm;codecs=opus' });
      audioChunksRef.current = [];
      recorder.ondataavailable = (e) => { if (e.data.size > 0) audioChunksRef.current.push(e.data); };
      recorder.onstop = async () => {
        stream.getTracks().forEach(t => t.stop());
        if (recordingTimerRef.current) clearInterval(recordingTimerRef.current);
        setIsRecording(false);
        setRecordingSecs(0);
        const blob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
        try {
          const text = await ragAsr(blob);
          if (text) setInput(prev => prev + text);
        } catch (error) {
          showToast(getErrorMessage(error, '语音识别失败'), 'error');
        }
      };
      recorder.start();
      mediaRecorderRef.current = recorder;
      setIsRecording(true);
      setRecordingSecs(0);
      recordingTimerRef.current = setInterval(() => {
        setRecordingSecs(prev => {
          if (prev >= 29) { recorder.stop(); return 0; }
          return prev + 1;
        });
      }, 1000);
    } catch (error) {
      showToast(getErrorMessage(error, '无法访问麦克风'), 'error');
    }
  }, [isRecording, showToast]);

  const appendAiToken = (messageId: string, token: string) => {
    setMessages(prev => prev.map(msg => (
      msg.id === messageId ? { ...msg, content: msg.content + token } : msg
    )));
  };

  const replaceAiMessage = (messageId: string, patch: Partial<Message>) => {
    setMessages(prev => prev.map(msg => (msg.id === messageId ? { ...msg, ...patch } : msg)));
  };

  const handleSend = async () => {
    const message = input.trim();
    if (!message || isTyping) return;

    const userMsg: Message = { id: `u-${Date.now()}`, role: 'user', content: message };
    const aiMsgId = `a-${Date.now()}`;
    setMessages(prev => [...prev, userMsg, { id: aiMsgId, role: 'ai', content: '', type: 'text' }]);
    setInput('');
    setIsTyping(true);

    try {
      const activeSessionId = await ensureSession();
      await ragScenarioChatStream(
        {
          scenario: selectedScenario,
          sessionId: activeSessionId,
          message,
          turnCount,
          modeHint,
        },
        ({ event, data }) => {
          if (event === 'token') appendAiToken(aiMsgId, data);
          if (event === 'error') replaceAiMessage(aiMsgId, { content: data || 'AI 服务暂时不可用，请稍后重试' });
          if (event === 'plan_required') {
            try {
              const plan = JSON.parse(data) as PlanData;
              replaceAiMessage(aiMsgId, { content: plan.planText || data, type: 'plan', planData: plan });
            } catch {
              replaceAiMessage(aiMsgId, { content: data, type: 'plan' });
            }
          }
          if (event === 'citations' && data && data !== '[]') {
            try {
              const parsed = JSON.parse(data) as Citation[];
              if (Array.isArray(parsed) && parsed.length > 0) {
                replaceAiMessage(aiMsgId, { citations: parsed });
              }
            } catch {
              // ignore malformed citations
            }
          }
        },
      );
      setTurnCount(prev => prev + 1);
    } catch (error) {
      const messageText = error instanceof Error ? error.message : 'AI 请求失败，请稍后重试';
      replaceAiMessage(aiMsgId, { content: messageText });
    } finally {
      setIsTyping(false);
    }
  };

  const handleApprovePlan = async (msgId: string, planId: string) => {
    if (isTyping) return;

    const planMsg = messages.find(m => m.id === msgId);
    const aiMsgId = `a-${Date.now()}`;
    setMessages(prev => [
      ...prev.map(m => (m.id === msgId ? { ...m, type: 'text' as const, planData: undefined } : m)),
      { id: `u-plan-${Date.now()}`, role: 'user', content: '✅ 确认执行计划' },
      { id: aiMsgId, role: 'ai', content: '', type: 'text' },
    ]);
    setIsTyping(true);

    try {
      const activeSessionId = await ensureSession();
      await ragScenarioChatStream(
        {
          scenario: selectedScenario,
          sessionId: activeSessionId,
          message: planMsg?.planData?.message || '确认执行计划',
          turnCount,
          modeHint,
          approvedPlanId: planId,
        },
        ({ event, data }) => {
          if (event === 'token') appendAiToken(aiMsgId, data);
          if (event === 'error') replaceAiMessage(aiMsgId, { content: data || '计划执行失败，请稍后重试' });
          if (event === 'citations' && data && data !== '[]') {
            try {
              const parsed = JSON.parse(data) as Citation[];
              if (Array.isArray(parsed) && parsed.length > 0) {
                replaceAiMessage(aiMsgId, { citations: parsed });
              }
            } catch {
              // ignore
            }
          }
        },
      );
      setTurnCount(prev => prev + 1);
    } catch (error) {
      const messageText = error instanceof Error ? error.message : '计划执行失败，请稍后重试';
      replaceAiMessage(aiMsgId, { content: messageText });
    } finally {
      setIsTyping(false);
    }
  };

  return (
    <div className="flex flex-col h-[calc(100vh-144px)] md:h-[calc(100vh-100px)] max-w-4xl mx-auto w-full px-4 pt-4 md:mt-4">
      <div className="bg-slate-900 rounded-2xl p-4 flex items-center justify-between shadow-lg shadow-slate-200/60 mb-6 text-white shrink-0">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 bg-white/10 rounded-xl flex items-center justify-center border border-white/10">
            <BrainCircuit className="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 className="text-lg font-black tracking-tight leading-none mb-1">{roleTitle[safeRole]}</h2>
            <div className="flex items-center gap-1.5 opacity-80">
              <span className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
              <span className="text-[10px] font-bold uppercase tracking-wider">{selectedScenario} · {modeHint}</span>
            </div>
          </div>
        </div>
        <div className="hidden sm:flex items-center gap-3">
          <RoleIcon className="w-5 h-5 text-white/70" />
          <select
            value={modeHint}
            onChange={(e) => setModeHint(e.target.value as AgentMode)}
            className="bg-white/10 border border-white/10 rounded-lg px-3 py-2 text-xs font-bold outline-none"
          >
            <option className="text-slate-900" value="AUTO">自动路由</option>
            <option className="text-slate-900" value="REACT">ReAct</option>
            <option className="text-slate-900" value="PLAN_EXECUTE">Plan</option>
          </select>
        </div>
      </div>

      <div className="mb-5 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 shrink-0">
        {roleScenarios[safeRole].map((item) => {
          const active = item.scenario === selectedScenario;
          return (
            <button
              key={item.scenario}
              type="button"
              onClick={() => setSelectedScenario(item.scenario)}
              className={cn(
                'text-left rounded-2xl border p-4 transition-all bg-white',
                active ? 'border-slate-900 shadow-lg shadow-slate-200/70' : 'border-slate-200 hover:border-slate-300',
              )}
            >
              <div className="flex items-center justify-between gap-3 mb-2">
                <span className="text-sm font-black text-slate-900">{item.label}</span>
                {active && <span className="w-2.5 h-2.5 rounded-full bg-green-500" />}
              </div>
              <p className="text-xs font-medium leading-relaxed text-slate-500">{item.summary}</p>
            </button>
          );
        })}
      </div>

      {/* 会话管理栏 */}
      <div className="flex items-center justify-between mb-4 px-1 shrink-0">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleNewSession}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold text-slate-500 hover:text-primary bg-white border border-slate-200 rounded-lg hover:border-primary/30 transition-colors"
          >
            <Plus size={14} />
            新会话
          </button>
          {sessionId && (
            <span className="text-[10px] text-slate-400 font-mono truncate max-w-[120px]">
              {sessionId.slice(0, 8)}…
            </span>
          )}
        </div>
        <div className="relative">
          <button
            type="button"
            onClick={() => { setShowSessionList(v => !v); if (!showSessionList) loadSessionList(); }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold text-slate-500 hover:text-primary bg-white border border-slate-200 rounded-lg hover:border-primary/30 transition-colors"
          >
            <List size={14} />
            历史会话
          </button>
          <AnimatePresence>
            {showSessionList && (
              <motion.div
                initial={{ opacity: 0, y: -4 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                className="absolute right-0 top-full mt-1 w-64 bg-white border border-slate-200 rounded-xl shadow-lg z-50 max-h-64 overflow-y-auto"
              >
                {sessionIds.length === 0 ? (
                  <div className="p-3 text-xs text-slate-400 text-center">暂无历史会话</div>
                ) : (
                  sessionIds.map(sid => (
                    <div
                      key={sid}
                      onClick={() => handleSwitchSession(sid)}
                      className={cn(
                        'flex items-center justify-between px-3 py-2 cursor-pointer hover:bg-slate-50 transition-colors',
                        sid === sessionId && 'bg-primary/5',
                      )}
                    >
                      <span className="text-xs font-mono text-slate-600 truncate flex-1">
                        {sid.slice(0, 12)}…
                      </span>
                      <button
                        type="button"
                        onClick={(e) => handleDeleteSession(sid, e)}
                        className="p-1 text-slate-300 hover:text-red-500 rounded transition-colors"
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  ))
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      <div ref={scrollRef} className="flex-1 overflow-y-auto mb-6 flex flex-col gap-8 pr-2 hide-scrollbar scroll-smooth">
        <AnimatePresence>
          {messages.map((msg) => (
            <motion.div
              key={msg.id}
              initial={{ opacity: 0, y: 10, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              className={cn(
                'flex flex-col max-w-[85%] md:max-w-[75%]',
                msg.role === 'user' ? 'ml-auto items-end text-right' : 'mr-auto items-start text-left',
              )}
            >
              <div className="flex items-center gap-2 mb-2">
                {msg.role === 'ai' && <div className="w-6 h-6 bg-primary/10 rounded-lg flex items-center justify-center text-primary"><Bot size={14} /></div>}
                <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">
                  {msg.role === 'ai' ? roleTitle[safeRole] : '你自己'}
                </span>
                {msg.role === 'user' && <div className="w-6 h-6 bg-slate-100 rounded-lg flex items-center justify-center text-slate-400"><User size={14} /></div>}
              </div>

              {msg.type === 'insight' ? (
                <div className="bg-blue-50 border border-blue-100 p-5 rounded-2xl shadow-sm">
                  <div className="flex items-center gap-2 mb-3">
                    <TrendingUp className="w-5 h-5 text-blue-500" />
                    <span className="text-sm font-black text-blue-900 tracking-tight">可处理场景</span>
                  </div>
                  <p className="text-[15px] text-blue-700 leading-relaxed font-medium">{msg.content}</p>
                </div>
              ) : (
                <div className={cn(
                  'p-5 rounded-[24px] shadow-sm font-inter text-[15px] leading-relaxed border whitespace-pre-wrap min-h-6',
                  msg.role === 'user'
                    ? 'bg-white border-slate-200 text-slate-800 rounded-tr-none'
                    : msg.type === 'plan'
                      ? 'bg-amber-50 border-amber-100 text-amber-950 rounded-tl-none'
                      : 'bg-slate-900 border-slate-800 text-slate-100 rounded-tl-none',
                )}>
                  {msg.content || (msg.role === 'ai' ? '...' : '')}
                </div>
              )}

              {msg.type === 'plan' && msg.planData?.planId && (
                <button
                  type="button"
                  onClick={() => handleApprovePlan(msg.id, msg.planData!.planId!)}
                  disabled={isTyping}
                  className="mt-3 inline-flex items-center gap-2 px-5 py-2.5 bg-amber-600 text-white text-sm font-bold rounded-xl hover:bg-amber-700 transition-colors disabled:opacity-40 disabled:cursor-not-allowed shadow-sm"
                >
                  <CheckCircle size={16} />
                  确认执行此计划
                </button>
              )}

              {msg.role === 'ai' && msg.citations && msg.citations.length > 0 && (
                <div className="mt-3 w-full max-w-md space-y-2">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">
                    <FileText size={12} />
                    <span>引用来源 ({msg.citations.length})</span>
                  </div>
                  {msg.citations.map((cite, idx) => (
                    <div key={idx} className="rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-xs">
                      <div className="flex items-center justify-between gap-2 mb-1">
                        <span className="font-bold text-slate-700 truncate">
                          {cite.sectionTitle || cite.label || cite.sourceName || `段落 ${idx + 1}`}
                        </span>
                        {cite.score != null && (
                          <span className={cn(
                            'shrink-0 px-1.5 py-0.5 rounded-md text-[10px] font-bold',
                            cite.score >= 70 ? 'bg-green-50 text-green-700' : cite.score >= 40 ? 'bg-amber-50 text-amber-700' : 'bg-slate-100 text-slate-500',
                          )}>
                            {cite.score}%
                          </span>
                        )}
                      </div>
                      {cite.docTitle && (
                        <div className="text-slate-400 mb-1 truncate">{cite.docTitle}</div>
                      )}
                      {cite.text && (
                        <p className="text-slate-500 leading-relaxed line-clamp-2">{cite.text}</p>
                      )}
                      {cite.minioUrl && (
                        <a href={cite.minioUrl} target="_blank" rel="noopener noreferrer" className="text-primary hover:underline mt-1 inline-block">
                          查看原文
                        </a>
                      )}
                    </div>
                  ))}
                </div>
              )}

              {msg.role === 'ai' && (
                <div className="flex gap-2 mt-3">
                  <button
                    onClick={() => handleFeedback(msg.id, 'UP')}
                    disabled={msg.feedback === 'UP'}
                    className={cn(
                      'p-2 rounded-full transition-colors',
                      msg.feedback === 'UP' ? 'text-green-500 bg-green-50' : 'text-slate-400 hover:text-primary hover:bg-primary/5',
                    )}
                  >
                    <ThumbsUp size={14} />
                  </button>
                  <button
                    onClick={() => handleFeedback(msg.id, 'DOWN')}
                    disabled={msg.feedback === 'DOWN'}
                    className={cn(
                      'p-2 rounded-full transition-colors',
                      msg.feedback === 'DOWN' ? 'text-red-500 bg-red-50' : 'text-slate-400 hover:text-primary hover:bg-primary/5',
                    )}
                  >
                    <ThumbsDown size={14} />
                  </button>
                  <button
                    onClick={handleRegenerate}
                    disabled={isTyping}
                    className="p-2 text-slate-400 hover:text-primary hover:bg-primary/5 rounded-full disabled:opacity-30"
                  >
                    <RefreshCw size={14} />
                  </button>
                </div>
              )}
            </motion.div>
          ))}

          {isTyping && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col items-start gap-4">
              <div className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-900/5 rounded-full border border-slate-100">
                <span className="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce [animation-delay:-0.3s]" />
                <span className="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce [animation-delay:-0.15s]" />
                <span className="w-1.5 h-1.5 bg-slate-400 rounded-full animate-bounce" />
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      <div className="bg-white border border-slate-200 rounded-[32px] p-2 pr-3 shadow-2xl shadow-slate-200/50 mb-10 flex items-center gap-4 group focus-within:ring-4 focus-within:ring-primary/5 transition-all">
        <div className="w-14 h-14 bg-slate-50 border border-slate-100 rounded-full flex items-center justify-center text-slate-400 shrink-0 shadow-inner group-focus-within:bg-primary/5 group-focus-within:text-primary transition-colors">
          <MessageCircle className="w-7 h-7" />
        </div>
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
          placeholder={isRecording ? `录音中 ${recordingSecs}s…` : '给 AI 助手发送消息...'}
          disabled={isRecording}
          className="flex-1 bg-transparent border-none outline-none font-bold text-slate-900 text-lg placeholder:text-slate-300 placeholder:font-medium disabled:opacity-50"
        />
        <button
          type="button"
          onClick={toggleRecording}
          className={cn(
            'w-10 h-10 rounded-full flex items-center justify-center transition-all shrink-0',
            isRecording
              ? 'bg-red-500 text-white animate-pulse'
              : 'bg-slate-100 text-slate-400 hover:bg-slate-200 hover:text-slate-600',
          )}
          title={isRecording ? '停止录音' : '语音输入'}
        >
          {isRecording ? <MicOff size={18} /> : <Mic size={18} />}
        </button>
        <button
          onClick={handleSend}
          disabled={!input.trim() || isTyping}
          className="w-14 h-14 bg-slate-900 text-white rounded-full flex items-center justify-center shadow-lg shadow-slate-900/10 hover:bg-primary transition-all disabled:opacity-20 disabled:grayscale"
        >
          <Zap className="w-7 h-7 fill-current" />
        </button>
      </div>
    </div>
  );
}
