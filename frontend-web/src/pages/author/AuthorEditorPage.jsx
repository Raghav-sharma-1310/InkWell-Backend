/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import api from '../../api/client';
import { RichTextEditor } from '../../components/forms/RichTextEditor';
import { Save, ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';

// Provides author editor page wiring so the framework can apply the expected runtime behavior.
export function AuthorEditorPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { postId } = useParams();
  const isEditing = Boolean(postId);

  const [form, setForm] = useState({
    title: '', content: '', excerpt: '', categorySlug: '', tagSlugs: '',
    status: 'DRAFT', featuredImageUrl: '', featured: false, pinned: false,
    visibility: 'PUBLIC',
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [loaded, setLoaded] = useState(!isEditing);

  // Reset form when switching between new and edit
  useEffect(() => {
    if (isEditing) {
      setLoaded(false);
      api.get(`/api/posts/author/${postId}`)
        .then((r) => {
          const p = r.data.data;
          setForm({
            title: p.title || '', content: p.content || '', excerpt: p.excerpt || '',
            categorySlug: p.categorySlug || '',
            tagSlugs: Array.isArray(p.tagSlugs) ? p.tagSlugs.join(', ') : (p.tagSlugs || ''),
            status: p.status || 'DRAFT', featuredImageUrl: p.featuredImageUrl || '',
            featured: p.featured || false, pinned: p.pinned || false,
            visibility: (p.visibility === 'PUBLIC' || p.visibility === 'PREMIUM') ? p.visibility : 'PUBLIC',
          });
        })
        .catch(() => setError('Failed to load post.'))
        .finally(() => setLoaded(true));
    } else {
      // Reset form for new post
      setForm({ title: '', content: '', excerpt: '', categorySlug: '', tagSlugs: '', status: 'DRAFT', featuredImageUrl: '', featured: false, pinned: false, visibility: 'PUBLIC' });
      setError('');
      setLoaded(true);
    }
  }, [postId, isEditing, location.key]);

  // Defines submit so related behavior stays grouped in one place.
  const submit = async (e) => {
    e.preventDefault();
    setSaving(true); setError('');
    try {
      const tagSet = String(form.tagSlugs).split(',').map((s) => s.trim()).filter(Boolean);
      const payload = { ...form, tagSlugs: tagSet };

      if (isEditing) {
        await api.put(`/api/posts/author/${postId}`, payload);
      } else {
        await api.post('/api/posts/author', payload);
      }
      navigate('/author/posts');
    } catch (err) {
      setError(err?.response?.data?.message || 'Failed to save post.');
    } finally { setSaving(false); }
  };

  // Defines set so related behavior stays grouped in one place.
  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));

  if (!loaded) {
    return <div className="flex items-center justify-center py-20"><div className="h-8 w-8 animate-spin rounded-full border-2 border-brand border-t-transparent" /></div>;
  }

  return (
    <form onSubmit={submit} className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link to="/author/posts" className="btn-ghost"><ArrowLeft size={16} /></Link>
          <h1 className="font-display text-3xl font-bold">{isEditing ? 'Edit Post' : 'Create Post'}</h1>
        </div>
        <button disabled={saving} className="btn-primary">
          <Save size={16} /> {saving ? 'Saving...' : 'Save Post'}
        </button>
      </div>

      {error && <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800/50 dark:bg-red-950/30 dark:text-red-300">{error}</div>}

      <input value={form.title} onChange={(e) => set('title', e.target.value)}
        placeholder="Post title" className="input-field font-display text-xl font-semibold" required />
      <input value={form.excerpt} onChange={(e) => set('excerpt', e.target.value)}
        placeholder="Short excerpt or summary" className="input-field" />
      <input value={form.featuredImageUrl} onChange={(e) => set('featuredImageUrl', e.target.value)}
        placeholder="Featured image URL (optional)" className="input-field" />

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <input value={form.categorySlug} onChange={(e) => set('categorySlug', e.target.value)}
          placeholder="Category slug" className="input-field" />
        <input value={form.tagSlugs} onChange={(e) => set('tagSlugs', e.target.value)}
          placeholder="Tags (comma separated)" className="input-field" />
        <select value={form.status} onChange={(e) => set('status', e.target.value)} className="input-field">
          <option value="DRAFT">Draft</option>
          <option value="PUBLISHED">Published</option>
          <option value="UNPUBLISHED">Unpublished</option>
          <option value="ARCHIVED">Archived</option>
        </select>
        <select value={form.visibility} onChange={(e) => set('visibility', e.target.value)} className="input-field">
          <option value="PUBLIC">Public</option>
          <option value="PREMIUM">Premium (PRO only)</option>
        </select>
      </div>

      <div className="flex gap-6">
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={form.featured} onChange={(e) => set('featured', e.target.checked)}
            className="h-4 w-4 rounded border-slate-300 text-brand focus:ring-brand dark:border-slate-600" />
          Featured
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={form.pinned} onChange={(e) => set('pinned', e.target.checked)}
            className="h-4 w-4 rounded border-slate-300 text-brand focus:ring-brand dark:border-slate-600" />
          Pinned
        </label>
      </div>

      <RichTextEditor value={form.content} onChange={(content) => set('content', content)} />
    </form>
  );
}
