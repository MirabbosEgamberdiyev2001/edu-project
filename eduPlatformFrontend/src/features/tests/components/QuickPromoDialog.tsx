import { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  IconButton,
  Tooltip,
  CircularProgress,
  Alert,
  Divider,
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import { useTranslation } from 'react-i18next';
import { assignmentApi } from '@/api/assignmentApi';
import { useToast } from '@/hooks/useToast';
import type { TestHistoryDto } from '@/types/test';

interface QuickPromoDialogProps {
  open: boolean;
  onClose: () => void;
  test: TestHistoryDto;
}

export default function QuickPromoDialog({ open, onClose, test }: QuickPromoDialogProps) {
  const { t } = useTranslation('test');
  const { t: tc } = useTranslation('common');
  const toast = useToast();

  const [durationMinutes, setDurationMinutes] = useState(45);
  const [maxUses, setMaxUses] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [promoCode, setPromoCode] = useState<string | null>(null);
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleGenerate = async () => {
    setIsPending(true);
    setError(null);
    try {
      // 1. Create a quick assignment (open — no group, no specific students)
      const assignResp = await assignmentApi.createAssignment({
        testHistoryId: test.id,
        titleTranslations: test.titleTranslations || undefined,
        title: test.title,
        durationMinutes,
        maxAttempts: 1,
        showResults: true,
        shuffleQuestions: true,
        preventCopyPaste: false,
        preventTabSwitch: false,
      });
      const assignmentId = assignResp.data.data!.id;

      // 2. Generate promo code for the assignment
      const promoResp = await assignmentApi.generatePromoCode(assignmentId, {
        maxUses: maxUses ? parseInt(maxUses, 10) : undefined,
        expiresAt: expiresAt || undefined,
      });
      setPromoCode(promoResp.data.data!.code);
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg || tc('error'));
    } finally {
      setIsPending(false);
    }
  };

  const handleCopy = () => {
    if (promoCode) {
      navigator.clipboard.writeText(promoCode);
      toast.success(t('quickPromo.copied'));
    }
  };

  const handleClose = () => {
    setPromoCode(null);
    setError(null);
    setDurationMinutes(45);
    setMaxUses('');
    setExpiresAt('');
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth>
      <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <VpnKeyIcon color="primary" />
        {t('quickPromo.title')}
      </DialogTitle>
      <DialogContent>
        {!promoCode ? (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              {t('quickPromo.description')}
            </Typography>
            <TextField
              label={t('quickPromo.duration')}
              type="number"
              size="small"
              fullWidth
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(Number(e.target.value))}
              inputProps={{ min: 1, max: 480 }}
            />
            <TextField
              label={t('quickPromo.maxUses')}
              type="number"
              size="small"
              fullWidth
              value={maxUses}
              onChange={(e) => setMaxUses(e.target.value)}
              placeholder={t('quickPromo.unlimited')}
              inputProps={{ min: 1 }}
            />
            <TextField
              label={t('quickPromo.expiresAt')}
              type="datetime-local"
              size="small"
              fullWidth
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            {error && <Alert severity="error">{error}</Alert>}
          </Box>
        ) : (
          <Box sx={{ textAlign: 'center', py: 2 }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {t('quickPromo.success')}
            </Typography>
            <Box sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 1,
              p: 2,
              borderRadius: 2,
              bgcolor: 'action.hover',
              mb: 2,
            }}>
              <Typography
                variant="h4"
                sx={{ fontFamily: 'monospace', fontWeight: 700, letterSpacing: 4, color: 'primary.main' }}
              >
                {promoCode}
              </Typography>
              <Tooltip title={t('quickPromo.copy')}>
                <IconButton onClick={handleCopy} color="primary">
                  <ContentCopyIcon />
                </IconButton>
              </Tooltip>
            </Box>
            <Divider sx={{ my: 1 }} />
            <Typography variant="caption" color="text.secondary">
              {t('quickPromo.shareHint')}
            </Typography>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>{tc('close')}</Button>
        {!promoCode && (
          <Button
            variant="contained"
            onClick={handleGenerate}
            disabled={isPending}
            startIcon={isPending ? <CircularProgress size={18} /> : <VpnKeyIcon />}
          >
            {t('quickPromo.generate')}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
