import { createContext, useContext, useState, useEffect, useRef, type ReactNode } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { AlertCircle, CheckCircle, Info, X } from 'lucide-react';

type ToastType = 'success' | 'error' | 'info';

interface Toast {
  id: string;
  message: string;
  type: ToastType;
}

interface ToastContextValue {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const timeoutsRef = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());

  useEffect(() => {
    return () => {
      timeoutsRef.current.forEach((t) => clearTimeout(t));
    };
  }, []);

  const showToast = (message: string, type: ToastType = 'info') => {
    const id = Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, message, type }]);

    const timeout = setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
      timeoutsRef.current.delete(id);
    }, 3000);
    timeoutsRef.current.set(id, timeout);
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {/* Toast container */}
      <div className="fixed top-20 left-1/2 -translate-x-1/2 z-[9999] flex flex-col gap-2 w-full max-w-sm px-4 pointer-events-none">
        <AnimatePresence>
          {toasts.map((toast) => (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, y: -20, scale: 0.9 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9, transition: { duration: 0.15 } }}
              className="pointer-events-auto flex items-center gap-3 w-full bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800 rounded-2xl p-4 shadow-xl shadow-slate-200/50 dark:shadow-none"
            >
              {toast.type === 'success' && (
                <CheckCircle className="w-5 h-5 text-green-500 shrink-0" />
              )}
              {toast.type === 'error' && (
                <AlertCircle className="w-5 h-5 text-red-500 shrink-0" />
              )}
              {toast.type === 'info' && (
                <Info className="w-5 h-5 text-blue-500 shrink-0" />
              )}
              
              <span className="flex-1 text-sm font-semibold text-slate-800 dark:text-slate-200 leading-snug">
                {toast.message}
              </span>

              <button
                onClick={() => removeToast(toast.id)}
                className="p-1 text-slate-400 hover:text-slate-600 hover:bg-slate-50 dark:hover:bg-slate-800 rounded-full transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
}
