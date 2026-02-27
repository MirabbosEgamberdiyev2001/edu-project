import { useParams } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Button,
  CircularProgress,
  Grid,
} from '@mui/material';
import DownloadIcon from '@mui/icons-material/Download';
import { useTranslation } from 'react-i18next';
import { PageShell } from '@/components/ui';
import { useAssignment } from '../hooks/useAssignments';
import { useAssignmentResults, useExportResults } from '../hooks/useAssignmentResults';
import ResultsTable from '../components/ResultsTable';

export default function AssignmentResultsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('assignment');
  const { data: assignment } = useAssignment(id!);
  const { data: results, isLoading } = useAssignmentResults(id!);
  const { exportResults } = useExportResults();

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <PageShell
      title={t('resultsTitle')}
      subtitle={assignment ? `${assignment.title} — ${assignment.groupName}` : undefined}
      breadcrumbs={[
        { label: t('common:assignments'), to: '/assignments' },
        { label: assignment?.title || '...', to: `/assignments/${id}` },
        { label: t('resultsTitle') },
      ]}
      actions={
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button size="small" startIcon={<DownloadIcon />} onClick={() => exportResults(id!, 'CSV')}>
            CSV
          </Button>
          <Button size="small" startIcon={<DownloadIcon />} onClick={() => exportResults(id!, 'EXCEL')}>
            Excel
          </Button>
        </Box>
      }
    >
      {results && (
        <>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={6} sm={3}>
              <Paper sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h4" color="primary.main">{results.completedStudents}</Typography>
                <Typography variant="caption">{t('completed')}</Typography>
              </Paper>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Paper sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h4" color="info.main">{Math.round(results.averageScore)}%</Typography>
                <Typography variant="caption">{t('averageScore')}</Typography>
              </Paper>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Paper sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h4" color="success.main">{Math.round(results.highestScore)}%</Typography>
                <Typography variant="caption">{t('highestScore')}</Typography>
              </Paper>
            </Grid>
            <Grid item xs={6} sm={3}>
              <Paper sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h4" color="error.main">{Math.round(results.lowestScore)}%</Typography>
                <Typography variant="caption">{t('lowestScore')}</Typography>
              </Paper>
            </Grid>
          </Grid>

          <Paper sx={{ p: 2, mb: 2 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>{t('studentResults')}</Typography>
            <ResultsTable students={results.students} />
          </Paper>
        </>
      )}
    </PageShell>
  );
}
