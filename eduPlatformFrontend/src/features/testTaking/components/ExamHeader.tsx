import { AppBar, Toolbar, Typography, Box, Chip, LinearProgress } from '@mui/material';
import { useTranslation } from 'react-i18next';
import TimerCountdown from './TimerCountdown';

interface ExamHeaderProps {
  title: string;
  answeredCount: number;
  totalQuestions: number;
  timeRemaining: number | null;
  onTimeUp: () => void;
}

export default function ExamHeader({ title, answeredCount, totalQuestions, timeRemaining, onTimeUp }: ExamHeaderProps) {
  const { t } = useTranslation('testTaking');
  const progress = totalQuestions > 0 ? (answeredCount / totalQuestions) * 100 : 0;
  const allAnswered = answeredCount === totalQuestions && totalQuestions > 0;
  const isTimerUrgent = timeRemaining != null && timeRemaining <= 60;

  return (
    <AppBar
      position="fixed"
      sx={{ bgcolor: 'white', color: 'text.primary', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}
    >
      <Toolbar sx={{ minHeight: { xs: 52, sm: 56 } }}>
        <Typography
          variant="subtitle1"
          fontWeight={600}
          noWrap
          sx={{ flex: 1, fontSize: { xs: '0.875rem', sm: '1rem' } }}
        >
          {title}
        </Typography>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: { xs: 1, sm: 1.5 } }}>
          {/* Answered progress chip */}
          <Chip
            label={
              allAnswered
                ? t('exam.allAnswered')
                : t('exam.answeredOf', { answered: answeredCount, total: totalQuestions })
            }
            size="small"
            color={allAnswered ? 'success' : 'default'}
            variant={allAnswered ? 'filled' : 'outlined'}
            sx={{ fontWeight: 600, fontSize: '0.75rem' }}
          />

          {/* Timer — only when there's a time limit */}
          {timeRemaining != null && timeRemaining > 0 && (
            <TimerCountdown initialSeconds={timeRemaining} onTimeUp={onTimeUp} />
          )}
        </Box>
      </Toolbar>

      {/* Progress bar — fills as questions are answered */}
      <LinearProgress
        variant="determinate"
        value={progress}
        sx={{
          height: 3,
          bgcolor: 'grey.100',
          '& .MuiLinearProgress-bar': {
            bgcolor: allAnswered
              ? 'success.main'
              : isTimerUrgent
                ? 'error.main'
                : 'primary.main',
            transition: 'transform 0.4s ease, background-color 0.4s ease',
          },
        }}
      />
    </AppBar>
  );
}
