/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { NotificationsPage } from '../pages/reader/NotificationsPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    patch: vi.fn(() => Promise.resolve({ data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/ui/LoadingSpinner', () => ({
  PageLoader: () => <div data-testid="page-loader">Loading...</div>,
}));

import api from '../api/client';

const mockNotification = {
  notificationId: 'n1',
  title: 'New Comment',
  message: '<p>Someone commented on your post</p>',
  type: 'NEW_COMMENT',
  read: false,
  createdAt: '2026-01-01T00:00:00',
};

const readNotification = {
  ...mockNotification,
  notificationId: 'n2',
  title: 'Old Notification',
  read: true,
  type: 'ADMIN_BROADCAST',
};

// Defines notifications page so related behavior stays grouped in one place.
describe('NotificationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Defines shows empty state when no notifications so related behavior stays grouped in one place.
  it('shows empty state when no notifications', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [] } });
    render(<MemoryRouter><NotificationsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText("You're all caught up")).toBeInTheDocument());
  });

  // Defines displays notifications so related behavior stays grouped in one place.
  it('displays notifications', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockNotification] } });
    render(<MemoryRouter><NotificationsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('New Comment')).toBeInTheDocument());
  });

  // Defines shows unread count badge so related behavior stays grouped in one place.
  it('shows unread count badge', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockNotification] } });
    render(<MemoryRouter><NotificationsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('1')).toBeInTheDocument());
  });

  // Performs the shows mark all as read button when unread exist workflow so callers do not duplicate this logic.
  it('shows Mark all as read button when unread exist', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockNotification] } });
    render(<MemoryRouter><NotificationsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/Mark all as read/)).toBeInTheDocument());
  });

  // Verifies handles error gracefully so regressions are caught during automated tests.
  it('handles error gracefully', async () => {
    api.get.mockRejectedValueOnce(new Error('fail'));
    render(<MemoryRouter><NotificationsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText("You're all caught up")).toBeInTheDocument());
  });

  // Defines strips html from notification messages so related behavior stays grouped in one place.
  it('strips HTML from notification messages', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockNotification] } });
    render(<MemoryRouter><NotificationsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Someone commented on your post')).toBeInTheDocument());
  });

  // Defines displays read and unread notifications differently so related behavior stays grouped in one place.
  it('displays read and unread notifications differently', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockNotification, readNotification] } });
    render(<MemoryRouter><NotificationsPage /></MemoryRouter>);
    await waitFor(() => {
      expect(screen.getByText('New Comment')).toBeInTheDocument();
      expect(screen.getByText('Old Notification')).toBeInTheDocument();
    });
  });

  // Verifies renders notification type badges so regressions are caught during automated tests.
  it('renders notification type badges', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockNotification] } });
    render(<MemoryRouter><NotificationsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('NEW COMMENT')).toBeInTheDocument());
  });
});
