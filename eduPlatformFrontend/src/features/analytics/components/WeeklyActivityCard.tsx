import { Box, Typography, Tooltip } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { WeeklyActivityDto } from '@/types/analytics';

interface WeeklyActivityCardProps {
  data: WeeklyActivityDto[];
}

export default function WeeklyActivityCard({ data }: WeeklyActivityCardProps) {
  const { t } = useTranslation('analytics');

  if (!data || data.length === 0) return null;

  const maxCount = Math.max(...data.map((d) => d.attemptCount), 1);

  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 1 }}>{t('weeklyActivity')}</Typography>
      <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
        {data.map((day) => {
          const intensity = day.attemptCount / maxCount;
          return (
            <Tooltip key={day.date} title={`${new Date(day.date).toLocaleDateString()}: ${day.attemptCount} ${t('attempts')}`}>
              <Box
                sx={{
                  width: 16,
                  height: 16,
                  borderRadius: 0.5,
                  bgcolor: day.attemptCount === 0
                    ? 'grey.200'
                    : `rgba(25, 118, 210, ${Math.max(0.2, intensity)})`,
                }}
              />
            </Tooltip>
          );
        })}
      </Box>
    </Box>
  );
}
