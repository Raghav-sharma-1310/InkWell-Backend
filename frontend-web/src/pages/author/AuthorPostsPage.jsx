/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Edit, Eye, Heart, Trash2 } from 'lucide-react';
import api from '../../api/client';
import { PageLoader, EmptyState } from '../../components/ui/LoadingSpinner';

// Provides author posts page wiring so the framework can apply the expected runtime behavior.
export function AuthorPostsPage() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const loadPosts = useCallback(() => {
    setLoading(true);
    api.get('/api/posts/author', { params: { size: 50 } })
      .then((r) => setPosts(r.data.data.content || []))
      .catch(() => setPosts([]))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { loadPosts(); }, [loadPosts]);

  // Performs the delete post workflow so callers do not duplicate this logic.
  const deletePost = async (postId) => {
    if (!window.confirm('Are you sure you want to delete this post?')) return;
    try {
      await api.delete(`/api/posts/author/${postId}`);
      loadPosts();
    } catch { /* silently handle */ }
  };

  if (loading) return <PageLoader />;

  return (
    <div>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-3xl font-bold">My Posts</h1>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{posts.length} post{posts.length !== 1 ? 's' : ''} total</p>
        </div>
        <Link to="/author/posts/new" className="btn-primary"><Plus size={16} /> New Post</Link>
      </div>
      <div className="mt-8 space-y-3">
        {posts.map((post) => (
          <div key={post.postId} className="flex items-center justify-between gap-4 rounded-xl border border-slate-100 bg-slate-50 p-4 transition-colors dark:border-slate-800 dark:bg-slate-800/50">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <span className={`badge text-xs ${post.status === 'PUBLISHED' ? 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300' : post.status === 'DRAFT' ? 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300' : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'}`}>
                  {post.status}
                </span>
                {post.featured && <span className="badge bg-purple-100 text-xs text-purple-700 dark:bg-purple-950 dark:text-purple-300">Featured</span>}
                {post.categorySlug && <span className="text-xs text-slate-400">{post.categorySlug}</span>}
              </div>
              <h2 className="mt-1 truncate font-display text-lg font-semibold">{post.title}</h2>
              {post.excerpt && <p className="mt-1 truncate text-sm text-slate-500 dark:text-slate-400">{post.excerpt}</p>}
              <div className="mt-2 flex gap-4 text-xs text-slate-400 dark:text-slate-500">
                <span className="flex items-center gap-1"><Eye size={12} /> {post.viewCount || 0}</span>
                <span className="flex items-center gap-1"><Heart size={12} /> {post.likesCount || 0}</span>
                <span>{post.readTimeMin || 0} min read</span>
              </div>
            </div>
            <div className="flex shrink-0 gap-2">
              <Link to={`/author/posts/${post.postId}/edit`} className="btn-ghost"><Edit size={16} /></Link>
              <button onClick={() => deletePost(post.postId)} className="btn-ghost text-red-500 hover:bg-red-50 dark:hover:bg-red-950/30"><Trash2 size={16} /></button>
            </div>
          </div>
        ))}
        {posts.length === 0 && (
          <EmptyState title="No posts yet" description="Create your first post to see it here." />
        )}
      </div>
    </div>
  );
}
