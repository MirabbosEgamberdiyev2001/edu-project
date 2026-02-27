import { useTranslation } from 'react-i18next';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
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

  return (
    <ConfirmDialog
      open={open}
      onClose={onClose}
      onConfirm={onConfirm}
      isPending={isPending}
      title={t('deleteAssignment')}
      message={t('deleteConfirm', { name: assignment?.title })}
      warning={t('deleteWarning')}
    />
  );
}
