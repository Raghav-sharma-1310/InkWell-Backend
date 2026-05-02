/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { resolveDashboardPath } from '../../utils/navigation';
import { Mail, Lock, User, Edit3, Feather, ArrowLeft, KeyRound, ShieldCheck } from 'lucide-react';
import api from '../../api/client';

/* ─── Shared wrapper ──────────────────────────────────────────── */
function AuthShell({ children, heading, subtext }) {
  return (
    <div className="mx-auto grid min-h-[calc(100vh-8rem)] max-w-6xl gap-8 px-4 py-10 md:px-8 lg:grid-cols-[1.1fr_0.9fr]">
      {/* Left decorative panel */}
      <section className="hidden overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 p-10 text-white shadow-glow lg:flex lg:flex-col lg:justify-center">
        <div className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand"><Feather size={16} /></div>
          <span className="font-display text-lg font-bold">InkWell</span>
        </div>
        <h1 className="mt-8 font-display text-4xl font-bold leading-tight xl:text-5xl">
          A polished publishing workspace for readers &amp; authors.
        </h1>
        <p className="mt-4 text-lg text-white/70">
          Role-based dashboards, OAuth integration, newsletters, payments, and more.
        </p>
      </section>

      {/* Right form panel */}
      <div className="flex flex-col justify-center space-y-5">
        <div>
          <p className="page-heading">{heading}</p>
          <h2 className="mt-2 font-display text-3xl font-bold">{subtext}</h2>
        </div>
        {children}
      </div>
    </div>
  );
}

/* ─── Login page ──────────────────────────────────────────────── */
export function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login, loading } = useAuth();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState(
    searchParams.get('error') === 'oauth' ? 'OAuth login failed. Please try again or use email/username login.' : ''
  );

  // Clear stale tokens when landing on the login page
  useEffect(() => {
    localStorage.removeItem('inkwell.accessToken');
    localStorage.removeItem('inkwell.refreshToken');
  }, []);

  // Defines submit so related behavior stays grouped in one place.
  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const user = await login({ email: form.email, password: form.password });
      navigate(resolveDashboardPath(user));
    } catch (err) {
      setError(err?.response?.data?.message || 'Login failed. Please check your credentials.');
    }
  };

  const authBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

  return (
    <AuthShell heading="Welcome back" subtext="Sign in to InkWell">
      <p className="text-sm text-slate-500 dark:text-slate-400">Use your email or username with your password.</p>

      <form onSubmit={submit} className="space-y-4">
        <div className="relative">
          <Mail size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
          <input type="text" placeholder="Email or username" value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })} className="input-field pl-10" required />
        </div>
        <div className="relative">
          <Lock size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
          <input type="password" placeholder="Password" value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} className="input-field pl-10" required />
        </div>

        <div className="flex items-center justify-end">
          <Link to="/forgot-password" className="text-sm font-medium text-brand hover:text-brand-800 dark:text-brand-400">
            Forgot password?
          </Link>
        </div>

        {error && (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800/50 dark:bg-red-950/30 dark:text-red-300">
            {error}
          </div>
        )}

        <button disabled={loading} className="btn-primary w-full py-3">
          {loading ? 'Signing in...' : 'Sign in'}
        </button>
      </form>

      {/* OAuth divider */}
      <div className="relative my-1">
        <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-slate-200 dark:border-slate-700" /></div>
        <div className="relative flex justify-center"><span className="bg-slate-50 px-3 text-xs text-slate-500 dark:bg-slate-950 dark:text-slate-400">or continue with</span></div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <a href={`${authBase}/oauth2/authorization/google`} className="btn-secondary justify-center py-2.5">
          <svg width="16" height="16" viewBox="0 0 24 24"><path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/><path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/><path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/><path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/></svg>
          Google
        </a>
        <a href={`${authBase}/oauth2/authorization/github`} className="btn-secondary justify-center py-2.5">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/></svg>
          GitHub
        </a>
      </div>

      <p className="text-center text-sm text-slate-500 dark:text-slate-400">
        Don&apos;t have an account?{' '}
        <Link to="/register" className="font-medium text-brand hover:text-brand-800 dark:text-brand-400">Create one</Link>
      </p>
    </AuthShell>
  );
}

