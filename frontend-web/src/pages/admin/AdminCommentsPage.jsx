/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useState, useEffect } from 'react';
import { MessageSquare, Trash2, ChevronRight, RefreshCw, AlertTriangle } from 'lucide-react';
import api from '../../api/client';
import { PageLoader } from '../../components/ui/LoadingSpinner';

// Defines admin comments page so related behavior stays grouped in one place.
export function AdminCommentsPage() {
  const [posts, setPosts] = useState([]);
  const [loadingPosts, setLoadingPosts] = useState(true);
  const [selectedPost, setSelectedPost] = useState(null);
  
  const [comments, setComments] = useState([]);
  const [loadingComments, setLoadingComments] = useState(false);
  
  useEffect(() => {
    api.get('/api/posts/public?size=50')
      .then(res => setPosts(res.data.data.content || []))
      .catch(() => setPosts([]))
      .finally(() => setLoadingPosts(false));
  }, []);

  // Performs the load comments workflow so callers do not duplicate this logic.
  const loadComments = async (post) => {
    setSelectedPost(post);
    setLoadingComments(true);
    try {
      const res = await api.get(`/api/comments/public/post/${post.postId}`);
      setComments(res.data.data || []);
    } catch {
      setComments([]);
    } finally {
      setLoadingComments(false);
    }
  };

  // Performs the delete comment workflow so callers do not duplicate this logic.
  const deleteComment = async (commentId) => {
    if (!confirm('Are you sure you want to delete this comment?')) return;
    try {
      await api.patch(`/api/comments/admin/${commentId}/delete?postId=${selectedPost.postId}`);
      setComments(comments.map(c => c.commentId === commentId ? { ...c, status: 'DELETED' } : c));
    } catch (err) {
      alert('Failed to delete comment.');
    }
  };

  if (loadingPosts) return <PageLoader />;

  return (
    <div>
      <h1 className="font-display text-3xl font-bold">Moderate Comments</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Select a post to view and moderate its discussion thread.</p>
      
      <div className="mt-8 grid gap-6 lg:grid-cols-3">
        {/* Posts Sidebar */}
        <div className="card h-[calc(100vh-12rem)] overflow-y-auto p-4 lg:col-span-1">
          <h2 className="mb-4 font-display text-lg font-bold">Recent Posts</h2>
          <div className="space-y-2">
            {posts.map(p => (
              <button
                key={p.postId}
                onClick={() => loadComments(p)}
                className={`w-full flex items-center justify-between rounded-lg p-3 text-left transition-colors ${
                  selectedPost?.postId === p.postId
                    ? 'bg-brand text-white'
                    : 'bg-slate-50 hover:bg-slate-100 dark:bg-slate-800/50 dark:hover:bg-slate-800'
                }`}
              >
                <div className="truncate pr-2">
                  <p className="truncate text-sm font-medium">{p.title}</p>
                  <p className={`text-xs ${selectedPost?.postId === p.postId ? 'text-brand-100' : 'text-slate-500 dark:text-slate-400'}`}>
                    By {p.authorName}
                  </p>
                </div>
                <ChevronRight size={16} className="shrink-0 opacity-50" />
              </button>
            ))}
            {posts.length === 0 && <p className="text-sm text-slate-500">No posts found.</p>}
          </div>
        </div>

        {/* Comments Area */}
        <div className="card h-[calc(100vh-12rem)] overflow-y-auto p-6 lg:col-span-2">
          {!selectedPost ? (
            <div className="flex h-full flex-col items-center justify-center text-slate-400">
              <MessageSquare size={48} className="mb-4 opacity-20" />
              <p>Select a post from the left to view comments</p>
            </div>
          ) : loadingComments ? (
            <div className="flex h-full items-center justify-center">
              <RefreshCw size={24} className="animate-spin text-brand" />
            </div>
          ) : (
            <div>
              <div className="mb-6 border-b border-slate-100 pb-4 dark:border-slate-800">
                <h2 className="font-display text-xl font-bold">{selectedPost.title}</h2>
                <p className="text-sm text-slate-500 dark:text-slate-400">{comments.length} comments</p>
              </div>

              <div className="space-y-4">
                {comments.filter(c => c.status !== 'DELETED').length > 0 ? (
                  comments.map(c => {
                    if (c.status === 'DELETED') return null;
                    return (
                      <div key={c.commentId} className="rounded-xl border border-slate-100 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-800/50">
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex items-center gap-3">
                            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-200 text-xs font-bold text-slate-600 dark:bg-slate-700 dark:text-slate-300">
                              {c.authorName?.[0]?.toUpperCase() || '?'}
                            </div>
                            <div>
                              <p className="text-sm font-medium">{c.authorName}</p>
                              <p className="text-xs text-slate-500">{new Date(c.createdAt).toLocaleString()}</p>
                            </div>
                          </div>
                          <button
                            onClick={() => deleteComment(c.commentId)}
                            className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-500"
                            title="Delete Comment"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                        <p className="mt-3 text-sm text-slate-700 dark:text-slate-300">{c.content}</p>
                        {c.status === 'PENDING' && (
                          <div className="mt-3 inline-flex items-center gap-1 rounded-md bg-amber-50 px-2 py-1 text-xs font-medium text-amber-700 dark:bg-amber-900/30 dark:text-amber-400">
                            <AlertTriangle size={12} /> Pending Approval
                          </div>
                        )}
                      </div>
                    );
                  })
                ) : (
                  <p className="py-10 text-center text-slate-500">No active comments on this post.</p>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
