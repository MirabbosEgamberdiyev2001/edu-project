import { Box, Typography, Tooltip } from '@mui/material';
import type { TrendPointDto } from '@/types/analytics';

interface ScoreTrendChartProps {
  data: TrendPointDto[];
  title: string;
  color?: string;
}

export default function ScoreTrendChart({ data, title, color = '#1976d2' }: ScoreTrendChartProps) {
  if (!data || data.length === 0) return null;

  const maxVal = Math.max(...data.map((d) => d.value), 1);

  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 1 }}>{title}</Typography>
      <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 0.5, height: 100 }}>
        {data.map((point) => (
          <Tooltip
            key={point.date}
            title={`${new Date(point.date).toLocaleDateString()}: ${Math.round(point.value)}%`}
          >
            <Box
              sx={{
                flex: 1,
                height: `${Math.max((point.value / maxVal) * 100, 4)}%`,
                bgcolor: color,
                borderRadius: '4px 4px 0 0',
                opacity: 0.8,
                '&:hover': { opacity: 1 },
              }}
            />
          </Tooltip>
        ))}
      </Box>
    </Box>
  );
}
