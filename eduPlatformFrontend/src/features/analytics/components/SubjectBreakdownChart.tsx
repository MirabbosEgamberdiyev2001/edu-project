import { Box, Typography, LinearProgress } from '@mui/material';
import type { SubjectBreakdownDto } from '@/types/analytics';
import { useTranslation } from 'react-i18next';

interface SubjectBreakdownChartProps {
  data: SubjectBreakdownDto[];
}

const COLORS = ['#1976d2', '#2e7d32', '#ed6c02', '#9c27b0', '#00796b', '#d32f2f'];

export default function SubjectBreakdownChart({ data }: SubjectBreakdownChartProps) {
  const { t } = useTranslation('analytics');

  if (!data || data.length === 0) return null;

  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 2 }}>{t('subjectBreakdown')}</Typography>
      {data.map((subject, i) => (
        <Box key={subject.subjectId} sx={{ mb: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
            <Typography variant="body2" fontWeight={500}>{subject.subjectName}</Typography>
            <Typography variant="body2" color="text.secondary">
              {Math.round(subject.averageScore)}% ({subject.totalAttempts} {t('attempts')})
            </Typography>
          </Box>
          <LinearProgress
            variant="determinate"
            value={subject.averageScore}
            sx={{
              height: 8,
              borderRadius: 1,
              bgcolor: 'grey.200',
              '& .MuiLinearProgress-bar': { bgcolor: COLORS[i % COLORS.length] },
            }}
          />
        </Box>
      ))}
    </Box>
  );
}
