/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { OAuthSuccessPage } from '../pages/public/OAuthSuccessPage';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { NotificationProvider } from '../context/NotificationContext';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

// Defines render page so related behavior stays grouped in one place.
function renderPage(searchParams = '') {
  localStorage.removeItem('inkwell.user');
  return render(
    <MemoryRouter initialEntries={[`/oauth/success${searchParams}`]}>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <Routes>
              <Route path="oauth/success" element={<OAuthSuccessPage />} />
              <Route path="login" element={<div>Login Page</div>} />
              <Route path="admin" element={<div>Admin Dashboard</div>} />
              <Route path="author" element={<div>Author Studio</div>} />
              <Route path="profile" element={<div>Reader Profile</div>} />
            </Routes>
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

// Provides oauth success page wiring so the framework can apply the expected runtime behavior.
describe('OAuthSuccessPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    api.get.mockResolvedValue({ data: { data: 0 } });
  });

  // Performs the shows loading state workflow so callers do not duplicate this logic.
  it('shows loading state', () => {
    api.get.mockImplementation(() => new Promise(() => {}));
    renderPage('?accessToken=mock-token&refreshToken=mock-refresh');
    expect(screen.getByText('Completing sign in...')).toBeInTheDocument();
    expect(screen.getByText('Please wait while we verify your account.')).toBeInTheDocument();
  });

  // Performs the redirects to login when no access token workflow so callers do not duplicate this logic.
  it('redirects to login when no access token', async () => {
    renderPage('');
    await waitFor(() => {
      expect(screen.getByText('Login Page')).toBeInTheDocument();
    });
  });

  // Performs the stores tokens and fetches user profile workflow so callers do not duplicate this logic.
  it('stores tokens and fetches user profile', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/auth/me') {
        return Promise.resolve({
          data: {
            data: { userId: 'u1', fullName: 'OAuth User', role: 'READER', username: 'oauthuser', email: 'oauth@test.com' },
          },
        });
      }
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage('?accessToken=test-access-token&refreshToken=test-refresh-token');

    await waitFor(() => {
      expect(localStorage.getItem('inkwell.accessToken')).toBe('test-access-token');
      expect(localStorage.getItem('inkwell.refreshToken')).toBe('test-refresh-token');
    });

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/api/auth/me');
    });
  });

  // Defines redirects admin to admin dashboard so related behavior stays grouped in one place.
  it('redirects admin to admin dashboard', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/auth/me') {
        return Promise.resolve({
          data: { data: { userId: 'u1', fullName: 'Admin', role: 'ADMIN', username: 'admin', email: 'admin@test.com' } },
        });
      }
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage('?accessToken=test-token');
    await waitFor(() => {
      expect(screen.getByText('Admin Dashboard')).toBeInTheDocument();
    });
  });

  // Provides redirects author to author studio wiring so the framework can apply the expected runtime behavior.
  it('redirects author to author studio', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/auth/me') {
        return Promise.resolve({
          data: { data: { userId: 'u1', fullName: 'Author', role: 'AUTHOR', username: 'author', email: 'author@test.com' } },
        });
      }
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage('?accessToken=test-token');
    await waitFor(() => {
      expect(screen.getByText('Author Studio')).toBeInTheDocument();
    });
  });

  // Defines redirects reader to profile so related behavior stays grouped in one place.
  it('redirects reader to profile', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/auth/me') {
        return Promise.resolve({
          data: { data: { userId: 'u1', fullName: 'Reader', role: 'READER', username: 'reader', email: 'reader@test.com' } },
        });
      }
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage('?accessToken=test-token');
    await waitFor(() => {
      expect(screen.getByText('Reader Profile')).toBeInTheDocument();
    });
  });

  // Verifies handles api error and redirects to login so regressions are caught during automated tests.
  it('handles API error and redirects to login', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/auth/me') {
        return Promise.reject(new Error('unauthorized'));
      }
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage('?accessToken=test-token');
    await waitFor(() => {
      expect(screen.getByText('Login Page')).toBeInTheDocument();
    });
  });

  // Verifies handles token without refresh token so regressions are caught during automated tests.
  it('handles token without refresh token', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/auth/me') {
        return Promise.resolve({
          data: { data: { userId: 'u1', fullName: 'Reader', role: 'READER', username: 'reader', email: 'r@t.com' } },
        });
      }
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage('?accessToken=test-token');
    await waitFor(() => {
      expect(localStorage.getItem('inkwell.accessToken')).toBe('test-token');
      expect(localStorage.getItem('inkwell.refreshToken')).toBeNull();
    });
  });
});
