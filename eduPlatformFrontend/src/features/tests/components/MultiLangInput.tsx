import { useState } from 'react';
import { Box, TextField, Tabs, Tab } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import { SUPPORTED_LANGUAGES, LANGUAGE_LABELS } from '@/config';
import { toLocaleKey } from '@/utils/i18nUtils';

interface MultiLangInputProps {
  label: string;
  value: Record<string, string>;
  onChange: (value: Record<string, string>) => void;
  placeholder?: string;
  size?: 'small' | 'medium';
  required?: boolean;
  fullWidth?: boolean;
}

export default function MultiLangInput({
  label,
  value,
  onChange,
  placeholder,
  size = 'small',
  required,
  fullWidth = true,
}: MultiLangInputProps) {
  const [langTab, setLangTab] = useState(0);

  const currentFrontendLang = SUPPORTED_LANGUAGES[langTab] || 'uzl';
  const currentKey = toLocaleKey(currentFrontendLang);

  const isLangFilled = (frontLang: string) => {
    const key = toLocaleKey(frontLang);
    return Boolean(value[key]?.trim());
  };

  return (
    <Box>
      <Tabs
        value={langTab}
        onChange={(_, v) => setLangTab(v)}
        sx={{ mb: 1, minHeight: 32, '& .MuiTab-root': { minHeight: 32, py: 0.5 } }}
      >
        {SUPPORTED_LANGUAGES.map((lang) => (
          <Tab
            key={lang}
            label={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                {isLangFilled(lang) ? (
                  <CheckCircleIcon sx={{ fontSize: 12, color: 'success.main' }} />
                ) : (
                  <RadioButtonUncheckedIcon sx={{ fontSize: 12, color: 'text.disabled' }} />
                )}
                {LANGUAGE_LABELS[lang]}
              </Box>
            }
            sx={{ textTransform: 'none', minWidth: 0, px: 1, fontSize: '0.8rem' }}
          />
        ))}
      </Tabs>
      <TextField
        label={`${label} (${LANGUAGE_LABELS[currentFrontendLang]})`}
        placeholder={placeholder}
        value={value[currentKey] || ''}
        onChange={(e) => onChange({ ...value, [currentKey]: e.target.value })}
        size={size}
        fullWidth={fullWidth}
        required={required && currentFrontendLang === 'uzl'}
      />
    </Box>
  );
}
