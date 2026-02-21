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
import type { SubjectDto } from '@/types/subject';
import { resolveTranslation } from '@/utils/i18nUtils';

interface SubjectDeleteDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  subject: SubjectDto | null;
  isPending: boolean;
}

export default function SubjectDeleteDialog({ open, onClose, onConfirm, subject, isPending }: SubjectDeleteDialogProps) {
  const { t } = useTranslation('subject');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('deleteTitle')}</DialogTitle>
      <DialogContent>
        <Typography>
          {t('deleteConfirm', { name: resolveTranslation(subject?.nameTranslations) || subject?.name || '' })}
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
          {isPending ? <CircularProgress size={20} /> : t('delete')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
