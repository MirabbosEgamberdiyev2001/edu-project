import { createBrowserRouter, Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import GuestRoute from './GuestRoute';
import RoleRoute from './RoleRoute';
import { Role } from '@/types/user';

// Auth pages
import LoginPage from '@/features/auth/pages/LoginPage';
import RegisterPage from '@/features/auth/pages/RegisterPage';
import OtpVerifyPage from '@/features/auth/pages/OtpVerifyPage';
import ForgotPasswordPage from '@/features/auth/pages/ForgotPasswordPage';
import ResetPasswordPage from '@/features/auth/pages/ResetPasswordPage';
import ChangePasswordPage from '@/features/auth/pages/ChangePasswordPage';

// App pages
import AppLayout from '@/components/AppLayout';
import DashboardPage from '@/pages/DashboardPage';
import SubjectsPage from '@/features/subjects/pages/SubjectsPage';
import SubjectDetailPage from '@/features/subjects/pages/SubjectDetailPage';
import TopicsPage from '@/features/topics/pages/TopicsPage';
import QuestionsPage from '@/features/questions/pages/QuestionsPage';
import QuestionDetailPage from '@/features/questions/pages/QuestionDetailPage';
import TestsPage from '@/features/tests/pages/TestsPage';
import TestGeneratePage from '@/features/tests/pages/TestGeneratePage';
import TestDetailPage from '@/features/tests/pages/TestDetailPage';
import ProfilePage from '@/pages/ProfilePage';
import NotFoundPage from '@/pages/NotFoundPage';
import UnauthorizedPage from '@/pages/UnauthorizedPage';

// Admin pages
import AdminDashboardPage from '@/features/admin/pages/AdminDashboardPage';
import AdminUsersPage from '@/features/admin/pages/AdminUsersPage';
import AdminUserDetailPage from '@/features/admin/pages/AdminUserDetailPage';
import AuditLogPage from '@/features/admin/pages/AuditLogPage';
import ModerationPage from '@/features/admin/pages/ModerationPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/dashboard" replace />,
  },

  // Guest routes (redirect to dashboard if authenticated)
  {
    element: <GuestRoute />,
    children: [
      { path: '/auth/login', element: <LoginPage /> },
      { path: '/auth/register', element: <RegisterPage /> },
      { path: '/auth/otp-verify', element: <OtpVerifyPage /> },
      { path: '/auth/forgot-password', element: <ForgotPasswordPage /> },
      { path: '/auth/reset-password', element: <ResetPasswordPage /> },
    ],
  },

  // Protected routes (redirect to login if not authenticated)
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/subjects', element: <SubjectsPage /> },
          { path: '/subjects/:id', element: <SubjectDetailPage /> },
          { path: '/topics', element: <TopicsPage /> },
          { path: '/questions', element: <QuestionsPage /> },
          { path: '/questions/:id', element: <QuestionDetailPage /> },
          { path: '/tests', element: <TestsPage /> },
          { path: '/tests/generate', element: <TestGeneratePage /> },
          { path: '/tests/:id', element: <TestDetailPage /> },
          { path: '/profile', element: <ProfilePage /> },
          { path: '/settings/change-password', element: <ChangePasswordPage /> },

          // Moderation routes (moderator, admin, super_admin)
          {
            element: <RoleRoute allowedRoles={[Role.MODERATOR, Role.ADMIN, Role.SUPER_ADMIN]} />,
            children: [
              { path: '/admin/moderation', element: <ModerationPage /> },
            ],
          },

          // Admin routes (admin, super_admin only)
          {
            element: <RoleRoute allowedRoles={[Role.ADMIN, Role.SUPER_ADMIN]} />,
            children: [
              { path: '/admin', element: <AdminDashboardPage /> },
              { path: '/admin/users', element: <AdminUsersPage /> },
              { path: '/admin/users/:id', element: <AdminUserDetailPage /> },
              { path: '/admin/audit-logs', element: <AuditLogPage /> },
            ],
          },
        ],
      },
    ],
  },

  // Public routes
  { path: '/unauthorized', element: <UnauthorizedPage /> },
  { path: '*', element: <NotFoundPage /> },
]);
