import { AppBar, Toolbar, Typography, Box, Chip } from '@mui/material';
import TimerCountdown from './TimerCountdown';

interface ExamHeaderProps {
  title: string;
  answeredCount: number;
  totalQuestions: number;
  timeRemaining: number | null;
  onTimeUp: () => void;
}

export default function ExamHeader({ title, answeredCount, totalQuestions, timeRemaining, onTimeUp }: ExamHeaderProps) {
  return (
    <AppBar position="fixed" sx={{ bgcolor: 'white', color: 'text.primary', boxShadow: '0 1px 3px rgba(0,0,0,0.08)' }}>
      <Toolbar>
        <Typography variant="h6" fontWeight={600} sx={{ flex: 1 }} noWrap>
          {title}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Chip label={`${answeredCount}/${totalQuestions}`} size="small" color="primary" variant="outlined" />
          {timeRemaining != null && timeRemaining > 0 && (
            <TimerCountdown initialSeconds={timeRemaining} onTimeUp={onTimeUp} />
          )}
        </Box>
      </Toolbar>
    </AppBar>
  );
}
