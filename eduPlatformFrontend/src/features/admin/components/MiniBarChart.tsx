import { Box, Typography, Tooltip } from '@mui/material';

interface MiniBarChartProps {
  data: { label: string; value: number; color: string }[];
  title: string;
}

export default function MiniBarChart({ data, title }: MiniBarChartProps) {
  const maxVal = Math.max(...data.map((d) => d.value), 1);

  return (
    <Box>
      <Typography variant="subtitle2" gutterBottom>
        {title}
      </Typography>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
        {data.map((item) => (
          <Box key={item.label} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="caption" sx={{ minWidth: 100, color: 'text.secondary' }} noWrap>
              {item.label}
            </Typography>
            <Box sx={{ flex: 1, height: 20, bgcolor: 'action.hover', borderRadius: 1, overflow: 'hidden' }}>
              <Tooltip title={item.value.toLocaleString()}>
                <Box
                  sx={{
                    height: '100%',
                    width: `${(item.value / maxVal) * 100}%`,
                    bgcolor: item.color,
                    borderRadius: 1,
                    minWidth: item.value > 0 ? 4 : 0,
                    transition: 'width 0.3s ease',
                  }}
                />
              </Tooltip>
            </Box>
            <Typography variant="caption" sx={{ minWidth: 40, textAlign: 'right' }}>
              {item.value.toLocaleString()}
            </Typography>
          </Box>
        ))}
      </Box>
    </Box>
  );
}
