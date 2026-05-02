/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { Shield, ShieldOff, Trash2, UserCheck, UserX, Search, Crown } from 'lucide-react';
import api from '../../api/client';
import { PageLoader } from '../../components/ui/LoadingSpinner';

// Defines admin users page so related behavior stays grouped in one place.
export function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  // Performs the load workflow so callers do not duplicate this logic.
  const load = () =>
    api.get('/api/auth/admin/users').then((r) => setUsers(r.data.data)).catch(() => setUsers([]));

  useEffect(() => { load().finally(() => setLoading(false)); }, []);

  // Defines promote so related behavior stays grouped in one place.
  const promote = async (userId, role) => {
    await api.patch(`/api/auth/admin/users/${userId}/role`, { role });
    await load();
  };

  // Defines suspend so related behavior stays grouped in one place.
  const suspend = async (userId) => {
    await api.patch(`/api/auth/admin/users/${userId}/suspend`);
    await load();
  };

  // Defines reactivate so related behavior stays grouped in one place.
  const reactivate = async (userId) => {
    await api.patch(`/api/auth/admin/users/${userId}/reactivate`);
    await load();
  };

  // Performs the delete user workflow so callers do not duplicate this logic.
  const deleteUser = async (userId, email) => {
    if (!confirm(`Are you sure you want to permanently delete ${email}?`)) return;
    await api.delete(`/api/auth/admin/users/${userId}`);
    await load();
  };

  if (loading) return <PageLoader />;

  const filteredUsers = users.filter(
    (u) =>
      u.fullName?.toLowerCase().includes(search.toLowerCase()) ||
      u.email?.toLowerCase().includes(search.toLowerCase()) ||
      u.username?.toLowerCase().includes(search.toLowerCase())
  );

  const totalActive = users.filter((u) => u.active).length;
  const totalSuspended = users.filter((u) => !u.active).length;

  return (
    <div>
      <h1 className="font-display text-3xl font-bold">Manage Users</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
        {users.length} total · {totalActive} active · {totalSuspended} suspended
      </p>

      {/* Search */}
      <div className="relative mt-6">
        <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search users by name, email, or username..."
          className="input-field pl-11"
        />
      </div>

      {/* Stats Bar */}
      <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
        {[
          { label: 'Total', value: users.length, color: 'text-blue-500' },
          { label: 'Authors', value: users.filter((u) => u.role === 'AUTHOR').length, color: 'text-green-500' },
          { label: 'Readers', value: users.filter((u) => u.role === 'READER').length, color: 'text-purple-500' },
          { label: 'Admins', value: users.filter((u) => u.role === 'ADMIN').length, color: 'text-amber-500' },
        ].map((s) => (
          <div key={s.label} className="rounded-xl border border-slate-100 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-800/50">
            <p className={`text-2xl font-bold ${s.color}`}>{s.value}</p>
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{s.label}</p>
          </div>
        ))}
      </div>

      {/* User Table */}
      <div className="mt-8 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900/50">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-600 dark:text-slate-300">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500 dark:bg-slate-800/80 dark:text-slate-400">
              <tr>
                <th className="px-6 py-4 font-semibold">User</th>
                <th className="px-6 py-4 font-semibold">Role</th>
                <th className="px-6 py-4 font-semibold">Status</th>
                <th className="px-6 py-4 text-right font-semibold">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
              {filteredUsers.map((u) => (
                <tr key={u.userId} className={`transition-colors hover:bg-slate-50 dark:hover:bg-slate-800/50 ${!u.active ? 'bg-red-50/30 dark:bg-red-900/10' : ''}`}>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-brand-100 text-sm font-bold text-brand-700 dark:bg-brand-900/50 dark:text-brand-300">
                        {u.fullName?.[0]?.toUpperCase() || '?'}
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-medium text-slate-900 dark:text-white">{u.fullName}</span>
                          {u.subscriptionTier === 'PRO' && u.subscriptionStatus === 'ACTIVE' && (
                            <span className="inline-flex items-center gap-0.5 rounded-md bg-yellow-100 px-1.5 py-0.5 text-[10px] font-bold text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400">
                              <Crown size={10} /> PRO
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-slate-500">
                          {u.email} · @{u.username}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="inline-flex rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                      {u.role}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    {u.active ? (
                      <span className="inline-flex items-center gap-1 rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-700 dark:bg-green-900/30 dark:text-green-400">
                        <span className="h-1.5 w-1.5 rounded-full bg-green-500"></span> Active
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-700 dark:bg-red-900/30 dark:text-red-400">
                        <span className="h-1.5 w-1.5 rounded-full bg-red-500"></span> Suspended
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex items-center justify-end gap-2">
                      {u.role !== 'ADMIN' && (
                        <>
                          <button onClick={() => promote(u.userId, u.role === 'AUTHOR' ? 'READER' : 'AUTHOR')} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-brand-600 dark:hover:bg-slate-800 dark:hover:text-brand-400" title={u.role === 'AUTHOR' ? 'Demote to Reader' : 'Promote to Author'}>
                            <Shield size={16} />
                          </button>
                          <button onClick={() => promote(u.userId, 'ADMIN')} className="rounded-lg p-2 text-slate-400 hover:bg-amber-50 hover:text-amber-600 dark:hover:bg-amber-900/20 dark:hover:text-amber-500" title="Make Admin">
                            <ShieldOff size={16} />
                          </button>
                          
                          {u.active ? (
                            <button onClick={() => suspend(u.userId)} className="rounded-lg p-2 text-slate-400 hover:bg-orange-50 hover:text-orange-600 dark:hover:bg-orange-900/20 dark:hover:text-orange-500" title="Suspend">
                              <UserX size={16} />
                            </button>
                          ) : (
                            <button onClick={() => reactivate(u.userId)} className="rounded-lg p-2 text-slate-400 hover:bg-green-50 hover:text-green-600 dark:hover:bg-green-900/20 dark:hover:text-green-500" title="Reactivate">
                              <UserCheck size={16} />
                            </button>
                          )}

                          <button onClick={() => deleteUser(u.userId, u.email)} className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-500" title="Delete">
                            <Trash2 size={16} />
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {filteredUsers.length === 0 && (
                <tr>
                  <td colSpan="4" className="px-6 py-10 text-center text-slate-500 dark:text-slate-400">
                    No users found matching your search.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
