import React, { act } from 'react';
import { createRoot } from 'react-dom/client';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import NotFoundPage from './NotFoundPage';

const mockUseAuth = vi.fn();

vi.mock('../context/AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

function renderNotFound(currentRole: 'user' | 'admin' | 'merchant' | null) {
  mockUseAuth.mockReturnValue({ currentRole });
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);
  act(() => {
    root.render(
      React.createElement(
        MemoryRouter,
        null,
        React.createElement(NotFoundPage),
      ),
    );
  });
  return { container, root };
}

describe('NotFoundPage', () => {
  it('shows home action for logged-in users', () => {
    const { container, root } = renderNotFound('user');

    expect(container.textContent).toContain('页面不存在');
    expect(container.textContent).toContain('返回首页');
    act(() => root.unmount());
    container.remove();
  });

  it('shows login action for anonymous users', () => {
    const { container, root } = renderNotFound(null);

    expect(container.textContent).toContain('页面不存在');
    expect(container.textContent).toContain('返回登录');
    act(() => root.unmount());
    container.remove();
  });
});
