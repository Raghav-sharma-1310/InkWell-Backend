/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { FileText, Eye, Heart, Edit, Star, BarChart3, Loader2 } from 'lucide-react';
import api from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { PageLoader } from '../../components/ui/LoadingSpinner';

// Provides author overview page wiring so the framework can apply the expected runtime behavior.
export function AuthorOverviewPage() {
  const { user } = useAuth();
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
  const drafts = posts.filter((p) => p.status === 'DRAFT').length;
  const published = posts.filter((p) => p.status === 'PUBLISHED').length;
  const featured = posts.filter((p) => p.featured).length;

  const stats = [
    { icon: <FileText size={18} />, label: 'Total Posts', value: posts.length, color: 'text-blue-500' },
    { icon: <Edit size={18} />, label: 'Drafts', value: drafts, color: 'text-amber-500' },
    { icon: <BarChart3 size={18} />, label: 'Published', value: published, color: 'text-green-500' },
    { icon: <Eye size={18} />, label: 'Total Views', value: totalViews, color: 'text-purple-500' },
    { icon: <Heart size={18} />, label: 'Total Likes', value: totalLikes, color: 'text-rose-500' },
    { icon: <Star size={18} />, label: 'Featured', value: featured, color: 'text-brand' },
  ];

  return (
    <div>
      <p className="page-heading">Author dashboard</p>
      <h1 className="mt-2 font-display text-3xl font-bold">Welcome, {user?.fullName}</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">All metrics are computed from your real posts in the database.</p>
      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {stats.map((s) => (
          <div key={s.label} className="stat-card">
            <div className={s.color}>{s.icon}</div>
            <p className="mt-3 text-2xl font-bold">{s.value}</p>
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{s.label}</p>
          </div>
        ))}
      </div>

      {/* Recent posts preview */}
      {posts.length > 0 && (
        <div className="mt-8">
          <h2 className="font-display text-xl font-bold">Recent Posts</h2>
          <div className="mt-4 space-y-3">
            {posts.slice(0, 5).map((post) => (
              <div key={post.postId} className="flex items-center justify-between gap-4 rounded-xl border border-slate-100 bg-slate-50 p-4 transition-colors dark:border-slate-800 dark:bg-slate-800/50">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className={`badge text-xs ${post.status === 'PUBLISHED' ? 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300' : 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300'}`}>
                      {post.status}
                    </span>
                  </div>
                  <p className="mt-1 truncate font-medium">{post.title}</p>
                </div>
                <div className="flex gap-4 text-xs text-slate-400 dark:text-slate-500">
                  <span className="flex items-center gap-1"><Eye size={12} /> {post.viewCount || 0}</span>
                  <span className="flex items-center gap-1"><Heart size={12} /> {post.likesCount || 0}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
