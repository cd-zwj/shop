import type { PageResult } from '../types/api';

export function getPageCurrent<T>(page: PageResult<T> | null | undefined): number {
  return page?.current ?? page?.page ?? 1;
}

export function getPageTotalPages<T>(page: PageResult<T> | null | undefined): number {
  if (!page) return 0;
  if (page.pages != null) return page.pages;
  if (page.size <= 0 || page.total <= 0) return 0;
  return Math.ceil(page.total / page.size);
}
