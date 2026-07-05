import {
  CreditCard,
  Gift,
  Info,
  Package,
  RefreshCcw,
  Sparkles,
  Wallet,
  type LucideIcon,
} from 'lucide-react';

export interface NotificationPresentation {
  label: string;
  icon: LucideIcon;
}

const PRESENTATIONS: Record<string, NotificationPresentation> = {
  ORDER: { label: '订单', icon: Package },
  PAYMENT: { label: '支付', icon: CreditCard },
  REFUND: { label: '售后', icon: RefreshCcw },
  COUPON: { label: '优惠券', icon: Gift },
  PROMOTION: { label: '活动', icon: Sparkles },
  WALLET: { label: '钱包', icon: Wallet },
  SYSTEM: { label: '系统', icon: Info },
};

export function getNotificationPresentation(category: string | null | undefined): NotificationPresentation {
  if (!category) {
    return { label: '通知', icon: Info };
  }

  return PRESENTATIONS[category] ?? { label: '通知', icon: Info };
}
