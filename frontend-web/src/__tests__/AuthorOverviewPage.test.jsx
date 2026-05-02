/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AuthorOverviewPage } from '../pages/author/AuthorOverviewPage';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { NotificationProvider } from '../context/NotificationContext';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn((url) => {
      if (url.includes('/posts/author')) return Promise.resolve({ data: { data: { content: [] } } });
      if (url.includes('/auth/me')) return Promise.resolve({ data: { data: JSON.parse(localStorage.getItem('inkwell.user') || '{}') } });
      return Promise.resolve({ data: { data: 0 } });
    }),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

const mockPost = {
  postId: 'p1',
  title: 'Test Post',
  status: 'PUBLISHED',
  viewCount: 100,
  likesCount: 10,
  featured: false,
};

const mockDraft = {
  postId: 'p2',
  title: 'Draft Post',
  status: 'DRAFT',
  viewCount: 0,
  likesCount: 0,
  featured: true,
};

// Defines render page so related behavior stays grouped in one place.
function renderPage(user) {
  if (user) {
    localStorage.setItem('inkwell.user', JSON.stringify(user));
    localStorage.setItem('inkwell.accessToken', 'mock-token');
  } else {
    localStorage.clear();
  }
  return render(
    <MemoryRouter>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <AuthorOverviewPage />
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

// Provides author overview page wiring so the framework can apply the expected runtime behavior.
describe('AuthorOverviewPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  // Verifies renders heading with username so regressions are caught during automated tests.
  it('renders heading with username', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/author')) return Promise.resolve({ data: { data: { content: [mockPost, mockDraft] } } });
      if (url.includes('/auth/me')) return Promise.resolve({ data: { data: JSON.parse(localStorage.getItem('inkwell.user') || '{}') } });
      return Promise.resolve({ data: { data: 0 } });
    });
    renderPage({ fullName: 'Author Name', role: 'AUTHOR' });
    
    await waitFor(() => expect(screen.getByText('Welcome, Author Name')).toBeInTheDocument());
  });

  // Defines calculates and displays stats so related behavior stays grouped in one place.
  it('calculates and displays stats', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/author')) return Promise.resolve({ data: { data: { content: [mockPost, mockDraft] } } });
      if (url.includes('/auth/me')) return Promise.resolve({ data: { data: JSON.parse(localStorage.getItem('inkwell.user') || '{}') } });
      return Promise.resolve({ data: { data: 0 } });
    });
    renderPage({ fullName: 'Author Name', role: 'AUTHOR' });
    
    await waitFor(() => {
      // Total Posts (2)
      expect(screen.getByText('2')).toBeInTheDocument();
      // Drafts (1)
      expect(screen.getAllByText('1')[0]).toBeInTheDocument();
      // Total Views (100)
      expect(screen.getAllByText('100')[0]).toBeInTheDocument();
      // Total Likes (10)
      expect(screen.getAllByText('10')[0]).toBeInTheDocument();
    });
  });

  // Defines displays recent posts so related behavior stays grouped in one place.
  it('displays recent posts', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/author')) return Promise.resolve({ data: { data: { content: [mockPost, mockDraft] } } });
      if (url.includes('/auth/me')) return Promise.resolve({ data: { data: JSON.parse(localStorage.getItem('inkwell.user') || '{}') } });
      return Promise.resolve({ data: { data: 0 } });
    });
    renderPage({ fullName: 'Author Name', role: 'AUTHOR' });
    
    await waitFor(() => {
      expect(screen.getByText('Recent Posts')).toBeInTheDocument();
      expect(screen.getByText('Test Post')).toBeInTheDocument();
      expect(screen.getByText('Draft Post')).toBeInTheDocument();
      expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
      expect(screen.getByText('DRAFT')).toBeInTheDocument();
    });
  });

  // Verifies handles api failure gracefully so regressions are caught during automated tests.
  it('handles API failure gracefully', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/author')) return Promise.reject(new Error('fail'));
      if (url.includes('/auth/me')) return Promise.resolve({ data: { data: JSON.parse(localStorage.getItem('inkwell.user') || '{}') } });
      return Promise.resolve({ data: { data: 0 } });
    });
    renderPage({ fullName: 'Author Name', role: 'AUTHOR' });
    
    await waitFor(() => expect(screen.getByText('Welcome, Author Name')).toBeInTheDocument());
    // Should render 0s for all stats since it fell back to empty array
    expect(screen.getAllByText('0').length).toBeGreaterThan(0);
  });
});
