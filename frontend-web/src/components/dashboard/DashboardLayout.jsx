/*
 * This file provides reusable UI behavior for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

// Defines dashboard layout so related behavior stays grouped in one place.
export function DashboardLayout({ title, links }) {
  const { user } = useAuth();

  return (
    <div className="mx-auto grid max-w-7xl gap-6 px-4 py-8 md:grid-cols-[260px_1fr] md:px-8">
      {/* Sidebar */}
      <aside className="card p-5">
        <div className="mb-6">
          <p className="page-heading text-xs">{title}</p>
          <h2 className="mt-2 font-display text-2xl font-bold">{user?.fullName || 'Dashboard'}</h2>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{user?.email}</p>
        </div>
        <nav className="space-y-1">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.end}
              className={({ isActive }) =>
                `flex items-center rounded-xl px-4 py-2.5 text-sm font-medium transition-all duration-200 ${
                  isActive
                    ? 'bg-brand text-white shadow-brand-glow'
                    : 'text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      {/* Content */}
      <section className="card p-6 md:p-8">
        <Outlet />
      </section>
    </div>
  );
}
