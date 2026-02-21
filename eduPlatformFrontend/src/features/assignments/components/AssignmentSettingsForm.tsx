import { Box, FormControlLabel, Switch, TextField, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { UpdateAssignmentRequest } from '@/types/assignment';

interface AssignmentSettingsFormProps {
  settings: UpdateAssignmentRequest;
  onChange: (settings: UpdateAssignmentRequest) => void;
  disabled?: boolean;
}

export default function AssignmentSettingsForm({ settings, onChange, disabled }: AssignmentSettingsFormProps) {
  const { t } = useTranslation('assignment');

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <TextField
        label={t('durationMinutes')}
        type="number"
        value={settings.durationMinutes || ''}
        onChange={(e) => onChange({ ...settings, durationMinutes: Number(e.target.value) })}
        fullWidth
        disabled={disabled}
        inputProps={{ min: 1, max: 600 }}
      />
      <TextField
        label={t('maxAttempts')}
        type="number"
        value={settings.maxAttempts || 1}
        onChange={(e) => onChange({ ...settings, maxAttempts: Number(e.target.value) })}
        fullWidth
        disabled={disabled}
        inputProps={{ min: 1, max: 10 }}
      />
      <Typography variant="subtitle2" sx={{ mt: 1 }}>{t('antiCheat')}</Typography>
      <FormControlLabel
        control={<Switch checked={settings.shuffleQuestions ?? false} onChange={(e) => onChange({ ...settings, shuffleQuestions: e.target.checked })} disabled={disabled} />}
        label={t('shuffleQuestions')}
      />
      <FormControlLabel
        control={<Switch checked={settings.shuffleOptions ?? false} onChange={(e) => onChange({ ...settings, shuffleOptions: e.target.checked })} disabled={disabled} />}
        label={t('shuffleOptions')}
      />
      <FormControlLabel
        control={<Switch checked={settings.preventTabSwitch ?? false} onChange={(e) => onChange({ ...settings, preventTabSwitch: e.target.checked })} disabled={disabled} />}
        label={t('preventTabSwitch')}
      />
      <FormControlLabel
        control={<Switch checked={settings.preventCopyPaste ?? false} onChange={(e) => onChange({ ...settings, preventCopyPaste: e.target.checked })} disabled={disabled} />}
        label={t('preventCopyPaste')}
      />
    </Box>
  );
}
