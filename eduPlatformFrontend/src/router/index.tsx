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

// Groups pages
import GroupsPage from '@/features/groups/pages/GroupsPage';
import GroupDetailPage from '@/features/groups/pages/GroupDetailPage';

// Assignments pages (Teacher)
import AssignmentsPage from '@/features/assignments/pages/AssignmentsPage';
import AssignmentDetailPage from '@/features/assignments/pages/AssignmentDetailPage';
import LiveMonitoringPage from '@/features/assignments/pages/LiveMonitoringPage';
import AssignmentResultsPage from '@/features/assignments/pages/AssignmentResultsPage';

// Test Taking pages (Student)
import AvailableAssignmentsPage from '@/features/testTaking/pages/AvailableAssignmentsPage';
import ExamPage from '@/features/testTaking/pages/ExamPage';
import MyAttemptsPage from '@/features/testTaking/pages/MyAttemptsPage';
import AttemptResultPage from '@/features/testTaking/pages/AttemptResultPage';

// Analytics pages
import TeacherAnalyticsPage from '@/features/analytics/pages/TeacherAnalyticsPage';
import StudentAnalyticsPage from '@/features/analytics/pages/StudentAnalyticsPage';
import GroupAnalyticsPage from '@/features/analytics/pages/GroupAnalyticsPage';

// Parent pages
import ParentDashboardPage from '@/features/parent/pages/ParentDashboardPage';
import ChildDashboardPage from '@/features/parent/pages/ChildDashboardPage';
import StudentPairingPage from '@/features/parent/pages/StudentPairingPage';

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
      // Full-screen routes (outside AppLayout - no sidebar)
      {
        element: <RoleRoute allowedRoles={[Role.STUDENT]} />,
        children: [
          { path: '/exam/:attemptId', element: <ExamPage /> },
        ],
      },

      {
        element: <AppLayout />,
        children: [
          // Common routes (all authenticated users)
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/profile', element: <ProfilePage /> },
          { path: '/settings/change-password', element: <ChangePasswordPage /> },

          // Teacher/Admin content management routes
          {
            element: <RoleRoute allowedRoles={[Role.TEACHER, Role.ADMIN, Role.SUPER_ADMIN]} />,
            children: [
              { path: '/subjects', element: <SubjectsPage /> },
              { path: '/subjects/:id', element: <SubjectDetailPage /> },
              { path: '/topics', element: <TopicsPage /> },
              { path: '/questions', element: <QuestionsPage /> },
              { path: '/questions/:id', element: <QuestionDetailPage /> },
              { path: '/tests', element: <TestsPage /> },
              { path: '/tests/generate', element: <TestGeneratePage /> },
              { path: '/tests/:id', element: <TestDetailPage /> },
              { path: '/groups', element: <GroupsPage /> },
              { path: '/groups/:id', element: <GroupDetailPage /> },
              { path: '/assignments', element: <AssignmentsPage /> },
              { path: '/assignments/:id', element: <AssignmentDetailPage /> },
              { path: '/assignments/:id/live', element: <LiveMonitoringPage /> },
              { path: '/assignments/:id/results', element: <AssignmentResultsPage /> },
              { path: '/analytics/teacher', element: <TeacherAnalyticsPage /> },
              { path: '/analytics/group/:groupId', element: <GroupAnalyticsPage /> },
            ],
          },

          // Moderator: questions access
          {
            element: <RoleRoute allowedRoles={[Role.MODERATOR]} />,
            children: [
              { path: '/questions', element: <QuestionsPage /> },
              { path: '/questions/:id', element: <QuestionDetailPage /> },
            ],
          },

          // Student routes
          {
            element: <RoleRoute allowedRoles={[Role.STUDENT]} />,
            children: [
              { path: '/my-tests', element: <AvailableAssignmentsPage /> },
              { path: '/my-attempts', element: <MyAttemptsPage /> },
              { path: '/attempt-result/:attemptId', element: <AttemptResultPage /> },
              { path: '/analytics/student', element: <StudentAnalyticsPage /> },
              { path: '/pairing', element: <StudentPairingPage /> },
            ],
          },

          // Parent routes
          {
            element: <RoleRoute allowedRoles={[Role.PARENT]} />,
            children: [
              { path: '/my-children', element: <ParentDashboardPage /> },
              { path: '/my-children/:childId', element: <ChildDashboardPage /> },
            ],
          },

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
