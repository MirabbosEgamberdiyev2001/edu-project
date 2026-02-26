import { useState, useEffect, useRef, useCallback } from 'react';
import { Typography, Box } from '@mui/material';
import TimerIcon from '@mui/icons-material/Timer';

interface TimerCountdownProps {
  initialSeconds: number;
  onTimeUp: () => void;
}

export default function TimerCountdown({ initialSeconds, onTimeUp }: TimerCountdownProps) {
  // Store absolute deadline so timer survives system sleep and tab switches accurately
  const endTimeRef = useRef(Date.now() + initialSeconds * 1000);
  const [seconds, setSeconds] = useState(initialSeconds);

  // Re-anchor endTime when server provides a fresh initialSeconds (tab restore refetch)
  useEffect(() => {
    endTimeRef.current = Date.now() + initialSeconds * 1000;
    setSeconds(Math.max(0, Math.round((endTimeRef.current - Date.now()) / 1000)));
  }, [initialSeconds]);

  // Keep onTimeUp in a ref so the interval closure never goes stale
  const onTimeUpRef = useRef(onTimeUp);
  useEffect(() => { onTimeUpRef.current = onTimeUp; }, [onTimeUp]);

  const tick = useCallback(() => {
    const remaining = Math.max(0, Math.round((endTimeRef.current - Date.now()) / 1000));
    setSeconds(remaining);
    if (remaining <= 0) onTimeUpRef.current();
  }, []); // stable — reads everything via refs

  useEffect(() => {
    const timer = setInterval(tick, 1000);
    // Re-sync immediately when the tab becomes visible after sleep / switching
    const handleVisibility = () => { if (!document.hidden) tick(); };
    document.addEventListener('visibilitychange', handleVisibility);
    return () => {
      clearInterval(timer);
      document.removeEventListener('visibilitychange', handleVisibility);
    };
  }, [tick]);

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
