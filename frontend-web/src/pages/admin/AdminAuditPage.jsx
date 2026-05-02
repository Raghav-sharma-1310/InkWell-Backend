/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { ScrollText } from 'lucide-react';
import api from '../../api/client';
import { PageLoader } from '../../components/ui/LoadingSpinner';

// Defines admin audit page so related behavior stays grouped in one place.
export function AdminAuditPage() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/api/notifications/admin/audit-logs')
      .then((r) => setLogs(r.data.data))
      .catch(() => setLogs([]))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <PageLoader />;

  return (
    <div>
      <h1 className="font-display text-3xl font-bold">Audit Logs</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{logs.length} log entries</p>
      <div className="mt-8 space-y-3">
        {logs.map((log) => (
          <div key={log.auditId} className="flex items-start gap-3 rounded-xl border border-slate-100 bg-slate-50 p-4 transition-colors dark:border-slate-800 dark:bg-slate-800/50">
            <ScrollText size={16} className="mt-0.5 shrink-0 text-slate-400" />
            <div>
              <p className="text-xs text-slate-400 dark:text-slate-500">{log.source}</p>
              <p className="mt-1 font-medium">{log.action}</p>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{log.details}</p>
            </div>
          </div>
        ))}
        {logs.length === 0 && <p className="py-10 text-center text-slate-500 dark:text-slate-400">No audit logs recorded yet.</p>}
      </div>
    </div>
  );
}
