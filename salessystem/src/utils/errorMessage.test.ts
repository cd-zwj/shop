import { describe, expect, it } from 'vitest';
import { ApiError } from '../types/api';
import { getErrorMessage } from './errorMessage';

describe('getErrorMessage', () => {
  it('uses ApiError message', () => {
    expect(getErrorMessage(new ApiError('接口不可用', 500))).toBe('接口不可用');
  });

  it('uses Error message', () => {
    expect(getErrorMessage(new Error('网络错误'))).toBe('网络错误');
  });

  it('falls back for unknown values', () => {
    expect(getErrorMessage({})).toBe('操作失败，请稍后重试');
  });
});
