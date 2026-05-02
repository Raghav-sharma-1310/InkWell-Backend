/*
 * This file provides frontend API communication for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
});

// Attach JWT token to every request — EXCEPT public auth endpoints
api.interceptors.request.use((config) => {
  const publicAuthPaths = [
    '/api/auth/login',
    '/api/auth/register',
    '/api/auth/refresh',
    '/api/auth/forgot-password',
    '/api/auth/verify-otp',
    '/api/auth/reset-password',
  ];
  const isPublicAuth = publicAuthPaths.some((p) => config.url?.startsWith(p));
  if (!isPublicAuth) {
    const accessToken = localStorage.getItem('inkwell.accessToken');
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
  }
  return config;
});

// Handle 401 responses — clear stale session and redirect to login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const currentPath = globalThis.location.pathname;
      // Don't redirect if already on public pages
      const publicPaths = ['/login', '/register', '/forgot-password', '/oauth/success', '/', '/explore', '/search', '/newsletter'];
      const isPublic = publicPaths.includes(currentPath) ||
        currentPath.startsWith('/posts/') ||
        currentPath.startsWith('/categories/') ||
        currentPath.startsWith('/tags/') ||
        currentPath.startsWith('/authors/');

      if (!isPublic) {
        localStorage.removeItem('inkwell.accessToken');
        localStorage.removeItem('inkwell.refreshToken');
        localStorage.removeItem('inkwell.user');
        globalThis.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
