import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  CircularProgress,
  Pagination,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Chip,
  Paper,
  Button,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import HistoryIcon from '@mui/icons-material/History';
import { useTranslation } from 'react-i18next';
import { useMyAttempts } from '../hooks/useAttempt';

const STATUS_COLORS: Record<string, 'info' | 'success' | 'warning' | 'default'> = {
  IN_PROGRESS: 'info',
  SUBMITTED: 'warning',
  AUTO_GRADED: 'success',
  NEEDS_REVIEW: 'warning',
  GRADED: 'success',
  EXPIRED: 'default',
};

export default function MyAttemptsPage() {
  const { t } = useTranslation('testTaking');
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  const params = useMemo(() => ({ page, size: 20 }), [page]);
  const { data, isLoading } = useMyAttempts(params);

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 1 }}>{t('myAttemptsTitle')}</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>{t('myAttemptsSubtitle')}</Typography>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : data && data.content.length > 0 ? (
        <>
          <Paper sx={{ overflow: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>#</TableCell>
                  <TableCell>{t('testName')}</TableCell>
                  <TableCell align="center">{t('score')}</TableCell>
                  <TableCell align="center">{t('percentage')}</TableCell>
                  <TableCell>{t('status')}</TableCell>
                  <TableCell>{t('date')}</TableCell>
                  <TableCell align="center">{t('actions')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.content.map((attempt, i) => (
                  <TableRow key={attempt.id} hover>
                    <TableCell>{page * 20 + i + 1}</TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight={500}>{attempt.testTitle}</Typography>
                    </TableCell>
                    <TableCell align="center">
                      {attempt.score != null ? `${attempt.score}/${attempt.maxScore}` : '-'}
                    </TableCell>
                    <TableCell align="center">
                      {attempt.percentage != null ? (
                        <Chip
                          label={`${Math.round(attempt.percentage)}%`}
                          size="small"
                          color={attempt.percentage >= 70 ? 'success' : attempt.percentage >= 40 ? 'warning' : 'error'}
                        />
                      ) : '-'}
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={t(`attemptStatus.${attempt.status}`)}
                        size="small"
                        color={STATUS_COLORS[attempt.status] || 'default'}
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>
                      {new Date(attempt.startedAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell align="center">
                      <Button
                        size="small"
                        startIcon={<VisibilityIcon />}
                        onClick={() => navigate(`/attempt-result/${attempt.id}`)}
                      >
                        {t('viewResult')}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
          {data.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
              <Pagination
                count={data.totalPages}
                page={page + 1}
                onChange={(_, p) => setPage(p - 1)}
                color="primary"
              />
            </Box>
          )}
        </>
      ) : (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <HistoryIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">{t('noAttempts')}</Typography>
          <Typography variant="body2" color="text.disabled">{t('noAttemptsDescription')}</Typography>
        </Box>
      )}
    </Box>
  );
}
