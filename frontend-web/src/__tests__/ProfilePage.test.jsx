/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { ProfilePage } from '../pages/reader/ProfilePage';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { NotificationProvider } from '../context/NotificationContext';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

const readerUser = {
  userId: 'u1', username: 'reader', email: 'reader@test.com',
  role: 'READER', fullName: 'Test Reader', provider: 'LOCAL', active: true,
  subscriptionTier: 'FREE', subscriptionStatus: 'INACTIVE',
  bio: 'Test bio', phoneNumber: '', avatarUrl: '', createdAt: '2026-01-01T00:00:00',
};

const proUser = {
  ...readerUser, subscriptionTier: 'PRO', subscriptionStatus: 'ACTIVE',
};

const authorUser = {
  ...readerUser, role: 'AUTHOR',
};

// Defines render page so related behavior stays grouped in one place.
function renderPage(user = readerUser, useDefaultMocks = true) {
  localStorage.setItem('inkwell.user', JSON.stringify(user));
  localStorage.setItem('inkwell.accessToken', 'mock-token');

  if (useDefaultMocks) {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/auth/payments/history')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/notifications')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/newsletter/me')) return Promise.resolve({ data: { data: { status: 'ACTIVE' } } });
      if (url.includes('/api/author-request/status')) return Promise.resolve({ data: { data: null } });
      if (url.includes('/api/posts/reader/bookmarks')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/posts/reader/history')) return Promise.resolve({ data: { data: { content: [] } } });
      return Promise.resolve({ data: { data: 0 } });
    });
  }

  return render(
    <MemoryRouter>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <ProfilePage />
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

// Defines profile page so related behavior stays grouped in one place.
describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  // Performs the shows loading state initially workflow so callers do not duplicate this logic.
  it('shows loading state initially', () => {
    api.get.mockImplementation(() => new Promise(() => {}));
    localStorage.setItem('inkwell.user', JSON.stringify(readerUser));
    localStorage.setItem('inkwell.accessToken', 'mock-token');
    render(
      <MemoryRouter>
        <ThemeProvider>
          <AuthProvider>
            <NotificationProvider>
              <ProfilePage />
            </NotificationProvider>
          </AuthProvider>
        </ThemeProvider>
      </MemoryRouter>
    );
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  // Verifies renders user header so regressions are caught during automated tests.
  it('renders user header', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getAllByText('Test Reader')[0]).toBeInTheDocument();
      expect(screen.getByText(/@reader · reader@test.com/)).toBeInTheDocument();
      expect(screen.getAllByText('READER')[0]).toBeInTheDocument();
      expect(screen.getAllByText('LOCAL')[0]).toBeInTheDocument();
    });
  });

  // Verifies renders overview tab by default so regressions are caught during automated tests.
  it('renders overview tab by default', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Account Info')).toBeInTheDocument();
      expect(screen.getByText('Recent Notifications')).toBeInTheDocument();
    });
  });

  // Defines shows account info fields so related behavior stays grouped in one place.
  it('shows account info fields', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Username')).toBeInTheDocument();
      expect(screen.getByText('Email')).toBeInTheDocument();
      expect(screen.getByText('Full Name')).toBeInTheDocument();
      expect(screen.getByText('Role')).toBeInTheDocument();
      expect(screen.getByText('Subscription')).toBeInTheDocument();
      expect(screen.getByText('Newsletter')).toBeInTheDocument();
    });
  });

  // Defines shows so related behavior stays grouped in one place.
  it('shows "Free" subscription for non-pro user', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getAllByText('Free')[0]).toBeInTheDocument();
    });
  });

  // Defines shows pro badge for pro user so related behavior stays grouped in one place.
  it('shows PRO badge for pro user', async () => {
    renderPage(proUser);
    await waitFor(() => {
      expect(screen.getAllByText('PRO')[0]).toBeInTheDocument();
    });
  });

  // Defines shows bio in header so related behavior stays grouped in one place.
  it('shows bio in header', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getAllByText('Test bio')[0]).toBeInTheDocument();
    });
  });

  // Defines switches to edit profile tab so related behavior stays grouped in one place.
  it('switches to edit profile tab', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Edit Profile'));

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Full name')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Username')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Bio (about you)')).toBeInTheDocument();
    });
  });

  // Defines validates full name so related behavior stays grouped in one place.
  it('validates full name', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit Profile'));

    await waitFor(() => expect(screen.getByPlaceholderText('Full name')).toBeInTheDocument());
    
    fireEvent.change(screen.getByPlaceholderText('Full name'), { target: { value: '' } });
    fireEvent.blur(screen.getByPlaceholderText('Full name'));

    await waitFor(() => {
      expect(screen.getByText('Full name is required')).toBeInTheDocument();
    });
  });

  // Defines validates username so related behavior stays grouped in one place.
  it('validates username', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit Profile'));

    await waitFor(() => expect(screen.getByPlaceholderText('Username')).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText('Username'), { target: { value: 'ab' } });
    fireEvent.blur(screen.getByPlaceholderText('Username'));

    await waitFor(() => {
      expect(screen.getByText('Username must be at least 3 characters')).toBeInTheDocument();
    });
  });

  // Performs the saves profile successfully workflow so callers do not duplicate this logic.
  it('saves profile successfully', async () => {
    api.patch.mockResolvedValueOnce({
      data: { data: { ...readerUser, fullName: 'Updated Name' } },
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit Profile'));

    await waitFor(() => expect(screen.getByPlaceholderText('Full name')).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText('Full name'), { target: { value: 'Updated Name' } });
    fireEvent.click(screen.getByText('Save Changes'));

    await waitFor(() => {
      expect(api.patch).toHaveBeenCalledWith('/api/auth/me', expect.objectContaining({ fullName: 'Updated Name' }));
    });
  });

  // Performs the shows save error workflow so callers do not duplicate this logic.
  it('shows save error', async () => {
    api.patch.mockRejectedValueOnce({
      response: { data: { message: 'Update failed' } },
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit Profile'));

    await waitFor(() => expect(screen.getByPlaceholderText('Full name')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Save Changes'));

    await waitFor(() => {
      expect(screen.getByText('Update failed')).toBeInTheDocument();
    });
  });

  // Defines switches to password tab so related behavior stays grouped in one place.
  it('switches to password tab', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Password')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Password'));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Change Password' })).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Current password')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('New password')).toBeInTheDocument();
    });
  });

  // Defines changes password successfully so related behavior stays grouped in one place.
  it('changes password successfully', async () => {
    api.patch.mockResolvedValueOnce({});

    renderPage();
    await waitFor(() => expect(screen.getByText('Password')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Password'));

    await waitFor(() => expect(screen.getByPlaceholderText('Current password')).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText('Current password'), { target: { value: 'OldPass123' } });
    fireEvent.change(screen.getByPlaceholderText('New password'), { target: { value: 'NewPass123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Change Password' }));

    await waitFor(() => {
      expect(api.patch).toHaveBeenCalledWith('/api/auth/me/password', { currentPassword: 'OldPass123', newPassword: 'NewPass123' });
    });
  });

  // Defines switches to billing tab so related behavior stays grouped in one place.
  it('switches to billing tab', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Billing')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Billing'));

    await waitFor(() => {
      expect(screen.getByText('Your Subscription')).toBeInTheDocument();
      expect(screen.getByText('Plans')).toBeInTheDocument();
      expect(screen.getByText('Transaction History')).toBeInTheDocument();
    });
  });

  // Defines shows free plan status on billing tab so related behavior stays grouped in one place.
  it('shows free plan status on billing tab', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Billing')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Billing'));

    await waitFor(() => {
      expect(screen.getByText('Free Plan')).toBeInTheDocument();
    });
  });

  // Defines shows pro active status for pro user on billing tab so related behavior stays grouped in one place.
  it('shows PRO Active status for pro user on billing tab', async () => {
    renderPage(proUser);
    await waitFor(() => expect(screen.getByText('Billing')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Billing'));

    await waitFor(() => {
      expect(screen.getAllByText(/Active/)[0]).toBeInTheDocument();
    });
  });

  // Defines shows reader pro plan for reader so related behavior stays grouped in one place.
  it('shows reader pro plan for reader', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Billing')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Billing'));

    await waitFor(() => {
      expect(screen.getByText('InkWell Reader Pro')).toBeInTheDocument();
      expect(screen.getByText('₹149')).toBeInTheDocument();
    });
  });

  // Performs the shows bookmarks tab with pro gate for free user workflow so callers do not duplicate this logic.
  it('shows bookmarks tab with PRO gate for free user', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Bookmarks')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Bookmarks'));

    await waitFor(() => {
      expect(screen.getByText('Bookmarks are a PRO feature')).toBeInTheDocument();
    });
  });

  // Defines shows reading history tab with pro gate for free user so related behavior stays grouped in one place.
  it('shows reading history tab with PRO gate for free user', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Reading History')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Reading History'));

    await waitFor(() => {
      expect(screen.getByText('Reading History is a PRO feature')).toBeInTheDocument();
    });
  });

  // Performs the shows saved posts for pro user workflow so callers do not duplicate this logic.
  it('shows saved posts for PRO user', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/posts/reader/bookmarks')) return Promise.resolve({
        data: { data: [{ postId: 'p1', title: 'Saved Post', slug: 'saved-post', excerpt: 'saved', createdAt: '2026-01-01T00:00:00' }] },
      });
      if (url.includes('/api/posts/reader/history')) return Promise.resolve({ data: { data: { content: [] } } });
      if (url.includes('/api/auth/payments/history')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/notifications')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/newsletter/me')) return Promise.resolve({ data: { data: { status: 'ACTIVE' } } });
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage(proUser, false);
    await waitFor(() => expect(screen.getByText('Bookmarks')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Bookmarks'));

    await waitFor(() => {
      expect(screen.getByText('Saved Post')).toBeInTheDocument();
    });
  });

  // Provides shows become author tab for reader wiring so the framework can apply the expected runtime behavior.
  it('shows become author tab for reader', async () => {
    renderPage(readerUser, false);
    await waitFor(() => expect(screen.getByText('Become Author')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Become Author'));

    await waitFor(() => {
      expect(screen.getByText('Become an Author')).toBeInTheDocument();
      expect(screen.getByText('Request to Become Author')).toBeInTheDocument();
    });
  });

  // Provides shows author request status when pending wiring so the framework can apply the expected runtime behavior.
  it('shows author request status when pending', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/author-request/status')) return Promise.resolve({
        data: { data: { status: 'PENDING', requestedAt: '2026-01-01T00:00:00' } },
      });
      if (url.includes('/api/auth/payments/history')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/notifications')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/newsletter/me')) return Promise.resolve({ data: { data: null } });
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage(readerUser, false);
    await waitFor(() => expect(screen.getByText('Become Author')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Become Author'));

    await waitFor(() => {
      expect(screen.getByText('Request Pending')).toBeInTheDocument();
    });
  });

  // Defines shows no notifications message so related behavior stays grouped in one place.
  it('shows no notifications message', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('No notifications yet.')).toBeInTheDocument();
    });
  });

  // Defines shows notifications when present so related behavior stays grouped in one place.
  it('shows notifications when present', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/notifications')) return Promise.resolve({
        data: { data: [{ notificationId: 'n1', title: 'New follower', message: 'Someone followed you' }] },
      });
      if (url.includes('/api/auth/payments/history')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/newsletter/me')) return Promise.resolve({ data: { data: null } });
      if (url.includes('/api/author-request/status')) return Promise.resolve({ data: { data: null } });
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage(readerUser, false);
    await waitFor(() => {
      expect(screen.getByText('New follower')).toBeInTheDocument();
    });
  });

  // Defines shows user initial as avatar fallback so related behavior stays grouped in one place.
  it('shows user initial as avatar fallback', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('T')).toBeInTheDocument(); // First letter of 'Test Reader'
    });
  });

  // Defines does not show billing tab for admin so related behavior stays grouped in one place.
  it('does not show billing tab for ADMIN', async () => {
    const adminUser = { ...readerUser, role: 'ADMIN' };
    // For ADMIN, no author-request needed
    api.get.mockImplementation((url) => {
      if (url.includes('/api/auth/payments/history')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/notifications')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/newsletter/me')) return Promise.resolve({ data: { data: null } });
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage(adminUser);
    await waitFor(() => expect(screen.getByText('Account Info')).toBeInTheDocument());
    expect(screen.queryByText('Billing')).not.toBeInTheDocument();
  });

  // Provides does not show become author tab for author wiring so the framework can apply the expected runtime behavior.
  it('does not show become author tab for AUTHOR', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/auth/payments/history')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/notifications')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/newsletter/me')) return Promise.resolve({ data: { data: null } });
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage(authorUser);
    await waitFor(() => expect(screen.getByText('Account Info')).toBeInTheDocument());
    expect(screen.queryByText('Become Author')).not.toBeInTheDocument();
  });

  // Defines validates phone number format so related behavior stays grouped in one place.
  it('validates phone number format', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit Profile'));

    await waitFor(() => expect(screen.getByPlaceholderText(/9876543210/)).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText(/9876543210/), { target: { value: 'abc' } });
    fireEvent.blur(screen.getByPlaceholderText(/9876543210/));

    await waitFor(() => {
      expect(screen.getByText(/Phone must be 10-15 digits/)).toBeInTheDocument();
    });
  });

  // Defines validates bio length so related behavior stays grouped in one place.
  it('validates bio length', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit Profile'));

    await waitFor(() => expect(screen.getByPlaceholderText('Bio (about you)')).toBeInTheDocument());

    const longBio = 'a'.repeat(1001);
    fireEvent.change(screen.getByPlaceholderText('Bio (about you)'), { target: { value: longBio } });
    fireEvent.blur(screen.getByPlaceholderText('Bio (about you)'));

    await waitFor(() => {
      expect(screen.getByText('Bio must not exceed 1000 characters')).toBeInTheDocument();
    });
  });

  // Defines validates profile form before saving so related behavior stays grouped in one place.
  it('validates profile form before saving', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit Profile'));

    await waitFor(() => expect(screen.getByPlaceholderText('Full name')).toBeInTheDocument());
    fireEvent.change(screen.getByPlaceholderText('Full name'), { target: { value: 'A1' } });
    fireEvent.change(screen.getByPlaceholderText('Username'), { target: { value: 'bad name' } });
    fireEvent.submit(screen.getByPlaceholderText('Full name').closest('form'));

    await waitFor(() => {
      expect(screen.getByText('Full name must contain only letters and spaces')).toBeInTheDocument();
      expect(screen.getByText('Username cannot contain spaces')).toBeInTheDocument();
      expect(screen.getByText('Please fix the validation errors before saving.')).toBeInTheDocument();
    });
    expect(api.patch).not.toHaveBeenCalled();
  });

  // Performs the maps field errors returned by profile update api workflow so callers do not duplicate this logic.
  it('maps fieldErrors returned by profile update API', async () => {
    api.patch.mockRejectedValueOnce({
      response: { data: { fieldErrors: { username: 'Username already exists' } } },
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit Profile'));
    await waitFor(() => expect(screen.getByPlaceholderText('Username')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Save Changes'));

    await waitFor(() => {
      expect(screen.getByText('Username already exists')).toBeInTheDocument();
      expect(screen.getByText('Validation failed. Please check the highlighted fields.')).toBeInTheDocument();
    });
  });

  // Performs the maps array validation errors returned by profile update api workflow so callers do not duplicate this logic.
  it('maps array validation errors returned by profile update API', async () => {
    api.patch.mockRejectedValueOnce({
      response: { data: { errors: [{ field: 'phoneNumber', message: 'Phone is invalid' }] } },
    });

    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit Profile'));
    await waitFor(() => expect(screen.getByPlaceholderText(/9876543210/)).toBeInTheDocument());
    fireEvent.click(screen.getByText('Save Changes'));

    await waitFor(() => {
      expect(screen.getByText('Phone is invalid')).toBeInTheDocument();
      expect(screen.getByText('Validation failed. Please check the highlighted fields.')).toBeInTheDocument();
    });
  });

  // Defines changes password and reports errors so related behavior stays grouped in one place.
  it('changes password and reports errors', async () => {
    api.patch.mockRejectedValueOnce({ response: { data: { message: 'Wrong current password' } } });

    renderPage();
    await waitFor(() => expect(screen.getByText('Password')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Password'));
    await waitFor(() => expect(screen.getByPlaceholderText('Current password')).toBeInTheDocument());
    fireEvent.change(screen.getByPlaceholderText('Current password'), { target: { value: 'badpass123' } });
    fireEvent.change(screen.getByPlaceholderText('New password'), { target: { value: 'NewPass123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Change Password' }));

    await waitFor(() => expect(screen.getByText('Wrong current password')).toBeInTheDocument());
  });

  // Verifies submits a new author request and handles approved/rejected statuses so regressions are caught during automated tests.
  it('submits a new author request and handles approved/rejected statuses', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/author-request/status')) return Promise.resolve({ data: { data: null } });
      if (url.includes('/api/auth/payments/history')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/notifications')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/newsletter/me')) return Promise.resolve({ data: { data: null } });
      return Promise.resolve({ data: { data: 0 } });
    });
    api.post.mockResolvedValueOnce({
      data: { data: { status: 'APPROVED', requestedAt: '2026-01-01T00:00:00', adminRemarks: 'Welcome aboard' } },
    });

    renderPage(readerUser, false);
    await waitFor(() => expect(screen.getByText('Become Author')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Become Author'));
    await waitFor(() => expect(screen.getByRole('button', { name: /Request to Become Author/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Request to Become Author/ }));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/author-request');
      expect(screen.getByText('Request Approved')).toBeInTheDocument();
      expect(screen.getByText(/Welcome aboard/)).toBeInTheDocument();
      expect(screen.getByText(/Congratulations/)).toBeInTheDocument();
    });
  });

  // Provides shows rejected author request guidance wiring so the framework can apply the expected runtime behavior.
  it('shows rejected author request guidance', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('/api/author-request/status')) return Promise.resolve({
        data: { data: { status: 'REJECTED', requestedAt: '2026-01-01T00:00:00', adminRemarks: 'Add more samples' } },
      });
      if (url.includes('/api/auth/payments/history')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/notifications')) return Promise.resolve({ data: { data: [] } });
      if (url.includes('/api/newsletter/me')) return Promise.resolve({ data: { data: null } });
      return Promise.resolve({ data: { data: 0 } });
    });

    renderPage(readerUser, false);
    await waitFor(() => expect(screen.getByText('Become Author')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Become Author'));

    await waitFor(() => {
      expect(screen.getByText('Request Rejected')).toBeInTheDocument();
      expect(screen.getByText(/Add more samples/)).toBeInTheDocument();
      expect(screen.getByText(/submit a new request/)).toBeInTheDocument();
    });
  });

  // Performs the uploads and removes an avatar workflow so callers do not duplicate this logic.
  it('uploads and removes an avatar', async () => {
    api.post.mockResolvedValueOnce({ data: { data: { url: 'https://cdn.test/avatar.png' } } });
    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit Profile'));
    await waitFor(() => expect(screen.getByPlaceholderText('Full name')).toBeInTheDocument());

    const fileInput = document.querySelector('input[type="file"]');
    fireEvent.change(fileInput, { target: { files: [new File(['avatar'], 'avatar.png', { type: 'image/png' })] } });

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/media/user/upload-avatar', expect.any(FormData), expect.objectContaining({
        headers: { 'Content-Type': 'multipart/form-data' },
      }));
      expect(screen.getByDisplayValue('https://cdn.test/avatar.png')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Remove Photo'));
    expect(screen.getByPlaceholderText('Avatar URL').value).toBe('');
  });

  // Verifies handles avatar upload failure and no selected file so regressions are caught during automated tests.
  it('handles avatar upload failure and no selected file', async () => {
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});
    api.post.mockRejectedValueOnce(new Error('upload failed'));
    renderPage();
    await waitFor(() => expect(screen.getByText('Edit Profile')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Edit Profile'));
    await waitFor(() => expect(screen.getByPlaceholderText('Avatar URL')).toBeInTheDocument());

    const fileInput = document.querySelector('input[type="file"]');
    fireEvent.change(fileInput, { target: { files: [new File(['bad'], 'bad.png', { type: 'image/png' })] } });
    await waitFor(() => expect(alertSpy).toHaveBeenCalledWith('Upload failed.'));
    alertSpy.mockRestore();
  });

  // Verifies handles successful razorpay payment verification so regressions are caught during automated tests.
  it('handles successful Razorpay payment verification', async () => {
    const verifiedUser = { ...proUser, fullName: 'Verified Reader' };
    Object.defineProperty(window, 'Razorpay', {
      configurable: true,
      writable: true,
      value: vi.fn().mockImplementation((options) => ({
        open: vi.fn(() => options.handler({
          razorpay_order_id: 'gateway-order',
          razorpay_payment_id: 'gateway-payment',
          razorpay_signature: 'sig',
        })),
      })),
    });
    api.post.mockImplementation((url) => {
      if (url.includes('/orders')) return Promise.resolve({
        data: { data: { paymentOrderId: 'po1', gatewayPublicKey: 'key', gatewayOrderId: 'gateway-order' } },
      });
      if (url.includes('/verify')) return Promise.resolve({
        data: { data: { accessToken: 'new-token', refreshToken: 'new-refresh', user: verifiedUser } },
      });
      return Promise.resolve({ data: { data: {} } });
    });

    renderPage(readerUser);
    await waitFor(() => expect(screen.getByText('Billing')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Billing'));
    await waitFor(() => expect(screen.getByText('Subscribe')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Subscribe'));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/auth/payments/orders', expect.objectContaining({
        amount: 149,
        currency: 'INR',
      }));
      expect(api.post).toHaveBeenCalledWith('/api/auth/payments/verify', expect.objectContaining({
        paymentOrderId: 'po1',
        gatewayPaymentId: 'gateway-payment',
      }));
      expect(screen.getByText('Payment verified! Your subscription is now active.')).toBeInTheDocument();
    });

    delete window.Razorpay;
  });

  // Verifies handles payment order and verification failures so regressions are caught during automated tests.
  it('handles payment order and verification failures', async () => {
    api.post.mockRejectedValueOnce({ response: { data: { message: 'Order failed' } } });
    renderPage(readerUser);
    await waitFor(() => expect(screen.getByText('Billing')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Billing'));
    await waitFor(() => expect(screen.getByText('Subscribe')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Subscribe'));
    await waitFor(() => expect(screen.getByText('Order failed')).toBeInTheDocument());

    Object.defineProperty(window, 'Razorpay', {
      configurable: true,
      writable: true,
      value: vi.fn().mockImplementation((options) => ({
        open: vi.fn(() => options.handler({
          razorpay_order_id: 'gateway-order',
          razorpay_payment_id: 'gateway-payment',
          razorpay_signature: 'sig',
        })),
      })),
    });
    api.post.mockImplementation((url) => {
      if (url.includes('/orders')) return Promise.resolve({
        data: { data: { paymentOrderId: 'po1', gatewayPublicKey: 'key', gatewayOrderId: 'gateway-order' } },
      });
      if (url.includes('/verify')) return Promise.reject({ response: { data: { message: 'Verify failed' } } });
      return Promise.resolve({ data: { data: {} } });
    });

    fireEvent.click(screen.getByText('Subscribe'));
    await waitFor(() => expect(screen.getByText('Verify failed')).toBeInTheDocument());
    delete window.Razorpay;
  });
});
