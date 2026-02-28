import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
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
  Tabs,
  Tab,
  Grid,
  Typography,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import HistoryIcon from '@mui/icons-material/History';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import HighlightOffIcon from '@mui/icons-material/HighlightOff';
import EmojiEventsIcon from '@mui/icons-material/EmojiEvents';
import PercentIcon from '@mui/icons-material/Percent';
import { useTranslation } from 'react-i18next';
import { PageShell, EmptyState, KpiWidget } from '@/components/ui';
import { useMyAttempts } from '../hooks/useAttempt';
import type { AttemptDto } from '@/types/testTaking';

const STATUS_COLORS: Record<string, 'info' | 'success' | 'warning' | 'default' | 'error'> = {
  IN_PROGRESS: 'info',
  SUBMITTED: 'warning',
  AUTO_GRADED: 'success',
  NEEDS_REVIEW: 'warning',
  GRADED: 'success',
  EXPIRED: 'error',
};

const PASS_THRESHOLD = 70;
const ITEMS_PER_PAGE = 20;

type TabKey = 'all' | 'passed' | 'failed' | 'in_progress';

function formatDuration(seconds: number): string {
  if (seconds < 60) return `${seconds}s`;
  const totalMin = Math.floor(seconds / 60);
  const secs = seconds % 60;
  if (totalMin < 60) return secs > 0 ? `${totalMin}m ${secs}s` : `${totalMin}m`;
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  return m > 0 ? `${h}h ${m}m` : `${h}h`;
}

function computeTimeTaken(attempt: AttemptDto): number | null {
  if (!attempt.submittedAt || !attempt.startedAt) return null;
  const diff = new Date(attempt.submittedAt).getTime() - new Date(attempt.startedAt).getTime();
  return diff > 0 ? Math.round(diff / 1000) : null;
}

function classifyAttempt(a: AttemptDto): 'passed' | 'failed' | 'in_progress' | 'pending' {
  if (a.status === 'IN_PROGRESS') return 'in_progress';
  if (a.status === 'EXPIRED') return 'failed';
  if (a.percentage != null && a.percentage >= PASS_THRESHOLD) return 'passed';
  if (a.percentage != null && a.percentage < PASS_THRESHOLD) return 'failed';
  return 'pending'; // SUBMITTED/NEEDS_REVIEW with no score yet
}

