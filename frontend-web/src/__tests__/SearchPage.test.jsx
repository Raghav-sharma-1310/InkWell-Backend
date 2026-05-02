/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { SearchPage } from '../pages/public/SearchPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: { content: [] } } })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

const mockPost = {
  postId: 'p1', slug: 'test-slug', title: 'Search Result Post',
  categorySlug: 'tech', readTimeMin: 5, excerpt: 'An excerpt',
};

const mockCategory = { categoryId: 'c1', slug: 'tech', name: 'Technology' };

// Defines search page so related behavior stays grouped in one place.
describe('SearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.get.mockImplementation((url) => {
      if (url.includes('categories')) return Promise.resolve({ data: { data: [mockCategory] } });
      return Promise.resolve({ data: { data: { content: [] } } });
    });
  });

  // Verifies renders search heading so regressions are caught during automated tests.
  it('renders search heading', async () => {
    render(<MemoryRouter><SearchPage /></MemoryRouter>);
    expect(screen.getByText('Explore Posts')).toBeInTheDocument();
  });

  // Defines shows no posts message when empty so related behavior stays grouped in one place.
  it('shows no posts message when empty', async () => {
    render(<MemoryRouter><SearchPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No posts found matching criteria.')).toBeInTheDocument());
  });

  // Defines displays search results so related behavior stays grouped in one place.
  it('displays search results', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('categories')) return Promise.resolve({ data: { data: [mockCategory] } });
      return Promise.resolve({ data: { data: { content: [mockPost] } } });
    });
    render(<MemoryRouter><SearchPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Search Result Post')).toBeInTheDocument());
  });

  // Verifies renders search input so regressions are caught during automated tests.
  it('renders search input', () => {
    render(<MemoryRouter><SearchPage /></MemoryRouter>);
    expect(screen.getByPlaceholderText('Search articles...')).toBeInTheDocument();
  });

  // Verifies renders category dropdown with options so regressions are caught during automated tests.
  it('renders category dropdown with options', async () => {
    render(<MemoryRouter><SearchPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Technology')).toBeInTheDocument());
  });

  // Performs the loads posts using existing search and category params workflow so callers do not duplicate this logic.
  it('loads posts using existing search and category params', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('categories')) return Promise.resolve({ data: { data: [mockCategory] } });
      return Promise.resolve({ data: { data: { content: [mockPost] } } });
    });

    render(
      <MemoryRouter initialEntries={['/search?search=ink&category=tech']}>
        <Routes>
          <Route path="/search" element={<SearchPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/api/posts/explore', {
        params: { search: 'ink', category: 'tech' },
      });
      expect(screen.getByDisplayValue('ink')).toBeInTheDocument();
      expect(screen.getByRole('combobox').value).toBe('tech');
      expect(screen.getByText('Clear Filters')).toBeInTheDocument();
    });
  });

  // Performs the updates search params and clears filters workflow so callers do not duplicate this logic.
  it('updates search params and clears filters', async () => {
    render(
      <MemoryRouter initialEntries={['/search?search=ink&category=tech']}>
        <Routes>
          <Route path="/search" element={<SearchPage />} />
        </Routes>
      </MemoryRouter>
    );

    fireEvent.change(screen.getByPlaceholderText('Search articles...'), { target: { value: '' } });
    await waitFor(() => expect(screen.getByPlaceholderText('Search articles...').value).toBe(''));

    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'tech' } });
    await waitFor(() => expect(screen.getByRole('combobox').value).toBe('tech'));

    fireEvent.click(screen.getByText('Clear Filters'));
    await waitFor(() => {
      expect(screen.getByPlaceholderText('Search articles...').value).toBe('');
      expect(screen.getByRole('combobox').value).toBe('');
    });
  });

  // Defines falls back gracefully when categories and posts fail so related behavior stays grouped in one place.
  it('falls back gracefully when categories and posts fail', async () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
    api.get.mockRejectedValue(new Error('network'));

    render(<MemoryRouter><SearchPage /></MemoryRouter>);

    await waitFor(() => expect(screen.getByText('No posts found matching criteria.')).toBeInTheDocument());
    expect(spy).toHaveBeenCalled();
    spy.mockRestore();
  });
});
