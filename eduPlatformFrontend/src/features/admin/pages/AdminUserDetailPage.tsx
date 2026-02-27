import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Chip,
  Button,
  CircularProgress,
  Divider,
  Avatar,
} from '@mui/material';
import SecurityIcon from '@mui/icons-material/Security';
import EditIcon from '@mui/icons-material/Edit';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import DeleteIcon from '@mui/icons-material/Delete';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { Role, UserStatus } from '@/types/user';
import type { AdminUserDto } from '@/types/admin';
import { useAuth } from '@/hooks/useAuth';
import {
  useAdminUser,
  useChangeRole,
  useChangeStatus,
  useUnlockUser,
  useDeleteUser,
} from '../hooks/useAdminUsers';
import { PageShell } from '@/components/ui';
import RoleChangeDialog from '../components/RoleChangeDialog';
import StatusChangeDialog from '../components/StatusChangeDialog';
import DeleteUserDialog from '../components/DeleteUserDialog';
import UnlockUserDialog from '../components/UnlockUserDialog';

const STATUS_COLORS: Record<string, 'success' | 'default' | 'error' | 'warning'> = {
  ACTIVE: 'success',
  INACTIVE: 'default',
  BLOCKED: 'error',
  PENDING_VERIFICATION: 'warning',
};

const ROLE_COLORS: Record<string, string> = {
  SUPER_ADMIN: '#9c27b0',
  ADMIN: '#1976d2',
  MODERATOR: '#ed6c02',
  TEACHER: '#2e7d32',
  PARENT: '#0288d1',
  STUDENT: '#607d8b',
};

