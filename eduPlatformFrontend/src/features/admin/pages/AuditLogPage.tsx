import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Pagination,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  IconButton,
  Collapse,
  Button,
  Tooltip,
} from '@mui/material';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import FilterAltOffIcon from '@mui/icons-material/FilterAltOff';
import { adminApi } from '@/api/adminApi';
import { useQuery } from '@tanstack/react-query';
import type { AuditLogDto } from '@/types/admin';
import type { PagedResponse } from '@/types/subject';
import { PageShell } from '@/components/ui';

const ACTION_CATEGORIES = ['AUTH', 'USER_MANAGEMENT', 'CONTENT', 'TEST', 'SUBSCRIPTION', 'MODERATION'];

const CATEGORY_COLORS: Record<string, string> = {
  AUTH: '#1976d2',
  USER_MANAGEMENT: '#9c27b0',
  CONTENT: '#2e7d32',
  TEST: '#ed6c02',
  SUBSCRIPTION: '#0288d1',
  MODERATION: '#c62828',
};

// Color actions by their verb prefix
function actionColor(action: string): string {
  const upper = action.toUpperCase();
  if (upper.includes('DELETE') || upper.includes('REJECT') || upper.includes('BLOCK')) return '#c62828';
  if (upper.includes('CREATE') || upper.includes('REGISTER') || upper.includes('ADD')) return '#2e7d32';
  if (upper.includes('UPDATE') || upper.includes('EDIT') || upper.includes('CHANGE')) return '#ed6c02';
  if (upper.includes('LOGIN') || upper.includes('LOGOUT') || upper.includes('AUTH')) return '#1976d2';
  if (upper.includes('APPROVE') || upper.includes('ACTIVE') || upper.includes('PUBLISH')) return '#0288d1';
  return '#546e7a';
}

