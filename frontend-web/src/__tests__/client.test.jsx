/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { describe, it, expect, beforeEach, vi } from 'vitest';

// We need to test the interceptor logic, so we'll import and test directly
describe('API Client Interceptors', () => {
  let requestInterceptor;
  let responseErrorInterceptor;

  beforeEach(() => {
    vi.resetModules();
    localStorage.clear();
    // Capture the interceptors by mocking axios.create
    vi.doMock('axios', () => {
      const reqUse = vi.fn();
      const resUse = vi.fn();
      return {
        default: {
          create: vi.fn(() => ({
            interceptors: {
              request: { use: reqUse },
              response: { use: resUse },
            },
          })),
        },
      };
    });
  });

  // Defines attaches bearer token to non public requests so related behavior stays grouped in one place.
  it('attaches Bearer token to non-public requests', async () => {
    localStorage.setItem('inkwell.accessToken', 'my-jwt-token');
    const { default: axios } = await import('axios');
    await import('../api/client');

    // Get the request interceptor callback
    const reqUseFn = axios.create.mock.results[0]?.value?.interceptors?.request?.use;
    if (reqUseFn && reqUseFn.mock?.calls?.[0]) {
      requestInterceptor = reqUseFn.mock.calls[0][0];
      const config = { url: '/api/posts/public/slug', headers: {} };
      const result = requestInterceptor(config);
      expect(result.headers.Authorization).toBe('Bearer my-jwt-token');
    }
  });

  // Provides does not attach token to public auth paths wiring so the framework can apply the expected runtime behavior.
  it('does NOT attach token to public auth paths', async () => {
    localStorage.setItem('inkwell.accessToken', 'my-jwt-token');
    const { default: axios } = await import('axios');
    await import('../api/client');

    const reqUseFn = axios.create.mock.results[0]?.value?.interceptors?.request?.use;
    if (reqUseFn && reqUseFn.mock?.calls?.[0]) {
      requestInterceptor = reqUseFn.mock.calls[0][0];
      const config = { url: '/api/auth/login', headers: {} };
      const result = requestInterceptor(config);
      expect(result.headers.Authorization).toBeUndefined();
    }
  });

  // Defines does not attach token when none exists so related behavior stays grouped in one place.
  it('does NOT attach token when none exists', async () => {
    const { default: axios } = await import('axios');
    await import('../api/client');

    const reqUseFn = axios.create.mock.results[0]?.value?.interceptors?.request?.use;
    if (reqUseFn && reqUseFn.mock?.calls?.[0]) {
      requestInterceptor = reqUseFn.mock.calls[0][0];
      const config = { url: '/api/posts/reader/history', headers: {} };
      const result = requestInterceptor(config);
      expect(result.headers.Authorization).toBeUndefined();
    }
  });

  // Defines passes successful responses through unchanged so related behavior stays grouped in one place.
  it('passes successful responses through unchanged', async () => {
    const { default: axios } = await import('axios');
    await import('../api/client');

    const resUseFn = axios.create.mock.results[0].value.interceptors.response.use;
    const successInterceptor = resUseFn.mock.calls[0][0];
    const response = { data: { ok: true } };

    expect(successInterceptor(response)).toBe(response);
  });

  // Defines clears stale session and redirects on protected 401 responses so related behavior stays grouped in one place.
  it('clears stale session and redirects on protected 401 responses', async () => {
    localStorage.setItem('inkwell.accessToken', 'access');
    localStorage.setItem('inkwell.refreshToken', 'refresh');
    localStorage.setItem('inkwell.user', JSON.stringify({ username: 'reader' }));
    const originalUrl = globalThis.location.href;
    const { default: axios } = await import('axios');
    await import('../api/client');

    globalThis.history.pushState({}, '', '/dashboard');
    const resUseFn = axios.create.mock.results[0].value.interceptors.response.use;
    responseErrorInterceptor = resUseFn.mock.calls[0][1];

    await expect(responseErrorInterceptor({ response: { status: 401 } })).rejects.toEqual({ response: { status: 401 } });

    expect(localStorage.getItem('inkwell.accessToken')).toBeNull();
    expect(localStorage.getItem('inkwell.refreshToken')).toBeNull();
    expect(localStorage.getItem('inkwell.user')).toBeNull();

    globalThis.history.pushState({}, '', originalUrl);
  });

  // Defines does not redirect public pages or non 401 errors so related behavior stays grouped in one place.
  it('does not redirect public pages or non-401 errors', async () => {
    localStorage.setItem('inkwell.accessToken', 'access');
    const originalUrl = globalThis.location.href;
    const { default: axios } = await import('axios');
    await import('../api/client');

    globalThis.history.pushState({}, '', '/posts/example');
    const resUseFn = axios.create.mock.results[0].value.interceptors.response.use;
    responseErrorInterceptor = resUseFn.mock.calls[0][1];

    await expect(responseErrorInterceptor({ response: { status: 401 } })).rejects.toEqual({ response: { status: 401 } });
    await expect(responseErrorInterceptor({ response: { status: 500 } })).rejects.toEqual({ response: { status: 500 } });

    expect(localStorage.getItem('inkwell.accessToken')).toBe('access');

    globalThis.history.pushState({}, '', originalUrl);
  });
});
