import { Box, Typography, LinearProgress } from '@mui/material';
import type { UsageDto } from '@/types/subscription';

interface UsageMeterProps {
  usage: UsageDto[];
}

export default function UsageMeter({ usage }: UsageMeterProps) {
  return (
    <Box>
      {usage.map((item) => (
        <Box key={item.usageType} sx={{ mb: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
            <Typography variant="body2" fontWeight={500}>{item.usageType}</Typography>
            <Typography variant="body2" color="text.secondary">
              {item.count}/{item.limit}
            </Typography>
          </Box>
          <LinearProgress
            variant="determinate"
            value={Math.min(item.percentageUsed, 100)}
            color={item.percentageUsed >= 90 ? 'error' : item.percentageUsed >= 70 ? 'warning' : 'primary'}
            sx={{ height: 8, borderRadius: 1 }}
          />
        </Box>
      ))}
    </Box>
  );
}
