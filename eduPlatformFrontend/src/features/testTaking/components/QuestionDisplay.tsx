import { Typography, Box, Chip } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { AttemptQuestionDto } from '@/types/testTaking';

interface QuestionDisplayProps {
  question: AttemptQuestionDto;
  questionNumber: number;
  totalQuestions: number;
}

export default function QuestionDisplay({ question, questionNumber, totalQuestions }: QuestionDisplayProps) {
  const { t } = useTranslation('testTaking');

  return (
    <Box sx={{ mb: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <Typography variant="subtitle2" color="text.secondary">
          {questionNumber}/{totalQuestions}
        </Typography>
        <Chip label={question.difficulty} size="small" variant="outlined" />
        <Chip label={`${question.points} ${t('pointsShort')}`} size="small" />
      </Box>
      <Typography variant="h6" sx={{ mb: 2, lineHeight: 1.5 }}>
        {question.questionText}
      </Typography>
      {question.media && (
        <Box sx={{ mb: 2 }}>
          {typeof question.media === 'object' && 'url' in question.media && (
            <img
              src={question.media.url as string}
              alt="Question media"
              style={{ maxWidth: '100%', maxHeight: 300, borderRadius: 8 }}
            />
          )}
        </Box>
      )}
    </Box>
  );
}
