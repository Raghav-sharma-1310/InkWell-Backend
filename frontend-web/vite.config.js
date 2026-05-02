/*
 * This file provides frontend application configuration and wiring for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: '0.0.0.0',
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/setupTests.js',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      reportsDirectory: './coverage',
      include: ['src/**/*.{js,jsx}'],
      exclude: ['src/**/*.test.{js,jsx}', 'src/setupTests.js', 'src/main.jsx', 'src/__tests__/testHelpers.jsx']
    }
  },
});
