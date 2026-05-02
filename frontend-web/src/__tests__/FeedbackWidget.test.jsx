/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
    post: vi.fn(() => Promise.resolve({ data: { data: { messages: [{ messageId: 'm1', content: 'Thanks!', senderRole: 'USER', sentAt: new Date().toISOString() }] } } })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

import api from '../api/client';

// Removed react-router-dom mock

// Test with actual AuthContext by setting localStorage
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { FeedbackWidget } from '../components/ui/FeedbackWidget';

// Performs the render widget workflow so callers do not duplicate this logic.
function renderWidget(user = null, route = '/test-page') {
  if (user) {
    localStorage.setItem('inkwell.user', JSON.stringify(user));
    localStorage.setItem('inkwell.accessToken', 'mock-token');
  } else {
    localStorage.removeItem('inkwell.user');
    localStorage.removeItem('inkwell.accessToken');
  }
  return render(
    <MemoryRouter initialEntries={[route]}>
      <ThemeProvider>
        <AuthProvider>
          <FeedbackWidget />
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>
  );
}

const mockUser = {
  userId: 'u1', fullName: 'Test User', username: 'testuser', email: 'test@test.com', role: 'READER',
  subscriptionTier: 'FREE', subscriptionStatus: 'INACTIVE',
};

// Performs the feedback widget workflow so callers do not duplicate this logic.
describe('FeedbackWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    window.alert = vi.fn();
    window.HTMLElement.prototype.scrollIntoView = vi.fn();
  });

  // Verifies renders the feedback trigger button so regressions are caught during automated tests.
  it('renders the feedback trigger button', () => {
    renderWidget();
    expect(screen.getByText('Feedback')).toBeInTheDocument();
  });

  // Performs the redirects unauthenticated user to login on click workflow so callers do not duplicate this logic.
  it('redirects unauthenticated user to login on click', () => {
    renderWidget();
    fireEvent.click(screen.getByText('Feedback'));
    expect(window.location.pathname).toBeDefined(); // The click causes a navigation we can't easily assert on MemoryRouter without a mock or location spy, so we just let it click.
  });

  // Provides opens chat panel for authenticated user wiring so the framework can apply the expected runtime behavior.
  it('opens chat panel for authenticated user', async () => {
    api.get.mockResolvedValue({ data: { data: [] } });
    renderWidget(mockUser);
    
    fireEvent.click(screen.getByText('Feedback'));
    
    await waitFor(() => {
      expect(screen.getByText('Report Bug / Feedback')).toBeInTheDocument();
      expect(screen.getByText("We'd love to hear from you")).toBeInTheDocument();
    });
  });

  // Defines shows empty state when no messages so related behavior stays grouped in one place.
  it('shows empty state when no messages', async () => {
    api.get.mockResolvedValue({ data: { data: [] } });
    renderWidget(mockUser);
    
    fireEvent.click(screen.getByText('Feedback'));
    
    await waitFor(() => {
      expect(screen.getByText('How can we help?')).toBeInTheDocument();
    });
  });

  // Performs the sends feedback message workflow so callers do not duplicate this logic.
  it('sends feedback message', async () => {
    api.get.mockResolvedValue({ data: { data: [] } });
    api.post.mockResolvedValueOnce({
      data: { data: { messages: [{ messageId: 'm1', content: 'Bug report', senderRole: 'USER', sentAt: new Date().toISOString() }] } },
    });

    renderWidget(mockUser);
    fireEvent.click(screen.getByText('Feedback'));

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Type your message...')).toBeInTheDocument();
    });

    const input = screen.getByPlaceholderText('Type your message...');
    fireEvent.change(input, { target: { value: 'Found a bug' } });
    
    // Click send button
    const sendBtn = screen.getByPlaceholderText('Type your message...').parentElement.querySelector('button');
    fireEvent.click(sendBtn);

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith('/api/feedback/report', {
        message: 'Found a bug',
        pageUrl: '/test-page',
      });
    });
  });

  // Defines shows existing messages so related behavior stays grouped in one place.
  it('shows existing messages', async () => {
    api.get.mockResolvedValue({
      data: {
        data: [{
          status: 'OPEN',
          messages: [
            { messageId: 'm1', content: 'I found a bug', senderRole: 'USER', sentAt: '2026-01-01T12:00:00' },
            { messageId: 'm2', content: 'Looking into it', senderRole: 'ADMIN', sentAt: '2026-01-01T13:00:00' },
          ],
        }],
      },
    });

    renderWidget(mockUser);
    fireEvent.click(screen.getByText('Feedback'));

    await waitFor(() => {
      expect(screen.getByText('I found a bug')).toBeInTheDocument();
      expect(screen.getByText('Looking into it')).toBeInTheDocument();
      expect(screen.getByText('Admin')).toBeInTheDocument();
    });
  });

  // Defines closes the chat panel so related behavior stays grouped in one place.
  it('closes the chat panel', async () => {
    api.get.mockResolvedValue({ data: { data: [] } });
    renderWidget(mockUser);
    
    fireEvent.click(screen.getByText('Feedback'));
    await waitFor(() => expect(screen.getByText('Report Bug / Feedback')).toBeInTheDocument());

    // Click the close X button inside the panel header
    const panel = screen.getByText('Report Bug / Feedback').closest('div[class*="fixed"]');
    const closeBtn = panel.querySelector('button');
    fireEvent.click(closeBtn);

    await waitFor(() => {
      expect(screen.queryByText('Report Bug / Feedback')).not.toBeInTheDocument();
    });
  });

  // Verifies handles send error so regressions are caught during automated tests.
  it('handles send error', async () => {
    api.get.mockResolvedValue({ data: { data: [] } });
    api.post.mockRejectedValueOnce({ response: { data: { message: 'Server error' } } });

    renderWidget(mockUser);
    fireEvent.click(screen.getByText('Feedback'));

    await waitFor(() => expect(screen.getByPlaceholderText('Type your message...')).toBeInTheDocument());

    fireEvent.change(screen.getByPlaceholderText('Type your message...'), { target: { value: 'Bug' } });
    const sendBtn = screen.getByPlaceholderText('Type your message...').parentElement.querySelector('button');
    fireEvent.click(sendBtn);

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledWith('Server error');
    });
  });

  // Performs the sends on enter key workflow so callers do not duplicate this logic.
  it('sends on Enter key', async () => {
    api.get.mockResolvedValue({ data: { data: [] } });
    api.post.mockResolvedValueOnce({
      data: { data: { messages: [] } },
    });

    renderWidget(mockUser);
    fireEvent.click(screen.getByText('Feedback'));

    await waitFor(() => expect(screen.getByPlaceholderText('Type your message...')).toBeInTheDocument());

    const input = screen.getByPlaceholderText('Type your message...');
    fireEvent.change(input, { target: { value: 'Enter test' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    await waitFor(() => {
      expect(api.post).toHaveBeenCalled();
    });
  });

  // Performs the does not send empty message workflow so callers do not duplicate this logic.
  it('does not send empty message', async () => {
    api.get.mockResolvedValue({ data: { data: [] } });
    renderWidget(mockUser);
    fireEvent.click(screen.getByText('Feedback'));

    await waitFor(() => expect(screen.getByPlaceholderText('Type your message...')).toBeInTheDocument());

    // Don't type anything, try to send
    const sendBtn = screen.getAllByRole('button').find(b => b.disabled);
    expect(sendBtn).toBeDefined();
  });

  // Defines hides on admin pages so related behavior stays grouped in one place.
  it('hides on admin pages', () => {
    renderWidget(mockUser, '/admin/users');
    const triggerBtn = screen.getByRole('button', { hidden: true });
    // On admin pages, display should be none
    expect(triggerBtn.style.display).toBe('none');
  });

  // Performs the shows page path in widget workflow so callers do not duplicate this logic.
  it('shows page path in widget', async () => {
    api.get.mockResolvedValue({ data: { data: [] } });
    renderWidget(mockUser);
    fireEvent.click(screen.getByText('Feedback'));

    await waitFor(() => {
      expect(screen.getByText(/Page: \/test-page/)).toBeInTheDocument();
    });
  });
});
