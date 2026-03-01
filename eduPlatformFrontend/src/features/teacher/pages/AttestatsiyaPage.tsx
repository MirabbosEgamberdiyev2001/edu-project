import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
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
  TextField,
  InputAdornment,
} from '@mui/material';
import SchoolIcon from '@mui/icons-material/School';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import TimerIcon from '@mui/icons-material/Timer';
import QuizIcon from '@mui/icons-material/Quiz';
import FilterListIcon from '@mui/icons-material/FilterList';
import SearchIcon from '@mui/icons-material/Search';
import { useTranslation } from 'react-i18next';
import { globalTestApi } from '@/api/globalTestApi';
import { subjectApi } from '@/api/subjectApi';
import { TestCategory, type TestHistoryDto } from '@/types/test';
import { useAuth } from '@/hooks/useAuth';
import { PageShell } from '@/components/ui';

const ATTESTATION_COLOR = '#c62828';

export default function AttestatsiyaPage() {
  const navigate = useNavigate();
  const { t } = useTranslation('testTaking');
  const { user } = useAuth();

  const [searchParams, setSearchParams] = useSearchParams();

  // Derive filter values from URL params
  const subjectId = searchParams.get('subject') ?? '';
  const page = parseInt(searchParams.get('page') ?? '0', 10);

  // Search state — local input + debounced value for API
  const [searchInput, setSearchInput] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchInput.trim());
      // Reset page when search changes
      if (searchInput.trim() !== debouncedSearch) {
        setSearchParams((prev) => {
          const next = new URLSearchParams(prev);
          next.delete('page');
          return next;
        });
      }
    }, 400);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchInput]);

  // Seed teacher's profile subject on first visit
  useEffect(() => {
    if (!searchParams.has('subject') && user?.subjectId) {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          next.set('subject', user.subjectId!);
          return next;
        },
        { replace: true }
      );
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const { data: subjectsData } = useQuery({
    queryKey: ['subjects-all'],
    queryFn: () => subjectApi.getSubjects({ size: 100 }).then((r) => r.data.data?.content || []),
    staleTime: 300_000,
  });

  const { data, isLoading, isError } = useQuery({
    queryKey: ['attestation-tests', subjectId, page, debouncedSearch],
    queryFn: () =>
      globalTestApi
        .getAll({
          category: TestCategory.ATTESTATSIYA,
          subjectId: subjectId || undefined,
          search: debouncedSearch || undefined,
          page,
          size: 12,
        })
        .then((r) => r.data.data),
    staleTime: 30_000,
  });

  const startMutation = useMutation({
    mutationFn: (id: string) => globalTestApi.start(id).then((r) => r.data.data!),
    onSuccess: (result) => {
      navigate(`/exam/${result.attemptId}`);
    },
  });

  const handleReset = useCallback(() => {
    setSearchInput('');
    setDebouncedSearch('');
    setSearchParams(user?.subjectId ? { subject: user.subjectId } : {});
  }, [user?.subjectId, setSearchParams]);

  const handleSubjectChange = useCallback(
    (value: string) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        if (value) next.set('subject', value);
        else next.delete('subject');
        next.delete('page');
        return next;
      });
    },
    [setSearchParams]
  );

  const handlePageChange = useCallback(
    (p: number) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        if (p === 0) next.delete('page');
        else next.set('page', String(p));
        return next;
      });
    },
    [setSearchParams]
  );

  const tests = data?.content || [];
  const totalPages = data?.totalPages || 0;
  const totalElements = data?.totalElements || 0;
  const hasFilters = searchParams.has('subject') || debouncedSearch.length > 0;

  return (
    <PageShell title={t('attestation.title')} subtitle={t('attestation.subtitle')}>

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 3, mt: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
          <FilterListIcon fontSize="small" color="action" />
          <Typography variant="subtitle2">{t('attestation.filter')}</Typography>
          {hasFilters && (
            <Button size="small" onClick={handleReset} sx={{ ml: 'auto' }}>
              {t('attestation.clearFilter')}
            </Button>
          )}
        </Box>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth
              size="small"
              placeholder={t('attestation.searchPlaceholder')}
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon fontSize="small" color="action" />
                  </InputAdornment>
                ),
              }}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth size="small">
              <InputLabel>{t('attestation.subject')}</InputLabel>
              <Select
                value={subjectId}
                label={t('attestation.subject')}
                onChange={(e) => handleSubjectChange(e.target.value)}
              >
                <MenuItem value="">{t('attestation.all')}</MenuItem>
                {(subjectsData || []).map((s) => (
                  <MenuItem key={s.id} value={s.id}>
                    {s.name}
                    {s.id === user?.subjectId ? ' ★' : ''}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      </Paper>

      {/* Results Count */}
      {!isLoading && !isError && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {t('attestation.found', { count: totalElements })}
        </Typography>
      )}

      {/* Error */}
      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('attestation.loadError')}
        </Alert>
      )}

      {/* Test Cards */}
      {isLoading ? (
        <Grid container spacing={2}>
          {Array.from({ length: 6 }).map((_, i) => (
            <Grid item xs={12} sm={6} md={4} key={i}>
              <Skeleton variant="rounded" height={200} />
            </Grid>
          ))}
        </Grid>
      ) : isError ? null : tests.length === 0 ? (
        <Paper sx={{ p: 6, textAlign: 'center' }}>
          <SchoolIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            {t('attestation.empty')}
          </Typography>
          <Typography variant="body2" color="text.disabled" sx={{ mt: 1 }}>
            {t('attestation.emptyHint')}
          </Typography>
        </Paper>
      ) : (
        <Grid container spacing={2}>
          {tests.map((test) => (
            <Grid item xs={12} sm={6} md={4} key={test.id}>
              <AttestationTestCard
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
            onChange={(_, p) => handlePageChange(p - 1)}
            color="primary"
          />
        </Box>
      )}
    </PageShell>
  );
}

function AttestationTestCard({
  test,
  onStart,
  isStarting,
}: {
  test: TestHistoryDto;
  onStart: () => void;
  isStarting: boolean;
}) {
  const { t } = useTranslation('testTaking');

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
      <Box sx={{ height: 6, bgcolor: ATTESTATION_COLOR, borderRadius: '4px 4px 0 0' }} />
      <CardContent sx={{ flex: 1 }}>
        <Box sx={{ display: 'flex', gap: 1, mb: 1.5, flexWrap: 'wrap' }}>
          <Chip
            label={`📋 ${t('attestation.title')}`}
            size="small"
            sx={{ bgcolor: ATTESTATION_COLOR, color: 'white', fontWeight: 700, fontSize: '0.7rem' }}
          />
          {test.gradeLevel && (
            <Chip
              label={t('attestation.gradeLabel', { grade: test.gradeLevel })}
              size="small"
              variant="outlined"
            />
          )}
        </Box>
        <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 0.5, lineHeight: 1.3 }}>
          {test.title}
        </Typography>
        {test.subjectName && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            {test.subjectName}
          </Typography>
        )}
        <Divider sx={{ my: 1 }} />
        <Stack direction="row" spacing={2}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <QuizIcon fontSize="small" color="action" />
            <Typography variant="caption" color="text.secondary">
              {t('attestation.questions', { count: test.questionCount })}
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
        </Stack>
        {test.teacherName && (
          <Typography variant="caption" color="text.disabled" sx={{ display: 'block', mt: 1 }}>
            {test.teacherName}
          </Typography>
        )}
      </CardContent>
      <CardActions sx={{ p: 2, pt: 0 }}>
        <Button
          variant="contained"
          fullWidth
          startIcon={
            isStarting ? <CircularProgress size={16} color="inherit" /> : <PlayArrowIcon />
          }
          onClick={onStart}
          disabled={isStarting}
          sx={{ fontWeight: 600, bgcolor: ATTESTATION_COLOR, '&:hover': { bgcolor: '#b71c1c' } }}
        >
          {isStarting ? t('attestation.starting') : t('attestation.startTest')}
        </Button>
      </CardActions>
    </Card>
  );
}
