/*
 * This file provides frontend application configuration and wiring for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f0fdfa',
          100: '#ccfbf1',
          200: '#99f6e4',
          300: '#5eead4',
          400: '#2dd4bf',
          500: '#14b8a6',
          600: '#0d9488',
          700: '#0f766e',
          800: '#115e59',
          900: '#134e4a',
          950: '#042f2e',
          DEFAULT: '#0f766e',
        },
        coral: {
          50: '#fff7ed',
          400: '#fb923c',
          500: '#f97316',
          600: '#ea580c',
          DEFAULT: '#f97316',
        },
        surface: {
          light: '#ffffff',
          dark: '#0f172a',
        },
      },
      boxShadow: {
        soft: '0 4px 24px -2px rgba(15, 23, 42, 0.06), 0 2px 8px -2px rgba(15, 23, 42, 0.04)',
        glow: '0 8px 40px -4px rgba(15, 23, 42, 0.12)',
        'brand-glow': '0 4px 20px rgba(15, 118, 110, 0.25)',
      },
      fontFamily: {
        display: ['Outfit', 'system-ui', 'sans-serif'],
        body: ['Inter', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        '4xl': '2rem',
        '5xl': '2.5rem',
      },
      keyframes: {
        fadeInUp: {
          '0%': { opacity: '0', transform: 'translateY(16px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideIn: {
          '0%': { opacity: '0', transform: 'translateX(-8px)' },
          '100%': { opacity: '1', transform: 'translateX(0)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        spin: {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        },
      },
      animation: {
        'fade-in-up': 'fadeInUp 0.5s ease-out both',
        'fade-in': 'fadeIn 0.4s ease-out both',
        'slide-in': 'slideIn 0.3s ease-out both',
        shimmer: 'shimmer 2s infinite linear',
        spin: 'spin 1s linear infinite',
      },
    },
  },
  plugins: [],
};
