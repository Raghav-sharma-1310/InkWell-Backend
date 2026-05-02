/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AdminFeedbackPage } from '../pages/admin/AdminFeedbackPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

const mockMessage = {
  messageId: 'm1',
  content: 'I found a bug',
  senderName: 'Test User',
  senderRole: 'READER',
  sentAt: '2026-01-01T00:00:00',
};

const mockAdminMessage = {
  messageId: 'm2',
  content: 'We will look into it',
  senderName: 'Admin User',
  senderRole: 'ADMIN',
  sentAt: '2026-01-02T00:00:00',
};

const mockReport = {
  reportId: 'r1',
  fullName: 'Test User',
  username: 'testuser',
  status: 'OPEN',
  createdAt: '2026-01-01T00:00:00',
  pageUrl: '/some-page',
  messages: [mockMessage],
};

const mockResolvedReport = {
  ...mockReport,
  reportId: 'r2',
  status: 'RESOLVED',
  messages: [mockMessage, mockAdminMessage],
};

// Defines admin feedback page so related behavior stays grouped in one place.
describe('AdminFeedbackPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.alert = vi.fn();
    api.get.mockResolvedValue({ data: { data: [mockReport, mockResolvedReport] } });
  });

  // Verifies renders heading and feedback reports so regressions are caught during automated tests.
  it('renders heading and feedback reports', async () => {
    render(<MemoryRouter><AdminFeedbackPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Bug Reports & Feedback')).toBeInTheDocument());
    expect(screen.getAllByText('Test User')[0]).toBeInTheDocument();
  });

  // Provides filters reports by status wiring so the framework can apply the expected runtime behavior.
  it('filters reports by status', async () => {
    render(<MemoryRouter><AdminFeedbackPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText('Test User').length).toBeGreaterThan(0)); // Both reports have same user
    
    fireEvent.click(screen.getByText('Open'));
    
    // Only the OPEN report should be visible, meaning only one 'Test User' should be seen instead of two
    // But since both have Test User, let's check counts
    const openBtn = screen.getByText('Open');
    expect(openBtn).toHaveClass('bg-brand');
    
    // Check that we can't see the RESOLVED status badge
    expect(screen.queryByText(/RESOLVED/)).not.toBeInTheDocument();
  });

  // Defines expands report details so related behavior stays grouped in one place.
  it('expands report details', async () => {
    render(<MemoryRouter><AdminFeedbackPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText('Test User')[0]).toBeInTheDocument());
    
    // Click the first report
    const reportButtons = screen.getAllByRole('button').filter(b => b.textContent.includes('Test User'));
    fireEvent.click(reportButtons[0]);
    
    // Should see messages
    expect(screen.getByText('I found a bug')).toBeInTheDocument();
    expect(screen.getByText(/some-page/)).toBeInTheDocument();
  });

  // Defines changes report status so related behavior stays grouped in one place.
  it('changes report status', async () => {
    render(<MemoryRouter><AdminFeedbackPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText('Test User')[0]).toBeInTheDocument());
    
    // Expand first report
    const reportButtons = screen.getAllByRole('button').filter(b => b.textContent.includes('Test User'));
    fireEvent.click(reportButtons[0]);
    
    await waitFor(() => expect(screen.getByText('IN PROGRESS')).toBeInTheDocument());
    fireEvent.click(screen.getByText('IN PROGRESS'));
    
    expect(api.put).toHaveBeenCalledWith('/api/admin/feedback/r1/status', { status: 'IN_PROGRESS' });
  });

  // Performs the sends reply workflow so callers do not duplicate this logic.
  it('sends reply', async () => {
    render(<MemoryRouter><AdminFeedbackPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText('Test User')[0]).toBeInTheDocument());
    
    // Expand first report
    const reportButtons = screen.getAllByRole('button').filter(b => b.textContent.includes('Test User'));
    fireEvent.click(reportButtons[0]);
    
    await waitFor(() => expect(screen.getByPlaceholderText('Type your reply...')).toBeInTheDocument());
    
    const input = screen.getByPlaceholderText('Type your reply...');
    fireEvent.change(input, { target: { value: 'Fixing this' } });
    
    fireEvent.click(screen.getByText('Reply'));
    
    expect(api.post).toHaveBeenCalledWith('/api/admin/feedback/r1/reply', { message: 'Fixing this' });
  });

  // Verifies handles api failure during reply so regressions are caught during automated tests.
  it('handles API failure during reply', async () => {
    api.post.mockRejectedValueOnce({ response: { data: { message: 'Failed to reply' } } });
    
    render(<MemoryRouter><AdminFeedbackPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getAllByText('Test User')[0]).toBeInTheDocument());
    
    const reportButtons = screen.getAllByRole('button').filter(b => b.textContent.includes('Test User'));
    fireEvent.click(reportButtons[0]);
    
    await waitFor(() => expect(screen.getByPlaceholderText('Type your reply...')).toBeInTheDocument());
    
    const input = screen.getByPlaceholderText('Type your reply...');
    fireEvent.change(input, { target: { value: 'Fixing this' } });
    
    fireEvent.click(screen.getByText('Reply'));
    
    await waitFor(() => expect(window.alert).toHaveBeenCalledWith('Failed to reply'));
  });

  // Verifies handles api failure during fetch gracefully so regressions are caught during automated tests.
  it('handles API failure during fetch gracefully', async () => {
    api.get.mockRejectedValueOnce(new Error('fail'));
    
    render(<MemoryRouter><AdminFeedbackPage /></MemoryRouter>);
    
    await waitFor(() => expect(screen.getByText('Bug Reports & Feedback')).toBeInTheDocument());
    // Should show no reports found
    expect(screen.getByText('No all reports found')).toBeInTheDocument();
  });
});
