/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import DOMPurify from 'dompurify';
import api from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { PageLoader, EmptyState } from '../../components/ui/LoadingSpinner';
import { PostImage } from '../../components/ui/PostImage';
import { Heart, Eye, Clock, MessageCircle, Send, User, Bookmark, Crown, UserPlus, UserCheck, Reply, Shield } from 'lucide-react';

// Defines post detail page so related behavior stays grouped in one place.
export function PostDetailPage() {
  const { slug } = useParams();
  const { user } = useAuth();
  const [post, setPost] = useState(null);
  const [comments, setComments] = useState([]);
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [liked, setLiked] = useState(false);
  const [likesCount, setLikesCount] = useState(0);
  const [liking, setLiking] = useState(false);
  const [bookmarked, setBookmarked] = useState(false);
  const [bookmarking, setBookmarking] = useState(false);

  // Author info
  const [author, setAuthor] = useState(null);
  const [following, setFollowing] = useState(false);
  const [followersCount, setFollowersCount] = useState(0);
  const [followLoading, setFollowLoading] = useState(false);
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyContent, setReplyContent] = useState('');

  const isPremium = user?.subscriptionTier === 'PRO' && user?.subscriptionStatus === 'ACTIVE';

  useEffect(() => {
    setLoading(true);
    api.get(`/api/posts/public/${slug}`)
      .then((response) => {
        const postData = response.data.data;
        setPost(postData);
        setLikesCount(postData.likesCount || 0);

        // Fetch author info
        if (postData.authorId) {
          api.get(`/api/auth/public/users/${postData.authorId}`)
            .then((r) => setAuthor(r.data.data))
            .catch(() => {});

          // Public followers count
          api.get(`/api/posts/authors/${postData.authorId}/followers/count`)
            .then((r) => setFollowersCount(r.data.data?.followersCount || 0))
            .catch(() => {});
        }

        return api.get(`/api/comments/public/post/${postData.postId}`);
      })
      .then((response) => setComments(response.data.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));

    if (user && isPremium) {
      api.get('/api/posts/reader/bookmarks').then((res) => {
         const bookmarks = res.data.data || [];
         const found = bookmarks.find(b => b.slug === slug);
         if (found) setBookmarked(true);
      }).catch(() => {});

      // Record reading history
      api.post('/api/reading-history', { postSlug: slug }).catch(() => {});
    }
  }, [slug, user, isPremium]);

  // Fetch follow status when we have post + user
  useEffect(() => {
    if (user && post?.authorId && user.userId !== post.authorId) {
      api.get(`/api/posts/authors/${post.authorId}/follow/status`)
        .then((r) => {
          setFollowing(r.data.data?.following || false);
          setFollowersCount(r.data.data?.followersCount || 0);
        })
        .catch(() => {});
    }
  }, [user, post?.authorId]);

  // Defines toggle like so related behavior stays grouped in one place.
  const toggleLike = async () => {
    if (!user || !post) return;
    setLiking(true);
    try {
      const response = await api.post(`/api/posts/reader/${post.postId}/like`);
      const data = response.data.data;
      setLiked(data.liked);
      setLikesCount(data.likesCount);
    } catch {
      // silently handle
    } finally {
      setLiking(false);
    }
  };

  // Performs the toggle bookmark workflow so callers do not duplicate this logic.
  const toggleBookmark = async () => {
    if (!user || !post) return;
    if (!isPremium) {
      alert("Bookmarking is a premium feature. Upgrade to PRO to save posts.");
      return;
    }
    setBookmarking(true);
    try {
      const response = await api.post(`/api/posts/reader/${post.postId}/bookmark`);
      setBookmarked(response.data.data.bookmarked);
    } catch {
      // silently handle
    } finally {
      setBookmarking(false);
    }
  };

  // Defines toggle follow so related behavior stays grouped in one place.
  const toggleFollow = async () => {
    if (!user || !post) return;
    setFollowLoading(true);
    try {
      const response = await api.post(`/api/posts/authors/${post.authorId}/follow`);
      const data = response.data.data;
      setFollowing(data.following);
      setFollowersCount(data.followersCount);
    } catch {
      // silently handle
    } finally {
      setFollowLoading(false);
    }
  };

  // Performs the refresh comments workflow so callers do not duplicate this logic.
  const refreshComments = async () => {
    if (!post) return;
    try {
      const response = await api.get(`/api/comments/public/post/${post.postId}`);
      setComments(response.data.data || []);
    } catch {}
  };

  // Defines submit comment so related behavior stays grouped in one place.
  const submitComment = async (event) => {
    event.preventDefault();
    if (!content.trim()) return;
    setSubmitting(true);
    try {
      await api.post('/api/comments', { postId: post.postId, content });
      await refreshComments();
      setContent('');
    } catch {
      // silently handle
    } finally {
      setSubmitting(false);
    }
  };

  // Defines submit reply so related behavior stays grouped in one place.
  const submitReply = async (commentId) => {
    if (!replyContent.trim()) return;
    setSubmitting(true);
    try {
      await api.post(`/api/comments/${commentId}/reply`, { content: replyContent });
      await refreshComments();
      setReplyContent('');
      setReplyingTo(null);
    } catch {
      // silently handle
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <PageLoader />;
  if (!post) return <EmptyState title="Post not found" description="The post you're looking for doesn't exist." />;

  const isOwnPost = user && post.authorId === user.userId;

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 md:px-8">
      {/* Article */}
      <article className="card animate-fade-in p-8 md:p-10">
        <div className="flex items-center gap-3">
          {post.categorySlug && (
            <Link to={`/categories/${post.categorySlug}`} className="badge-brand">
              {post.categorySlug}
            </Link>
          )}
          {post.tagSlugs && post.tagSlugs.length > 0 && post.tagSlugs.map((tag) => (
            <Link key={tag} to={`/tags/${tag}`} className="rounded-lg bg-slate-100 px-2 py-1 text-xs text-slate-600 transition hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-400 dark:hover:bg-slate-700">
              #{tag}
            </Link>
          ))}
        </div>
        <h1 className="mt-4 font-display text-3xl font-bold leading-tight md:text-4xl">{post.title}</h1>
        <div className="mt-4 flex flex-wrap items-center gap-4 text-sm text-slate-500 dark:text-slate-400">
          <span className="flex items-center gap-1"><Clock size={14} /> {post.readTimeMin} min read</span>
          <span className="flex items-center gap-1"><Eye size={14} /> {post.viewCount} views</span>
          <span className="flex items-center gap-1"><Heart size={14} /> {likesCount} likes</span>
          {post.publishedAt && (
            <span>{new Date(post.publishedAt).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })}</span>
          )}
        </div>

        {/* Author Card */}
        <div className="mt-6 flex items-center justify-between rounded-xl border border-slate-100 bg-slate-50/50 p-4 dark:border-slate-800 dark:bg-slate-800/30">
          <Link to={`/authors/${post.authorId}`} className="flex items-center gap-3 transition-opacity hover:opacity-80">
            {author?.avatarUrl ? (
              <img src={author.avatarUrl} alt="" className="h-12 w-12 rounded-xl object-cover ring-2 ring-brand-100 dark:ring-brand-900" />
            ) : (
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-100 text-lg font-bold text-brand-700 ring-2 ring-brand-200 dark:bg-brand-900 dark:text-brand-300 dark:ring-brand-800">
                {author?.fullName?.[0]?.toUpperCase() || <User size={18} />}
              </div>
            )}
            <div>
              <p className="font-semibold text-slate-900 dark:text-white">{author?.fullName || 'Author'}</p>
              {author?.bio && (
                <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400 line-clamp-1 max-w-xs">{author.bio}</p>
              )}
              <p className="mt-0.5 text-xs text-slate-400 dark:text-slate-500">
                {followersCount} {followersCount === 1 ? 'follower' : 'followers'}
              </p>
            </div>
          </Link>
          {user && !isOwnPost && (
            <button
              onClick={toggleFollow}
              disabled={followLoading}
              className={`flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-medium transition-all ${
                following
                  ? 'bg-brand-50 text-brand-700 border border-brand-200 dark:bg-brand-950/40 dark:text-brand-400 dark:border-brand-800'
                  : 'bg-brand text-white hover:bg-brand-700'
              }`}
            >
              {following ? <><UserCheck size={15} /> Following</> : <><UserPlus size={15} /> Follow</>}
            </button>
          )}
        </div>

        <PostImage src={post.featuredImageUrl} alt={post.title} className="mt-6 h-64 w-full rounded-xl object-cover md:h-80" />
        <div className="prose mt-8 max-w-none" dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(post.content) }} />

        {/* Like button */}
        <div className="mt-8 flex items-center gap-4 border-t border-slate-100 pt-6 dark:border-slate-800">
          {user ? (
            <>
              <button
                onClick={toggleLike}
              disabled={liking}
              className={`flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-medium transition-all ${
                liked
                  ? 'bg-rose-50 text-rose-600 shadow-sm dark:bg-rose-950/40 dark:text-rose-400'
                  : 'bg-slate-100 text-slate-600 hover:bg-rose-50 hover:text-rose-600 dark:bg-slate-800 dark:text-slate-400 dark:hover:bg-rose-950/40 dark:hover:text-rose-400'
              }`}
            >
              <Heart size={16} className={liked ? 'fill-current' : ''} />
              {liked ? 'Liked' : 'Like'} · {likesCount}
            </button>
            <button
              onClick={toggleBookmark}
              disabled={bookmarking}
              className={`flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-medium transition-all ${
                bookmarked
                  ? 'bg-brand-50 text-brand-600 shadow-sm dark:bg-brand-950/40 dark:text-brand-400'
                  : 'bg-slate-100 text-slate-600 hover:bg-brand-50 hover:text-brand-600 dark:bg-slate-800 dark:text-slate-400 dark:hover:bg-brand-950/40 dark:hover:text-brand-400'
              }`}
            >
              {bookmarked ? (
                <>
                  <Bookmark size={16} className="fill-current" /> Saved
                </>
              ) : (
                <>
                  <Bookmark size={16} /> Save {!isPremium && <Crown size={12} className="text-yellow-500" />}
                </>
              )}
            </button>
          </>
          ) : (
            <p className="text-sm text-slate-500 dark:text-slate-400">
              <Link to="/login" className="font-medium text-brand">Sign in</Link> to like and bookmark this post.
            </p>
          )}
        </div>
      </article>

      {/* Comments */}
      <section className="card mt-8 p-8">
        <h2 className="flex items-center gap-2 font-display text-2xl font-bold">
          <MessageCircle size={22} /> Discussion ({comments.length})
        </h2>

        {user && (
          <form onSubmit={submitComment} className="mt-6">
            <div className="flex gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-sm font-bold text-brand-700 dark:bg-brand-950 dark:text-brand-300">
                {user.fullName?.[0]?.toUpperCase() || 'U'}
              </div>
              <div className="flex-1 space-y-3">
                <textarea
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  placeholder="Share your thoughts..."
                  className="input-field min-h-[80px] resize-y"
                />
                <button disabled={submitting || !content.trim()} className="btn-primary text-sm">
                  <Send size={14} /> {submitting ? 'Posting...' : 'Post Comment'}
                </button>
              </div>
            </div>
          </form>
        )}
        {!user && (
          <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">
            <Link to="/login" className="font-medium text-brand">Sign in</Link> to join the discussion.
          </p>
        )}

        <div className="mt-6 space-y-4">
          {(() => {
            const topLevel = comments.filter((c) => !c.parentCommentId);
            const replies = comments.filter((c) => c.parentCommentId);
            const replyMap = {};
            replies.forEach((r) => {
              if (!replyMap[r.parentCommentId]) replyMap[r.parentCommentId] = [];
              replyMap[r.parentCommentId].push(r);
            });

            // Defines render comment so related behavior stays grouped in one place.
            const renderComment = (comment, isReply = false) => (
              <div key={comment.commentId} className={`rounded-xl border p-4 transition-colors ${
                isReply
                  ? 'ml-8 border-brand-100 bg-brand-50/30 dark:border-brand-900/40 dark:bg-brand-950/20'
                  : 'border-slate-100 bg-slate-50 dark:border-slate-800 dark:bg-slate-800/50'
              }`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className={`flex h-8 w-8 items-center justify-center rounded-lg text-xs font-bold ${
                      comment.isPostAuthor
                        ? 'bg-brand-100 text-brand-700 ring-2 ring-brand-300 dark:bg-brand-900 dark:text-brand-300 dark:ring-brand-700'
                        : 'bg-brand-50 text-brand-700 dark:bg-brand-950 dark:text-brand-300'
                    }`}>
                      {comment.isPostAuthor ? <Shield size={14} /> : <User size={14} />}
                    </div>
                    <div>
                      <span className="text-sm font-medium">{comment.authorName || 'User'}</span>
                      {comment.isPostAuthor && (
                        <span className="ml-2 inline-flex items-center gap-1 rounded-md bg-brand-100 px-1.5 py-0.5 text-xs font-semibold text-brand-700 dark:bg-brand-900 dark:text-brand-300">
                          <Shield size={10} /> Author
                        </span>
                      )}
                      <span className="ml-2 text-xs text-slate-400 dark:text-slate-500">
                        {comment.createdAt ? new Date(comment.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : ''}
                      </span>
                    </div>
                  </div>
                  <span className={`rounded-md px-2 py-0.5 text-xs font-medium ${
                    comment.status === 'APPROVED' ? 'bg-green-100 text-green-700 dark:bg-green-950 dark:text-green-300' :
                    comment.status === 'PENDING' ? 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300' :
                    'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300'
                  }`}>{comment.status}</span>
                </div>
                <p className="mt-2 text-sm leading-relaxed">{comment.content}</p>

                {/* Reply button for post author */}
                {user && isOwnPost && !isReply && !comment.isPostAuthor && (
                  <button
                    onClick={() => { setReplyingTo(replyingTo === comment.commentId ? null : comment.commentId); setReplyContent(''); }}
                    className="mt-2 flex items-center gap-1 text-xs font-medium text-brand-600 transition hover:text-brand-700 dark:text-brand-400"
                  >
                    <Reply size={12} /> Reply as Author
                  </button>
                )}

                {/* Inline reply form */}
                {replyingTo === comment.commentId && (
                  <div className="mt-3 flex gap-2">
                    <input
                      value={replyContent}
                      onChange={(e) => setReplyContent(e.target.value)}
                      placeholder="Write your reply..."
                      className="input-field flex-1 text-sm"
                      onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), submitReply(comment.commentId))}
                    />
                    <button
                      onClick={() => submitReply(comment.commentId)}
                      disabled={submitting || !replyContent.trim()}
                      className="btn-primary text-xs"
                    >
                      <Send size={12} /> Reply
                    </button>
                  </div>
                )}

                {/* Render replies */}
                {replyMap[comment.commentId] && replyMap[comment.commentId].length > 0 && (
                  <div className="mt-3 space-y-3">
                    {replyMap[comment.commentId].map((reply) => renderComment(reply, true))}
                  </div>
                )}
              </div>
            );

            return topLevel.length > 0 ? topLevel.map((c) => renderComment(c)) : (
              <p className="text-sm text-slate-500 dark:text-slate-400">No comments yet. Be the first!</p>
            );
          })()}
        </div>
      </section>
    </div>
  );
}