export default function MyAttemptsPage() {
  const { t } = useTranslation('testTaking');
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState<TabKey>('all');
  const [tablePage, setTablePage] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');

  const { data, isLoading } = useMyAttempts({ page: 0, size: 200 });
  const allAttempts: AttemptDto[] = data?.content ?? [];
  const totalFromServer = data?.totalElements ?? 0;

  const tabFiltered = useMemo<AttemptDto[]>(() => {
    if (activeTab === 'all') return allAttempts;
    return allAttempts.filter((a) => {
      const cls = classifyAttempt(a);
      if (activeTab === 'passed') return cls === 'passed';
      if (activeTab === 'failed') return cls === 'failed';
      if (activeTab === 'in_progress') return cls === 'in_progress';
      return true;
    });
  }, [allAttempts, activeTab]);

  const filtered = useMemo<AttemptDto[]>(() => {
    if (!statusFilter) return tabFiltered;
    return tabFiltered.filter((a) => a.status === statusFilter);
  }, [tabFiltered, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / ITEMS_PER_PAGE));
  const pageItems = filtered.slice(tablePage * ITEMS_PER_PAGE, (tablePage + 1) * ITEMS_PER_PAGE);

  const gradedAttempts = useMemo(
    () => allAttempts.filter((a) => a.percentage != null && a.status !== 'IN_PROGRESS'),
    [allAttempts]
  );

  const avgScore = useMemo(() => {
    if (!gradedAttempts.length) return null;
    const sum = gradedAttempts.reduce((acc, a) => acc + (a.percentage ?? 0), 0);
    return Math.round(sum / gradedAttempts.length);
  }, [gradedAttempts]);

  const bestScore = useMemo(() => {
    if (!gradedAttempts.length) return null;
    return Math.round(Math.max(...gradedAttempts.map((a) => a.percentage ?? 0)));
  }, [gradedAttempts]);

  const passRate = useMemo(() => {
    if (!gradedAttempts.length) return null;
    const passed = gradedAttempts.filter((a) => (a.percentage ?? 0) >= PASS_THRESHOLD).length;
    return Math.round((passed / gradedAttempts.length) * 100);
  }, [gradedAttempts]);

  const handleTabChange = (_: React.SyntheticEvent, val: TabKey) => {
    setActiveTab(val);
    setTablePage(0);
    setStatusFilter('');
  };

  return (
    <PageShell
      title={t('myAttemptsTitle')}
      subtitle={t('myAttemptsSubtitle')}
    >
      {/* KPI row */}
      {!isLoading && totalFromServer > 0 && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={6} sm={3}>
            <KpiWidget
              label={t('kpi.total')}
              value={totalFromServer}
              icon={<HistoryIcon sx={{ color: '#3b82f6' }} />}
              iconBg="#eff6ff"
            />
          </Grid>
          <Grid item xs={6} sm={3}>
            <KpiWidget
              label={t('kpi.avgScore')}
              value={avgScore != null ? `${avgScore}%` : '—'}
              icon={<PercentIcon sx={{ color: '#8b5cf6' }} />}
              iconBg="#f5f3ff"
            />
          </Grid>
          <Grid item xs={6} sm={3}>
            <KpiWidget
              label={t('kpi.bestScore')}
              value={bestScore != null ? `${bestScore}%` : '—'}
              icon={<EmojiEventsIcon sx={{ color: '#f59e0b' }} />}
              iconBg="#fffbeb"
            />
          </Grid>
          <Grid item xs={6} sm={3}>
            <KpiWidget
              label={t('kpi.passRate')}
              value={passRate != null ? `${passRate}%` : '—'}
              icon={<CheckCircleOutlineIcon sx={{ color: '#10b981' }} />}
              iconBg="#ecfdf5"
            />
          </Grid>
        </Grid>
      )}

      {/* Tabs */}
      <Tabs
        value={activeTab}
        onChange={handleTabChange}
        sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
        variant="scrollable"
        scrollButtons="auto"
      >
        <Tab label={t('tabs.all')} value="all" sx={{ textTransform: 'none', fontWeight: 500 }} />
        <Tab
          label={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <CheckCircleOutlineIcon sx={{ fontSize: 15, color: activeTab === 'passed' ? 'success.main' : 'text.secondary' }} />
              {t('tabs.passed')}
            </Box>
          }
          value="passed"
          sx={{ textTransform: 'none', fontWeight: 500 }}
        />
        <Tab
          label={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <HighlightOffIcon sx={{ fontSize: 15, color: activeTab === 'failed' ? 'error.main' : 'text.secondary' }} />
              {t('tabs.failed')}
            </Box>
          }
          value="failed"
          sx={{ textTransform: 'none', fontWeight: 500 }}
        />
        <Tab label={t('tabs.inProgress')} value="in_progress" sx={{ textTransform: 'none', fontWeight: 500 }} />
      </Tabs>

      {/* Status sub-filter */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2, alignItems: 'center' }}>
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>{t('status')}</InputLabel>
          <Select
            value={statusFilter}
            label={t('status')}
            onChange={(e) => { setStatusFilter(e.target.value); setTablePage(0); }}
          >
            <MenuItem value="">{t('tabs.all')}</MenuItem>
            {['IN_PROGRESS', 'SUBMITTED', 'AUTO_GRADED', 'NEEDS_REVIEW', 'GRADED', 'EXPIRED'].map((s) => (
              <MenuItem key={s} value={s}>
                {t(`attemptStatus.${s}`)}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        {(statusFilter || activeTab !== 'all') && (
          <Typography variant="body2" color="text.secondary">
            {filtered.length} result{filtered.length !== 1 ? 's' : ''}
          </Typography>
        )}
      </Box>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : pageItems.length > 0 ? (
        <>
          <Paper sx={{ overflow: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow sx={{ '& .MuiTableCell-head': { fontWeight: 600, bgcolor: 'action.hover' } }}>
                  <TableCell>#</TableCell>
                  <TableCell>{t('testName')}</TableCell>
                  <TableCell align="center">{t('score')}</TableCell>
                  <TableCell align="center">{t('percentage')}</TableCell>
                  <TableCell align="center">{t('timeTaken')}</TableCell>
                  <TableCell>{t('status')}</TableCell>
                  <TableCell>{t('date')}</TableCell>
                  <TableCell align="center">{t('actions')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {pageItems.map((attempt, i) => {
                  const timeSec = computeTimeTaken(attempt);
                  const isActive = attempt.status === 'IN_PROGRESS';
                  return (
                    <TableRow key={attempt.id} hover>
                      <TableCell sx={{ color: 'text.secondary', fontSize: '0.8rem' }}>
                        {tablePage * ITEMS_PER_PAGE + i + 1}
                      </TableCell>
                      <TableCell sx={{ maxWidth: 220 }}>
                        <Typography variant="body2" noWrap sx={{ fontWeight: 500 }}>
                          {attempt.testTitle ?? attempt.assignmentTitle ?? '—'}
                        </Typography>
                      </TableCell>
                      <TableCell align="center">
                        <Typography variant="body2">
                          {attempt.score != null && attempt.maxScore != null
                            ? `${attempt.score}/${attempt.maxScore}`
                            : '—'}
                        </Typography>
                      </TableCell>
                      <TableCell align="center">
                        {attempt.percentage != null ? (
                          <Chip
                            label={`${Math.round(attempt.percentage)}%`}
                            size="small"
                            color={
                              attempt.percentage >= PASS_THRESHOLD
                                ? 'success'
                                : attempt.percentage >= 40
                                ? 'warning'
                                : 'error'
                            }
                            sx={{ fontWeight: 600, minWidth: 56 }}
                          />
                        ) : '—'}
                      </TableCell>
                      <TableCell align="center">
                        <Typography variant="body2" color="text.secondary">
                          {timeSec != null ? formatDuration(timeSec) : '—'}
                        </Typography>
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
                        <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
                          {new Date(attempt.startedAt).toLocaleDateString()}
                        </Typography>
                      </TableCell>
                      <TableCell align="center">
                        <Button
                          size="small"
                          variant={isActive ? 'contained' : 'outlined'}
                          startIcon={isActive ? <PlayArrowIcon /> : <VisibilityIcon />}
                          onClick={() => navigate(`/attempt-result/${attempt.id}`)}
                          disabled={attempt.status === 'SUBMITTED'}
                        >
                          {isActive ? t('continueTest') : t('viewResult')}
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </Paper>

          {totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
              <Pagination
                count={totalPages}
                page={tablePage + 1}
                onChange={(_, p) => setTablePage(p - 1)}
                color="primary"
              />
            </Box>
          )}
        </>
      ) : (
        <EmptyState
          icon={<HistoryIcon />}
          title={t('noAttempts')}
          description={t('noAttemptsDescription')}
        />
      )}
    </PageShell>
  );
}
