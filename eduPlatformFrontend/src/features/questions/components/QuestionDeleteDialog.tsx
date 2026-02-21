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
import type { QuestionDto } from '@/types/question';
import { resolveTranslation } from '@/utils/i18nUtils';

interface QuestionDeleteDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  question: QuestionDto | null;
  isPending: boolean;
}

export default function QuestionDeleteDialog({ open, onClose, onConfirm, question, isPending }: QuestionDeleteDialogProps) {
  const { t } = useTranslation('question');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('deleteTitle')}</DialogTitle>
      <DialogContent>
        <Typography>{t('deleteConfirm')}</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          {resolveTranslation(question?.questionTextTranslations) || question?.questionText || ''}
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
