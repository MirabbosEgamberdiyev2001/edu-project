import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Paper,
  Grid,
  TextField,
  Button,
  Avatar,
  Chip,
  Divider,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import LockIcon from '@mui/icons-material/Lock';
import { useAuth } from '@/hooks/useAuth';
import { userApi } from '@/api/userApi';
import { useAuthStore } from '@/stores/authStore';
import type { UpdateProfileRequest } from '@/types/auth';

export default function ProfilePage() {
  const { t } = useTranslation('profile');
  const navigate = useNavigate();
  const { user } = useAuth();
  const setAuth = useAuthStore((s) => s.setAuth);
  const queryClient = useQueryClient();

  const [editing, setEditing] = useState(false);
  const [firstName, setFirstName] = useState(user?.firstName || '');
  const [lastName, setLastName] = useState(user?.lastName || '');

  const updateProfile = useMutation({
    mutationFn: (data: UpdateProfileRequest) => userApi.updateProfile(data),
    onSuccess: (res) => {
      const updatedUser = res.data.data;
      setAuth(updatedUser);
      queryClient.invalidateQueries({ queryKey: ['currentUser'] });
      setEditing(false);
    },
  });

  const handleSave = () => {
    updateProfile.mutate({ firstName, lastName });
  };

  const handleCancel = () => {
    setFirstName(user?.firstName || '');
    setLastName(user?.lastName || '');
    setEditing(false);
  };

  if (!user) return null;

  const VerifyBadge = ({ verified }: { verified: boolean }) =>
    verified
      ? <Chip icon={<CheckCircleIcon />} label={t('verified')} size="small" color="success" variant="outlined" />
      : <Chip icon={<CancelIcon />} label={t('notVerified')} size="small" color="default" variant="outlined" />;

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" fontWeight={700}>{t('title')}</Typography>
        <Typography variant="body2" color="text.secondary">{t('subtitle')}</Typography>
      </Box>

      {/* Profile Card */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
          <Avatar
            sx={{ width: 72, height: 72, bgcolor: 'primary.main', fontSize: '1.75rem' }}
            src={user.avatarUrl || undefined}
          >
            {user.firstName[0]?.toUpperCase()}{user.lastName[0]?.toUpperCase()}
          </Avatar>
          <Box sx={{ flex: 1 }}>
            <Typography variant="h6" fontWeight={700}>
              {user.firstName} {user.lastName}
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, mt: 0.5 }}>
              <Chip label={user.role} size="small" color="primary" />
              <Chip label={user.status} size="small" color="success" variant="outlined" />
            </Box>
          </Box>
          {!editing && (
            <Button variant="outlined" onClick={() => setEditing(true)}>
              {t('editProfile')}
            </Button>
          )}
        </Box>

        <Divider sx={{ mb: 3 }} />

        <Typography variant="subtitle2" gutterBottom>{t('personalInfo')}</Typography>

        {editing ? (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, maxWidth: 400 }}>
            <TextField
              size="small"
              label={t('firstName')}
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
            />
            <TextField
              size="small"
              label={t('lastName')}
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
            />
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button
                variant="contained"
                onClick={handleSave}
                disabled={updateProfile.isPending || (!firstName.trim() && !lastName.trim())}
              >
                {updateProfile.isPending ? t('saving') : t('saveChanges')}
              </Button>
              <Button variant="outlined" onClick={handleCancel}>
                {t('common:cancel')}
              </Button>
            </Box>
          </Box>
        ) : (
          <Grid container spacing={2}>
            <InfoItem label={t('firstName')} value={user.firstName} />
            <InfoItem label={t('lastName')} value={user.lastName} />
            <InfoItem label={t('email')} value={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                {user.email || '—'}
                {user.email && <VerifyBadge verified={user.emailVerified} />}
              </Box>
            } />
            <InfoItem label={t('phone')} value={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                {user.phone || '—'}
                {user.phone && <VerifyBadge verified={user.phoneVerified} />}
              </Box>
            } />
            <InfoItem label={t('role')} value={user.role} />
            <InfoItem label={t('status')} value={user.status} />
            <InfoItem label={t('lastLogin')} value={user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : '—'} />
            <InfoItem label={t('memberSince')} value={new Date(user.createdAt).toLocaleDateString()} />
          </Grid>
        )}
      </Paper>

      {/* Actions */}
      <Paper sx={{ p: 3 }}>
        <Typography variant="subtitle2" gutterBottom>{t('accountInfo')}</Typography>
        <Button
          variant="outlined"
          startIcon={<LockIcon />}
          onClick={() => navigate('/settings/change-password')}
        >
          {t('changePassword')}
        </Button>
      </Paper>
    </Box>
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
