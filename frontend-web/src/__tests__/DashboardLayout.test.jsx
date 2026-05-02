/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { DashboardLayout } from '../components/dashboard/DashboardLayout';

const authState = vi.hoisted(() => ({
  user: { userId: 'u1', fullName: 'Test Author', email: 'author@test.com', role: 'AUTHOR' },
  loading: false,
}));

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: 0 } })),
    post: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../context/AuthContext', () => ({
  AuthProvider: ({ children }) => children,
  useAuth: () => authState,
}));

const mockLinks = [
  { to: '/dashboard', label: 'Overview', end: true },
  { to: '/dashboard/posts', label: 'Posts' },
  { to: '/dashboard/settings', label: 'Settings' },
];

// Defines dashboard layout so related behavior stays grouped in one place.
describe('DashboardLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authState.user = { userId: 'u1', fullName: 'Test Author', email: 'author@test.com', role: 'AUTHOR' };
    authState.loading = false;
  });

  // Verifies renders title and user info so regressions are caught during automated tests.
  it('renders title and user info', () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="/dashboard" element={<DashboardLayout title="Author Studio" links={mockLinks} />}>
            <Route index element={<div>Dashboard Content</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Author Studio')).toBeInTheDocument();
    expect(screen.getByText('Test Author')).toBeInTheDocument();
    expect(screen.getByText('author@test.com')).toBeInTheDocument();
  });

  // Verifies renders navigation links so regressions are caught during automated tests.
  it('renders navigation links', () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="/dashboard" element={<DashboardLayout title="Test" links={mockLinks} />}>
            <Route index element={<div>Content</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Overview')).toBeInTheDocument();
    expect(screen.getByText('Posts')).toBeInTheDocument();
    expect(screen.getByText('Settings')).toBeInTheDocument();
  });

  // Verifies renders outlet content so regressions are caught during automated tests.
  it('renders outlet content', () => {
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="/dashboard" element={<DashboardLayout title="Test" links={mockLinks} />}>
            <Route index element={<div>Dashboard Content Here</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Dashboard Content Here')).toBeInTheDocument();
  });

  // Verifies renders fallback when user is null so regressions are caught during automated tests.
  it('renders fallback when user is null', () => {
    authState.user = null;

    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <Routes>
          <Route path="/dashboard" element={<DashboardLayout title="Test" links={mockLinks} />}>
            <Route index element={<div>Content</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });
});