export default function AuditLogPage() {
  const { t } = useTranslation('admin');

  const [category, setCategory] = useState('');
  const [actionSearch, setActionSearch] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(0);
  const size = 30;

  const queryParams = useMemo(() => ({ page, size }), [page]);

  const hasFilters = Boolean(category || actionSearch || dateFrom || dateTo);

  function clearFilters() {
    setCategory('');
    setActionSearch('');
    setDateFrom('');
    setDateTo('');
    setPage(0);
  }

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'audit-logs', { category, dateFrom, dateTo, page, size }],
    queryFn: async () => {
      let response;
      if (category) {
        response = await adminApi.getAuditLogsByCategory(category, queryParams);
      } else if (dateFrom && dateTo) {
        response = await adminApi.getAuditLogsByDateRange(dateFrom, dateTo, queryParams);
      } else {
        response = await adminApi.getAuditLogs(queryParams);
      }
      return response.data.data as PagedResponse<AuditLogDto>;
    },
    staleTime: 15_000,
  });

  // Client-side action search filter
  const filteredContent = useMemo(() => {
    if (!data?.content) return [];
    if (!actionSearch.trim()) return data.content;
    const lower = actionSearch.toLowerCase();
    return data.content.filter(
      (log) =>
        log.action.toLowerCase().includes(lower) ||
        (log.entityType ?? '').toLowerCase().includes(lower)
    );
  }, [data?.content, actionSearch]);

  const totalCount = data?.totalElements ?? 0;

  return (
    <PageShell
      title={t('auditLog.title')}
      subtitle={t('auditLog.subtitle')}
      actions={
        totalCount > 0 ? (
          <Chip
            label={`${totalCount.toLocaleString()} ${t('auditLog.totalEntries') || 'entries'}`}
            size="small"
            color="default"
          />
        ) : undefined
      }
    >
      {/* Filters */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel>{t('auditLog.filterCategory')}</InputLabel>
            <Select
              value={category}
              label={t('auditLog.filterCategory')}
              onChange={(e) => { setCategory(e.target.value); setPage(0); }}
            >
              <MenuItem value="">{t('users.all')}</MenuItem>
              {ACTION_CATEGORIES.map((cat) => (
                <MenuItem key={cat} value={cat}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box sx={{ width: 10, height: 10, borderRadius: '50%', bgcolor: CATEGORY_COLORS[cat] || '#607d8b' }} />
                    {cat}
                  </Box>
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <TextField
            size="small"
            label={t('auditLog.searchAction') || 'Search action'}
            value={actionSearch}
            onChange={(e) => setActionSearch(e.target.value)}
            sx={{ minWidth: 200 }}
            placeholder="e.g. QUESTION_CREATED"
          />

          <TextField
            size="small"
            type="datetime-local"
            label={t('auditLog.from')}
            value={dateFrom}
            onChange={(e) => { setDateFrom(e.target.value); setPage(0); }}
            InputLabelProps={{ shrink: true }}
            disabled={!!category}
          />
          <TextField
            size="small"
            type="datetime-local"
            label={t('auditLog.to')}
            value={dateTo}
            onChange={(e) => { setDateTo(e.target.value); setPage(0); }}
            InputLabelProps={{ shrink: true }}
            disabled={!!category}
          />

          {hasFilters && (
            <Tooltip title={t('auditLog.clearFilters') || 'Clear filters'}>
              <Button
                size="small"
                variant="outlined"
                color="secondary"
                startIcon={<FilterAltOffIcon />}
                onClick={clearFilters}
              >
                {t('auditLog.clearFilters') || 'Clear'}
              </Button>
            </Tooltip>
          )}
        </Box>
      </Paper>

      {/* Table */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : filteredContent.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">{t('auditLog.noLogs')}</Typography>
        </Paper>
      ) : (
        <>
          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow sx={{ bgcolor: 'grey.50' }}>
                  <TableCell width={40} />
                  <TableCell>{t('auditLog.timestamp')}</TableCell>
                  <TableCell>{t('auditLog.action')}</TableCell>
                  <TableCell>{t('auditLog.category')}</TableCell>
                  <TableCell>{t('auditLog.entityType')}</TableCell>
                  <TableCell>{t('auditLog.userRole')}</TableCell>
                  <TableCell>{t('auditLog.ipAddress')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredContent.map((log) => (
                  <AuditLogRow key={log.id} log={log} t={t} />
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          {(data?.totalPages ?? 0) > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
              <Pagination
                count={data!.totalPages}
                page={page + 1}
                onChange={(_, p) => setPage(p - 1)}
                color="primary"
              />
            </Box>
          )}
        </>
      )}
    </PageShell>
  );
}

function AuditLogRow({ log, t }: { log: AuditLogDto; t: (key: string) => string }) {
  const [open, setOpen] = useState(false);
  const hasDetails = log.oldValues || log.newValues || log.userAgent || log.entityId || log.userId;

  return (
    <>
      <TableRow hover sx={{ '&:hover': { bgcolor: 'grey.50' } }}>
        <TableCell>
          {hasDetails && (
            <IconButton size="small" onClick={() => setOpen(!open)}>
              {open ? <KeyboardArrowUpIcon fontSize="small" /> : <KeyboardArrowDownIcon fontSize="small" />}
            </IconButton>
          )}
        </TableCell>
        <TableCell>
          <Typography variant="caption" sx={{ fontFamily: 'monospace', whiteSpace: 'nowrap' }}>
            {new Date(log.createdAt).toLocaleString()}
          </Typography>
        </TableCell>
        <TableCell>
          <Chip
            label={log.action}
            size="small"
            sx={{
              bgcolor: actionColor(log.action),
              color: '#fff',
              fontWeight: 600,
              fontSize: '0.7rem',
              maxWidth: 280,
            }}
          />
        </TableCell>
        <TableCell>
          <Chip
            label={log.actionCategory}
            size="small"
            sx={{
              bgcolor: CATEGORY_COLORS[log.actionCategory] || '#607d8b',
              color: '#fff',
              fontWeight: 600,
              fontSize: '0.7rem',
            }}
          />
        </TableCell>
        <TableCell>
          {log.entityType ? (
            <Chip label={log.entityType} size="small" variant="outlined" />
          ) : '—'}
        </TableCell>
        <TableCell>
          <Typography variant="caption" sx={{ fontFamily: 'monospace' }}>{log.userRole || '—'}</Typography>
        </TableCell>
        <TableCell>
          <Typography variant="caption" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
            {log.ipAddress || '—'}
          </Typography>
        </TableCell>
      </TableRow>

      {/* Expandable detail row */}
      {hasDetails && (
        <TableRow>
          <TableCell colSpan={7} sx={{ py: 0 }}>
            <Collapse in={open} timeout="auto" unmountOnExit>
              <Box sx={{ p: 2, bgcolor: 'grey.50' }}>
                <Box sx={{ display: 'flex', gap: 4, flexWrap: 'wrap', mb: 1 }}>
                  {log.entityId && (
                    <Box>
                      <Typography variant="caption" color="text.secondary">{t('auditLog.entityId')}</Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                        {log.entityId}
                      </Typography>
                    </Box>
                  )}
                  {log.userId && (
                    <Box>
                      <Typography variant="caption" color="text.secondary">{t('auditLog.user')} ID</Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                        {log.userId}
                      </Typography>
                    </Box>
                  )}
                  {log.userAgent && (
                    <Box sx={{ maxWidth: 400 }}>
                      <Typography variant="caption" color="text.secondary">{t('auditLog.userAgent')}</Typography>
                      <Typography variant="body2" sx={{ fontSize: '0.75rem', wordBreak: 'break-all' }}>
                        {log.userAgent}
                      </Typography>
                    </Box>
                  )}
                </Box>

                {/* Old / New Values */}
                {(log.oldValues || log.newValues) && (
                  <Box sx={{ display: 'flex', gap: 3, mt: 1 }}>
                    {log.oldValues && Object.keys(log.oldValues).length > 0 && (
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="caption" color="error.main" fontWeight={600}>
                          {t('auditLog.oldValues')}
                        </Typography>
                        <Paper variant="outlined" sx={{ p: 1.5, mt: 0.5, bgcolor: '#fff8f8', borderColor: 'error.light' }}>
                          <pre style={{ margin: 0, fontSize: '0.75rem', whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                            {JSON.stringify(log.oldValues, null, 2)}
                          </pre>
                        </Paper>
                      </Box>
                    )}
                    {log.newValues && Object.keys(log.newValues).length > 0 && (
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="caption" color="success.main" fontWeight={600}>
                          {t('auditLog.newValues')}
                        </Typography>
                        <Paper variant="outlined" sx={{ p: 1.5, mt: 0.5, bgcolor: '#f8fff8', borderColor: 'success.light' }}>
                          <pre style={{ margin: 0, fontSize: '0.75rem', whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                            {JSON.stringify(log.newValues, null, 2)}
                          </pre>
                        </Paper>
                      </Box>
                    )}
                  </Box>
                )}
              </Box>
            </Collapse>
          </TableCell>
        </TableRow>
      )}
    </>
  );
}
