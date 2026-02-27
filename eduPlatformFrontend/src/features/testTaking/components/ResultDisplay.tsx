import { Box, Typography, Paper, Grid, Chip, Divider } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import { useTranslation } from 'react-i18next';
import type { AttemptResultDto, DetailedAnswerDto } from '@/types/testTaking';

type OptionItem = { id?: unknown; text?: unknown; isCorrect?: boolean };

/** Look up the text of the option that matches the given student answer id. */
function resolveStudentAnswerText(
  options: unknown,
  studentAnswer: unknown,
  trueFalseTrue: string,
  trueFalseFalse: string,
  questionType: string,
): string {
  if (studentAnswer == null) return '-';
  if (questionType === 'TRUE_FALSE') {
    const s = String(studentAnswer).toLowerCase();
    if (s === 'true') return trueFalseTrue;
    if (s === 'false') return trueFalseFalse;
  }
  if (Array.isArray(options) && options.length > 0) {
    const opts = options as OptionItem[];
    if (Array.isArray(studentAnswer)) {
      const texts = (studentAnswer as unknown[]).map((id) => {
        const opt = opts.find((o) => String(o.id) === String(id));
        return opt?.text != null ? String(opt.text) : String(id);
      });
      return texts.join(', ');
    }
    const opt = opts.find((o) => String(o.id) === String(studentAnswer));
    if (opt?.text != null) return String(opt.text);
  }
  return String(studentAnswer);
}

/** Find the text of the correct option(s) using isCorrect flag. */
function resolveCorrectOptionText(answer: DetailedAnswerDto): string | null {
  if (answer.correctAnswer === null) return null;
  if (answer.questionType === 'TRUE_FALSE') {
    const s = String(answer.correctAnswer).toLowerCase();
    return s; // will be shown as-is; caller can localise if needed
  }
  const opts = Array.isArray(answer.options) ? (answer.options as OptionItem[]) : [];
  const correct = opts.filter((o) => o.isCorrect === true);
  if (correct.length > 0) return correct.map((o) => String(o.text ?? '')).join(', ');
  return String(answer.correctAnswer);
}

interface ResultDisplayProps {
  result: AttemptResultDto;
}

export default function ResultDisplay({ result }: ResultDisplayProps) {
  const { t } = useTranslation('testTaking');
  const trueFalseTrue = t('trueFalseTrue');
  const trueFalseFalse = t('trueFalseFalse');

  const percentage = result.percentage;
  const scoreColor = percentage >= 70 ? 'success.main' : percentage >= 40 ? 'warning.main' : 'error.main';

  const durationMin = Math.floor(result.durationSeconds / 60);
  const durationSec = result.durationSeconds % 60;

  return (
    <Box>
      <Paper sx={{ p: 3, textAlign: 'center', mb: 3 }}>
        <Typography variant="h2" fontWeight={700} sx={{ color: scoreColor }}>
          {Math.round(percentage)}%
        </Typography>
        <Typography variant="h6" color="text.secondary" sx={{ mt: 1 }}>
          {result.score}/{result.maxScore} {t('result.points')}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          {result.testTitle}
        </Typography>
      </Paper>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={4}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <CheckCircleIcon sx={{ fontSize: 32, color: 'success.main' }} />
            <Typography variant="h5" fontWeight={600}>{result.correctAnswers}</Typography>
            <Typography variant="caption">{t('result.correct')}</Typography>
          </Paper>
        </Grid>
        <Grid item xs={4}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <CancelIcon sx={{ fontSize: 32, color: 'error.main' }} />
            <Typography variant="h5" fontWeight={600}>{result.wrongAnswers}</Typography>
            <Typography variant="caption">{t('result.wrong')}</Typography>
          </Paper>
        </Grid>
        <Grid item xs={4}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <HelpOutlineIcon sx={{ fontSize: 32, color: 'text.disabled' }} />
            <Typography variant="h5" fontWeight={600}>{result.unanswered}</Typography>
            <Typography variant="caption">{t('result.unanswered')}</Typography>
          </Paper>
        </Grid>
      </Grid>

      <Paper sx={{ p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
          <Typography variant="body2" color="text.secondary">{t('result.duration')}</Typography>
          <Typography variant="body2">{t('durationFormat', { min: durationMin, sec: durationSec })}</Typography>
        </Box>
        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
          <Typography variant="body2" color="text.secondary">{t('result.tabSwitches')}</Typography>
          <Typography variant="body2">{result.tabSwitches}</Typography>
        </Box>
      </Paper>

      <Typography variant="h6" sx={{ mb: 2 }}>{t('result.reviewTitle')}</Typography>
      {result.answers.map((answer, i) => (
        <Paper key={answer.questionId} sx={{ p: 2, mb: 1.5 }}>
          <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, mb: 1 }}>
            <Chip
              label={i + 1}
              size="small"
              color={answer.isCorrect ? 'success' : 'error'}
              sx={{ minWidth: 28 }}
            />
            <Typography variant="body1" sx={{ flex: 1 }}>{answer.questionText}</Typography>
            <Typography variant="body2" fontWeight={600}>{answer.score}/{answer.maxScore}</Typography>
          </Box>
          <Divider sx={{ my: 1 }} />
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <Typography variant="body2" color="text.secondary">
              {t('result.yourAnswer')}:{' '}
              <strong>
                {resolveStudentAnswerText(
                  answer.options,
                  answer.studentAnswer,
                  trueFalseTrue,
                  trueFalseFalse,
                  answer.questionType,
                )}
              </strong>
            </Typography>
            {!answer.isCorrect && answer.correctAnswer !== null && (() => {
              const correctText = resolveCorrectOptionText(answer);
              return correctText ? (
                <Typography variant="body2" color="success.main">
                  {t('result.correctAnswer')}: <strong>{correctText}</strong>
                </Typography>
              ) : null;
            })()}
          </Box>
          {answer.proof && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1, fontStyle: 'italic' }}>
              {answer.proof}
            </Typography>
          )}
        </Paper>
      ))}
    </Box>
  );
}
