/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AuthorMediaPage } from '../pages/author/AuthorMediaPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({})),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/ui/LoadingSpinner', () => ({
  PageLoader: () => <div data-testid="page-loader">Loading...</div>,
}));

import api from '../api/client';

const mockMedia = {
  mediaId: 'm1',
  url: 'http://example.com/image.jpg',
  originalName: 'image.jpg',
  sizeKb: 100,
  mimeType: 'image/jpeg',
};

// Provides author media page wiring so the framework can apply the expected runtime behavior.
describe('AuthorMediaPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.alert = vi.fn();
    window.confirm = vi.fn(() => true);
    
    // Mock navigator.clipboard
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn(),
      },
    });
  });

  // Defines shows empty state when no media so related behavior stays grouped in one place.
  it('shows empty state when no media', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [] } });
    render(<MemoryRouter><AuthorMediaPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No media uploaded yet')).toBeInTheDocument());
  });

  // Verifies renders media library so regressions are caught during automated tests.
  it('renders media library', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockMedia] } });
    render(<MemoryRouter><AuthorMediaPage /></MemoryRouter>);
    
    await waitFor(() => {
      expect(screen.getByText('image.jpg')).toBeInTheDocument();
      expect(screen.getByText('100 KB · JPEG')).toBeInTheDocument();
    });
  });

  // Verifies handles image preview so regressions are caught during automated tests.
  it('handles image preview', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockMedia] } });
    render(<MemoryRouter><AuthorMediaPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('image.jpg')).toBeInTheDocument());
    
    const image = screen.getByAltText('image.jpg');
    fireEvent.click(image);
    
    // Modal header or details should appear
    await waitFor(() => {
      // Find the modal details by text content of elements
      expect(screen.getAllByText('image.jpg').length).toBeGreaterThan(1);
      expect(screen.getByText('100 KB · image/jpeg')).toBeInTheDocument();
    });
    
    // Close modal — find the close button inside the modal overlay
    const modalOverlay = screen.getByText('100 KB · image/jpeg').closest('div[class*="fixed"]');
    const allBtns = modalOverlay.querySelectorAll('button');
    // The first button inside the modal is the close (X) button
    const closeBtn = allBtns[0];
    fireEvent.click(closeBtn);
    
    await waitFor(() => expect(screen.queryByText('100 KB · image/jpeg')).not.toBeInTheDocument());
  });

  // Performs the deletes media workflow so callers do not duplicate this logic.
  it('deletes media', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockMedia] } });
    render(<MemoryRouter><AuthorMediaPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('image.jpg')).toBeInTheDocument());
    
    // Two buttons: Copy URL, Delete
    const deleteBtn = screen.getAllByRole('button').find(b => !b.textContent.includes('Copy') && !b.textContent.includes('Copied'));
    fireEvent.click(deleteBtn);
    
    expect(window.confirm).toHaveBeenCalled();
    expect(api.delete).toHaveBeenCalledWith('/api/media/m1');
    
    await waitFor(() => expect(screen.queryByText('image.jpg')).not.toBeInTheDocument());
  });

  // Defines copies media url so related behavior stays grouped in one place.
  it('copies media URL', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockMedia] } });
    render(<MemoryRouter><AuthorMediaPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('image.jpg')).toBeInTheDocument());
    
    const copyBtn = screen.getByText(/Copy URL/);
    fireEvent.click(copyBtn);
    
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('http://example.com/image.jpg');
    expect(screen.getByText(/Copied!/)).toBeInTheDocument();
  });

  // Performs the rejects unsupported file upload workflow so callers do not duplicate this logic.
  it('rejects unsupported file upload', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [] } });
    render(<MemoryRouter><AuthorMediaPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Media Library')).toBeInTheDocument());
    
    const file = new File(['hello'], 'hello.txt', { type: 'text/plain' });
    const input = document.querySelector('input[type="file"]');
    
    fireEvent.change(input, { target: { files: [file] } });
    
    expect(window.alert).toHaveBeenCalledWith('Only JPG, JPEG, PNG, and WebP images are supported.');
    expect(api.post).not.toHaveBeenCalled();
  });

  // Performs the rejects large file upload workflow so callers do not duplicate this logic.
  it('rejects large file upload', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [] } });
    render(<MemoryRouter><AuthorMediaPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Media Library')).toBeInTheDocument());
    
    const file = new File([new ArrayBuffer(11 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' });
    const input = document.querySelector('input[type="file"]');
    
    fireEvent.change(input, { target: { files: [file] } });
    
    expect(window.alert).toHaveBeenCalledWith('File must be under 10MB.');
    expect(api.post).not.toHaveBeenCalled();
  });

  // Performs the uploads valid file workflow so callers do not duplicate this logic.
  it('uploads valid file', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [] } });
    // the second get after successful upload
    api.get.mockResolvedValueOnce({ data: { data: [mockMedia] } });
    
    render(<MemoryRouter><AuthorMediaPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Media Library')).toBeInTheDocument());
    
    const file = new File(['fake-image'], 'test.jpg', { type: 'image/jpeg' });
    const input = document.querySelector('input[type="file"]');
    
    fireEvent.change(input, { target: { files: [file] } });
    
    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/api/media/author/upload', expect.any(FormData), expect.any(Object)));
    
    // Wait for the library to reload
    await waitFor(() => expect(screen.getByText('image.jpg')).toBeInTheDocument());
  });

  // Verifies handles drag and drop so regressions are caught during automated tests.
  it('handles drag and drop', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [] } });
    render(<MemoryRouter><AuthorMediaPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Media Library')).toBeInTheDocument());
    
    const dropzone = screen.getByText(/Drop image here/i).parentElement;
    
    fireEvent.dragOver(dropzone);
    expect(dropzone).toHaveClass('border-brand');
    
    fireEvent.dragLeave(dropzone);
    expect(dropzone).not.toHaveClass('border-brand');
    
    const file = new File(['fake-image'], 'test.png', { type: 'image/png' });
    fireEvent.drop(dropzone, {
      dataTransfer: { files: [file] },
    });
    
    await waitFor(() => expect(api.post).toHaveBeenCalled());
  });

  // Verifies handles fetch error gracefully so regressions are caught during automated tests.
  it('handles fetch error gracefully', async () => {
    api.get.mockRejectedValueOnce(new Error('fail'));
    render(<MemoryRouter><AuthorMediaPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No media uploaded yet')).toBeInTheDocument());
  });
});
