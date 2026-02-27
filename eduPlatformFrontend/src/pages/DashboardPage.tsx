import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
import { useAuth } from '@/hooks/useAuth';
import { Role } from '@/types/user';

/**
 * Role-based dashboard router.
 * Redirects each role to their own dedicated dashboard page.
 */
export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!user) return;

    switch (user.role) {
      case Role.TEACHER:
        navigate('/teacher-dashboard', { replace: true });
        break;
      case Role.STUDENT:
        navigate('/student', { replace: true });
        break;
      case Role.PARENT:
        navigate('/my-children', { replace: true });
        break;
      case Role.ADMIN:
      case Role.SUPER_ADMIN:
        navigate('/admin', { replace: true });
        break;
      case Role.MODERATOR:
        navigate('/admin/moderation', { replace: true });
        break;
      default:
        navigate('/profile', { replace: true });
    }
  }, [user, navigate]);

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
      <CircularProgress />
    </Box>
  );
}
