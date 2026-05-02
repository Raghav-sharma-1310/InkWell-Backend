/*
 * This file provides reusable UI behavior for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useState } from 'react';
import { ImageOff } from 'lucide-react';

const FALLBACK = 'data:image/svg+xml,' + encodeURIComponent(
  `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="240" fill="none">
    <rect width="400" height="240" rx="12" fill="#1e293b"/>
    <text x="200" y="120" text-anchor="middle" fill="#475569" font-family="system-ui" font-size="14">No image</text>
  </svg>`
);

// Defines is valid url so related behavior stays grouped in one place.
function isValidUrl(url) {
  if (!url || typeof url !== 'string') return false;
  const trimmed = url.trim();
  return trimmed.startsWith('http://') || trimmed.startsWith('https://') || trimmed.startsWith('/');
}

// Defines post image so related behavior stays grouped in one place.
export function PostImage({ src, alt = '', className = '' }) {
  const [error, setError] = useState(false);
  const validSrc = isValidUrl(src) ? src : null;

  if (!validSrc || error) {
    return (
      <div className={`flex items-center justify-center bg-gradient-to-br from-slate-100 to-slate-200 dark:from-slate-800 dark:to-slate-900 ${className}`}>
        <ImageOff size={24} className="text-slate-400" />
      </div>
    );
  }

  return (
    <img
      src={validSrc}
      alt={alt}
      className={className}
      onError={() => setError(true)}
      loading="lazy"
    />
  );
}

// Defines avatar so related behavior stays grouped in one place.
export function Avatar({ src, name = '', size = 'md', className = '' }) {
  const [error, setError] = useState(false);
  const validSrc = isValidUrl(src) ? src : null;
  const initial = name?.[0]?.toUpperCase() || 'U';

  const sizes = {
    sm: 'h-8 w-8 text-xs',
    md: 'h-10 w-10 text-sm',
    lg: 'h-16 w-16 text-2xl',
    xl: 'h-24 w-24 text-3xl',
  };
  const sizeClass = sizes[size] || sizes.md;

  if (!validSrc || error) {
    return (
      <div className={`flex items-center justify-center rounded-2xl bg-brand-100 font-bold text-brand-700 dark:bg-brand-900 dark:text-brand-300 ${sizeClass} ${className}`}>
        {initial}
      </div>
    );
  }

  return (
    <img
      src={validSrc}
      alt={name}
      className={`rounded-2xl object-cover ${sizeClass} ${className}`}
      onError={() => setError(true)}
      loading="lazy"
    />
  );
}
