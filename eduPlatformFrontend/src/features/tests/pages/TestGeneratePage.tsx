import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Box, Typography, IconButton, Tabs, Tab } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import TestGenerateForm from '../components/TestGenerateForm';
import ManualTestForm from '../components/ManualTestForm';

export default function TestGeneratePage() {
  const { t } = useTranslation('test');
  const navigate = useNavigate();
  const [mode, setMode] = useState(0);

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <IconButton onClick={() => navigate('/tests')}>
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h5" fontWeight={700}>
          {t('generate')}
        </Typography>
      </Box>

      <Tabs
        value={mode}
        onChange={(_, v) => setMode(v)}
        sx={{ mb: 3 }}
      >
        <Tab label={t('mode.auto')} />
        <Tab label={t('mode.manual')} />
      </Tabs>

      {mode === 0 ? <TestGenerateForm /> : <ManualTestForm />}
    </Box>
  );
}
