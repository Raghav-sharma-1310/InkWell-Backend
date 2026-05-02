/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { CheckCircle, XCircle, Loader2, Mail } from 'lucide-react';
import api from '../../api/client';

// Defines newsletter confirm page so related behavior stays grouped in one place.
export function NewsletterConfirmPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState('loading'); // loading, success, error
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setMessage('Invalid or missing confirmation token.');
      return;
    }

    api.get(`/api/newsletter/public/confirm/${token}`)
      .then((res) => {
        setStatus('success');
        setMessage(res.data.message || 'Subscription confirmed successfully!');
      })
      .catch((err) => {
        setStatus('error');
        setMessage(err?.response?.data?.message || 'Failed to confirm subscription. The link might be expired.');
      });
  }, [token]);

  return (
    <div className="flex min-h-[60vh] items-center justify-center p-4">
      <div className="w-full max-w-md text-center">
        {status === 'loading' && (
          <div className="flex flex-col items-center gap-4 text-slate-500">
            <Loader2 size={40} className="animate-spin text-brand" />
            <p>Confirming your subscription...</p>
          </div>
        )}

        {status === 'success' && (
          <div className="flex flex-col items-center gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-100 text-green-600 dark:bg-green-900/30">
              <CheckCircle size={32} />
            </div>
            <h1 className="font-display text-2xl font-bold">Subscription Confirmed!</h1>
            <p className="text-slate-500 dark:text-slate-400">
              {message} You're all set to receive our latest updates.
            </p>
            <Link to="/" className="btn-primary mt-4">
              Return to Home
            </Link>
          </div>
        )}

        {status === 'error' && (
          <div className="flex flex-col items-center gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-red-100 text-red-600 dark:bg-red-900/30">
              <XCircle size={32} />
            </div>
            <h1 className="font-display text-2xl font-bold">Confirmation Failed</h1>
            <p className="text-slate-500 dark:text-slate-400">
              {message}
            </p>
            <Link to="/newsletter" className="btn-secondary mt-4">
              Try Subscribing Again
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
