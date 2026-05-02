/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { NotificationProvider } from '../context/NotificationContext';
import { vi } from 'vitest';

// Default mock user
export const mockUser = {
  userId: '123e4567-e89b-12d3-a456-426614174000',
  username: 'testuser',
  email: 'test@inkwell.com',
  role: 'READER',
  fullName: 'Test User',
  subscriptionTier: 'FREE',
  subscriptionStatus: 'INACTIVE',
};

export const mockProUser = {
  ...mockUser,
  subscriptionTier: 'PRO',
  subscriptionStatus: 'ACTIVE',
};

export const mockAdminUser = {
  ...mockUser,
  role: 'ADMIN',
  username: 'admin',
  email: 'admin@inkwell.com',
};

export const mockAuthorUser = {
  ...mockUser,
  role: 'AUTHOR',
  username: 'author',
  email: 'author@inkwell.com',
};

/**
 * Render with all providers
 */
export function renderWithProviders(ui, { route = '/', user = null } = {}) {
  // Set up localStorage for user
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
            {ui}
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

/**
 * Create a mock API response
 */
export function mockApiResponse(data, message = 'Success') {
  return { data: { data, message, status: 200 } };
}

/**
 * Create a mock paginated API response
 */
export function mockPageResponse(content, totalElements = content.length) {
  return mockApiResponse({
    content,
    page: 0,
    size: 10,
    totalElements,
    totalPages: Math.ceil(totalElements / 10),
    first: true,
    last: true,
  });
}

export const mockPost = {
  postId: '550e8400-e29b-41d4-a716-446655440000',
  authorId: '123e4567-e89b-12d3-a456-426614174001',
  title: 'Test Post Title',
  slug: 'test-post-title',
  content: '<p>Test content for the post</p>',
  excerpt: 'Test excerpt',
  featuredImageUrl: null,
  status: 'PUBLISHED',
  readTimeMin: 5,
  viewCount: 100,
  likesCount: 10,
  categorySlug: 'technology',
  tagSlugs: ['javascript', 'react'],
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00',
  publishedAt: '2026-01-01T00:00:00',
  featured: false,
  pinned: false,
  visibility: 'PUBLIC',
  scheduledAt: null,
};