export default function AdminUserDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('admin');
  const { user: currentUser } = useAuth();

  const { data: user, isLoading } = useAdminUser(id!);
  const changeRole = useChangeRole();
  const changeStatus = useChangeStatus();
  const unlockUser = useUnlockUser();
  const deleteUser = useDeleteUser();

  const [roleDialogOpen, setRoleDialogOpen] = useState(false);
  const [statusDialogOpen, setStatusDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [unlockDialogOpen, setUnlockDialogOpen] = useState(false);

  const isSuperAdmin = currentUser?.role === Role.SUPER_ADMIN;
  const isProtected = user?.role === Role.SUPER_ADMIN;

  const handleRoleChange = (role: Role) => {
    if (!user) return;
    changeRole.mutate(
      { id: user.id, data: { role } },
      { onSuccess: () => setRoleDialogOpen(false) },
    );
  };

  const handleStatusChange = (status: UserStatus, reason?: string) => {
    if (!user) return;
    changeStatus.mutate(
      { id: user.id, data: { status, reason } },
      { onSuccess: () => setStatusDialogOpen(false) },
    );
  };

  const handleUnlock = () => {
    if (!user) return;
    unlockUser.mutate(user.id, {
      onSuccess: () => setUnlockDialogOpen(false),
    });
  };

  const handleDelete = () => {
    if (!user) return;
    deleteUser.mutate(user.id, {
      onSuccess: () => {
        setDeleteDialogOpen(false);
        navigate('/admin/users');
      },
    });
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!user) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography color="text.secondary">{t('users.noUsers')}</Typography>
      </Box>
    );
  }

  const VerifyIcon = ({ verified }: { verified: boolean }) =>
    verified
      ? <CheckCircleIcon fontSize="small" sx={{ color: 'success.main', ml: 0.5 }} />
      : <CancelIcon fontSize="small" sx={{ color: 'text.disabled', ml: 0.5 }} />;

  return (
    <PageShell
      title={`${user.firstName} ${user.lastName}`}
      breadcrumbs={[
        { label: t('users.title'), to: '/admin/users' },
        { label: `${user.firstName} ${user.lastName}` },
      ]}
      actions={!isProtected ? (
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Button
            size="small"
            variant="outlined"
            startIcon={<SecurityIcon />}
            onClick={() => setRoleDialogOpen(true)}
          >
            {t('users.changeRole')}
          </Button>
          <Button
            size="small"
            variant="outlined"
            startIcon={<EditIcon />}
            onClick={() => setStatusDialogOpen(true)}
          >
            {t('users.changeStatus')}
          </Button>
          {user.failedLoginAttempts > 0 && (
            <Button
              size="small"
              variant="outlined"
              color="warning"
              startIcon={<LockOpenIcon />}
              onClick={() => setUnlockDialogOpen(true)}
            >
              {t('users.unlock')}
            </Button>
          )}
          {isSuperAdmin && (
            <Button
              size="small"
              variant="outlined"
              color="error"
              startIcon={<DeleteIcon />}
              onClick={() => setDeleteDialogOpen(true)}
            >
              {t('users.deleteUser')}
            </Button>
          )}
        </Box>
      ) : undefined}
    >
      {/* User Info Card */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          <Avatar
            sx={{ width: 64, height: 64, bgcolor: ROLE_COLORS[user.role] || '#607d8b', fontSize: '1.5rem' }}
            src={user.avatarUrl || undefined}
          >
            {user.firstName[0]?.toUpperCase()}{user.lastName[0]?.toUpperCase()}
          </Avatar>
          <Box>
            <Box sx={{ display: 'flex', gap: 1, mt: 0.5 }}>
              <Chip
                label={user.role}
                size="small"
                sx={{ bgcolor: ROLE_COLORS[user.role] || '#607d8b', color: '#fff', fontWeight: 600 }}
              />
              <Chip
                label={t(`usersByStatus.${user.status === UserStatus.PENDING_VERIFICATION ? 'pendingVerification' : user.status.toLowerCase()}`)}
                size="small"
                color={STATUS_COLORS[user.status] || 'default'}
                variant="outlined"
              />
            </Box>
          </Box>
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Detail Grid */}
        <Grid container spacing={2}>
          <InfoItem label={t('contentStats.email')} value={
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              {user.email || '—'}
              {user.email && <VerifyIcon verified={user.emailVerified} />}
            </Box>
          } />
          <InfoItem label={t('users.emailPhone')} value={
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              {user.phone || '—'}
              {user.phone && <VerifyIcon verified={user.phoneVerified} />}
            </Box>
          } />
          <InfoItem label={t('users.role')} value={user.role} />
          <InfoItem label={t('users.status')} value={
            t(`usersByStatus.${user.status === UserStatus.PENDING_VERIFICATION ? 'pendingVerification' : user.status.toLowerCase()}`)
          } />
          {user.statusReason && (
            <InfoItem label={t('users.statusReason')} value={user.statusReason} />
          )}
          <InfoItem label={t('users.failedAttempts')} value={String(user.failedLoginAttempts)} />
          {user.lockedUntil && (
            <InfoItem label={t('users.lockedUntil')} value={new Date(user.lockedUntil).toLocaleString()} />
          )}
          <InfoItem label={t('users.lastLogin')} value={user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : '—'} />
          {user.lastLoginIp && (
            <InfoItem label={t('users.lastLoginIp')} value={user.lastLoginIp} />
          )}
          <InfoItem label={t('users.createdAt')} value={new Date(user.createdAt).toLocaleString()} />
          <InfoItem label={t('users.updatedAt')} value={new Date(user.updatedAt).toLocaleString()} />
        </Grid>
      </Paper>

      {/* Dialogs */}
      <RoleChangeDialog
        open={roleDialogOpen}
        user={user}
        loading={changeRole.isPending}
        onClose={() => setRoleDialogOpen(false)}
        onConfirm={handleRoleChange}
      />
      <StatusChangeDialog
        open={statusDialogOpen}
        user={user}
        loading={changeStatus.isPending}
        onClose={() => setStatusDialogOpen(false)}
        onConfirm={handleStatusChange}
      />
      <UnlockUserDialog
        open={unlockDialogOpen}
        user={user}
        loading={unlockUser.isPending}
        onClose={() => setUnlockDialogOpen(false)}
        onConfirm={handleUnlock}
      />
      <DeleteUserDialog
        open={deleteDialogOpen}
        user={user}
        loading={deleteUser.isPending}
        onClose={() => setDeleteDialogOpen(false)}
        onConfirm={handleDelete}
      />
    </PageShell>
  );
}

function InfoItem({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <Grid item xs={12} sm={6} md={4}>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
      <Typography variant="body2">{value}</Typography>
    </Grid>
  );
}
