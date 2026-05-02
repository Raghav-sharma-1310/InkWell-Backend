/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import api from '../../api/client';
import { PageLoader } from '../../components/ui/LoadingSpinner';
import { Users, User, Calendar } from 'lucide-react';

// Provides author followers page wiring so the framework can apply the expected runtime behavior.
export function AuthorFollowersPage() {
  const [followers, setFollowers] = useState([]);
  const [followerDetails, setFollowerDetails] = useState({});
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      api.get('/api/posts/authors/me/followers').then((r) => r.data.data || []).catch(() => []),
      api.get('/api/posts/authors/me/followers/count').then((r) => r.data.data?.followersCount || 0).catch(() => 0),
    ]).then(([fList, fCount]) => {
      setFollowers(fList);
      setTotalCount(fCount);

      // Fetch user details for each follower
      const detailPromises = fList.map((f) =>
        api.get(`/api/auth/public/users/${f.followerId}`)
          .then((r) => ({ id: f.followerId, ...r.data.data }))
          .catch(() => ({ id: f.followerId, fullName: 'Unknown User', username: '', email: '' }))
      );
      return Promise.all(detailPromises);
    }).then((details) => {
      const detailMap = {};
      details.forEach((d) => { detailMap[d.id] = d; });
      setFollowerDetails(detailMap);
      setLoading(false);
    });
  }, []);

  if (loading) return <PageLoader />;

  return (
    <div className="mx-auto max-w-4xl">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-6 dark:border-slate-800">
        <div>
          <p className="page-heading">Dashboard</p>
          <div className="mt-2 flex items-center gap-3">
            <h1 className="font-display text-3xl font-bold">Followers</h1>
            <span className="flex h-7 w-auto min-w-[28px] items-center justify-center rounded-full bg-brand px-2 text-xs font-bold text-white">
              {totalCount}
            </span>
          </div>
        </div>
      </div>

      {/* Followers List */}
      <div className="mt-8 space-y-3">
        {followers.map((f) => {
          const detail = followerDetails[f.followerId] || {};
          return (
            <div
              key={f.followerId}
              className="card flex items-center gap-4 p-5 transition-all hover:shadow-md"
            >
              {/* Avatar */}
              {detail.avatarUrl ? (
                <img
                  src={detail.avatarUrl}
                  alt=""
                  className="h-12 w-12 rounded-xl object-cover ring-2 ring-slate-100 dark:ring-slate-800"
                />
              ) : (
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-50 text-lg font-bold text-brand-700 ring-2 ring-brand-100 dark:bg-brand-950 dark:text-brand-300 dark:ring-brand-800">
                  {detail.fullName?.[0]?.toUpperCase() || <User size={18} />}
                </div>
              )}

              {/* Info */}
              <div className="min-w-0 flex-1">
                <p className="font-semibold text-slate-900 dark:text-white">
                  {detail.fullName || 'Unknown User'}
                </p>
                {detail.username && (
                  <p className="text-sm text-slate-500 dark:text-slate-400">@{detail.username}</p>
                )}
              </div>

              {/* Follow Date */}
              <div className="flex items-center gap-1.5 text-xs text-slate-400 dark:text-slate-500">
                <Calendar size={13} />
                <span>
                  {f.followedAt
                    ? new Date(f.followedAt).toLocaleDateString('en-US', {
                        month: 'short', day: 'numeric', year: 'numeric'
                      })
                    : '—'}
                </span>
              </div>
            </div>
          );
        })}

        {followers.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-slate-50 dark:bg-slate-800/50">
              <Users size={24} className="text-slate-400" />
            </div>
            <p className="mt-4 text-base font-medium text-slate-600 dark:text-slate-300">No followers yet</p>
            <p className="mt-1 text-sm text-slate-500">Share your posts to grow your audience!</p>
          </div>
        )}
      </div>
    </div>
  );
}
