/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AdminOverviewPage } from '../pages/admin/AdminOverviewPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

// Defines admin overview page so related behavior stays grouped in one place.
describe('AdminOverviewPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.get.mockImplementation((url) => {
      if (url.includes('/search')) return Promise.resolve({ data: { data: [{ id: 1 }, { id: 2 }] } });
      if (url.includes('/admin') && url.includes('posts')) return Promise.resolve({ data: { data: { totalElements: 15 } } });
      if (url.includes('/count')) return Promise.resolve({ data: { data: 50 } });
      if (url.includes('/subscribers')) return Promise.resolve({ data: { data: [{ status: 'ACTIVE' }, { status: 'UNSUBSCRIBED' }] } });
      if (url.includes('/stats')) return Promise.resolve({ data: { data: { totalViews: 1200, totalPublishedPosts: 8 } } });
      return Promise.resolve({ data: { data: [] } });
    });
  });

  // Verifies renders overview heading so regressions are caught during automated tests.
  it('renders overview heading', async () => {
    render(<MemoryRouter><AdminOverviewPage /></MemoryRouter>);
    expect(screen.getByText('Platform Overview')).toBeInTheDocument();
  });

  // Defines shows stat cards with data so related behavior stays grouped in one place.
  it('shows stat cards with data', async () => {
    render(<MemoryRouter><AdminOverviewPage /></MemoryRouter>);
    await waitFor(() => {
      expect(screen.getByText('Users')).toBeInTheDocument();
      expect(screen.getByText('Posts')).toBeInTheDocument();
      expect(screen.getByText('Comments')).toBeInTheDocument();
      expect(screen.getByText('Subscribers')).toBeInTheDocument();
      expect(screen.getByText('Total Views')).toBeInTheDocument();
      expect(screen.getByText('Published Posts')).toBeInTheDocument();
    });
  });

  // Defines shows correct stat values so related behavior stays grouped in one place.
  it('shows correct stat values', async () => {
    render(<MemoryRouter><AdminOverviewPage /></MemoryRouter>);
    await waitFor(() => {
      expect(screen.getByText('2')).toBeInTheDocument(); // users
      expect(screen.getByText('15')).toBeInTheDocument(); // posts
      expect(screen.getByText('50')).toBeInTheDocument(); // comments
    });
  });

  // Verifies handles api errors gracefully so regressions are caught during automated tests.
  it('handles API errors gracefully', async () => {
    api.get.mockRejectedValue(new Error('fail'));
    render(<MemoryRouter><AdminOverviewPage /></MemoryRouter>);
    // Should still render without crashing
    expect(screen.getByText('Platform Overview')).toBeInTheDocument();
  });
});
