import { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  TextField,
  Divider,
  CircularProgress,
  Chip,
  Stack,
  Alert,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { testTakingApi } from '@/api/testTakingApi';
import { MathText } from '@/components/math';
import { useManualGrading } from '../hooks/useManualGrading';
import { useToast } from '@/hooks/useToast';
import type { AttemptQuestionDto, AttemptAnswerDto } from '@/types/testTaking';

interface ManualGradingDialogProps {
  open: boolean;
  onClose: () => void;
  attemptId: string;
  assignmentId: string;
  studentName: string;
}

interface GradingState {
  score: string;
  feedback: string;
}

export default function ManualGradingDialog({
  open,
  onClose,
  attemptId,
  assignmentId,
  studentName,
}: ManualGradingDialogProps) {
  const { t } = useTranslation('assignment');
  const toast = useToast();
  const [grades, setGrades] = useState<Record<string, GradingState>>({});
  const [validationError, setValidationError] = useState<string | null>(null);

  const { data: attempt, isLoading } = useQuery({
    queryKey: ['attempts', attemptId],
    queryFn: () => testTakingApi.getAttempt(attemptId).then((r) => r.data.data),
    enabled: open && !!attemptId,
  });

  const { gradeAnswer, isPending } = useManualGrading(assignmentId);

  const answersMap = (attempt?.answers ?? {}) as Record<string, AttemptAnswerDto>;
  const questions = (attempt?.questions ?? []) as AttemptQuestionDto[];

  // Show all questions that need manual grading, including already-graded ones for re-review
  const manualQuestions = questions.filter((q) => {
    const ans = answersMap[q.id];
    return ans?.needsManualGrading === true || (ans?.isCorrect == null && q.questionType !== 'MCQ_SINGLE' && q.questionType !== 'MCQ_MULTI' && q.questionType !== 'TRUE_FALSE');
  });

  const handleSubmitAll = async () => {
    setValidationError(null);

    // Client-side validation: check all entered scores are within range
    for (const q of manualQuestions) {
      const grade = grades[q.id];
      // Skip questions where teacher didn't enter a score (leave as-is)
      if (grade?.score == null || grade.score === '') continue;
      const scoreNum = Number(grade.score);
      if (isNaN(scoreNum) || scoreNum < 0) {
        setValidationError(t('grading.scoreInvalid', { num: q.questionIndex ?? '' }));
        return;
      }
      if (scoreNum > q.points) {
        setValidationError(t('grading.scoreExceedsMax', { max: q.points }));
        return;
      }
    }

    let savedCount = 0;
    try {
      for (const q of manualQuestions) {
        const ans = answersMap[q.id];
        const grade = grades[q.id];
        // Skip if no answer ID or teacher didn't enter a score
        if (!ans?.id || grade?.score == null || grade.score === '') continue;
        await gradeAnswer(ans.id, Number(grade.score), grade.feedback || undefined);
        savedCount++;
      }
      if (savedCount > 0) {
        toast.success(t('grading.savedSuccess'));
      }
      onClose();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg || t('grading.saveFailed'));
    }
  };

  const updateGrade = (questionId: string, field: keyof GradingState, val: string) => {
    setValidationError(null);
    setGrades((prev) => ({
      ...prev,
      [questionId]: { ...(prev[questionId] ?? { score: '', feedback: '' }), [field]: val },
    }));
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Typography variant="h6" fontWeight={700}>
          {t('manualGrading')}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {studentName}
        </Typography>
      </DialogTitle>
      <Divider />
      <DialogContent sx={{ py: 2 }}>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        ) : manualQuestions.length === 0 ? (
          <Typography variant="body2" color="text.secondary" textAlign="center" sx={{ py: 4 }}>
            {t('noManualQuestions')}
          </Typography>
        ) : (
          <Stack spacing={3}>
            {validationError && (
              <Alert severity="error" onClose={() => setValidationError(null)}>
                {validationError}
              </Alert>
            )}
            {manualQuestions.map((q, idx) => {
              const ans = answersMap[q.id];
              const grade = grades[q.id] ?? { score: '', feedback: '' };
              const maxPts = q.points ?? 1;
              const currentScore = Number(grade.score);
              const scoreInvalid = grade.score !== '' && (isNaN(currentScore) || currentScore < 0 || currentScore > maxPts);

              return (
                <Box key={q.id}>
                  <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, mb: 1.5 }}>
                    <Chip label={idx + 1} size="small" color="primary" sx={{ fontWeight: 700, flexShrink: 0 }} />
                    <MathText text={q.questionText} variant="body1" sx={{ flex: 1 }} />
                    <Chip
                      label={`max ${maxPts} ${t('points')}`}
                      size="small"
                      variant="outlined"
                      color="default"
                      sx={{ flexShrink: 0 }}
                    />
                  </Box>

                  {/* Student answer */}
                  <Box sx={{ p: 1.5, bgcolor: 'grey.50', borderRadius: 1.5, mb: 1.5, border: '1px solid', borderColor: 'divider' }}>
                    <Typography variant="caption" fontWeight={700} color="text.secondary" display="block" sx={{ mb: 0.5 }}>
                      {t('studentAnswer')}:
                    </Typography>
                    <Typography variant="body2">
                      {String(ans?.response ?? ans?.selectedAnswer ?? '—')}
                    </Typography>
                  </Box>

                  {/* Score + feedback inputs */}
                  <Stack direction="row" spacing={2} alignItems="flex-start">
                    <TextField
                      label={`${t('score')} (0 — ${maxPts})`}
                      type="number"
                      size="small"
                      value={grade.score}
                      onChange={(e) => updateGrade(q.id, 'score', e.target.value)}
                      error={scoreInvalid}
                      helperText={scoreInvalid ? t('grading.scoreExceedsMax', { max: maxPts }) : undefined}
                      inputProps={{ min: 0, max: maxPts, step: 0.5 }}
                      sx={{ width: 160, flexShrink: 0 }}
                    />
                    <TextField
                      label={t('feedback')}
                      size="small"
                      fullWidth
                      multiline
                      rows={2}
                      value={grade.feedback}
                      onChange={(e) => updateGrade(q.id, 'feedback', e.target.value)}
                      placeholder={t('feedbackPlaceholder')}
                    />
                  </Stack>
                  {idx < manualQuestions.length - 1 && <Divider sx={{ mt: 3 }} />}
                </Box>
              );
            })}
          </Stack>
        )}
      </DialogContent>
      <Divider />
      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose} disabled={isPending}>
          {t('cancel')}
        </Button>
        <Button
          variant="contained"
          onClick={handleSubmitAll}
          disabled={isPending || isLoading || manualQuestions.length === 0}
          startIcon={isPending ? <CircularProgress size={16} /> : undefined}
        >
          {t('gradeSubmit')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
