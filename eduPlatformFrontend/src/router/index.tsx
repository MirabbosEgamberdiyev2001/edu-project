import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import GuestRoute from './GuestRoute';
import RoleRoute from './RoleRoute';
import { Role } from '@/types/user';
import { Box, CircularProgress } from '@mui/material';

// Shared layout — not lazy (needed immediately)
import AppLayout from '@/components/AppLayout';

// Lazy loading fallback
const PageLoader = () => (
  <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh">
    <CircularProgress />
  </Box>
);

const Lazy = ({ component: Component }: { component: React.LazyExoticComponent<React.ComponentType> }) => (
  <Suspense fallback={<PageLoader />}>
    <Component />
  </Suspense>
);

// Auth pages
const LoginPage = lazy(() => import('@/features/auth/pages/LoginPage'));
const RegisterPage = lazy(() => import('@/features/auth/pages/RegisterPage'));
const OtpVerifyPage = lazy(() => import('@/features/auth/pages/OtpVerifyPage'));
const ForgotPasswordPage = lazy(() => import('@/features/auth/pages/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('@/features/auth/pages/ResetPasswordPage'));
const ChangePasswordPage = lazy(() => import('@/features/auth/pages/ChangePasswordPage'));

// App pages
const DashboardPage = lazy(() => import('@/pages/DashboardPage'));
const SubjectsPage = lazy(() => import('@/features/subjects/pages/SubjectsPage'));
const SubjectDetailPage = lazy(() => import('@/features/subjects/pages/SubjectDetailPage'));
const TopicsPage = lazy(() => import('@/features/topics/pages/TopicsPage'));
const QuestionsPage = lazy(() => import('@/features/questions/pages/QuestionsPage'));
const QuestionDetailPage = lazy(() => import('@/features/questions/pages/QuestionDetailPage'));
const TestsPage = lazy(() => import('@/features/tests/pages/TestsPage'));
const TestGeneratePage = lazy(() => import('@/features/tests/pages/TestGeneratePage'));
const TestDetailPage = lazy(() => import('@/features/tests/pages/TestDetailPage'));
const ProfilePage = lazy(() => import('@/pages/ProfilePage'));
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'));
const UnauthorizedPage = lazy(() => import('@/pages/UnauthorizedPage'));

// Groups pages
const GroupsPage = lazy(() => import('@/features/groups/pages/GroupsPage'));
const GroupDetailPage = lazy(() => import('@/features/groups/pages/GroupDetailPage'));

// Assignments pages (Teacher)
const AssignmentsPage = lazy(() => import('@/features/assignments/pages/AssignmentsPage'));
const AssignmentDetailPage = lazy(() => import('@/features/assignments/pages/AssignmentDetailPage'));
const LiveMonitoringPage = lazy(() => import('@/features/assignments/pages/LiveMonitoringPage'));
const AssignmentResultsPage = lazy(() => import('@/features/assignments/pages/AssignmentResultsPage'));

// Test Taking pages (Student)
const AvailableAssignmentsPage = lazy(() => import('@/features/testTaking/pages/AvailableAssignmentsPage'));
const ExamPage = lazy(() => import('@/features/testTaking/pages/ExamPage'));
const MyAttemptsPage = lazy(() => import('@/features/testTaking/pages/MyAttemptsPage'));
const AttemptResultPage = lazy(() => import('@/features/testTaking/pages/AttemptResultPage'));

// Analytics pages
const TeacherAnalyticsPage = lazy(() => import('@/features/analytics/pages/TeacherAnalyticsPage'));
const StudentAnalyticsPage = lazy(() => import('@/features/analytics/pages/StudentAnalyticsPage'));
const GroupAnalyticsPage = lazy(() => import('@/features/analytics/pages/GroupAnalyticsPage'));

// Parent pages
const ParentDashboardPage = lazy(() => import('@/features/parent/pages/ParentDashboardPage'));
const ChildDashboardPage = lazy(() => import('@/features/parent/pages/ChildDashboardPage'));
const StudentPairingPage = lazy(() => import('@/features/parent/pages/StudentPairingPage'));

// Admin pages
const AdminDashboardPage = lazy(() => import('@/features/admin/pages/AdminDashboardPage'));
const AdminUsersPage = lazy(() => import('@/features/admin/pages/AdminUsersPage'));
const AdminUserDetailPage = lazy(() => import('@/features/admin/pages/AdminUserDetailPage'));
const AuditLogPage = lazy(() => import('@/features/admin/pages/AuditLogPage'));
const ModerationPage = lazy(() => import('@/features/admin/pages/ModerationPage'));

// Public test page
const PublicTestPage = lazy(() => import('@/features/tests/pages/PublicTestPage'));

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Navigate to="/dashboard" replace />,
  },

  // Guest routes (redirect to dashboard if authenticated)
  {
    element: <GuestRoute />,
    children: [
      { path: '/auth/login', element: <Lazy component={LoginPage} /> },
      { path: '/auth/register', element: <Lazy component={RegisterPage} /> },
      { path: '/auth/otp-verify', element: <Lazy component={OtpVerifyPage} /> },
      { path: '/auth/forgot-password', element: <Lazy component={ForgotPasswordPage} /> },
      { path: '/auth/reset-password', element: <Lazy component={ResetPasswordPage} /> },
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
          { path: '/exam/:attemptId', element: <Lazy component={ExamPage} /> },
        ],
      },

      {
        element: <AppLayout />,
        children: [
          // Common routes (all authenticated users)
          { path: '/dashboard', element: <Lazy component={DashboardPage} /> },
          { path: '/profile', element: <Lazy component={ProfilePage} /> },
          { path: '/settings/change-password', element: <Lazy component={ChangePasswordPage} /> },

          // Teacher/Admin content management routes
          {
            element: <RoleRoute allowedRoles={[Role.TEACHER, Role.ADMIN, Role.SUPER_ADMIN]} />,
            children: [
              { path: '/subjects', element: <Lazy component={SubjectsPage} /> },
              { path: '/subjects/:id', element: <Lazy component={SubjectDetailPage} /> },
              { path: '/topics', element: <Lazy component={TopicsPage} /> },
              { path: '/questions', element: <Lazy component={QuestionsPage} /> },
              { path: '/questions/:id', element: <Lazy component={QuestionDetailPage} /> },
              { path: '/tests', element: <Lazy component={TestsPage} /> },
              { path: '/tests/generate', element: <Lazy component={TestGeneratePage} /> },
              { path: '/tests/:id', element: <Lazy component={TestDetailPage} /> },
              { path: '/groups', element: <Lazy component={GroupsPage} /> },
              { path: '/groups/:id', element: <Lazy component={GroupDetailPage} /> },
              { path: '/assignments', element: <Lazy component={AssignmentsPage} /> },
              { path: '/assignments/:id', element: <Lazy component={AssignmentDetailPage} /> },
              { path: '/assignments/:id/live', element: <Lazy component={LiveMonitoringPage} /> },
              { path: '/assignments/:id/results', element: <Lazy component={AssignmentResultsPage} /> },
              { path: '/analytics/teacher', element: <Lazy component={TeacherAnalyticsPage} /> },
              { path: '/analytics/group/:groupId', element: <Lazy component={GroupAnalyticsPage} /> },
            ],
          },

          // Moderator: questions access
          {
            element: <RoleRoute allowedRoles={[Role.MODERATOR]} />,
            children: [
              { path: '/questions', element: <Lazy component={QuestionsPage} /> },
              { path: '/questions/:id', element: <Lazy component={QuestionDetailPage} /> },
            ],
          },

          // Student routes
          {
            element: <RoleRoute allowedRoles={[Role.STUDENT]} />,
            children: [
              { path: '/my-tests', element: <Lazy component={AvailableAssignmentsPage} /> },
              { path: '/my-attempts', element: <Lazy component={MyAttemptsPage} /> },
              { path: '/attempt-result/:attemptId', element: <Lazy component={AttemptResultPage} /> },
              { path: '/analytics/student', element: <Lazy component={StudentAnalyticsPage} /> },
              { path: '/pairing', element: <Lazy component={StudentPairingPage} /> },
            ],
          },

          // Parent routes
          {
            element: <RoleRoute allowedRoles={[Role.PARENT]} />,
            children: [
              { path: '/my-children', element: <Lazy component={ParentDashboardPage} /> },
              { path: '/my-children/:childId', element: <Lazy component={ChildDashboardPage} /> },
            ],
          },

          // Moderation routes (moderator, admin, super_admin)
          {
            element: <RoleRoute allowedRoles={[Role.MODERATOR, Role.ADMIN, Role.SUPER_ADMIN]} />,
            children: [
              { path: '/admin/moderation', element: <Lazy component={ModerationPage} /> },
            ],
          },

          // Admin routes (admin, super_admin only)
          {
            element: <RoleRoute allowedRoles={[Role.ADMIN, Role.SUPER_ADMIN]} />,
            children: [
              { path: '/admin', element: <Lazy component={AdminDashboardPage} /> },
              { path: '/admin/users', element: <Lazy component={AdminUsersPage} /> },
              { path: '/admin/users/:id', element: <Lazy component={AdminUserDetailPage} /> },
              { path: '/admin/audit-logs', element: <Lazy component={AuditLogPage} /> },
            ],
          },
        ],
      },
    ],
  },

  // Public routes
  { path: '/test/:slug', element: <Lazy component={PublicTestPage} /> },
  { path: '/unauthorized', element: <Lazy component={UnauthorizedPage} /> },
  { path: '*', element: <Lazy component={NotFoundPage} /> },
]);
