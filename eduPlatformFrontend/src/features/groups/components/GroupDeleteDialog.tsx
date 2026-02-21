import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  CircularProgress,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { GroupDto } from '@/types/group';

interface GroupDeleteDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  group: GroupDto | null;
  isPending: boolean;
}

export default function GroupDeleteDialog({ open, onClose, onConfirm, group, isPending }: GroupDeleteDialogProps) {
  const { t } = useTranslation('group');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('deleteGroup')}</DialogTitle>
      <DialogContent>
        <Typography>
          {t('deleteConfirm', { name: group?.name || '' })}
        </Typography>
        <Typography variant="body2" color="error" sx={{ mt: 1 }}>
          {t('deleteWarning')}
        </Typography>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>{t('common:cancel')}</Button>
        <Button
          variant="contained"
          color="error"
          onClick={onConfirm}
          disabled={isPending}
        >
          {isPending ? <CircularProgress size={20} /> : t('deleteGroup')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
