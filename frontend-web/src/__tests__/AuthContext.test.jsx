/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { NotificationProvider } from '../context/NotificationContext';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import api from '../api/client';

vi.mock('../api/client', () => ({
  default: {
    post: vi.fn(() => Promise.resolve({ data: {} })),
    get: vi.fn(() => Promise.resolve({ data: { data: 0 } })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

// Verifies test consumer so regressions are caught during automated tests.
function TestConsumer() {
  const { user, login, logout, register, updateAuthState, refreshUser, loading } = useAuth();
  return (
    <div>
      <div data-testid="user">{user ? user.email : 'none'}</div>
      <div data-testid="loading">{loading ? 'true' : 'false'}</div>
      <button data-testid="login" onClick={() => login({ email: 'test@test.com', password: 'pass' })}>Login</button>
      <button data-testid="register" onClick={() => register({ email: 'r@test.com', password: 'pass' })}>Register</button>
      <button data-testid="logout" onClick={logout}>Logout</button>
      <button data-testid="update" onClick={() => updateAuthState('newToken', 'newRefresh', { email: 'new@test.com' })}>Update</button>
      <button data-testid="refresh" onClick={refreshUser}>Refresh</button>
    </div>
  );
}

// Defines render consumer so related behavior stays grouped in one place.
function renderConsumer(user = null) {
  if (user) {
    localStorage.setItem('inkwell.user', JSON.stringify(user));
  } else {
    localStorage.removeItem('inkwell.user');
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

// Provides auth context wiring so the framework can apply the expected runtime behavior.
describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  // Defines starts with no user when local storage is empty so related behavior stays grouped in one place.
  it('starts with no user when localStorage is empty', () => {
    renderConsumer();
    expect(screen.getByTestId('user').textContent).toBe('none');
  });

  // Defines restores user from local storage so related behavior stays grouped in one place.
  it('restores user from localStorage', () => {
    renderConsumer({ email: 'stored@test.com' });
    expect(screen.getByTestId('user').textContent).toBe('stored@test.com');
  });

  // Defines logs in successfully so related behavior stays grouped in one place.
  it('logs in successfully', async () => {
    api.post.mockResolvedValueOnce({
      data: { data: { user: { email: 'test@test.com' }, accessToken: 'at', refreshToken: 'rt' } },
    });
    renderConsumer();
    fireEvent.click(screen.getByTestId('login'));
    await waitFor(() => expect(screen.getByTestId('user').textContent).toBe('test@test.com'));
    expect(localStorage.getItem('inkwell.accessToken')).toBe('at');
  });

  // Performs the registers successfully workflow so callers do not duplicate this logic.
  it('registers successfully', async () => {
    api.post.mockResolvedValueOnce({
      data: { data: { user: { email: 'r@test.com' }, accessToken: 'at2', refreshToken: 'rt2' } },
    });
    renderConsumer();
    fireEvent.click(screen.getByTestId('register'));
    await waitFor(() => expect(screen.getByTestId('user').textContent).toBe('r@test.com'));
  });

  // Defines logs out and clears state so related behavior stays grouped in one place.
  it('logs out and clears state', async () => {
    renderConsumer({ email: 'logged@test.com' });
    expect(screen.getByTestId('user').textContent).toBe('logged@test.com');
    fireEvent.click(screen.getByTestId('logout'));
    await waitFor(() => expect(screen.getByTestId('user').textContent).toBe('none'));
    expect(localStorage.getItem('inkwell.accessToken')).toBeNull();
  });

  // Performs the updates auth state workflow so callers do not duplicate this logic.
  it('updates auth state', async () => {
    renderConsumer();
    fireEvent.click(screen.getByTestId('update'));
    await waitFor(() => expect(screen.getByTestId('user').textContent).toBe('new@test.com'));
    expect(localStorage.getItem('inkwell.accessToken')).toBe('newToken');
  });

  // Performs the refreshes user from api workflow so callers do not duplicate this logic.
  it('refreshes user from API', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/auth/me') {
        return Promise.resolve({ data: { data: { email: 'refreshed@test.com' } } });
      }
      return Promise.resolve({ data: { data: 0 } });
    });
    renderConsumer({ email: 'old@test.com' });
    fireEvent.click(screen.getByTestId('refresh'));
    await waitFor(() => expect(screen.getByTestId('user').textContent).toBe('refreshed@test.com'));
  });

  // Verifies handles refresh failure gracefully so regressions are caught during automated tests.
  it('handles refresh failure gracefully', async () => {
    api.get.mockRejectedValueOnce(new Error('Network error'));
    renderConsumer({ email: 'old@test.com' });
    fireEvent.click(screen.getByTestId('refresh'));
    // User stays the same on failure
    await waitFor(() => expect(screen.getByTestId('user').textContent).toBe('old@test.com'));
  });

  // Verifies handles corrupted local storage so regressions are caught during automated tests.
  it('handles corrupted localStorage', () => {
    localStorage.setItem('inkwell.user', 'not-valid-json');
    renderConsumer();
    expect(screen.getByTestId('user').textContent).toBe('none');
  });
});
