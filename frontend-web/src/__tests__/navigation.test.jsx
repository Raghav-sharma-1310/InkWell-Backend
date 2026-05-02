/*
 * This file provides frontend test coverage for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { describe, it, expect } from 'vitest';
import { resolveDashboardPath, resolveDashboardLabel } from '../utils/navigation';

// Defines resolve dashboard path so related behavior stays grouped in one place.
describe('resolveDashboardPath', () => {
  // Performs the returns /login when user is null workflow so callers do not duplicate this logic.
  it('returns /login when user is null', () => {
    expect(resolveDashboardPath(null)).toBe('/login');
  });

  // Performs the returns /login when user is undefined workflow so callers do not duplicate this logic.
  it('returns /login when user is undefined', () => {
    expect(resolveDashboardPath(undefined)).toBe('/login');
  });

  // Defines returns /admin for admin role so related behavior stays grouped in one place.
  it('returns /admin for ADMIN role', () => {
    expect(resolveDashboardPath({ role: 'ADMIN' })).toBe('/admin');
  });

  // Provides returns /author for author role wiring so the framework can apply the expected runtime behavior.
  it('returns /author for AUTHOR role', () => {
    expect(resolveDashboardPath({ role: 'AUTHOR' })).toBe('/author');
  });

  // Defines returns /profile for reader role so related behavior stays grouped in one place.
  it('returns /profile for READER role', () => {
    expect(resolveDashboardPath({ role: 'READER' })).toBe('/profile');
  });

  // Defines returns /profile for any other role so related behavior stays grouped in one place.
  it('returns /profile for any other role', () => {
    expect(resolveDashboardPath({ role: 'OTHER' })).toBe('/profile');
  });
});

// Defines resolve dashboard label so related behavior stays grouped in one place.
describe('resolveDashboardLabel', () => {
  // Performs the returns login when user is null workflow so callers do not duplicate this logic.
  it('returns Login when user is null', () => {
    expect(resolveDashboardLabel(null)).toBe('Login');
  });

  // Performs the returns login when user is undefined workflow so callers do not duplicate this logic.
  it('returns Login when user is undefined', () => {
    expect(resolveDashboardLabel(undefined)).toBe('Login');
  });

  // Defines returns admin console for admin role so related behavior stays grouped in one place.
  it('returns Admin Console for ADMIN role', () => {
    expect(resolveDashboardLabel({ role: 'ADMIN' })).toBe('Admin Console');
  });

  // Provides returns author studio for author role wiring so the framework can apply the expected runtime behavior.
  it('returns Author Studio for AUTHOR role', () => {
    expect(resolveDashboardLabel({ role: 'AUTHOR' })).toBe('Author Studio');
  });

  // Defines returns my dashboard for reader role so related behavior stays grouped in one place.
  it('returns My Dashboard for READER role', () => {
    expect(resolveDashboardLabel({ role: 'READER' })).toBe('My Dashboard');
  });
});
