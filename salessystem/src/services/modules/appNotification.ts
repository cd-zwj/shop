import { request } from '../request';
import type { AppNotification } from '../../types/addressNotification';
import type { PageResult } from '../../types/api';

export const appNotificationService = {
  /** 获取当前用户通知列表（分页） */
  list(pageNum = 1, pageSize = 20) {
    return request<PageResult<AppNotification>>({
      url: '/v1/app/notifications',
      method: 'get',
      params: { current: pageNum, size: pageSize },
      authRole: 'user',
    });
  },

  /** 标记单条通知已读 */
  markRead(id: number) {
    return request<AppNotification>({
      url: `/v1/app/notifications/${id}/read`,
      method: 'put',
      authRole: 'user',
    });
  },

  markAllRead() {
    return request<void>({
      url: '/v1/app/notifications/read-all',
      method: 'put',
      authRole: 'user',
    });
  },

  getUnreadCount() {
    return request<{ count: number }>({
      url: '/v1/app/notifications/unread-count',
      method: 'get',
      authRole: 'user',
    });
  },
};

export default appNotificationService;
