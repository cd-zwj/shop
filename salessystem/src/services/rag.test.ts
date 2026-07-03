import { describe, it, expect, vi, beforeEach } from 'vitest';

const mockGetToken = vi.hoisted(() => vi.fn());
const mockGetCurrentAuthRole = vi.hoisted(() => vi.fn());

vi.mock('../utils/token', () => ({ getToken: mockGetToken }));
vi.mock('../utils/authSession', () => ({ getCurrentAuthRole: mockGetCurrentAuthRole }));
vi.mock('../config/env', () => ({ API_BASE_URL: '/api' }));

import { createRagScenarioStreamRequest, ragScenarioChatStream } from './rag';

beforeEach(() => {
  vi.clearAllMocks();
  mockGetCurrentAuthRole.mockReturnValue('merchant');
  mockGetToken.mockReturnValue('merchant-token');
});

describe('createRagScenarioStreamRequest', () => {
  it('builds unified scenario SSE request with auth token and scenario payload', () => {
    const request = createRagScenarioStreamRequest({
      scenario: 'MERCHANT_ORDER_ASSISTANT',
      sessionId: 's-1',
      message: '帮我分析今天订单',
      turnCount: 2,
      modeHint: 'REACT',
    });

    expect(request.url).toBe('/api/rag/ai/scenario/chat');
    expect(request.method).toBe('POST');
    expect(request.headers.Authorization).toBe('Bearer merchant-token');
    expect(request.headers.satoken).toBe('Bearer merchant-token');
    expect(request.headers.Accept).toBe('text/event-stream');
    expect(JSON.parse(request.body)).toEqual({
      scenario: 'MERCHANT_ORDER_ASSISTANT',
      sessionId: 's-1',
      message: '帮我分析今天订单',
      turnCount: 2,
      modeHint: 'REACT',
    });
  });

  it('includes approvedPlanId when provided', () => {
    const request = createRagScenarioStreamRequest({
      scenario: 'USER_WALLET_ADVISOR',
      sessionId: 's-2',
      message: '确认执行',
      modeHint: 'PLAN_EXECUTE',
      approvedPlanId: 'plan-abc',
    });

    const body = JSON.parse(request.body);
    expect(body.approvedPlanId).toBe('plan-abc');
    expect(body.modeHint).toBe('PLAN_EXECUTE');
  });

  it('omits approvedPlanId when not provided', () => {
    const request = createRagScenarioStreamRequest({
      scenario: 'GENERAL_RAG_QA',
      sessionId: 's-3',
      message: 'test',
    });

    const body = JSON.parse(request.body);
    expect(body.approvedPlanId).toBeUndefined();
    expect(body.turnCount).toBe(0);
    expect(body.modeHint).toBe('AUTO');
  });
});

describe('ragScenarioChatStream', () => {
  it('treats done event as a successful stream completion', async () => {
    const cancel = vi.fn().mockResolvedValue(undefined);
    const read = vi.fn()
      .mockResolvedValueOnce({
        done: false,
        value: new TextEncoder().encode('event: token\ndata: 你好\n\nevent: done\ndata: {}\n\n'),
      })
      .mockRejectedValueOnce(new TypeError('network error after done'));
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      body: {
        getReader: () => ({ read, cancel }),
      },
    });
    vi.stubGlobal('fetch', fetchMock);
    const events: Array<{ event: string; data: string }> = [];

    await expect(ragScenarioChatStream({
      scenario: 'ADMIN_GOVERNANCE_ASSISTANT',
      sessionId: 's-1',
      message: '测试',
    }, event => events.push(event))).resolves.toBeUndefined();

    expect(events).toEqual([
      { event: 'token', data: '你好' },
      { event: 'done', data: '{}' },
    ]);
    expect(cancel).toHaveBeenCalledTimes(1);
    expect(read).toHaveBeenCalledTimes(1);
  });
});
