/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AuthorFollowersPage } from '../pages/author/AuthorFollowersPage';

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

const mockFollower = {
  followerId: 'u1',
  followedAt: '2026-01-01T00:00:00',
};

const mockFollowerDetails = {
  userId: 'u1',
  fullName: 'Follower User',
  username: 'follower1',
  avatarUrl: null,
};

// Provides author followers page wiring so the framework can apply the expected runtime behavior.
describe('AuthorFollowersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Defines shows empty state when no followers so related behavior stays grouped in one place.
  it('shows empty state when no followers', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/followers/count')) return Promise.resolve({ data: { data: { followersCount: 0 } } });
      if (url.includes('/followers')) return Promise.resolve({ data: { data: [] } });
      return Promise.resolve({ data: { data: {} } });
    });
    
    render(<MemoryRouter><AuthorFollowersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No followers yet')).toBeInTheDocument());
  });

  // Verifies renders followers list so regressions are caught during automated tests.
  it('renders followers list', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/followers/count')) return Promise.resolve({ data: { data: { followersCount: 1 } } });
      if (url.includes('/followers')) return Promise.resolve({ data: { data: [mockFollower] } });
      if (url.includes('/auth/public/users/u1')) return Promise.resolve({ data: { data: mockFollowerDetails } });
      return Promise.resolve({ data: { data: {} } });
    });
    
    render(<MemoryRouter><AuthorFollowersPage /></MemoryRouter>);
    
    await waitFor(() => {
      // The badge for count (1)
      expect(screen.getByText('1')).toBeInTheDocument();
      // The follower details
      expect(screen.getByText('Follower User')).toBeInTheDocument();
      expect(screen.getByText('@follower1')).toBeInTheDocument();
    });
  });

  // Verifies handles follower details fetch failure gracefully so regressions are caught during automated tests.
  it('handles follower details fetch failure gracefully', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/followers/count')) return Promise.resolve({ data: { data: { followersCount: 1 } } });
      if (url.includes('/followers')) return Promise.resolve({ data: { data: [mockFollower] } });
      if (url.includes('/auth/public/users/u1')) return Promise.reject(new Error('fail'));
      return Promise.resolve({ data: { data: {} } });
    });
    
    render(<MemoryRouter><AuthorFollowersPage /></MemoryRouter>);
    
    await waitFor(() => {
      expect(screen.getByText('Unknown User')).toBeInTheDocument();
    });
  });

  // Verifies handles base fetch failure gracefully so regressions are caught during automated tests.
  it('handles base fetch failure gracefully', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/followers/count')) return Promise.reject(new Error('fail'));
      if (url.includes('/followers')) return Promise.reject(new Error('fail'));
      return Promise.resolve({ data: { data: {} } });
    });
    
    render(<MemoryRouter><AuthorFollowersPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('No followers yet')).toBeInTheDocument());
    expect(screen.getByText('0')).toBeInTheDocument(); // The badge for count (0)
  });
});
