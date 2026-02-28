import { Box, Typography, LinearProgress, Chip } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { SubjectScoreDto } from '@/types/parent';

interface SubjectScoreBarProps {
  subject: SubjectScoreDto;
}

const LEVEL_COLOR: Record<string, 'success' | 'primary' | 'warning' | 'error'> = {
  EXCELLENT: 'success',
  GOOD: 'primary',
  ATTENTION: 'warning',
  CRITICAL: 'error',
};

const PROGRESS_COLOR: Record<string, 'success' | 'info' | 'warning' | 'error'> = {
  EXCELLENT: 'success',
  GOOD: 'info',
  ATTENTION: 'warning',
  CRITICAL: 'error',
};

export default function SubjectScoreBar({ subject }: SubjectScoreBarProps) {
  const { t } = useTranslation('parent');
  const percentage = Math.round(Number(subject.averageScore));
  const levelColor = LEVEL_COLOR[subject.level] ?? 'primary';
  const progressColor = PROGRESS_COLOR[subject.level] ?? 'info';

  return (
    <Box sx={{ mb: 2.5 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.75 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 0 }}>
          <Typography variant="body2" fontWeight={600} noWrap>
            {subject.subjectName}
          </Typography>
          <Chip
            label={t(`level.${subject.level}`)}
            size="small"
            color={levelColor}
            variant="outlined"
            sx={{ fontSize: '0.65rem', height: 18, flexShrink: 0 }}
          />
        </Box>
        <Typography variant="body2" fontWeight={700} sx={{ ml: 1, flexShrink: 0 }}>
          {percentage}%
        </Typography>
      </Box>
      <LinearProgress
        variant="determinate"
        value={percentage}
        color={progressColor}
        sx={{ height: 8, borderRadius: 4, mb: 0.5 }}
      />
      <Typography variant="caption" color="text.disabled">
        {subject.attemptCount} {t('attempts')}
      </Typography>
    </Box>
  );
}
