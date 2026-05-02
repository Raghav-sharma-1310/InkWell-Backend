/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import api from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { CreditCard, Bell, Shield, Loader2, Edit3, Save, Lock, User, Mail, FileText, KeyRound, Upload, Bookmark, History, Crown, CheckCircle, AlertTriangle, UserPlus, Clock, XCircle } from 'lucide-react';
import { PageLoader } from '../../components/ui/LoadingSpinner';

// Defines strip html so related behavior stays grouped in one place.
const stripHtml = (html) => {
  if (!html) return '';
  const tmp = document.createElement('DIV');
  tmp.innerHTML = html;
  return tmp.textContent || tmp.innerText || '';
};

// Performs the load razorpay script workflow so callers do not duplicate this logic.
function loadRazorpayScript() {
  return new Promise((resolve) => {
    if (window.Razorpay) { resolve(true); return; }
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

// Defines profile page so related behavior stays grouped in one place.
export function ProfilePage() {
  const { user, updateAuthState } = useAuth();
  const [tab, setTab] = useState('overview');
  const [loading, setLoading] = useState(true);
  const [payments, setPayments] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [savedPosts, setSavedPosts] = useState([]);
  const [historyPosts, setHistoryPosts] = useState([]);
  const [newsletterStatus, setNewsletterStatus] = useState(null);
  const [authorRequest, setAuthorRequest] = useState(null);
  const [authorReqLoading, setAuthorReqLoading] = useState(false);

  const [editForm, setEditForm] = useState({ fullName: '', username: '', bio: '', phoneNumber: '', avatarUrl: '' });
  const [editMsg, setEditMsg] = useState('');
  const [editMsgType, setEditMsgType] = useState('');
  const [editSaving, setEditSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});

  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '' });
  const [pwMsg, setPwMsg] = useState('');
  const [pwSaving, setPwSaving] = useState(false);

  const [payMsg, setPayMsg] = useState('');
  const [processing, setProcessing] = useState('');

  const plans = [
    { id: 'reader-pro', purpose: 'InkWell Reader Pro', description: 'Priority alerts, premium reading lists, and digest access.', amount: 149 },
    { id: 'author-plus', purpose: 'InkWell Author Plus', description: 'Advanced analytics, media workflows, and monetization.', amount: 499 },
  ];

  // Derived subscription state
  const isPro = user?.subscriptionTier === 'PRO' && user?.subscriptionStatus === 'ACTIVE';
  const isExpired = user?.subscriptionStatus === 'EXPIRED';
  const isAuthorOrAdmin = user?.role === 'AUTHOR' || user?.role === 'ADMIN';

  // Performs the refresh data workflow so callers do not duplicate this logic.
  const refreshData = async () => {
    const [payRes, notifRes, newsletterRes] = await Promise.all([
      api.get('/api/auth/payments/history').catch(() => ({ data: { data: [] } })),
      api.get('/api/notifications').catch(() => ({ data: { data: [] } })),
      api.get('/api/newsletter/me').catch(() => ({ data: { data: null } })),
    ]);
    setPayments(payRes.data.data || []);
    setNotifications((notifRes.data.data || []).slice(0, 5));
    setNewsletterStatus(newsletterRes.data.data?.status || 'Not Subscribed');

    // Only fetch premium endpoints for PRO subscribers to avoid 403
    if (isPro) {
      const [savedRes, historyRes] = await Promise.all([
        api.get('/api/posts/reader/bookmarks').catch(() => ({ data: { data: [] } })),
        api.get('/api/posts/reader/history').catch(() => ({ data: { data: { content: [] } } })),
      ]);
      setSavedPosts(savedRes.data.data || []);
      setHistoryPosts(historyRes.data.data?.content || []);
    } else {
      setSavedPosts([]);
      setHistoryPosts([]);
    }

    // Fetch author request status for READERs
    if (user?.role === 'READER') {
      try {
        const arRes = await api.get('/api/author-request/status');
        setAuthorRequest(arRes.data.data);
      } catch {
        setAuthorRequest(null);
      }
    }
  };

  useEffect(() => {
    if (user) {
      setEditForm({ 
        fullName: user.fullName || '', 
        username: user.username || '', 
        bio: user.bio || '', 
        phoneNumber: user.phoneNumber || '', 
        avatarUrl: user.avatarUrl || '' 
      });
    }
    refreshData().finally(() => setLoading(false));
  }, [user]);

  // ── Validation helpers ──
  const validators = {
    fullName: (v) => {
      if (!v || !v.trim()) return 'Full name is required';
      if (v.trim().length < 2) return 'Full name must be at least 2 characters';
      if (!/^[a-zA-Z\s]+$/.test(v.trim())) return 'Full name must contain only letters and spaces';
      return '';
    },
    username: (v) => {
      if (!v || !v.trim()) return 'Username is required';
      if (v.trim().length < 3) return 'Username must be at least 3 characters';
      if (/\s/.test(v)) return 'Username cannot contain spaces';
      if (!/^[a-zA-Z0-9_.]+$/.test(v.trim())) return 'Username can only contain letters, numbers, underscores, and dots';
      return '';
    },
    phoneNumber: (v) => {
      if (!v || !v.trim()) return '';
      if (!/^\+?\d{10,15}$/.test(v.trim())) return 'Phone must be 10-15 digits, optionally starting with +';
      return '';
    },
    bio: (v) => {
      if (v && v.length > 1000) return 'Bio must not exceed 1000 characters';
      return '';
    },
  };

  // Defines validate field so related behavior stays grouped in one place.
  const validateField = (field, value) => {
    const fn = validators[field];
    if (!fn) return '';
    const err = fn(value);
    setFieldErrors((prev) => ({ ...prev, [field]: err }));
    return err;
  };

  // Defines validate all so related behavior stays grouped in one place.
  const validateAll = () => {
    const errors = {};
    Object.keys(validators).forEach((field) => {
      const err = validators[field](editForm[field]);
      if (err) errors[field] = err;
    });
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Defines handle field change so related behavior stays grouped in one place.
  const handleFieldChange = (field, value) => {
    setEditForm({ ...editForm, [field]: value });
    if (fieldErrors[field]) validateField(field, value);
  };

  // Defines is form valid so related behavior stays grouped in one place.
  const isFormValid = () => {
    return !validators.fullName(editForm.fullName) &&
           !validators.username(editForm.username) &&
           !validators.phoneNumber(editForm.phoneNumber) &&
           !validators.bio(editForm.bio);
  };

  // Performs the save profile workflow so callers do not duplicate this logic.
  const saveProfile = async (e) => {
    e.preventDefault();
    setEditMsg(''); setEditMsgType('');
    if (!validateAll()) {
      setEditMsg('Please fix the validation errors before saving.');
      setEditMsgType('error');
      return;
    }
    setEditSaving(true);
    try {
      const res = await api.patch('/api/auth/me', editForm);
      updateAuthState(null, null, res.data.data);
      setEditMsg('Profile updated successfully.');
      setEditMsgType('success');
      setFieldErrors({});
    } catch (err) {
      const data = err?.response?.data;
      if (data?.fieldErrors) {
        setFieldErrors(data.fieldErrors);
        setEditMsg('Validation failed. Please check the highlighted fields.');
      } else if (data?.errors && Array.isArray(data.errors)) {
        const parsed = {};
        data.errors.forEach((e) => { if (e.field && e.message) parsed[e.field] = e.message; });
        setFieldErrors(parsed);
        setEditMsg('Validation failed. Please check the highlighted fields.');
      } else {
        setEditMsg(data?.message || 'Failed to update profile.');
      }
      setEditMsgType('error');
    } finally { setEditSaving(false); }
  };

  // Defines change password so related behavior stays grouped in one place.
  const changePassword = async (e) => {
    e.preventDefault();
    setPwSaving(true); setPwMsg('');
    try {
      await api.patch('/api/auth/me/password', pwForm);
      setPwMsg('Password changed successfully.');
      setPwForm({ currentPassword: '', newPassword: '' });
    } catch (err) {
      setPwMsg(err?.response?.data?.message || 'Failed to change password.');
    } finally { setPwSaving(false); }
  };

  // Defines handle payment so related behavior stays grouped in one place.
  const handlePayment = async (plan) => {
    setProcessing(plan.id); setPayMsg('');
    try {
      const { data } = await api.post('/api/auth/payments/orders', {
        amount: plan.amount, currency: 'INR', purpose: plan.purpose, description: plan.description,
      });
      const order = data.data;
      const loaded = await loadRazorpayScript();
      if (!loaded) { setPayMsg('Unable to load Razorpay.'); setProcessing(''); return; }
      new window.Razorpay({
        key: order.gatewayPublicKey, amount: Math.round(plan.amount * 100), currency: 'INR',
        name: 'InkWell', description: plan.description, order_id: order.gatewayOrderId,
        theme: { color: '#0f766e' },
        handler: async (response) => {
          try {
            const verifyRes = await api.post('/api/auth/payments/verify', {
              paymentOrderId: order.paymentOrderId, gatewayOrderId: response.razorpay_order_id,
              gatewayPaymentId: response.razorpay_payment_id, gatewaySignature: response.razorpay_signature, status: 'SUCCESS',
            });
            const result = verifyRes.data.data;
            // Update tokens and user from the verify response
            if (result.accessToken && result.refreshToken && result.user) {
              updateAuthState(result.accessToken, result.refreshToken, result.user);
            }
            setPayMsg('Payment verified! Your subscription is now active.');
            await refreshData();
          } catch (err) {
            setPayMsg(err?.response?.data?.message || 'Payment verification failed.');
          } finally {
            setProcessing('');
          }
        },
        modal: {
          ondismiss: () => { setProcessing(''); }
        }
      }).open();
    } catch (err) {
      setPayMsg(err?.response?.data?.message || 'Payment failed.');
      setProcessing('');
    }
  };

  if (loading) return <PageLoader />;

  const tabs = [
    { key: 'overview', label: 'Overview', icon: <User size={16} /> },
    ...(user?.role === 'READER' ? [{ key: 'author-request', label: 'Become Author', icon: <UserPlus size={16} /> }] : []),
    { key: 'saved', label: 'Bookmarks', icon: <Bookmark size={16} /> },
    { key: 'history', label: 'Reading History', icon: <History size={16} /> },
    { key: 'edit', label: 'Edit Profile', icon: <Edit3 size={16} /> },
    { key: 'password', label: 'Password', icon: <KeyRound size={16} /> },
    ...(user?.role !== 'ADMIN' ? [{ key: 'billing', label: 'Billing', icon: <CreditCard size={16} /> }] : []),
  ];

  const availablePlans = plans.filter(p => {
    if (user?.role === 'READER') return p.id === 'reader-pro';
    if (user?.role === 'AUTHOR') return p.id === 'author-plus';
    return false;
  });

  // Performs the get author request panel class workflow so callers do not duplicate this logic.
  const getAuthorRequestPanelClass = (status) => {
    if (status === 'PENDING') return 'border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-900/20';
    if (status === 'APPROVED') return 'border-green-200 bg-green-50 dark:border-green-800 dark:bg-green-900/20';
    return 'border-red-200 bg-red-50 dark:border-red-800 dark:bg-red-900/20';
  };

  // Performs the get author request title class workflow so callers do not duplicate this logic.
  const getAuthorRequestTitleClass = (status) => {
    if (status === 'PENDING') return 'text-amber-700 dark:text-amber-400';
    if (status === 'APPROVED') return 'text-green-700 dark:text-green-400';
    return 'text-red-700 dark:text-red-400';
  };

  // Performs the get plan button class workflow so callers do not duplicate this logic.
  const getPlanButtonClass = (isDisabled, isCurrent, buttonText) => {
    const disabledClass = isDisabled && !isCurrent && buttonText !== 'Upgrade' ? 'opacity-50 cursor-not-allowed' : '';
    const currentClass = isCurrent ? 'bg-green-600 hover:bg-green-600 border-none cursor-default' : '';
    return `btn-primary mt-2 text-xs ${disabledClass} ${currentClass}`;
  };

  // Performs the get subscription label workflow so callers do not duplicate this logic.
  const getSubscriptionLabel = () => {
    if (isPro) return 'PRO (Active)';
    if (isExpired) return 'Expired';
    return 'Free';
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 md:px-8">
      {/* Header */}
      <div className="card p-8">
        <div className="flex items-center gap-4">
          {user?.avatarUrl ? (
            <img src={user.avatarUrl} alt="" className="h-16 w-16 rounded-2xl object-cover" />
          ) : (
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-brand-100 text-2xl font-bold text-brand-700 dark:bg-brand-900 dark:text-brand-300">
              {user?.fullName?.[0]?.toUpperCase() || 'U'}
            </div>
          )}
          <div>
            <h1 className="font-display text-3xl font-bold">{user?.fullName}</h1>
            <p className="text-sm text-slate-500 dark:text-slate-400">@{user?.username} · {user?.email}</p>
            <div className="mt-1 flex gap-2">
              <span className="badge-brand">{user?.role}</span>
              <span className="badge bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300">{user?.provider}</span>
              {isPro && (
                <span className="badge bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400 font-bold border border-yellow-200 dark:border-yellow-800 flex items-center gap-1">
                  <Crown size={12} /> PRO
                </span>
              )}
              {isExpired && (
                <span className="badge bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400 font-bold border border-red-200 dark:border-red-800 flex items-center gap-1">
                  <AlertTriangle size={12} /> Expired
                </span>
              )}
            </div>
          </div>
        </div>
        {user?.bio && <p className="mt-4 text-sm text-slate-600 dark:text-slate-400">{user.bio}</p>}
      </div>

      {/* Tab nav */}
      <div className="mt-6 flex gap-1 overflow-x-auto rounded-xl border border-slate-200 bg-white p-1 dark:border-slate-800 dark:bg-slate-900">
        {tabs.map((t) => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`flex items-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition-all whitespace-nowrap ${
              tab === t.key ? 'bg-brand text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800'}`}>
            {t.icon} {t.label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {/* Overview */}
        {tab === 'overview' && (
          <div className="grid gap-6 lg:grid-cols-2">
            <div className="card p-6">
              <h2 className="font-display text-xl font-bold">Account Info</h2>
              <div className="mt-4 space-y-3">
                {[
                  { label: 'Username', value: user?.username },
                  { label: 'Email', value: user?.email },
                  { label: 'Full Name', value: user?.fullName },
                  { label: 'Role', value: user?.role },
                  { label: 'Provider', value: user?.provider },
                  { label: 'Status', value: user?.active ? 'Active' : 'Deactivated' },
                  { label: 'Subscription', value: getSubscriptionLabel() },
                  { label: 'Newsletter', value: newsletterStatus },
                  { label: 'Member since', value: user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—' },
                ].map((r) => (
                  <div key={r.label} className="flex items-center justify-between rounded-lg bg-slate-50 px-4 py-3 transition-colors dark:bg-slate-800/50">
                    <span className="text-sm text-slate-500 dark:text-slate-400">{r.label}</span>
                    <span className="text-sm font-medium">{r.value || '—'}</span>
                  </div>
                ))}
              </div>
            </div>
            <div className="card p-6">
              <h2 className="font-display text-xl font-bold">Recent Notifications</h2>
              <div className="mt-4 space-y-3">
                {notifications.length > 0 ? notifications.map((n) => (
                  <div key={n.notificationId} className="rounded-lg bg-slate-50 p-3 transition-colors dark:bg-slate-800/50">
                    <p className="text-sm font-medium">{n.title}</p>
                    <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{stripHtml(n.message)}</p>
                  </div>
                )) : <p className="text-sm text-slate-500 dark:text-slate-400">No notifications yet.</p>}
              </div>
            </div>
          </div>
        )}

        {/* Author Request */}
        {tab === 'author-request' && (
          <div className="card max-w-2xl p-8">
            <div className="flex items-center gap-3 mb-6">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-teal-500">
                <UserPlus size={24} className="text-white" />
              </div>
              <div>
                <h2 className="font-display text-xl font-bold">Become an Author</h2>
                <p className="text-sm text-slate-500 dark:text-slate-400">Request to upgrade your account to Author role</p>
              </div>
            </div>

            {authorRequest ? (
              <div className="space-y-4">
                <div className={`rounded-xl border-2 p-5 ${getAuthorRequestPanelClass(authorRequest.status)}`}>
                  <div className="flex items-center gap-3">
                    {authorRequest.status === 'PENDING' && <Clock size={24} className="text-amber-500" />}
                    {authorRequest.status === 'APPROVED' && <CheckCircle size={24} className="text-green-500" />}
                    {authorRequest.status === 'REJECTED' && <XCircle size={24} className="text-red-500" />}
                    <div>
                      <p className={`text-lg font-bold ${getAuthorRequestTitleClass(authorRequest.status)}`}>
                        Request {authorRequest.status.charAt(0) + authorRequest.status.slice(1).toLowerCase()}
                      </p>
                      <p className="text-sm text-slate-600 dark:text-slate-400">
                        Submitted on {new Date(authorRequest.requestedAt).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                  {authorRequest.adminRemarks && (
                    <div className="mt-3 rounded-lg bg-white/60 p-3 text-sm dark:bg-slate-800/50">
                      <span className="font-medium">Admin remarks:</span> {authorRequest.adminRemarks}
                    </div>
                  )}
                </div>
                {authorRequest.status === 'APPROVED' && (
                  <p className="text-sm text-green-600 dark:text-green-400 font-medium">
                    🎉 Congratulations! Your role has been upgraded to Author. Please log out and log back in to access your Author Studio.
                  </p>
                )}
                {authorRequest.status === 'REJECTED' && (
                  <p className="text-sm text-slate-500 dark:text-slate-400">
                    You can submit a new request after addressing any feedback from the admin.
                  </p>
                )}
              </div>
            ) : (
              <div className="space-y-6">
                <div className="rounded-xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-800/50">
                  <h3 className="font-semibold">What you'll get as an Author:</h3>
                  <ul className="mt-3 space-y-2 text-sm text-slate-600 dark:text-slate-400">
                    <li className="flex items-center gap-2"><CheckCircle size={14} className="text-brand" /> Create and publish blog posts</li>
                    <li className="flex items-center gap-2"><CheckCircle size={14} className="text-brand" /> Manage your content library</li>
                    <li className="flex items-center gap-2"><CheckCircle size={14} className="text-brand" /> Access Author Studio dashboard</li>
                    <li className="flex items-center gap-2"><CheckCircle size={14} className="text-brand" /> View analytics and engagement metrics</li>
                    <li className="flex items-center gap-2"><CheckCircle size={14} className="text-brand" /> Build your follower base</li>
                  </ul>
                </div>
                <button
                  onClick={async () => {
                    setAuthorReqLoading(true);
                    try {
                      const res = await api.post('/api/author-request');
                      setAuthorRequest(res.data.data);
                    } catch (err) {
                      alert(err?.response?.data?.message || 'Failed to submit request');
                    } finally {
                      setAuthorReqLoading(false);
                    }
                  }}
                  disabled={authorReqLoading}
                  className="btn-primary flex w-full items-center justify-center gap-2 py-3"
                >
                  {authorReqLoading ? <Loader2 size={16} className="animate-spin" /> : <UserPlus size={16} />}
                  Request to Become Author
                </button>
              </div>
            )}
          </div>
        )}

        {/* Bookmarks */}
        {tab === 'saved' && (
          <div className="card p-6 max-w-4xl">
            <h2 className="font-display text-xl font-bold mb-4">Saved Posts</h2>
            {!isPro ? (
              <div className="flex flex-col items-center gap-3 py-8 text-center">
                <Crown size={32} className="text-amber-500" />
                <p className="font-medium">Bookmarks are a PRO feature</p>
                <p className="text-sm text-slate-500 dark:text-slate-400">Upgrade to Pro to save and organize your favorite articles.</p>
                <button onClick={() => setTab('billing')} className="btn-primary mt-2 text-sm">View Plans</button>
              </div>
            ) : (
              <div className="space-y-3">
                {savedPosts.length > 0 ? savedPosts.map((post) => (
                  <a key={post.postId} href={`/posts/${post.slug}`} className="block rounded-lg border border-slate-100 p-4 transition-all hover:border-brand-200 hover:shadow-glow dark:border-slate-800 dark:hover:border-brand-800">
                    <h3 className="font-medium">{post.title}</h3>
                    <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{post.excerpt}</p>
                    <p className="mt-2 text-xs text-slate-400">{new Date(post.createdAt).toLocaleDateString()}</p>
                  </a>
                )) : <p className="text-sm text-slate-500 dark:text-slate-400">No saved posts.</p>}
              </div>
            )}
          </div>
        )}

        {/* Reading History */}
        {tab === 'history' && (
          <div className="card p-6 max-w-4xl">
            <h2 className="font-display text-xl font-bold mb-4">Reading History</h2>
            {!isPro ? (
              <div className="flex flex-col items-center gap-3 py-8 text-center">
                <Crown size={32} className="text-amber-500" />
                <p className="font-medium">Reading History is a PRO feature</p>
                <p className="text-sm text-slate-500 dark:text-slate-400">Upgrade to Pro to track and revisit your reading journey.</p>
                <button onClick={() => setTab('billing')} className="btn-primary mt-2 text-sm">View Plans</button>
              </div>
            ) : (
              <div className="space-y-3">
                {historyPosts.length > 0 ? historyPosts.map((post) => (
                  <a key={post.postId} href={`/posts/${post.slug}`} className="block rounded-lg border border-slate-100 p-4 transition-all hover:border-brand-200 hover:shadow-glow dark:border-slate-800 dark:hover:border-brand-800">
                    <h3 className="font-medium">{post.title}</h3>
                    <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{post.excerpt}</p>
                  </a>
                )) : <p className="text-sm text-slate-500 dark:text-slate-400">No reading history yet.</p>}
              </div>
            )}
          </div>
        )}

        {/* Edit Profile */}
        {tab === 'edit' && (
          <form onSubmit={saveProfile} className="card max-w-2xl space-y-5 p-8">
            <h2 className="font-display text-xl font-bold">Edit Profile</h2>
            <div className="flex flex-col items-center gap-6 sm:flex-row">
              <label className="relative group cursor-pointer overflow-hidden rounded-full">
                {editForm.avatarUrl ? (
                  <img src={editForm.avatarUrl} alt="" className="h-24 w-24 object-cover" onError={(e) => { e.target.style.display='none'; }} />
                ) : (
                  <div className="flex h-24 w-24 items-center justify-center bg-brand-100 text-3xl font-bold text-brand-700 dark:bg-brand-900 dark:text-brand-300">
                    {user?.fullName?.[0]?.toUpperCase() || 'U'}
                  </div>
                )}
                <div className="absolute inset-0 flex items-center justify-center bg-black/50 opacity-0 transition-opacity group-hover:opacity-100">
                  <span className="text-xs font-medium text-white text-center px-2">Change Photo</span>
                </div>
                <input type="file" accept=".jpg,.jpeg,.png,.webp" className="hidden" onChange={async (e) => {
                  const file = e.target.files?.[0];
                  if (!file) return;
                  const formData = new FormData();
                  formData.append('file', file);
                  try {
                    const res = await api.post('/api/media/user/upload-avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
                    setEditForm({ ...editForm, avatarUrl: res.data.data.url });
                  } catch { alert('Upload failed.'); }
                  e.target.value = '';
                }} />
              </label>
              <div className="flex flex-col text-sm text-slate-500 dark:text-slate-400 text-center sm:text-left">
                <p>Click your profile picture to upload a new photo.</p>
                <p className="text-xs mt-1">Recommended size: 256x256px. JPG, PNG, WEBP.</p>
                {editForm.avatarUrl && (
                  <button type="button" onClick={() => setEditForm({ ...editForm, avatarUrl: '' })} className="btn-ghost mt-2 self-center sm:self-start text-xs text-red-500">Remove Photo</button>
                )}
              </div>
            </div>

            {/* Full Name */}
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">Full Name</label>
              <div className="relative">
                <User size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  value={editForm.fullName}
                  onChange={(e) => handleFieldChange('fullName', e.target.value)}
                  onBlur={(e) => validateField('fullName', e.target.value)}
                  placeholder="Full name"
                  className={`input-field pl-10 ${fieldErrors.fullName ? 'border-red-400 focus:border-red-500 focus:ring-red-200 dark:border-red-600' : ''}`}
                />
              </div>
              {fieldErrors.fullName && <p className="mt-1 text-xs text-red-500 dark:text-red-400">{fieldErrors.fullName}</p>}
              <p className="mt-1 text-xs text-slate-400">Only letters and spaces. Minimum 2 characters.</p>
            </div>

            {/* Username */}
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">Username</label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">@</span>
                <input
                  value={editForm.username}
                  onChange={(e) => handleFieldChange('username', e.target.value)}
                  onBlur={(e) => validateField('username', e.target.value)}
                  placeholder="Username"
                  className={`input-field pl-10 ${fieldErrors.username ? 'border-red-400 focus:border-red-500 focus:ring-red-200 dark:border-red-600' : ''}`}
                />
              </div>
              {fieldErrors.username && <p className="mt-1 text-xs text-red-500 dark:text-red-400">{fieldErrors.username}</p>}
              <p className="mt-1 text-xs text-slate-400">Letters, numbers, underscores, and dots. No spaces. Min 3 characters.</p>
            </div>

            {/* Phone Number */}
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">Phone Number <span className="text-slate-400">(optional)</span></label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">📞</span>
                <input
                  value={editForm.phoneNumber}
                  onChange={(e) => handleFieldChange('phoneNumber', e.target.value)}
                  onBlur={(e) => validateField('phoneNumber', e.target.value)}
                  placeholder="e.g. +919876543210 or 9876543210"
                  className={`input-field pl-10 ${fieldErrors.phoneNumber ? 'border-red-400 focus:border-red-500 focus:ring-red-200 dark:border-red-600' : ''}`}
                />
              </div>
              {fieldErrors.phoneNumber && <p className="mt-1 text-xs text-red-500 dark:text-red-400">{fieldErrors.phoneNumber}</p>}
              <p className="mt-1 text-xs text-slate-400">Indian: 10 digits or +91. International: +country code + number (10-15 digits).</p>
            </div>

            {/* Bio */}
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">Bio</label>
              <textarea
                value={editForm.bio}
                onChange={(e) => handleFieldChange('bio', e.target.value)}
                onBlur={(e) => validateField('bio', e.target.value)}
                placeholder="Bio (about you)"
                className={`input-field min-h-[100px] resize-y ${fieldErrors.bio ? 'border-red-400 focus:border-red-500 focus:ring-red-200 dark:border-red-600' : ''}`}
              />
              {fieldErrors.bio && <p className="mt-1 text-xs text-red-500 dark:text-red-400">{fieldErrors.bio}</p>}
              <p className="mt-1 text-xs text-slate-400">{editForm.bio?.length || 0}/1000 characters</p>
            </div>

            {/* Avatar URL */}
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700 dark:text-slate-300">Avatar URL</label>
              <div className="relative">
                <FileText size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
                <input value={editForm.avatarUrl} onChange={(e) => setEditForm({ ...editForm, avatarUrl: e.target.value })} placeholder="Avatar URL" className="input-field pl-10" />
              </div>
            </div>

            {/* Message */}
            {editMsg && (
              <div className={`flex items-center gap-2 rounded-xl border p-3 text-sm font-medium ${
                editMsgType === 'success'
                  ? 'border-green-200 bg-green-50 text-green-700 dark:border-green-800 dark:bg-green-950/30 dark:text-green-400'
                  : 'border-red-200 bg-red-50 text-red-700 dark:border-red-800 dark:bg-red-950/30 dark:text-red-400'
              }`}>
                {editMsgType === 'success' ? <CheckCircle size={16} /> : <AlertTriangle size={16} />}
                {editMsg}
              </div>
            )}

            <button disabled={editSaving || !isFormValid()} className={`btn-primary ${!isFormValid() ? 'opacity-50 cursor-not-allowed' : ''}`}>
              <Save size={16} /> {editSaving ? 'Saving...' : 'Save Changes'}
            </button>
          </form>
        )}

        {/* Change Password */}
        {tab === 'password' && (
          <form onSubmit={changePassword} className="card max-w-2xl space-y-5 p-8">
            <h2 className="font-display text-xl font-bold">Change Password</h2>
            {user?.provider !== 'LOCAL' && (
              <p className="text-sm text-amber-600 dark:text-amber-400">You signed in via {user?.provider}. Password change may not apply.</p>
            )}
            <div className="relative">
              <Lock size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
              <input type="password" value={pwForm.currentPassword} onChange={(e) => setPwForm({ ...pwForm, currentPassword: e.target.value })} placeholder="Current password" className="input-field pl-10" required />
            </div>
            <div className="relative">
              <Shield size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
              <input type="password" value={pwForm.newPassword} onChange={(e) => setPwForm({ ...pwForm, newPassword: e.target.value })} placeholder="New password" className="input-field pl-10" required minLength={8} />
            </div>
            {pwMsg && <p className="text-sm text-brand dark:text-brand-400">{pwMsg}</p>}
            <button disabled={pwSaving} className="btn-primary"><KeyRound size={16} /> {pwSaving ? 'Changing...' : 'Change Password'}</button>
          </form>
        )}

        {/* Billing */}
        {tab === 'billing' && (
          <div className="grid gap-6 lg:grid-cols-2">
            {/* Active Plan Info Card */}
            <div className="card p-6 lg:col-span-2">
              <h2 className="font-display text-xl font-bold flex items-center gap-2">
                <Crown size={20} /> Your Subscription
              </h2>
              {isPro ? (
                <div className="mt-4 rounded-xl border-2 border-brand bg-brand-50 p-5 dark:bg-brand-900/20 dark:border-brand-700">
                  <div className="flex items-center gap-3">
                    <CheckCircle size={24} className="text-brand" />
                    <div>
                      <p className="text-lg font-bold text-brand-800 dark:text-brand-300">
                        {isAuthorOrAdmin ? 'InkWell Author Plus' : 'InkWell Reader Pro'} — Active
                      </p>
                      <p className="text-sm text-slate-600 dark:text-slate-400">
                        Your subscription is active{user?.subscriptionEndDate ? ` until ${new Date(user.subscriptionEndDate).toLocaleDateString()}` : ''}.
                      </p>
                    </div>
                  </div>
                </div>
              ) : isExpired ? (
                <div className="mt-4 rounded-xl border-2 border-red-300 bg-red-50 p-5 dark:bg-red-900/20 dark:border-red-700">
                  <div className="flex items-center gap-3">
                    <AlertTriangle size={24} className="text-red-500" />
                    <div>
                      <p className="text-lg font-bold text-red-700 dark:text-red-400">Subscription Expired</p>
                      <p className="text-sm text-slate-600 dark:text-slate-400">Renew your plan below to regain access to premium features.</p>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-800/50">
                  <p className="text-lg font-bold">Free Plan</p>
                  <p className="text-sm text-slate-500 dark:text-slate-400">Subscribe to a plan below to unlock premium features.</p>
                </div>
              )}
            </div>

            <div className="card p-6">
              <h2 className="font-display text-xl font-bold">Plans</h2>
              <div className="mt-4 space-y-4">
                {availablePlans.map((plan) => {
                  let buttonText = 'Subscribe';
                  let isDisabled = processing === plan.id;
                  let isCurrent = false;

                  if (isPro) {
                    if (plan.id === 'reader-pro') {
                      if (isAuthorOrAdmin) {
                         buttonText = 'Included in Plus';
                         isDisabled = true;
                      } else {
                         buttonText = 'Current Plan';
                         isDisabled = true;
                         isCurrent = true;
                      }
                    } else if (plan.id === 'author-plus') {
                      if (isAuthorOrAdmin) {
                         buttonText = 'Current Plan';
                         isDisabled = true;
                         isCurrent = true;
                      } else {
                         buttonText = 'Upgrade';
                      }
                    }
                  } else if (isExpired) {
                    buttonText = 'Renew';
                  }

                  return (
                  <div key={plan.id} className={`flex items-center justify-between gap-4 rounded-xl border p-4 transition-colors ${isCurrent ? 'border-brand bg-brand-50 dark:bg-brand-900/20' : 'border-slate-200 dark:border-slate-700'}`}>
                    <div>
                      <p className="font-medium">{plan.purpose} {isCurrent && <span className="ml-2 text-xs font-bold text-brand">(Active)</span>}</p>
                      <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{plan.description}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-xl font-bold">₹{plan.amount}</p>
                      <button onClick={() => handlePayment(plan)} disabled={isDisabled} className={getPlanButtonClass(isDisabled, isCurrent, buttonText)}>
                        {processing === plan.id ? <><Loader2 size={14} className="animate-spin" /> Wait</> : buttonText}
                      </button>
                    </div>
                  </div>
                )})}
              </div>
              {payMsg && <p className="mt-3 text-sm text-brand dark:text-brand-400">{payMsg}</p>}
            </div>
            <div className="card p-6">
              <h2 className="font-display text-xl font-bold">Transaction History</h2>
              <div className="mt-4 space-y-3">
                {payments.length > 0 ? payments.map((p) => (
                  <div key={p.paymentOrderId} className="rounded-lg bg-slate-50 p-3 transition-colors dark:bg-slate-800/50">
                    <div className="flex items-center justify-between">
                      <p className="font-medium">{p.purpose}</p>
                      <p className="font-bold">{p.currency} {p.amount}</p>
                    </div>
                    <div className="mt-1 flex gap-2 text-xs text-slate-400"><span className="badge-brand">{p.status}</span></div>
                  </div>
                )) : <p className="text-sm text-slate-500 dark:text-slate-400">No transactions yet.</p>}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
