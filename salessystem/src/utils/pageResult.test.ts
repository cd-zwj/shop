import { describe, expect, it } from 'vitest';
import { getPageCurrent, getPageTotalPages } from './pageResult';
import type { PageResult } from '../types/api';

describe('pageResult helpers', () => {
  it('prefers current when both current and page exist', () => {
    const page = { records: [], total: 0, size: 10, page: 2, current: 3 } as PageResult<unknown>;

    expect(getPageCurrent(page)).toBe(3);
  });

  it('uses page when current is missing', () => {
    const page = { records: [], total: 0, size: 10, page: 2 } as PageResult<unknown>;

    expect(getPageCurrent(page)).toBe(2);
  });

  it('calculates pages when pages is missing', () => {
    const page = { records: [], total: 21, size: 10, page: 1 } as PageResult<unknown>;

    expect(getPageTotalPages(page)).toBe(3);
  });

  it('returns zero pages when size is invalid', () => {
    const page = { records: [], total: 21, size: 0, page: 1 } as PageResult<unknown>;

    expect(getPageTotalPages(page)).toBe(0);
  });
});
