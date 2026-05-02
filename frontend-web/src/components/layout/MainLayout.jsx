/*
 * This file provides reusable UI behavior for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Bell, Menu, Moon, Sun, X, LogOut, ChevronDown, Feather, Crown } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useNotifications } from '../../context/NotificationContext';
import { resolveDashboardLabel, resolveDashboardPath } from '../../utils/navigation';
import { FeedbackWidget } from '../ui/FeedbackWidget';

// Defines main layout so related behavior stays grouped in one place.
export function MainLayout() {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const { unreadCount } = useNotifications();
  const [menuOpen, setMenuOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const navigate = useNavigate();

  const dashboardPath = resolveDashboardPath(user);
  const dashboardLabel = resolveDashboardLabel(user);

  const navLinks = [
    { to: '/', label: 'Home' },
    { to: '/search', label: 'Explore' },
    { to: '/newsletter', label: 'Newsletter' },
    ...(user ? [{ to: dashboardPath, label: dashboardLabel }] : []),
  ];

  // Defines handle logout so related behavior stays grouped in one place.
  const handleLogout = () => {
    logout();
    setProfileOpen(false);
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 transition-colors duration-300 dark:bg-slate-950 dark:text-slate-100">
      {/* Navbar */}
      <header className="sticky top-0 z-30 border-b border-slate-200/80 bg-white/80 backdrop-blur-xl transition-colors duration-300 dark:border-slate-800/80 dark:bg-slate-950/80">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3 md:px-8">
          {/* Logo */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMenuOpen((v) => !v)}
              className="rounded-lg p-2 transition-colors hover:bg-slate-100 md:hidden dark:hover:bg-slate-800"
            >
              {menuOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
            <NavLink to="/" className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand text-white">
                <Feather size={16} />
              </div>
              <span className="font-display text-xl font-bold text-slate-900 dark:text-white">InkWell</span>
            </NavLink>
          </div>

          {/* Desktop nav */}
          <nav className="hidden items-center gap-1 md:flex">
            {navLinks.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.to === '/'}
                className={({ isActive }) =>
                  `rounded-lg px-3 py-2 text-sm font-medium transition-colors duration-200 ${
                    isActive
                      ? 'bg-brand-50 text-brand-700 dark:bg-brand-950 dark:text-brand-300'
                      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-200'
                  }`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>

          {/* Right actions */}
          <div className="flex items-center gap-2">
            {/* Theme toggle */}
            <button
              onClick={toggleTheme}
              className="rounded-lg p-2 text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-700 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-200"
              aria-label="Toggle theme"
            >
              {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
            </button>

            {/* Notifications */}
            {user && (
              <NavLink
                to="/notifications"
                className="relative rounded-lg p-2 text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-700 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-200"
              >
                <Bell size={18} />
                {unreadCount > 0 && (
                  <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-coral text-[10px] font-bold text-white">
                    {unreadCount}
                  </span>
                )}
              </NavLink>
            )}

            {/* Profile / Login */}
            {user ? (
              <div className="relative">
                <button
                  onClick={() => setProfileOpen((v) => !v)}
                  className="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium transition-colors hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-800"
                >
                  <div className="flex h-6 w-6 items-center justify-center rounded-full bg-brand-100 text-xs font-bold text-brand-700 dark:bg-brand-900 dark:text-brand-300">
                    {user.fullName?.[0]?.toUpperCase() || 'U'}
                  </div>
                  <span className="hidden sm:inline">{user.fullName?.split(' ')[0]}</span>
                  {user.subscriptionTier === 'PRO' && user.subscriptionStatus === 'ACTIVE' && (
                    <Crown size={12} className="text-yellow-500" />
                  )}
                  <ChevronDown size={14} className={`transition-transform ${profileOpen ? 'rotate-180' : ''}`} />
                </button>

                {profileOpen && (
                  <>
                    <div className="fixed inset-0 z-40" onClick={() => setProfileOpen(false)} />
                    <div className="absolute right-0 z-50 mt-2 w-56 rounded-xl border border-slate-200 bg-white p-2 shadow-glow dark:border-slate-700 dark:bg-slate-900">
                      <div className="border-b border-slate-100 px-3 py-2 dark:border-slate-800">
                        <p className="text-sm font-medium">{user.fullName}</p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">{user.email}</p>
                        <div className="mt-1 flex items-center gap-1.5">
                          <span className="badge-brand">{user.role}</span>
                          {user.subscriptionTier === 'PRO' && user.subscriptionStatus === 'ACTIVE' && (
                            <span className="inline-flex items-center gap-0.5 rounded-md bg-yellow-100 px-1.5 py-0.5 text-[10px] font-bold text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400">
                              <Crown size={10} /> PRO
                            </span>
                          )}
                        </div>
                      </div>
                      <NavLink
                        to="/profile"
                        onClick={() => setProfileOpen(false)}
                        className="mt-1 block rounded-lg px-3 py-2 text-sm transition-colors hover:bg-slate-100 dark:hover:bg-slate-800"
                      >
                        My Profile
                      </NavLink>
                      {user.role === 'READER' && (
                        <>
                          <NavLink
                            to="/bookmarks"
                            onClick={() => setProfileOpen(false)}
                            className="block rounded-lg px-3 py-2 text-sm transition-colors hover:bg-slate-100 dark:hover:bg-slate-800"
                          >
                            Bookmarks
                          </NavLink>
                          <NavLink
                            to="/history"
                            onClick={() => setProfileOpen(false)}
                            className="block rounded-lg px-3 py-2 text-sm transition-colors hover:bg-slate-100 dark:hover:bg-slate-800"
                          >
                            Reading History
                          </NavLink>
                        </>
                      )}
                      {dashboardPath !== '/profile' && (
                        <NavLink
                          to={dashboardPath}
                          onClick={() => setProfileOpen(false)}
                          className="block rounded-lg px-3 py-2 text-sm transition-colors hover:bg-slate-100 dark:hover:bg-slate-800"
                        >
                          {dashboardLabel}
                        </NavLink>
                      )}
                      <button
                        onClick={handleLogout}
                        className="mt-1 flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-red-600 transition-colors hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/30"
                      >
                        <LogOut size={14} />
                        Sign out
                      </button>
                    </div>
                  </>
                )}
              </div>
            ) : (
              <NavLink to="/login" className="btn-primary text-sm">
                Sign in
              </NavLink>
            )}
          </div>
        </div>

        {/* Mobile menu */}
        {menuOpen && (
          <div className="animate-slide-in border-t border-slate-200 px-4 py-3 md:hidden dark:border-slate-800">
            <nav className="space-y-1">
              {navLinks.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  onClick={() => setMenuOpen(false)}
                  className={({ isActive }) =>
                    `block rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-brand-50 text-brand-700 dark:bg-brand-950 dark:text-brand-300'
                        : 'text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800'
                    }`
                  }
                >
                  {link.label}
                </NavLink>
              ))}
              {user && (
                <button
                  onClick={handleLogout}
                  className="block w-full rounded-lg px-4 py-2.5 text-left text-sm font-medium text-red-600 transition-colors hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950/30"
                >
                  Sign out
                </button>
              )}
            </nav>
          </div>
        )}
      </header>

      {/* Main content */}
      <main className="min-h-[calc(100vh-64px)]">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-200 bg-white px-4 py-8 transition-colors duration-300 dark:border-slate-800 dark:bg-slate-950">
        <div className="mx-auto max-w-7xl text-center text-sm text-slate-500 dark:text-slate-400">
          <p>© {new Date().getFullYear()} InkWell Publishing Platform. Built with React, Spring Boot & Microservices.</p>
        </div>
      </footer>

      {/* Global Feedback Widget */}
      <FeedbackWidget />
    </div>
  );
}
