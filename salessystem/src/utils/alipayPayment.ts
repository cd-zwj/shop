const ALIPAY_PAYLOAD_PREFIX = 'salessystem:alipay:payload:';

export interface AlipayPaymentPayload {
  billNo: string;
  bizNo?: string | null;
  orderNo?: string | null;
  source: 'order' | 'recharge' | 'merchant-recharge';
  payHtml: string;
  amount?: number | null;
  title?: string | null;
  createdAt: number;
}

function getStorage() {
  if (typeof window === 'undefined') {
    return null;
  }

  return window.sessionStorage;
}

export function saveAlipayPaymentPayload(payload: Omit<AlipayPaymentPayload, 'createdAt'>): void {
  const storage = getStorage();
  if (!storage || !payload.billNo || !payload.payHtml) {
    return;
  }

  storage.setItem(
    `${ALIPAY_PAYLOAD_PREFIX}${payload.billNo}`,
    JSON.stringify({
      ...payload,
      createdAt: Date.now(),
    }),
  );
}

export function readAlipayPaymentPayload(billNo: string | null): AlipayPaymentPayload | null {
  const storage = getStorage();
  if (!storage || !billNo) {
    return null;
  }

  const raw = storage.getItem(`${ALIPAY_PAYLOAD_PREFIX}${billNo}`);
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as Partial<AlipayPaymentPayload>;
    if (parsed.billNo !== billNo || typeof parsed.payHtml !== 'string') {
      return null;
    }

    return {
      billNo,
      bizNo: parsed.bizNo ?? null,
      orderNo: parsed.orderNo ?? null,
      source: parsed.source === 'merchant-recharge' || parsed.source === 'recharge' ? parsed.source : 'order',
      payHtml: parsed.payHtml,
      amount: typeof parsed.amount === 'number' ? parsed.amount : null,
      title: parsed.title ?? null,
      createdAt: typeof parsed.createdAt === 'number' ? parsed.createdAt : 0,
    };
  } catch {
    return null;
  }
}

export function clearAlipayPaymentPayload(billNo: string | null): void {
  const storage = getStorage();
  if (!storage || !billNo) {
    return;
  }

  storage.removeItem(`${ALIPAY_PAYLOAD_PREFIX}${billNo}`);
}

export function openAlipayPaymentWindow(payHtml: string): boolean {
  if (typeof window === 'undefined' || !payHtml.trim()) {
    return false;
  }

  const payWindow = window.open('', '_blank', 'noopener,noreferrer');
  if (!payWindow) {
    return false;
  }

  payWindow.document.open();
  payWindow.document.write(payHtml);
  payWindow.document.close();
  return true;
}

export function buildPaymentStatusPath(params: {
  billNo: string;
  orderNo?: string | null;
  bizNo?: string | null;
  source: 'order' | 'recharge' | 'merchant-recharge';
  reused?: boolean | null;
}): string {
  const searchParams = new URLSearchParams({
    billNo: params.billNo,
    source: params.source,
  });

  if (params.orderNo) {
    searchParams.set('orderNo', params.orderNo);
  }

  if (params.bizNo) {
    searchParams.set('bizNo', params.bizNo);
  }

  if (typeof params.reused === 'boolean') {
    searchParams.set('reused', params.reused ? '1' : '0');
  }

  return `/payment/status?${searchParams.toString()}`;
}

export function buildAlipayLaunchPath(params: {
  billNo: string;
  orderNo?: string | null;
  bizNo?: string | null;
  source: 'order' | 'recharge' | 'merchant-recharge';
  reused?: boolean | null;
}): string {
  const searchParams = new URLSearchParams({
    billNo: params.billNo,
    source: params.source,
  });

  if (params.orderNo) {
    searchParams.set('orderNo', params.orderNo);
  }

  if (params.bizNo) {
    searchParams.set('bizNo', params.bizNo);
  }

  if (typeof params.reused === 'boolean') {
    searchParams.set('reused', params.reused ? '1' : '0');
  }

  return `/payment/alipay/launch?${searchParams.toString()}`;
}
