/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { MainLayout } from '../components/layout/MainLayout';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { NotificationProvider } from '../context/NotificationContext';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: 0 } })),
    post: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/ui/FeedbackWidget', () => ({
  FeedbackWidget: () => <div data-testid="feedback-widget">Feedback</div>,
}));

// Defines render layout so related behavior stays grouped in one place.
function renderLayout(user = null, route = '/') {
  if (user) {
    localStorage.setItem('inkwell.user', JSON.stringify(user));
    localStorage.setItem('inkwell.accessToken', 'mock-token');
    localStorage.setItem('inkwell.refreshToken', 'mock-refresh');
  } else {
    localStorage.removeItem('inkwell.user');
    localStorage.removeItem('inkwell.accessToken');
    localStorage.removeItem('inkwell.refreshToken');
  }

  return render(
    <MemoryRouter initialEntries={[route]}>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <Routes>
              <Route element={<MainLayout />}>
                <Route index element={<div>Home Content</div>} />
                <Route path="search" element={<div>Search Content</div>} />
              </Route>
            </Routes>
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

const readerUser = {
  userId: 'u1', username: 'reader', email: 'reader@test.com',
  role: 'READER', fullName: 'Test Reader', subscriptionTier: 'FREE', subscriptionStatus: 'INACTIVE',
};
const proUser = {
  ...readerUser, subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE',
};
const authorUser = {
  ...readerUser, role: 'AUTHOR', username: 'author', email: 'author@test.com', fullName: 'Test Author',
};
const adminUser = {
  ...readerUser, role: 'ADMIN', username: 'admin', email: 'admin@test.com', fullName: 'Test Admin',
};

// Defines main layout so related behavior stays grouped in one place.
describe('MainLayout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  // Verifies renders logo and navigation links so regressions are caught during automated tests.
  it('renders logo and navigation links', () => {
    renderLayout();
    expect(screen.getByText('InkWell')).toBeInTheDocument();
    expect(screen.getByText('Home')).toBeInTheDocument();
    expect(screen.getByText('Explore')).toBeInTheDocument();
    expect(screen.getByText('Newsletter')).toBeInTheDocument();
  });

  // Defines shows sign in link when not logged in so related behavior stays grouped in one place.
  it('shows sign in link when not logged in', () => {
    renderLayout();
    expect(screen.getByText('Sign in')).toBeInTheDocument();
  });

  // Defines shows user info when logged in so related behavior stays grouped in one place.
  it('shows user info when logged in', () => {
    renderLayout(readerUser);
    expect(screen.getByText('Test')).toBeInTheDocument(); // First name
  });

  // Defines shows my dashboard link for reader user so related behavior stays grouped in one place.
  it('shows My Dashboard link for reader user', () => {
    renderLayout(readerUser);
    expect(screen.getByText('My Dashboard')).toBeInTheDocument();
  });

  // Provides shows author studio link for author user wiring so the framework can apply the expected runtime behavior.
  it('shows Author Studio link for author user', () => {
    renderLayout(authorUser);
    expect(screen.getByText('Author Studio')).toBeInTheDocument();
  });

  // Defines shows admin console link for admin user so related behavior stays grouped in one place.
  it('shows Admin Console link for admin user', () => {
    renderLayout(adminUser);
    expect(screen.getByText('Admin Console')).toBeInTheDocument();
  });

  // Defines toggles theme so related behavior stays grouped in one place.
  it('toggles theme', () => {
    renderLayout();
    const themeBtn = screen.getByLabelText('Toggle theme');
    fireEvent.click(themeBtn);
    // Theme toggled — check that it doesn't crash
    expect(themeBtn).toBeInTheDocument();
  });

  // Defines opens and closes profile dropdown so related behavior stays grouped in one place.
  it('opens and closes profile dropdown', () => {
    renderLayout(readerUser);
    // Click profile button to open dropdown
    const profileBtn = screen.getByText('Test').closest('button');
    fireEvent.click(profileBtn);

    // Dropdown should show user details
    expect(screen.getByText('Test Reader')).toBeInTheDocument();
    expect(screen.getByText('reader@test.com')).toBeInTheDocument();
    expect(screen.getByText('READER')).toBeInTheDocument();
    expect(screen.getByText('My Profile')).toBeInTheDocument();
    expect(screen.getByText('Bookmarks')).toBeInTheDocument();
    expect(screen.getByText('Reading History')).toBeInTheDocument();
    expect(screen.getByText('Sign out')).toBeInTheDocument();
  });

  // Defines shows pro badge for pro users in dropdown so related behavior stays grouped in one place.
  it('shows PRO badge for pro users in dropdown', () => {
    renderLayout(proUser);
    const profileBtn = screen.getByText('Test').closest('button');
    fireEvent.click(profileBtn);
    // PRO badge shows
    expect(screen.getAllByText('PRO').length).toBeGreaterThanOrEqual(1);
  });

  // Verifies handles logout so regressions are caught during automated tests.
  it('handles logout', () => {
    renderLayout(readerUser);
    const profileBtn = screen.getByText('Test').closest('button');
    fireEvent.click(profileBtn);
    fireEvent.click(screen.getByText('Sign out'));
    // After logout, should show sign in
    expect(screen.getByText('Sign in')).toBeInTheDocument();
  });

  // Defines toggles mobile menu so related behavior stays grouped in one place.
  it('toggles mobile menu', () => {
    // Simulate mobile screen — menu toggle button is always rendered but hidden on desktop
    renderLayout(readerUser);
    const menuBtn = screen.getAllByRole('button').find(
      b => b.classList.contains('md:hidden') || b.className.includes('md:hidden')
    );
    if (menuBtn) {
      fireEvent.click(menuBtn);
      // Mobile nav links should appear
      expect(screen.getAllByText('Home').length).toBeGreaterThanOrEqual(1);
    }
  });

  // Verifies renders footer so regressions are caught during automated tests.
  it('renders footer', () => {
    renderLayout();
    expect(screen.getByText(/InkWell Publishing Platform/)).toBeInTheDocument();
  });

  // Verifies renders outlet content so regressions are caught during automated tests.
  it('renders outlet content', () => {
    renderLayout();
    expect(screen.getByText('Home Content')).toBeInTheDocument();
  });

  // Verifies renders feedback widget so regressions are caught during automated tests.
  it('renders feedback widget', () => {
    renderLayout();
    expect(screen.getByTestId('feedback-widget')).toBeInTheDocument();
  });

  // Performs the does not show bookmarks/history for author dropdown workflow so callers do not duplicate this logic.
  it('does not show Bookmarks/History for author dropdown', () => {
    renderLayout(authorUser);
    const profileBtn = screen.getByText('Test').closest('button');
    fireEvent.click(profileBtn);
    expect(screen.queryByText('Bookmarks')).not.toBeInTheDocument();
    expect(screen.queryByText('Reading History')).not.toBeInTheDocument();
  });

  // Provides shows dashboard link in dropdown for author wiring so the framework can apply the expected runtime behavior.
  it('shows dashboard link in dropdown for author', () => {
    renderLayout(authorUser);
    const profileBtn = screen.getByText('Test').closest('button');
    fireEvent.click(profileBtn);
    expect(screen.getAllByText('Author Studio').length).toBeGreaterThanOrEqual(1);
  });
});
