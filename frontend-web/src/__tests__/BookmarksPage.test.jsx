/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { BookmarksPage } from '../pages/reader/BookmarksPage';
import { ThemeProvider } from '../context/ThemeContext';
import { AuthProvider } from '../context/AuthContext';
import { NotificationProvider } from '../context/NotificationContext';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: 0 } })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve()),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/ui/PostImage', () => ({
  PostImage: ({ alt }) => <img alt={alt} data-testid="post-image" />,
}));

import api from '../api/client';

const mockPost = {
  postId: 'p1', slug: 'test-slug', title: 'Test Bookmark', categorySlug: 'tech',
  readTimeMin: 5, featuredImageUrl: null, excerpt: 'An excerpt',
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
            <BookmarksPage />
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

// Performs the bookmarks page workflow so callers do not duplicate this logic.
describe('BookmarksPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    api.get.mockImplementation(() => Promise.resolve({ data: { data: 0 } }));
  });

  // Defines shows premium prompt for free users so related behavior stays grouped in one place.
  it('shows premium prompt for free users', () => {
    renderPage({ subscriptionTier: 'FREE', subscriptionStatus: 'INACTIVE' });
    expect(screen.getByText('Premium Feature')).toBeInTheDocument();
  });

  // Performs the shows empty state when no bookmarks for pro user workflow so callers do not duplicate this logic.
  it('shows empty state when no bookmarks for pro user', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/posts/reader/bookmarks') return Promise.resolve({ data: { data: [] } });
      return Promise.resolve({ data: { data: 0 } });
    });
    renderPage({ subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE' });
    await waitFor(() => expect(screen.getByText('No bookmarks yet')).toBeInTheDocument());
  });

  // Performs the displays bookmarks for pro user workflow so callers do not duplicate this logic.
  it('displays bookmarks for pro user', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/posts/reader/bookmarks') return Promise.resolve({ data: { data: [mockPost] } });
      return Promise.resolve({ data: { data: 0 } });
    });
    renderPage({ subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE' });
    await waitFor(() => expect(screen.getByText('Test Bookmark')).toBeInTheDocument());
  });

  // Defines shows error on 403 so related behavior stays grouped in one place.
  it('shows error on 403', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/posts/reader/bookmarks') return Promise.reject({ response: { status: 403 } });
      return Promise.resolve({ data: { data: 0 } });
    });
    renderPage({ subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE' });
    await waitFor(() => expect(screen.getByText('Premium subscription required.')).toBeInTheDocument());
  });

  // Defines shows generic error on failure so related behavior stays grouped in one place.
  it('shows generic error on failure', async () => {
    api.get.mockImplementation((url) => {
      if (url === '/api/posts/reader/bookmarks') return Promise.reject({ response: { status: 500 } });
      return Promise.resolve({ data: { data: 0 } });
    });
    renderPage({ subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE' });
    await waitFor(() => expect(screen.getByText('Failed to load bookmarks.')).toBeInTheDocument());
  });
});
