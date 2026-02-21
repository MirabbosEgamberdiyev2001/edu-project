import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
} from '@mui/material';
import type { AdminUserDto } from '@/types/admin';

interface DeleteUserDialogProps {
  open: boolean;
  user: AdminUserDto | null;
  loading?: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export default function DeleteUserDialog({ open, user, loading, onClose, onConfirm }: DeleteUserDialogProps) {
  const { t } = useTranslation('admin');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('deleteUser.title')}</DialogTitle>
      <DialogContent>
        {t('deleteUser.confirm', { name: `${user?.firstName} ${user?.lastName}` })}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common:cancel', 'Cancel')}</Button>
        <Button
          variant="contained"
          color="error"
          onClick={onConfirm}
          disabled={loading}
        >
          {loading ? t('deleteUser.deleting') : t('deleteUser.title')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
