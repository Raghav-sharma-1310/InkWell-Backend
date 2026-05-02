/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AdminAuthorRequestsPage } from '../pages/admin/AdminAuthorRequestsPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

const mockPendingRequest = {
  requestId: 'r1',
  fullName: 'Pending User',
  username: 'pendinguser',
  email: 'pending@example.com',
  status: 'PENDING',
  requestedAt: '2026-01-01T00:00:00',
};

const mockApprovedRequest = {
  requestId: 'r2',
  fullName: 'Approved User',
  username: 'approveduser',
  email: 'approved@example.com',
  status: 'APPROVED',
  requestedAt: '2026-01-02T00:00:00',
  adminRemarks: 'Welcome to the author team!',
};

const mockRejectedRequest = {
  requestId: 'r3',
  fullName: 'Rejected User',
  username: 'rejecteduser',
  email: 'rejected@example.com',
  status: 'REJECTED',
  requestedAt: '2026-01-03T00:00:00',
};

// Provides admin author requests page wiring so the framework can apply the expected runtime behavior.
describe('AdminAuthorRequestsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.alert = vi.fn();
    api.get.mockResolvedValue({ data: { data: [mockPendingRequest, mockApprovedRequest, mockRejectedRequest] } });
  });

  // Verifies renders heading and author requests so regressions are caught during automated tests.
  it('renders heading and author requests', async () => {
    render(<MemoryRouter><AdminAuthorRequestsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Author Requests')).toBeInTheDocument());
    expect(screen.getByText('Pending User')).toBeInTheDocument();
    expect(screen.getByText('Approved User')).toBeInTheDocument();
  });

  // Provides filters requests by status wiring so the framework can apply the expected runtime behavior.
  it('filters requests by status', async () => {
    render(<MemoryRouter><AdminAuthorRequestsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Pending User')).toBeInTheDocument());
    
    const pendingTab = screen.getByText('Pending');
    fireEvent.click(pendingTab);
    
    expect(screen.getByText('Pending User')).toBeInTheDocument();
    expect(screen.queryByText('Approved User')).not.toBeInTheDocument();
    expect(screen.queryByText('Rejected User')).not.toBeInTheDocument();
  });

  // Performs the shows remarks for approved request workflow so callers do not duplicate this logic.
  it('shows remarks for approved request', async () => {
    render(<MemoryRouter><AdminAuthorRequestsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Welcome to the author team!')).toBeInTheDocument());
  });

  // Defines approves a request so related behavior stays grouped in one place.
  it('approves a request', async () => {
    render(<MemoryRouter><AdminAuthorRequestsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Pending User')).toBeInTheDocument());
    
    // There's only one PENDING request, so there should be one Approve button
    const approveButton = screen.getByText('Approve');
    fireEvent.click(approveButton);
    
    expect(api.put).toHaveBeenCalledWith('/api/admin/author-requests/r1/approve', { remarks: '' });
  });

  // Performs the rejects a request with remarks workflow so callers do not duplicate this logic.
  it('rejects a request with remarks', async () => {
    render(<MemoryRouter><AdminAuthorRequestsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Pending User')).toBeInTheDocument());
    
    const remarkInput = screen.getByPlaceholderText('Add optional remarks...');
    fireEvent.change(remarkInput, { target: { value: 'Not enough content' } });
    
    const rejectButton = screen.getByText('Reject');
    fireEvent.click(rejectButton);
    
    expect(api.put).toHaveBeenCalledWith('/api/admin/author-requests/r1/reject', { remarks: 'Not enough content' });
  });

  // Verifies handles api failure during action so regressions are caught during automated tests.
  it('handles API failure during action', async () => {
    api.put.mockRejectedValueOnce({ response: { data: { message: 'Action failed' } } });
    
    render(<MemoryRouter><AdminAuthorRequestsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Pending User')).toBeInTheDocument());
    
    const approveButton = screen.getByText('Approve');
    fireEvent.click(approveButton);
    
    await waitFor(() => expect(window.alert).toHaveBeenCalledWith('Action failed'));
  });

  // Verifies handles empty results gracefully so regressions are caught during automated tests.
  it('handles empty results gracefully', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [] } });
    
    render(<MemoryRouter><AdminAuthorRequestsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No all requests found')).toBeInTheDocument());
  });

  // Verifies handles fetch failure gracefully so regressions are caught during automated tests.
  it('handles fetch failure gracefully', async () => {
    api.get.mockRejectedValueOnce(new Error('fail'));
    
    render(<MemoryRouter><AdminAuthorRequestsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Author Requests')).toBeInTheDocument());
    expect(screen.getByText('No all requests found')).toBeInTheDocument();
  });
});
