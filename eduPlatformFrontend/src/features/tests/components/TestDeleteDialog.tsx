import { useTranslation } from 'react-i18next';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import type { TestHistoryDto } from '@/types/test';

interface TestDeleteDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  test: TestHistoryDto | null;
  isPending: boolean;
}

export default function TestDeleteDialog({ open, onClose, onConfirm, test, isPending }: TestDeleteDialogProps) {
  const { t } = useTranslation('test');

  return (
    <ConfirmDialog
      open={open}
      onClose={onClose}
      onConfirm={onConfirm}
      isPending={isPending}
      title={t('delete.title')}
      message={test ? `${t('delete.confirm')} "${test.title}"` : t('delete.confirm')}
    />
  );
}
