/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { Plus, Folder, Hash, Trash2, AlertTriangle } from 'lucide-react';
import api from '../../api/client';

// Defines admin categories page so related behavior stays grouped in one place.
export function AdminCategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [tags, setTags] = useState([]);
  const [categoryName, setCategoryName] = useState('');
  const [tagName, setTagName] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(null);

  // Performs the load workflow so callers do not duplicate this logic.
  const load = () => Promise.all([
    api.get('/api/categories/public/categories').catch(() => ({ data: { data: [] } })),
    api.get('/api/categories/public/tags').catch(() => ({ data: { data: [] } })),
  ]).then(([c, t]) => { setCategories(c.data.data); setTags(t.data.data); });

  useEffect(() => { load(); }, []);

  // Defines add category so related behavior stays grouped in one place.
  const addCategory = async () => {
    if (!categoryName.trim()) return;
    await api.post('/api/categories/admin/categories', { name: categoryName });
    setCategoryName('');
    load();
  };

  // Defines add tag so related behavior stays grouped in one place.
  const addTag = async () => {
    if (!tagName.trim()) return;
    await api.post('/api/categories/admin/tags', { name: tagName });
    setTagName('');
    load();
  };

  // Performs the delete category workflow so callers do not duplicate this logic.
  const deleteCategory = async (id) => {
    try {
      await api.delete(`/api/categories/admin/categories/${id}`);
      setConfirmDelete(null);
      load();
    } catch {
      alert('Failed to delete category.');
    }
  };

  // Performs the delete tag workflow so callers do not duplicate this logic.
  const deleteTag = async (id) => {
    try {
      await api.delete(`/api/categories/admin/tags/${id}`);
      setConfirmDelete(null);
      load();
    } catch {
      alert('Failed to delete tag.');
    }
  };

  return (
    <div className="grid gap-8 md:grid-cols-2">
      {/* Categories */}
      <div>
        <h1 className="font-display text-3xl font-bold">Categories</h1>
        <div className="mt-5 flex gap-3">
          <input value={categoryName} onChange={(e) => setCategoryName(e.target.value)} placeholder="New category" className="input-field flex-1"
            onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addCategory())} />
          <button onClick={addCategory} className="btn-primary"><Plus size={16} /> Add</button>
        </div>
        <div className="mt-6 space-y-2">
          {categories.map((item) => (
            <div key={item.categoryId} className="flex items-center gap-3 rounded-xl border border-slate-100 bg-slate-50 p-3 transition-colors dark:border-slate-800 dark:bg-slate-800/50">
              <Folder size={16} className="text-brand" />
              <span className="font-medium">{item.name}</span>
              <span className="ml-auto text-sm text-slate-500 dark:text-slate-400">{item.postCount} posts</span>
              {confirmDelete === `cat-${item.categoryId}` ? (
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => deleteCategory(item.categoryId)}
                    className="rounded-lg bg-red-100 px-2 py-1 text-xs font-medium text-red-700 transition hover:bg-red-200 dark:bg-red-950/40 dark:text-red-400"
                  >
                    Confirm
                  </button>
                  <button
                    onClick={() => setConfirmDelete(null)}
                    className="rounded-lg bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600 transition hover:bg-slate-200 dark:bg-slate-700 dark:text-slate-300"
                  >
                    Cancel
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setConfirmDelete(`cat-${item.categoryId}`)}
                  className="rounded-lg p-1.5 text-slate-400 transition hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-950/30 dark:hover:text-red-400"
                  title="Delete category"
                >
                  <Trash2 size={14} />
                </button>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Tags */}
      <div>
        <h2 className="font-display text-3xl font-bold">Tags</h2>
        <div className="mt-5 flex gap-3">
          <input value={tagName} onChange={(e) => setTagName(e.target.value)} placeholder="New tag" className="input-field flex-1"
            onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addTag())} />
          <button onClick={addTag} className="btn-primary"><Plus size={16} /> Add</button>
        </div>
        <div className="mt-6 space-y-2">
          {tags.map((item) => (
            <div key={item.tagId} className="flex items-center gap-3 rounded-xl border border-slate-100 bg-slate-50 p-3 transition-colors dark:border-slate-800 dark:bg-slate-800/50">
              <Hash size={16} className="text-purple-500" />
              <span className="font-medium">{item.name}</span>
              <span className="ml-auto text-sm text-slate-500 dark:text-slate-400">{item.postCount} posts</span>
              {confirmDelete === `tag-${item.tagId}` ? (
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => deleteTag(item.tagId)}
                    className="rounded-lg bg-red-100 px-2 py-1 text-xs font-medium text-red-700 transition hover:bg-red-200 dark:bg-red-950/40 dark:text-red-400"
                  >
                    Confirm
                  </button>
                  <button
                    onClick={() => setConfirmDelete(null)}
                    className="rounded-lg bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600 transition hover:bg-slate-200 dark:bg-slate-700 dark:text-slate-300"
                  >
                    Cancel
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setConfirmDelete(`tag-${item.tagId}`)}
                  className="rounded-lg p-1.5 text-slate-400 transition hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-950/30 dark:hover:text-red-400"
                  title="Delete tag"
                >
                  <Trash2 size={14} />
                </button>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Safety notice */}
      <div className="col-span-full mt-4 flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 dark:border-amber-800/40 dark:bg-amber-950/20">
        <AlertTriangle size={18} className="mt-0.5 shrink-0 text-amber-600 dark:text-amber-400" />
        <div>
          <p className="text-sm font-medium text-amber-800 dark:text-amber-300">Safe Deletion</p>
          <p className="mt-1 text-xs text-amber-700 dark:text-amber-400">
            Deleting a category or tag will remove the association with posts but will <strong>not</strong> delete any posts. Posts will remain accessible and can be re-categorized later.
          </p>
        </div>
      </div>
    </div>
  );
}
