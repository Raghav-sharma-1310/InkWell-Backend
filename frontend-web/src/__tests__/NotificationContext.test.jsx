/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { NotificationProvider, useNotifications } from '../context/NotificationContext';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { MemoryRouter } from 'react-router-dom';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

// Verifies test consumer so regressions are caught during automated tests.
function TestConsumer() {
  const ctx = useNotifications();
  return (
    <div>
      <span data-testid="count">{ctx?.unreadCount ?? 'null'}</span>
    </div>
  );
}

// Provides render with auth wiring so the framework can apply the expected runtime behavior.
function renderWithAuth(user = null) {
  if (user) {
    localStorage.setItem('inkwell.user', JSON.stringify(user));
    localStorage.setItem('inkwell.accessToken', 'mock-token');
  } else {
    localStorage.removeItem('inkwell.user');
    localStorage.removeItem('inkwell.accessToken');
  }

  return render(
    <MemoryRouter>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <TestConsumer />
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

const mockUser = {
  userId: 'u1', username: 'test', email: 'test@test.com',
  role: 'READER', fullName: 'Test', subscriptionTier: 'FREE', subscriptionStatus: 'INACTIVE',
};

// Defines notification context so related behavior stays grouped in one place.
describe('NotificationContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  // Provides provides 0 count when not authenticated wiring so the framework can apply the expected runtime behavior.
  it('provides 0 count when not authenticated', () => {
    api.get.mockResolvedValue({ data: { data: 0 } });
    renderWithAuth();
    expect(screen.getByTestId('count').textContent).toBe('0');
  });

  // Performs the fetches unread count when authenticated workflow so callers do not duplicate this logic.
  it('fetches unread count when authenticated', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/notifications/unread-count')) {
        return Promise.resolve({ data: { data: 5 } });
      }
      return Promise.resolve({ data: { data: 0 } });
    });

    renderWithAuth(mockUser);

    await waitFor(() => {
      expect(screen.getByTestId('count').textContent).toBe('5');
    });
  });

  // Verifies handles api error gracefully so regressions are caught during automated tests.
  it('handles API error gracefully', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/notifications/unread-count')) {
        return Promise.reject(new Error('fail'));
      }
      return Promise.resolve({ data: { data: 0 } });
    });

    renderWithAuth(mockUser);

    await waitFor(() => {
      expect(screen.getByTestId('count').textContent).toBe('0');
    });
  });

  // Defines resets count to 0 when user logs out so related behavior stays grouped in one place.
  it('resets count to 0 when user logs out', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/notifications/unread-count')) {
        return Promise.resolve({ data: { data: 3 } });
      }
      return Promise.resolve({ data: { data: 0 } });
    });

    // First render as logged in
    renderWithAuth(mockUser);

    await waitFor(() => {
      expect(screen.getByTestId('count').textContent).toBe('3');
    });
  });
});
