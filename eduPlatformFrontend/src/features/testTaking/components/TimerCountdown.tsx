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

  // Three urgency levels:
  //   normal   ≥ 180 s  — neutral colors
  //   warning  < 180 s  — amber/orange (3 minutes)
  //   critical < 60 s   — red + pulse (1 minute)
  const isWarning = seconds > 0 && seconds < 180;
  const isCritical = seconds > 0 && seconds < 60;

  const timerColor = isCritical ? 'error.main' : isWarning ? 'warning.main' : 'text.primary';
  const iconColor = isCritical ? 'error' : isWarning ? ('warning' as const) : ('action' as const);

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 0.5,
        '@keyframes timerPulse': {
          '0%, 100%': { opacity: 1 },
          '50%': { opacity: 0.55 },
        },
        animation: isCritical ? 'timerPulse 1s ease-in-out infinite' : 'none',
      }}
    >
      <TimerIcon fontSize="small" color={iconColor} />
      <Typography
        variant="subtitle1"
        fontWeight={700}
        sx={{ color: timerColor, fontFamily: 'monospace', fontSize: '1rem', lineHeight: 1 }}
      >
        {String(minutes).padStart(2, '0')}:{String(secs).padStart(2, '0')}
      </Typography>
    </Box>
  );
}
