/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AdminNotificationsPage } from '../pages/admin/AdminNotificationsPage';

vi.mock('../api/client', () => ({
  default: {
    post: vi.fn(() => Promise.resolve({ data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

// Defines admin notifications page so related behavior stays grouped in one place.
describe('AdminNotificationsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Verifies renders heading so regressions are caught during automated tests.
  it('renders heading', () => {
    render(<MemoryRouter><AdminNotificationsPage /></MemoryRouter>);
    expect(screen.getByText('Admin Broadcasts')).toBeInTheDocument();
  });

  // Performs the sends broadcast successfully workflow so callers do not duplicate this logic.
  it('sends broadcast successfully', async () => {
    render(<MemoryRouter><AdminNotificationsPage /></MemoryRouter>);
    
    const titleInput = screen.getByPlaceholderText('Broadcast title');
    const messageInput = screen.getByPlaceholderText('Broadcast message');
    const sendButton = screen.getByText('Send Broadcast');
    
    fireEvent.change(titleInput, { target: { value: 'Test Title' } });
    fireEvent.change(messageInput, { target: { value: 'Test Message' } });
    fireEvent.click(sendButton);
    
    expect(api.post).toHaveBeenCalledWith('/api/notifications/admin/broadcast', { title: 'Test Title', message: 'Test Message' });
    await waitFor(() => expect(screen.getByText('Broadcast sent successfully!')).toBeInTheDocument());
  });

  // Performs the does not send if empty workflow so callers do not duplicate this logic.
  it('does not send if empty', () => {
    render(<MemoryRouter><AdminNotificationsPage /></MemoryRouter>);
    const sendButton = screen.getByText('Send Broadcast');
    fireEvent.click(sendButton);
    expect(api.post).not.toHaveBeenCalled();
  });

  // Verifies handles send failure so regressions are caught during automated tests.
  it('handles send failure', async () => {
    api.post.mockRejectedValueOnce(new Error('fail'));
    render(<MemoryRouter><AdminNotificationsPage /></MemoryRouter>);
    
    const titleInput = screen.getByPlaceholderText('Broadcast title');
    const messageInput = screen.getByPlaceholderText('Broadcast message');
    const sendButton = screen.getByText('Send Broadcast');
    
    fireEvent.change(titleInput, { target: { value: 'Test Title' } });
    fireEvent.change(messageInput, { target: { value: 'Test Message' } });
    fireEvent.click(sendButton);
    
    await waitFor(() => expect(screen.getByText('Failed to send broadcast.')).toBeInTheDocument());
  });
});
