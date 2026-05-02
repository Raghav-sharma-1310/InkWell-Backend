/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AuthorProfilePage } from '../pages/public/AuthorProfilePage';
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

const mockAuthor = { fullName: 'John Doe', username: 'johndoe', bio: 'Writer & dev', avatarUrl: null };
const mockPosts = [
  { postId: 'p1', authorId: 'author-1', title: 'First Post', slug: 'first-post', excerpt: 'Excerpt 1', categorySlug: 'tech', readTimeMin: 3, viewCount: 50, likesCount: 5, featuredImageUrl: null },
  { postId: 'p2', authorId: 'author-1', title: 'Second Post', slug: 'second-post', excerpt: 'Excerpt 2', categorySlug: 'design', readTimeMin: 7, viewCount: 120, likesCount: 15, featuredImageUrl: null },
];

const readerUser = {
  userId: 'u1', username: 'reader', email: 'r@test.com', role: 'READER', fullName: 'Reader',
  subscriptionTier: 'FREE', subscriptionStatus: 'INACTIVE',
};
const proUser = { ...readerUser, subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE' };
const authorUser = { ...readerUser, role: 'AUTHOR', userId: 'author-1' };

// Defines render page so related behavior stays grouped in one place.
function renderPage(user = null) {
  if (user) {
    localStorage.setItem('inkwell.user', JSON.stringify(user));
    localStorage.setItem('inkwell.accessToken', 'mock-token');
  } else {
    localStorage.removeItem('inkwell.user');
    localStorage.removeItem('inkwell.accessToken');
  }

  return render(
    <MemoryRouter initialEntries={['/authors/author-1']}>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <Routes>
              <Route path="authors/:authorId" element={<AuthorProfilePage />} />
            </Routes>
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

// Provides author profile page wiring so the framework can apply the expected runtime behavior.
describe('AuthorProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();

    api.get.mockImplementation((url) => {
      if (url.includes('/api/auth/public/users/')) return Promise.resolve({ data: { data: mockAuthor } });
      if (url.includes('/api/posts/public')) return Promise.resolve({ data: { data: { content: mockPosts } } });
      if (url.includes('/followers/count')) return Promise.resolve({ data: { data: { followersCount: 42 } } });
      if (url.includes('/follow/status')) return Promise.resolve({ data: { data: { following: false, followersCount: 42 } } });
      return Promise.resolve({ data: { data: 0 } });
    });
    api.post.mockResolvedValue({ data: { data: { following: true, followersCount: 43 } } });
  });

  // Performs the shows loading state workflow so callers do not duplicate this logic.
  it('shows loading state', () => {
    api.get.mockImplementation(() => new Promise(() => {}));
    renderPage();
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  // Verifies renders author info so regressions are caught during automated tests.
  it('renders author info', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('@johndoe')).toBeInTheDocument();
      expect(screen.getByText('Writer & dev')).toBeInTheDocument();
    });
  });

  // Verifies renders posts by author so regressions are caught during automated tests.
  it('renders posts by author', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('First Post')).toBeInTheDocument();
      expect(screen.getByText('Second Post')).toBeInTheDocument();
    });
  });

  // Verifies renders stats (posts and followers) so regressions are caught during automated tests.
  it('renders stats (posts and followers)', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument(); // 2 posts
      expect(screen.getByText('Posts')).toBeInTheDocument();
      expect(screen.getByText('Followers')).toBeInTheDocument();
    });
  });

  // Defines shows follow button for non own profile with pro user so related behavior stays grouped in one place.
  it('shows follow button for non-own profile with PRO user', async () => {
    renderPage(proUser);
    await waitFor(() => {
      expect(screen.getByText('Follow')).toBeInTheDocument();
    });
  });

  // Verifies handles follow click so regressions are caught during automated tests.
  it('handles follow click', async () => {
    renderPage(proUser);
    await waitFor(() => expect(screen.getByText('Follow')).toBeInTheDocument());
    
    fireEvent.click(screen.getByText('Follow'));
    
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/posts/authors/author-1/follow');
    });
  });

  // Defines does not show follow button for own profile so related behavior stays grouped in one place.
  it('does not show follow button for own profile', async () => {
    renderPage(authorUser);
    await waitFor(() => expect(screen.getByText('John Doe')).toBeInTheDocument());
    expect(screen.queryByText('Follow')).not.toBeInTheDocument();
  });

  // Defines shows pro gate for non pro reader so related behavior stays grouped in one place.
  it('shows pro gate for non-pro reader', async () => {
    renderPage(readerUser);
    await waitFor(() => {
      expect(screen.getByText('Upgrade to Pro')).toBeInTheDocument();
    });
  });

  // Defines shows no posts message when empty so related behavior stays grouped in one place.
  it('shows no posts message when empty', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/auth/public/users/')) return Promise.resolve({ data: { data: mockAuthor } });
      if (url.includes('/api/posts/public')) return Promise.resolve({ data: { data: { content: [] } } });
      if (url.includes('/followers/count')) return Promise.resolve({ data: { data: { followersCount: 0 } } });
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage();
    await waitFor(() => {
      expect(screen.getByText('No published posts yet')).toBeInTheDocument();
    });
  });

  // Verifies allows unauthenticated access so regressions are caught during automated tests.
  it('allows unauthenticated access', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
    });
  });
});
