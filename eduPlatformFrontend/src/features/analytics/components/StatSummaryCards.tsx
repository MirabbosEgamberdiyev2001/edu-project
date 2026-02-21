import { Grid, Paper, Typography, Box } from '@mui/material';

interface StatItem {
  label: string;
  value: number | string;
  color?: string;
  suffix?: string;
}

interface StatSummaryCardsProps {
  stats: StatItem[];
}

export default function StatSummaryCards({ stats }: StatSummaryCardsProps) {
  return (
    <Grid container spacing={2}>
      {stats.map((stat) => (
        <Grid item xs={6} sm={4} md={3} key={stat.label}>
          <Paper sx={{ p: 2, textAlign: 'center' }}>
            <Typography variant="h4" fontWeight={700} sx={{ color: stat.color || 'primary.main' }}>
              {stat.value}{stat.suffix || ''}
            </Typography>
            <Typography variant="caption" color="text.secondary">{stat.label}</Typography>
          </Paper>
        </Grid>
      ))}
    </Grid>
  );
}
