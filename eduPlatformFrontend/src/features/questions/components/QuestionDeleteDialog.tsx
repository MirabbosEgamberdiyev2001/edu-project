import { useTranslation } from 'react-i18next';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
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
  const questionText = resolveTranslation(question?.questionTextTranslations) || question?.questionText || '';

  return (
    <ConfirmDialog
      open={open}
      onClose={onClose}
      onConfirm={onConfirm}
      isPending={isPending}
      title={t('deleteTitle')}
      message={questionText ? `${t('deleteConfirm')} "${questionText}"` : t('deleteConfirm')}
    />
  );
}
