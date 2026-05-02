/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AdminCommentsPage } from '../pages/admin/AdminCommentsPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(() => Promise.resolve({ data: {} })),
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
  authorName: 'Author Name',
};

const mockComment = {
  commentId: 'c1',
  content: 'This is a test comment',
  authorName: 'Commenter',
  createdAt: '2026-01-01T00:00:00',
  status: 'APPROVED',
};

// Defines admin comments page so related behavior stays grouped in one place.
describe('AdminCommentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.confirm = vi.fn(() => true);
    window.alert = vi.fn();
    
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/public')) return Promise.resolve({ data: { data: { content: [mockPost] } } });
      if (url.includes('/comments/public')) return Promise.resolve({ data: { data: [mockComment] } });
      return Promise.resolve({ data: { data: [] } });
    });
  });

  // Verifies renders heading and posts sidebar so regressions are caught during automated tests.
  it('renders heading and posts sidebar', async () => {
    render(<MemoryRouter><AdminCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Moderate Comments')).toBeInTheDocument());
    expect(screen.getByText('Test Post')).toBeInTheDocument();
  });

  // Defines shows no posts message so related behavior stays grouped in one place.
  it('shows no posts message', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/public')) return Promise.resolve({ data: { data: { content: [] } } });
      return Promise.resolve({ data: { data: [] } });
    });
    render(<MemoryRouter><AdminCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No posts found.')).toBeInTheDocument());
  });

  // Defines shows placeholder before post is selected so related behavior stays grouped in one place.
  it('shows placeholder before post is selected', async () => {
    render(<MemoryRouter><AdminCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Select a post from the left to view comments')).toBeInTheDocument());
  });

  // Performs the loads and displays comments when post is selected workflow so callers do not duplicate this logic.
  it('loads and displays comments when post is selected', async () => {
    render(<MemoryRouter><AdminCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    
    fireEvent.click(screen.getByText('Test Post'));
    
    await waitFor(() => expect(screen.getByText('This is a test comment')).toBeInTheDocument());
    expect(screen.getByText('Commenter')).toBeInTheDocument();
  });

  // Defines shows no comments message when empty so related behavior stays grouped in one place.
  it('shows no comments message when empty', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/public')) return Promise.resolve({ data: { data: { content: [mockPost] } } });
      if (url.includes('/comments/public')) return Promise.resolve({ data: { data: [] } });
      return Promise.resolve({ data: { data: [] } });
    });
    
    render(<MemoryRouter><AdminCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    
    fireEvent.click(screen.getByText('Test Post'));
    
    await waitFor(() => expect(screen.getByText('No active comments on this post.')).toBeInTheDocument());
  });

  // Performs the deletes comment successfully workflow so callers do not duplicate this logic.
  it('deletes comment successfully', async () => {
    render(<MemoryRouter><AdminCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Test Post'));
    await waitFor(() => expect(screen.getByText('This is a test comment')).toBeInTheDocument());
    
    fireEvent.click(screen.getByTitle('Delete Comment'));
    
    expect(window.confirm).toHaveBeenCalled();
    expect(api.patch).toHaveBeenCalledWith('/api/comments/admin/c1/delete?postId=p1');
    await waitFor(() => expect(screen.queryByText('This is a test comment')).not.toBeInTheDocument());
  });

  // Performs the does not delete comment if confirmation cancelled workflow so callers do not duplicate this logic.
  it('does not delete comment if confirmation cancelled', async () => {
    window.confirm.mockReturnValueOnce(false);
    render(<MemoryRouter><AdminCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Test Post'));
    await waitFor(() => expect(screen.getByText('This is a test comment')).toBeInTheDocument());
    
    fireEvent.click(screen.getByTitle('Delete Comment'));
    
    expect(api.patch).not.toHaveBeenCalled();
    expect(screen.getByText('This is a test comment')).toBeInTheDocument();
  });

  // Verifies handles delete failure gracefully so regressions are caught during automated tests.
  it('handles delete failure gracefully', async () => {
    api.patch.mockRejectedValueOnce(new Error('fail'));
    render(<MemoryRouter><AdminCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Test Post'));
    await waitFor(() => expect(screen.getByText('This is a test comment')).toBeInTheDocument());
    
    fireEvent.click(screen.getByTitle('Delete Comment'));
    
    await waitFor(() => expect(window.alert).toHaveBeenCalledWith('Failed to delete comment.'));
    // Comment should still be there
    expect(screen.getByText('This is a test comment')).toBeInTheDocument();
  });
});
