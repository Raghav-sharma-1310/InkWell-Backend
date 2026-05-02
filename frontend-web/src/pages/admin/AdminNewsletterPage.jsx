/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { Send, Mail, UserMinus } from 'lucide-react';
import api from '../../api/client';

// Defines admin newsletter page so related behavior stays grouped in one place.
export function AdminNewsletterPage() {
  const [subject, setSubject] = useState('');
  const [content, setContent] = useState('');
  const [subscribers, setSubscribers] = useState([]);
  const [sending, setSending] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    api.get('/api/newsletter/admin/subscribers')
      .then((r) => setSubscribers(r.data.data.filter(s => s.status !== 'UNSUBSCRIBED')))
      .catch(() => setSubscribers([]));
  }, []);

  // Performs the send campaign workflow so callers do not duplicate this logic.
  const sendCampaign = async () => {
    if (!subject.trim() || !content.trim()) return;
    setSending(true);
    try {
      const response = await api.post('/api/newsletter/admin/campaigns', { subject, content });
      setSubject('');
      setContent('');
      setMessage(response.data.message || 'Campaign sent successfully!');
    } catch (e) {
      setMessage(e.response?.data?.message || 'Failed to send campaign.');
    } finally {
      setSending(false);
    }
  };

  // Defines unsubscribe user so related behavior stays grouped in one place.
  const unsubscribeUser = async (id) => {
    if (!window.confirm('Are you sure you want to unsubscribe this user?')) return;
    try {
      await api.patch(`/api/newsletter/admin/subscribers/${id}`);
      setSubscribers(subscribers.filter(s => s.subscriberId !== id));
    } catch {
      alert('Failed to unsubscribe user.');
    }
  };

  return (
    <div>
      <h1 className="font-display text-3xl font-bold">Newsletter</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{subscribers.length} subscriber{subscribers.length !== 1 ? 's' : ''}</p>

      <div className="mt-8 grid gap-8 md:grid-cols-[1.2fr_1fr]">
        <div className="space-y-4">
          <input value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="Campaign subject" className="input-field" />
          <textarea value={content} onChange={(e) => setContent(e.target.value)} placeholder="Campaign content" className="input-field min-h-[200px] resize-y" />
          <button onClick={sendCampaign} disabled={sending} className="btn-primary">
            <Send size={16} /> {sending ? 'Sending...' : 'Send Campaign'}
          </button>
          {message && <p className="text-sm text-brand dark:text-brand-400">{message}</p>}
        </div>

        <div className="space-y-2">
          <p className="mb-3 text-sm font-medium text-slate-500 dark:text-slate-400">Subscribers</p>
          {subscribers.map((s) => (
            <div key={s.subscriberId} className="flex items-center gap-3 rounded-xl border border-slate-100 bg-slate-50 p-3 transition-colors dark:border-slate-800 dark:bg-slate-800/50">
              <Mail size={14} className="text-slate-400" />
              <span className="text-sm break-all">{s.email}</span>
              <div className="ml-auto flex items-center gap-2">
                <span className="badge-brand text-xs">{s.status}</span>
                <button 
                  onClick={() => unsubscribeUser(s.subscriberId)}
                  className="p-1.5 text-slate-400 hover:text-red-500 transition-colors"
                  title="Unsubscribe user"
                >
                  <UserMinus size={14} />
                </button>
              </div>
            </div>
          ))}
          {subscribers.length === 0 && <p className="text-sm text-slate-500 dark:text-slate-400">No subscribers yet.</p>}
        </div>
      </div>
    </div>
  );
}
