/*
 * This file provides shared React state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react';

const ThemeContext = createContext(null);

// Defines theme provider so related behavior stays grouped in one place.
export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => {
    const stored = localStorage.getItem('inkwell.theme');
    if (stored) return stored;
    // Respect system preference on first visit
    if (window.matchMedia?.('(prefers-color-scheme: dark)').matches) return 'dark';
    return 'light';
  });

  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
    localStorage.setItem('inkwell.theme', theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme((current) => (current === 'dark' ? 'light' : 'dark'));
  }, []);

  const value = useMemo(() => ({ theme, toggleTheme }), [theme, toggleTheme]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

// Defines use theme so related behavior stays grouped in one place.
export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used within ThemeProvider');
  return context;
}
