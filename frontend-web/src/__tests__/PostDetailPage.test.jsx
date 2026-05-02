/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { PostDetailPage } from '../pages/public/PostDetailPage';
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

vi.mock('dompurify', () => ({
  default: { sanitize: (html) => html },
}));

import api from '../api/client';

const mockPost = {
  postId: 'p1',
  authorId: 'a1',
  title: 'Test Post Title',
  slug: 'test-post',
  content: '<p>Test content</p>',
  excerpt: 'Test excerpt',
  featuredImageUrl: null,
  status: 'PUBLISHED',
  readTimeMin: 5,
  viewCount: 100,
  likesCount: 10,
  categorySlug: 'technology',
  tagSlugs: ['react', 'javascript'],
  createdAt: '2026-01-01T00:00:00',
  publishedAt: '2026-01-01T00:00:00',
};

const mockComment = {
  commentId: 'c1',
  authorName: 'Commenter',
  content: 'Great post!',
  status: 'APPROVED',
  isPostAuthor: false,
  parentCommentId: null,
  createdAt: '2026-01-02T00:00:00',
};

const readerUser = {
  userId: 'u1', username: 'reader', email: 'reader@test.com',
  role: 'READER', fullName: 'Test Reader', subscriptionTier: 'FREE', subscriptionStatus: 'INACTIVE',
};

const proUser = {
  ...readerUser, subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE',
};

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
    <MemoryRouter initialEntries={['/posts/test-post']}>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <Routes>
              <Route path="posts/:slug" element={<PostDetailPage />} />
            </Routes>
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

