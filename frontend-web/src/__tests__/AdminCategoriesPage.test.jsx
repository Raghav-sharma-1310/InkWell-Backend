/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AdminCategoriesPage } from '../pages/admin/AdminCategoriesPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({})),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

const mockCategory = { categoryId: 'c1', name: 'Technology', postCount: 5 };
const mockTag = { tagId: 't1', name: 'React', postCount: 3 };

// Defines admin categories page so related behavior stays grouped in one place.
describe('AdminCategoriesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.alert = vi.fn();
    
    api.get.mockImplementation((url) => {
      if (url.endsWith('/categories')) return Promise.resolve({ data: { data: [mockCategory] } });
      if (url.endsWith('/tags')) return Promise.resolve({ data: { data: [mockTag] } });
      return Promise.resolve({ data: { data: [] } });
    });
  });

  // Verifies renders categories and tags so regressions are caught during automated tests.
  it('renders categories and tags', async () => {
    render(<MemoryRouter><AdminCategoriesPage /></MemoryRouter>);
    await waitFor(() => {
      expect(screen.getByText('Technology')).toBeInTheDocument();
      expect(screen.getByText('React')).toBeInTheDocument();
    });
  });

  // Defines adds a new category so related behavior stays grouped in one place.
  it('adds a new category', async () => {
    render(<MemoryRouter><AdminCategoriesPage /></MemoryRouter>);
    
    const input = screen.getByPlaceholderText('New category');
    fireEvent.change(input, { target: { value: 'New Cat' } });
    
    // Find the add button for categories (first one)
    const addButtons = screen.getAllByText('Add');
    fireEvent.click(addButtons[0]);
    
    expect(api.post).toHaveBeenCalledWith('/api/categories/admin/categories', { name: 'New Cat' });
  });

  // Defines does not add empty category so related behavior stays grouped in one place.
  it('does not add empty category', async () => {
    render(<MemoryRouter><AdminCategoriesPage /></MemoryRouter>);
    const addButtons = screen.getAllByText('Add');
    fireEvent.click(addButtons[0]);
    expect(api.post).not.toHaveBeenCalled();
  });

  // Defines adds a new tag via enter key so related behavior stays grouped in one place.
  it('adds a new tag via enter key', async () => {
    render(<MemoryRouter><AdminCategoriesPage /></MemoryRouter>);
    
    const input = screen.getByPlaceholderText('New tag');
    fireEvent.change(input, { target: { value: 'New Tag' } });
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });
    
    expect(api.post).toHaveBeenCalledWith('/api/categories/admin/tags', { name: 'New Tag' });
  });

  // Performs the deletes a category after confirmation workflow so callers do not duplicate this logic.
  it('deletes a category after confirmation', async () => {
    render(<MemoryRouter><AdminCategoriesPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByTitle('Delete category')).toBeInTheDocument());
    
    fireEvent.click(screen.getByTitle('Delete category'));
    
    await waitFor(() => expect(screen.getByText('Confirm')).toBeInTheDocument());
    
    fireEvent.click(screen.getByText('Confirm'));
    
    expect(api.delete).toHaveBeenCalledWith('/api/categories/admin/categories/c1');
  });

  // Defines cancels category deletion so related behavior stays grouped in one place.
  it('cancels category deletion', async () => {
    render(<MemoryRouter><AdminCategoriesPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByTitle('Delete category')).toBeInTheDocument());
    fireEvent.click(screen.getByTitle('Delete category'));
    
    await waitFor(() => expect(screen.getByText('Cancel')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Cancel'));
    
    expect(api.delete).not.toHaveBeenCalled();
    expect(screen.queryByText('Confirm')).not.toBeInTheDocument();
  });

  // Performs the deletes a tag after confirmation workflow so callers do not duplicate this logic.
  it('deletes a tag after confirmation', async () => {
    render(<MemoryRouter><AdminCategoriesPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByTitle('Delete tag')).toBeInTheDocument());
    
    fireEvent.click(screen.getByTitle('Delete tag'));
    
    await waitFor(() => expect(screen.getAllByText('Confirm')[0]).toBeInTheDocument());
    
    fireEvent.click(screen.getAllByText('Confirm')[0]);
    
    expect(api.delete).toHaveBeenCalledWith('/api/categories/admin/tags/t1');
  });

  // Verifies handles delete api failure so regressions are caught during automated tests.
  it('handles delete API failure', async () => {
    api.delete.mockRejectedValueOnce(new Error('fail'));
    
    render(<MemoryRouter><AdminCategoriesPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByTitle('Delete category')).toBeInTheDocument());
    fireEvent.click(screen.getByTitle('Delete category'));
    
    await waitFor(() => expect(screen.getByText('Confirm')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Confirm'));
    
    await waitFor(() => expect(window.alert).toHaveBeenCalledWith('Failed to delete category.'));
  });

  // Verifies handles fetch errors gracefully so regressions are caught during automated tests.
  it('handles fetch errors gracefully', async () => {
    api.get.mockRejectedValue(new Error('fail'));
    render(<MemoryRouter><AdminCategoriesPage /></MemoryRouter>);
    // Should render without crashing, just empty lists
    await waitFor(() => {
      expect(screen.getByText('Categories')).toBeInTheDocument();
      expect(screen.getByText('Tags')).toBeInTheDocument();
    });
  });
});
