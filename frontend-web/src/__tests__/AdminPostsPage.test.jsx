/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AdminPostsPage } from '../pages/admin/AdminPostsPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: { content: [] } } })),
    patch: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({})),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/ui/LoadingSpinner', () => ({
  PageLoader: () => <div data-testid="page-loader">Loading...</div>,
}));

import api from '../api/client';

const mockPost = {
  postId: 'p1',
  title: 'Test Admin Post',
  status: 'PUBLISHED',
  visibility: 'PREMIUM',
  featured: false,
  viewCount: 100,
  likesCount: 10,
  readTimeMin: 5,
  categorySlug: 'tech'
};

const featuredPost = { ...mockPost, postId: 'p2', title: 'Featured Post', featured: true };

// Defines admin posts page so related behavior stays grouped in one place.
describe('AdminPostsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Verifies renders heading so regressions are caught during automated tests.
  it('renders heading', async () => {
    render(<MemoryRouter><AdminPostsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Manage Posts')).toBeInTheDocument());
  });

  // Defines displays posts so related behavior stays grouped in one place.
  it('displays posts', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost] } } });
    render(<MemoryRouter><AdminPostsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Admin Post')).toBeInTheDocument());
    expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
    expect(screen.getByText('Premium')).toBeInTheDocument();
    expect(screen.getByText('100')).toBeInTheDocument();
    expect(screen.getByText('10')).toBeInTheDocument();
  });

  // Defines shows no posts message so related behavior stays grouped in one place.
  it('shows no posts message', async () => {
    render(<MemoryRouter><AdminPostsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No posts yet.')).toBeInTheDocument());
  });

  // Defines toggles feature status so related behavior stays grouped in one place.
  it('toggles feature status', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost] } } });
    render(<MemoryRouter><AdminPostsPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('Test Admin Post')).toBeInTheDocument());
    
    // The load function inside toggleFeature needs to be mocked for the second call
    api.get.mockResolvedValueOnce({ data: { data: { content: [featuredPost] } } });
    
    fireEvent.click(screen.getByTitle('Feature'));
    
    expect(api.patch).toHaveBeenCalledWith('/api/posts/admin/p1/feature', null, { params: { featured: true } });
  });

  // Defines unfeatures a featured post so related behavior stays grouped in one place.
  it('unfeatures a featured post', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [featuredPost] } } });
    render(<MemoryRouter><AdminPostsPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('Featured Post')).toBeInTheDocument());
    
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost] } } });
    
    fireEvent.click(screen.getByTitle('Unfeature'));
    
    expect(api.patch).toHaveBeenCalledWith('/api/posts/admin/p2/feature', null, { params: { featured: false } });
  });

  // Performs the opens delete confirmation modal workflow so callers do not duplicate this logic.
  it('opens delete confirmation modal', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost] } } });
    render(<MemoryRouter><AdminPostsPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('Test Admin Post')).toBeInTheDocument());
    
    fireEvent.click(screen.getByTitle('Delete post'));
    
    await waitFor(() => expect(screen.getByText(/Are you sure you want to permanently delete/)).toBeInTheDocument());
    expect(screen.getByText(/"Test Admin Post"/)).toBeInTheDocument();
  });

  // Defines cancels deletion so related behavior stays grouped in one place.
  it('cancels deletion', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost] } } });
    render(<MemoryRouter><AdminPostsPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('Test Admin Post')).toBeInTheDocument());
    
    fireEvent.click(screen.getByTitle('Delete post'));
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Delete Post' })).toBeInTheDocument()); // Inside modal
    
    fireEvent.click(screen.getByText('Cancel'));
    
    expect(screen.queryByText(/Are you sure you want to permanently delete/)).not.toBeInTheDocument();
    expect(api.delete).not.toHaveBeenCalled();
  });

  // Defines confirms deletion so related behavior stays grouped in one place.
  it('confirms deletion', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost] } } });
    render(<MemoryRouter><AdminPostsPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('Test Admin Post')).toBeInTheDocument());
    
    fireEvent.click(screen.getByTitle('Delete post'));
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Delete Post' })).toBeInTheDocument()); // Inside modal
    
    // There are two "Delete Post" texts (heading and button), use getAllByText
    const deleteButtons = screen.getAllByText(/Delete Post/i);
    // The button has a Trash icon and "Delete Post", so it matches /Delete Post/i. The heading is exactly "Delete Post".
    // Let's click the button specifically
    const confirmButton = screen.getAllByRole('button').find(b => b.textContent.includes('Delete Post'));
    fireEvent.click(confirmButton);
    
    expect(api.delete).toHaveBeenCalledWith('/api/posts/admin/p1');
    await waitFor(() => expect(screen.queryByText('Test Admin Post')).not.toBeInTheDocument());
  });

  // Verifies handles delete failure gracefully so regressions are caught during automated tests.
  it('handles delete failure gracefully', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost] } } });
    api.delete.mockRejectedValueOnce(new Error('fail'));
    
    render(<MemoryRouter><AdminPostsPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('Test Admin Post')).toBeInTheDocument());
    
    fireEvent.click(screen.getByTitle('Delete post'));
    await waitFor(() => expect(screen.getByRole('heading', { name: 'Delete Post' })).toBeInTheDocument()); // Inside modal
    
    const confirmButton = screen.getAllByRole('button').find(b => b.textContent.includes('Delete Post'));
    fireEvent.click(confirmButton);
    
    await waitFor(() => expect(consoleSpy).toHaveBeenCalledWith('Failed to delete post:', expect.any(Error)));
    // Post should still be there because delete failed
    expect(screen.getByText('Test Admin Post')).toBeInTheDocument();
    
    consoleSpy.mockRestore();
  });
});
