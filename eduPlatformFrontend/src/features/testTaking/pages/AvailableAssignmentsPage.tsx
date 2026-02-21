import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Grid,
  CircularProgress,
  Pagination,
} from '@mui/material';
import AssignmentIcon from '@mui/icons-material/Assignment';
import { useTranslation } from 'react-i18next';
import { useAvailableAssignments } from '../hooks/useAvailableAssignments';
import { useAttemptMutations } from '../hooks/useAttemptMutations';
import AssignmentListCard from '../components/AssignmentListCard';

export default function AvailableAssignmentsPage() {
  const { t } = useTranslation('testTaking');
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  const params = useMemo(() => ({ page, size: 12 }), [page]);
  const { data, isLoading } = useAvailableAssignments(params);
  const { startAttempt } = useAttemptMutations();

  const handleStart = (assignmentId: string) => {
    startAttempt.mutate(assignmentId, {
      onSuccess: ({ data: resp }) => {
        navigate(`/exam/${resp.data.id}`);
      },
    });
  };

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 1 }}>{t('availableTitle')}</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>{t('availableSubtitle')}</Typography>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : data && data.content.length > 0 ? (
        <>
          <Grid container spacing={2.5}>
            {data.content.map((assignment) => (
              <Grid item xs={12} sm={6} md={4} key={assignment.id}>
                <AssignmentListCard
                  assignment={assignment}
                  onStart={handleStart}
                  isPending={startAttempt.isPending}
                />
              </Grid>
            ))}
          </Grid>
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
          <AssignmentIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">{t('noAvailable')}</Typography>
          <Typography variant="body2" color="text.disabled">{t('noAvailableDescription')}</Typography>
        </Box>
      )}
    </Box>
  );
}
