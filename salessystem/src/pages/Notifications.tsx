import { useCallback, useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  ArrowLeft,
  BellOff,
  CheckCheck,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useToast } from '../context/ToastContext';
import { appNotificationService } from '../services/modules/appNotification';
import type { AppNotification } from '../types/addressNotification';
import { EmptyState } from '../components/ui/EmptyState';
import { cn } from '../lib/utils';
import { getNotificationAction } from '../utils/notificationAction';
import { getNotificationPresentation } from '../utils/notificationPresentation';

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */

function formatDate(isoString: string | null): string {
  if (!isoString) return '';
  const date = new Date(isoString);
  if (isNaN(date.getTime())) return isoString.split('T')[0] || '';
  return date
    .toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
    .replace(/\//g, '-');
}

/* ------------------------------------------------------------------ */
/*  Notifications Page                                                 */
/* ------------------------------------------------------------------ */

export default function Notifications() {
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [unreadCount, setUnreadCount] = useState(0);

  const loadNotifications = useCallback(
    async (page: number) => {
      setIsLoading(true);
      try {
        const [data, unread] = await Promise.all([
          appNotificationService.list(page, 20),
          appNotificationService.getUnreadCount(),
        ]);
        setNotifications(data.records ?? []);
        setTotalPages(data.pages ?? 1);
        setUnreadCount(unread.count ?? 0);
      } catch (err) {
        showToast(err instanceof Error ? err.message : '加载通知失败', 'error');
      } finally {
        setIsLoading(false);
      }
    },
    [showToast],
  );

  useEffect(() => {
    void loadNotifications(currentPage);
  }, [currentPage, loadNotifications]);

  const handleMarkRead = async (id: number) => {
    try {
      await appNotificationService.markRead(id);
      setNotifications((prev) =>
        prev.map((n) =>
          n.id === id ? { ...n, readStatus: 1, readTime: new Date().toISOString() } : n
        )
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (err) {
      showToast(err instanceof Error ? err.message : '标记已读失败', 'error');
    }
  };

  const handleMarkAllRead = async () => {
    const unread = notifications.filter((n) => n.readStatus === 0);
    if (unread.length === 0) {
      showToast('没有未读通知', 'info');
      return;
    }
    try {
      await appNotificationService.markAllRead();
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, readStatus: 1, readTime: new Date().toISOString() }))
      );
      setUnreadCount(0);
      showToast('已全部标为已读', 'success');
    } catch (err) {
      showToast(err instanceof Error ? err.message : '操作失败', 'error');
    }
  };

  const handlePageChange = (page: number) => {
    if (page < 1 || page > totalPages) return;
    setCurrentPage(page);
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
            <h1 className="text-2xl font-black text-slate-900 dark:text-white">消息通知</h1>
            <p className="text-xs font-semibold text-slate-400 mt-0.5">
              系统动态与福利提醒
              {unreadCount > 0 && (
                <span className="ml-2 inline-flex items-center gap-1 rounded-lg bg-red-50 px-2 py-0.5 text-[10px] font-black text-red-500">
                  {unreadCount} 条未读
                </span>
              )}
            </p>
          </div>
        </div>
        <button
          onClick={handleMarkAllRead}
          className="flex items-center gap-2 rounded-2xl border border-slate-200 px-4 py-2.5 text-xs font-bold text-slate-600 transition-all hover:border-primary hover:text-primary active:scale-95"
        >
          <CheckCheck className="w-4 h-4" />
          全部已读
        </button>
      </header>

      {/* Content */}
      {isLoading ? (
        <div className="flex flex-col items-center justify-center py-20 gap-3 text-slate-400">
          <div className="w-8 h-8 border-2 border-primary/20 border-t-primary rounded-full animate-spin" />
          <span className="text-sm font-medium">加载通知中...</span>
        </div>
      ) : notifications.length === 0 ? (
        <EmptyState
          icon={<BellOff className="w-12 h-12" />}
          title="暂无通知"
          subtitle="您目前没有收到任何通知消息"
        />
      ) : (
        <AnimatePresence mode="wait">
          <motion.div
            key={currentPage}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.15 }}
            className="flex flex-col gap-3"
          >
            {notifications.map((notification) => {
              const presentation = getNotificationPresentation(notification.category);
              const Icon = presentation.icon;
              const isUnread = notification.readStatus === 0;
              const action = getNotificationAction(notification);

              return (
                <motion.div
                  key={notification.id}
                  layout
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  className={cn(
                    'group relative overflow-hidden rounded-3xl border bg-white shadow-sm transition-all hover:shadow-md cursor-pointer',
                    isUnread ? 'border-primary/20' : 'border-slate-100'
                  )}
                  onClick={() => {
                    if (isUnread) void handleMarkRead(notification.id);
                  }}
                >
                  <div className="flex items-start gap-4 p-5">
                    {/* Icon */}
                    <div
                      className={cn(
                        'flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl',
                        isUnread
                          ? 'bg-primary/10 text-primary'
                          : 'bg-slate-50 text-slate-400'
                      )}
                    >
                      <Icon className="h-5 w-5" />
                    </div>

                    {/* Content */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <span
                          className={cn(
                            'text-sm font-black truncate',
                            isUnread ? 'text-slate-900' : 'text-slate-700'
                          )}
                        >
                          {notification.title}
                        </span>
                        {isUnread && (
                          <span className="shrink-0 w-2 h-2 rounded-full bg-primary" />
                        )}
                      </div>
                      <p
                        className={cn(
                          'text-xs leading-relaxed line-clamp-2',
                          isUnread ? 'font-medium text-slate-600' : 'font-medium text-slate-400'
                        )}
                      >
                        {notification.content}
                      </p>
                      <div className="flex items-center gap-2 mt-2">
                        <span className="text-[10px] font-bold text-slate-300 uppercase tracking-wider">
                          {presentation.label}
                        </span>
                        <span className="text-slate-200">&bull;</span>
                        <span className="text-[10px] font-semibold text-slate-300">
                          {formatDate(notification.createTime)}
                        </span>
                      </div>
                      {action && (
                        <button
                          onClick={(event) => {
                            event.stopPropagation();
                            if (isUnread) void handleMarkRead(notification.id);
                            navigate(action.path);
                          }}
                          className="mt-3 inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-black text-slate-600 transition-all hover:border-primary hover:text-primary"
                        >
                          {action.label}
                          <ExternalLink className="h-3.5 w-3.5" />
                        </button>
                      )}
                    </div>

                    {/* Mark Read Button */}
                    {isUnread && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          void handleMarkRead(notification.id);
                        }}
                        className="shrink-0 p-2 text-primary/40 hover:text-primary hover:bg-primary/5 rounded-xl transition-colors"
                        title="标记已读"
                      >
                        <CheckCheck className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </motion.div>
              );
            })}
          </motion.div>
        </AnimatePresence>
      )}

      {/* Pagination */}
      {!isLoading && totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 pt-4">
          <button
            disabled={currentPage <= 1}
            onClick={() => handlePageChange(currentPage - 1)}
            className={cn(
              'p-2 rounded-xl transition-colors',
              currentPage <= 1
                ? 'text-slate-300 cursor-not-allowed'
                : 'text-slate-600 hover:bg-slate-100'
            )}
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <span className="text-sm font-bold text-slate-600">
            {currentPage} / {totalPages}
          </span>
          <button
            disabled={currentPage >= totalPages}
            onClick={() => handlePageChange(currentPage + 1)}
            className={cn(
              'p-2 rounded-xl transition-colors',
              currentPage >= totalPages
                ? 'text-slate-300 cursor-not-allowed'
                : 'text-slate-600 hover:bg-slate-100'
            )}
          >
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
      )}
    </div>
  );
}
