import { useTranslation } from 'react-i18next';
import {
  Dialog, DialogTitle, DialogContent, DialogActions, Typography, Button, CircularProgress,
} from '@mui/material';
import WarningIcon from '@mui/icons-material/Warning';
import type { TestHistoryDto } from '@/types/test';

interface TestDeleteDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  test: TestHistoryDto | null;
  isPending: boolean;
}

export default function TestDeleteDialog({ open, onClose, onConfirm, test, isPending }: TestDeleteDialogProps) {
  const { t } = useTranslation('test');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <WarningIcon color="error" />
        {t('delete.title')}
      </DialogTitle>
      <DialogContent>
        <Typography>
          {t('delete.confirm')}
        </Typography>
        {test && (
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            {test.title}
          </Typography>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isPending}>{t('common:cancel')}</Button>
        <Button
          variant="contained"
          color="error"
          onClick={onConfirm}
          disabled={isPending}
        >
          {isPending ? <CircularProgress size={20} /> : t('common:delete')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
