import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  TextField,
  Button,
  CircularProgress,
} from '@mui/material';
import LinkIcon from '@mui/icons-material/Link';
import QrCodeScannerIcon from '@mui/icons-material/QrCodeScanner';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { useTranslation } from 'react-i18next';
import { PageShell } from '@/components/ui';
import { useParentMutations } from '../hooks/useParentMutations';

export default function ParentPairPage() {
  const { t } = useTranslation('parent');
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const urlCode = searchParams.get('code') ?? '';

  const [code, setCode] = useState(urlCode.toUpperCase());
  const { pairWithCode } = useParentMutations();

  // Auto-submit when a valid 8-char code arrives via QR URL param
  useEffect(() => {
    if (urlCode.length === 8) {
      pairWithCode.mutate({ code: urlCode.toUpperCase() });
    }
    // Run only on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = code.trim();
    if (trimmed.length === 8) {
      pairWithCode.mutate({ code: trimmed });
    }
  };

  // After success, redirect to children list after a short delay
  useEffect(() => {
    if (pairWithCode.isSuccess) {
      const timer = setTimeout(() => navigate('/my-children', { replace: true }), 1500);
      return () => clearTimeout(timer);
    }
  }, [pairWithCode.isSuccess, navigate]);

  if (pairWithCode.isSuccess) {
    return (
      <PageShell title={t('linkSuccess')} subtitle="">
        <Paper
          sx={{
            p: 5,
            textAlign: 'center',
            maxWidth: 400,
            mx: 'auto',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 2,
          }}
        >
          <CheckCircleIcon sx={{ fontSize: 72, color: 'success.main' }} />
          <Typography variant="h6" fontWeight={700}>
            {t('linkSuccess')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t('noChildrenDescription')}
          </Typography>
          <Button
            variant="contained"
            onClick={() => navigate('/my-children', { replace: true })}
          >
            {t('myChildrenTitle')}
          </Button>
        </Paper>
      </PageShell>
    );
  }

  // Auto-submitting state (QR code flow — code in URL, waiting for response)
  if (urlCode.length === 8 && pairWithCode.isPending) {
    return (
      <PageShell title={t('enterCode')} subtitle={t('pairChildSubtitle')}>
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 8, gap: 2 }}>
          <CircularProgress size={48} />
          <Typography color="text.secondary">{t('pairing')}</Typography>
        </Box>
      </PageShell>
    );
  }

  return (
    <PageShell title={t('enterCode')} subtitle={t('pairChildSubtitle')}>
      <Paper sx={{ p: 4, maxWidth: 420, mx: 'auto' }}>
        <Box sx={{ display: 'flex', justifyContent: 'center', mb: 3 }}>
          <QrCodeScannerIcon sx={{ fontSize: 72, color: 'text.disabled' }} />
        </Box>

        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ mb: 3, textAlign: 'center' }}
        >
          {t('enterCodeDesc')}
        </Typography>

        <Box
          component="form"
          onSubmit={handleSubmit}
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          <TextField
            value={code}
            onChange={(e) =>
              setCode(e.target.value.replace(/[^a-zA-Z0-9]/g, '').slice(0, 8).toUpperCase())
            }
            placeholder="XXXXXXXX"
            size="medium"
            fullWidth
            inputProps={{
              maxLength: 8,
              style: {
                textAlign: 'center',
                letterSpacing: 8,
                fontWeight: 700,
                fontSize: '1.5rem',
              },
            }}
            autoFocus={!urlCode}
          />
          <Button
            type="submit"
            variant="contained"
            size="large"
            fullWidth
            disabled={pairWithCode.isPending || code.trim().length < 8}
            startIcon={
              pairWithCode.isPending ? (
                <CircularProgress size={18} color="inherit" />
              ) : (
                <LinkIcon />
              )
            }
          >
            {pairWithCode.isPending ? t('pairing') : t('pairWithCode')}
          </Button>
        </Box>
      </Paper>
    </PageShell>
  );
}
