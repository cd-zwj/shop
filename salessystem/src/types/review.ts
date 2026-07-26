export interface StoreReview {
  id: number;
  storeId: number;
  orderNo: string;
  rating: number;
  content: string | null;
  imageUrls: string[];
  merchantReply: string | null;
  merchantReplyTime: string | null;
  status: 'VISIBLE' | 'HIDDEN';
  moderationRemark: string | null;
  createTime: string | null;
}

export interface StoreReviewCreatePayload {
  rating: number;
  content?: string;
  imageUrls?: string[];
}
