/*
 * This file provides frontend application configuration and wiring for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { Navigate, Route, Routes } from 'react-router-dom';
import { MainLayout } from './components/layout/MainLayout';
import { ProtectedRoute } from './components/layout/ProtectedRoute';
import { DashboardLayout } from './components/dashboard/DashboardLayout';
import { HomePage } from './pages/public/HomePage';
import { PostDetailPage } from './pages/public/PostDetailPage';
import { SearchPage } from './pages/public/SearchPage';
import { CategoryPage } from './pages/public/CategoryPage';
import { TagPage } from './pages/public/TagPage';
import { AuthorProfilePage } from './pages/public/AuthorProfilePage';
import { LoginPage, ForgotPasswordPage } from './pages/public/LoginPage';
import { RegisterPage } from './pages/public/RegisterPage';
import { NewsletterPage } from './pages/public/NewsletterPage';
import { NewsletterConfirmPage } from './pages/public/NewsletterConfirmPage';
import { OAuthSuccessPage } from './pages/public/OAuthSuccessPage';

import { ProfilePage } from './pages/reader/ProfilePage';
import { NotificationsPage } from './pages/reader/NotificationsPage';
import { BookmarksPage } from './pages/reader/BookmarksPage';
import { HistoryPage } from './pages/reader/HistoryPage';
import { AuthorOverviewPage } from './pages/author/AuthorOverviewPage';
import { AuthorPostsPage } from './pages/author/AuthorPostsPage';
import { AuthorEditorPage } from './pages/author/AuthorEditorPage';
import { AuthorCommentsPage } from './pages/author/AuthorCommentsPage';
import { AuthorMediaPage } from './pages/author/AuthorMediaPage';
import { AuthorAnalyticsPage } from './pages/author/AuthorAnalyticsPage';
import { AuthorFollowersPage } from './pages/author/AuthorFollowersPage';
import { AdminOverviewPage } from './pages/admin/AdminOverviewPage';
import { AdminUsersPage } from './pages/admin/AdminUsersPage';
import { AdminPostsPage } from './pages/admin/AdminPostsPage';
import { AdminCategoriesPage } from './pages/admin/AdminCategoriesPage';
import { AdminCommentsPage } from './pages/admin/AdminCommentsPage';
import { AdminNewsletterPage } from './pages/admin/AdminNewsletterPage';
import { AdminNotificationsPage } from './pages/admin/AdminNotificationsPage';
import { AdminAuditPage } from './pages/admin/AdminAuditPage';
import { AdminAuthorRequestsPage } from './pages/admin/AdminAuthorRequestsPage';
import { AdminFeedbackPage } from './pages/admin/AdminFeedbackPage';

const authorLinks = [
  { to: '/author', label: 'Overview', end: true },
  { to: '/author/posts', label: 'My Posts' },
  { to: '/author/posts/new', label: 'Create Post' },
  { to: '/author/comments', label: 'Comments' },
  { to: '/author/media', label: 'Media' },
  { to: '/author/analytics', label: 'Analytics' },
  { to: '/author/followers', label: 'Followers' },
];

const adminLinks = [
  { to: '/admin', label: 'Overview', end: true },
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/posts', label: 'Posts' },
  { to: '/admin/categories', label: 'Categories & Tags' },
  { to: '/admin/comments', label: 'Comments' },
  { to: '/admin/author-requests', label: 'Author Requests' },
  { to: '/admin/feedback', label: 'Bug Reports' },
  { to: '/admin/newsletter', label: 'Newsletter' },
  { to: '/admin/notifications', label: 'Broadcasts' },
  { to: '/admin/audit-logs', label: 'Audit Logs' },
];

// Defines app so related behavior stays grouped in one place.
export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="posts/:slug" element={<PostDetailPage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="explore" element={<Navigate to="/search" replace />} />
        <Route path="categories/:slug" element={<CategoryPage />} />
        <Route path="tags/:slug" element={<TagPage />} />
        <Route path="authors/:authorId" element={<AuthorProfilePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="newsletter" element={<NewsletterPage />} />
        <Route path="newsletter/confirm" element={<NewsletterConfirmPage />} />
        <Route path="oauth/success" element={<OAuthSuccessPage />} />


        <Route element={<ProtectedRoute roles={['READER', 'AUTHOR', 'ADMIN']} />}>
          <Route path="profile" element={<ProfilePage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="bookmarks" element={<BookmarksPage />} />
          <Route path="history" element={<HistoryPage />} />
        </Route>

        <Route element={<ProtectedRoute roles={['AUTHOR', 'ADMIN']} />}>
          <Route path="author" element={<DashboardLayout title="Author Studio" links={authorLinks} />}>
            <Route index element={<AuthorOverviewPage />} />
            <Route path="posts" element={<AuthorPostsPage />} />
            <Route path="posts/new" element={<AuthorEditorPage />} />
            <Route path="posts/:postId/edit" element={<AuthorEditorPage />} />
            <Route path="comments" element={<AuthorCommentsPage />} />
            <Route path="media" element={<AuthorMediaPage />} />
            <Route path="analytics" element={<AuthorAnalyticsPage />} />
            <Route path="followers" element={<AuthorFollowersPage />} />
          </Route>
        </Route>

        <Route element={<ProtectedRoute roles={['ADMIN']} />}>
          <Route path="admin" element={<DashboardLayout title="Admin Console" links={adminLinks} />}>
            <Route index element={<AdminOverviewPage />} />
            <Route path="users" element={<AdminUsersPage />} />
            <Route path="posts" element={<AdminPostsPage />} />
            <Route path="categories" element={<AdminCategoriesPage />} />
            <Route path="comments" element={<AdminCommentsPage />} />
            <Route path="newsletter" element={<AdminNewsletterPage />} />
            <Route path="notifications" element={<AdminNotificationsPage />} />
            <Route path="author-requests" element={<AdminAuthorRequestsPage />} />
            <Route path="feedback" element={<AdminFeedbackPage />} />
            <Route path="audit-logs" element={<AdminAuditPage />} />
          </Route>
        </Route>
      </Route>
    </Routes>
  );
}
