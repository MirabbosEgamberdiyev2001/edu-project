import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Paper,
  Button,
  CircularProgress,
  Grid,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import DownloadIcon from '@mui/icons-material/Download';
import { useTranslation } from 'react-i18next';
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
    <Box>
      <Button startIcon={<ArrowBackIcon />} onClick={() => navigate(`/assignments/${id}`)} sx={{ mb: 2 }}>
        {t('backToDetail')}
      </Button>

      <Typography variant="h5" fontWeight={700} sx={{ mb: 1 }}>
        {t('resultsTitle')}
      </Typography>
      {assignment && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          {assignment.title} - {assignment.groupName}
        </Typography>
      )}

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
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">{t('studentResults')}</Typography>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button
                  size="small"
                  startIcon={<DownloadIcon />}
                  onClick={() => exportResults(id!, 'CSV')}
                >
                  CSV
                </Button>
                <Button
                  size="small"
                  startIcon={<DownloadIcon />}
                  onClick={() => exportResults(id!, 'EXCEL')}
                >
                  Excel
                </Button>
              </Box>
            </Box>
            <ResultsTable students={results.students} />
          </Paper>
        </>
      )}
    </Box>
  );
}
