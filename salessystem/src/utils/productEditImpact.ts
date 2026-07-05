import type { MerchantProductUpsertPayload } from '../types/merchant';

export type ProductEditImpactTone = 'blue' | 'amber' | 'emerald' | 'slate';

export interface ProductEditImpactItem {
  key: string;
  label: string;
  description: string;
  tone: ProductEditImpactTone;
}

export function buildProductEditImpacts(form: MerchantProductUpsertPayload): ProductEditImpactItem[] {
  const impacts: ProductEditImpactItem[] = [
    {
      key: 'display',
      label: '用户侧展示',
      description: '名称、类目、主图、描述和销售状态会直接影响用户在首页、发现页、详情页看到的信息。',
      tone: 'blue',
    },
    {
      key: 'settlement',
      label: '结算金额',
      description: '基础定价会影响购物车、结算页、订单金额和后续退款可退金额，修改前应确认是否已有在售订单。',
      tone: 'amber',
    },
    {
      key: 'inventory',
      label: '库存校验',
      description: Number(form.stock ?? 0) <= 5
        ? '库存已低于或等于 5，用户结算时可能触发库存不足提示，建议补货或下架。'
        : '库存数量会参与下单前校验，库存变化会在购物车结算时重新确认。',
      tone: Number(form.stock ?? 0) <= 5 ? 'amber' : 'emerald',
    },
    {
      key: 'fulfillment',
      label: '履约方式',
      description: getFulfillmentImpactDescription(form),
      tone: 'emerald',
    },
  ];

  if (form.status !== 'active') {
    impacts.push({
      key: 'status',
      label: '销售状态',
      description: '非 active 状态会影响用户购买入口，下单前校验会阻止不可售商品继续结算。',
      tone: 'slate',
    });
  }

  return impacts;
}

function getFulfillmentImpactDescription(form: MerchantProductUpsertPayload) {
  if (form.productType === 'CARD_KEY') {
    return '卡密商品依赖库存池发放，保存后需要保证可售卡密充足，用户支付后会在已购内容查看兑换码。';
  }
  if (form.productType === 'VIRTUAL') {
    return '虚拟内容依赖交付配置，用户支付后会在已购内容重新查看文件、链接或账号信息。';
  }
  if (form.productType === 'SERVICE') {
    return '服务商品会生成核销码，用户到店或履约时出示，商户在订单中心核销。';
  }
  if (form.productType === 'SUBSCRIPTION') {
    return '订阅权益会影响有效期和用户可持续使用时间，建议在交付配置中说明 validityDays。';
  }
  return '实物商品依赖地址快照和商户发货，用户支付后会进入待发货/已发货流程。';
}
