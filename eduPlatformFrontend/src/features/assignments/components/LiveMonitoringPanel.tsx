import {
  Box,
  Typography,
  Paper,
  LinearProgress,
  Chip,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
} from '@mui/material';
import WarningIcon from '@mui/icons-material/Warning';
import { useTranslation } from 'react-i18next';
import type { LiveMonitoringDto } from '@/types/assignment';

interface LiveMonitoringPanelProps {
  data: LiveMonitoringDto;
}

export default function LiveMonitoringPanel({ data }: LiveMonitoringPanelProps) {
  const { t } = useTranslation('assignment');

  return (
    <Box>
      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <Paper sx={{ p: 2, flex: 1, minWidth: 120, textAlign: 'center' }}>
          <Typography variant="h4" color="info.main">{data.activeStudents}</Typography>
          <Typography variant="caption">{t('liveActive')}</Typography>
        </Paper>
        <Paper sx={{ p: 2, flex: 1, minWidth: 120, textAlign: 'center' }}>
          <Typography variant="h4" color="success.main">{data.completedStudents}</Typography>
          <Typography variant="caption">{t('liveCompleted')}</Typography>
        </Paper>
        <Paper sx={{ p: 2, flex: 1, minWidth: 120, textAlign: 'center' }}>
          <Typography variant="h4" color="text.secondary">{data.notStartedStudents}</Typography>
          <Typography variant="caption">{t('liveNotStarted')}</Typography>
        </Paper>
      </Box>

      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>{t('student')}</TableCell>
            <TableCell>{t('status')}</TableCell>
            <TableCell>{t('progress')}</TableCell>
            <TableCell>{t('tabSwitches')}</TableCell>
            <TableCell>{t('timeRemaining')}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {data.students.map((student) => {
            const progress = student.totalQuestions > 0
              ? (student.answeredQuestions / student.totalQuestions) * 100
              : 0;

            return (
              <TableRow key={student.studentId}>
                <TableCell>
                  {student.firstName} {student.lastName}
                </TableCell>
                <TableCell>
                  <Chip
                    label={student.status}
                    size="small"
                    color={student.status === 'IN_PROGRESS' ? 'info' : student.status === 'SUBMITTED' ? 'success' : 'default'}
                  />
                </TableCell>
                <TableCell sx={{ minWidth: 150 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <LinearProgress variant="determinate" value={progress} sx={{ flex: 1, borderRadius: 1 }} />
                    <Typography variant="caption">
                      {student.answeredQuestions}/{student.totalQuestions}
                    </Typography>
                  </Box>
                </TableCell>
                <TableCell>
                  {student.tabSwitches > 0 && (
                    <Chip
                      icon={<WarningIcon />}
                      label={student.tabSwitches}
                      size="small"
                      color="warning"
                    />
                  )}
                </TableCell>
                <TableCell>
                  {student.timeRemaining != null ? `${Math.floor(student.timeRemaining / 60)}:${String(student.timeRemaining % 60).padStart(2, '0')}` : '-'}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </Box>
  );
}
