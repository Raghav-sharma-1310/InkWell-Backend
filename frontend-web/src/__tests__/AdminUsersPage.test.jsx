/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AdminUsersPage } from '../pages/admin/AdminUsersPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
    patch: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({})),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/ui/LoadingSpinner', () => ({
  PageLoader: () => <div data-testid="page-loader">Loading...</div>,
}));

import api from '../api/client';

const mockUser = {
  userId: 'u1',
  fullName: 'Test User',
  email: 'test@example.com',
  username: 'testuser',
  role: 'READER',
  active: true,
  subscriptionTier: 'FREE',
  subscriptionStatus: 'INACTIVE',
};

const mockAdmin = {
  ...mockUser,
  userId: 'u2',
  fullName: 'Admin User',
  email: 'admin@example.com',
  role: 'ADMIN',
};

const suspendedAuthor = {
  ...mockUser,
  userId: 'u3',
  fullName: 'Suspended Author',
  email: 'author@example.com',
  role: 'AUTHOR',
  active: false,
};

const proUser = {
  ...mockUser,
  userId: 'u4',
  fullName: 'Pro User',
  subscriptionTier: 'PRO',
  subscriptionStatus: 'ACTIVE',
};

// Defines admin users page so related behavior stays grouped in one place.
describe('AdminUsersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.confirm = vi.fn(() => true);
    
    api.get.mockResolvedValue({ data: { data: [mockUser, mockAdmin, suspendedAuthor, proUser] } });
  });

  // Verifies renders heading and stats so regressions are caught during automated tests.
  it('renders heading and stats', async () => {
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Manage Users')).toBeInTheDocument());
    expect(screen.getByText('4 total · 3 active · 1 suspended')).toBeInTheDocument();
  });

  // Verifies renders users in the table so regressions are caught during automated tests.
  it('renders users in the table', async () => {
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => {
      expect(screen.getByText('Test User')).toBeInTheDocument();
      expect(screen.getByText('Admin User')).toBeInTheDocument();
      expect(screen.getByText('Suspended Author')).toBeInTheDocument();
    });
    expect(screen.getByText('PRO')).toBeInTheDocument(); // Pro user badge
  });

  // Provides filters users by search wiring so the framework can apply the expected runtime behavior.
  it('filters users by search', async () => {
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test User')).toBeInTheDocument());
    
    const searchInput = screen.getByPlaceholderText(/Search users/);
    fireEvent.change(searchInput, { target: { value: 'Admin' } });
    
    expect(screen.getByText('Admin User')).toBeInTheDocument();
    expect(screen.queryByText('Test User')).not.toBeInTheDocument();
  });

  // Defines shows no users message when search has no matches so related behavior stays grouped in one place.
  it('shows no users message when search has no matches', async () => {
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test User')).toBeInTheDocument());
    
    const searchInput = screen.getByPlaceholderText(/Search users/);
    fireEvent.change(searchInput, { target: { value: 'nonexistent' } });
    
    expect(screen.getByText('No users found matching your search.')).toBeInTheDocument();
  });

  // Provides promotes reader to author wiring so the framework can apply the expected runtime behavior.
  it('promotes reader to author', async () => {
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test User')).toBeInTheDocument());
    
    const promoteButtons = screen.getAllByTitle('Promote to Author');
    fireEvent.click(promoteButtons[0]);
    
    expect(api.patch).toHaveBeenCalledWith('/api/auth/admin/users/u1/role', { role: 'AUTHOR' });
  });

  // Provides demotes author to reader wiring so the framework can apply the expected runtime behavior.
  it('demotes author to reader', async () => {
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Suspended Author')).toBeInTheDocument());
    
    const demoteButton = screen.getByTitle('Demote to Reader');
    fireEvent.click(demoteButton);
    
    expect(api.patch).toHaveBeenCalledWith('/api/auth/admin/users/u3/role', { role: 'READER' });
  });

  // Defines makes user an admin so related behavior stays grouped in one place.
  it('makes user an admin', async () => {
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test User')).toBeInTheDocument());
    
    // Test user is the first non-admin, Make Admin is the second button
    const makeAdminButtons = screen.getAllByTitle('Make Admin');
    fireEvent.click(makeAdminButtons[0]);
    
    expect(api.patch).toHaveBeenCalledWith('/api/auth/admin/users/u1/role', { role: 'ADMIN' });
  });

  // Defines suspends active user so related behavior stays grouped in one place.
  it('suspends active user', async () => {
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test User')).toBeInTheDocument());
    
    const suspendButtons = screen.getAllByTitle('Suspend');
    fireEvent.click(suspendButtons[0]);
    
    expect(api.patch).toHaveBeenCalledWith('/api/auth/admin/users/u1/suspend');
  });

  // Defines reactivates suspended user so related behavior stays grouped in one place.
  it('reactivates suspended user', async () => {
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Suspended Author')).toBeInTheDocument());
    
    const reactivateButton = screen.getByTitle('Reactivate');
    fireEvent.click(reactivateButton);
    
    expect(api.patch).toHaveBeenCalledWith('/api/auth/admin/users/u3/reactivate');
  });

  // Performs the deletes user after confirmation workflow so callers do not duplicate this logic.
  it('deletes user after confirmation', async () => {
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test User')).toBeInTheDocument());
    
    const deleteButtons = screen.getAllByTitle('Delete');
    fireEvent.click(deleteButtons[0]);
    
    expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to permanently delete test@example.com?');
    expect(api.delete).toHaveBeenCalledWith('/api/auth/admin/users/u1');
  });

  // Performs the does not delete user if confirmation cancelled workflow so callers do not duplicate this logic.
  it('does not delete user if confirmation cancelled', async () => {
    window.confirm.mockReturnValueOnce(false);
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Test User')).toBeInTheDocument());
    
    const deleteButtons = screen.getAllByTitle('Delete');
    fireEvent.click(deleteButtons[0]);
    
    expect(api.delete).not.toHaveBeenCalled();
  });

  // Verifies handles fetch errors gracefully so regressions are caught during automated tests.
  it('handles fetch errors gracefully', async () => {
    api.get.mockRejectedValueOnce(new Error('fail'));
    render(<MemoryRouter><AdminUsersPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Manage Users')).toBeInTheDocument());
    expect(screen.getByText('0 total · 0 active · 0 suspended')).toBeInTheDocument();
  });
});
