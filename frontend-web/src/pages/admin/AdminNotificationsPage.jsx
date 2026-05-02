/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useState } from 'react';
import { Megaphone, Send } from 'lucide-react';
import api from '../../api/client';

// Defines admin notifications page so related behavior stays grouped in one place.
export function AdminNotificationsPage() {
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [feedback, setFeedback] = useState('');

  // Performs the send workflow so callers do not duplicate this logic.
  const send = async () => {
    if (!title.trim() || !message.trim()) return;
    setSending(true);
    try {
      await api.post('/api/notifications/admin/broadcast', { title, message });
      setTitle('');
      setMessage('');
      setFeedback('Broadcast sent successfully!');
    } catch {
      setFeedback('Failed to send broadcast.');
    } finally {
      setSending(false);
    }
  };

  return (
    <div>
      <h1 className="font-display text-3xl font-bold">Admin Broadcasts</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Send notifications to all platform users.</p>

      <div className="mt-8 space-y-4">
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Broadcast title" className="input-field" />
        <textarea value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Broadcast message" className="input-field min-h-[160px] resize-y" />
        <button onClick={send} disabled={sending} className="btn-primary">
          <Megaphone size={16} /> {sending ? 'Sending...' : 'Send Broadcast'}
        </button>
        {feedback && <p className="text-sm text-brand dark:text-brand-400">{feedback}</p>}
      </div>
    </div>
  );
}
