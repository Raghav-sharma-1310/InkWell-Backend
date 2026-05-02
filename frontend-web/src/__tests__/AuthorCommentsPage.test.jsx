/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AuthorCommentsPage } from '../pages/author/AuthorCommentsPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(() => Promise.resolve({})),
    patch: vi.fn(() => Promise.resolve({})),
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
};

const mockComment = {
  commentId: 'c1',
  content: 'This is a test comment',
  authorName: 'User A',
  status: 'PENDING',
  isPostAuthor: false,
};

const mockAuthorReply = {
  commentId: 'c2',
  content: 'Thanks for the comment',
  authorName: 'Author',
  status: 'APPROVED',
  isPostAuthor: true,
  parentCommentId: 'c1',
};

// Provides author comments page wiring so the framework can apply the expected runtime behavior.
describe('AuthorCommentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Defines shows empty state when no posts so related behavior stays grouped in one place.
  it('shows empty state when no posts', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [] } } });
    render(<MemoryRouter><AuthorCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No posts to moderate.')).toBeInTheDocument());
  });

  // Verifies renders post and expands to show comments so regressions are caught during automated tests.
  it('renders post and expands to show comments', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/author')) return Promise.resolve({ data: { data: { content: [mockPost] } } });
      if (url.includes('/comments/public/post/p1')) return Promise.resolve({ data: { data: [mockComment] } });
      return Promise.resolve({ data: { data: [] } });
    });
    
    render(<MemoryRouter><AuthorCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    
    fireEvent.click(screen.getByText('Test Post'));
    
    await waitFor(() => {
      expect(screen.getByText('This is a test comment')).toBeInTheDocument();
      expect(screen.getByText('PENDING')).toBeInTheDocument();
      expect(screen.getByText('User A')).toBeInTheDocument();
    });
  });

  // Defines shows threaded replies so related behavior stays grouped in one place.
  it('shows threaded replies', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/author')) return Promise.resolve({ data: { data: { content: [mockPost] } } });
      if (url.includes('/comments/public/post/p1')) return Promise.resolve({ data: { data: [mockComment, mockAuthorReply] } });
      return Promise.resolve({ data: { data: [] } });
    });
    
    render(<MemoryRouter><AuthorCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    
    fireEvent.click(screen.getByText('Test Post'));
    
    await waitFor(() => {
      expect(screen.getByText('This is a test comment')).toBeInTheDocument();
      expect(screen.getByText('Thanks for the comment')).toBeInTheDocument();
      // "Author" badge for the reply (and the authorName)
      expect(screen.getAllByText('Author')[0]).toBeInTheDocument();
    });
  });

  // Defines approves a comment so related behavior stays grouped in one place.
  it('approves a comment', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/author')) return Promise.resolve({ data: { data: { content: [mockPost] } } });
      if (url.includes('/comments/public/post/p1')) return Promise.resolve({ data: { data: [mockComment] } });
      return Promise.resolve({ data: { data: [] } });
    });
    
    render(<MemoryRouter><AuthorCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Test Post'));
    
    await waitFor(() => expect(screen.getByText('This is a test comment')).toBeInTheDocument());
    
    const approveBtn = screen.getByTitle('Approve');
    fireEvent.click(approveBtn);
    
    expect(api.patch).toHaveBeenCalledWith('/api/comments/author/c1/approve', null, { params: { postId: 'p1' } });
  });

  // Defines rejects a comment so related behavior stays grouped in one place.
  it('rejects a comment', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/author')) return Promise.resolve({ data: { data: { content: [mockPost] } } });
      if (url.includes('/comments/public/post/p1')) return Promise.resolve({ data: { data: [mockComment] } });
      return Promise.resolve({ data: { data: [] } });
    });
    
    render(<MemoryRouter><AuthorCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Test Post'));
    
    await waitFor(() => expect(screen.getByText('This is a test comment')).toBeInTheDocument());
    
    const rejectBtn = screen.getByTitle('Reject');
    fireEvent.click(rejectBtn);
    
    expect(api.patch).toHaveBeenCalledWith('/api/comments/author/c1/reject', null, { params: { postId: 'p1' } });
  });

  // Defines replies to a comment so related behavior stays grouped in one place.
  it('replies to a comment', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/author')) return Promise.resolve({ data: { data: { content: [mockPost] } } });
      if (url.includes('/comments/public/post/p1')) return Promise.resolve({ data: { data: [mockComment] } });
      return Promise.resolve({ data: { data: [] } });
    });
    
    render(<MemoryRouter><AuthorCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Test Post'));
    
    await waitFor(() => expect(screen.getByText('This is a test comment')).toBeInTheDocument());
    
    const replyBtn = screen.getByTitle('Reply as Author');
    fireEvent.click(replyBtn);
    
    const input = screen.getByPlaceholderText('Write your reply...');
    fireEvent.change(input, { target: { value: 'My reply' } });
    
    fireEvent.click(screen.getByText('Reply'));
    
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/comments/c1/reply', { content: 'My reply' });
    });
  });

  // Verifies handles fetch comments error gracefully so regressions are caught during automated tests.
  it('handles fetch comments error gracefully', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/posts/author')) return Promise.resolve({ data: { data: { content: [mockPost] } } });
      if (url.includes('/comments/public/post/p1')) return Promise.reject(new Error('fail'));
      return Promise.resolve({ data: { data: [] } });
    });
    
    render(<MemoryRouter><AuthorCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test Post')).toBeInTheDocument());
    
    fireEvent.click(screen.getByText('Test Post'));
    
    await waitFor(() => expect(screen.getByText('No comments on this post yet.')).toBeInTheDocument());
  });

  // Verifies handles fetch posts error gracefully so regressions are caught during automated tests.
  it('handles fetch posts error gracefully', async () => {
    api.get.mockRejectedValueOnce(new Error('fail'));
    render(<MemoryRouter><AuthorCommentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No posts to moderate.')).toBeInTheDocument());
  });
});
