import { useState, useEffect, useCallback } from 'react';
import { Button, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { OTP_COUNTDOWN_SECONDS } from '@/config';

interface OtpCountdownProps {
  onResend: () => void;
  isResending?: boolean;
}

export default function OtpCountdown({ onResend, isResending }: OtpCountdownProps) {
  const { t } = useTranslation('auth');
  const [seconds, setSeconds] = useState(OTP_COUNTDOWN_SECONDS);

  useEffect(() => {
    if (seconds <= 0) return;
    const timer = setInterval(() => setSeconds((s) => s - 1), 1000);
    return () => clearInterval(timer);
  }, [seconds]);

  const handleResend = useCallback(() => {
    onResend();
    setSeconds(OTP_COUNTDOWN_SECONDS);
  }, [onResend]);

  if (seconds > 0) {
    return (
      <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 500 }}>
        {t('otp.resendIn', { seconds })}
      </Typography>
    );
  }

  return (
    <Button
      onClick={handleResend}
      disabled={isResending}
      fullWidth
      sx={{ textTransform: 'none', fontWeight: 600 }}
    >
      {t('otp.resend')}
    </Button>
  );
}
