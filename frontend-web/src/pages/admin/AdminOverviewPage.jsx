/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect, useState } from 'react';
import { Users, FileText, MessageSquare, Mail, Eye, BarChart3 } from 'lucide-react';
import api from '../../api/client';

// Defines admin overview page so related behavior stays grouped in one place.
export function AdminOverviewPage() {
  const [stats, setStats] = useState({ users: 0, posts: 0, comments: 0, subscribers: 0, totalViews: 0, totalPublishedPosts: 0 });

  useEffect(() => {
    Promise.all([
      api.get('/api/auth/public/search', { params: { query: '' } }).catch(() => ({ data: { data: [] } })),
      api.get('/api/posts/admin').catch(() => ({ data: { data: { totalElements: 0 } } })),
      api.get('/api/comments/admin/count').catch(() => ({ data: { data: 0 } })),
      api.get('/api/newsletter/admin/subscribers').catch(() => ({ data: { data: [] } })),
      api.get('/api/posts/public/stats').catch(() => ({ data: { data: { totalViews: 0, totalPublishedPosts: 0 } } })),
    ]).then(([users, posts, comments, subscribers, platformStats]) => setStats({
      users: users.data.data.length,
      posts: posts.data.data.totalElements,
      comments: comments.data.data,
      subscribers: subscribers.data.data.filter(s => s.status !== 'UNSUBSCRIBED').length,
      totalViews: platformStats.data.data?.totalViews || 0,
      totalPublishedPosts: platformStats.data.data?.totalPublishedPosts || 0,
    }));
  }, []);

  const cards = [
    { icon: <Users size={20} />, label: 'Users', value: stats.users, color: 'text-blue-500' },
    { icon: <FileText size={20} />, label: 'Posts', value: stats.posts, color: 'text-green-500' },
    { icon: <MessageSquare size={20} />, label: 'Comments', value: stats.comments, color: 'text-purple-500' },
    { icon: <Mail size={20} />, label: 'Subscribers', value: stats.subscribers, color: 'text-rose-500' },
    { icon: <Eye size={20} />, label: 'Total Views', value: stats.totalViews.toLocaleString(), color: 'text-amber-500' },
    { icon: <BarChart3 size={20} />, label: 'Published Posts', value: stats.totalPublishedPosts, color: 'text-cyan-500' },
  ];

  return (
    <div>
      <p className="page-heading">Admin control center</p>
      <h1 className="mt-2 font-display text-3xl font-bold">Platform Overview</h1>
      <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Monitor adoption, content throughput, website analytics, and newsletter reach.</p>
      <div className="mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {cards.map((c) => (
          <div key={c.label} className="stat-card">
            <div className={c.color}>{c.icon}</div>
            <p className="mt-3 text-3xl font-bold">{c.value}</p>
            <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{c.label}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
