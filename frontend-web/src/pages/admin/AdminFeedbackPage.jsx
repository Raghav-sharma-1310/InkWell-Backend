/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import api from '../../api/client';
import { Bug, Loader2, Clock, CheckCircle, AlertTriangle, Send, ChevronDown, ChevronUp, Search } from 'lucide-react';

// Defines admin feedback page so related behavior stays grouped in one place.
export function AdminFeedbackPage() {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL');
  const [expandedId, setExpandedId] = useState(null);
  const [replyInputs, setReplyInputs] = useState({});
  const [actionLoading, setActionLoading] = useState('');

  // Performs the fetch reports workflow so callers do not duplicate this logic.
  const fetchReports = async () => {
    try {
      const res = await api.get('/api/admin/feedback');
      setReports(res.data.data || []);
    } catch {
      // silently handle
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchReports(); }, []);

  // Performs the handle status change workflow so callers do not duplicate this logic.
  const handleStatusChange = async (reportId, newStatus) => {
    setActionLoading(`status-${reportId}`);
    try {
      await api.put(`/api/admin/feedback/${reportId}/status`, { status: newStatus });
      await fetchReports();
    } catch (err) {
      alert(err?.response?.data?.message || 'Failed to update status');
    } finally {
      setActionLoading('');
    }
  };

  // Defines handle reply so related behavior stays grouped in one place.
  const handleReply = async (reportId) => {
    const message = replyInputs[reportId]?.trim();
    if (!message) return;
    setActionLoading(`reply-${reportId}`);
    try {
      await api.post(`/api/admin/feedback/${reportId}/reply`, { message });
      setReplyInputs({ ...replyInputs, [reportId]: '' });
      await fetchReports();
    } catch (err) {
      alert(err?.response?.data?.message || 'Failed to send reply');
    } finally {
      setActionLoading('');
    }
  };

  // Defines status icon so related behavior stays grouped in one place.
  const statusIcon = (status) => {
    switch (status) {
      case 'OPEN': return <AlertTriangle size={14} className="text-amber-500" />;
      case 'IN_PROGRESS': return <Clock size={14} className="text-blue-500" />;
      case 'RESOLVED': return <CheckCircle size={14} className="text-green-500" />;
      default: return null;
    }
  };

  // Defines status badge so related behavior stays grouped in one place.
  const statusBadge = (status) => {
    const base = 'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold';
    switch (status) {
      case 'OPEN': return `${base} bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400`;
      case 'IN_PROGRESS': return `${base} bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400`;
      case 'RESOLVED': return `${base} bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400`;
      default: return base;
    }
  };

  const filtered = filter === 'ALL' ? reports : reports.filter(r => r.status === filter);

  const counts = {
    ALL: reports.length,
    OPEN: reports.filter(r => r.status === 'OPEN').length,
    IN_PROGRESS: reports.filter(r => r.status === 'IN_PROGRESS').length,
    RESOLVED: reports.filter(r => r.status === 'RESOLVED').length,
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
        <Bug size={24} className="text-brand" />
        <div>
          <h1 className="font-display text-2xl font-bold">Bug Reports & Feedback</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">Review and respond to user feedback</p>
        </div>
      </div>

      {/* Filter tabs */}
      <div className="mt-6 flex gap-1 overflow-x-auto rounded-xl border border-slate-200 bg-white p-1 dark:border-slate-800 dark:bg-slate-900">
        {['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED'].map(f => (
          <button key={f} onClick={() => setFilter(f)}
            className={`flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-all whitespace-nowrap ${
              filter === f ? 'bg-brand text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800'
            }`}>
            {f === 'ALL' ? 'All' : f.replace('_', ' ').split(' ').map(w => w.charAt(0) + w.slice(1).toLowerCase()).join(' ')}
            <span className={`rounded-full px-1.5 py-0.5 text-[10px] font-bold ${
              filter === f ? 'bg-white/20' : 'bg-slate-200 dark:bg-slate-700'
            }`}>{counts[f]}</span>
          </button>
        ))}
      </div>

      {/* Reports list */}
      <div className="mt-6 space-y-4">
        {filtered.length === 0 ? (
          <div className="card flex flex-col items-center justify-center py-12 text-center">
            <Search size={32} className="text-slate-300 dark:text-slate-600" />
            <p className="mt-3 font-medium text-slate-500 dark:text-slate-400">No {filter.toLowerCase().replace('_', ' ')} reports found</p>
          </div>
        ) : filtered.map(report => {
          const isExpanded = expandedId === report.reportId;
          return (
            <div key={report.reportId} className="card overflow-hidden transition-all hover:shadow-glow">
              {/* Header */}
              <button
                onClick={() => setExpandedId(isExpanded ? null : report.reportId)}
                className="flex w-full items-center justify-between p-5 text-left"
              >
                <div className="flex items-center gap-4">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-100 text-sm font-bold text-brand-700 dark:bg-brand-900 dark:text-brand-300">
                    {report.fullName?.[0]?.toUpperCase() || '?'}
                  </div>
                  <div>
                    <p className="font-semibold">{report.fullName}</p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      @{report.username} · {report.messages?.length || 0} message(s) · {new Date(report.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className={statusBadge(report.status)}>
                    {statusIcon(report.status)} {report.status.replace('_', ' ')}
                  </span>
                  {isExpanded ? <ChevronUp size={16} className="text-slate-400" /> : <ChevronDown size={16} className="text-slate-400" />}
                </div>
              </button>

              {/* Expanded content */}
              {isExpanded && (
                <div className="border-t border-slate-100 dark:border-slate-800">
                  {/* Page URL */}
                  {report.pageUrl && (
                    <div className="bg-slate-50 px-5 py-2 text-xs text-slate-500 dark:bg-slate-800/50 dark:text-slate-400">
                      Page: <span className="font-mono">{report.pageUrl}</span>
                    </div>
                  )}

                  {/* Chat messages */}
                  <div className="max-h-80 space-y-3 overflow-y-auto p-5">
                    {report.messages?.map(msg => (
                      <div key={msg.messageId} className={`flex ${msg.senderRole === 'ADMIN' ? 'justify-end' : 'justify-start'}`}>
                        <div className={`max-w-[80%] rounded-2xl px-4 py-3 ${
                          msg.senderRole === 'ADMIN'
                            ? 'bg-brand text-white rounded-br-md'
                            : 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-200 rounded-bl-md'
                        }`}>
                          <p className="text-xs font-semibold opacity-70">{msg.senderName} · {msg.senderRole}</p>
                          <p className="mt-1 text-sm">{msg.content}</p>
                          <p className="mt-1 text-[10px] opacity-50">{new Date(msg.sentAt).toLocaleString()}</p>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Actions */}
                  <div className="border-t border-slate-100 p-5 dark:border-slate-800">
                    {/* Status actions */}
                    <div className="mb-4 flex flex-wrap gap-2">
                      <span className="text-xs font-medium text-slate-500 dark:text-slate-400 self-center mr-2">Change status:</span>
                      {['OPEN', 'IN_PROGRESS', 'RESOLVED'].filter(s => s !== report.status).map(s => (
                        <button
                          key={s}
                          onClick={() => handleStatusChange(report.reportId, s)}
                          disabled={!!actionLoading}
                          className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium transition-all hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-800 disabled:opacity-50"
                        >
                          {actionLoading === `status-${report.reportId}` ? <Loader2 size={12} className="animate-spin inline" /> : null}
                          {' '}{s.replace('_', ' ')}
                        </button>
                      ))}
                    </div>

                    {/* Reply input */}
                    <div className="flex gap-2">
                      <input
                        type="text"
                        placeholder="Type your reply..."
                        value={replyInputs[report.reportId] || ''}
                        onChange={(e) => setReplyInputs({ ...replyInputs, [report.reportId]: e.target.value })}
                        onKeyDown={(e) => e.key === 'Enter' && handleReply(report.reportId)}
                        className="input-field flex-1 text-sm"
                      />
                      <button
                        onClick={() => handleReply(report.reportId)}
                        disabled={!replyInputs[report.reportId]?.trim() || !!actionLoading}
                        className="btn-primary flex items-center gap-2 text-sm disabled:opacity-50"
                      >
                        {actionLoading === `reply-${report.reportId}` ? <Loader2 size={14} className="animate-spin" /> : <Send size={14} />}
                        Reply
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
