import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  CircularProgress,
} from '@mui/material';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { useTranslation } from 'react-i18next';

interface RemoveMemberConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  memberCount: number;
  memberName?: string;
  isPending: boolean;
}

export default function RemoveMemberConfirmDialog({
  open,
  onClose,
  onConfirm,
  memberCount,
  memberName,
  isPending,
}: RemoveMemberConfirmDialogProps) {
  const { t } = useTranslation('group');
  const { t: tc } = useTranslation('common');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <WarningAmberIcon color="warning" />
        {tc('confirm')}
      </DialogTitle>
      <DialogContent>
        <Typography variant="body1" sx={{ mb: 1 }}>
          {memberCount === 1 && memberName
            ? t('removeMemberConfirm', { name: memberName })
            : t('removeMembersBatchConfirm', { count: memberCount })}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {t('removeWarning')}
        </Typography>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} disabled={isPending}>{tc('cancel')}</Button>
        <Button
          variant="contained"
          color="error"
          onClick={onConfirm}
          disabled={isPending}
        >
          {isPending ? <CircularProgress size={20} /> : tc('delete')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
