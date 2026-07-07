import { request } from '../request';
import type { PointsBalance, PointsLog, ExchangeProduct } from '../../types/points';
import type { PageResult } from '../../types/api';
import type { AssetTracePresentation } from '../../types/wallet';

interface BackendPointsLog {
  id: number;
  tenantId?: number | null;
  platformUserId?: number | null;
  bizType?: string | null;
  bizNo?: string | null;
  changePoints?: number | null;
  pointsAfter?: number | null;
  remark?: string | null;
  status?: string | null;
  expireTime?: string | null;
  createTime?: string | null;
  trace?: AssetTracePresentation | null;
}

export const appPointsService = {
  /** 积分余额 */
  getPointsBalance(tenantId: number) {
    return request<PointsBalance>({
      url: `/v1/app/tenants/${tenantId}/points`,
      method: 'get',
      authRole: 'user',
    });
  },

  /** 积分明细（分页） */
  async getPointsLogs(tenantId: number, pageNum = 1, pageSize = 20) {
    const result = await request<PageResult<BackendPointsLog>>({
      url: `/v1/app/tenants/${tenantId}/points/logs`,
      method: 'get',
      params: { current: pageNum, size: pageSize },
      authRole: 'user',
    });
    return {
      ...result,
      records: (result.records ?? []).map((log) => normalizePointsLog(log, tenantId)),
    } satisfies PageResult<PointsLog>;
  },

  /** 兑换商品列表 */
  getExchangeProducts(tenantId: number) {
    return request<ExchangeProduct[]>({
      url: `/v1/app/tenants/${tenantId}/points/exchange/products`,
      method: 'get',
      authRole: 'user',
    });
  },

  /** 兑换商品 */
  exchangeProduct(tenantId: number, exchangeProductId: number) {
    return request<{ orderNo: string; message: string }>({
      url: `/v1/app/tenants/${tenantId}/points/exchange/${exchangeProductId}`,
      method: 'post',
      authRole: 'user',
    });
  },
};
export default appPointsService;

function normalizePointsLog(log: BackendPointsLog, tenantId: number): PointsLog {
  const changePoints = Number(log.changePoints ?? 0);
  return {
    id: log.id,
    tenantId: Number(log.tenantId ?? tenantId),
    userId: Number(log.platformUserId ?? 0),
    points: changePoints,
    balance: Number(log.pointsAfter ?? 0),
    type: changePoints >= 0 ? 'GRANT' : 'DEDUCT',
    reason: log.remark || log.bizType || '积分变动',
    expireTime: log.expireTime ?? undefined,
    orderNo: log.bizNo ?? null,
    createTime: log.createTime || '',
    trace: log.trace ?? null,
  };
}
