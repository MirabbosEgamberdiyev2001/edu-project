import type { ReactNode } from 'react';
import { useAuth } from '@/hooks/useAuth';
import type { Role } from '@/types/user';

interface PermissionGateProps {
  /** Roles that are allowed to see the children. */
  allowedRoles: Role[];
  /** Rendered when the user does not have the required role. Defaults to null. */
  fallback?: ReactNode;
  children: ReactNode;
}

/**
 * Conditionally renders children based on the current user's role.
 *
 * Usage:
 * ```tsx
 * <PermissionGate allowedRoles={[Role.ADMIN, Role.SUPER_ADMIN]}>
 *   <AdminOnlyButton />
 * </PermissionGate>
 * ```
 */
export function PermissionGate({ allowedRoles, fallback = null, children }: PermissionGateProps) {
  const { user } = useAuth();

  if (!user || !allowedRoles.includes(user.role)) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
}

export default PermissionGate;
