/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import api from '../../api/client';
import { PageLoader } from '../../components/ui/LoadingSpinner';
import { MessageSquare, CheckCircle, XCircle, ChevronDown, ChevronUp, User, Reply, Send, Shield } from 'lucide-react';

// Provides author comments page wiring so the framework can apply the expected runtime behavior.
export function AuthorCommentsPage() {
  const [posts, setPosts] = useState([]);
  const [commentsMap, setCommentsMap] = useState({});
  const [expandedPost, setExpandedPost] = useState(null);
  const [loading, setLoading] = useState(true);
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyContent, setReplyContent] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    api.get('/api/posts/author', { params: { size: 50 } })
      .then((r) => setPosts(r.data.data.content || []))
      .catch(() => setPosts([]))
      .finally(() => setLoading(false));
  }, []);

  // Defines toggle post so related behavior stays grouped in one place.
  const togglePost = async (postId) => {
    if (expandedPost === postId) {
      setExpandedPost(null);
      return;
    }
    setExpandedPost(postId);
    if (!commentsMap[postId]) {
      try {
        const r = await api.get(`/api/comments/public/post/${postId}`);
        setCommentsMap((prev) => ({ ...prev, [postId]: r.data.data || [] }));
      } catch {
        setCommentsMap((prev) => ({ ...prev, [postId]: [] }));
      }
    }
  };

  // Performs the refresh comments workflow so callers do not duplicate this logic.
  const refreshComments = async (postId) => {
    try {
      const r = await api.get(`/api/comments/public/post/${postId}`);
      setCommentsMap((prev) => ({ ...prev, [postId]: r.data.data || [] }));
    } catch {}
  };

  // Defines moderate so related behavior stays grouped in one place.
  const moderate = async (commentId, postId, action) => {
    try {
      await api.patch(`/api/comments/author/${commentId}/${action}`, null, { params: { postId } });
      await refreshComments(postId);
    } catch {
      // silently handle
    }
  };

  // Defines submit reply so related behavior stays grouped in one place.
  const submitReply = async (commentId, postId) => {
    if (!replyContent.trim()) return;
    setSubmitting(true);
    try {
      await api.post(`/api/comments/${commentId}/reply`, { content: replyContent });
      await refreshComments(postId);
      setReplyContent('');
      setReplyingTo(null);
    } catch {
      // silently handle
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <PageLoader />;

  return (
    <div>
      <p className="page-heading">Moderation</p>
      <h1 className="mt-2 font-display text-3xl font-bold">Comment Moderation</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
        Click on a post to view and moderate its comments. Approve, reject, or reply as the author.
      </p>

      <div className="mt-8 space-y-3">
        {posts.map((post) => {
          const isExpanded = expandedPost === post.postId;
          const comments = commentsMap[post.postId] || [];
          const topLevel = comments.filter((c) => !c.parentCommentId);
          const replies = comments.filter((c) => c.parentCommentId);
          const replyMap = {};
          replies.forEach((r) => {
            if (!replyMap[r.parentCommentId]) replyMap[r.parentCommentId] = [];
            replyMap[r.parentCommentId].push(r);
          });

          return (
            <div key={post.postId} className="overflow-hidden rounded-xl border border-slate-100 bg-white transition-colors dark:border-slate-800 dark:bg-slate-900">
              <button
                onClick={() => togglePost(post.postId)}
                className="flex w-full items-center justify-between gap-3 p-4 text-left transition-colors hover:bg-slate-50 dark:hover:bg-slate-800/50"
              >
                <div className="flex items-center gap-3">
                  <MessageSquare size={18} className="shrink-0 text-slate-400" />
                  <div>
                    <p className="font-medium">{post.title}</p>
                    <div className="mt-1 flex items-center gap-2">
                      <span className={`inline-block rounded-md px-2 py-0.5 text-xs font-medium ${
                        post.status === 'PUBLISHED' ? 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300' :
                        'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300'
                      }`}>{post.status}</span>
                      <span className="text-xs text-slate-400">{comments.length} comments</span>
                    </div>
                  </div>
                </div>
                {isExpanded ? <ChevronUp size={18} className="text-slate-400" /> : <ChevronDown size={18} className="text-slate-400" />}
              </button>

              {isExpanded && (
                <div className="border-t border-slate-100 bg-slate-50/50 p-4 dark:border-slate-800 dark:bg-slate-800/30">
                  {topLevel.length > 0 ? (
                    <div className="space-y-3">
                      {topLevel.map((comment) => (
                        <div key={comment.commentId}>
                          {/* Parent comment */}
                          <div className="flex items-start justify-between gap-4 rounded-lg border border-slate-200 bg-white p-3 dark:border-slate-700 dark:bg-slate-900">
                            <div className="min-w-0 flex-1">
                              <div className="flex items-center gap-2">
                                <div className={`flex h-7 w-7 items-center justify-center rounded-lg text-xs font-bold ${
                                  comment.isPostAuthor
                                    ? 'bg-brand-100 text-brand-700 ring-1 ring-brand-300 dark:bg-brand-900 dark:text-brand-300'
                                    : 'bg-slate-100 text-slate-500 dark:bg-slate-700 dark:text-slate-400'
                                }`}>
                                  {comment.isPostAuthor ? <Shield size={12} /> : <User size={12} />}
                                </div>
                                <span className="text-sm font-medium">{comment.authorName || 'User'}</span>
                                {comment.isPostAuthor && (
                                  <span className="inline-flex items-center gap-1 rounded-md bg-brand-100 px-1.5 py-0.5 text-xs font-semibold text-brand-700 dark:bg-brand-900 dark:text-brand-300">
                                    <Shield size={9} /> Author
                                  </span>
                                )}
                                <span className={`rounded px-1.5 py-0.5 text-xs font-medium ${
                                  comment.status === 'APPROVED' ? 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300' :
                                  comment.status === 'REJECTED' ? 'bg-red-100 text-red-700 dark:bg-red-950 dark:text-red-300' :
                                  'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300'
                                }`}>{comment.status}</span>
                              </div>
                              <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{comment.content}</p>
                              {comment.createdAt && (
                                <p className="mt-1 text-xs text-slate-400">{new Date(comment.createdAt).toLocaleString()}</p>
                              )}
                            </div>
                            <div className="flex shrink-0 gap-1">
                              <button
                                onClick={() => moderate(comment.commentId, post.postId, 'approve')}
                                className="rounded-lg p-1.5 text-green-600 transition hover:bg-green-50 dark:hover:bg-green-950/30"
                                title="Approve"
                              >
                                <CheckCircle size={16} />
                              </button>
                              <button
                                onClick={() => moderate(comment.commentId, post.postId, 'reject')}
                                className="rounded-lg p-1.5 text-red-600 transition hover:bg-red-50 dark:hover:bg-red-950/30"
                                title="Reject"
                              >
                                <XCircle size={16} />
                              </button>
                              {!comment.isPostAuthor && (
                                <button
                                  onClick={() => { setReplyingTo(replyingTo === comment.commentId ? null : comment.commentId); setReplyContent(''); }}
                                  className="rounded-lg p-1.5 text-brand-600 transition hover:bg-brand-50 dark:hover:bg-brand-950/30"
                                  title="Reply as Author"
                                >
                                  <Reply size={16} />
                                </button>
                              )}
                            </div>
                          </div>

                          {/* Reply form */}
                          {replyingTo === comment.commentId && (
                            <div className="ml-6 mt-2 flex gap-2">
                              <input
                                value={replyContent}
                                onChange={(e) => setReplyContent(e.target.value)}
                                placeholder="Write your reply..."
                                className="input-field flex-1 text-sm"
                                onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), submitReply(comment.commentId, post.postId))}
                                autoFocus
                              />
                              <button
                                onClick={() => submitReply(comment.commentId, post.postId)}
                                disabled={submitting || !replyContent.trim()}
                                className="btn-primary text-xs"
                              >
                                <Send size={12} /> Reply
                              </button>
                            </div>
                          )}

                          {/* Threaded replies */}
                          {replyMap[comment.commentId] && replyMap[comment.commentId].length > 0 && (
                            <div className="ml-6 mt-2 space-y-2">
                              {replyMap[comment.commentId].map((reply) => (
                                <div key={reply.commentId} className="flex items-start gap-3 rounded-lg border border-brand-100 bg-brand-50/30 p-3 dark:border-brand-900/40 dark:bg-brand-950/20">
                                  <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-brand-100 text-brand-700 dark:bg-brand-900 dark:text-brand-300">
                                    <Shield size={10} />
                                  </div>
                                  <div className="min-w-0 flex-1">
                                    <div className="flex items-center gap-2">
                                      <span className="text-sm font-medium">{reply.authorName}</span>
                                      <span className="inline-flex items-center gap-0.5 rounded bg-brand-100 px-1 py-0.5 text-[10px] font-semibold text-brand-700 dark:bg-brand-900 dark:text-brand-300">
                                        Author
                                      </span>
                                    </div>
                                    <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{reply.content}</p>
                                    {reply.createdAt && (
                                      <p className="mt-1 text-xs text-slate-400">{new Date(reply.createdAt).toLocaleString()}</p>
                                    )}
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-slate-500 dark:text-slate-400">No comments on this post yet.</p>
                  )}
                </div>
              )}
            </div>
          );
        })}
        {posts.length === 0 && <p className="py-10 text-center text-slate-500 dark:text-slate-400">No posts to moderate.</p>}
      </div>
    </div>
  );
}
