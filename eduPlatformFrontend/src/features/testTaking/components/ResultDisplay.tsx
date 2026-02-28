import { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  Chip,
  Divider,
  Tabs,
  Tab,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import RemoveCircleOutlineIcon from '@mui/icons-material/RemoveCircleOutline';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { useTranslation } from 'react-i18next';
import { resolveTranslation } from '@/utils/i18nUtils';
import type { AttemptResultDto, DetailedAnswerDto } from '@/types/testTaking';
import { MathText } from '@/components/math';

type OptionItem = { id?: unknown; key?: unknown; text?: unknown; isCorrect?: boolean };
type TabValue = 'all' | 'correct' | 'wrong' | 'skipped';

/** Universal safe-to-string that handles multilingual maps without producing [object Object] */
function resolveValue(val: unknown): string {
  if (val == null) return '';
  if (typeof val === 'string') return val;
  if (typeof val === 'boolean') return String(val);
  if (typeof val === 'number') return String(val);
  if (Array.isArray(val)) return (val as unknown[]).map(resolveValue).join(', ');
  if (typeof val === 'object') {
    const translated = resolveTranslation(val as Record<string, string>);
    if (translated != null && translated !== '') return translated;
  }
  return '';
}

function resolveCorrectOptionText(
  answer: DetailedAnswerDto,
  trueFalseTrue: string,
  trueFalseFalse: string,
): string | null {
  if (answer.correctAnswer == null) return null;

  if (answer.questionType === 'TRUE_FALSE') {
    const s = String(answer.correctAnswer).toLowerCase();
    if (s === 'true') return trueFalseTrue;
    if (s === 'false') return trueFalseFalse;
  }

  const opts = Array.isArray(answer.options) ? (answer.options as OptionItem[]) : [];

  if (['MCQ_SINGLE', 'MCQ_MULTI'].includes(answer.questionType) && opts.length > 0) {
    const ids = Array.isArray(answer.correctAnswer)
      ? (answer.correctAnswer as unknown[]).map(String)
      : typeof answer.correctAnswer === 'string'
        ? [answer.correctAnswer]
        : [];

    if (ids.length > 0) {
      const texts = ids
        .map((id) => {
          const opt = opts.find((o) => String(o.id ?? o.key ?? '') === id);
          return opt?.text != null ? resolveValue(opt.text) : id;
        })
        .filter(Boolean);
      if (texts.length > 0) return texts.join(', ');
    }

    // Fallback: isCorrect flag
    const flagged = opts.filter((o) => o.isCorrect === true);
    if (flagged.length > 0) return flagged.map((o) => resolveValue(o.text ?? '')).join(', ');
  }

  const resolved = resolveValue(answer.correctAnswer);
  return resolved || null;
}

function resolveStudentAnswerText(
  options: unknown,
  studentAnswer: unknown,
  trueFalseTrue: string,
  trueFalseFalse: string,
  questionType: string,
  noAnswerText: string,
): string {
  const isEmpty =
    studentAnswer == null ||
    studentAnswer === '' ||
    (Array.isArray(studentAnswer) && (studentAnswer as unknown[]).length === 0);
  if (isEmpty) return noAnswerText;

  if (questionType === 'TRUE_FALSE') {
    const s = String(studentAnswer).toLowerCase();
    if (s === 'true') return trueFalseTrue;
    if (s === 'false') return trueFalseFalse;
  }

  if (Array.isArray(options) && options.length > 0) {
    const opts = options as OptionItem[];
    if (Array.isArray(studentAnswer)) {
      const texts = (studentAnswer as unknown[]).map((id) => {
        const opt = opts.find((o) => String(o.id ?? o.key ?? '') === String(id));
        return opt?.text != null ? resolveValue(opt.text) : String(id);
      });
      return texts.join(', ');
    }
    const opt = opts.find((o) => String(o.id ?? o.key ?? '') === String(studentAnswer));
    if (opt?.text != null) return resolveValue(opt.text);
  }

  return resolveValue(studentAnswer);
}

interface ResultDisplayProps {
  result: AttemptResultDto;
}

export default function ResultDisplay({ result }: ResultDisplayProps) {
  const { t } = useTranslation('testTaking');
  const [tab, setTab] = useState<TabValue>('all');

  const trueFalseTrue = t('trueFalseTrue');
  const trueFalseFalse = t('trueFalseFalse');
  const noAnswerText = t('result.noAnswer');

  const percentage = Math.round(result.percentage);
  const scoreColor = percentage >= 70 ? 'success.main' : percentage >= 40 ? 'warning.main' : 'error.main';

  const statusLabel =
    percentage >= 70 ? t('result.passed') : percentage >= 40 ? t('result.average') : t('result.failed');
  const statusColor: 'success' | 'warning' | 'error' =
    percentage >= 70 ? 'success' : percentage >= 40 ? 'warning' : 'error';

  const durationMin = Math.floor(result.durationSeconds / 60);
  const durationSec = result.durationSeconds % 60;

  const isSkipped = (a: DetailedAnswerDto) =>
    a.studentAnswer == null ||
    a.studentAnswer === '' ||
    (Array.isArray(a.studentAnswer) && (a.studentAnswer as unknown[]).length === 0);

  // Compute counts from the actual answers array for accurate tab labels
  const correctCount = result.answers.filter((a) => a.isCorrect === true).length;
  const skippedCount = result.answers.filter(isSkipped).length;
  const wrongCount = result.answers.filter((a) => a.isCorrect === false && !isSkipped(a)).length;

  const filtered = result.answers.filter((a) => {
    if (tab === 'correct') return a.isCorrect === true;
    if (tab === 'wrong') return a.isCorrect === false && !isSkipped(a);
    if (tab === 'skipped') return isSkipped(a);
    return true;
  });

  return (
    <Box>
      {/* Score header */}
      <Paper sx={{ p: 3, mb: 2, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1 }}>
        <Chip label={statusLabel} color={statusColor} size="medium" sx={{ fontWeight: 700, fontSize: '0.875rem' }} />
        <Typography variant="h2" fontWeight={800} sx={{ color: scoreColor, lineHeight: 1 }}>
          {percentage}%
        </Typography>
        <Typography variant="body1" color="text.secondary">
          {result.score}/{result.maxScore} {t('result.points')}
        </Typography>
        {result.testTitle && (
          <Typography variant="body2" color="text.disabled">
            {result.testTitle}
          </Typography>
        )}
      </Paper>

      {/* Summary row */}
      <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
        {[
          {
            icon: <CheckCircleIcon sx={{ fontSize: 20, color: 'success.main' }} />,
            value: correctCount,
            label: t('result.correct'),
            bgcolor: '#f0fdf4',
            border: '#bbf7d0',
          },
          {
            icon: <CancelIcon sx={{ fontSize: 20, color: 'error.main' }} />,
            value: wrongCount,
            label: t('result.wrong'),
            bgcolor: '#fef2f2',
            border: '#fecaca',
          },
          {
            icon: <RemoveCircleOutlineIcon sx={{ fontSize: 20, color: 'text.disabled' }} />,
            value: skippedCount,
            label: t('result.unanswered'),
            bgcolor: '#f8fafc',
            border: '#e2e8f0',
          },
          {
            icon: <AccessTimeIcon sx={{ fontSize: 20, color: 'text.secondary' }} />,
            value: t('durationFormat', { min: durationMin, sec: durationSec }),
            label: t('result.duration'),
            bgcolor: '#f8fafc',
            border: '#e2e8f0',
          },
        ].map((item) => (
          <Box
            key={item.label}
            sx={{
              flex: '1 1 auto',
              minWidth: 80,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 0.25,
              p: 1.5,
              bgcolor: item.bgcolor,
              border: `1px solid ${item.border}`,
              borderRadius: 2,
            }}
          >
            {item.icon}
            <Typography variant="h6" fontWeight={700} lineHeight={1.2}>
              {item.value}
            </Typography>
            <Typography variant="caption" color="text.secondary" textAlign="center">
              {item.label}
            </Typography>
          </Box>
        ))}
      </Box>

      {/* Filter tabs */}
      <Paper variant="outlined" sx={{ mb: 2 }}>
        <Tabs
          value={tab}
          onChange={(_, v: TabValue) => setTab(v)}
          variant="fullWidth"
          textColor="primary"
          indicatorColor="primary"
        >
          <Tab value="all" label={t('result.tabAll')} />
          <Tab value="correct" label={`${t('result.tabCorrect')} (${correctCount})`} />
          <Tab value="wrong" label={`${t('result.tabWrong')} (${wrongCount})`} />
          <Tab value="skipped" label={`${t('result.tabSkipped')} (${skippedCount})`} />
        </Tabs>
      </Paper>

      {/* Question blocks */}
      {filtered.map((answer) => {
        const globalIndex = result.answers.indexOf(answer);
        const skipped = isSkipped(answer);
        const chipColor = answer.isCorrect ? 'success' : skipped ? 'default' : 'error';

        const studentAnswerText = resolveStudentAnswerText(
          answer.options,
          answer.studentAnswer,
          trueFalseTrue,
          trueFalseFalse,
          answer.questionType,
          noAnswerText,
        );
        const correctAnswerText = !answer.isCorrect
          ? resolveCorrectOptionText(answer, trueFalseTrue, trueFalseFalse)
          : null;

        return (
          <Paper key={answer.questionId} variant="outlined" sx={{ mb: 1.5, overflow: 'hidden' }}>
            {/* Question header */}
            <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, p: 2, pb: 1.5 }}>
              <Chip
                label={globalIndex + 1}
                size="small"
                color={chipColor}
                sx={{ minWidth: 32, fontWeight: 700, flexShrink: 0 }}
              />
              <MathText text={answer.questionText} variant="body1" sx={{ flex: 1 }} />
              <Typography variant="body2" fontWeight={600} whiteSpace="nowrap">
                {answer.score}/{answer.maxScore}
              </Typography>
            </Box>

            <Divider />

            {/* Answer details */}
            <Box sx={{ px: 2, py: 1.5, display: 'flex', flexDirection: 'column', gap: 0.75 }}>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', alignItems: 'baseline' }}>
                <Typography variant="body2" color="text.secondary" sx={{ minWidth: 130, flexShrink: 0 }}>
                  {t('result.yourAnswer')}:
                </Typography>
                <Typography
                  variant="body2"
                  fontWeight={600}
                  color={skipped ? 'text.disabled' : answer.isCorrect ? 'success.dark' : 'error.dark'}
                  fontStyle={skipped ? 'italic' : 'normal'}
                >
                  {studentAnswerText}
                </Typography>
              </Box>

              {correctAnswerText && (
                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', alignItems: 'baseline' }}>
                  <Typography variant="body2" color="text.secondary" sx={{ minWidth: 130, flexShrink: 0 }}>
                    {t('result.correctAnswer')}:
                  </Typography>
                  <Typography variant="body2" fontWeight={600} color="success.dark">
                    {correctAnswerText}
                  </Typography>
                </Box>
              )}
            </Box>

            {/* Collapsible explanation */}
            {answer.proof && (
              <Accordion
                disableGutters
                elevation={0}
                sx={{
                  borderTop: '1px solid',
                  borderColor: 'divider',
                  bgcolor: '#fafafa',
                  '&:before': { display: 'none' },
                }}
              >
                <AccordionSummary
                  expandIcon={<ExpandMoreIcon />}
                  sx={{ px: 2, minHeight: 40, '& .MuiAccordionSummary-content': { my: 0.5 } }}
                >
                  <Typography variant="body2" color="text.secondary" fontWeight={500}>
                    {t('result.explanation')}
                  </Typography>
                </AccordionSummary>
                <AccordionDetails sx={{ px: 2, pt: 0, pb: 1.5 }}>
                  <MathText text={answer.proof ?? ''} variant="body2" sx={{ color: 'text.secondary' }} />
                </AccordionDetails>
              </Accordion>
            )}
          </Paper>
        );
      })}

      {filtered.length === 0 && (
        <Box sx={{ textAlign: 'center', py: 4 }}>
          <Typography variant="body2" color="text.disabled">
            —
          </Typography>
        </Box>
      )}
    </Box>
  );
}
