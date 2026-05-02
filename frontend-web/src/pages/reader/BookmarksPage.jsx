/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { Bookmark as BookmarkIcon, Crown, Trash2 } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { PageLoader } from '../../components/ui/LoadingSpinner';
import { PostImage } from '../../components/ui/PostImage';

// Performs the bookmarks page workflow so callers do not duplicate this logic.
export function BookmarksPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [bookmarks, setBookmarks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const isPremium = user?.subscriptionTier === 'PRO' && user?.subscriptionStatus === 'ACTIVE';

  useEffect(() => {
    if (!isPremium) {
      setLoading(false);
      return;
    }

    // Performs the load bookmarks workflow so callers do not duplicate this logic.
    const loadBookmarks = async () => {
      try {
        const response = await api.get('/api/posts/reader/bookmarks');
        setBookmarks(response.data.data || []);
      } catch (err) {
        if (err.response?.status === 403) {
          setError("Premium subscription required.");
        } else {
          setError("Failed to load bookmarks.");
        }
      } finally {
        setLoading(false);
      }
    };

    loadBookmarks();
  }, [isPremium]);

  // Performs the remove bookmark workflow so callers do not duplicate this logic.
  const removeBookmark = async (postId) => {
    try {
      await api.post(`/api/posts/reader/${postId}/bookmark`);
      setBookmarks((prev) => prev.filter((b) => b.postId !== postId));
    } catch (err) {
      console.error("Failed to remove bookmark", err);
    }
  };

  if (loading) return <PageLoader />;

  if (!isPremium) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center text-center">
        <div className="mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-yellow-100 text-yellow-600 dark:bg-yellow-900/30 dark:text-yellow-500">
          <Crown size={40} />
        </div>
        <h2 className="font-display text-3xl font-bold">Premium Feature</h2>
        <p className="mt-4 max-w-md text-slate-500 dark:text-slate-400">
          Bookmarking posts is an exclusive feature for our PRO subscribers. Upgrade your plan to save your favorite articles and read them anytime!
        </p>
        <button
          onClick={() => navigate('/profile')}
          className="btn-primary mt-8 gap-2"
        >
          <Crown size={18} />
          Upgrade to PRO
        </button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl py-8 px-4">
      <div className="mb-8 flex items-center gap-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-100 text-brand-700 dark:bg-brand-900/30 dark:text-brand-300">
          <BookmarkIcon size={24} />
        </div>
        <div>
          <h1 className="font-display text-3xl font-bold">Your Bookmarks</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">Posts you've saved for later</p>
        </div>
      </div>

      {error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-600 dark:border-red-900/30 dark:bg-red-900/10">
          {error}
        </div>
      ) : bookmarks.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <BookmarkIcon size={48} className="mb-4 text-slate-300 dark:text-slate-700" />
          <h3 className="text-lg font-semibold text-slate-700 dark:text-slate-300">No bookmarks yet</h3>
          <p className="mt-2 text-slate-500 dark:text-slate-400">
            When you find a post you like, click the bookmark icon to save it here.
          </p>
          <Link to="/search" className="btn-primary mt-6">
            Explore Posts
          </Link>
        </div>
      ) : (
        <div className="grid gap-6 sm:grid-cols-2">
          {bookmarks.map((post) => (
            <div key={post.postId} className="group relative flex flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white transition-shadow hover:shadow-glow dark:border-slate-800 dark:bg-slate-900">
              <Link to={`/posts/${post.slug}`} className="block h-48 overflow-hidden">
                <PostImage
                  src={post.featuredImageUrl}
                  alt={post.title}
                  className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                />
              </Link>
              <div className="flex flex-1 flex-col p-5">
                <div className="mb-3 flex items-center justify-between">
                  <span className="badge-brand">{post.categorySlug}</span>
                  <button
                    onClick={(e) => {
                      e.preventDefault();
                      removeBookmark(post.postId);
                    }}
                    className="rounded-full p-2 text-slate-400 transition-colors hover:bg-red-50 hover:text-red-500 dark:hover:bg-red-950/30"
                    title="Remove bookmark"
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
                <Link to={`/posts/${post.slug}`}>
                  <h3 className="font-display text-xl font-bold leading-tight group-hover:text-brand-600 dark:group-hover:text-brand-400">
                    {post.title}
                  </h3>
                </Link>
                <p className="mt-3 line-clamp-2 text-sm text-slate-600 dark:text-slate-400">
                  {post.excerpt}
                </p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
