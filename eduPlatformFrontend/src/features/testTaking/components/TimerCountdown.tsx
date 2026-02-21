import { useState, useEffect } from 'react';
import { Typography, Box } from '@mui/material';
import TimerIcon from '@mui/icons-material/Timer';

interface TimerCountdownProps {
  initialSeconds: number;
  onTimeUp: () => void;
}

export default function TimerCountdown({ initialSeconds, onTimeUp }: TimerCountdownProps) {
  const [seconds, setSeconds] = useState(initialSeconds);

  useEffect(() => {
    setSeconds(initialSeconds);
  }, [initialSeconds]);

  useEffect(() => {
    if (seconds <= 0) {
      onTimeUp();
      return;
    }
    const timer = setInterval(() => setSeconds((s) => s - 1), 1000);
    return () => clearInterval(timer);
  }, [seconds, onTimeUp]);

  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  const isLow = seconds < 300; // less than 5 minutes

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
      <TimerIcon fontSize="small" sx={{ color: isLow ? 'error.main' : 'text.secondary' }} />
      <Typography
        variant="h6"
        fontWeight={700}
        sx={{ color: isLow ? 'error.main' : 'text.primary', fontFamily: 'monospace' }}
      >
        {String(minutes).padStart(2, '0')}:{String(secs).padStart(2, '0')}
      </Typography>
    </Box>
  );
}
