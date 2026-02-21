import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
} from '@mui/material';
import type { AdminUserDto } from '@/types/admin';

interface UnlockUserDialogProps {
  open: boolean;
  user: AdminUserDto | null;
  loading?: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export default function UnlockUserDialog({ open, user, loading, onClose, onConfirm }: UnlockUserDialogProps) {
  const { t } = useTranslation('admin');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('unlockUser.title')}</DialogTitle>
      <DialogContent>
        {t('unlockUser.confirm', { name: `${user?.firstName} ${user?.lastName}` })}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common:cancel', 'Cancel')}</Button>
        <Button
          variant="contained"
          color="warning"
          onClick={onConfirm}
          disabled={loading}
        >
          {t('users.unlock')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
