/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState, useCallback } from 'react';
import { History, Crown, Trash2 } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { PageLoader } from '../../components/ui/LoadingSpinner';
import { PostImage } from '../../components/ui/PostImage';

// Defines history page so related behavior stays grouped in one place.
export function HistoryPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const isPremium = user?.subscriptionTier === 'PRO' && user?.subscriptionStatus === 'ACTIVE';

  const loadHistory = useCallback(async () => {
    try {
      const response = await api.get('/api/reading-history/me', { params: { size: 50 } });
      setHistory(response.data.data?.content || []);
    } catch (err) {
      if (err.response?.status === 403) {
        setError("Premium subscription required.");
      } else {
        setError("Failed to load history.");
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isPremium) {
      setLoading(false);
      return;
    }
    loadHistory();
  }, [isPremium, loadHistory]);

  // Defines clear history so related behavior stays grouped in one place.
  const clearHistory = async () => {
    if (!window.confirm("Are you sure you want to clear your reading history?")) return;
    try {
      await api.delete('/api/reading-history/clear');
      setHistory([]);
    } catch (err) {
      console.error("Failed to clear history", err);
    }
  };

  // Defines remove history item so related behavior stays grouped in one place.
  const removeHistoryItem = async (e, postId) => {
    e.preventDefault();
    e.stopPropagation();
    try {
      await api.delete(`/api/reading-history/${postId}`);
      setHistory(prev => prev.filter(p => p.postId !== postId));
    } catch (err) {
      console.error("Failed to remove history item", err);
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
          Reading history is an exclusive feature for our PRO subscribers. Upgrade your plan to track your reading journey and easily revisit past articles!
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
      <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-100 text-brand-700 dark:bg-brand-900/30 dark:text-brand-300">
            <History size={24} />
          </div>
          <div>
            <h1 className="font-display text-3xl font-bold">Reading History</h1>
            <p className="text-sm text-slate-500 dark:text-slate-400">Recently viewed posts</p>
          </div>
        </div>

        {history.length > 0 && (
          <button
            onClick={clearHistory}
            className="btn-ghost flex items-center gap-2 text-red-600 hover:bg-red-50 hover:text-red-700 dark:text-red-400 dark:hover:bg-red-900/30"
          >
            <Trash2 size={16} />
            Clear History
          </button>
        )}
      </div>

      {error ? (
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-red-600 dark:border-red-900/30 dark:bg-red-900/10">
          {error}
        </div>
      ) : history.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <History size={48} className="mb-4 text-slate-300 dark:text-slate-700" />
          <h3 className="text-lg font-semibold text-slate-700 dark:text-slate-300">No reading history</h3>
          <p className="mt-2 text-slate-500 dark:text-slate-400">
            Posts you read will automatically appear here.
          </p>
          <Link to="/search" className="btn-primary mt-6">
            Start Reading
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {history.map((post) => (
            <Link
              key={post.postId}
              to={`/posts/${post.slug}`}
              className="group flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-4 transition-all hover:shadow-glow sm:flex-row sm:items-center dark:border-slate-800 dark:bg-slate-900"
            >
              <div className="h-24 w-full shrink-0 overflow-hidden rounded-lg sm:w-32">
                <PostImage
                  src={post.featuredImageUrl}
                  alt={post.title}
                  className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                />
              </div>
              <div className="flex flex-col justify-center flex-1">
                <span className="mb-1 w-fit badge-brand text-[10px]">{post.categorySlug}</span>
                <h3 className="font-display text-lg font-semibold leading-tight group-hover:text-brand-600 dark:group-hover:text-brand-400 pr-8">
                  {post.title}
                </h3>
                <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
                  {post.readTimeMin} min read
                </p>
              </div>
              <button 
                onClick={(e) => removeHistoryItem(e, post.postId)}
                className="absolute right-4 top-1/2 -translate-y-1/2 rounded-full p-2 text-slate-400 opacity-0 transition-all hover:bg-red-50 hover:text-red-500 group-hover:opacity-100 sm:relative sm:right-0 sm:top-0 sm:translate-y-0 sm:opacity-100 dark:hover:bg-red-900/30 dark:hover:text-red-400"
                title="Remove from history"
              >
                <Trash2 size={16} />
              </button>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
