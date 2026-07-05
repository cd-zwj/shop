import { beforeEach, describe, expect, it, vi } from 'vitest';

const { mockRequest } = vi.hoisted(() => ({
  mockRequest: vi.fn(),
}));

vi.mock('../request', () => ({
  request: mockRequest,
}));

import { appNotificationService } from './appNotification';

describe('appNotificationService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('passes readStatus filter when listing unread notifications', () => {
    appNotificationService.list(2, 20, 0);

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/app/notifications',
      method: 'get',
      params: { current: 2, size: 20, readStatus: 0 },
      authRole: 'user',
    });
  });

  it('omits readStatus when listing all notifications', () => {
    appNotificationService.list(1, 20);

    expect(mockRequest).toHaveBeenCalledWith({
      url: '/v1/app/notifications',
      method: 'get',
      params: { current: 1, size: 20, readStatus: undefined },
      authRole: 'user',
    });
  });
});
