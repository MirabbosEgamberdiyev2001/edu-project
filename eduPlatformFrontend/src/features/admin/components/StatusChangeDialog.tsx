import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
} from '@mui/material';
import { UserStatus } from '@/types/user';
import type { AdminUserDto } from '@/types/admin';

interface StatusChangeDialogProps {
  open: boolean;
  user: AdminUserDto | null;
  loading?: boolean;
  onClose: () => void;
  onConfirm: (status: UserStatus, reason?: string) => void;
}

const STATUSES = [
  UserStatus.ACTIVE,
  UserStatus.INACTIVE,
  UserStatus.BLOCKED,
  UserStatus.PENDING_VERIFICATION,
];

export default function StatusChangeDialog({ open, user, loading, onClose, onConfirm }: StatusChangeDialogProps) {
  const { t } = useTranslation('admin');
  const [selectedStatus, setSelectedStatus] = useState<UserStatus | ''>('');
  const [reason, setReason] = useState('');

  const handleEnter = () => {
    setSelectedStatus(user?.status ?? '');
    setReason('');
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth TransitionProps={{ onEnter: handleEnter }}>
      <DialogTitle>{t('changeStatus.title')}</DialogTitle>
      <DialogContent>
        <p style={{ margin: '0 0 16px' }}>
          {t('changeStatus.description', { name: `${user?.firstName} ${user?.lastName}` })}
        </p>
        <FormControl fullWidth size="small" sx={{ mb: 2 }}>
          <InputLabel>{t('changeStatus.selectStatus')}</InputLabel>
          <Select
            value={selectedStatus}
            label={t('changeStatus.selectStatus')}
            onChange={(e) => setSelectedStatus(e.target.value as UserStatus)}
          >
            {STATUSES.map((status) => (
              <MenuItem key={status} value={status}>
                {t(`usersByStatus.${status === UserStatus.PENDING_VERIFICATION ? 'pendingVerification' : status.toLowerCase()}`)}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <TextField
          fullWidth
          size="small"
          label={t('changeStatus.reason')}
          placeholder={t('changeStatus.reasonPlaceholder')}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          multiline
          rows={2}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common:cancel', 'Cancel')}</Button>
        <Button
          variant="contained"
          onClick={() => selectedStatus && onConfirm(selectedStatus as UserStatus, reason || undefined)}
          disabled={!selectedStatus || selectedStatus === user?.status || loading}
        >
          {t('changeStatus.confirm')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
