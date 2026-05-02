/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { LoadingSpinner, PageLoader, EmptyState } from '../components/ui/LoadingSpinner';

// Performs the loading spinner workflow so callers do not duplicate this logic.
describe('LoadingSpinner', () => {
  // Verifies renders with default md size so regressions are caught during automated tests.
  it('renders with default md size', () => {
    const { container } = render(<LoadingSpinner />);
    const spinner = container.querySelector('.animate-spin');
    expect(spinner).not.toBeNull();
    expect(spinner.className).toContain('h-6');
  });

  // Verifies renders with sm size so regressions are caught during automated tests.
  it('renders with sm size', () => {
    const { container } = render(<LoadingSpinner size="sm" />);
    const spinner = container.querySelector('.animate-spin');
    expect(spinner.className).toContain('h-4');
  });

  // Verifies renders with lg size so regressions are caught during automated tests.
  it('renders with lg size', () => {
    const { container } = render(<LoadingSpinner size="lg" />);
    const spinner = container.querySelector('.animate-spin');
    expect(spinner.className).toContain('h-10');
  });

  // Defines applies custom class name so related behavior stays grouped in one place.
  it('applies custom className', () => {
    const { container } = render(<LoadingSpinner className="custom" />);
    expect(container.firstChild.className).toContain('custom');
  });
});

// Performs the page loader workflow so callers do not duplicate this logic.
describe('PageLoader', () => {
  // Verifies renders loading text so regressions are caught during automated tests.
  it('renders loading text', () => {
    render(<PageLoader />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  // Verifies renders a spinner so regressions are caught during automated tests.
  it('renders a spinner', () => {
    const { container } = render(<PageLoader />);
    expect(container.querySelector('.animate-spin')).not.toBeNull();
  });
});

// Defines empty state so related behavior stays grouped in one place.
describe('EmptyState', () => {
  // Verifies renders title so regressions are caught during automated tests.
  it('renders title', () => {
    render(<EmptyState title="Nothing here" />);
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
  });

  // Verifies renders description when provided so regressions are caught during automated tests.
  it('renders description when provided', () => {
    render(<EmptyState title="Empty" description="Try something else" />);
    expect(screen.getByText('Try something else')).toBeInTheDocument();
  });

  // Defines does not render description when not provided so related behavior stays grouped in one place.
  it('does not render description when not provided', () => {
    const { container } = render(<EmptyState title="Empty" />);
    expect(container.querySelectorAll('p').length).toBe(0);
  });

  // Verifies renders icon when provided so regressions are caught during automated tests.
  it('renders icon when provided', () => {
    render(<EmptyState title="Empty" icon={<span data-testid="icon">📦</span>} />);
    expect(screen.getByTestId('icon')).toBeInTheDocument();
  });

  // Defines does not render icon when not provided so related behavior stays grouped in one place.
  it('does not render icon when not provided', () => {
    const { container } = render(<EmptyState title="Empty" />);
    // Only the h3 should be present, no icon div
    expect(container.querySelectorAll('[class*="mx-auto"]').length).toBe(0);
  });
});
