import { useTranslation } from 'react-i18next';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import type { GroupDto } from '@/types/group';

interface GroupDeleteDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  group: GroupDto | null;
  isPending: boolean;
}

export default function GroupDeleteDialog({ open, onClose, onConfirm, group, isPending }: GroupDeleteDialogProps) {
  const { t } = useTranslation('group');

  return (
    <ConfirmDialog
      open={open}
      onClose={onClose}
      onConfirm={onConfirm}
      isPending={isPending}
      title={t('deleteGroup')}
      message={t('deleteConfirm', { name: group?.name || '' })}
      warning={t('deleteWarning')}
    />
  );
}
