import { Box, LinearProgress, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useMemo } from 'react';

interface PasswordStrengthProps {
  password: string;
}

function getStrength(password: string): number {
  let score = 0;
  if (password.length >= 8) score++;
  if (/[a-z]/.test(password)) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (password.length >= 12) score++;
  return score;
}

export default function PasswordStrength({ password }: PasswordStrengthProps) {
  const { t } = useTranslation('auth');

  const strength = useMemo(() => getStrength(password), [password]);

  if (!password) return null;

  const config = [
    { label: t('passwordStrength.weak'), color: '#f44336' },
    { label: t('passwordStrength.weak'), color: '#f44336' },
    { label: t('passwordStrength.fair'), color: '#ff9800' },
    { label: t('passwordStrength.good'), color: '#2196f3' },
    { label: t('passwordStrength.strong'), color: '#4caf50' },
    { label: t('passwordStrength.strong'), color: '#4caf50' },
  ][strength];

  return (
    <Box sx={{ mt: 1 }}>
      <LinearProgress
        variant="determinate"
        value={(strength / 5) * 100}
        sx={{
          height: 6,
          borderRadius: 3,
          bgcolor: 'grey.200',
          '& .MuiLinearProgress-bar': { bgcolor: config.color, borderRadius: 3 },
        }}
      />
      <Typography variant="caption" sx={{ color: config.color, mt: 0.5 }}>
        {config.label}
      </Typography>
    </Box>
  );
}
