/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AuthorPostsPage } from '../pages/author/AuthorPostsPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    delete: vi.fn(() => Promise.resolve({})),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/ui/LoadingSpinner', () => ({
  PageLoader: () => <div data-testid="page-loader">Loading...</div>,
  EmptyState: ({ title }) => <div data-testid="empty-state">{title}</div>,
}));

import api from '../api/client';

const mockPost = {
  postId: 'p1',
  title: 'Test Post',
  status: 'PUBLISHED',
  viewCount: 100,
  likesCount: 10,
  featured: false,
  excerpt: 'A short excerpt',
  readTimeMin: 5,
};

const mockDraft = {
  postId: 'p2',
  title: 'Draft Post',
  status: 'DRAFT',
  featured: true,
  categorySlug: 'tech',
};

// Provides author posts page wiring so the framework can apply the expected runtime behavior.
describe('AuthorPostsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.confirm = vi.fn(() => true);
  });

  // Defines shows empty state when no posts so related behavior stays grouped in one place.
  it('shows empty state when no posts', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [] } } });
    render(<MemoryRouter><AuthorPostsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByTestId('empty-state')).toBeInTheDocument());
  });

  // Verifies renders posts correctly so regressions are caught during automated tests.
  it('renders posts correctly', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost, mockDraft] } } });
    render(<MemoryRouter><AuthorPostsPage /></MemoryRouter>);
    
    await waitFor(() => {
      expect(screen.getByText('Test Post')).toBeInTheDocument();
      expect(screen.getByText('Draft Post')).toBeInTheDocument();
      expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
      expect(screen.getByText('DRAFT')).toBeInTheDocument();
      expect(screen.getByText('Featured')).toBeInTheDocument();
      expect(screen.getByText('tech')).toBeInTheDocument();
      expect(screen.getByText('A short excerpt')).toBeInTheDocument();
    });
  });

  // Performs the deletes post successfully workflow so callers do not duplicate this logic.
  it('deletes post successfully', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost] } } });
    render(<MemoryRouter><AuthorPostsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    
    const deleteButton = screen.getByRole('button');
    fireEvent.click(deleteButton);
    
    expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to delete this post?');
    expect(api.delete).toHaveBeenCalledWith('/api/posts/author/p1');
    
    // API gets called again to reload posts
    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(2));
  });

  // Performs the does not delete post if cancelled workflow so callers do not duplicate this logic.
  it('does not delete post if cancelled', async () => {
    window.confirm.mockReturnValueOnce(false);
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost] } } });
    render(<MemoryRouter><AuthorPostsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    
    const deleteButton = screen.getByRole('button');
    fireEvent.click(deleteButton);
    
    expect(api.delete).not.toHaveBeenCalled();
  });

  // Verifies handles fetch error gracefully so regressions are caught during automated tests.
  it('handles fetch error gracefully', async () => {
    api.get.mockRejectedValueOnce(new Error('fail'));
    render(<MemoryRouter><AuthorPostsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByTestId('empty-state')).toBeInTheDocument());
  });
});
