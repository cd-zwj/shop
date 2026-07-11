import { request } from '../request';
import type { PageResult } from '../../types/api';
import type {
  MerchantRechargeRule,
  MerchantWalletRechargePayload,
  RechargePayment,
  UnifiedRechargeRule,
  UnifiedWalletRechargePayload,
  AssetActivity,
  TenantAssetSummary,
  WalletAccount,
  WalletLog,
} from '../../types/wallet';
import type { MemberPointsAccount, MemberPointsLog } from './memberPointsTypes';

export const appWalletService = {
  getUnifiedWallet() {
    return request<WalletAccount>({
      url: '/v1/app/wallets/unified',
      method: 'get',
      authRole: 'user',
    });
  },

  getUnifiedWalletLogs(current = 1, size = 10) {
    return request<PageResult<WalletLog>>({
      url: '/v1/app/wallets/unified/logs',
      method: 'get',
      params: { current, size },
      authRole: 'user',
    });
  },

  listTenantAssetSummaries() {
    return request<TenantAssetSummary[]>({
      url: '/v1/app/assets/tenant-summaries',
      method: 'get',
      authRole: 'user',
    });
  },

  listAssetActivities(size = 20) {
    return request<AssetActivity[]>({
      url: '/v1/app/assets/activities',
      method: 'get',
      params: { size },
      authRole: 'user',
    });
  },

  createUnifiedRecharge(payload: UnifiedWalletRechargePayload) {
    return request<RechargePayment>({
      url: '/v1/app/wallets/unified/recharges',
      method: 'post',
      data: payload,
      authRole: 'user',
    });
  },

  listUnifiedRechargeRules() {
    return request<UnifiedRechargeRule[]>({
      url: '/v1/app/wallets/unified/recharge-rules',
      method: 'get',
      authRole: 'user',
    });
  },

  getMerchantWallet(tenantId: number) {
    return request<WalletAccount>({
      url: `/v1/app/tenants/${tenantId}/wallet`,
      method: 'get',
      authRole: 'user',
    });
  },

  getMerchantWalletLogs(tenantId: number, current = 1, size = 10) {
    return request<PageResult<WalletLog>>({
      url: `/v1/app/tenants/${tenantId}/wallet/logs`,
      method: 'get',
      params: { current, size },
      authRole: 'user',
    });
  },

  listMerchantRechargeRules(tenantId: number) {
    return request<MerchantRechargeRule[]>({
      url: `/v1/app/tenants/${tenantId}/recharge-rules`,
      method: 'get',
      authRole: 'user',
    });
  },

  createMerchantRecharge(tenantId: number, payload: MerchantWalletRechargePayload) {
    return request<RechargePayment>({
      url: `/v1/app/tenants/${tenantId}/wallet/recharges`,
      method: 'post',
      data: payload,
      authRole: 'user',
    });
  },

  getPointsAccount(tenantId: number) {
    return request<MemberPointsAccount>({
      url: `/v1/app/tenants/${tenantId}/points`,
      method: 'get',
      authRole: 'user',
    });
  },

  getPointsLogs(tenantId: number, current = 1, size = 10) {
    return request<PageResult<MemberPointsLog>>({
      url: `/v1/app/tenants/${tenantId}/points/logs`,
      method: 'get',
      params: { current, size },
      authRole: 'user',
    });
  },
};

