import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Box, Typography, IconButton, Tabs, Tab,
  Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import TestGenerateForm from '../components/TestGenerateForm';
import ManualTestForm from '../components/ManualTestForm';
import PageBreadcrumbs from '@/components/PageBreadcrumbs';

export default function TestGeneratePage() {
  const { t } = useTranslation('test');
  const navigate = useNavigate();
  const [mode, setMode] = useState(0);
  const [pendingMode, setPendingMode] = useState<number | null>(null);

  const handleModeChange = useCallback((_: React.SyntheticEvent, newMode: number) => {
    if (newMode !== mode) {
      setPendingMode(newMode);
    }
  }, [mode]);

  const confirmModeSwitch = useCallback(() => {
    if (pendingMode !== null) {
      setMode(pendingMode);
      setPendingMode(null);
    }
  }, [pendingMode]);

  const cancelModeSwitch = useCallback(() => {
    setPendingMode(null);
  }, []);

  return (
    <Box>
      <PageBreadcrumbs items={[
        { label: t('common:tests'), href: '/tests' },
        { label: t('generate') },
      ]} />

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <IconButton onClick={() => navigate('/tests')} aria-label={t('back')}>
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h5" fontWeight={700}>
          {t('generate')}
        </Typography>
      </Box>

      <Tabs
        value={mode}
        onChange={handleModeChange}
        sx={{ mb: 3 }}
      >
        <Tab label={t('mode.auto')} />
        <Tab label={t('mode.manual')} />
      </Tabs>

      {mode === 0 ? <TestGenerateForm /> : <ManualTestForm />}

      <Dialog open={pendingMode !== null} onClose={cancelModeSwitch}>
        <DialogTitle>{t('modeSwitchWarning.title')}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {t('modeSwitchWarning.message')}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={cancelModeSwitch}>{t('modeSwitchWarning.cancel')}</Button>
          <Button onClick={confirmModeSwitch} color="warning" variant="contained">
            {t('modeSwitchWarning.confirm')}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
