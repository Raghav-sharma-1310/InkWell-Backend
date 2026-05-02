/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AuthorAnalyticsPage } from '../pages/author/AuthorAnalyticsPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/ui/LoadingSpinner', () => ({
  PageLoader: () => <div data-testid="page-loader">Loading...</div>,
}));

import api from '../api/client';

const mockPost = {
  postId: 'p1',
  title: 'Test Post',
  status: 'PUBLISHED',
  viewCount: 100,
  likesCount: 10,
  readTimeMin: 5,
};

const mockDraft = {
  postId: 'p2',
  title: 'Draft Post',
  status: 'DRAFT',
  viewCount: 50, // drafted post with views just for testing sort
  likesCount: 0,
  readTimeMin: 2,
};

// Provides author analytics page wiring so the framework can apply the expected runtime behavior.
describe('AuthorAnalyticsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Defines shows empty state when no posts so related behavior stays grouped in one place.
  it('shows empty state when no posts', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [] } } });
    render(<MemoryRouter><AuthorAnalyticsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No posts yet')).toBeInTheDocument());
  });

  // Verifies renders analytics data correctly so regressions are caught during automated tests.
  it('renders analytics data correctly', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost, mockDraft] } } });
    render(<MemoryRouter><AuthorAnalyticsPage /></MemoryRouter>);
    
    await waitFor(() => {
      // Total Posts
      expect(screen.getByText('2')).toBeInTheDocument();
      // Published
      expect(screen.getAllByText('1')[0]).toBeInTheDocument();
      // Total Views (150)
      expect(screen.getByText('150')).toBeInTheDocument();
      // Total Likes (10) - use getAllByText since it could be in the table too
      expect(screen.getAllByText('10')[0]).toBeInTheDocument();
      
      // Check table entries
      expect(screen.getByText('Test Post')).toBeInTheDocument();
      expect(screen.getByText('Draft Post')).toBeInTheDocument();
      expect(screen.getByText('5m')).toBeInTheDocument();
      expect(screen.getByText('2m')).toBeInTheDocument();
    });
  });

  // Defines sorts posts by view count in the table so related behavior stays grouped in one place.
  it('sorts posts by view count in the table', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockDraft, mockPost] } } });
    render(<MemoryRouter><AuthorAnalyticsPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    
    const rows = screen.getAllByRole('row');
    // Row 0 is header. Row 1 should be Test Post (100 views). Row 2 should be Draft Post (50 views).
    expect(rows[1]).toHaveTextContent('Test Post');
    expect(rows[2]).toHaveTextContent('Draft Post');
  });

  // Verifies handles fetch error gracefully so regressions are caught during automated tests.
  it('handles fetch error gracefully', async () => {
    api.get.mockRejectedValueOnce(new Error('fail'));
    render(<MemoryRouter><AuthorAnalyticsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No posts yet')).toBeInTheDocument());
  });
});
