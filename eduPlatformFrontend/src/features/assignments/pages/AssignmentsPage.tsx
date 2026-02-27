import { useState, useMemo } from 'react';
import {
  Box,
  Typography,
  TextField,
  InputAdornment,
  Grid,
  Fab,
  CircularProgress,
  Pagination,
  Tabs,
  Tab,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import AssignmentIcon from '@mui/icons-material/Assignment';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useAssignments } from '../hooks/useAssignments';
import { useAssignmentMutations } from '../hooks/useAssignmentMutations';
import { useGroups } from '@/features/groups/hooks/useGroups';
import { useTests } from '@/features/tests/hooks/useTests';
import AssignmentCard from '../components/AssignmentCard';
import AssignmentFormDialog from '../components/AssignmentFormDialog';
import AssignmentDeleteDialog from '../components/AssignmentDeleteDialog';
import type { AssignmentDto, CreateAssignmentRequest } from '@/types/assignment';
import { AssignmentStatus } from '@/types/assignment';

const STATUS_TABS: (AssignmentStatus | '')[] = ['', AssignmentStatus.DRAFT, AssignmentStatus.SCHEDULED, AssignmentStatus.ACTIVE, AssignmentStatus.COMPLETED, AssignmentStatus.CANCELLED];

export default function AssignmentsPage() {
  const { t } = useTranslation('assignment');
  const navigate = useNavigate();

  const [statusTab, setStatusTab] = useState(0);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);

  const params = useMemo(() => ({
    ...(search && { search }),
    ...(STATUS_TABS[statusTab] && { status: STATUS_TABS[statusTab] }),
    page,
    size: 12,
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

  const handleCreate = () => {
    setEditAssignment(null);
    setFormOpen(true);
  };

  const handleEdit = (assignment: AssignmentDto) => {
    setEditAssignment(assignment);
    setFormOpen(true);
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

  const handleDelete = (assignment: AssignmentDto) => {
    setDeleteAssignment(assignment);
  };

  const handleDeleteConfirm = () => {
    if (deleteAssignment) {
      remove.mutate(deleteAssignment.id, { onSuccess: () => setDeleteAssignment(null) });
    }
  };

  const handleActivate = (assignment: AssignmentDto) => {
    activate.mutate(assignment.id);
  };

  const handleCancel = (assignment: AssignmentDto) => {
    cancel.mutate(assignment.id);
  };

  const handleMonitor = (assignment: AssignmentDto) => {
    navigate(`/assignments/${assignment.id}/live`);
  };

  const handleResults = (assignment: AssignmentDto) => {
    navigate(`/assignments/${assignment.id}/results`);
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>{t('title')}</Typography>
          <Typography variant="body2" color="text.secondary">{t('subtitle')}</Typography>
        </Box>
      </Box>

      <Tabs
        value={statusTab}
        onChange={(_, v) => { setStatusTab(v); setPage(0); }}
        sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
      >
        <Tab label={t('allTab')} sx={{ textTransform: 'none' }} />
        <Tab label={t('status.DRAFT')} sx={{ textTransform: 'none' }} />
        <Tab label={t('status.SCHEDULED')} sx={{ textTransform: 'none' }} />
        <Tab label={t('status.ACTIVE')} sx={{ textTransform: 'none' }} />
        <Tab label={t('status.COMPLETED')} sx={{ textTransform: 'none' }} />
        <Tab label={t('status.CANCELLED')} sx={{ textTransform: 'none' }} />
      </Tabs>

      <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
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
          sx={{ minWidth: 240 }}
        />
      </Box>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : data && data.content.length > 0 ? (
        <>
          <Grid container spacing={2.5}>
            {data.content.map((assignment) => (
              <Grid item xs={12} sm={6} md={4} key={assignment.id}>
                <AssignmentCard
                  assignment={assignment}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onActivate={handleActivate}
                  onCancel={handleCancel}
                  onMonitor={handleMonitor}
                  onResults={handleResults}
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
          <Typography variant="h6" color="text.secondary">{t('empty')}</Typography>
          <Typography variant="body2" color="text.disabled">{t('emptyDescription')}</Typography>
        </Box>
      )}

      <Fab color="primary" onClick={handleCreate} sx={{ position: 'fixed', bottom: 32, right: 32 }}>
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
    </Box>
  );
}
