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
import GroupIcon from '@mui/icons-material/Group';
import { useTranslation } from 'react-i18next';
import { useGroups } from '../hooks/useGroups';
import { useGroupMutations } from '../hooks/useGroupMutations';
import GroupCard from '../components/GroupCard';
import GroupFormDialog from '../components/GroupFormDialog';
import GroupDeleteDialog from '../components/GroupDeleteDialog';
import { GroupStatus, type GroupDto, type CreateGroupRequest, type UpdateGroupRequest } from '@/types/group';
import { useDebounce } from '@/features/subjects/hooks/useDebounce';

export default function GroupsPage() {
  const { t } = useTranslation('group');

  const [viewTab, setViewTab] = useState(0);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [archivedPage, setArchivedPage] = useState(0);

  const debouncedSearch = useDebounce(search, 300);

  const activeParams = useMemo(() => ({
    status: GroupStatus.ACTIVE,
    ...(debouncedSearch && { search: debouncedSearch }),
    page,
    size: 12,
  }), [debouncedSearch, page]);

  const archivedParams = useMemo(() => ({
    status: GroupStatus.ARCHIVED,
    page: archivedPage,
    size: 12,
  }), [archivedPage]);

  const { data, isLoading } = useGroups(activeParams);
  const { data: archivedData, isLoading: archivedLoading } = useGroups(archivedParams);
  const { create, update, remove, archive } = useGroupMutations();

  const [formOpen, setFormOpen] = useState(false);
  const [editGroup, setEditGroup] = useState<GroupDto | null>(null);
  const [deleteGroup, setDeleteGroup] = useState<GroupDto | null>(null);

  const handleCreate = () => {
    setEditGroup(null);
    setFormOpen(true);
  };

  const handleEdit = (group: GroupDto) => {
    setEditGroup(group);
    setFormOpen(true);
  };

  const handleFormSubmit = (formData: CreateGroupRequest | UpdateGroupRequest) => {
    if (editGroup) {
      update.mutate({ id: editGroup.id, data: formData }, { onSuccess: () => setFormOpen(false) });
    } else {
      create.mutate(formData as CreateGroupRequest, { onSuccess: () => setFormOpen(false) });
    }
  };

  const handleDelete = (group: GroupDto) => {
    setDeleteGroup(group);
  };

  const handleDeleteConfirm = () => {
    if (deleteGroup) {
      remove.mutate(deleteGroup.id, { onSuccess: () => setDeleteGroup(null) });
    }
  };

  const handleArchive = (group: GroupDto) => {
    archive.mutate(group.id);
  };

  const isActive = viewTab === 0;
  const currentData = isActive ? data : archivedData;
  const currentLoading = isActive ? isLoading : archivedLoading;
  const currentPage = isActive ? page : archivedPage;
  const setCurrentPage = isActive ? setPage : setArchivedPage;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>{t('title')}</Typography>
          <Typography variant="body2" color="text.secondary">{t('subtitle')}</Typography>
        </Box>
      </Box>

      <Tabs
        value={viewTab}
        onChange={(_, v) => setViewTab(v)}
        sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
      >
        <Tab label={t('activeTab')} sx={{ textTransform: 'none' }} />
        <Tab label={t('archivedTab')} sx={{ textTransform: 'none' }} />
      </Tabs>

      {isActive && (
        <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap', alignItems: 'center' }}>
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
      )}

      {currentLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : currentData && currentData.content.length > 0 ? (
        <>
          <Grid container spacing={2.5}>
            {currentData.content.map((group) => (
              <Grid item xs={12} sm={6} md={4} lg={3} key={group.id}>
                <GroupCard
                  group={group}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onArchive={handleArchive}
                />
              </Grid>
            ))}
          </Grid>
          {currentData.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
              <Pagination
                count={currentData.totalPages}
                page={currentPage + 1}
                onChange={(_, p) => setCurrentPage(p - 1)}
                color="primary"
              />
            </Box>
          )}
        </>
      ) : (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <GroupIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            {isActive ? t('empty') : t('emptyArchived')}
          </Typography>
          <Typography variant="body2" color="text.disabled">
            {isActive ? t('emptyDescription') : t('emptyArchivedDescription')}
          </Typography>
        </Box>
      )}

      {isActive && (
        <Fab
          color="primary"
          onClick={handleCreate}
          sx={{ position: 'fixed', bottom: 32, right: 32 }}
        >
          <AddIcon />
        </Fab>
      )}

      <GroupFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleFormSubmit}
        group={editGroup}
        isPending={create.isPending || update.isPending}
      />

      <GroupDeleteDialog
        open={Boolean(deleteGroup)}
        onClose={() => setDeleteGroup(null)}
        onConfirm={handleDeleteConfirm}
        group={deleteGroup}
        isPending={remove.isPending}
      />
    </Box>
  );
}
