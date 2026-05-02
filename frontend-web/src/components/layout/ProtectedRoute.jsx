/*
 * This file provides reusable UI behavior for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
﻿import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

// Handles protected route requests so the UI can call this feature through a stable endpoint.
export function ProtectedRoute({ roles }) {
  const { user } = useAuth();
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
