import { Typography, Box, Chip } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { AttemptQuestionDto } from '@/types/testTaking';
import { MathText } from '@/components/math';

interface QuestionDisplayProps {
  question: AttemptQuestionDto;
  questionNumber: number;
  totalQuestions: number;
}

// totalQuestions is intentionally unused here — progress is shown in ExamHeader and nav
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export default function QuestionDisplay({ question, questionNumber, totalQuestions: _total }: QuestionDisplayProps) {
  const { t } = useTranslation('testTaking');

  return (
    <Box sx={{ mb: 2 }}>
      {/* Meta row: question label + points chip. Difficulty omitted — irrelevant during exam. */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
        <Typography
          variant="caption"
          fontWeight={600}
          color="text.secondary"
          sx={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontSize: '0.6875rem' }}
        >
          {t('question')} {questionNumber}
        </Typography>
        <Chip
          label={`${question.points} ${t('pointsShort')}`}
          size="small"
          sx={{ height: 20, fontSize: '0.6875rem' }}
        />
      </Box>

      {/* Question text with KaTeX formula support */}
      <MathText
        text={question.questionText}
        variant="h6"
        sx={{ lineHeight: 1.65, fontSize: { xs: '1rem', sm: '1.125rem' } }}
      />

      {/* Optional attached media */}
      {question.media && typeof question.media === 'object' && 'url' in question.media && (
        <Box sx={{ mt: 2 }}>
          <img
            src={question.media.url as string}
            alt="Question media"
            style={{ maxWidth: '100%', maxHeight: 300, borderRadius: 8 }}
          />
        </Box>
      )}
    </Box>
  );
}
