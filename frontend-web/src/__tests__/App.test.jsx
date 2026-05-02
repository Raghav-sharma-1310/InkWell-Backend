/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import App from '../App';
import { MainLayout } from '../components/layout/MainLayout';
import { AuthProvider } from '../context/AuthContext';
import { ThemeProvider } from '../context/ThemeContext';
import { NotificationProvider } from '../context/NotificationContext';
import { RegisterPage } from '../pages/public/RegisterPage';
import { TagPage } from '../pages/public/TagPage';

vi.mock('../api/client', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
    post: vi.fn(() => Promise.resolve({ data: { data: {} } })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
  },
}));

// Defines render with providers so related behavior stays grouped in one place.
function renderWithProviders(children, initialEntries = ['/']) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>{children}</NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </MemoryRouter>,
  );
}

// Defines main layout so related behavior stays grouped in one place.
describe('MainLayout', () => {
  // Verifies renders brand name so regressions are caught during automated tests.
  it('renders brand name', () => {
    renderWithProviders(<MainLayout />);

    expect(screen.getByText('InkWell')).toBeInTheDocument();
  });

  // Verifies renders app route configuration so regressions are caught during automated tests.
  it('renders App route configuration', () => {
    renderWithProviders(<App />, ['/explore']);

    expect(screen.getByText('InkWell')).toBeInTheDocument();
  });

  // Defines exposes public page re export shims so related behavior stays grouped in one place.
  it('exposes public page re-export shims', () => {
    expect(RegisterPage).toBeDefined();
    expect(TagPage).toBeDefined();
  });
});
