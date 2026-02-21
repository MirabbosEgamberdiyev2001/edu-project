import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
} from '@mui/material';
import WarningIcon from '@mui/icons-material/Warning';
import { useTranslation } from 'react-i18next';

interface SubmitConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  answeredCount: number;
  totalQuestions: number;
  isPending: boolean;
}

export default function SubmitConfirmDialog({
  open,
  onClose,
  onConfirm,
  answeredCount,
  totalQuestions,
  isPending,
}: SubmitConfirmDialogProps) {
  const { t } = useTranslation('testTaking');
  const unanswered = totalQuestions - answeredCount;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('submitConfirm.title')}</DialogTitle>
      <DialogContent>
        <Box sx={{ textAlign: 'center', py: 2 }}>
          <Typography variant="h3" fontWeight={700} color="primary.main">
            {answeredCount}/{totalQuestions}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            {t('submitConfirm.answered')}
          </Typography>
          {unanswered > 0 && (
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mt: 2 }}>
              <WarningIcon color="warning" />
              <Typography variant="body2" color="warning.main">
                {t('submitConfirm.unanswered', { count: unanswered })}
              </Typography>
            </Box>
          )}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isPending}>{t('submitConfirm.cancel')}</Button>
        <Button onClick={onConfirm} variant="contained" disabled={isPending} color="primary">
          {t('submitConfirm.submit')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
