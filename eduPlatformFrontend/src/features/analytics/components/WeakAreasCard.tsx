import { Box, Typography, Chip, Paper, LinearProgress } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { WeakAreaDto, StrongAreaDto } from '@/types/analytics';

interface WeakAreasCardProps {
  weakAreas: WeakAreaDto[];
  strongAreas: StrongAreaDto[];
}

export default function WeakAreasCard({ weakAreas, strongAreas }: WeakAreasCardProps) {
  const { t } = useTranslation('analytics');

  return (
    <Box>
      {weakAreas.length > 0 && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" color="error.main" sx={{ mb: 1 }}>
            {t('weakAreas')}
          </Typography>
          {weakAreas.map((area) => (
            <Paper key={area.topicId} variant="outlined" sx={{ p: 1.5, mb: 1 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                <Typography variant="body2" fontWeight={500}>{area.topicName}</Typography>
                <Chip label={`${Math.round(area.averageScore)}%`} size="small" color="error" />
              </Box>
              <Typography variant="caption" color="text.secondary">{area.subjectName}</Typography>
              <LinearProgress
                variant="determinate"
                value={area.averageScore}
                color="error"
                sx={{ mt: 0.5, height: 4, borderRadius: 1 }}
              />
            </Paper>
          ))}
        </Box>
      )}

      {strongAreas.length > 0 && (
        <Box>
          <Typography variant="subtitle2" color="success.main" sx={{ mb: 1 }}>
            {t('strongAreas')}
          </Typography>
          {strongAreas.map((area) => (
            <Paper key={area.topicId} variant="outlined" sx={{ p: 1.5, mb: 1 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                <Typography variant="body2" fontWeight={500}>{area.topicName}</Typography>
                <Chip label={`${Math.round(area.averageScore)}%`} size="small" color="success" />
              </Box>
              <Typography variant="caption" color="text.secondary">{area.subjectName}</Typography>
              <LinearProgress
                variant="determinate"
                value={area.averageScore}
                color="success"
                sx={{ mt: 0.5, height: 4, borderRadius: 1 }}
              />
            </Paper>
          ))}
        </Box>
      )}
    </Box>
  );
}
