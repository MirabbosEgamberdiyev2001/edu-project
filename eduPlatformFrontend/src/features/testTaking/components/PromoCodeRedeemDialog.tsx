import { useState } from 'react';
import {
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Typography,
  CircularProgress,
} from '@mui/material';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';
import { useTranslation } from 'react-i18next';
import { useRedeemPromoCode } from '../hooks/usePromoCodeRedeem';

interface PromoCodeRedeemDialogProps {
  open: boolean;
  onClose: () => void;
}

export default function PromoCodeRedeemDialog({ open, onClose }: PromoCodeRedeemDialogProps) {
  const { t } = useTranslation('testTaking');
  const { t: tc } = useTranslation('common');
  const [code, setCode] = useState('');
  const redeemPromoCode = useRedeemPromoCode();

  const errorMessage = redeemPromoCode.isError
    ? (redeemPromoCode.error as AxiosError<ApiError>)?.response?.data?.message ?? tc('error')
    : null;

  const handleSubmit = () => {
    if (code.length !== 8) return;
    redeemPromoCode.mutate(
      { code: code.toUpperCase() },
      {
        onSuccess: () => {
          setCode('');
          onClose();
        },
      },
    );
  };

  const handleClose = () => {
    setCode('');
    redeemPromoCode.reset();
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('promoCode.title')}</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {t('promoCode.description')}
        </Typography>
        {errorMessage && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {errorMessage}
          </Alert>
        )}
        <TextField
          autoFocus
          fullWidth
          placeholder={t('promoCode.placeholder')}
          value={code}
          onChange={(e) => setCode(e.target.value.toUpperCase().slice(0, 8))}
          inputProps={{
            maxLength: 8,
            style: {
              fontFamily: 'monospace',
              fontSize: '1.2rem',
              fontWeight: 700,
              letterSpacing: 4,
              textAlign: 'center',
            },
          }}
          sx={{ mt: 1 }}
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={handleClose}>{tc('cancel')}</Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={redeemPromoCode.isPending || code.length !== 8}
        >
          {redeemPromoCode.isPending ? (
            <CircularProgress size={20} />
          ) : (
            t('promoCode.submit')
          )}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
