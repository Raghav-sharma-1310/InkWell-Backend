/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { render, screen, fireEvent, act } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ThemeProvider, useTheme } from '../context/ThemeContext';

// Verifies test consumer so regressions are caught during automated tests.
function TestConsumer() {
  const { theme, toggleTheme } = useTheme();
  return (
    <div>
      <div data-testid="theme">{theme}</div>
      <button data-testid="toggle" onClick={toggleTheme}>Toggle</button>
    </div>
  );
}

// Defines theme context so related behavior stays grouped in one place.
describe('ThemeContext', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
  });

  // Defines defaults to light when no preference so related behavior stays grouped in one place.
  it('defaults to light when no preference', () => {
    render(<ThemeProvider><TestConsumer /></ThemeProvider>);
    expect(screen.getByTestId('theme').textContent).toBe('light');
  });

  // Defines restores theme from local storage so related behavior stays grouped in one place.
  it('restores theme from localStorage', () => {
    localStorage.setItem('inkwell.theme', 'dark');
    render(<ThemeProvider><TestConsumer /></ThemeProvider>);
    expect(screen.getByTestId('theme').textContent).toBe('dark');
  });

  // Defines toggles theme from light to dark so related behavior stays grouped in one place.
  it('toggles theme from light to dark', () => {
    render(<ThemeProvider><TestConsumer /></ThemeProvider>);
    fireEvent.click(screen.getByTestId('toggle'));
    expect(screen.getByTestId('theme').textContent).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  // Defines toggles theme from dark to light so related behavior stays grouped in one place.
  it('toggles theme from dark to light', () => {
    localStorage.setItem('inkwell.theme', 'dark');
    render(<ThemeProvider><TestConsumer /></ThemeProvider>);
    fireEvent.click(screen.getByTestId('toggle'));
    expect(screen.getByTestId('theme').textContent).toBe('light');
  });

  // Defines persists theme to local storage so related behavior stays grouped in one place.
  it('persists theme to localStorage', () => {
    render(<ThemeProvider><TestConsumer /></ThemeProvider>);
    fireEvent.click(screen.getByTestId('toggle'));
    expect(localStorage.getItem('inkwell.theme')).toBe('dark');
  });

  // Defines adds dark class to document when theme is dark so related behavior stays grouped in one place.
  it('adds dark class to document when theme is dark', () => {
    localStorage.setItem('inkwell.theme', 'dark');
    render(<ThemeProvider><TestConsumer /></ThemeProvider>);
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  // Defines removes dark class from document when theme is light so related behavior stays grouped in one place.
  it('removes dark class from document when theme is light', () => {
    document.documentElement.classList.add('dark');
    localStorage.setItem('inkwell.theme', 'light');
    render(<ThemeProvider><TestConsumer /></ThemeProvider>);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  // Defines throws error when use theme used outside provider so related behavior stays grouped in one place.
  it('throws error when useTheme used outside provider', () => {
    expect(() => render(<TestConsumer />)).toThrow('useTheme must be used within ThemeProvider');
  });
});
