export interface MemberPointsAccount {
  id: number;
  tenantId: number;
  platformUserId: number;
  points: number;
  totalEarned: number;
  totalUsed: number;
  version?: number | null;
  status?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface MemberPointsLog {
  id: number;
  tenantId: number;
  platformUserId: number;
  bizType: string;
  bizNo: string;
  changePoints: number;
  pointsBefore: number;
  pointsAfter: number;
  remark?: string | null;
  createTime?: string | null;
}

