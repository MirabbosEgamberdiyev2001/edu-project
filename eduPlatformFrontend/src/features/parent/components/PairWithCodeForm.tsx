import { useState } from 'react';
import {
  Box,
  TextField,
  Button,
  Paper,
  Typography,
  CircularProgress,
} from '@mui/material';
import LinkIcon from '@mui/icons-material/Link';
import { useTranslation } from 'react-i18next';

interface PairWithCodeFormProps {
  onSubmit: (code: string) => void;
  isPending: boolean;
}

export default function PairWithCodeForm({ onSubmit, isPending }: PairWithCodeFormProps) {
  const { t } = useTranslation('parent');
  const [code, setCode] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = code.trim();
    if (trimmed.length > 0) {
      onSubmit(trimmed);
      setCode('');
    }
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>
        {t('enterCode')}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        {t('scanQR')}
      </Typography>
      <Box
        component="form"
        onSubmit={handleSubmit}
        sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start' }}
      >
        <TextField
          value={code}
          onChange={(e) => setCode(e.target.value.replace(/[^a-zA-Z0-9]/g, '').slice(0, 8))}
          placeholder="XXXXXXXX"
          size="small"
          inputProps={{
            maxLength: 8,
            style: { textAlign: 'center', letterSpacing: 4, fontWeight: 600, fontSize: '1.1rem' },
          }}
          sx={{ width: 180 }}
        />
        <Button
          type="submit"
          variant="contained"
          disabled={isPending || code.trim().length === 0}
          startIcon={isPending ? <CircularProgress size={18} /> : <LinkIcon />}
        >
          {t('pairWithCode')}
        </Button>
      </Box>
    </Paper>
  );
}
