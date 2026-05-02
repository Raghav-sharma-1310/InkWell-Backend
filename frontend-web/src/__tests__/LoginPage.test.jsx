/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { LoginPage } from '../pages/public/LoginPage';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { NotificationProvider } from '../context/NotificationContext';
import client from '../api/client';

// Mock the API client
vi.mock('../api/client');

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    client.get.mockResolvedValue({ data: { data: 0 } });
  });

  // Performs the render login page workflow so callers do not duplicate this logic.
  const renderLoginPage = () => {
    return render(
      <MemoryRouter>
        <ThemeProvider>
          <AuthProvider>
            <NotificationProvider>
              <LoginPage />
            </NotificationProvider>
          </AuthProvider>
        </ThemeProvider>
      </MemoryRouter>
    );
  };

  // Verifies renders login form elements so regressions are caught during automated tests.
  it('renders login form elements', () => {
    renderLoginPage();

    expect(screen.getByText('Welcome back')).toBeInTheDocument();
    expect(screen.getByText('Sign in to InkWell')).toBeInTheDocument();
    
    // Check if input fields exist
    expect(screen.getByPlaceholderText('Email or username')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Password')).toBeInTheDocument();
    
    // Check if sign in button exists
    expect(screen.getByRole('button', { name: /Sign in/i })).toBeInTheDocument();
  });

  // Verifies handles successful login so regressions are caught during automated tests.
  it('handles successful login', async () => {
    // Mock the API response for login
    client.post.mockResolvedValueOnce({
      data: {
        data: {
          token: 'mock-token',
          user: {
            userId: '1',
            email: 'test@example.com',
            username: 'testuser',
            role: 'READER'
          }
        }
      }
    });

    renderLoginPage();

    // Fill out the form
    fireEvent.change(screen.getByPlaceholderText('Email or username'), {
      target: { value: 'test@example.com' }
    });
    fireEvent.change(screen.getByPlaceholderText('Password'), {
      target: { value: 'password123' }
    });

    // Submit the form
    fireEvent.click(screen.getByRole('button', { name: /Sign in/i }));

    // Verify the API was called with the correct arguments
    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith('/api/auth/login', {
        email: 'test@example.com',
        password: 'password123'
      });
    });
  });

  // Performs the displays error message on failed login workflow so callers do not duplicate this logic.
  it('displays error message on failed login', async () => {
    // Mock the API to simulate a failed login
    client.post.mockRejectedValueOnce({
      response: {
        data: {
          message: 'Invalid credentials'
        }
      }
    });

    renderLoginPage();

    // Fill out the form
    fireEvent.change(screen.getByPlaceholderText('Email or username'), {
      target: { value: 'wrong@example.com' }
    });
    fireEvent.change(screen.getByPlaceholderText('Password'), {
      target: { value: 'wrongpass' }
    });

    // Submit the form
    fireEvent.click(screen.getByRole('button', { name: /Sign in/i }));

    // Wait for the error message to appear in the UI
    await waitFor(() => {
      expect(screen.getByText('Invalid credentials')).toBeInTheDocument();
    });
  });
});
