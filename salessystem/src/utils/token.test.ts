import { describe, it, expect, beforeEach, vi } from 'vitest';
import { getToken, setToken, clearToken, clearAllTokens } from './token';

// Mock localStorage
const localStorageMock = (() => {
  const store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value;
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key];
    }),
    clear: vi.fn(() => {
      Object.keys(store).forEach((key) => delete store[key]);
    }),
    get length() {
      return Object.keys(store).length;
    },
    key: vi.fn((_index: number) => null),
  };
})();

beforeEach(() => {
  vi.stubGlobal('window', { localStorage: localStorageMock });
  localStorageMock.clear();
  vi.clearAllMocks();
});

describe('setToken / getToken', () => {
  it('应能设置并获取用户端 token', () => {
    // Arrange
    const token = 'user-token-123';

    // Act
    setToken('user', token);

    // Assert
    expect(localStorageMock.setItem).toHaveBeenCalledWith(
      'salessystem:app:token',
      token,
    );
  });

  it('应能设置并获取商户端 token', () => {
    // Arrange
    const token = 'merchant-token-456';

    // Act
    setToken('merchant', token);

    // Assert
    expect(localStorageMock.setItem).toHaveBeenCalledWith(
      'salessystem:merchant:token',
      token,
    );
  });

  it('应能设置并获取管理端 token', () => {
    // Arrange
    const token = 'admin-token-789';

    // Act
    setToken('admin', token);

    // Assert
    expect(localStorageMock.setItem).toHaveBeenCalledWith(
      'salessystem:platform:token',
      token,
    );
  });
});

describe('getToken', () => {
  it('存储中存在 token 时应返回该 token', () => {
    // Arrange
    localStorageMock.getItem.mockReturnValue('stored-token');

    // Act
    const result = getToken('user');

    // Assert
    expect(result).toBe('stored-token');
    expect(localStorageMock.getItem).toHaveBeenCalledWith('salessystem:app:token');
  });

  it('存储中不存在 token 时应返回 null', () => {
    // Arrange
    localStorageMock.getItem.mockReturnValue(null);

    // Act
    const result = getToken('admin');

    // Assert
    expect(result).toBeNull();
  });
});

describe('clearToken', () => {
  it('应清除指定角色的 token', () => {
    // Arrange & Act
    clearToken('merchant');

    // Assert
    expect(localStorageMock.removeItem).toHaveBeenCalledWith(
      'salessystem:merchant:token',
    );
  });
});

describe('clearAllTokens', () => {
  it('应清除所有三端 token', () => {
    // Arrange & Act
    clearAllTokens();

    // Assert
    expect(localStorageMock.removeItem).toHaveBeenCalledTimes(3);
    expect(localStorageMock.removeItem).toHaveBeenCalledWith('salessystem:app:token');
    expect(localStorageMock.removeItem).toHaveBeenCalledWith('salessystem:merchant:token');
    expect(localStorageMock.removeItem).toHaveBeenCalledWith('salessystem:platform:token');
  });
});

describe('SSR 环境（window 不存在）', () => {
  it('无 window 时 getToken 应返回 null', () => {
    // Arrange
    vi.stubGlobal('window', undefined);

    // Act
    const result = getToken('user');

    // Assert
    expect(result).toBeNull();
  });

  it('无 window 时 setToken 不应抛错', () => {
    // Arrange
    vi.stubGlobal('window', undefined);

    // Act & Assert
    expect(() => setToken('user', 'token')).not.toThrow();
  });
});
