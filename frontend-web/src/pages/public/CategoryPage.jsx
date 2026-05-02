/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api from '../../api/client';
import { PageLoader } from '../../components/ui/LoadingSpinner';

// Defines taxonomy page so related behavior stays grouped in one place.
function TaxonomyPage({ type }) {
  const { slug } = useParams();
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    const params = type === 'category' ? { categorySlug: slug } : { tagSlug: slug };
    api.get('/api/posts/public', { params })
      .then((r) => setPosts(r.data.data.content))
      .catch(() => setPosts([]))
      .finally(() => setLoading(false));
  }, [slug, type]);

  if (loading) return <PageLoader />;

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 md:px-8">
      <p className="page-heading">{type}</p>
      <h1 className="mt-2 font-display text-3xl font-bold capitalize">{slug?.replace(/-/g, ' ')}</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">{posts.length} post{posts.length !== 1 ? 's' : ''} found</p>
      <div className="mt-8 grid gap-5 sm:grid-cols-2">
        {posts.map((post) => (
          <Link key={post.postId} to={`/posts/${post.slug}`} className="card group p-5 transition-all hover:shadow-glow">
            <h2 className="font-display text-lg font-semibold group-hover:text-brand dark:group-hover:text-brand-400">{post.title}</h2>
            <p className="mt-2 line-clamp-2 text-sm text-slate-500 dark:text-slate-400">{post.excerpt}</p>
          </Link>
        ))}
      </div>
      {posts.length === 0 && <p className="mt-10 text-center text-slate-500 dark:text-slate-400">No posts in this {type} yet.</p>}
    </div>
  );
}

// Defines category page so related behavior stays grouped in one place.
export function CategoryPage() { return <TaxonomyPage type="category" />; }
export function TagPage() { return <TaxonomyPage type="tag" />; }
