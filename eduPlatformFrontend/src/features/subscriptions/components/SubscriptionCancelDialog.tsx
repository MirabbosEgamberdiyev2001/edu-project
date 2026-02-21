import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
} from '@mui/material';
import WarningIcon from '@mui/icons-material/Warning';
import { useTranslation } from 'react-i18next';

interface SubscriptionCancelDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  isPending: boolean;
}

export default function SubscriptionCancelDialog({ open, onClose, onConfirm, isPending }: SubscriptionCancelDialogProps) {
  const { t } = useTranslation('subscription');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <WarningIcon color="warning" />
        {t('cancelConfirm.title')}
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2">{t('cancelConfirm.message')}</Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isPending}>{t('cancelConfirm.keep')}</Button>
        <Button onClick={onConfirm} color="error" variant="contained" disabled={isPending}>
          {t('cancelConfirm.confirm')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
