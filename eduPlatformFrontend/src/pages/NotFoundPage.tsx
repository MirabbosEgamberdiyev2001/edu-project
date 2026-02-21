import { Box, Typography, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function NotFoundPage() {
  const { t } = useTranslation('common');
  const navigate = useNavigate();

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        gap: 2,
      }}
    >
      <Typography variant="h1" fontWeight={700} color="primary">
        404
      </Typography>
      <Typography variant="h5">{t('notFound.title')}</Typography>
      <Typography variant="body1" color="text.secondary">
        {t('notFound.description')}
      </Typography>
      <Button variant="contained" onClick={() => navigate('/dashboard')}>
        {t('notFound.goHome')}
      </Button>
    </Box>
  );
}
