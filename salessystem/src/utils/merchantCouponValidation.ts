export type CouponDraftValue = number | string | null | undefined;

export interface MerchantCouponDraft {
  name: string;
  couponType: 'FIXED' | 'RATE';
  thresholdAmount: CouponDraftValue;
  discountAmount: CouponDraftValue;
  discountRate: CouponDraftValue;
  maxDiscountAmount: CouponDraftValue;
  totalStock: CouponDraftValue;
  perUserLimit: CouponDraftValue;
  validityType: 'RANGE' | 'DAYS';
  validDaysAfterReceive: CouponDraftValue;
  validStartTime: string;
  validEndTime: string;
  receiveStartTime: string;
  receiveEndTime: string;
}

export function validateMerchantCouponDraft(draft: MerchantCouponDraft) {
  const issues: string[] = [];
  const thresholdAmount = toNumber(draft.thresholdAmount);
  const discountAmount = toNumber(draft.discountAmount);
  const discountRate = toNumber(draft.discountRate);
  const maxDiscountAmount = toNumber(draft.maxDiscountAmount);
  const totalStock = toNumber(draft.totalStock);
  const perUserLimit = toNumber(draft.perUserLimit);
  const validDaysAfterReceive = toNumber(draft.validDaysAfterReceive);

  if (!draft.name.trim()) {
    issues.push('请输入优惠券名称');
  }

  if (thresholdAmount < 0) {
    issues.push('门槛金额不能小于 0');
  }

  if (totalStock <= 0) {
    issues.push('发行总量必须大于 0');
  }

  if (perUserLimit <= 0) {
    issues.push('每人限领必须大于 0');
  }

  if (totalStock > 0 && perUserLimit > totalStock) {
    issues.push('每人限领不能大于发行总量');
  }

  if (draft.couponType === 'FIXED') {
    if (discountAmount <= 0) {
      issues.push('满减面值必须大于 0');
    }
    if (thresholdAmount > 0 && discountAmount > thresholdAmount) {
      issues.push('满减面值不能大于门槛金额');
    }
  }

  if (draft.couponType === 'RATE') {
    if (discountRate <= 0 || discountRate >= 1) {
      issues.push('折扣比例必须大于 0 且小于 1');
    }
    if (maxDiscountAmount < 0) {
      issues.push('折扣封顶金额不能小于 0');
    }
  }

  if (draft.validityType === 'DAYS') {
    if (validDaysAfterReceive <= 0) {
      issues.push('领取后有效天数必须大于 0');
    }
  } else {
    validateDateRange({
      start: draft.validStartTime,
      end: draft.validEndTime,
      missingMessage: '请选择有效期开始与结束时间',
      invertedMessage: '有效期结束时间必须晚于开始时间',
      issues,
    });
  }

  validateOptionalReceiveWindow(draft, issues);
  return issues;
}

function validateOptionalReceiveWindow(draft: MerchantCouponDraft, issues: string[]) {
  if (!draft.receiveStartTime && !draft.receiveEndTime) {
    return;
  }

  validateDateRange({
    start: draft.receiveStartTime,
    end: draft.receiveEndTime,
    missingMessage: '领取时间需要同时填写开始与结束',
    invertedMessage: '领取结束时间必须晚于领取开始时间',
    issues,
  });

  if (draft.validityType !== 'RANGE' || !draft.validStartTime || !draft.validEndTime) {
    return;
  }

  const receiveStart = parseDateTime(draft.receiveStartTime);
  const receiveEnd = parseDateTime(draft.receiveEndTime);
  const validStart = parseDateTime(draft.validStartTime);
  const validEnd = parseDateTime(draft.validEndTime);

  if (receiveStart && validStart && receiveStart.getTime() < validStart.getTime()) {
    issues.push('领取开始时间不能早于优惠券有效期开始时间');
  }

  if (receiveEnd && validEnd && receiveEnd.getTime() > validEnd.getTime()) {
    issues.push('领取结束时间不能晚于优惠券有效期结束时间');
  }
}

function validateDateRange({
  start,
  end,
  missingMessage,
  invertedMessage,
  issues,
}: {
  start: string;
  end: string;
  missingMessage: string;
  invertedMessage: string;
  issues: string[];
}) {
  if (!start || !end) {
    issues.push(missingMessage);
    return;
  }

  const parsedStart = parseDateTime(start);
  const parsedEnd = parseDateTime(end);
  if (!parsedStart || !parsedEnd || parsedEnd.getTime() <= parsedStart.getTime()) {
    issues.push(invertedMessage);
  }
}

function toNumber(value: CouponDraftValue) {
  if (value === '' || value == null) {
    return 0;
  }
  const amount = Number(value);
  return Number.isFinite(amount) ? amount : 0;
}

function parseDateTime(value: string) {
  if (!value) {
    return null;
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}
