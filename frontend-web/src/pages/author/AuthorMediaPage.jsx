/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState, useRef } from 'react';
import { Upload, Trash2, Copy, Check, Image, X, Loader2, ExternalLink } from 'lucide-react';
import api from '../../api/client';
import { PageLoader } from '../../components/ui/LoadingSpinner';

// Provides author media page wiring so the framework can apply the expected runtime behavior.
export function AuthorMediaPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [dragActive, setDragActive] = useState(false);
  const [copied, setCopied] = useState(null);
  const [preview, setPreview] = useState(null);
  const fileRef = useRef(null);

  const ALLOWED = ['image/jpeg', 'image/png', 'image/webp', 'image/jpg'];

  // Performs the fetch library workflow so callers do not duplicate this logic.
  const fetchLibrary = () => {
    api.get('/api/media/author/library')
      .then((r) => setItems(r.data.data || []))
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchLibrary(); }, []);

  // Performs the upload file workflow so callers do not duplicate this logic.
  const uploadFile = async (file) => {
    if (!file) return;
    if (!ALLOWED.includes(file.type)) {
      alert('Only JPG, JPEG, PNG, and WebP images are supported.');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      alert('File must be under 10MB.');
      return;
    }

    setUploading(true);
    setProgress(0);
    const formData = new FormData();
    formData.append('file', file);

    try {
      await api.post('/api/media/author/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (e) => {
          if (e.total) setProgress(Math.round((e.loaded / e.total) * 100));
        },
      });
      fetchLibrary();
    } catch (err) {
      alert(err?.response?.data?.message || 'Upload failed.');
    } finally {
      setUploading(false);
      setProgress(0);
    }
  };

  // Defines handle drop so related behavior stays grouped in one place.
  const handleDrop = (e) => {
    e.preventDefault();
    setDragActive(false);
    const file = e.dataTransfer.files?.[0];
    if (file) uploadFile(file);
  };

  // Performs the handle delete workflow so callers do not duplicate this logic.
  const handleDelete = async (mediaId) => {
    if (!confirm('Delete this media file?')) return;
    try {
      await api.delete(`/api/media/${mediaId}`);
      setItems((prev) => prev.filter((i) => i.mediaId !== mediaId));
      if (preview?.mediaId === mediaId) setPreview(null);
    } catch {
      alert('Failed to delete.');
    }
  };

  // Defines copy url so related behavior stays grouped in one place.
  const copyUrl = (url, mediaId) => {
    navigator.clipboard.writeText(url);
    setCopied(mediaId);
    setTimeout(() => setCopied(null), 2000);
  };

  if (loading) return <PageLoader />;

  return (
    <div>
      <p className="page-heading">Media</p>
      <h1 className="mt-2 font-display text-3xl font-bold">Media Library</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Upload and manage images for your posts. Click to preview, copy URL to use in posts.</p>

      {/* Upload zone */}
      <div
        className={`relative mt-8 flex cursor-pointer flex-col items-center justify-center rounded-2xl border-2 border-dashed p-10 transition-all ${
          dragActive
            ? 'border-brand bg-brand-50/50 dark:border-brand-400 dark:bg-brand-950/30'
            : 'border-slate-300 bg-slate-50 hover:border-brand hover:bg-brand-50/30 dark:border-slate-700 dark:bg-slate-800/50 dark:hover:border-brand-400'
        }`}
        onDragOver={(e) => { e.preventDefault(); setDragActive(true); }}
        onDragLeave={() => setDragActive(false)}
        onDrop={handleDrop}
        onClick={() => fileRef.current?.click()}
      >
        <input
          ref={fileRef}
          type="file"
          accept=".jpg,.jpeg,.png,.webp"
          className="hidden"
          onChange={(e) => { uploadFile(e.target.files?.[0]); e.target.value = ''; }}
        />
        {uploading ? (
          <div className="flex flex-col items-center gap-3">
            <Loader2 size={32} className="animate-spin text-brand" />
            <div className="h-2 w-48 overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700">
              <div className="h-full rounded-full bg-brand transition-all duration-300" style={{ width: `${progress}%` }} />
            </div>
            <p className="text-sm font-medium text-brand">{progress}% uploading...</p>
          </div>
        ) : (
          <>
            <Upload size={32} className="text-slate-400" />
            <p className="mt-3 text-sm font-medium">Drop image here or click to browse</p>
            <p className="mt-1 text-xs text-slate-400 dark:text-slate-500">JPG, PNG, WebP · Max 10MB</p>
          </>
        )}
      </div>

      {/* Grid */}
      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {items.map((item) => (
          <div
            key={item.mediaId}
            className="group relative overflow-hidden rounded-xl border border-slate-200 bg-white transition-all hover:shadow-lg dark:border-slate-800 dark:bg-slate-900"
          >
            <div className="relative cursor-pointer" onClick={() => setPreview(item)}>
              <img
                src={item.url}
                alt={item.altText || item.originalName}
                className="h-40 w-full object-cover transition-transform duration-300 group-hover:scale-105"
                onError={(e) => { e.target.src = ''; e.target.style.display = 'none'; }}
              />
              <div className="absolute inset-0 flex items-center justify-center bg-black/0 transition-all group-hover:bg-black/20">
                <ExternalLink size={20} className="text-white opacity-0 transition group-hover:opacity-100" />
              </div>
            </div>
            <div className="p-3">
              <p className="truncate text-sm font-medium">{item.originalName}</p>
              <p className="mt-0.5 text-xs text-slate-400">{item.sizeKb} KB · {item.mimeType?.split('/')[1]?.toUpperCase()}</p>
              <div className="mt-2 flex gap-1">
                <button
                  onClick={() => copyUrl(item.url, item.mediaId)}
                  className="flex-1 rounded-lg bg-slate-100 px-2 py-1.5 text-xs font-medium text-slate-600 transition hover:bg-brand-50 hover:text-brand dark:bg-slate-800 dark:text-slate-400 dark:hover:bg-brand-950 dark:hover:text-brand-300"
                >
                  {copied === item.mediaId ? <><Check size={12} /> Copied!</> : <><Copy size={12} /> Copy URL</>}
                </button>
                <button
                  onClick={() => handleDelete(item.mediaId)}
                  className="rounded-lg bg-red-50 px-2 py-1.5 text-xs font-medium text-red-600 transition hover:bg-red-100 dark:bg-red-950/30 dark:text-red-400 dark:hover:bg-red-950/50"
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {items.length === 0 && !uploading && (
        <div className="mt-10 flex flex-col items-center rounded-2xl border border-dashed border-slate-300 bg-slate-50 py-16 dark:border-slate-700 dark:bg-slate-800/50">
          <Image size={40} className="text-slate-300 dark:text-slate-600" />
          <p className="mt-4 font-medium text-slate-500">No media uploaded yet</p>
          <p className="mt-1 text-sm text-slate-400">Upload your first image using the area above.</p>
        </div>
      )}

      {/* Preview modal */}
      {preview && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm" onClick={() => setPreview(null)}>
          <div className="relative max-h-[90vh] max-w-4xl overflow-hidden rounded-2xl bg-white shadow-2xl dark:bg-slate-900" onClick={(e) => e.stopPropagation()}>
            <button onClick={() => setPreview(null)} className="absolute right-3 top-3 z-10 rounded-full bg-black/40 p-1.5 text-white transition hover:bg-black/60">
              <X size={16} />
            </button>
            <img src={preview.url} alt={preview.originalName} className="max-h-[70vh] w-full object-contain" />
            <div className="flex items-center justify-between p-4">
              <div>
                <p className="font-medium">{preview.originalName}</p>
                <p className="text-sm text-slate-500">{preview.sizeKb} KB · {preview.mimeType}</p>
              </div>
              <button onClick={() => copyUrl(preview.url, preview.mediaId)} className="btn-primary text-sm">
                {copied === preview.mediaId ? <><Check size={14} /> Copied</> : <><Copy size={14} /> Copy URL</>}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
