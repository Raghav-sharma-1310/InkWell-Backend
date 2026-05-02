/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { BarChart3, Eye, Heart, MessageSquare, FileText, TrendingUp } from 'lucide-react';
import api from '../../api/client';
import { PageLoader } from '../../components/ui/LoadingSpinner';

// Provides author analytics page wiring so the framework can apply the expected runtime behavior.
export function AuthorAnalyticsPage() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/api/posts/author', { params: { size: 50 } })
      .then((r) => setPosts(r.data.data.content || []))
      .catch(() => setPosts([]))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <PageLoader />;

  const totalViews = posts.reduce((s, p) => s + (p.viewCount || 0), 0);
  const totalLikes = posts.reduce((s, p) => s + (p.likesCount || 0), 0);
  const published = posts.filter((p) => p.status === 'PUBLISHED').length;
  const drafts = posts.filter((p) => p.status === 'DRAFT').length;

  const summaryCards = [
    { icon: <FileText size={20} />, label: 'Total Posts', value: posts.length, color: 'text-blue-500', bg: 'bg-blue-50 dark:bg-blue-950/30' },
    { icon: <TrendingUp size={20} />, label: 'Published', value: published, color: 'text-green-500', bg: 'bg-green-50 dark:bg-green-950/30' },
    { icon: <Eye size={20} />, label: 'Total Views', value: totalViews, color: 'text-purple-500', bg: 'bg-purple-50 dark:bg-purple-950/30' },
    { icon: <Heart size={20} />, label: 'Total Likes', value: totalLikes, color: 'text-rose-500', bg: 'bg-rose-50 dark:bg-rose-950/30' },
  ];

  // Sort posts by views descending for the table
  const sorted = [...posts].sort((a, b) => (b.viewCount || 0) - (a.viewCount || 0));

  return (
    <div>
      <p className="page-heading">Analytics</p>
      <h1 className="mt-2 font-display text-3xl font-bold">Content Performance</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Track how your posts are performing across the platform.</p>

      {/* Summary cards */}
      <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {summaryCards.map((c) => (
          <div key={c.label} className="stat-card">
            <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${c.bg} ${c.color}`}>{c.icon}</div>
            <p className="mt-3 text-3xl font-bold">{c.value}</p>
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{c.label}</p>
          </div>
        ))}
      </div>

      {/* Per-post breakdown */}
      <div className="mt-10">
        <h2 className="font-display text-xl font-bold">Per-Post Breakdown</h2>
        {sorted.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-700">
                  <th className="pb-3 font-medium text-slate-500 dark:text-slate-400">Post</th>
                  <th className="pb-3 text-center font-medium text-slate-500 dark:text-slate-400">Status</th>
                  <th className="pb-3 text-center font-medium text-slate-500 dark:text-slate-400"><Eye size={14} className="mx-auto" /></th>
                  <th className="pb-3 text-center font-medium text-slate-500 dark:text-slate-400"><Heart size={14} className="mx-auto" /></th>
                  <th className="pb-3 text-center font-medium text-slate-500 dark:text-slate-400">Read Time</th>
                </tr>
              </thead>
              <tbody>
                {sorted.map((post) => (
                  <tr key={post.postId} className="border-b border-slate-100 transition-colors hover:bg-slate-50 dark:border-slate-800 dark:hover:bg-slate-800/50">
                    <td className="max-w-[300px] truncate py-3 font-medium">{post.title}</td>
                    <td className="py-3 text-center">
                      <span className={`inline-block rounded-md px-2 py-0.5 text-xs font-medium ${
                        post.status === 'PUBLISHED' ? 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300' :
                        'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300'
                      }`}>{post.status}</span>
                    </td>
                    <td className="py-3 text-center font-medium">{post.viewCount || 0}</td>
                    <td className="py-3 text-center font-medium">{post.likesCount || 0}</td>
                    <td className="py-3 text-center text-slate-500 dark:text-slate-400">{post.readTimeMin}m</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="mt-6 flex items-center gap-4 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-8 dark:border-slate-700 dark:bg-slate-800/50">
            <BarChart3 size={32} className="text-slate-400" />
            <div>
              <p className="font-medium">No posts yet</p>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">Create and publish posts to see analytics here.</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
