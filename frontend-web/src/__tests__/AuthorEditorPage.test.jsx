/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AuthorEditorPage } from '../pages/author/AuthorEditorPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/forms/RichTextEditor', () => ({
  RichTextEditor: ({ value, onChange }) => (
    <textarea 
      data-testid="rich-text" 
      value={value} 
      onChange={(e) => onChange(e.target.value)} 
    />
  ),
}));

import api from '../api/client';

const mockPost = {
  title: 'Test Post',
  content: 'Content here',
  excerpt: 'Short excerpt',
  categorySlug: 'tech',
  tagSlugs: ['react', 'testing'],
  status: 'PUBLISHED',
  visibility: 'PREMIUM',
  featured: true,
  pinned: false,
};

// Defines render page so related behavior stays grouped in one place.
function renderPage(postId = null) {
  return render(
    <MemoryRouter initialEntries={[postId ? `/author/posts/${postId}/edit` : '/author/posts/new']}>
      <Routes>
        <Route path="/author/posts/new" element={<AuthorEditorPage />} />
        <Route path="/author/posts/:postId/edit" element={<AuthorEditorPage />} />
      </Routes>
    </MemoryRouter>
  );
}

// Provides author editor page wiring so the framework can apply the expected runtime behavior.
describe('AuthorEditorPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Verifies renders create form so regressions are caught during automated tests.
  it('renders create form', () => {
    renderPage();
    expect(screen.getByText('Create Post')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Post title').value).toBe('');
    expect(screen.getByPlaceholderText('Category slug').value).toBe('');
    expect(screen.getByDisplayValue('Draft')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Public')).toBeInTheDocument();
  });

  // Defines submits new post so related behavior stays grouped in one place.
  it('submits new post', async () => {
    renderPage();
    
    fireEvent.change(screen.getByPlaceholderText('Post title'), { target: { value: 'New Title' } });
    fireEvent.change(screen.getByPlaceholderText('Category slug'), { target: { value: 'news' } });
    fireEvent.change(screen.getByPlaceholderText('Tags (comma separated)'), { target: { value: 'tag1, tag2' } });
    
    fireEvent.click(screen.getByText('Save Post'));
    
    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/posts/author', expect.objectContaining({
        title: 'New Title',
        categorySlug: 'news',
        tagSlugs: ['tag1', 'tag2'],
        status: 'DRAFT',
        visibility: 'PUBLIC',
      }));
    });
  });

  // Defines submits all editable fields for a new post so related behavior stays grouped in one place.
  it('submits all editable fields for a new post', async () => {
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('Post title'), { target: { value: 'Complete Post' } });
    fireEvent.change(screen.getByPlaceholderText('Short excerpt or summary'), { target: { value: 'Summary' } });
    fireEvent.change(screen.getByPlaceholderText('Featured image URL (optional)'), { target: { value: 'https://img.test/post.png' } });
    fireEvent.change(screen.getByPlaceholderText('Category slug'), { target: { value: 'craft' } });
    fireEvent.change(screen.getByPlaceholderText('Tags (comma separated)'), { target: { value: 'alpha, , beta' } });
    fireEvent.change(screen.getByDisplayValue('Draft'), { target: { value: 'PUBLISHED' } });
    fireEvent.change(screen.getByDisplayValue('Public'), { target: { value: 'PREMIUM' } });
    fireEvent.click(screen.getByLabelText('Featured'));
    fireEvent.click(screen.getByLabelText('Pinned'));
    fireEvent.change(screen.getByTestId('rich-text'), { target: { value: '<p>Body</p>' } });

    fireEvent.click(screen.getByText('Save Post'));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/posts/author', expect.objectContaining({
        title: 'Complete Post',
        excerpt: 'Summary',
        featuredImageUrl: 'https://img.test/post.png',
        categorySlug: 'craft',
        tagSlugs: ['alpha', 'beta'],
        status: 'PUBLISHED',
        visibility: 'PREMIUM',
        featured: true,
        pinned: true,
        content: '<p>Body</p>',
      }));
    });
  });

  // Verifies loads and renders edit form so regressions are caught during automated tests.
  it('loads and renders edit form', async () => {
    api.get.mockResolvedValueOnce({ data: { data: mockPost } });
    renderPage('p1');
    
    await waitFor(() => {
      expect(screen.getByText('Edit Post')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Test Post')).toBeInTheDocument();
      expect(screen.getByDisplayValue('tech')).toBeInTheDocument();
      expect(screen.getByDisplayValue('react, testing')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Premium (PRO only)')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Published')).toBeInTheDocument();
    });
  });

  // Defines submits edits so related behavior stays grouped in one place.
  it('submits edits', async () => {
    api.get.mockResolvedValueOnce({ data: { data: mockPost } });
    renderPage('p1');
    
    await waitFor(() => expect(screen.getByDisplayValue('Test Post')).toBeInTheDocument());
    
    fireEvent.change(screen.getByPlaceholderText('Post title'), { target: { value: 'Updated Title' } });
    
    fireEvent.click(screen.getByText('Save Post'));
    
    await waitFor(() => {
      expect(api.put).toHaveBeenCalledWith('/api/posts/author/p1', expect.objectContaining({
        title: 'Updated Title',
        categorySlug: 'tech',
      }));
    });
  });

  // Verifies handles load error so regressions are caught during automated tests.
  it('handles load error', async () => {
    api.get.mockRejectedValueOnce(new Error('fail'));
    renderPage('p1');
    
    await waitFor(() => expect(screen.getByText('Failed to load post.')).toBeInTheDocument());
  });

  // Defines normalizes missing and invalid edit values so related behavior stays grouped in one place.
  it('normalizes missing and invalid edit values', async () => {
    api.get.mockResolvedValueOnce({
      data: {
        data: {
          title: null,
          content: null,
          excerpt: null,
          categorySlug: null,
          tagSlugs: 'solo',
          status: null,
          visibility: 'PRIVATE',
          featuredImageUrl: null,
          featured: null,
          pinned: null,
        },
      },
    });

    renderPage('p2');

    await waitFor(() => {
      expect(screen.getByText('Edit Post')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Post title').value).toBe('');
      expect(screen.getByDisplayValue('solo')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Draft')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Public')).toBeInTheDocument();
      expect(screen.getByLabelText('Featured')).not.toBeChecked();
      expect(screen.getByLabelText('Pinned')).not.toBeChecked();
    });
  });

  // Verifies handles save error so regressions are caught during automated tests.
  it('handles save error', async () => {
    api.post.mockRejectedValueOnce({ response: { data: { message: 'Save failed' } } });
    renderPage();
    
    fireEvent.change(screen.getByPlaceholderText('Post title'), { target: { value: 'New Title' } });
    fireEvent.click(screen.getByText('Save Post'));
    
    await waitFor(() => expect(screen.getByText('Save failed')).toBeInTheDocument());
  });

  // Performs the shows default save error when the server omits a message workflow so callers do not duplicate this logic.
  it('shows default save error when the server omits a message', async () => {
    api.post.mockRejectedValueOnce(new Error('fail'));
    renderPage();

    fireEvent.change(screen.getByPlaceholderText('Post title'), { target: { value: 'New Title' } });
    fireEvent.click(screen.getByText('Save Post'));

    await waitFor(() => expect(screen.getByText('Failed to save post.')).toBeInTheDocument());
  });
});
