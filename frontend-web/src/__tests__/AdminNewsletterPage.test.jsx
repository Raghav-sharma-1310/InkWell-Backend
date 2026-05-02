/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AdminNewsletterPage } from '../pages/admin/AdminNewsletterPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    patch: vi.fn(() => Promise.resolve({ data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

const mockSubscriber = { subscriberId: 's1', email: 'test@example.com', status: 'ACTIVE' };

// Defines admin newsletter page so related behavior stays grouped in one place.
describe('AdminNewsletterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.confirm = vi.fn(() => true);
    window.alert = vi.fn();
  });

  // Verifies renders heading so regressions are caught during automated tests.
  it('renders heading', async () => {
    render(<MemoryRouter><AdminNewsletterPage /></MemoryRouter>);
    expect(screen.getByText('Newsletter')).toBeInTheDocument();
  });

  // Defines displays active subscribers so related behavior stays grouped in one place.
  it('displays active subscribers', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockSubscriber, { subscriberId: 's2', email: 'unsub@example.com', status: 'UNSUBSCRIBED' }] } });
    render(<MemoryRouter><AdminNewsletterPage /></MemoryRouter>);
    await waitFor(() => {
      expect(screen.getByText('test@example.com')).toBeInTheDocument();
      expect(screen.queryByText('unsub@example.com')).not.toBeInTheDocument(); // Unsubscribed are filtered
    });
  });

  // Defines shows no subscribers message so related behavior stays grouped in one place.
  it('shows no subscribers message', async () => {
    render(<MemoryRouter><AdminNewsletterPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No subscribers yet.')).toBeInTheDocument());
  });

  // Performs the sends campaign successfully workflow so callers do not duplicate this logic.
  it('sends campaign successfully', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [] } });
    api.post.mockResolvedValueOnce({ data: { message: 'Campaign sent successfully!' } });
    
    render(<MemoryRouter><AdminNewsletterPage /></MemoryRouter>);
    
    const subjectInput = screen.getByPlaceholderText('Campaign subject');
    const contentInput = screen.getByPlaceholderText('Campaign content');
    const sendButton = screen.getByText('Send Campaign');
    
    fireEvent.change(subjectInput, { target: { value: 'Subject' } });
    fireEvent.change(contentInput, { target: { value: 'Content' } });
    fireEvent.click(sendButton);
    
    await waitFor(() => expect(screen.getByText('Campaign sent successfully!')).toBeInTheDocument());
    expect(api.post).toHaveBeenCalledWith('/api/newsletter/admin/campaigns', { subject: 'Subject', content: 'Content' });
  });

  // Performs the does not send campaign if empty workflow so callers do not duplicate this logic.
  it('does not send campaign if empty', async () => {
    render(<MemoryRouter><AdminNewsletterPage /></MemoryRouter>);
    
    const sendButton = screen.getByText('Send Campaign');
    fireEvent.click(sendButton);
    
    expect(api.post).not.toHaveBeenCalled();
  });

  // Verifies handles campaign send failure so regressions are caught during automated tests.
  it('handles campaign send failure', async () => {
    api.post.mockRejectedValueOnce({ response: { data: { message: 'Failed to send campaign.' } } });
    
    render(<MemoryRouter><AdminNewsletterPage /></MemoryRouter>);
    
    const subjectInput = screen.getByPlaceholderText('Campaign subject');
    const contentInput = screen.getByPlaceholderText('Campaign content');
    const sendButton = screen.getByText('Send Campaign');
    
    fireEvent.change(subjectInput, { target: { value: 'Subject' } });
    fireEvent.change(contentInput, { target: { value: 'Content' } });
    fireEvent.click(sendButton);
    
    await waitFor(() => expect(screen.getByText('Failed to send campaign.')).toBeInTheDocument());
  });

  // Defines unsubscribes user successfully so related behavior stays grouped in one place.
  it('unsubscribes user successfully', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockSubscriber] } });
    api.patch.mockResolvedValueOnce({});
    
    render(<MemoryRouter><AdminNewsletterPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('test@example.com')).toBeInTheDocument());
    
    const unsubButton = screen.getByTitle('Unsubscribe user');
    fireEvent.click(unsubButton);
    
    expect(window.confirm).toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByText('test@example.com')).not.toBeInTheDocument());
    expect(api.patch).toHaveBeenCalledWith('/api/newsletter/admin/subscribers/s1');
  });

  // Defines does not unsubscribe if confirm cancelled so related behavior stays grouped in one place.
  it('does not unsubscribe if confirm cancelled', async () => {
    window.confirm.mockReturnValueOnce(false);
    api.get.mockResolvedValueOnce({ data: { data: [mockSubscriber] } });
    
    render(<MemoryRouter><AdminNewsletterPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('test@example.com')).toBeInTheDocument());
    
    const unsubButton = screen.getByTitle('Unsubscribe user');
    fireEvent.click(unsubButton);
    
    expect(api.patch).not.toHaveBeenCalled();
  });

  // Verifies handles unsubscribe failure so regressions are caught during automated tests.
  it('handles unsubscribe failure', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockSubscriber] } });
    api.patch.mockRejectedValueOnce(new Error('fail'));
    
    render(<MemoryRouter><AdminNewsletterPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('test@example.com')).toBeInTheDocument());
    
    const unsubButton = screen.getByTitle('Unsubscribe user');
    fireEvent.click(unsubButton);
    
    await waitFor(() => expect(window.alert).toHaveBeenCalledWith('Failed to unsubscribe user.'));
  });
});
