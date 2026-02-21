import { Box, Typography, Tooltip } from '@mui/material';
import type { TrendPoint } from '@/types/admin';

interface TrendChartProps {
  data: TrendPoint[];
  title: string;
  color: string;
}

export default function TrendChart({ data, title, color }: TrendChartProps) {
  if (!data || data.length === 0) return null;

  const maxVal = Math.max(...data.map((d) => d.count), 1);
  const total = data.reduce((s, d) => s + d.count, 0);

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', mb: 1 }}>
        <Typography variant="subtitle2">{title}</Typography>
        <Typography variant="h6" fontWeight={700} color={color}>
          {total.toLocaleString()}
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 0.5, height: 80 }}>
        {data.map((point) => (
          <Tooltip
            key={point.date}
            title={`${new Date(point.date).toLocaleDateString()}: ${point.count}`}
          >
            <Box
              sx={{
                flex: 1,
                height: `${Math.max((point.count / maxVal) * 100, 4)}%`,
                bgcolor: color,
                borderRadius: '4px 4px 0 0',
                opacity: 0.8,
                transition: 'height 0.3s ease',
                '&:hover': { opacity: 1 },
              }}
            />
          </Tooltip>
        ))}
      </Box>
    </Box>
  );
}
