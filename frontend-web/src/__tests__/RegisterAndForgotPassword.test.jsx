/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { RegisterPage, ForgotPasswordPage } from '../pages/public/LoginPage';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { NotificationProvider } from '../context/NotificationContext';
import client from '../api/client';

vi.mock('../api/client');

// Defines render page so related behavior stays grouped in one place.
function renderPage(Component, route = '/register') {
  localStorage.clear();
  client.get.mockResolvedValue({ data: { data: 0 } });

  return render(
    <MemoryRouter initialEntries={[route]}>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <Routes>
              <Route path="register" element={<Component />} />
              <Route path="forgot-password" element={<Component />} />
              <Route path="login" element={<div>Login Page</div>} />
              <Route path="profile" element={<div>Profile</div>} />
              <Route path="admin" element={<div>Admin</div>} />
              <Route path="author" element={<div>Author</div>} />
            </Routes>
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

// Performs the register page workflow so callers do not duplicate this logic.
describe('RegisterPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    localStorage.clear();
  });

  // Verifies renders registration form so regressions are caught during automated tests.
  it('renders registration form', () => {
    renderPage(RegisterPage);
    expect(screen.getAllByText('Create account')[0]).toBeInTheDocument();
    expect(screen.getByText('Get started for free')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Full name')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Username')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Email')).toBeInTheDocument();
  });

  // Verifies renders role picker so regressions are caught during automated tests.
  it('renders role picker', () => {
    renderPage(RegisterPage);
    expect(screen.getByText('Reader')).toBeInTheDocument();
    expect(screen.getByText('Author')).toBeInTheDocument();
  });

  // Verifies handles role selection so regressions are caught during automated tests.
  it('handles role selection', () => {
    renderPage(RegisterPage);
    fireEvent.click(screen.getByText('Author'));
    // The Author button should now be selected (border changes, checked visually)
    expect(screen.getByText('Write posts, manage media, moderate.')).toBeInTheDocument();
  });

  // Verifies handles successful registration so regressions are caught during automated tests.
  it('handles successful registration', async () => {
    client.post.mockResolvedValueOnce({
      data: {
        data: {
          accessToken: 'token',
          refreshToken: 'refresh',
          user: { userId: 'u1', fullName: 'Test', role: 'READER', username: 'test', email: 'test@t.com' },
        },
      },
    });

    renderPage(RegisterPage);

    fireEvent.change(screen.getByPlaceholderText('Full name'), { target: { value: 'Test User' } });
    fireEvent.change(screen.getByPlaceholderText('Username'), { target: { value: 'testuser' } });
    fireEvent.change(screen.getByPlaceholderText('Email'), { target: { value: 'test@test.com' } });
    fireEvent.change(screen.getByPlaceholderText(/Password/), { target: { value: 'Password123' } });

    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith('/api/auth/register', expect.objectContaining({
        email: 'test@test.com',
        password: 'Password123',
        username: 'testuser',
        fullName: 'Test User',
        role: 'READER',
      }));
    });
  });

  // Defines shows error on failed registration so related behavior stays grouped in one place.
  it('shows error on failed registration', async () => {
    client.post.mockRejectedValueOnce({
      response: { data: { message: 'Email already taken' } },
    });

    renderPage(RegisterPage);

    fireEvent.change(screen.getByPlaceholderText('Full name'), { target: { value: 'Test' } });
    fireEvent.change(screen.getByPlaceholderText('Username'), { target: { value: 'test' } });
    fireEvent.change(screen.getByPlaceholderText('Email'), { target: { value: 'taken@test.com' } });
    fireEvent.change(screen.getByPlaceholderText(/Password/), { target: { value: 'Pass1234' } });

    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));

    await waitFor(() => {
      expect(screen.getByText('Email already taken')).toBeInTheDocument();
    });
  });

  // Performs the shows link to login page workflow so callers do not duplicate this logic.
  it('shows link to login page', () => {
    renderPage(RegisterPage);
    expect(screen.getByText('Sign in')).toBeInTheDocument();
  });
});

