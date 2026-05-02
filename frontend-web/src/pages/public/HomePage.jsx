/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, BookOpen, Tag, Sparkles, Eye, Heart } from 'lucide-react';
import api from '../../api/client';
import { PageLoader } from '../../components/ui/LoadingSpinner';
import { PostImage } from '../../components/ui/PostImage';

// Defines home page so related behavior stays grouped in one place.
export function HomePage() {
  const [posts, setPosts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [tags, setTags] = useState([]);
  const [stats, setStats] = useState({ totalPublishedPosts: 0, totalViews: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.get('/api/posts/public').then((r) => r.data.data.content).catch(() => []),
      api.get('/api/categories/public/categories/top').then((r) => r.data.data).catch(() => []),
      api.get('/api/categories/public/tags/trending').then((r) => r.data.data).catch(() => []),
      api.get('/api/posts/public/stats').then((r) => r.data.data).catch(() => ({ totalPublishedPosts: 0, totalViews: 0 })),
    ]).then(([postsData, catsData, tagsData, statsData]) => {
      setPosts(postsData);
      setCategories(catsData);
      setTags(tagsData);
      setStats(statsData);
      setLoading(false);
    });
  }, []);

  if (loading) return <PageLoader />;

  const totalViews = stats.totalViews;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 md:px-8">
      {/* Hero */}
      <section className="animate-fade-in-up overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 p-8 text-white shadow-glow md:p-14">
        <div className="grid gap-10 lg:grid-cols-[1.3fr_1fr]">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full bg-white/10 px-4 py-1.5 text-sm backdrop-blur">
              <Sparkles size={14} className="text-brand-300" />
              <span className="text-white/80">Publishing OS for everyone</span>
            </div>
            <h1 className="mt-6 font-display text-4xl font-bold leading-tight md:text-5xl lg:text-6xl">
              Ship thoughtful stories with a{' '}
              <span className="bg-gradient-to-r from-brand-300 to-teal-200 bg-clip-text text-transparent">polished</span>{' '}
              experience.
            </h1>
            <p className="mt-5 max-w-xl text-lg leading-relaxed text-white/70">
              Discover articles, publish with role-based dashboards, collect payments, run newsletters, and monitor the platform.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/register" className="inline-flex items-center gap-2 rounded-xl bg-white px-6 py-3 font-medium text-slate-900 transition hover:bg-brand-50 hover:shadow-lg">
                Start Writing <ArrowRight size={16} />
              </Link>
              <Link to="/search" className="rounded-xl border border-white/20 px-6 py-3 font-medium transition hover:bg-white/10">
                Explore Posts
              </Link>
            </div>
            {/* Live stats from backend */}
            <div className="mt-10 grid grid-cols-2 gap-6">
              {[
                { icon: <BookOpen size={18} />, label: 'Published', value: stats.totalPublishedPosts },
                { icon: <Tag size={18} />, label: 'Categories', value: categories.length },
              ].map((s) => (
                <div key={s.label} className="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur">
                  <div className="text-brand-300">{s.icon}</div>
                  <p className="mt-3 text-2xl font-bold">{s.value}</p>
                  <p className="mt-1 text-xs text-white/50">{s.label}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Trending tags + Categories */}
          <div className="space-y-6">
            <div className="rounded-2xl bg-white/5 p-6 backdrop-blur">
              <h2 className="font-display text-xl font-semibold">Trending Tags</h2>
              <div className="mt-4 flex flex-wrap gap-2">
                {tags.map((tag) => (
                  <Link key={tag.tagId} to={`/tags/${tag.slug}`} className="rounded-lg bg-white/10 px-3 py-1.5 text-sm transition hover:bg-white/20">
                    #{tag.name}
                  </Link>
                ))}
                {tags.length === 0 && <p className="text-sm text-white/50">No trending tags yet.</p>}
              </div>
            </div>
            <div className="rounded-2xl bg-white/5 p-6 backdrop-blur">
              <h2 className="font-display text-xl font-semibold">Categories</h2>
              <div className="mt-4 space-y-2">
                {categories.length > 0 ? categories.map((cat) => (
                  <Link key={cat.categoryId} to={`/categories/${cat.slug}`}
                    className="flex items-center justify-between rounded-xl border border-white/10 p-3 text-sm transition hover:bg-white/10">
                    <span className="font-medium">{cat.name}</span>
                    <span className="text-white/50">{cat.postCount} posts</span>
                  </Link>
                )) : <p className="text-sm text-white/50">No categories yet.</p>}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Posts grid */}
      <section className="mt-12">
        <div className="flex items-center justify-between">
          <h2 className="font-display text-2xl font-bold">Latest Posts</h2>
          <Link to="/search" className="text-sm font-medium text-brand hover:text-brand-800 dark:text-brand-400 dark:hover:text-brand-300">
            View all →
          </Link>
        </div>
        {posts.length > 0 ? (
          <div className="mt-6 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {posts.map((post, index) => (
              <article key={post.postId}
                className="card group animate-fade-in-up overflow-hidden transition-all duration-300 hover:-translate-y-1 hover:shadow-glow"
                style={{ animationDelay: `${index * 80}ms` }}>
                <PostImage src={post.featuredImageUrl} alt={post.title} className="h-44 w-full rounded-t-2xl object-cover" />
                <div className="p-6">
                  <span className="badge-brand">{post.categorySlug || 'post'}</span>
                  <h3 className="mt-3 font-display text-xl font-semibold leading-snug">
                    <Link to={`/posts/${post.slug}`} className="transition-colors hover:text-brand dark:hover:text-brand-400">
                      {post.title}
                    </Link>
                  </h3>
                  <p className="mt-2 line-clamp-2 text-sm text-slate-500 dark:text-slate-400">{post.excerpt}</p>
                  <div className="mt-4 flex items-center justify-between text-xs text-slate-400 dark:text-slate-500">
                    <span>{post.readTimeMin} min read</span>
                    <span className="flex items-center gap-1"><Heart size={12} /> {post.likesCount}</span>
                  </div>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="mt-8 rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-12 text-center dark:border-slate-700 dark:bg-slate-800/50">
            <BookOpen size={32} className="mx-auto text-slate-400" />
            <p className="mt-4 font-medium text-slate-600 dark:text-slate-300">No posts published yet</p>
            <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Once authors publish content, it will appear here.</p>
          </div>
        )}
      </section>
    </div>
  );
}
