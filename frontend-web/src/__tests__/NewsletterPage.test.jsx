/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { NewsletterPage } from '../pages/public/NewsletterPage';
import { ThemeProvider } from '../context/ThemeContext';
import client from '../api/client';

vi.mock('../api/client');

// Defines newsletter page so related behavior stays grouped in one place.
describe('NewsletterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    client.get.mockResolvedValue({ data: { data: [] } });
  });

  // Defines render newsletter page so related behavior stays grouped in one place.
  const renderNewsletterPage = () => {
    return render(
      <MemoryRouter>
        <ThemeProvider>
          <NewsletterPage />
        </ThemeProvider>
      </MemoryRouter>
    );
  };

  // Verifies renders newsletter subscription form so regressions are caught during automated tests.
  it('renders newsletter subscription form', () => {
    renderNewsletterPage();

    expect(screen.getByText('Weekly notes, thoughtful writing, and release updates.')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Email address')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Subscribe/i })).toBeInTheDocument();
  });

  // Verifies handles successful subscription so regressions are caught during automated tests.
  it('handles successful subscription', async () => {
    client.post.mockResolvedValueOnce({
      data: {
        message: 'Please check your email to confirm your subscription.'
      }
    });

    renderNewsletterPage();

    fireEvent.change(screen.getByPlaceholderText('Email address'), {
      target: { value: 'subscriber@example.com' }
    });

    fireEvent.click(screen.getByRole('button', { name: /Subscribe/i }));

    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith('/api/newsletter/public/subscribe', {
        email: 'subscriber@example.com',
        fullName: ''
      });
    });

    // Check for success message
    await waitFor(() => {
      expect(screen.getByText('Please check your email to confirm your subscription.')).toBeInTheDocument();
    });
  });

  // Defines displays error message on failed subscription so related behavior stays grouped in one place.
  it('displays error message on failed subscription', async () => {
    client.post.mockRejectedValueOnce({
      response: {
        data: {
          message: 'Email is already subscribed.'
        }
      }
    });

    renderNewsletterPage();

    fireEvent.change(screen.getByPlaceholderText('Email address'), {
      target: { value: 'existing@example.com' }
    });

    fireEvent.click(screen.getByRole('button', { name: /Subscribe/i }));

    await waitFor(() => {
      expect(screen.getByText('Email is already subscribed.')).toBeInTheDocument();
    });
  });
});
