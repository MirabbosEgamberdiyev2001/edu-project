import { useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  TextField,
  IconButton,
  Tooltip,
  Chip,
  CircularProgress,
  Grid,
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import LinkIcon from '@mui/icons-material/Link';
import LinkOffIcon from '@mui/icons-material/LinkOff';
import { useTranslation } from 'react-i18next';
import { usePromoCode, useGeneratePromoCode, useRevokePromoCode } from '../hooks/usePromoCode';
import { useToast } from '@/hooks/useToast';

interface PromoCodeSectionProps {
  assignmentId: string;
}

export default function PromoCodeSection({ assignmentId }: PromoCodeSectionProps) {
  const { t } = useTranslation('assignment');
  const toast = useToast();
  const { data: promoCode, isLoading } = usePromoCode(assignmentId);
  const generatePromoCode = useGeneratePromoCode(assignmentId);
  const revokePromoCode = useRevokePromoCode(assignmentId);

  const [maxUses, setMaxUses] = useState<string>('');
  const [expiresAt, setExpiresAt] = useState<string>('');

  const handleGenerate = () => {
    generatePromoCode.mutate({
      maxUses: maxUses ? parseInt(maxUses, 10) : undefined,
      expiresAt: expiresAt || undefined,
    });
    setMaxUses('');
    setExpiresAt('');
  };

  const handleCopy = () => {
    if (promoCode?.code) {
      navigator.clipboard.writeText(promoCode.code);
      toast.success(t('promoCodeCopied'));
    }
  };

  if (isLoading) {
    return (
      <Paper sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
          <CircularProgress size={24} />
        </Box>
      </Paper>
    );
  }

  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      <Typography variant="h6" sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
        <LinkIcon color="primary" />
        {t('promoCode')}
      </Typography>

      {promoCode ? (
        <Box>
          <Box sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 2,
            p: 2,
            borderRadius: 2,
            bgcolor: 'action.hover',
            mb: 2,
          }}>
            <Typography
              variant="h4"
              sx={{ fontFamily: 'monospace', fontWeight: 700, letterSpacing: 4 }}
            >
              {promoCode.code}
            </Typography>
            <Tooltip title={t('promoCodeCopied')}>
              <IconButton onClick={handleCopy} color="primary">
                <ContentCopyIcon />
              </IconButton>
            </Tooltip>
          </Box>

          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
            <Chip
              label={t('promoCodeActive')}
              color="success"
              size="small"
            />
            <Chip
              label={
                promoCode.maxUses
                  ? t('promoCodeUsage', { current: promoCode.currentUses, max: promoCode.maxUses })
                  : t('promoCodeUsageUnlimited', { current: promoCode.currentUses })
              }
              variant="outlined"
              size="small"
            />
            {promoCode.expiresAt && (
              <Chip
                label={`${t('promoCodeExpiry')}: ${new Date(promoCode.expiresAt).toLocaleString()}`}
                variant="outlined"
                size="small"
              />
            )}
          </Box>

          <Button
            variant="outlined"
            color="error"
            size="small"
            startIcon={<LinkOffIcon />}
            onClick={() => revokePromoCode.mutate()}
            disabled={revokePromoCode.isPending}
          >
            {revokePromoCode.isPending ? <CircularProgress size={18} /> : t('revokePromoCode')}
          </Button>
        </Box>
      ) : (
        <Box>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {t('noPromoCode')}
          </Typography>

          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                label={t('promoCodeMaxUses')}
                type="number"
                size="small"
                fullWidth
                value={maxUses}
                onChange={(e) => setMaxUses(e.target.value)}
                placeholder={t('promoCodeUnlimited')}
                inputProps={{ min: 1 }}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                label={t('promoCodeExpiry')}
                type="datetime-local"
                size="small"
                fullWidth
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
          </Grid>

          <Button
            variant="contained"
            startIcon={<LinkIcon />}
            onClick={handleGenerate}
            disabled={generatePromoCode.isPending}
          >
            {generatePromoCode.isPending ? <CircularProgress size={20} /> : t('generatePromoCode')}
          </Button>
        </Box>
      )}
    </Paper>
  );
}
