export const ROUTES = {
  LOGIN: '/auth/login',
  REGISTER: '/auth/register',
  OTP_VERIFY: '/auth/otp-verify',
  FORGOT_PASSWORD: '/auth/forgot-password',
  RESET_PASSWORD: '/auth/reset-password',
  DASHBOARD: '/dashboard',
  PROFILE: '/profile',
  CHANGE_PASSWORD: '/settings/change-password',
  QUESTIONS: '/questions',
  QUESTION_DETAIL: '/questions/:id',
  TESTS: '/tests',
  TEST_GENERATE: '/tests/generate',
  TEST_DETAIL: '/tests/:id',

  // Groups
  GROUPS: '/groups',
  GROUP_DETAIL: '/groups/:id',

  // Assignments (Teacher)
  ASSIGNMENTS: '/assignments',
  ASSIGNMENT_DETAIL: '/assignments/:id',
  ASSIGNMENT_LIVE: '/assignments/:id/live',
  ASSIGNMENT_RESULTS: '/assignments/:id/results',

  // Test Taking (Student)
  AVAILABLE_TESTS: '/my-tests',
  EXAM: '/exam/:attemptId',
  MY_ATTEMPTS: '/my-attempts',
  ATTEMPT_RESULT: '/attempt-result/:attemptId',

  // Analytics
  TEACHER_ANALYTICS: '/analytics/teacher',
  STUDENT_ANALYTICS: '/analytics/student',
  GROUP_ANALYTICS: '/analytics/group/:groupId',

  // Parent
  PARENT_DASHBOARD: '/my-children',
  CHILD_DASHBOARD: '/my-children/:childId',
  STUDENT_PAIRING: '/pairing',

  // Admin
  ADMIN_DASHBOARD: '/admin',
  ADMIN_USERS: '/admin/users',
  ADMIN_USER_DETAIL: '/admin/users/:id',
  ADMIN_AUDIT_LOGS: '/admin/audit-logs',
  ADMIN_MODERATION: '/admin/moderation',

  // Public test
  PUBLIC_TEST: '/test/:slug',

  UNAUTHORIZED: '/unauthorized',
} as const;
