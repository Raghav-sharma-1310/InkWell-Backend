/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { AdminAuditPage } from '../pages/admin/AdminAuditPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

vi.mock('../components/ui/LoadingSpinner', () => ({
  PageLoader: () => <div data-testid="page-loader">Loading...</div>,
}));

import api from '../api/client';

const mockLog = {
  auditId: 'a1',
  source: 'AUTH_SERVICE',
  action: 'USER_LOGIN',
  details: 'User user@example.com logged in successfully',
};

// Defines admin audit page so related behavior stays grouped in one place.
describe('AdminAuditPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // Defines shows empty state so related behavior stays grouped in one place.
  it('shows empty state', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [] } });
    render(<MemoryRouter><AdminAuditPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No audit logs recorded yet.')).toBeInTheDocument());
  });

  // Defines displays audit logs so related behavior stays grouped in one place.
  it('displays audit logs', async () => {
    api.get.mockResolvedValueOnce({ data: { data: [mockLog] } });
    render(<MemoryRouter><AdminAuditPage /></MemoryRouter>);
    await waitFor(() => {
      expect(screen.getByText('USER_LOGIN')).toBeInTheDocument();
      expect(screen.getByText('AUTH_SERVICE')).toBeInTheDocument();
      expect(screen.getByText('User user@example.com logged in successfully')).toBeInTheDocument();
    });
  });

  // Verifies handles fetch error gracefully so regressions are caught during automated tests.
  it('handles fetch error gracefully', async () => {
    api.get.mockRejectedValueOnce(new Error('fail'));
    render(<MemoryRouter><AdminAuditPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('No audit logs recorded yet.')).toBeInTheDocument());
  });
});
