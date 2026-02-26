import { Box, Typography, Button } from '@mui/material';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import { useTranslation } from 'react-i18next';

interface ErrorStateProps {
  title?: string;
  description?: string;
  onRetry?: () => void;
  retryLabel?: string;
}

export default function ErrorState({ title, description, onRetry, retryLabel }: ErrorStateProps) {
  const { t } = useTranslation('common');

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: 300,
        gap: 2,
        p: 4,
        textAlign: 'center',
      }}
    >
      <ErrorOutlineIcon sx={{ fontSize: 48, color: 'error.main' }} />
      <Typography variant="h6" fontWeight={600}>
        {title ?? t('error')}
      </Typography>
      {description && (
        <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 400 }}>
          {description}
        </Typography>
      )}
      {onRetry && (
        <Button variant="outlined" onClick={onRetry}>
          {retryLabel ?? t('back')}
        </Button>
      )}
    </Box>
  );
}
