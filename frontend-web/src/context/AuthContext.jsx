/*
 * This file provides shared React state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

// Provides auth provider wiring so the framework can apply the expected runtime behavior.
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const raw = localStorage.getItem('inkwell.user');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  });
  const [loading, setLoading] = useState(false);

  // Persist user to localStorage whenever it changes
  useEffect(() => {
    if (user) {
      localStorage.setItem('inkwell.user', JSON.stringify(user));
    } else {
      localStorage.removeItem('inkwell.user');
    }
  }, [user]);

  const login = useCallback(async (payload) => {
    setLoading(true);
    try {
      const response = await api.post('/api/auth/login', payload);
      const auth = response.data.data;
      localStorage.setItem('inkwell.accessToken', auth.accessToken);
      localStorage.setItem('inkwell.refreshToken', auth.refreshToken);
      setUser(auth.user);
      return auth.user;
    } finally {
      setLoading(false);
    }
  }, []);

  const register = useCallback(async (payload) => {
    setLoading(true);
    try {
      const response = await api.post('/api/auth/register', payload);
      const auth = response.data.data;
      localStorage.setItem('inkwell.accessToken', auth.accessToken);
      localStorage.setItem('inkwell.refreshToken', auth.refreshToken);
      setUser(auth.user);
      return auth.user;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('inkwell.accessToken');
    localStorage.removeItem('inkwell.refreshToken');
    localStorage.removeItem('inkwell.user');
    setUser(null);
  }, []);

  /**
   * Update tokens and user state directly (e.g., after payment verification
   * returns fresh credentials). This avoids a stale JWT holding old
   * subscription claims.
   */
  const updateAuthState = useCallback((accessToken, refreshToken, userData) => {
    if (accessToken) {
      localStorage.setItem('inkwell.accessToken', accessToken);
    }
    if (refreshToken) {
      localStorage.setItem('inkwell.refreshToken', refreshToken);
    }
    if (userData) {
      setUser(userData);
    }
  }, []);

  /**
   * Reload the current user from /api/auth/me to pick up any
   * server-side changes (subscription, role, profile updates).
   */
  const refreshUser = useCallback(async () => {
    try {
      const res = await api.get('/api/auth/me');
      setUser(res.data.data);
      return res.data.data;
    } catch (e) {
      console.error("Failed to refresh user", e);
      return null;
    }
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, register, logout, setUser, updateAuthState, refreshUser }),
    [user, loading, login, register, logout, updateAuthState, refreshUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// Provides use auth wiring so the framework can apply the expected runtime behavior.
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
