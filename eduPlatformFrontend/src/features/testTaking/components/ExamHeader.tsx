import { AppBar, Toolbar, Typography, Box, Chip, LinearProgress } from '@mui/material';
import TimerCountdown from './TimerCountdown';

interface ExamHeaderProps {
  title: string;
  answeredCount: number;
  totalQuestions: number;
  timeRemaining: number | null;
  onTimeUp: () => void;
}

export default function ExamHeader({ title, answeredCount, totalQuestions, timeRemaining, onTimeUp }: ExamHeaderProps) {
  const progress = totalQuestions > 0 ? (answeredCount / totalQuestions) * 100 : 0;
  const allAnswered = answeredCount === totalQuestions && totalQuestions > 0;

  return (
    <AppBar
      position="fixed"
      sx={{ bgcolor: 'white', color: 'text.primary', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}
    >
      <Toolbar>
        <Typography variant="h6" fontWeight={600} sx={{ flex: 1 }} noWrap>
          {title}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Chip
            label={`${answeredCount}/${totalQuestions}`}
            size="small"
            color={allAnswered ? 'success' : 'primary'}
            variant="outlined"
            sx={{ fontWeight: 600 }}
          />
          {timeRemaining != null && timeRemaining > 0 && (
            <TimerCountdown initialSeconds={timeRemaining} onTimeUp={onTimeUp} />
          )}
        </Box>
      </Toolbar>
      <LinearProgress
        variant="determinate"
        value={progress}
        sx={{
          height: 3,
          bgcolor: 'grey.100',
          '& .MuiLinearProgress-bar': {
            bgcolor: allAnswered ? 'success.main' : 'primary.main',
            transition: 'transform 0.3s ease, background-color 0.3s ease',
          },
        }}
      />
    </AppBar>
  );
}
