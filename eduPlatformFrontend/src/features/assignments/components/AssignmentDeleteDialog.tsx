import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { AssignmentDto } from '@/types/assignment';

interface AssignmentDeleteDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  assignment: AssignmentDto | null;
  isPending: boolean;
}

export default function AssignmentDeleteDialog({
  open,
  onClose,
  onConfirm,
  assignment,
  isPending,
}: AssignmentDeleteDialogProps) {
  const { t } = useTranslation('assignment');
  const { t: tc } = useTranslation('common');

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('deleteAssignment')}</DialogTitle>
      <DialogContent>
        <DialogContentText>
          {t('deleteConfirm', { name: assignment?.title })}
        </DialogContentText>
        <DialogContentText sx={{ mt: 1, color: 'error.main', fontSize: '0.875rem' }}>
          {t('deleteWarning')}
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isPending}>{tc('cancel')}</Button>
        <Button onClick={onConfirm} color="error" variant="contained" disabled={isPending}>
          {tc('delete')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
