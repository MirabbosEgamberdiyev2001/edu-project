import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Button,
  CircularProgress,
  Typography,
  Drawer,
  IconButton,
  Divider,
  Chip,
} from '@mui/material';
import NavigateBeforeIcon from '@mui/icons-material/NavigateBefore';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import SendIcon from '@mui/icons-material/Send';
import GridViewIcon from '@mui/icons-material/GridView';
import CloseIcon from '@mui/icons-material/Close';
import { useTranslation } from 'react-i18next';
import { useAttempt } from '../hooks/useAttempt';
import { useAttemptMutations } from '../hooks/useAttemptMutations';
import { useAutoSave } from '../hooks/useAutoSave';
import ExamHeader from '../components/ExamHeader';
import QuestionDisplay from '../components/QuestionDisplay';
import AnswerInput from '../components/AnswerInput';
import QuestionNavigation from '../components/QuestionNavigation';
import SubmitConfirmDialog from '../components/SubmitConfirmDialog';
import ErrorState from '@/components/ErrorState';

const SIDEBAR_WIDTH = 272;

function getPositionKey(attemptId: string) {
  return `exam_position_${attemptId}`;
}

export default function ExamPage() {
  const { attemptId } = useParams<{ attemptId: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('testTaking');
  const { data: attempt, isLoading, isError, refetch } = useAttempt(attemptId!);
  const { submitAttempt, reportTabSwitch } = useAttemptMutations();
  const { addAnswer, flush, saveStatus } = useAutoSave(attemptId!, !!attempt);

  const [currentIndex, setCurrentIndex] = useState(() => {
    if (!attemptId) return 0;
    try {
      const saved = localStorage.getItem(getPositionKey(attemptId));
      return saved ? parseInt(saved, 10) : 0;
    } catch {
      return 0;
    }
  });
  const [localAnswers, setLocalAnswers] = useState<Record<string, unknown>>({});
  const [submitOpen, setSubmitOpen] = useState(false);
  const [navDrawerOpen, setNavDrawerOpen] = useState(false);

  // Initialize local answers from attempt data
  useEffect(() => {
    if (attempt?.answers) {
      const existing: Record<string, unknown> = {};
      Object.entries(attempt.answers).forEach(([qId, ans]) => {
        existing[qId] = ans.response ?? ans.selectedAnswer;
      });
      setLocalAnswers((prev) => ({ ...existing, ...prev }));
    }
  }, [attempt]);

  // Clamp restored index to valid range
  useEffect(() => {
    if (attempt?.questions?.length) {
      setCurrentIndex((i) => Math.min(i, attempt.questions.length - 1));
    }
  }, [attempt?.questions?.length]);

  // Persist position to localStorage
  useEffect(() => {
    if (!attemptId) return;
    try {
      localStorage.setItem(getPositionKey(attemptId), String(currentIndex));
    } catch { /* ignore */ }
  }, [currentIndex, attemptId]);

  // Anti-cheat: tab switch reporting
  useEffect(() => {
    if (!attempt || !attemptId) return;
    const handleVisibility = () => {
      if (document.hidden) {
        reportTabSwitch.mutate(attemptId);
      } else {
        refetch();
      }
    };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => document.removeEventListener('visibilitychange', handleVisibility);
  }, [attempt, attemptId, reportTabSwitch, refetch]);

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

  // Warn before browser close
  useEffect(() => {
    if (!attempt) return;
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
      e.returnValue = '';
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [attempt]);

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
    addAnswer({ questionId: currentQuestion.id, questionIndex: currentIndex, response: value });
    const singleChoiceTypes = ['MCQ_SINGLE', 'TRUE_FALSE'];
    if (singleChoiceTypes.includes(currentQuestion.questionType) && currentIndex < questions.length - 1) {
      const idx = currentIndex;
      setTimeout(() => setCurrentIndex((i) => (i === idx ? i + 1 : i)), 300);
    }
  }, [currentQuestion, addAnswer, currentIndex, questions.length]);

  const handleTimeUp = useCallback(async () => {
    await flush();
    if (attemptId) {
      try { localStorage.removeItem(getPositionKey(attemptId)); } catch { /* ignore */ }
    }
    submitAttempt.mutate(attemptId!, {
      onSuccess: () => navigate(`/attempt-result/${attemptId}`, { replace: true }),
    });
  }, [flush, submitAttempt, attemptId, navigate]);

  const handleSubmit = async () => {
    await flush();
    if (attemptId) {
      try { localStorage.removeItem(getPositionKey(attemptId)); } catch { /* ignore */ }
    }
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

  if (isError || (!isLoading && !attempt)) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <ErrorState title={t('notFound')} onRetry={() => refetch()} retryLabel={t('examRetry')} />
      </Box>
    );
  }

  if (!isLoading && attempt && !currentQuestion) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
        <ErrorState title={t('examNoQuestions')} />
      </Box>
    );
  }

  if (!attempt || !currentQuestion) return null;

  const unansweredCount = questions.length - answeredSet.size;
  const HEADER_SM = 56;
  const HEADER_XS = 52;

  const questionNavContent = (
    <QuestionNavigation
      totalQuestions={questions.length}
      currentIndex={currentIndex}
      answeredSet={answeredSet}
      questionIds={questions.map((q) => q.id)}
      onNavigate={(i) => {
        setCurrentIndex(i);
        setNavDrawerOpen(false);
      }}
    />
  );

  const submitButton = (
    <Button
      variant="contained"
      color={unansweredCount > 0 ? 'warning' : 'success'}
      startIcon={<SendIcon />}
      fullWidth
      onClick={() => { setNavDrawerOpen(false); setSubmitOpen(true); }}
      sx={{ fontWeight: 700 }}
    >
      {t('finish')}
    </Button>
  );

  const questionHeader = (
    <Box
      sx={{
        px: { xs: 2, sm: 3 },
        py: 1.5,
        bgcolor: 'primary.main',
        color: 'primary.contrastText',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}
    >
      <Typography variant="subtitle2" fontWeight={700}>
        {t('questionOf', { current: currentIndex + 1, total: questions.length })}
      </Typography>
      <Chip
        label={currentQuestion.questionType?.replace(/_/g, ' ')}
        size="small"
        sx={{
          bgcolor: 'rgba(255,255,255,0.18)',
          color: 'white',
          fontSize: '0.6875rem',
          height: 20,
          fontWeight: 500,
        }}
      />
    </Box>
  );

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'grey.50' }}>
      <ExamHeader
        title={attempt.testTitle ?? attempt.assignmentTitle ?? ''}
        answeredCount={answeredSet.size}
        totalQuestions={questions.length}
        timeRemaining={attempt.timeRemaining}
        onTimeUp={handleTimeUp}
        saveStatus={saveStatus}
      />

      {/* ─── Desktop two-panel layout (md+) ─── */}
      <Box sx={{ display: { xs: 'none', md: 'flex' }, pt: `${HEADER_SM}px`, minHeight: '100vh' }}>
        {/* Left sidebar */}
        <Box
          sx={{
            width: SIDEBAR_WIDTH,
            flexShrink: 0,
            position: 'fixed',
            top: HEADER_SM,
            bottom: 0,
            overflowY: 'auto',
            borderRight: '1px solid',
            borderColor: 'divider',
            bgcolor: 'background.paper',
            p: 2,
            display: 'flex',
            flexDirection: 'column',
            gap: 2,
          }}
        >
          <Typography
            variant="caption"
            color="text.secondary"
            fontWeight={700}
            sx={{ textTransform: 'uppercase', letterSpacing: '0.08em' }}
          >
            {t('exam.navLabel')}
          </Typography>

          <Box sx={{ flex: 1, overflowY: 'auto' }}>
            {questionNavContent}
          </Box>

          <Divider />

          {unansweredCount > 0 && (
            <Typography variant="caption" color="warning.dark" sx={{ textAlign: 'center', fontWeight: 500 }}>
              {t('exam.submitWarning', { count: unansweredCount })}
            </Typography>
          )}

          {submitButton}
        </Box>

        {/* Right content panel */}
        <Box
          sx={{
            flex: 1,
            ml: `${SIDEBAR_WIDTH}px`,
            p: 3,
            display: 'flex',
            flexDirection: 'column',
            maxWidth: `calc(860px + ${SIDEBAR_WIDTH}px)`,
          }}
        >
          <Paper
            elevation={0}
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 2,
              overflow: 'hidden',
              flex: 1,
            }}
          >
            {questionHeader}
            <Box sx={{ p: 3 }}>
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
            </Box>
          </Paper>

          {/* Desktop Prev/Next */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 2 }}>
            <Button
              variant="outlined"
              startIcon={<NavigateBeforeIcon />}
              onClick={() => setCurrentIndex((i) => Math.max(0, i - 1))}
              disabled={currentIndex === 0}
            >
              {t('prev')}
            </Button>
            {currentIndex < questions.length - 1 && (
              <Button
                variant="contained"
                endIcon={<NavigateNextIcon />}
                onClick={() => setCurrentIndex((i) => Math.min(questions.length - 1, i + 1))}
              >
                {t('next')}
              </Button>
            )}
          </Box>
        </Box>
      </Box>

      {/* ─── Mobile layout (xs-sm) ─── */}
      <Box
        sx={{
          display: { xs: 'flex', md: 'none' },
          flexDirection: 'column',
          pt: `${HEADER_XS}px`,
          minHeight: '100vh',
        }}
      >
        {/* Mobile sticky sub-header: progress + nav button */}
        <Paper
          elevation={0}
          sx={{
            position: 'sticky',
            top: HEADER_XS,
            zIndex: 9,
            px: 2,
            py: 0.75,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            borderBottom: '1px solid',
            borderColor: 'divider',
            bgcolor: 'background.paper',
          }}
        >
          <Typography variant="body2" color="text.secondary">
            {t('questionOf', { current: currentIndex + 1, total: questions.length })}
          </Typography>
          <Button
            size="small"
            variant="outlined"
            startIcon={<GridViewIcon fontSize="small" />}
            onClick={() => setNavDrawerOpen(true)}
          >
            {answeredSet.size}/{questions.length}
          </Button>
        </Paper>

        {/* Question content */}
        <Box sx={{ flex: 1, px: 1.5, py: 2 }}>
          <Paper
            elevation={0}
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              borderRadius: 2,
              overflow: 'hidden',
            }}
          >
            {questionHeader}
            <Box sx={{ p: 2 }}>
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
            </Box>
          </Paper>
        </Box>

        {/* Sticky bottom navigation */}
        <Paper
          elevation={0}
          sx={{
            position: 'sticky',
            bottom: 0,
            zIndex: 9,
            px: 2,
            py: 1.25,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderTop: '1px solid',
            borderColor: 'divider',
            borderRadius: 0,
            bgcolor: 'background.paper',
          }}
        >
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
              color={unansweredCount > 0 ? 'warning' : 'success'}
              onClick={() => setSubmitOpen(true)}
              sx={{ fontWeight: 700 }}
            >
              {t('finish')}
            </Button>
          )}
        </Paper>
      </Box>

      {/* Mobile nav drawer (bottom sheet) */}
      <Drawer
        anchor="bottom"
        open={navDrawerOpen}
        onClose={() => setNavDrawerOpen(false)}
        PaperProps={{
          sx: {
            borderTopLeftRadius: 16,
            borderTopRightRadius: 16,
            maxHeight: '72vh',
            px: 2,
            pt: 1.5,
            pb: 3,
            display: 'flex',
            flexDirection: 'column',
            gap: 1.5,
          },
        }}
      >
        {/* Drag handle */}
        <Box sx={{ width: 40, height: 4, bgcolor: 'grey.300', borderRadius: 2, mx: 'auto', mb: 0.5 }} />

        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="subtitle1" fontWeight={700}>
            {t('exam.navLabel')}
          </Typography>
          <IconButton onClick={() => setNavDrawerOpen(false)} size="small">
            <CloseIcon />
          </IconButton>
        </Box>

        <Box sx={{ overflowY: 'auto', flex: 1 }}>
          {questionNavContent}
        </Box>

        <Divider />

        {unansweredCount > 0 && (
          <Typography variant="caption" color="warning.dark" fontWeight={500} sx={{ textAlign: 'center' }}>
            {t('exam.submitWarning', { count: unansweredCount })}
          </Typography>
        )}

        {submitButton}
      </Drawer>

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
