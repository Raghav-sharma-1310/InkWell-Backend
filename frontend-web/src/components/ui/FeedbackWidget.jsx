/*
 * This file provides reusable UI behavior for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate, useLocation } from 'react-router-dom';
import api from '../../api/client';
import { Bug, X, Send, Loader2, MessageCircle, CheckCircle, Minimize2 } from 'lucide-react';

// Performs the feedback widget workflow so callers do not duplicate this logic.
export function FeedbackWidget() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [sent, setSent] = useState(false);
  const chatEndRef = useRef(null);

  // Load existing messages when widget opens
  useEffect(() => {
    if (open && user) {
      api.get('/api/feedback/my-reports')
        .then(res => {
          const reports = res.data.data || [];
          // Get the most recent open/in-progress report
          const active = reports.find(r => r.status === 'OPEN' || r.status === 'IN_PROGRESS');
          if (active) {
            setMessages(active.messages || []);
          }
        })
        .catch(() => {});
    }
  }, [open, user]);

  // Scroll to bottom
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Performs the handle send workflow so callers do not duplicate this logic.
  const handleSend = async () => {
    if (!input.trim() || sending) return;
    if (!user) {
      navigate('/login');
      return;
    }

    setSending(true);
    try {
      const res = await api.post('/api/feedback/report', {
        message: input.trim(),
        pageUrl: location.pathname,
      });
      setMessages(res.data.data?.messages || []);
      setInput('');
      setSent(true);
      setTimeout(() => setSent(false), 2000);
    } catch (err) {
      alert(err?.response?.data?.message || 'Failed to send feedback');
    } finally {
      setSending(false);
    }
  };

  // Don't show on admin/author dashboard pages
  const isAdminPage = location.pathname.startsWith('/admin');

  return (
    <>
      {/* Floating button */}
      <button
        id="feedback-widget-trigger"
        onClick={() => {
          if (!user) {
            navigate('/login');
            return;
          }
          setOpen(v => !v);
        }}
        className={`fixed bottom-6 right-6 z-50 flex h-14 items-center gap-2 rounded-2xl px-5 shadow-2xl transition-all duration-300 hover:scale-105 hover:shadow-glow ${
          open
            ? 'bg-slate-700 text-white'
            : 'bg-gradient-to-r from-brand-600 to-teal-600 text-white'
        }`}
        style={{ display: isAdminPage ? 'none' : undefined }}
      >
        {open ? (
          <>
            <Minimize2 size={18} />
            <span className="hidden text-sm font-semibold sm:inline">Close</span>
          </>
        ) : (
          <>
            <Bug size={18} />
            <span className="hidden text-sm font-semibold sm:inline">Feedback</span>
          </>
        )}
      </button>

      {/* Chat panel */}
      {open && (
        <div className="fixed bottom-24 right-6 z-50 flex w-[360px] max-w-[calc(100vw-2rem)] flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-slate-700 dark:bg-slate-900"
          style={{ height: '480px', animation: 'slideUp 0.3s ease-out' }}
        >
          {/* Header */}
          <div className="flex items-center justify-between bg-gradient-to-r from-brand-600 to-teal-600 px-5 py-4">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-white/20">
                <MessageCircle size={18} className="text-white" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-white">Report Bug / Feedback</h3>
                <p className="text-[11px] text-white/70">We'd love to hear from you</p>
              </div>
            </div>
            <button onClick={() => setOpen(false)} className="rounded-lg p-1.5 text-white/70 transition-colors hover:bg-white/20 hover:text-white">
              <X size={16} />
            </button>
          </div>

          {/* Messages area */}
          <div className="flex-1 space-y-3 overflow-y-auto p-4">
            {messages.length === 0 ? (
              <div className="flex h-full flex-col items-center justify-center text-center">
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-50 dark:bg-brand-900/30">
                  <Bug size={24} className="text-brand" />
                </div>
                <p className="mt-4 text-sm font-medium text-slate-700 dark:text-slate-300">How can we help?</p>
                <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">Report a bug or share your feedback below.</p>
              </div>
            ) : (
              messages.map(msg => (
                <div key={msg.messageId} className={`flex ${msg.senderRole === 'ADMIN' ? 'justify-start' : 'justify-end'}`}>
                  <div className={`max-w-[85%] rounded-2xl px-4 py-2.5 ${
                    msg.senderRole === 'ADMIN'
                      ? 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-200 rounded-bl-md'
                      : 'bg-brand text-white rounded-br-md'
                  }`}>
                    {msg.senderRole === 'ADMIN' && (
                      <p className="text-[10px] font-semibold text-brand-600 dark:text-brand-400 mb-0.5">Admin</p>
                    )}
                    <p className="text-sm leading-relaxed">{msg.content}</p>
                    <p className="mt-1 text-[10px] opacity-50">{new Date(msg.sentAt).toLocaleTimeString()}</p>
                  </div>
                </div>
              ))
            )}
            <div ref={chatEndRef} />
          </div>

          {/* Input area */}
          <div className="border-t border-slate-100 p-3 dark:border-slate-800">
            {sent && (
              <div className="mb-2 flex items-center gap-2 rounded-lg bg-green-50 px-3 py-1.5 text-xs font-medium text-green-700 dark:bg-green-900/30 dark:text-green-400">
                <CheckCircle size={12} /> Feedback sent!
              </div>
            )}
            <div className="flex gap-2">
              <input
                type="text"
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSend()}
                placeholder="Type your message..."
                className="flex-1 rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none transition-all focus:border-brand focus:ring-2 focus:ring-brand/20 dark:border-slate-700 dark:bg-slate-800 dark:focus:border-brand"
                autoFocus
              />
              <button
                onClick={handleSend}
                disabled={!input.trim() || sending}
                className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand text-white transition-all hover:bg-brand-700 disabled:opacity-50"
              >
                {sending ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />}
              </button>
            </div>
            <p className="mt-1.5 text-center text-[10px] text-slate-400 dark:text-slate-500">
              Page: {location.pathname}
            </p>
          </div>
        </div>
      )}

      {/* Animation keyframes */}
      <style>{`
        @keyframes slideUp {
          from { opacity: 0; transform: translateY(20px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </>
  );
}
