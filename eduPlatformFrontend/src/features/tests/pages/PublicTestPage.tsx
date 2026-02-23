import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Paper,
  Button,
  CircularProgress,
  Chip,
} from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import QuizIcon from '@mui/icons-material/Quiz';
import { testApi } from '@/api/testApi';

export default function PublicTestPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('test');
  const { t: tTaking } = useTranslation('testTaking');

  const { data: test, isLoading, error } = useQuery({
    queryKey: ['public-test', slug],
    queryFn: async () => {
      const { data } = await testApi.getPublicTest(slug!);
      return data.data;
    },
    enabled: !!slug,
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !test) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography variant="h5" gutterBottom>
          {t('empty.title')}
        </Typography>
        <Typography color="text.secondary">
          {t('empty.description')}
        </Typography>
      </Box>
    );
  }

  const handleStart = () => {
    navigate(`/my-tests`);
  };

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto', py: 4, px: 2 }}>
      <Paper sx={{ p: 4, textAlign: 'center' }}>
        <QuizIcon sx={{ fontSize: 64, color: 'primary.main', mb: 2 }} />
        <Typography variant="h4" fontWeight={700} gutterBottom>
          {test.title}
        </Typography>
        <Typography variant="body1" color="text.secondary" gutterBottom>
          {test.subjectName}
        </Typography>

        <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1, my: 3, flexWrap: 'wrap' }}>
          <Chip
            label={t('card.questions', { count: test.questionCount })}
            variant="outlined"
          />
          <Chip
            label={t('card.variants', { count: test.variantCount })}
            variant="outlined"
          />
          {test.publicDurationMinutes && (
            <Chip
              icon={<AccessTimeIcon />}
              label={`${test.publicDurationMinutes} ${t('publish.duration').toLowerCase()}`}
              variant="outlined"
              color="warning"
            />
          )}
        </Box>

        <Button
          variant="contained"
          size="large"
          startIcon={<PlayArrowIcon />}
          onClick={handleStart}
          sx={{ mt: 2, px: 4, py: 1.5 }}
        >
          {tTaking('startTest')}
        </Button>
      </Paper>
    </Box>
  );
}
