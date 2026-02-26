import { useParams, useNavigate } from 'react-router-dom';
import { Box, Button, CircularProgress, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useTranslation } from 'react-i18next';
import { useAttempt } from '../hooks/useAttempt';
import ResultDisplay from '../components/ResultDisplay';
import type { AttemptResultDto } from '@/types/testTaking';

export default function AttemptResultPage() {
  const { attemptId } = useParams<{ attemptId: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('testTaking');
  const { data: attempt, isLoading } = useAttempt(attemptId!);

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!attempt) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography color="text.secondary">{t('notFound')}</Typography>
      </Box>
    );
  }

  const answersMap = attempt.answers ?? {};
  const answerValues = Object.values(answersMap);

  // Map AttemptDto to AttemptResultDto shape for ResultDisplay
  const result: AttemptResultDto = {
    id: attempt.id,
    testTitle: attempt.testTitle ?? attempt.assignmentTitle ?? '',
    score: attempt.score ?? 0,
    maxScore: attempt.maxScore ?? 0,
    percentage: attempt.percentage ?? 0,
    totalQuestions: attempt.totalQuestions,
    correctAnswers: answerValues.filter((a) => a.isCorrect === true).length,
    wrongAnswers: answerValues.filter((a) => a.isCorrect === false).length,
    unanswered: attempt.totalQuestions - attempt.answeredQuestions,
    tabSwitches: attempt.tabSwitches ?? 0,
    startedAt: attempt.startedAt,
    submittedAt: attempt.submittedAt || attempt.startedAt,
    durationSeconds: attempt.submittedAt
      ? Math.floor((new Date(attempt.submittedAt).getTime() - new Date(attempt.startedAt).getTime()) / 1000)
      : 0,
    answers: (attempt.questions ?? []).map((q) => {
      const ans = answersMap[q.id];
      return {
        questionId: q.id,
        questionText: q.questionText,
        questionType: q.questionType,
        options: q.options,
        correctAnswer: null,
        studentAnswer: ans?.response ?? ans?.selectedAnswer ?? null,
        isCorrect: ans?.isCorrect ?? false,
        score: ans?.score ?? ans?.earnedPoints ?? 0,
        maxScore: q.points,
        proof: null,
      };
    }),
  };

  return (
    <Box sx={{ maxWidth: 800, mx: 'auto', py: 3, px: 2 }}>
      <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/my-attempts')} sx={{ mb: 2 }}>
        {t('backToAttempts')}
      </Button>
      <ResultDisplay result={result} />
    </Box>
  );
}
