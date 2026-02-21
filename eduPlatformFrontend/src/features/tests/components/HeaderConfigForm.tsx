import { useTranslation } from 'react-i18next';
import { Box, TextField } from '@mui/material';
import type { HeaderConfig } from '@/types/test';

interface HeaderConfigFormProps {
  value: HeaderConfig;
  onChange: (value: HeaderConfig) => void;
}

export default function HeaderConfigForm({ value, onChange }: HeaderConfigFormProps) {
  const { t } = useTranslation('test');

  const handleChange = (field: keyof HeaderConfig, val: string) => {
    onChange({ ...value, [field]: val });
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <TextField
        label={t('form.schoolName')}
        value={value.schoolName || ''}
        onChange={(e) => handleChange('schoolName', e.target.value)}
        size="small"
        fullWidth
      />
      <TextField
        label={t('form.className')}
        value={value.className || ''}
        onChange={(e) => handleChange('className', e.target.value)}
        size="small"
        fullWidth
      />
      <TextField
        label={t('form.teacherName')}
        value={value.teacherName || ''}
        onChange={(e) => handleChange('teacherName', e.target.value)}
        size="small"
        fullWidth
      />
      <TextField
        label={t('form.date')}
        value={value.date || ''}
        onChange={(e) => handleChange('date', e.target.value)}
        size="small"
        fullWidth
        placeholder="2025-01-01"
      />
    </Box>
  );
}
