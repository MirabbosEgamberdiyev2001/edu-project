import { useTranslation } from 'react-i18next';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
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
    <ConfirmDialog
      open={open}
      onClose={onClose}
      onConfirm={onConfirm}
      isPending={isPending}
      title={t('deleteTitle')}
      message={t('deleteConfirm', { name: resolveTranslation(subject?.nameTranslations) || subject?.name || '' })}
    />
  );
}
