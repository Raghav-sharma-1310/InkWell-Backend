/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { HomePage } from '../pages/public/HomePage';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { NotificationProvider } from '../context/NotificationContext';
import client from '../api/client';

// Mock the API client
vi.mock('../api/client');

describe('HomePage', () => {
  beforeEach(() => {
    // Reset all mocks
    vi.clearAllMocks();
    
    // Default mock implementation
    client.get.mockImplementation((url) => {
      if (url === '/api/posts/public') {
        return Promise.resolve({
          data: {
            data: {
              content: [
                {
                  postId: '1',
                  title: 'Test Post 1',
                  excerpt: 'This is a test post',
                  authorName: 'Test Author',
                  slug: 'test-post-1',
                  publishedAt: new Date().toISOString(),
                  viewCount: 10,
                  likesCount: 5,
                  categorySlug: 'tech',
                  categoryName: 'Technology'
                }
              ],
              totalPages: 1
            }
          }
        });
      }
      if (url === '/api/categories/public/categories/top') {
        return Promise.resolve({ data: { data: [{ categoryId: '1', name: 'Tech', slug: 'tech', postCount: 1 }] } });
      }
      if (url === '/api/categories/public/tags/trending') {
        return Promise.resolve({ data: { data: [{ tagId: '1', name: 'react', slug: 'react' }] } });
      }
      if (url === '/api/posts/public/stats') {
        return Promise.resolve({ data: { data: { totalPublishedPosts: 1, totalViews: 10 } } });
      }
      return Promise.resolve({ data: { data: [] } });
    });
  });

  // Verifies renders loading state initially so regressions are caught during automated tests.
  it('renders loading state initially', () => {
    render(
      <MemoryRouter>
        <ThemeProvider>
          <AuthProvider>
            <NotificationProvider>
              <HomePage />
            </NotificationProvider>
          </AuthProvider>
        </ThemeProvider>
      </MemoryRouter>
    );

    // Look for the Loading text
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  // Verifies renders posts after loading so regressions are caught during automated tests.
  it('renders posts after loading', async () => {
    render(
      <MemoryRouter>
        <ThemeProvider>
          <AuthProvider>
            <NotificationProvider>
              <HomePage />
            </NotificationProvider>
          </AuthProvider>
        </ThemeProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Test Post 1')).toBeInTheDocument();
    });
    expect(screen.getByText('This is a test post')).toBeInTheDocument();
  });
});
