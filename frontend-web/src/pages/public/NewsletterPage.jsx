/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useState } from 'react';
import { Mail, User, Send, CheckCircle } from 'lucide-react';
import api from '../../api/client';

// Defines newsletter page so related behavior stays grouped in one place.
export function NewsletterPage() {
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [message, setMessage] = useState('');
  const [success, setSuccess] = useState(false);

  // Defines submit so related behavior stays grouped in one place.
  const submit = async (event) => {
    event.preventDefault();
    try {
      const response = await api.post('/api/newsletter/public/subscribe', { email, fullName });
      setMessage(response.data.message);
      setSuccess(true);
      setEmail('');
      setFullName('');
    } catch (err) {
      setMessage(err?.response?.data?.message || 'Subscription failed. Please try again.');
      setSuccess(false);
    }
  };

  return (
    <div className="mx-auto max-w-3xl px-4 py-14 md:px-8">
      <div className="card overflow-hidden">
        <div className="bg-gradient-to-br from-brand-700 to-brand-900 p-10 text-white">
          <Send size={32} className="text-brand-200" />
          <h1 className="mt-4 font-display text-4xl font-bold leading-tight md:text-5xl">
            Weekly notes, thoughtful writing, and release updates.
          </h1>
          <p className="mt-4 text-lg text-white/70">Join our newsletter and stay up-to-date with the InkWell community.</p>
        </div>
        <div className="p-8">
          <form onSubmit={submit} className="grid gap-4 md:grid-cols-[1fr_1fr_auto]">
            <div className="relative">
              <User size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
              <input value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Full name" className="input-field pl-10" />
            </div>
            <div className="relative">
              <Mail size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
              <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Email address" className="input-field pl-10" type="email" required />
            </div>
            <button className="btn-primary">Subscribe</button>
          </form>
          {message && (
            <div className={`mt-4 flex items-center gap-2 rounded-xl p-3 text-sm ${success ? 'bg-green-50 text-green-700 dark:bg-green-950/30 dark:text-green-300' : 'bg-red-50 text-red-700 dark:bg-red-950/30 dark:text-red-300'}`}>
              {success && <CheckCircle size={16} />}
              {message}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
