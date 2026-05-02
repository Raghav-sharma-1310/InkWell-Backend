/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { ProtectedRoute } from '../components/layout/ProtectedRoute';
import { ThemeProvider } from '../context/ThemeContext';
import { AuthProvider } from '../context/AuthContext';
import { NotificationProvider } from '../context/NotificationContext';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: 0 } })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

// Defines render protected so related behavior stays grouped in one place.
function renderProtected(user, roles, route = '/protected') {
  if (user) {
    localStorage.setItem('inkwell.user', JSON.stringify(user));
    localStorage.setItem('inkwell.accessToken', 'mock-token');
  } else {
    localStorage.removeItem('inkwell.user');
    localStorage.removeItem('inkwell.accessToken');
  }

  return render(
    <MemoryRouter initialEntries={[route]}>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <Routes>
              <Route path="/login" element={<div>Login Page</div>} />
              <Route path="/" element={<div>Home Page</div>} />
              <Route element={<ProtectedRoute roles={roles} />}>
                <Route path="/protected" element={<div>Protected Content</div>} />
              </Route>
            </Routes>
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

// Handles protected route requests so the UI can call this feature through a stable endpoint.
describe('ProtectedRoute', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  // Performs the redirects to /login when not authenticated workflow so callers do not duplicate this logic.
  it('redirects to /login when not authenticated', () => {
    renderProtected(null);
    expect(screen.getByText('Login Page')).toBeInTheDocument();
  });

  // Verifies allows access when authenticated with no role restriction so regressions are caught during automated tests.
  it('allows access when authenticated with no role restriction', () => {
    renderProtected({ role: 'READER' });
    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  // Verifies allows access when user has matching role so regressions are caught during automated tests.
  it('allows access when user has matching role', () => {
    renderProtected({ role: 'ADMIN' }, ['ADMIN']);
    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  // Defines redirects to / when user role does not match so related behavior stays grouped in one place.
  it('redirects to / when user role does not match', () => {
    renderProtected({ role: 'READER' }, ['ADMIN']);
    expect(screen.getByText('Home Page')).toBeInTheDocument();
  });
});
