/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { NewsletterConfirmPage } from '../pages/public/NewsletterConfirmPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

// Defines render page so related behavior stays grouped in one place.
function renderPage(searchParams = '') {
  return render(
    <MemoryRouter initialEntries={[`/newsletter/confirm${searchParams}`]}>
      <Routes>
        <Route path="newsletter/confirm" element={<NewsletterConfirmPage />} />
        <Route path="newsletter" element={<div>Newsletter Page</div>} />
      </Routes>
    </MemoryRouter>
  );
}

// Defines newsletter confirm page so related behavior stays grouped in one place.
describe('NewsletterConfirmPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Performs the shows loading state initially workflow so callers do not duplicate this logic.
  it('shows loading state initially', () => {
    api.get.mockImplementation(() => new Promise(() => {}));
    renderPage('?token=valid-token');
    expect(screen.getByText('Confirming your subscription...')).toBeInTheDocument();
  });

  // Defines shows error when no token provided so related behavior stays grouped in one place.
  it('shows error when no token provided', async () => {
    renderPage('');
    await waitFor(() => {
      expect(screen.getByText('Confirmation Failed')).toBeInTheDocument();
      expect(screen.getByText('Invalid or missing confirmation token.')).toBeInTheDocument();
    });
  });

  // Defines shows success on valid confirmation so related behavior stays grouped in one place.
  it('shows success on valid confirmation', async () => {
    api.get.mockResolvedValueOnce({ data: { message: 'Subscription confirmed!' } });
    renderPage('?token=valid-token');

    await waitFor(() => {
      expect(screen.getByText('Subscription Confirmed!')).toBeInTheDocument();
    });
    expect(screen.getByText(/Subscription confirmed!/)).toBeInTheDocument();
    expect(screen.getByText('Return to Home')).toBeInTheDocument();
  });

  // Defines shows error on failed confirmation so related behavior stays grouped in one place.
  it('shows error on failed confirmation', async () => {
    api.get.mockRejectedValueOnce({
      response: { data: { message: 'Token expired' } },
    });
    renderPage('?token=expired-token');

    await waitFor(() => {
      expect(screen.getByText('Confirmation Failed')).toBeInTheDocument();
      expect(screen.getByText('Token expired')).toBeInTheDocument();
    });
    expect(screen.getByText('Try Subscribing Again')).toBeInTheDocument();
  });

  // Defines shows generic error message on network failure so related behavior stays grouped in one place.
  it('shows generic error message on network failure', async () => {
    api.get.mockRejectedValueOnce(new Error('Network error'));
    renderPage('?token=some-token');

    await waitFor(() => {
      expect(screen.getByText('Confirmation Failed')).toBeInTheDocument();
      expect(screen.getByText(/Failed to confirm subscription/)).toBeInTheDocument();
    });
  });

  // Handles calls the correct api endpoint requests so the UI can call this feature through a stable endpoint.
  it('calls the correct API endpoint', async () => {
    api.get.mockResolvedValueOnce({ data: { message: 'OK' } });
    renderPage('?token=abc123');

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/api/newsletter/public/confirm/abc123');
    });
  });
});
