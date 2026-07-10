import type { AssetTracePresentation } from './wallet';

/** 成长值概览 — 对应后端 MemberGrowthAccountVO */
export interface GrowthOverview {
  totalGrowth: number;
  levelId: number;
  levelName: string;
  /** null 表示已达最高等级 */
  nextLevelGrowth: number | null;
  discountRate?: number | null;
  benefitJson?: string | null;
}

/** 成长值变动日志 — 对应后端 MemberGrowthLogVO */
export interface GrowthLog {
  id: number;
  changeType: string;   // EARN | DEDUCT | ADJUST
  changeGrowth: number;
  growthBefore: number;
  growthAfter: number;
  bizType: string;      // ORDER | RECHARGE | MANUAL
  bizNo: string | null;
  remark: string | null;
  createTime: string;
  trace?: AssetTracePresentation | null;
}
