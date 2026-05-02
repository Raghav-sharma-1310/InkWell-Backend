/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import api from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { PageLoader } from '../../components/ui/LoadingSpinner';
import { PostImage } from '../../components/ui/PostImage';
import { UserPlus, UserCheck, FileText, Users, Clock, Eye, Heart, User, Crown, Lock } from 'lucide-react';

// Provides author profile page wiring so the framework can apply the expected runtime behavior.
export function AuthorProfilePage() {
  const { authorId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [author, setAuthor] = useState(null);
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [following, setFollowing] = useState(false);
  const [followersCount, setFollowersCount] = useState(0);
  const [followLoading, setFollowLoading] = useState(false);

  const isOwnProfile = user && user.userId === authorId;
  const isPro = user?.subscriptionTier === 'PRO' && user?.subscriptionStatus === 'ACTIVE';
  const isAdminOrAuthor = user?.role === 'ADMIN' || user?.role === 'AUTHOR';
  // Pro users, admins, and authors can always view profiles; non-pro readers get a gate
  const canViewFullProfile = !user || isPro || isAdminOrAuthor || isOwnProfile;

  useEffect(() => {
    setLoading(true);
    Promise.all([
      api.get(`/api/auth/public/users/${authorId}`).then((r) => r.data.data).catch(() => null),
      api.get(`/api/posts/public?size=50`).then((r) => {
        const allPosts = r.data.data?.content || [];
        return allPosts.filter((p) => p.authorId === authorId);
      }).catch(() => []),
      api.get(`/api/posts/authors/${authorId}/followers/count`).then((r) => r.data.data?.followersCount || 0).catch(() => 0),
    ]).then(([a, p, fc]) => {
      setAuthor(a);
      setPosts(p);
      setFollowersCount(fc);
      setLoading(false);
    });
  }, [authorId]);

  // Fetch follow status for logged-in PRO users
  useEffect(() => {
    if (user && !isOwnProfile && canViewFullProfile) {
      api.get(`/api/posts/authors/${authorId}/follow/status`)
        .then((r) => {
          setFollowing(r.data.data?.following || false);
          setFollowersCount(r.data.data?.followersCount || 0);
        })
        .catch(() => {});
    }
  }, [user, authorId, isOwnProfile, canViewFullProfile]);

  // Defines toggle follow so related behavior stays grouped in one place.
  const toggleFollow = async () => {
    if (!user) {
      navigate('/login');
      return;
    }
    if (!canViewFullProfile) return;
    setFollowLoading(true);
    try {
      const response = await api.post(`/api/posts/authors/${authorId}/follow`);
      const data = response.data.data;
      setFollowing(data.following);
      setFollowersCount(data.followersCount);
    } catch {
      // silently handle
    } finally {
      setFollowLoading(false);
    }
  };

  if (loading) return <PageLoader />;

  // Pro gate for non-pro readers
  if (user && !canViewFullProfile) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-20 text-center">
        <div className="card animate-fade-in p-10">
          <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-3xl bg-gradient-to-br from-amber-100 to-yellow-200 dark:from-amber-900/40 dark:to-yellow-900/40">
            <Crown size={36} className="text-amber-600 dark:text-amber-400" />
          </div>
          <h1 className="mt-6 font-display text-2xl font-bold">Upgrade to Pro</h1>
          <p className="mt-3 text-slate-600 dark:text-slate-400">
            Author profiles and the follow feature are exclusive to <strong>Pro subscribers</strong>. 
            Upgrade to Pro to view author profiles, follow your favorite writers, and get premium reading access.
          </p>
          <div className="mt-6 flex flex-col items-center gap-3">
            <button
              onClick={() => navigate('/profile')}
              className="btn-primary flex items-center gap-2 px-8 py-3 text-sm"
            >
              <Crown size={16} /> View Subscription Plans
            </button>
            <button
              onClick={() => navigate(-1)}
              className="text-sm text-slate-500 transition-colors hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200"
            >
              Go Back
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 md:px-8">
      {/* Author Header Card */}
      <div className="card animate-fade-in overflow-hidden">
        <div className="h-28 bg-gradient-to-r from-brand-600 to-teal-700" />
        <div className="relative px-8 pb-8">
          {/* Avatar */}
          <div className="-mt-14 flex items-end justify-between">
            <div className="flex items-end gap-5">
              {author?.avatarUrl ? (
                <img
                  src={author.avatarUrl}
                  alt=""
                  className="h-24 w-24 rounded-2xl border-4 border-white object-cover shadow-lg dark:border-slate-900"
                />
              ) : (
                <div className="flex h-24 w-24 items-center justify-center rounded-2xl border-4 border-white bg-brand-100 text-3xl font-bold text-brand-700 shadow-lg dark:border-slate-900 dark:bg-brand-900 dark:text-brand-300">
                  {author?.fullName?.[0]?.toUpperCase() || '?'}
                </div>
              )}
              <div className="pb-1">
                <h1 className="font-display text-2xl font-bold md:text-3xl">{author?.fullName || 'Unknown Author'}</h1>
                {author?.username && (
                  <p className="text-sm text-slate-500 dark:text-slate-400">@{author.username}</p>
                )}
              </div>
            </div>
            {user && !isOwnProfile && canViewFullProfile && (
              <button
                onClick={toggleFollow}
                disabled={followLoading}
                className={`flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-semibold transition-all ${
                  following
                    ? 'border border-brand-200 bg-brand-50 text-brand-700 hover:bg-brand-100 dark:border-brand-800 dark:bg-brand-950/40 dark:text-brand-400'
                    : 'bg-brand text-white shadow-md hover:bg-brand-700 hover:shadow-lg'
                }`}
              >
                {following ? <><UserCheck size={16} /> Following</> : <><UserPlus size={16} /> Follow</>}
              </button>
            )}
            {user && !isOwnProfile && !canViewFullProfile && (
              <div className="flex items-center gap-2 rounded-xl border border-amber-200 bg-amber-50 px-4 py-2 text-xs font-medium text-amber-700 dark:border-amber-800 dark:bg-amber-900/30 dark:text-amber-400">
                <Lock size={14} /> Pro Only
              </div>
            )}
          </div>

          {/* Bio */}
          {author?.bio && (
            <p className="mt-5 max-w-2xl text-sm leading-relaxed text-slate-600 dark:text-slate-400">
              {author.bio}
            </p>
          )}

          {/* Stats */}
          <div className="mt-6 flex flex-wrap gap-6 border-t border-slate-100 pt-6 dark:border-slate-800">
            <div className="flex items-center gap-2 text-sm">
              <FileText size={16} className="text-brand" />
              <span className="font-bold">{posts.length}</span>
              <span className="text-slate-500 dark:text-slate-400">Posts</span>
            </div>
            <div className="flex items-center gap-2 text-sm">
              <Users size={16} className="text-brand" />
              <span className="font-bold">{followersCount}</span>
              <span className="text-slate-500 dark:text-slate-400">{followersCount === 1 ? 'Follower' : 'Followers'}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Posts */}
      <h2 className="mt-10 font-display text-xl font-bold">
        Posts by {author?.fullName || 'this author'}
      </h2>
      <div className="mt-4 grid gap-5 sm:grid-cols-2">
        {posts.map((post) => (
          <Link
            key={post.postId}
            to={`/posts/${post.slug}`}
            className="card group overflow-hidden transition-all hover:shadow-glow"
          >
            <PostImage
              src={post.featuredImageUrl}
              alt={post.title}
              className="h-40 w-full object-cover transition-transform duration-300 group-hover:scale-105"
            />
            <div className="p-5">
              {post.categorySlug && (
                <span className="badge-brand mb-2 inline-block text-xs">{post.categorySlug}</span>
              )}
              <h3 className="font-display text-lg font-semibold leading-snug line-clamp-2">
                {post.title}
              </h3>
              <p className="mt-2 text-sm text-slate-500 dark:text-slate-400 line-clamp-2">
                {post.excerpt}
              </p>
              <div className="mt-3 flex items-center gap-4 text-xs text-slate-400 dark:text-slate-500">
                <span className="flex items-center gap-1"><Clock size={12} /> {post.readTimeMin} min</span>
                <span className="flex items-center gap-1"><Eye size={12} /> {post.viewCount}</span>
                <span className="flex items-center gap-1"><Heart size={12} /> {post.likesCount}</span>
              </div>
            </div>
          </Link>
        ))}
      </div>
      {posts.length === 0 && (
        <div className="mt-8 flex flex-col items-center justify-center py-12 text-center">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-slate-50 dark:bg-slate-800/50">
            <FileText size={24} className="text-slate-400" />
          </div>
          <p className="mt-4 text-base font-medium text-slate-600 dark:text-slate-300">No published posts yet</p>
          <p className="mt-1 text-sm text-slate-500">Check back later for new content from this author.</p>
        </div>
      )}
    </div>
  );
}
