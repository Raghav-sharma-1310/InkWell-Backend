/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { Bell, MessageCircle, AlertCircle, Info, CheckCircle2 } from 'lucide-react';
import api from '../../api/client';
import { PageLoader } from '../../components/ui/LoadingSpinner';

// Defines strip html so related behavior stays grouped in one place.
const stripHtml = (html) => {
  if (!html) return '';
  const tmp = document.createElement("DIV");
  tmp.innerHTML = html;
  return tmp.textContent || tmp.innerText || "";
};

// Performs the get icon for type workflow so callers do not duplicate this logic.
const getIconForType = (type) => {
  switch (type) {
    case 'NEW_COMMENT':
    case 'COMMENT_REPLY':
      return <MessageCircle size={18} className="text-blue-500" />;
    case 'ADMIN_BROADCAST':
      return <AlertCircle size={18} className="text-amber-500" />;
    default:
      return <Info size={18} className="text-brand" />;
  }
};

// Defines notifications page so related behavior stays grouped in one place.
export function NotificationsPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  // Performs the fetch notifications workflow so callers do not duplicate this logic.
  const fetchNotifications = () => {
    api.get('/api/notifications')
      .then((r) => setItems(r.data.data))
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  // Performs the mark as read workflow so callers do not duplicate this logic.
  const markAsRead = async (id) => {
    try {
      await api.patch(`/api/notifications/${id}/read`);
      setItems(items.map(item => item.notificationId === id ? { ...item, read: true } : item));
    } catch (err) {
      console.error(err);
    }
  };

  // Performs the mark all as read workflow so callers do not duplicate this logic.
  const markAllAsRead = async () => {
    try {
      await api.patch('/api/notifications/read-all');
      setItems(items.map(item => ({ ...item, read: true })));
    } catch (err) {
      console.error(err);
    }
  };

  if (loading) return <PageLoader />;

  const unreadCount = items.filter(i => !i.read).length;

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 md:px-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-6 dark:border-slate-800">
        <div>
          <p className="page-heading">Inbox</p>
          <div className="mt-2 flex items-center gap-3">
            <h1 className="font-display text-3xl font-bold">Notifications</h1>
            {unreadCount > 0 && (
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-brand text-xs font-bold text-white">
                {unreadCount}
              </span>
            )}
          </div>
        </div>
        {unreadCount > 0 && (
          <button onClick={markAllAsRead} className="btn-ghost text-sm flex items-center gap-2 text-brand">
            <CheckCircle2 size={16} /> Mark all as read
          </button>
        )}
      </div>

      <div className="mt-8 space-y-4">
        {items.map((item) => {
          const cleanMessage = stripHtml(item.message);
          return (
            <div 
              key={item.notificationId} 
              onClick={() => !item.read && markAsRead(item.notificationId)}
              className={`card relative overflow-hidden transition-all duration-200 cursor-default ${
                !item.read 
                  ? 'border-brand-200 bg-brand-50/30 dark:border-brand-800/50 dark:bg-brand-950/20' 
                  : 'bg-white dark:bg-slate-900'
              }`}
            >
              {!item.read && (
                <div className="absolute left-0 top-0 bottom-0 w-1 bg-brand" />
              )}
              <div className="flex items-start gap-4 p-5 sm:p-6">
                <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${!item.read ? 'bg-brand-100 dark:bg-brand-900/40' : 'bg-slate-100 dark:bg-slate-800'}`}>
                  {getIconForType(item.type)}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <p className={`font-semibold text-base ${!item.read ? 'text-slate-900 dark:text-white' : 'text-slate-700 dark:text-slate-300'}`}>
                      {item.title}
                    </p>
                    <span className="shrink-0 text-xs font-medium text-slate-400">
                      {new Date(item.createdAt).toLocaleString(undefined, {
                        month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit'
                      })}
                    </span>
                  </div>
                  <div className="mt-1.5 flex items-center gap-2">
                    <span className="inline-flex rounded bg-slate-100 px-2 py-0.5 text-[10px] font-bold tracking-wider text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                      {item.type.replace(/_/g, ' ')}
                    </span>
                  </div>
                  <p className={`mt-3 text-sm leading-relaxed ${!item.read ? 'text-slate-700 dark:text-slate-300' : 'text-slate-500 dark:text-slate-400'}`}>
                    {cleanMessage}
                  </p>
                </div>
              </div>
            </div>
          );
        })}
        {items.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-slate-50 dark:bg-slate-800/50">
              <Bell size={24} className="text-slate-400" />
            </div>
            <p className="mt-4 text-base font-medium text-slate-600 dark:text-slate-300">You're all caught up</p>
            <p className="mt-1 text-sm text-slate-500">No new notifications right now.</p>
          </div>
        )}
      </div>
    </div>
  );
}
