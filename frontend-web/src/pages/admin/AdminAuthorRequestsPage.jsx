/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import api from '../../api/client';
import { CheckCircle, XCircle, Clock, UserPlus, Loader2, Search } from 'lucide-react';

// Provides admin author requests page wiring so the framework can apply the expected runtime behavior.
export function AdminAuthorRequestsPage() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState('');
  const [filter, setFilter] = useState('ALL');
  const [remarkInputs, setRemarkInputs] = useState({});

  // Performs the fetch requests workflow so callers do not duplicate this logic.
  const fetchRequests = async () => {
    try {
      const res = await api.get('/api/admin/author-requests');
      setRequests(res.data.data || []);
    } catch {
      // silently handle
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchRequests(); }, []);

  // Defines handle action so related behavior stays grouped in one place.
  const handleAction = async (requestId, action) => {
    setActionLoading(`${requestId}-${action}`);
    try {
      await api.put(`/api/admin/author-requests/${requestId}/${action}`, {
        remarks: remarkInputs[requestId] || ''
      });
      await fetchRequests();
    } catch (err) {
      alert(err?.response?.data?.message || `Failed to ${action} request`);
    } finally {
      setActionLoading('');
    }
  };

  // Defines status icon so related behavior stays grouped in one place.
  const statusIcon = (status) => {
    switch (status) {
      case 'PENDING': return <Clock size={14} className="text-amber-500" />;
      case 'APPROVED': return <CheckCircle size={14} className="text-green-500" />;
      case 'REJECTED': return <XCircle size={14} className="text-red-500" />;
      default: return null;
    }
  };

  // Defines status badge so related behavior stays grouped in one place.
  const statusBadge = (status) => {
    const base = 'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold';
    switch (status) {
      case 'PENDING': return `${base} bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400`;
      case 'APPROVED': return `${base} bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400`;
      case 'REJECTED': return `${base} bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400`;
      default: return base;
    }
  };

  const filtered = filter === 'ALL' ? requests : requests.filter(r => r.status === filter);

  const counts = {
    ALL: requests.length,
    PENDING: requests.filter(r => r.status === 'PENDING').length,
    APPROVED: requests.filter(r => r.status === 'APPROVED').length,
    REJECTED: requests.filter(r => r.status === 'REJECTED').length,
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 size={32} className="animate-spin text-brand" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center gap-3">
        <UserPlus size={24} className="text-brand" />
        <div>
          <h1 className="font-display text-2xl font-bold">Author Requests</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">Manage reader-to-author upgrade requests</p>
        </div>
      </div>

      {/* Filter tabs */}
      <div className="mt-6 flex gap-1 overflow-x-auto rounded-xl border border-slate-200 bg-white p-1 dark:border-slate-800 dark:bg-slate-900">
        {['ALL', 'PENDING', 'APPROVED', 'REJECTED'].map(f => (
          <button key={f} onClick={() => setFilter(f)}
            className={`flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-all whitespace-nowrap ${
              filter === f ? 'bg-brand text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800'
            }`}>
            {f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase()}
            <span className={`rounded-full px-1.5 py-0.5 text-[10px] font-bold ${
              filter === f ? 'bg-white/20' : 'bg-slate-200 dark:bg-slate-700'
            }`}>{counts[f]}</span>
          </button>
        ))}
      </div>

      {/* Requests list */}
      <div className="mt-6 space-y-4">
        {filtered.length === 0 ? (
          <div className="card flex flex-col items-center justify-center py-12 text-center">
            <Search size={32} className="text-slate-300 dark:text-slate-600" />
            <p className="mt-3 font-medium text-slate-500 dark:text-slate-400">No {filter.toLowerCase()} requests found</p>
          </div>
        ) : filtered.map(req => (
          <div key={req.requestId} className="card overflow-hidden transition-all hover:shadow-glow">
            <div className="p-5">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-center gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-100 text-lg font-bold text-brand-700 dark:bg-brand-900 dark:text-brand-300">
                    {req.fullName?.[0]?.toUpperCase() || '?'}
                  </div>
                  <div>
                    <p className="font-semibold">{req.fullName}</p>
                    <p className="text-sm text-slate-500 dark:text-slate-400">@{req.username} · {req.email}</p>
                    <p className="mt-1 text-xs text-slate-400">Requested: {new Date(req.requestedAt).toLocaleString()}</p>
                  </div>
                </div>
                <span className={statusBadge(req.status)}>
                  {statusIcon(req.status)} {req.status}
                </span>
              </div>

              {req.adminRemarks && (
                <div className="mt-3 rounded-lg bg-slate-50 p-3 text-sm text-slate-600 dark:bg-slate-800/50 dark:text-slate-400">
                  <span className="font-medium">Admin remarks:</span> {req.adminRemarks}
                </div>
              )}

              {req.status === 'PENDING' && (
                <div className="mt-4 flex flex-col gap-3 border-t border-slate-100 pt-4 dark:border-slate-800">
                  <input
                    type="text"
                    placeholder="Add optional remarks..."
                    value={remarkInputs[req.requestId] || ''}
                    onChange={(e) => setRemarkInputs({ ...remarkInputs, [req.requestId]: e.target.value })}
                    className="input-field text-sm"
                  />
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleAction(req.requestId, 'approve')}
                      disabled={!!actionLoading}
                      className="flex items-center gap-2 rounded-xl bg-green-600 px-4 py-2 text-sm font-semibold text-white transition-all hover:bg-green-700 disabled:opacity-50"
                    >
                      {actionLoading === `${req.requestId}-approve` ? <Loader2 size={14} className="animate-spin" /> : <CheckCircle size={14} />}
                      Approve
                    </button>
                    <button
                      onClick={() => handleAction(req.requestId, 'reject')}
                      disabled={!!actionLoading}
                      className="flex items-center gap-2 rounded-xl bg-red-600 px-4 py-2 text-sm font-semibold text-white transition-all hover:bg-red-700 disabled:opacity-50"
                    >
                      {actionLoading === `${req.requestId}-reject` ? <Loader2 size={14} className="animate-spin" /> : <XCircle size={14} />}
                      Reject
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
