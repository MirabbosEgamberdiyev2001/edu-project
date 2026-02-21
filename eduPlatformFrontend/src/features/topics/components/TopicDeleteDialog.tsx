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
import type { TopicTreeDto } from '@/types/topic';
import { resolveTranslation } from '@/utils/i18nUtils';

interface TopicDeleteDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  topic: TopicTreeDto | null;
  isPending: boolean;
}

export default function TopicDeleteDialog({ open, onClose, onConfirm, topic, isPending }: TopicDeleteDialogProps) {
  const { t } = useTranslation('topic');

  const displayName = resolveTranslation(topic?.nameTranslations) || topic?.name || '';

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('deleteTitle')}</DialogTitle>
      <DialogContent>
        <Typography>
          {t('deleteConfirm', { name: displayName })}
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
          {isPending ? <CircularProgress size={20} /> : t('common:delete')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
