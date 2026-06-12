import { request } from '../request';
import type { PageResult } from '../../types/api';
import type {
  MerchantPointsRule,
  MerchantPointsRulePayload,
  MerchantRechargeRule,
  MerchantRechargeRulePayload,
  MerchantTransaction,
  MerchantTransactionFilters,
  MerchantWalletSummary,
  MerchantWithdrawal,
  MerchantWithdrawalApplyPayload,
  MerchantWithdrawalFilters,
} from '../../types/merchant';

export const merchantFinanceService = {
  getWalletSummary(tenantId: number) {
    return request<MerchantWalletSummary>({
      url: `/v1/merchant/tenants/${tenantId}/wallet-summary`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  getPointsRule(tenantId: number) {
    return request<MerchantPointsRule>({
      url: `/v1/merchant/tenants/${tenantId}/points-rule`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  updatePointsRule(tenantId: number, payload: MerchantPointsRulePayload) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/points-rule`,
      method: 'put',
      data: payload,
      authRole: 'merchant',
    });
  },

  listRechargeRules(tenantId: number) {
    return request<MerchantRechargeRule[]>({
      url: `/v1/merchant/tenants/${tenantId}/recharge-rules`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  replaceRechargeRules(tenantId: number, payload: MerchantRechargeRulePayload[]) {
    return request<void>({
      url: `/v1/merchant/tenants/${tenantId}/recharge-rules`,
      method: 'put',
      data: payload,
      authRole: 'merchant',
    });
  },

  getWithdrawalBalance(tenantId: number) {
    return request<MerchantWalletSummary>({
      url: `/v1/merchant/tenants/${tenantId}/withdrawals/balance`,
      method: 'get',
      authRole: 'merchant',
    });
  },

  listWithdrawals(tenantId: number, filters: MerchantWithdrawalFilters = {}) {
    return request<PageResult<MerchantWithdrawal>>({
      url: `/v1/merchant/tenants/${tenantId}/withdrawals`,
      method: 'get',
      params: {
        current: filters.current ?? 1,
        size: filters.size ?? 10,
        status: filters.status,
      },
      authRole: 'merchant',
    });
  },

  createWithdrawal(tenantId: number, payload: MerchantWithdrawalApplyPayload) {
    return request<MerchantWithdrawal>({
      url: `/v1/merchant/tenants/${tenantId}/withdrawals`,
      method: 'post',
      data: payload,
      authRole: 'merchant',
    });
  },

  listTransactions(tenantId: number, filters: MerchantTransactionFilters = {}) {
    return request<PageResult<MerchantTransaction>>({
      url: `/v1/merchant/tenants/${tenantId}/transactions`,
      method: 'get',
      params: {
        current: filters.current ?? 1,
        size: filters.size ?? 20,
        type: filters.type,
        startDate: filters.startDate,
        endDate: filters.endDate,
      },
      authRole: 'merchant',
    });
  },
};
