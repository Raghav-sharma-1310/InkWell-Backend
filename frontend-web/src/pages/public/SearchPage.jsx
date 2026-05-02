/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Search } from 'lucide-react';
import api from '../../api/client';

// Defines search page so related behavior stays grouped in one place.
export function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [posts, setPosts] = useState([]);
  const [categories, setCategories] = useState([]);
  const categorySlug = searchParams.get('category') || '';
  const query = searchParams.get('search') || '';

  useEffect(() => {
    api.get('/api/categories/public/categories/active').then((r) => setCategories(r.data.data)).catch(console.error);
  }, []);

  useEffect(() => {
    const params = {};
    if (query) params.search = query;
    if (categorySlug) params.category = categorySlug;

    api.get('/api/posts/explore', { params })
      .then((r) => setPosts(r.data.data.content))
      .catch(() => setPosts([]));
  }, [query, categorySlug]);

  // Provides clear filters wiring so the framework can apply the expected runtime behavior.
  const clearFilters = () => {
    setSearchParams(new URLSearchParams());
  };

  // Performs the update params workflow so callers do not duplicate this logic.
  const updateParams = (key, value) => {
    const newParams = new URLSearchParams(searchParams);
    if (value) {
      newParams.set(key, value);
    } else {
      newParams.delete(key);
    }
    setSearchParams(newParams);
  };

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 md:px-8">
      <h1 className="font-display text-3xl font-bold">Explore Posts</h1>
      <div className="mt-6 flex flex-col gap-4 md:flex-row">
        <div className="relative flex-1">
          <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            value={query}
            onChange={(e) => updateParams('search', e.target.value)}
            placeholder="Search articles..."
            className="input-field pl-11"
          />
        </div>
        <select
          value={categorySlug}
          onChange={(e) => updateParams('category', e.target.value)}
          className="input-field md:w-48"
        >
          <option value="">All Categories</option>
          {categories.map((c) => (
            <option key={c.categoryId} value={c.slug}>{c.name}</option>
          ))}
        </select>
        {(query || categorySlug) && (
          <button onClick={clearFilters} className="btn-ghost shrink-0 text-slate-500 hover:text-slate-700 dark:hover:text-slate-300">
            Clear Filters
          </button>
        )}
      </div>
      <div className="mt-8 space-y-4">
        {posts.map((post) => (
          <Link key={post.postId} to={`/posts/${post.slug}`} className="card group block p-5 transition-all hover:shadow-glow">
            <div className="flex items-start justify-between gap-4">
              <div>
                <span className="badge-brand text-xs">{post.categorySlug || 'post'}</span>
                <h2 className="mt-2 font-display text-xl font-semibold group-hover:text-brand dark:group-hover:text-brand-400">{post.title}</h2>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{post.excerpt}</p>
              </div>
              <span className="shrink-0 text-xs text-slate-400 dark:text-slate-500">{post.readTimeMin}m</span>
            </div>
          </Link>
        ))}
        {posts.length === 0 && (
          <p className="py-10 text-center text-slate-500 dark:text-slate-400">No posts found matching criteria.</p>
        )}
      </div>
    </div>
  );
}
