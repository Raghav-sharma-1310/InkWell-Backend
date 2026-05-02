/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { HistoryPage } from '../pages/reader/HistoryPage';
import { ThemeProvider } from '../context/ThemeContext';
import { AuthProvider } from '../context/AuthContext';
import { NotificationProvider } from '../context/NotificationContext';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve()),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/ui/PostImage', () => ({
  PostImage: ({ alt }) => <img alt={alt} data-testid="post-image" />,
}));

import api from '../api/client';

const mockPost = {
  postId: 'p1', slug: 'test-slug', title: 'Test Post Title', categorySlug: 'tech',
  readTimeMin: 5, featuredImageUrl: null,
};

// Defines render page so related behavior stays grouped in one place.
function renderPage(user) {
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
            <HistoryPage />
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

// Defines history page so related behavior stays grouped in one place.
describe('HistoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    // Default: api.get returns empty for notification count
    api.get.mockImplementation((url) => {
      if (url === '/api/notifications/unread-count') {
        return Promise.resolve({ data: { data: 0 } });
      }
      return Promise.resolve({ data: { data: { content: [] } } });
    });
  });

  // Defines shows premium prompt for free users so related behavior stays grouped in one place.
  it('shows premium prompt for free users', () => {
    renderPage({ subscriptionTier: 'FREE', subscriptionStatus: 'INACTIVE' });
    expect(screen.getByText('Premium Feature')).toBeInTheDocument();
  });

  // Defines shows empty state when no history for pro user so related behavior stays grouped in one place.
  it('shows empty state when no history for pro user', async () => {
    renderPage({ subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE' });
    await waitFor(() => expect(screen.getByText('No reading history')).toBeInTheDocument());
  });

  // Defines displays history items for pro user so related behavior stays grouped in one place.
  it('displays history items for pro user', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/notifications/unread-count') {
        return Promise.resolve({ data: { data: 0 } });
      }
      return Promise.resolve({ data: { data: { content: [mockPost] } } });
    });
    renderPage({ subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE' });
    await waitFor(() => expect(screen.getByText('Test Post Title')).toBeInTheDocument());
  });

  // Defines shows error on 403 so related behavior stays grouped in one place.
  it('shows error on 403', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/notifications/unread-count') {
        return Promise.resolve({ data: { data: 0 } });
      }
      return Promise.reject({ response: { status: 403 } });
    });
    renderPage({ subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE' });
    await waitFor(() => expect(screen.getByText('Premium subscription required.')).toBeInTheDocument());
  });

  // Defines shows generic error on failure so related behavior stays grouped in one place.
  it('shows generic error on failure', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/notifications/unread-count') {
        return Promise.resolve({ data: { data: 0 } });
      }
      return Promise.reject({ response: { status: 500 } });
    });
    renderPage({ subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE' });
    await waitFor(() => expect(screen.getByText('Failed to load history.')).toBeInTheDocument());
  });
});
