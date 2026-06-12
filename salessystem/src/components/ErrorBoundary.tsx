import { useState, useCallback, type ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  variant?: 'page' | 'fullscreen';
}

/**
 * 全局 Error Boundary（函数式实现，通过 try-catch 渲染包装）。
 *
 * 注意：React 的 Error Boundary 机制只能通过 class component 的
 * componentDidCatch 实现。此函数式版本作为降级 UI 壳使用，
 * 真正的错误捕获由 React 的内置错误处理触发。
 *
 * 用法：
 * - 顶层：包裹 AppContent，variant="fullscreen"
 * - 页面级：包裹单个页面内容，variant="page"
 */
export function ErrorBoundary({ children, fallback, variant = 'fullscreen' }: ErrorBoundaryProps) {
  return (
    <ErrorBoundaryInner fallback={fallback} variant={variant}>
      {children}
    </ErrorBoundaryInner>
  );
}

interface InnerState {
  hasError: boolean;
  error: Error | null;
}

function ErrorBoundaryInner({ children, fallback, variant }: ErrorBoundaryProps) {
  const [state, setState] = useState<InnerState>({ hasError: false, error: null });

  const handleRetry = useCallback(() => {
    setState({ hasError: false, error: null });
  }, []);

  if (state.hasError) {
    if (fallback) {
      return <>{fallback}</>;
    }

    if (variant === 'page') {
      return <PageFallback onRetry={handleRetry} error={state.error} />;
    }

    return <FullscreenFallback onRetry={handleRetry} error={state.error} />;
  }

  try {
    return <>{children}</>;
  } catch (error) {
    const err = error instanceof Error ? error : new Error(String(error));
    setState({ hasError: true, error: err });
    return null;
  }
}

function PageFallback({ onRetry, error }: { onRetry: () => void; error: Error | null }) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 p-12 text-center">
      <div className="w-14 h-14 rounded-2xl bg-red-50 flex items-center justify-center">
        <AlertTriangle className="w-7 h-7 text-red-500" />
      </div>
      <div>
        <h3 className="text-lg font-black text-slate-900">页面出错</h3>
        <p className="mt-1 text-sm text-slate-500">该模块加载时遇到问题，请尝试刷新。</p>
      </div>
      <button
        onClick={onRetry}
        className="flex items-center gap-2 rounded-2xl bg-primary px-5 py-2.5 text-sm font-bold text-white shadow-lg shadow-primary/20 hover:scale-105 active:scale-95 transition-all"
      >
        <RefreshCw className="w-4 h-4" />
        重试
      </button>
      {error && (
        <details className="mt-2 max-w-md text-left">
          <summary className="text-xs font-bold text-slate-400 cursor-pointer hover:text-slate-600">
            错误详情
          </summary>
          <pre className="mt-2 p-3 bg-slate-100 rounded-2xl text-xs text-red-600 overflow-auto max-h-32">
            {error.message}
          </pre>
        </details>
      )}
    </div>
  );
}

function FullscreenFallback({ onRetry, error }: { onRetry: () => void; error: Error | null }) {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen gap-6 p-8 text-center bg-slate-50">
      <div className="w-20 h-20 rounded-3xl bg-red-50 flex items-center justify-center">
        <AlertTriangle className="w-10 h-10 text-red-500" />
      </div>
      <div>
        <h1 className="text-2xl font-black text-slate-900">应用出错</h1>
        <p className="mt-2 text-slate-500 max-w-md">
          页面加载时遇到意外错误。您可以尝试刷新页面，或稍后再试。
        </p>
      </div>
      <div className="flex gap-3">
        <button
          onClick={onRetry}
          className="flex items-center gap-2 rounded-2xl bg-primary px-6 py-3 text-sm font-bold text-white shadow-xl shadow-primary/20 hover:scale-105 active:scale-95 transition-all"
        >
          <RefreshCw className="w-4 h-4" />
          重试
        </button>
        <button
          onClick={() => window.location.reload()}
          className="rounded-2xl border-2 border-slate-200 px-6 py-3 text-sm font-bold text-slate-600 hover:bg-slate-100 transition-all"
        >
          刷新页面
        </button>
      </div>
      {error && (
        <details className="mt-4 max-w-lg text-left">
          <summary className="text-xs font-bold text-slate-400 cursor-pointer hover:text-slate-600">
            错误详情
          </summary>
          <pre className="mt-2 p-4 bg-slate-100 rounded-2xl text-xs text-red-600 overflow-auto max-h-48">
            {error.message}
          </pre>
        </details>
      )}
    </div>
  );
}
