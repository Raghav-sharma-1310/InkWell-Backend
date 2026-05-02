/*
 * This file provides shared frontend helpers for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
export function resolveDashboardPath(user) {
  if (!user) {
    return '/login';
  }
  if (user.role === 'ADMIN') {
    return '/admin';
  }
  if (user.role === 'AUTHOR') {
    return '/author';
  }
  return '/profile';
}

// Defines resolve dashboard label so related behavior stays grouped in one place.
export function resolveDashboardLabel(user) {
  if (!user) {
    return 'Login';
  }
  if (user.role === 'ADMIN') {
    return 'Admin Console';
  }
  if (user.role === 'AUTHOR') {
    return 'Author Studio';
  }
  return 'My Dashboard';
}
