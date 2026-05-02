/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { CategoryPage, TagPage } from '../pages/public/CategoryPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: { content: [] } } })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/ui/LoadingSpinner', () => ({
  PageLoader: () => <div data-testid="page-loader">Loading...</div>,
}));

import api from '../api/client';

const mockPost = { postId: 'p1', slug: 'test-slug', title: 'Category Post', excerpt: 'Excerpt' };

// Defines category page so related behavior stays grouped in one place.
describe('CategoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Defines shows empty state so related behavior stays grouped in one place.
  it('shows empty state', async () => {
    render(
      <MemoryRouter initialEntries={['/categories/technology']}>
        <Routes>
          <Route path="/categories/:slug" element={<CategoryPage />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByText(/No posts in this category yet/)).toBeInTheDocument());
  });

  // Defines displays posts so related behavior stays grouped in one place.
  it('displays posts', async () => {
    api.get.mockResolvedValueOnce({ data: { data: { content: [mockPost] } } });
    render(
      <MemoryRouter initialEntries={['/categories/technology']}>
        <Routes>
          <Route path="/categories/:slug" element={<CategoryPage />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByText('Category Post')).toBeInTheDocument());
  });

  // Defines shows category type heading so related behavior stays grouped in one place.
  it('shows category type heading', async () => {
    render(
      <MemoryRouter initialEntries={['/categories/technology']}>
        <Routes>
          <Route path="/categories/:slug" element={<CategoryPage />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByText('category')).toBeInTheDocument());
  });
});

// Defines tag page so related behavior stays grouped in one place.
describe('TagPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Defines shows tag type heading so related behavior stays grouped in one place.
  it('shows tag type heading', async () => {
    render(
      <MemoryRouter initialEntries={['/tags/react']}>
        <Routes>
          <Route path="/tags/:slug" element={<TagPage />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByText('tag')).toBeInTheDocument());
  });

  // Defines shows empty state for tags so related behavior stays grouped in one place.
  it('shows empty state for tags', async () => {
    render(
      <MemoryRouter initialEntries={['/tags/react']}>
        <Routes>
          <Route path="/tags/:slug" element={<TagPage />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByText(/No posts in this tag yet/)).toBeInTheDocument());
  });
});
