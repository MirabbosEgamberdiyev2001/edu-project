import { useTranslation } from 'react-i18next';
import { Box, TextField, MenuItem } from '@mui/material';
import type { HeaderConfig } from '@/types/test';
import MultiLangInput from './MultiLangInput';

interface HeaderConfigFormProps {
  value: HeaderConfig;
  onChange: (value: HeaderConfig) => void;
}

const LOCALE_TO_LANG: Record<string, string> = {
  uzl: 'uz',
  uzc: 'uz',
  en: 'en',
  ru: 'ru',
};

export default function HeaderConfigForm({ value, onChange }: HeaderConfigFormProps) {
  const { t, i18n } = useTranslation('test');
  const dateLang = LOCALE_TO_LANG[i18n.language] || 'uz';

  const handleChange = (field: keyof HeaderConfig, val: unknown) => {
    onChange({ ...value, [field]: val });
  };

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <MultiLangInput
        label={t('form.schoolName')}
        value={value.schoolNameTranslations || {}}
        onChange={(v) => handleChange('schoolNameTranslations', v)}
      />
      <TextField
        select
        label={t('form.className')}
        value={value.className || ''}
        onChange={(e) => handleChange('className', e.target.value)}
        size="small"
        fullWidth
      >
        <MenuItem value="">{t('form.selectClass')}</MenuItem>
        {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map((grade) => (
          <MenuItem key={grade} value={String(grade)}>
            {grade}-{t('form.classLabel')}
          </MenuItem>
        ))}
      </TextField>
      <MultiLangInput
        label={t('form.teacherName')}
        value={value.teacherNameTranslations || {}}
        onChange={(v) => handleChange('teacherNameTranslations', v)}
      />
      <TextField
        type="date"
        label={t('form.date')}
        value={value.date || ''}
        onChange={(e) => handleChange('date', e.target.value)}
        size="small"
        fullWidth
        InputLabelProps={{ shrink: true }}
        inputProps={{ lang: dateLang }}
      />
    </Box>
  );
}