/* ─── Register page ───────────────────────────────────────────── */
export function RegisterPage() {
  const navigate = useNavigate();
  const { register, loading } = useAuth();
  const [form, setForm] = useState({ email: '', password: '', username: '', fullName: '', role: 'READER' });
  const [error, setError] = useState('');

  // Defines submit so related behavior stays grouped in one place.
  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const user = await register(form);
      navigate(resolveDashboardPath(user));
    } catch (err) {
      setError(err?.response?.data?.message || 'Registration failed.');
    }
  };

  return (
    <AuthShell heading="Create account" subtext="Get started for free">
      <form onSubmit={submit} className="space-y-4">
        {/* Role picker */}
        <div className="grid grid-cols-2 gap-3">
          {[
            { key: 'READER', label: 'Reader', desc: 'Read, bookmark, and join discussions.' },
            { key: 'AUTHOR', label: 'Author', desc: 'Write posts, manage media, moderate.' },
          ].map((r) => (
            <button key={r.key} type="button" onClick={() => setForm((c) => ({ ...c, role: r.key }))}
              className={`rounded-xl border p-4 text-left transition-all ${form.role === r.key
                ? 'border-brand bg-brand-50 dark:border-brand-600 dark:bg-brand-950/40'
                : 'border-slate-200 hover:border-slate-300 dark:border-slate-700 dark:hover:border-slate-600'}`}>
              <p className="text-sm font-semibold">{r.label}</p>
              <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{r.desc}</p>
            </button>
          ))}
        </div>
        <div className="relative">
          <User size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
          <input placeholder="Full name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} className="input-field pl-10" required />
        </div>
        <div className="relative">
          <Edit3 size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
          <input placeholder="Username" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} className="input-field pl-10" required />
        </div>
        <div className="relative">
          <Mail size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
          <input type="email" placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className="input-field pl-10" required />
        </div>
        <div className="relative">
          <Lock size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
          <input type="password" placeholder="Password (min 8 chars, upper+lower+digit)" value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })} className="input-field pl-10" required minLength={8} />
        </div>
        {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800/50 dark:bg-red-950/30 dark:text-red-300">{error}</div>}
        <button disabled={loading} className="btn-primary w-full py-3">{loading ? 'Creating...' : 'Create account'}</button>
      </form>
      <p className="text-center text-sm text-slate-500 dark:text-slate-400">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-brand hover:text-brand-800 dark:text-brand-400">Sign in</Link>
      </p>
    </AuthShell>
  );
}

/* ─── Forgot Password page (3 steps: email → OTP → new password) */
export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1); // 1=email, 2=otp, 3=new password
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  // Performs the send otp workflow so callers do not duplicate this logic.
  const sendOtp = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      await api.post('/api/auth/forgot-password', { email });
      setMessage('OTP sent to your email.');
      setStep(2);
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to send OTP.');
    } finally { setLoading(false); }
  };

  // Performs the verify otp workflow so callers do not duplicate this logic.
  const verifyOtp = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      await api.post('/api/auth/verify-otp', { email, otp });
      setMessage('OTP verified. Set your new password.');
      setStep(3);
    } catch (err) {
      setError(err?.response?.data?.message || 'OTP verification failed.');
    } finally { setLoading(false); }
  };

  // Defines reset password so related behavior stays grouped in one place.
  const resetPassword = async (e) => {
    e.preventDefault();
    setError(''); setLoading(true);
    try {
      await api.post('/api/auth/reset-password', { email, newPassword });
      setMessage('Password reset successfully!');
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      setError(err?.response?.data?.message || 'Password reset failed.');
    } finally { setLoading(false); }
  };

  return (
    <AuthShell heading="Reset password" subtext={step === 1 ? 'Enter your email' : step === 2 ? 'Verify OTP' : 'New password'}>
      <Link to="/login" className="flex items-center gap-1 text-sm text-slate-500 hover:text-brand dark:text-slate-400 dark:hover:text-brand-400">
        <ArrowLeft size={14} /> Back to sign in
      </Link>

      {/* Step indicators */}
      <div className="flex items-center gap-2">
        {[1, 2, 3].map((s) => (
          <div key={s} className={`h-1.5 flex-1 rounded-full transition-colors ${s <= step ? 'bg-brand' : 'bg-slate-200 dark:bg-slate-700'}`} />
        ))}
      </div>

      {step === 1 && (
        <form onSubmit={sendOtp} className="space-y-4">
          <p className="text-sm text-slate-500 dark:text-slate-400">We'll send a 6-digit code to your registered email.</p>
          <div className="relative">
            <Mail size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
            <input type="email" placeholder="Registered email" value={email} onChange={(e) => setEmail(e.target.value)} className="input-field pl-10" required />
          </div>
          {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800/50 dark:bg-red-950/30 dark:text-red-300">{error}</div>}
          <button disabled={loading} className="btn-primary w-full py-3">{loading ? 'Sending...' : 'Send OTP'}</button>
        </form>
      )}

      {step === 2 && (
        <form onSubmit={verifyOtp} className="space-y-4">
          <p className="text-sm text-slate-500 dark:text-slate-400">Enter the 6-digit code sent to <strong>{email}</strong>.</p>
          <div className="relative">
            <KeyRound size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
            <input placeholder="Enter OTP" value={otp} onChange={(e) => setOtp(e.target.value)} className="input-field pl-10 text-center text-2xl tracking-[0.5em] font-bold" maxLength={6} required />
          </div>
          {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800/50 dark:bg-red-950/30 dark:text-red-300">{error}</div>}
          <button disabled={loading} className="btn-primary w-full py-3">{loading ? 'Verifying...' : 'Verify OTP'}</button>
          <button type="button" onClick={() => { setStep(1); setError(''); }} className="btn-ghost w-full">Resend OTP</button>
        </form>
      )}

      {step === 3 && (
        <form onSubmit={resetPassword} className="space-y-4">
          <p className="text-sm text-slate-500 dark:text-slate-400">Choose a strong new password.</p>
          <div className="relative">
            <ShieldCheck size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
            <input type="password" placeholder="New password (min 8 chars)" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} className="input-field pl-10" required minLength={8} />
          </div>
          {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800/50 dark:bg-red-950/30 dark:text-red-300">{error}</div>}
          <button disabled={loading} className="btn-primary w-full py-3">{loading ? 'Resetting...' : 'Reset Password'}</button>
        </form>
      )}

      {message && <div className="rounded-xl bg-brand-50 p-3 text-sm text-brand-700 dark:bg-brand-950/30 dark:text-brand-300">{message}</div>}
    </AuthShell>
  );
}
