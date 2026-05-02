/*
 * This file provides shared React state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
﻿import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import api from '../api/client';
import { useAuth } from './AuthContext';

const NotificationContext = createContext(null);

// Defines notification provider so related behavior stays grouped in one place.
export function NotificationProvider({ children }) {
  const { user } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (!user) {
      setUnreadCount(0);
      return;
    }
    api.get('/api/notifications/unread-count').then((response) => setUnreadCount(response.data.data)).catch(() => setUnreadCount(0));
  }, [user]);

  const value = useMemo(() => ({ unreadCount, setUnreadCount }), [unreadCount]);
  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

// Defines use notifications so related behavior stays grouped in one place.
export function useNotifications() {
  return useContext(NotificationContext);
}
