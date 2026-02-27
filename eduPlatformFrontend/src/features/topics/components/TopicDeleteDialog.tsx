import { useTranslation } from 'react-i18next';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
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
    <ConfirmDialog
      open={open}
      onClose={onClose}
      onConfirm={onConfirm}
      isPending={isPending}
      title={t('deleteTitle')}
      message={t('deleteConfirm', { name: displayName })}
    />
  );
}
