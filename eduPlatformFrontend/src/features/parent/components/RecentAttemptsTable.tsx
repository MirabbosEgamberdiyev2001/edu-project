import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Typography,
  Chip,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { ChildAttemptDto } from '@/types/parent';

interface RecentAttemptsTableProps {
  attempts: ChildAttemptDto[];
}

export default function RecentAttemptsTable({ attempts }: RecentAttemptsTableProps) {
  const { t } = useTranslation('parent');

  const getPercentageColor = (pct: number): 'success' | 'warning' | 'error' => {
    if (pct >= 70) return 'success';
    if (pct >= 40) return 'warning';
    return 'error';
  };

  if (attempts.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
        {t('noChildren')}
      </Typography>
    );
  }

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>{t('testTitle')}</TableCell>
            <TableCell align="center">{t('score')}</TableCell>
            <TableCell align="center">{t('percentage')}</TableCell>
            <TableCell align="right">{t('date')}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {attempts.map((attempt) => (
            <TableRow key={attempt.id} hover>
              <TableCell>
                <Typography variant="body2" fontWeight={500}>
                  {attempt.testTitle}
                </Typography>
              </TableCell>
              <TableCell align="center">
                <Typography variant="body2">
                  {attempt.score}/{attempt.maxScore}
                </Typography>
              </TableCell>
              <TableCell align="center">
                <Chip
                  label={`${Math.round(attempt.percentage)}%`}
                  size="small"
                  color={getPercentageColor(attempt.percentage)}
                  variant="outlined"
                />
              </TableCell>
              <TableCell align="right">
                <Typography variant="body2" color="text.secondary">
                  {new Date(attempt.submittedAt).toLocaleDateString()}
                </Typography>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
