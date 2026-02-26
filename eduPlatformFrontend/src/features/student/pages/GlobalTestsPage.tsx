import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
  CircularProgress,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Pagination,
  Alert,
  Divider,
  Stack,
  Skeleton,
} from '@mui/material';
import QuizIcon from '@mui/icons-material/Quiz';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import TimerIcon from '@mui/icons-material/Timer';
import FilterListIcon from '@mui/icons-material/FilterList';
import { useTranslation } from 'react-i18next';
import { globalTestApi } from '@/api/globalTestApi';
import { subjectApi } from '@/api/subjectApi';
import { TestCategory, type TestHistoryDto } from '@/types/test';
import PageBreadcrumbs from '@/components/PageBreadcrumbs';

const CATEGORY_COLORS: Record<string, string> = {
  DTM: '#1565c0',
  SCHOOL: '#2e7d32',
  OLYMPIAD: '#f57c00',
  CERTIFICATE: '#6a1b9a',
  ATTESTATSIYA: '#c62828',
};

const GRADE_LEVELS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];

export default function GlobalTestsPage() {
  const navigate = useNavigate();
  const { t } = useTranslation('testTaking');
  const [category, setCategory] = useState<TestCategory | ''>('');
  const [subjectId, setSubjectId] = useState('');
  const [gradeLevel, setGradeLevel] = useState<number | ''>('');
  const [page, setPage] = useState(0);

  const { data: subjectsData } = useQuery({
    queryKey: ['subjects-all'],
    queryFn: () => subjectApi.getSubjects({ size: 100 }).then(r => r.data.data?.content || []),
  });

  const { data, isLoading, isError } = useQuery({
    queryKey: ['global-tests', category, subjectId, gradeLevel, page],
    queryFn: () => globalTestApi.getAll({
      category: category || undefined,
      subjectId: subjectId || undefined,
      gradeLevel: gradeLevel || undefined,
      page,
      size: 12,
    }).then(r => r.data.data),
    staleTime: 30_000,
  });

  const startMutation = useMutation({
    mutationFn: (id: string) => globalTestApi.start(id).then(r => r.data.data!),
    onSuccess: (result) => {
      navigate(`/exam/${result.attemptId}`);
    },
  });

  const handleReset = useCallback(() => {
    setCategory('');
    setSubjectId('');
    setGradeLevel('');
    setPage(0);
  }, []);

  const tests = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const totalElements = data?.totalElements || 0;

  return (
    <Box>
      <PageBreadcrumbs items={[{ label: t('globalTests.title') }]} />

      <Typography variant="h5" fontWeight={700} sx={{ mb: 1 }}>
        {t('globalTests.title')}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        {t('globalTests.subtitle')}
      </Typography>

      {/* Category Quick Filter */}
      <Box sx={{ display: 'flex', gap: 1, mb: 3, flexWrap: 'wrap' }}>
        <Chip
          label={t('globalTests.all')}
          onClick={() => { setCategory(''); setPage(0); }}
          color={!category ? 'primary' : 'default'}
          variant={!category ? 'filled' : 'outlined'}
        />
        {Object.values(TestCategory).map(cat => (
          <Chip
            key={cat}
            label={t(`globalTests.category.${cat}`, cat)}
            onClick={() => { setCategory(cat as TestCategory); setPage(0); }}
            color={category === cat ? 'primary' : 'default'}
            variant={category === cat ? 'filled' : 'outlined'}
            sx={{ fontWeight: category === cat ? 700 : 400 }}
          />
        ))}
      </Box>

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <FilterListIcon fontSize="small" color="action" />
          <Typography variant="subtitle2">{t('globalTests.filter')}</Typography>
          {(category || subjectId || gradeLevel) && (
            <Button size="small" onClick={handleReset} sx={{ ml: 'auto' }}>
              {t('globalTests.clearFilter')}
            </Button>
          )}
        </Box>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={4}>
            <FormControl fullWidth size="small">
              <InputLabel>{t('globalTests.categoryLabel')}</InputLabel>
              <Select
                value={category}
                label={t('globalTests.categoryLabel')}
                onChange={(e) => { setCategory(e.target.value as TestCategory | ''); setPage(0); }}
              >
                <MenuItem value="">{t('globalTests.all')}</MenuItem>
                {Object.values(TestCategory).map(cat => (
                  <MenuItem key={cat} value={cat}>{t(`globalTests.category.${cat}`, cat)}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={4}>
            <FormControl fullWidth size="small">
              <InputLabel>{t('globalTests.subject')}</InputLabel>
              <Select
                value={subjectId}
                label={t('globalTests.subject')}
                onChange={(e) => { setSubjectId(e.target.value); setPage(0); }}
              >
                <MenuItem value="">{t('globalTests.all')}</MenuItem>
                {(subjectsData || []).map(s => (
                  <MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={4}>
            <FormControl fullWidth size="small">
              <InputLabel>{t('globalTests.grade')}</InputLabel>
              <Select
                value={gradeLevel}
                label={t('globalTests.grade')}
                onChange={(e) => { setGradeLevel(e.target.value as number | ''); setPage(0); }}
              >
                <MenuItem value="">{t('globalTests.all')}</MenuItem>
                {GRADE_LEVELS.map(g => (
                  <MenuItem key={g} value={g}>{t('globalTests.gradeLabel', { grade: g })}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </Paper>

      {/* Results Count */}
      {!isLoading && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {t('globalTests.found', { count: totalElements })}
        </Typography>
      )}

      {/* Test Cards */}
      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('globalTests.loadError')}
        </Alert>
      )}

      {isLoading ? (
        <Grid container spacing={2}>
          {Array.from({ length: 8 }).map((_, i) => (
            <Grid item xs={12} sm={6} md={4} key={i}>
              <Skeleton variant="rounded" height={200} />
            </Grid>
          ))}
        </Grid>
      ) : tests.length === 0 ? (
        <Paper sx={{ p: 6, textAlign: 'center' }}>
          <QuizIcon sx={{ fontSize: 60, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            {t('globalTests.empty')}
          </Typography>
          <Typography variant="body2" color="text.disabled" sx={{ mt: 1 }}>
            {t('globalTests.emptyHint')}
          </Typography>
        </Paper>
      ) : (
        <Grid container spacing={2}>
          {tests.map((test) => (
            <Grid item xs={12} sm={6} md={4} key={test.id}>
              <GlobalTestCard
                test={test}
                onStart={() => startMutation.mutate(test.id)}
                isStarting={startMutation.isPending && startMutation.variables === test.id}
              />
            </Grid>
          ))}
        </Grid>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <Pagination
            count={totalPages}
            page={page + 1}
            onChange={(_, p) => setPage(p - 1)}
            color="primary"
          />
        </Box>
      )}
    </Box>
  );
}

function GlobalTestCard({ test, onStart, isStarting }: {
  test: TestHistoryDto;
  onStart: () => void;
  isStarting: boolean;
}) {
  const { t } = useTranslation('testTaking');
  const category = test.category || 'SCHOOL';
  const catColor = CATEGORY_COLORS[category] || '#1976d2';

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        border: '1px solid',
        borderColor: 'divider',
        transition: 'box-shadow 0.2s',
        '&:hover': { boxShadow: 4 },
      }}
    >
      <Box sx={{ height: 6, bgcolor: catColor, borderRadius: '4px 4px 0 0' }} />
      <CardContent sx={{ flex: 1 }}>
        <Box sx={{ display: 'flex', gap: 1, mb: 1.5, flexWrap: 'wrap' }}>
          <Chip
            label={t(`globalTests.category.${category}`, category)}
            size="small"
            sx={{ bgcolor: catColor, color: 'white', fontWeight: 700, fontSize: '0.7rem' }}
          />
          {test.gradeLevel && (
            <Chip label={t('globalTests.gradeLabel', { grade: test.gradeLevel })} size="small" variant="outlined" />
          )}
        </Box>
        <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 1, lineHeight: 1.3 }}>
          {test.title}
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
          {test.subjectName}
        </Typography>
        <Divider sx={{ my: 1 }} />
        <Stack direction="row" spacing={2} sx={{ mt: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <QuizIcon fontSize="small" color="action" />
            <Typography variant="caption" color="text.secondary">
              {t('globalTests.questions', { count: test.questionCount })}
            </Typography>
          </Box>
          {test.publicDurationMinutes && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <TimerIcon fontSize="small" color="action" />
              <Typography variant="caption" color="text.secondary">
                {test.publicDurationMinutes} {t('minutesShort')}
              </Typography>
            </Box>
          )}
          {test.variantCount > 1 && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <Typography variant="caption" color="text.secondary">
                {t('globalTests.variants', { count: test.variantCount })}
              </Typography>
            </Box>
          )}
        </Stack>
        {test.difficultyDistribution && (
          <Box sx={{ display: 'flex', gap: 0.5, mt: 1.5, flexWrap: 'wrap' }}>
            {Object.entries(test.difficultyDistribution).map(([key, val]) => (
              <Chip
                key={key}
                size="small"
                label={`${t(`globalTests.difficulty.${key}`)}: ${val}%`}
                color={key === 'easy' ? 'success' : key === 'medium' ? 'warning' : 'error'}
                sx={{ fontSize: '0.65rem' }}
              />
            ))}
          </Box>
        )}
      </CardContent>
      <CardActions sx={{ p: 2, pt: 0 }}>
        <Button
          variant="contained"
          fullWidth
          startIcon={isStarting ? <CircularProgress size={16} color="inherit" /> : <PlayArrowIcon />}
          onClick={onStart}
          disabled={isStarting}
          sx={{ fontWeight: 600 }}
        >
          {isStarting ? t('globalTests.starting') : t('globalTests.startTest')}
        </Button>
      </CardActions>
    </Card>
  );
}
