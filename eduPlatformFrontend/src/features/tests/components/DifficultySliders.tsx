import { useTranslation } from 'react-i18next';
import { Box, Slider, Typography } from '@mui/material';
import type { DifficultyDistribution } from '@/types/test';

interface DifficultySlidersProps {
  value: DifficultyDistribution;
  onChange: (value: DifficultyDistribution) => void;
}

export default function DifficultySliders({ value, onChange }: DifficultySlidersProps) {
  const { t } = useTranslation('test');

  const handleChange = (field: keyof DifficultyDistribution, newVal: number) => {
    const remaining = 100 - newVal;
    const otherFields = (['easy', 'medium', 'hard'] as const).filter(f => f !== field);
    const otherSum = otherFields.reduce((s, f) => s + value[f], 0);

    const updated = { ...value, [field]: newVal };
    if (otherSum === 0) {
      updated[otherFields[0]] = remaining;
      updated[otherFields[1]] = 0;
    } else {
      for (const f of otherFields) {
        updated[f] = Math.round((value[f] / otherSum) * remaining);
      }
      const diff = 100 - updated.easy - updated.medium - updated.hard;
      updated[otherFields[0]] += diff;
    }
    onChange(updated);
  };

  return (
    <Box>
      <Typography variant="subtitle2" gutterBottom>{t('form.difficulty')}</Typography>
      {([
        { key: 'easy' as const, label: t('form.easy'), color: '#4caf50' },
        { key: 'medium' as const, label: t('form.medium'), color: '#ff9800' },
        { key: 'hard' as const, label: t('form.hard'), color: '#f44336' },
      ]).map(({ key, label, color }) => (
        <Box key={key} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="body2" sx={{ minWidth: 60 }}>{label}</Typography>
          <Slider
            value={value[key]}
            onChange={(_, v) => handleChange(key, v as number)}
            min={0}
            max={100}
            sx={{ color, flex: 1 }}
          />
          <Typography variant="body2" sx={{ minWidth: 40, textAlign: 'right' }}>
            {value[key]}%
          </Typography>
        </Box>
      ))}
    </Box>
  );
}
