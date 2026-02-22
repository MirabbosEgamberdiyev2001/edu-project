import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  CircularProgress,
  Grid,
  Button,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Chip,
  LinearProgress,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import DownloadIcon from '@mui/icons-material/Download';
import { useTranslation } from 'react-i18next';
import { useGroupStatistics, useExportGroupStatistics } from '../hooks/useGroupStatistics';
import StatSummaryCards from '../components/StatSummaryCards';

export default function GroupAnalyticsPage() {
  const { groupId } = useParams<{ groupId: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('analytics');
  const { data, isLoading } = useGroupStatistics(groupId!);
  const { exportGroupStats } = useExportGroupStatistics();

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!data) return null;

  const stats = [
    { label: t('members'), value: data.memberCount, color: '#1976d2' },
    { label: t('avgScore'), value: Math.round(data.averageScore), color: '#2e7d32', suffix: '%' },
    { label: t('completionRate'), value: Math.round(data.completionRate), color: '#ed6c02', suffix: '%' },
    { label: t('totalAssignments'), value: data.totalAssignments, color: '#9c27b0' },
  ];

  return (
    <Box>
      <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/groups')} sx={{ mb: 2 }}>
        {t('backToGroups')}
      </Button>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>{data.groupName}</Typography>
          <Typography variant="body2" color="text.secondary">{t('groupAnalytics')}</Typography>
        </Box>
        <Button startIcon={<DownloadIcon />} onClick={() => exportGroupStats(groupId!)} variant="outlined" size="small">
          {t('exportPdf')}
        </Button>
      </Box>

      <Box sx={{ mb: 3 }}>
        <StatSummaryCards stats={stats} />
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 2 }}>{t('scoreDistribution')}</Typography>
            {data.scoreDistribution.map((dist) => (
              <Box key={dist.range} sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                <Typography variant="body2" sx={{ minWidth: 60 }}>{dist.range}</Typography>
                <LinearProgress
                  variant="determinate"
                  value={data.memberCount > 0 ? (dist.count / data.memberCount) * 100 : 0}
                  sx={{ flex: 1, height: 8, borderRadius: 1 }}
                />
                <Typography variant="caption" sx={{ minWidth: 24 }}>{dist.count}</Typography>
              </Box>
            ))}
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 2 }}>{t('subjectPerformance')}</Typography>
            {data.subjectPerformance.map((sp) => (
              <Box key={sp.subjectId} sx={{ mb: 1.5 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                  <Typography variant="body2">{sp.subjectName}</Typography>
                  <Typography variant="body2" fontWeight={500}>{Math.round(sp.averageScore)}%</Typography>
                </Box>
                <LinearProgress variant="determinate" value={sp.averageScore} sx={{ height: 6, borderRadius: 1 }} />
              </Box>
            ))}
          </Paper>
        </Grid>

        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 2 }}>{t('studentRankings')}</Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('rank')}</TableCell>
                  <TableCell>{t('student')}</TableCell>
                  <TableCell align="center">{t('avgScore')}</TableCell>
                  <TableCell align="center">{t('attempts')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.studentRankings.map((student) => (
                  <TableRow key={student.studentId}>
                    <TableCell>#{student.rank}</TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight={500}>
                        {student.firstName} {student.lastName}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      <Chip
                        label={`${Math.round(student.averageScore)}%`}
                        size="small"
                        color={student.averageScore >= 70 ? 'success' : student.averageScore >= 40 ? 'warning' : 'error'}
                      />
                    </TableCell>
                    <TableCell align="center">{student.attemptCount}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}
