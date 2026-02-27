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
} from '@mui/material';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import { adminApi } from '@/api/adminApi';
import { useQuery } from '@tanstack/react-query';
import type { AuditLogDto } from '@/types/admin';
import type { PagedResponse } from '@/types/subject';
import { PageShell } from '@/components/ui';

const ACTION_CATEGORIES = ['AUTH', 'USER_MANAGEMENT', 'CONTENT', 'TEST'];

const CATEGORY_COLORS: Record<string, string> = {
  AUTH: '#1976d2',
  USER_MANAGEMENT: '#9c27b0',
  CONTENT: '#2e7d32',
  TEST: '#ed6c02',
};

export default function AuditLogPage() {
  const { t } = useTranslation('admin');

  const [category, setCategory] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [page, setPage] = useState(0);
  const size = 30;

  const queryParams = useMemo(() => ({ page, size }), [page]);

  // Decide which API to call based on filters
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

  return (
    <PageShell title={t('auditLog.title')} subtitle={t('auditLog.subtitle')}>

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
                <MenuItem key={cat} value={cat}>{cat}</MenuItem>
              ))}
            </Select>
          </FormControl>
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
        </Box>
      </Paper>

      {/* Table */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : !data || data.content.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">{t('auditLog.noLogs')}</Typography>
        </Paper>
      ) : (
        <>
          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
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
                {data.content.map((log) => (
                  <AuditLogRow key={log.id} log={log} t={t} />
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          {data.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
              <Pagination
                count={data.totalPages}
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
  const hasDetails = log.oldValues || log.newValues || log.userAgent;

  return (
    <>
      <TableRow hover>
        <TableCell>
          {hasDetails && (
            <IconButton size="small" onClick={() => setOpen(!open)}>
              {open ? <KeyboardArrowUpIcon fontSize="small" /> : <KeyboardArrowDownIcon fontSize="small" />}
            </IconButton>
          )}
        </TableCell>
        <TableCell>
          <Typography variant="caption">
            {new Date(log.createdAt).toLocaleString()}
          </Typography>
        </TableCell>
        <TableCell>
          <Typography variant="body2" fontWeight={500}>{log.action}</Typography>
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
          <Typography variant="caption">{log.userRole || '—'}</Typography>
        </TableCell>
        <TableCell>
          <Typography variant="caption" color="text.secondary">{log.ipAddress || '—'}</Typography>
        </TableCell>
      </TableRow>

      {/* Expandable detail row */}
      {hasDetails && (
        <TableRow>
          <TableCell colSpan={7} sx={{ py: 0 }}>
            <Collapse in={open} timeout="auto" unmountOnExit>
              <Box sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
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
                  <Box sx={{ display: 'flex', gap: 3, mt: 2 }}>
                    {log.oldValues && Object.keys(log.oldValues).length > 0 && (
                      <Box sx={{ flex: 1 }}>
                        <Typography variant="caption" color="error.main" fontWeight={600}>
                          {t('auditLog.oldValues')}
                        </Typography>
                        <Paper variant="outlined" sx={{ p: 1.5, mt: 0.5, bgcolor: 'error.50' }}>
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
                        <Paper variant="outlined" sx={{ p: 1.5, mt: 0.5, bgcolor: 'success.50' }}>
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
