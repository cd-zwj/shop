import type { UserCoupon } from '../types/coupon';
import type { GrowthLog } from '../types/growth';
import type { PointsLog } from '../types/points';

export type AssetTraceTone = 'positive' | 'negative' | 'neutral';

export interface AssetTracePresentation {
  title?: string;
  source: string;
  effect?: string;
  balance?: string;
  status?: string;
  hint?: string;
  actionLabel?: string;
  actionPath?: string;
  tone: AssetTraceTone;
}

const GROWTH_BIZ_TYPE_LABELS: Record<string, string> = {
  ORDER: '订单消费',
  RECHARGE: '充值',
  MANUAL: '人工调整',
};

const GROWTH_CHANGE_TYPE_LABELS: Record<string, string> = {
  EARN: '获得',
  DEDUCT: '扣减',
  ADJUST: '调整',
};

export function getPointsTracePresentation(log: PointsLog): AssetTracePresentation {
  const orderNo = log.orderNo?.trim();
  const points = Math.abs(log.points);
  const isGrant = log.points >= 0 || log.type === 'GRANT';
  const hint = log.expireTime ? `将于 ${formatDateTime(log.expireTime)} 过期` : undefined;

  return {
    title: log.reason || (isGrant ? '积分入账' : '积分支出'),
    source: orderNo ? `来源：订单 ${orderNo}` : `来源：${log.reason || '积分变动'}`,
    effect: `${isGrant ? '+' : '-'}${points} 积分`,
    balance: `变动后余额 ${log.balance}`,
    hint,
    actionPath: orderNo ? `/orders/${orderNo}` : undefined,
    actionLabel: orderNo ? '查看订单' : undefined,
    tone: isGrant ? 'positive' : 'negative',
  };
}

export function getGrowthTracePresentation(log: GrowthLog): AssetTracePresentation {
  const bizLabel = GROWTH_BIZ_TYPE_LABELS[log.bizType] || log.bizType || '成长值变动';
  const isEarn = log.changeType === 'EARN' || log.changeGrowth > 0;
  const isDeduct = log.changeType === 'DEDUCT' || log.changeGrowth < 0;
  const bizNo = log.bizNo?.trim();
  const effectPrefix = isEarn ? '+' : isDeduct ? '-' : '~';

  return {
    title: log.remark || bizLabel || GROWTH_CHANGE_TYPE_LABELS[log.changeType] || log.changeType,
    source: bizNo ? `来源：${bizLabel.replace('消费', '')} ${bizNo}` : `来源：${bizLabel}`,
    effect: `${effectPrefix}${Math.abs(log.changeGrowth)} 成长值`,
    balance: `${log.growthBefore} -> ${log.growthAfter}`,
    actionPath: log.bizType === 'ORDER' && bizNo ? `/orders/${bizNo}` : undefined,
    actionLabel: log.bizType === 'ORDER' && bizNo ? '查看订单' : undefined,
    tone: isEarn ? 'positive' : isDeduct ? 'negative' : 'neutral',
  };
}

export function getCouponTracePresentation(coupon: UserCoupon): AssetTracePresentation {
  if (coupon.status === 'USED') {
    return {
      source: `领取时间 ${formatDateTime(coupon.receiveTime)}`,
      status: '已使用',
      hint: `使用时间 ${formatDateTime(coupon.usedTime)}`,
      tone: 'neutral',
    };
  }

  if (coupon.status === 'EXPIRED') {
    return {
      source: `领取时间 ${formatDateTime(coupon.receiveTime)}`,
      status: '已过期',
      hint: `过期时间 ${formatDateTime(coupon.expireTime)}`,
      tone: 'negative',
    };
  }

  return {
    source: `领取时间 ${formatDateTime(coupon.receiveTime)}`,
    status: '可使用',
    hint: `${formatCouponThreshold(coupon)}，有效期至 ${formatDateTime(coupon.expireTime)}`,
    actionLabel: '去使用',
    actionPath: `/?tenantId=${coupon.tenantId}`,
    tone: 'positive',
  };
}

function formatCouponThreshold(coupon: UserCoupon): string {
  return coupon.thresholdAmount > 0 ? `满 ${trimNumber(coupon.thresholdAmount)} 元可用` : '无门槛可用';
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '--';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ').slice(0, 16);
  }

  const year = date.getFullYear();
  const month = pad(date.getMonth() + 1);
  const day = pad(date.getDate());
  const hour = pad(date.getHours());
  const minute = pad(date.getMinutes());
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

function pad(value: number): string {
  return value.toString().padStart(2, '0');
}

function trimNumber(value: number): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(2).replace(/\.?0+$/, '');
}
