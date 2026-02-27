import { useState, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Box,
  TextField,
  InputAdornment,
  Fab,
  CircularProgress,
  Pagination,
  Tabs,
  Tab,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  TableSortLabel,
  IconButton,
  Tooltip,
  Button,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import AssignmentIcon from '@mui/icons-material/Assignment';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import MonitorIcon from '@mui/icons-material/Monitor';
import BarChartIcon from '@mui/icons-material/BarChart';
import { useTranslation } from 'react-i18next';
import { useAssignments } from '../hooks/useAssignments';
import { useAssignmentMutations } from '../hooks/useAssignmentMutations';
import { useGroups } from '@/features/groups/hooks/useGroups';
import { useTests } from '@/features/tests/hooks/useTests';
import AssignmentFormDialog from '../components/AssignmentFormDialog';
import AssignmentDeleteDialog from '../components/AssignmentDeleteDialog';
import { PageShell, EmptyState, StatusBadge } from '@/components/ui';
import type { AssignmentDto, CreateAssignmentRequest } from '@/types/assignment';
import { AssignmentStatus } from '@/types/assignment';
import { formatDate, formatPercent } from '@/utils/formatters';

const STATUS_TABS: (AssignmentStatus | '')[] = [
  '',
  AssignmentStatus.DRAFT,
  AssignmentStatus.SCHEDULED,
  AssignmentStatus.ACTIVE,
  AssignmentStatus.COMPLETED,
  AssignmentStatus.CANCELLED,
];

type SortKey = 'title' | 'status' | 'startDate' | 'endDate' | 'completedStudents' | 'averageScore' | 'createdAt';
type SortDir = 'asc' | 'desc';


export default function AssignmentsPage() {
  const { t } = useTranslation('assignment');
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [statusTab, setStatusTab] = useState(0);
  const [search, setSearch] = useState(searchParams.get('q') || '');
  const [page, setPage] = useState(0);
  const [sortKey, setSortKey] = useState<SortKey>('createdAt');
  const [sortDir, setSortDir] = useState<SortDir>('desc');

  const params = useMemo(() => ({
    ...(search && { search }),
    ...(STATUS_TABS[statusTab] && { status: STATUS_TABS[statusTab] }),
    page,
    size: 20,
  }), [search, statusTab, page]);

  const { data, isLoading } = useAssignments(params);
  const { create, update, activate, cancel, remove } = useAssignmentMutations();
  const { data: groupsData } = useGroups({ size: 100 });
  const { data: testsData } = useTests({ size: 100 });

  const [formOpen, setFormOpen] = useState(false);
  const [editAssignment, setEditAssignment] = useState<AssignmentDto | null>(null);
  const [deleteAssignment, setDeleteAssignment] = useState<AssignmentDto | null>(null);

  const groups = useMemo(() =>
    groupsData?.content?.map((g) => ({ id: g.id, name: g.name })) || [],
    [groupsData],
  );

  const tests = useMemo(() =>
    testsData?.content?.map((item) => ({ id: item.id, title: item.title })) || [],
    [testsData],
  );

  // Client-side sort on current page
  const sortedRows = useMemo(() => {
    if (!data?.content) return [];
    return [...data.content].sort((a, b) => {
      let av: string | number | null = null;
      let bv: string | number | null = null;
      if (sortKey === 'title') { av = a.title ?? ''; bv = b.title ?? ''; }
      else if (sortKey === 'status') { av = a.status; bv = b.status; }
      else if (sortKey === 'startDate') { av = a.startDate ?? ''; bv = b.startDate ?? ''; }
      else if (sortKey === 'endDate') { av = a.endDate ?? ''; bv = b.endDate ?? ''; }
      else if (sortKey === 'completedStudents') { av = a.completedStudents; bv = b.completedStudents; }
      else if (sortKey === 'averageScore') { av = a.averageScore ?? -1; bv = b.averageScore ?? -1; }
      else if (sortKey === 'createdAt') { av = a.createdAt; bv = b.createdAt; }
      if (av === null) return 0;
      const cmp = av < bv! ? -1 : av > bv! ? 1 : 0;
      return sortDir === 'asc' ? cmp : -cmp;
    });
  }, [data?.content, sortKey, sortDir]);

  const toggleSort = (key: SortKey) => {
    if (key === sortKey) {
      setSortDir(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  };

  const handleCreate = () => { setEditAssignment(null); setFormOpen(true); };
  const handleEdit = (a: AssignmentDto) => { setEditAssignment(a); setFormOpen(true); };
  const handleDelete = (a: AssignmentDto) => setDeleteAssignment(a);
  const handleDeleteConfirm = () => {
    if (deleteAssignment) remove.mutate(deleteAssignment.id, { onSuccess: () => setDeleteAssignment(null) });
  };

  const handleFormSubmit = (formData: CreateAssignmentRequest) => {
    if (editAssignment) {
      update.mutate(
        { id: editAssignment.id, data: formData },
        { onSuccess: () => { setFormOpen(false); setEditAssignment(null); } },
      );
    } else {
      create.mutate(formData, { onSuccess: () => setFormOpen(false) });
    }
  };

  const SortLabel = ({ col }: { col: SortKey }) => (
    <TableSortLabel
      active={sortKey === col}
      direction={sortKey === col ? sortDir : 'asc'}
      onClick={() => toggleSort(col)}
    />
  );

  return (
    <PageShell
      title={t('title')}
      subtitle={t('subtitle')}
      actions={
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleCreate}>
          {t('create')}
        </Button>
      }
    >
      {/* Status tabs */}
      <Tabs
        value={statusTab}
        onChange={(_, v) => { setStatusTab(v); setPage(0); }}
        sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
        variant="scrollable"
        scrollButtons="auto"
      >
        <Tab label={t('allTab')} />
        {STATUS_TABS.slice(1).map((s) => (
          <Tab key={s} label={t(`status.${s}`)} />
        ))}
      </Tabs>

      {/* Search */}
      <Box sx={{ mb: 2 }}>
        <TextField
          size="small"
          placeholder={t('search')}
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon fontSize="small" />
              </InputAdornment>
            ),
          }}
          sx={{ minWidth: 260 }}
        />
      </Box>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : sortedRows.length === 0 ? (
        <EmptyState
          icon={<AssignmentIcon sx={{ fontSize: 'inherit' }} />}
          title={t('empty')}
          description={t('emptyDescription')}
          action={{ label: t('create'), onClick: handleCreate, icon: <AddIcon /> }}
        />
      ) : (
        <>
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>
                    {t('columns.title')}
                    <SortLabel col="title" />
                  </TableCell>
                  <TableCell>{t('groupName')}</TableCell>
                  <TableCell>
                    {t('columns.status')}
                    <SortLabel col="status" />
                  </TableCell>
                  <TableCell>
                    {t('columns.startDate')}
                    <SortLabel col="startDate" />
                  </TableCell>
                  <TableCell>
                    {t('columns.endDate')}
                    <SortLabel col="endDate" />
                  </TableCell>
                  <TableCell align="center">
                    {t('columns.completion')}
                    <SortLabel col="completedStudents" />
                  </TableCell>
                  <TableCell align="center">
                    {t('columns.avgScore')}
                    <SortLabel col="averageScore" />
                  </TableCell>
                  <TableCell align="right">{t('columns.actions')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {sortedRows.map((assignment) => (
                  <TableRow
                    key={assignment.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => navigate(`/assignments/${assignment.id}`)}
                  >
                    <TableCell
                      sx={{ fontWeight: 500, maxWidth: 220, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
                    >
                      {assignment.title}
                    </TableCell>
                    <TableCell sx={{ color: 'text.secondary', fontSize: '0.8125rem' }}>
                      {assignment.groupName}
                    </TableCell>
                    <TableCell>
                      <StatusBadge
                        status={assignment.status}
                        label={t(`status.${assignment.status}`)}
                      />
                    </TableCell>
                    <TableCell sx={{ fontSize: '0.8125rem', color: 'text.secondary' }}>
                      {formatDate(assignment.startDate)}
                    </TableCell>
                    <TableCell sx={{ fontSize: '0.8125rem', color: 'text.secondary' }}>
                      {formatDate(assignment.endDate)}
                    </TableCell>
                    <TableCell align="center" sx={{ fontSize: '0.8125rem' }}>
                      {assignment.completedStudents}/{assignment.totalStudents}
                    </TableCell>
                    <TableCell align="center" sx={{ fontSize: '0.8125rem', fontWeight: 500 }}>
                      {assignment.averageScore != null ? formatPercent(assignment.averageScore) : '-'}
                    </TableCell>
                    <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                      <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
                        {assignment.status === AssignmentStatus.DRAFT && (
                          <Tooltip title={t('activate')}>
                            <IconButton size="small" color="success" onClick={() => activate.mutate(assignment.id)}>
                              <PlayArrowIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        {assignment.status === AssignmentStatus.ACTIVE && (
                          <Tooltip title={t('monitor')}>
                            <IconButton size="small" color="primary" onClick={() => navigate(`/assignments/${assignment.id}/live`)}>
                              <MonitorIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        {[AssignmentStatus.COMPLETED, AssignmentStatus.ACTIVE].includes(assignment.status as AssignmentStatus) && (
                          <Tooltip title={t('results')}>
                            <IconButton size="small" onClick={() => navigate(`/assignments/${assignment.id}/results`)}>
                              <BarChartIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        {assignment.status === AssignmentStatus.DRAFT && (
                          <Tooltip title={t('common:edit')}>
                            <IconButton size="small" onClick={() => handleEdit(assignment)}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        {[AssignmentStatus.DRAFT, AssignmentStatus.SCHEDULED].includes(assignment.status as AssignmentStatus) && (
                          <Tooltip title={t('common:delete')}>
                            <IconButton size="small" color="error" onClick={() => handleDelete(assignment)}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                      </Box>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Box>

          {data && data.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
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

      {/* Mobile FAB */}
      <Fab color="primary" onClick={handleCreate} sx={{ position: 'fixed', bottom: 32, right: 32, display: { sm: 'none' } }}>
        <AddIcon />
      </Fab>

      <AssignmentFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleFormSubmit}
        assignment={editAssignment}
        isPending={create.isPending || update.isPending}
        groups={groups}
        tests={tests}
      />

      <AssignmentDeleteDialog
        open={Boolean(deleteAssignment)}
        onClose={() => setDeleteAssignment(null)}
        onConfirm={handleDeleteConfirm}
        assignment={deleteAssignment}
        isPending={remove.isPending}
      />
    </PageShell>
  );
}
