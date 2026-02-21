import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  CircularProgress,
  Alert,
} from '@mui/material';

interface SubmitToModerationDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  isPending: boolean;
  totalQuestions: number;
  submittableCount: number;
  isLoadingStatuses?: boolean;
}

export default function SubmitToModerationDialog({
  open,
  onClose,
  onConfirm,
  isPending,
  totalQuestions,
  submittableCount,
  isLoadingStatuses,
}: SubmitToModerationDialogProps) {
  const { t } = useTranslation('test');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{t('submitDialog.title')}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" sx={{ mb: 2 }}>
          {t('submitDialog.description')}
        </Typography>
        <Alert severity="info" sx={{ mb: 1 }}>
          {t('submitDialog.total', { count: totalQuestions })}
        </Alert>
        {isLoadingStatuses ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
            <CircularProgress size={24} />
          </Box>
        ) : submittableCount > 0 ? (
          <Alert severity="success">
            {t('submitDialog.submittable', { count: submittableCount })}
          </Alert>
        ) : (
          <Alert severity="warning">
            {t('submitDialog.noneSubmittable')}
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isPending}>
          {t('common:cancel', 'Cancel')}
        </Button>
        <Button
          variant="contained"
          onClick={onConfirm}
          disabled={isPending || isLoadingStatuses || submittableCount === 0}
          startIcon={isPending ? <CircularProgress size={20} /> : undefined}
        >
          {isPending ? t('submitDialog.submitting') : t('submitDialog.confirm')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