// Defines post detail page so related behavior stays grouped in one place.
describe('PostDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    window.alert = vi.fn();

    api.get.mockImplementation((url) => {
      if (url.includes('/api/posts/public/')) return Promise.resolve({ data: { data: mockPost } });
      if (url.includes('/api/comments/public/post/')) return Promise.resolve({ data: { data: [mockComment] } });
      if (url.includes('/api/auth/public/users/')) return Promise.resolve({ data: { data: { fullName: 'Author Name', bio: 'Test bio', avatarUrl: null } } });
      if (url.includes('/followers/count')) return Promise.resolve({ data: { data: { followersCount: 42 } } });
      if (url.includes('/follow/status')) return Promise.resolve({ data: { data: { following: false, followersCount: 42 } } });
      if (url.includes('/reader/bookmarks')) return Promise.resolve({ data: { data: [] } });
      return Promise.resolve({ data: { data: 0 } });
    });
    api.post.mockResolvedValue({ data: { data: {} } });
  });

  // Performs the shows loading state workflow so callers do not duplicate this logic.
  it('shows loading state', () => {
    // Make the API hang
    api.get.mockImplementation(() => new Promise(() => {}));
    renderPage();
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  // Verifies renders post title and content so regressions are caught during automated tests.
  it('renders post title and content', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Test Post Title')).toBeInTheDocument();
    });
    expect(screen.getByText('Test content')).toBeInTheDocument();
  });

  // Verifies renders post metadata so regressions are caught during automated tests.
  it('renders post metadata', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('5 min read')).toBeInTheDocument();
      expect(screen.getByText('100 views')).toBeInTheDocument();
    });
  });

  // Verifies renders category and tags so regressions are caught during automated tests.
  it('renders category and tags', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('technology')).toBeInTheDocument();
      expect(screen.getByText('#react')).toBeInTheDocument();
      expect(screen.getByText('#javascript')).toBeInTheDocument();
    });
  });

  // Verifies renders author card so regressions are caught during automated tests.
  it('renders author card', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Author Name')).toBeInTheDocument();
      expect(screen.getByText('Test bio')).toBeInTheDocument();
    });
  });

  // Verifies renders followers count so regressions are caught during automated tests.
  it('renders followers count', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/42 followers/)).toBeInTheDocument();
    });
  });

  // Verifies renders comments section so regressions are caught during automated tests.
  it('renders comments section', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Great post!')).toBeInTheDocument();
      expect(screen.getByText('Commenter')).toBeInTheDocument();
      expect(screen.getByText('APPROVED')).toBeInTheDocument();
    });
  });

  // Defines shows sign in prompt when not logged in so related behavior stays grouped in one place.
  it('shows sign in prompt when not logged in', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getAllByText(/Sign in/i).length).toBeGreaterThan(0);
    });
  });

  // Defines shows comment form when logged in so related behavior stays grouped in one place.
  it('shows comment form when logged in', async () => {
    renderPage(readerUser);
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Share your thoughts...')).toBeInTheDocument();
    });
  });

  // Performs the shows like and bookmark buttons when logged in workflow so callers do not duplicate this logic.
  it('shows like and bookmark buttons when logged in', async () => {
    renderPage(readerUser);
    await waitFor(() => {
      expect(screen.getByText(/Like/)).toBeInTheDocument();
      expect(screen.getByText(/Save/)).toBeInTheDocument();
    });
  });

  // Verifies handles like toggle so regressions are caught during automated tests.
  it('handles like toggle', async () => {
    api.post.mockResolvedValue({ data: { data: { liked: true, likesCount: 11 } } });
    renderPage(readerUser);
    
    await waitFor(() => expect(screen.getByText(/Like/)).toBeInTheDocument());
    
    fireEvent.click(screen.getByText(/Like/));
    
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/posts/reader/p1/like');
    });
  });

  // Performs the shows premium gate for bookmark (free user) workflow so callers do not duplicate this logic.
  it('shows premium gate for bookmark (free user)', async () => {
    renderPage(readerUser);
    
    await waitFor(() => expect(screen.getByText(/Save/)).toBeInTheDocument());
    
    fireEvent.click(screen.getByText(/Save/));
    
    expect(window.alert).toHaveBeenCalledWith(expect.stringContaining('premium'));
  });

  // Verifies handles bookmark for pro user so regressions are caught during automated tests.
  it('handles bookmark for pro user', async () => {
    api.post.mockResolvedValue({ data: { data: { bookmarked: true } } });
    renderPage(proUser);
    
    await waitFor(() => expect(screen.getByText(/Save/)).toBeInTheDocument());
    
    fireEvent.click(screen.getByText(/Save/));
    
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/posts/reader/p1/bookmark');
    });
  });

  // Verifies handles follow toggle so regressions are caught during automated tests.
  it('handles follow toggle', async () => {
    api.post.mockResolvedValue({ data: { data: { following: true, followersCount: 43 } } });
    renderPage(readerUser);
    
    await waitFor(() => expect(screen.getByText('Follow')).toBeInTheDocument());
    
    fireEvent.click(screen.getByText('Follow'));
    
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/posts/authors/a1/follow');
    });
  });

  // Defines submits a comment so related behavior stays grouped in one place.
  it('submits a comment', async () => {
    api.post.mockResolvedValue({ data: {} });
    // After comment submit, refresh comments
    api.get.mockImplementation((url) => {
      if (url.includes('/api/posts/public/')) return Promise.resolve({ data: { data: mockPost } });
      if (url.includes('/api/comments/public/post/')) return Promise.resolve({ data: { data: [mockComment] } });
      if (url.includes('/api/auth/public/users/')) return Promise.resolve({ data: { data: { fullName: 'Author Name' } } });
      if (url.includes('/followers/count')) return Promise.resolve({ data: { data: { followersCount: 0 } } });
      if (url.includes('/follow/status')) return Promise.resolve({ data: { data: { following: false, followersCount: 0 } } });
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage(readerUser);
    
    await waitFor(() => expect(screen.getByPlaceholderText('Share your thoughts...')).toBeInTheDocument());
    
    fireEvent.change(screen.getByPlaceholderText('Share your thoughts...'), {
      target: { value: 'My comment' },
    });
    fireEvent.click(screen.getByText('Post Comment'));
    
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/comments', { postId: 'p1', content: 'My comment' });
    });
  });

  // Defines shows post not found for missing post so related behavior stays grouped in one place.
  it('shows post not found for missing post', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/posts/public/')) return Promise.resolve({ data: { data: null } });
      return Promise.resolve({ data: { data: [] } });
    });

    renderPage();
    
    await waitFor(() => {
      expect(screen.getByText('Post not found')).toBeInTheDocument();
    });
  });

  // Verifies renders comment with replies so regressions are caught during automated tests.
  it('renders comment with replies', async () => {
    const replyComment = {
      commentId: 'c2',
      authorName: 'Author Reply',
      content: 'Thanks!',
      status: 'APPROVED',
      isPostAuthor: true,
      parentCommentId: 'c1',
      createdAt: '2026-01-03T00:00:00',
    };

    api.get.mockImplementation((url) => {
      if (url.includes('/api/posts/public/')) return Promise.resolve({ data: { data: mockPost } });
      if (url.includes('/api/comments/public/post/')) return Promise.resolve({ data: { data: [mockComment, replyComment] } });
      if (url.includes('/api/auth/public/users/')) return Promise.resolve({ data: { data: { fullName: 'Author Name' } } });
      if (url.includes('/followers/count')) return Promise.resolve({ data: { data: { followersCount: 0 } } });
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage();
    
    await waitFor(() => {
      expect(screen.getByText('Great post!')).toBeInTheDocument();
      expect(screen.getByText('Thanks!')).toBeInTheDocument();
      expect(screen.getByText('Author Reply')).toBeInTheDocument();
    });
  });

  // Defines shows empty comments state so related behavior stays grouped in one place.
  it('shows empty comments state', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/posts/public/')) return Promise.resolve({ data: { data: mockPost } });
      if (url.includes('/api/comments/public/post/')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/auth/public/users/')) return Promise.resolve({ data: { data: { fullName: 'Author' } } });
      if (url.includes('/followers/count')) return Promise.resolve({ data: { data: { followersCount: 0 } } });
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage();
    
    await waitFor(() => {
      expect(screen.getByText('No comments yet. Be the first!')).toBeInTheDocument();
    });
  });

  // Defines records reading history for pro users so related behavior stays grouped in one place.
  it('records reading history for PRO users', async () => {
    renderPage(proUser);
    
    await waitFor(() => expect(screen.getByText('Test Post Title')).toBeInTheDocument());
    
    expect(api.post).toHaveBeenCalledWith('/api/reading-history', { postSlug: 'test-post' });
  });
});