// Defines forgot password page so related behavior stays grouped in one place.
describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    localStorage.clear();
  });

  // Verifies renders step 1 email input so regressions are caught during automated tests.
  it('renders step 1 - email input', () => {
    renderPage(ForgotPasswordPage, '/forgot-password');
    expect(screen.getByText('Reset password')).toBeInTheDocument();
    expect(screen.getByText('Enter your email')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Registered email')).toBeInTheDocument();
    expect(screen.getByText('Send OTP')).toBeInTheDocument();
  });

  // Defines shows back to sign in link so related behavior stays grouped in one place.
  it('shows back to sign in link', () => {
    renderPage(ForgotPasswordPage, '/forgot-password');
    expect(screen.getByText('Back to sign in')).toBeInTheDocument();
  });

  // Performs the sends otp and advances to step 2 workflow so callers do not duplicate this logic.
  it('sends OTP and advances to step 2', async () => {
    client.post.mockResolvedValueOnce({ data: { message: 'OTP sent' } });

    renderPage(ForgotPasswordPage, '/forgot-password');

    fireEvent.change(screen.getByPlaceholderText('Registered email'), { target: { value: 'user@test.com' } });
    fireEvent.click(screen.getByText('Send OTP'));

    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith('/api/auth/forgot-password', { email: 'user@test.com' });
      expect(screen.getByText('OTP sent to your email.')).toBeInTheDocument();
      expect(screen.getAllByText('Verify OTP')[0]).toBeInTheDocument();
    });
  });

  // Performs the shows error on otp send failure workflow so callers do not duplicate this logic.
  it('shows error on OTP send failure', async () => {
    client.post.mockRejectedValueOnce({
      response: { data: { message: 'User not found' } },
    });

    renderPage(ForgotPasswordPage, '/forgot-password');

    fireEvent.change(screen.getByPlaceholderText('Registered email'), { target: { value: 'bad@test.com' } });
    fireEvent.click(screen.getByText('Send OTP'));

    await waitFor(() => {
      expect(screen.getByText('User not found')).toBeInTheDocument();
    });
  });

  // Defines verifies otp and advances to step 3 so related behavior stays grouped in one place.
  it('verifies OTP and advances to step 3', async () => {
    client.post.mockResolvedValueOnce({ data: {} }); // Send OTP
    client.post.mockResolvedValueOnce({ data: {} }); // Verify OTP

    renderPage(ForgotPasswordPage, '/forgot-password');

    // Step 1: Send OTP
    fireEvent.change(screen.getByPlaceholderText('Registered email'), { target: { value: 'user@test.com' } });
    fireEvent.click(screen.getByText('Send OTP'));

    await waitFor(() => expect(screen.getAllByText('Verify OTP')[0]).toBeInTheDocument());

    // Step 2: Enter OTP
    fireEvent.change(screen.getByPlaceholderText('Enter OTP'), { target: { value: '123456' } });
    fireEvent.click(screen.getByRole('button', { name: 'Verify OTP' }));

    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith('/api/auth/verify-otp', { email: 'user@test.com', otp: '123456' });
      expect(screen.getByText('OTP verified. Set your new password.')).toBeInTheDocument();
    });
  });

  // Defines resets password in step 3 so related behavior stays grouped in one place.
  it('resets password in step 3', async () => {
    client.post.mockResolvedValue({ data: {} }); // Resolve for all steps

    renderPage(ForgotPasswordPage, '/forgot-password');

    // Step 1
    fireEvent.change(screen.getByPlaceholderText('Registered email'), { target: { value: 'user@test.com' } });
    fireEvent.click(screen.getByText('Send OTP'));

    await waitFor(() => expect(screen.getByPlaceholderText('Enter OTP')).toBeInTheDocument());

    // Step 2
    fireEvent.change(screen.getByPlaceholderText('Enter OTP'), { target: { value: '123456' } });
    fireEvent.click(screen.getByRole('button', { name: 'Verify OTP' }));

    await waitFor(() => expect(screen.getByPlaceholderText(/New password/)).toBeInTheDocument());

    // Step 3
    fireEvent.change(screen.getByPlaceholderText(/New password/), { target: { value: 'NewPass123' } });
    fireEvent.click(screen.getByText('Reset Password'));

    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith('/api/auth/reset-password', { email: 'user@test.com', newPassword: 'NewPass123' });
      expect(screen.getByText('Password reset successfully!')).toBeInTheDocument();
    });
  });

  // Defines shows step indicators so related behavior stays grouped in one place.
  it('shows step indicators', () => {
    renderPage(ForgotPasswordPage, '/forgot-password');
    // 3 step indicator divs
    const indicators = document.querySelectorAll('div[class*="h-1.5"]');
    expect(indicators.length).toBe(3);
  });

  // Verifies handles resend otp so regressions are caught during automated tests.
  it('handles resend OTP', async () => {
    client.post.mockResolvedValueOnce({ data: {} }); // Send OTP

    renderPage(ForgotPasswordPage, '/forgot-password');

    fireEvent.change(screen.getByPlaceholderText('Registered email'), { target: { value: 'user@test.com' } });
    fireEvent.click(screen.getByText('Send OTP'));

    await waitFor(() => expect(screen.getByText('Resend OTP')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Resend OTP'));
    // Should go back to step 1
    await waitFor(() => expect(screen.getByPlaceholderText('Registered email')).toBeInTheDocument());
  });
});
