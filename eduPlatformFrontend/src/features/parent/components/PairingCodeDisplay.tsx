import { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Paper,
  Button,
  CircularProgress,
  IconButton,
  Tooltip,
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import RefreshIcon from '@mui/icons-material/Refresh';
import QrCodeIcon from '@mui/icons-material/QrCode';
import { useTranslation } from 'react-i18next';
import type { GeneratePairingCodeResponse } from '@/types/parent';

interface PairingCodeDisplayProps {
  data: GeneratePairingCodeResponse | null;
  isPending: boolean;
  onGenerate: () => void;
}

export default function PairingCodeDisplay({ data, isPending, onGenerate }: PairingCodeDisplayProps) {
  const { t } = useTranslation('parent');
  const [copied, setCopied] = useState(false);
  const [remainingSeconds, setRemainingSeconds] = useState(0);

  useEffect(() => {
    if (data?.expiresIn) {
      setRemainingSeconds(data.expiresIn);
    }
  }, [data?.expiresIn]);

  useEffect(() => {
    if (remainingSeconds <= 0) return;

    const timer = setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [remainingSeconds]);

  const handleCopy = useCallback(async () => {
    if (!data?.code) return;
    try {
      await navigator.clipboard.writeText(data.code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard not available
    }
  }, [data?.code]);

  const remainingMinutes = Math.ceil(remainingSeconds / 60);

  if (!data) {
    return (
      <Paper sx={{ p: 3, textAlign: 'center' }}>
        <QrCodeIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
        <Typography variant="body1" color="text.secondary" sx={{ mb: 2 }}>
          {t('scanQR')}
        </Typography>
        <Button
          variant="contained"
          onClick={onGenerate}
          disabled={isPending}
          startIcon={isPending ? <CircularProgress size={18} /> : undefined}
        >
          {t('generateCode')}
        </Button>
      </Paper>
    );
  }

  const isExpired = remainingSeconds <= 0;

  return (
    <Paper sx={{ p: 3, textAlign: 'center' }}>
      {data.qrCodeDataUri && (
        <Box sx={{ mb: 2 }}>
          <img
            src={data.qrCodeDataUri}
            alt="QR Code"
            style={{ width: 200, height: 200, imageRendering: 'pixelated' }}
          />
        </Box>
      )}

      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 1,
          mb: 1,
        }}
      >
        <Typography
          variant="h4"
          fontWeight={700}
          letterSpacing={6}
          sx={{ opacity: isExpired ? 0.4 : 1 }}
        >
          {data.code}
        </Typography>
        <Tooltip title={copied ? t('codeCopied') : ''}>
          <IconButton onClick={handleCopy} disabled={isExpired} size="small">
            <ContentCopyIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </Box>

      <Typography
        variant="body2"
        color={isExpired ? 'error.main' : 'text.secondary'}
        sx={{ mb: 2 }}
      >
        {isExpired
          ? t('codeExpires', { minutes: 0 })
          : t('codeExpires', { minutes: remainingMinutes })}
      </Typography>

      <Button
        variant="outlined"
        size="small"
        onClick={onGenerate}
        disabled={isPending}
        startIcon={isPending ? <CircularProgress size={16} /> : <RefreshIcon />}
      >
        {t('generateCode')}
      </Button>
    </Paper>
  );
}
