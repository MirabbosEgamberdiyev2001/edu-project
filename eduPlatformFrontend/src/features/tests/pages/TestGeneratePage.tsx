import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Tabs, Tab,
  Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button,
} from '@mui/material';
import TestGenerateForm from '../components/TestGenerateForm';
import ManualTestForm from '../components/ManualTestForm';
import { PageShell } from '@/components/ui';

export default function TestGeneratePage() {
  const { t } = useTranslation('test');
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
    <PageShell
      title={t('generate')}
      breadcrumbs={[
        { label: t('common:tests'), to: '/tests' },
        { label: t('generate') },
      ]}
    >
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
    </PageShell>
  );
}
