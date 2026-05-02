/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { Star, StarOff, Trash2, AlertTriangle, X, Eye, Clock, Heart } from 'lucide-react';
import api from '../../api/client';
import { PageLoader } from '../../components/ui/LoadingSpinner';

// Defines admin posts page so related behavior stays grouped in one place.
export function AdminPostsPage() {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // Performs the load workflow so callers do not duplicate this logic.
  const load = () =>
    api.get('/api/posts/admin').then((r) => setPosts(r.data.data.content)).catch(() => setPosts([]));

  useEffect(() => {
    load().finally(() => setLoading(false));
  }, []);

  // Defines toggle feature so related behavior stays grouped in one place.
  const toggleFeature = async (post) => {
    await api.patch(`/api/posts/admin/${post.postId}/feature`, null, { params: { featured: !post.featured } });
    await load();
  };

  // Performs the confirm delete workflow so callers do not duplicate this logic.
  const confirmDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await api.delete(`/api/posts/admin/${deleteTarget.postId}`);
      setPosts((prev) => prev.filter((p) => p.postId !== deleteTarget.postId));
    } catch (err) {
      console.error('Failed to delete post:', err);
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  };

  if (loading) return <PageLoader />;

  return (
    <div>
      <h1 className="font-display text-3xl font-bold">Manage Posts</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
        {posts.length} post{posts.length !== 1 ? 's' : ''} total. Feature or delete any post.
      </p>

      <div className="mt-8 space-y-3">
        {posts.map((post) => (
          <div
            key={post.postId}
            className="flex items-center justify-between gap-4 rounded-xl border border-slate-100 bg-slate-50 p-4 transition-colors dark:border-slate-800 dark:bg-slate-800/50"
          >
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <span
                  className={`badge text-xs ${
                    post.status === 'PUBLISHED'
                      ? 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300'
                      : post.status === 'DRAFT'
                        ? 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300'
                        : 'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300'
                  }`}
                >
                  {post.status}
                </span>
                {post.visibility === 'PREMIUM' && (
                  <span className="badge bg-purple-100 text-xs text-purple-700 dark:bg-purple-950 dark:text-purple-300">
                    Premium
                  </span>
                )}
                {post.featured && (
                  <span className="badge bg-amber-100 text-xs text-amber-700 dark:bg-amber-950 dark:text-amber-300">
                    ★ Featured
                  </span>
                )}
              </div>
              <h2 className="mt-1 truncate font-display text-lg font-semibold">{post.title}</h2>
              <div className="mt-1 flex flex-wrap items-center gap-3 text-xs text-slate-500 dark:text-slate-400">
                {post.viewCount != null && (
                  <span className="flex items-center gap-1">
                    <Eye size={12} /> {post.viewCount}
                  </span>
                )}
                {post.likesCount != null && (
                  <span className="flex items-center gap-1">
                    <Heart size={12} /> {post.likesCount}
                  </span>
                )}
                {post.readTimeMin != null && (
                  <span className="flex items-center gap-1">
                    <Clock size={12} /> {post.readTimeMin} min
                  </span>
                )}
                {post.categorySlug && (
                  <span className="rounded bg-slate-200 px-1.5 py-0.5 dark:bg-slate-700">{post.categorySlug}</span>
                )}
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => toggleFeature(post)}
                className={`btn-ghost text-xs ${post.featured ? 'text-amber-600' : ''}`}
                title={post.featured ? 'Unfeature' : 'Feature'}
              >
                {post.featured ? (
                  <>
                    <StarOff size={14} /> Unfeature
                  </>
                ) : (
                  <>
                    <Star size={14} /> Feature
                  </>
                )}
              </button>
              <button
                onClick={() => setDeleteTarget(post)}
                className="btn-ghost text-xs text-red-600 hover:bg-red-50 hover:text-red-700 dark:text-red-400 dark:hover:bg-red-950/30 dark:hover:text-red-300"
                title="Delete post"
              >
                <Trash2 size={14} /> Delete
              </button>
            </div>
          </div>
        ))}
        {posts.length === 0 && (
          <p className="py-10 text-center text-slate-500 dark:text-slate-400">No posts yet.</p>
        )}
      </div>

      {/* ─── Delete Confirmation Modal ─── */}
      {deleteTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
          <div className="mx-4 w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-700 dark:bg-slate-900">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-red-100 dark:bg-red-950/50">
                <AlertTriangle size={20} className="text-red-600 dark:text-red-400" />
              </div>
              <div className="flex-1">
                <h3 className="font-display text-lg font-bold text-slate-900 dark:text-white">Delete Post</h3>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                  Are you sure you want to permanently delete{' '}
                  <strong className="text-slate-900 dark:text-white">"{deleteTarget.title}"</strong>?
                </p>
                <p className="mt-2 text-xs text-red-600 dark:text-red-400">
                  This will also remove all likes, bookmarks, comments, and reading history for this post. This action
                  cannot be undone.
                </p>
              </div>
              <button
                onClick={() => setDeleteTarget(null)}
                className="text-slate-400 transition-colors hover:text-slate-600 dark:hover:text-slate-300"
              >
                <X size={18} />
              </button>
            </div>

            <div className="mt-6 flex gap-3">
              <button
                onClick={() => setDeleteTarget(null)}
                disabled={deleting}
                className="btn-secondary flex-1 justify-center py-2.5"
              >
                Cancel
              </button>
              <button
                onClick={confirmDelete}
                disabled={deleting}
                className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-red-600 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-red-700 disabled:opacity-50"
              >
                {deleting ? (
                  'Deleting...'
                ) : (
                  <>
                    <Trash2 size={14} /> Delete Post
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
