import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
} from '@mui/material';
import { useTranslation } from 'react-i18next';

interface RevokePairingDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  childName?: string;
  isPending: boolean;
}

export default function RevokePairingDialog({ open, onClose, onConfirm, childName, isPending }: RevokePairingDialogProps) {
  const { t } = useTranslation('parent');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('revokeConfirm.title')}</DialogTitle>
      <DialogContent>
        <Typography variant="body2">
          {t('revokeConfirm.message', { name: childName })}
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isPending}>{t('cancel')}</Button>
        <Button onClick={onConfirm} color="error" variant="contained" disabled={isPending}>
          {t('revokeConfirm.confirm')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
