import type { CouponTemplate, UserCoupon } from '../types/coupon';

export interface CouponLike {
  couponType: 'FIXED' | 'RATE';
  discountAmount: number | null;
  discountRate: number | null;
  maxDiscountAmount: number | null;
}

export interface CouponDataByTenant {
  availableTemplates: CouponTemplate[];
  myUsableCoupons: UserCoupon[];
}

export interface SelectedCartCoupon {
  key: string;
  type: 'TEMPLATE' | 'OWNED';
  id: number;
  name: string;
}

export interface SelectableCartCoupon {
  key: string;
  id: number;
  type: 'OWNED' | 'TEMPLATE';
  name: string;
  desc: string;
  discountAmount: number;
  thresholdAmount: number;
  isUsable: boolean;
  reason?: string;
}

export interface SelectedCouponResolution {
  discountAmount: number;
  isUsable: boolean;
  reason?: string;
}

// 单位约定：购物车小计 subtotal 与本文件所有返回的 discountAmount 均为“分”
// （与 CartItem.price、ProductVO.price 一致）；优惠券面额/门槛字段
// （thresholdAmount/discountAmount/maxDiscountAmount）来自后端，单位为“元”，
// 在本文件内统一乘 100 换算后参与比较和计算。

const FEN_PER_YUAN = 100;

export function calculateCartCouponDiscount(coupon: CouponLike, subtotal: number) {
  if (coupon.couponType === 'FIXED') {
    return (coupon.discountAmount ?? 0) * FEN_PER_YUAN;
  }

  const rate = coupon.discountRate ?? 1;
  const discount = subtotal * (1 - rate);
  const maxDiscountFen = (coupon.maxDiscountAmount ?? 0) * FEN_PER_YUAN;
  if (maxDiscountFen > 0 && discount > maxDiscountFen) {
    return maxDiscountFen;
  }
  return discount;
}

export function getSelectableCartCoupons(
  data: CouponDataByTenant | undefined,
  subtotal: number,
): SelectableCartCoupon[] {
  if (!data) return [];

  const options: SelectableCartCoupon[] = [
    ...data.myUsableCoupons.map((coupon) => {
      const isUsable = subtotal >= coupon.thresholdAmount * FEN_PER_YUAN;
      const discount = calculateCartCouponDiscount(coupon, subtotal);

      return {
        key: `owned-${coupon.id}`,
        id: coupon.id,
        type: 'OWNED' as const,
        name: coupon.name,
        desc: getCouponDescription(coupon),
        discountAmount: discount,
        thresholdAmount: coupon.thresholdAmount,
        isUsable,
        reason: isUsable ? undefined : getThresholdReason(coupon.thresholdAmount, subtotal),
      };
    }),
    ...data.availableTemplates.flatMap((template) => {
      const hasOwnedUsable = data.myUsableCoupons.some((coupon) => coupon.couponTemplateId === template.id);
      if (hasOwnedUsable) return [];

      const isUsable = subtotal >= template.thresholdAmount * FEN_PER_YUAN
        && template.receivable && template.remainingStock > 0;
      const discount = calculateCartCouponDiscount(template, subtotal);

      return [{
        key: `template-${template.id}`,
        id: template.id,
        type: 'TEMPLATE' as const,
        name: `${template.name} (可领用)`,
        desc: getCouponDescription(template),
        discountAmount: discount,
        thresholdAmount: template.thresholdAmount,
        isUsable,
        reason: getTemplateUnavailableReason(template, subtotal),
      }];
    }),
  ];

  return options.sort((a, b) => {
    if (a.isUsable && !b.isUsable) return -1;
    if (!a.isUsable && b.isUsable) return 1;
    return b.discountAmount - a.discountAmount;
  });
}

export function resolveSelectedCartCoupon(
  data: CouponDataByTenant | undefined,
  selectedCoupon: SelectedCartCoupon | null | undefined,
  subtotal: number,
): SelectedCouponResolution {
  if (!selectedCoupon) {
    return { discountAmount: 0, isUsable: false };
  }

  const option = getSelectableCartCoupons(data, subtotal).find((coupon) => coupon.key === selectedCoupon.key);
  if (!option) {
    return {
      discountAmount: 0,
      isUsable: false,
      reason: '所选优惠券已不可用，请重新选择',
    };
  }

  return {
    discountAmount: option.isUsable ? option.discountAmount : 0,
    isUsable: option.isUsable,
    reason: option.reason,
  };
}

function getCouponDescription(coupon: CouponLike & { thresholdAmount: number }) {
  return coupon.couponType === 'FIXED'
    ? `满 ¥${coupon.thresholdAmount} 减 ¥${coupon.discountAmount}`
    : `满 ¥${coupon.thresholdAmount} 打 ${(coupon.discountRate ?? 1) * 10} 折`;
}

function getTemplateUnavailableReason(template: CouponTemplate, subtotal: number) {
  if (subtotal < template.thresholdAmount * FEN_PER_YUAN) {
    return getThresholdReason(template.thresholdAmount, subtotal);
  }
  if (template.remainingStock <= 0) {
    return '无库存';
  }
  if (!template.receivable) {
    return '已领超限';
  }
  return undefined;
}

function getThresholdReason(thresholdAmount: number, subtotal: number) {
  return `还差 ¥${(thresholdAmount - subtotal / FEN_PER_YUAN).toFixed(2)}`;
}
