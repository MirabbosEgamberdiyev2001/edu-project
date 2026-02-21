import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Paper, Button, CircularProgress } from '@mui/material';
import NavigateBeforeIcon from '@mui/icons-material/NavigateBefore';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import SendIcon from '@mui/icons-material/Send';
import { useTranslation } from 'react-i18next';
import { useAttempt } from '../hooks/useAttempt';
import { useAttemptMutations } from '../hooks/useAttemptMutations';
import { useAutoSave } from '../hooks/useAutoSave';
import ExamHeader from '../components/ExamHeader';
import QuestionDisplay from '../components/QuestionDisplay';
import AnswerInput from '../components/AnswerInput';
import QuestionNavigation from '../components/QuestionNavigation';
import SubmitConfirmDialog from '../components/SubmitConfirmDialog';

export default function ExamPage() {
  const { attemptId } = useParams<{ attemptId: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('testTaking');
  const { data: attempt, isLoading } = useAttempt(attemptId!);
  const { submitAnswer, submitAttempt, reportTabSwitch } = useAttemptMutations();
  const { addAnswer, flush } = useAutoSave(attemptId!, !!attempt);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [localAnswers, setLocalAnswers] = useState<Record<string, unknown>>({});
  const [submitOpen, setSubmitOpen] = useState(false);

  // Initialize local answers from attempt data
  useEffect(() => {
    if (attempt?.answers) {
      const existing: Record<string, unknown> = {};
      Object.entries(attempt.answers).forEach(([qId, ans]) => {
        existing[qId] = ans.response;
      });
      setLocalAnswers((prev) => ({ ...existing, ...prev }));
    }
  }, [attempt]);

  // Anti-cheat: visibilitychange listener
  useEffect(() => {
    if (!attempt || !attemptId) return;

    const handleVisibility = () => {
      if (document.hidden) {
        reportTabSwitch.mutate(attemptId);
      }
    };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [attempt, attemptId, reportTabSwitch]);

  // Anti-cheat: prevent copy/paste/context menu
  useEffect(() => {
    const prevent = (e: Event) => e.preventDefault();
    document.addEventListener('copy', prevent);
    document.addEventListener('paste', prevent);
    document.addEventListener('contextmenu', prevent);
    return () => {
      document.removeEventListener('copy', prevent);
      document.removeEventListener('paste', prevent);
      document.removeEventListener('contextmenu', prevent);
    };
  }, []);

  const questions = attempt?.questions || [];
  const currentQuestion = questions[currentIndex];

  const answeredSet = useMemo(() => {
    const set = new Set<string>();
    Object.entries(localAnswers).forEach(([qId, val]) => {
      if (val != null && val !== '' && !(Array.isArray(val) && val.length === 0)) {
        set.add(qId);
      }
    });
    return set;
  }, [localAnswers]);

  const handleAnswerChange = useCallback((value: unknown) => {
    if (!currentQuestion) return;
    setLocalAnswers((prev) => ({ ...prev, [currentQuestion.id]: value }));
    addAnswer({ questionId: currentQuestion.id, response: value });
    submitAnswer.mutate({
      attemptId: attemptId!,
      data: { questionId: currentQuestion.id, response: value },
    });
  }, [currentQuestion, attemptId, addAnswer, submitAnswer]);

  const handleTimeUp = useCallback(async () => {
    await flush();
    submitAttempt.mutate(attemptId!, {
      onSuccess: () => navigate(`/attempt-result/${attemptId}`, { replace: true }),
    });
  }, [flush, submitAttempt, attemptId, navigate]);

  const handleSubmit = async () => {
    await flush();
    submitAttempt.mutate(attemptId!, {
      onSuccess: () => navigate(`/attempt-result/${attemptId}`, { replace: true }),
    });
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!attempt || !currentQuestion) {
    return null;
  }

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'grey.50' }}>
      <ExamHeader
        title={attempt.testTitle}
        answeredCount={answeredSet.size}
        totalQuestions={questions.length}
        timeRemaining={attempt.timeRemaining}
        onTimeUp={handleTimeUp}
      />

      <Box sx={{ pt: 10, px: 2, pb: 4, maxWidth: 900, mx: 'auto' }}>
        <Box sx={{ mb: 3 }}>
          <QuestionNavigation
            totalQuestions={questions.length}
            currentIndex={currentIndex}
            answeredSet={answeredSet}
            questionIds={questions.map((q) => q.id)}
            onNavigate={setCurrentIndex}
          />
        </Box>

        <Paper sx={{ p: 3, mb: 3 }}>
          <QuestionDisplay
            question={currentQuestion}
            questionNumber={currentIndex + 1}
            totalQuestions={questions.length}
          />
          <AnswerInput
            question={currentQuestion}
            value={localAnswers[currentQuestion.id]}
            onChange={handleAnswerChange}
          />
        </Paper>

        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
          <Button
            startIcon={<NavigateBeforeIcon />}
            onClick={() => setCurrentIndex((i) => Math.max(0, i - 1))}
            disabled={currentIndex === 0}
          >
            {t('prev')}
          </Button>
          {currentIndex < questions.length - 1 ? (
            <Button
              endIcon={<NavigateNextIcon />}
              variant="contained"
              onClick={() => setCurrentIndex((i) => Math.min(questions.length - 1, i + 1))}
            >
              {t('next')}
            </Button>
          ) : (
            <Button
              endIcon={<SendIcon />}
              variant="contained"
              color="success"
              onClick={() => setSubmitOpen(true)}
            >
              {t('finish')}
            </Button>
          )}
        </Box>
      </Box>

      <SubmitConfirmDialog
        open={submitOpen}
        onClose={() => setSubmitOpen(false)}
        onConfirm={handleSubmit}
        answeredCount={answeredSet.size}
        totalQuestions={questions.length}
        isPending={submitAttempt.isPending}
      />
    </Box>
  );
}
