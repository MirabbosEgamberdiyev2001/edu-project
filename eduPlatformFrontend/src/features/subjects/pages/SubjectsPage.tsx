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
  MenuItem,
  Button,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import FilterListIcon from '@mui/icons-material/FilterList';
import { useTranslation } from 'react-i18next';
import { useSubjects, useArchivedSubjects } from '../hooks/useSubjects';
import { useSubjectMutations } from '../hooks/useSubjectMutations';
import { useAuthStore } from '@/stores/authStore';
import SubjectCard from '../components/SubjectCard';
import SubjectFormDialog from '../components/SubjectFormDialog';
import SubjectDeleteDialog from '../components/SubjectDeleteDialog';
import { type SubjectDto, type CreateSubjectRequest, type UpdateSubjectRequest } from '@/types/subject';
import { useDebounce } from '../hooks/useDebounce';

export default function SubjectsPage() {
  const { t } = useTranslation('subject');
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN';

  const [viewTab, setViewTab] = useState(0); // 0 = active, 1 = archived
  const [search, setSearch] = useState('');
  const [gradeFilter, setGradeFilter] = useState<number | ''>('');
  const [page, setPage] = useState(0);
  const [archivedPage, setArchivedPage] = useState(0);

  const debouncedSearch = useDebounce(search, 300);

  const params = useMemo(() => ({
    ...(debouncedSearch && { search: debouncedSearch }),
    page,
    size: 20,
  }), [debouncedSearch, page]);

  const { data, isLoading } = useSubjects(params);
  const { data: archivedData, isLoading: archivedLoading } = useArchivedSubjects({ page: archivedPage, size: 12 });
  const { create, update, remove, archive, restore } = useSubjectMutations();

  const [formOpen, setFormOpen] = useState(false);
  const [editSubject, setEditSubject] = useState<SubjectDto | null>(null);
  const [deleteSubject, setDeleteSubject] = useState<SubjectDto | null>(null);

  const hasActiveFilters = Boolean(debouncedSearch || gradeFilter !== '');

  // Client-side grade filter
  const filteredContent = useMemo(() => {
    if (!data?.content) return [];
    if (gradeFilter === '') return data.content;
    return data.content.filter(s => s.gradeLevel === gradeFilter);
  }, [data?.content, gradeFilter]);

  const handleCreate = () => {
    setEditSubject(null);
    setFormOpen(true);
  };

  const handleEdit = (subject: SubjectDto) => {
    setEditSubject(subject);
    setFormOpen(true);
  };

  const handleFormSubmit = (formData: CreateSubjectRequest | UpdateSubjectRequest) => {
    if (editSubject) {
      update.mutate({ id: editSubject.id, data: formData }, { onSuccess: () => setFormOpen(false) });
    } else {
      create.mutate(formData as CreateSubjectRequest, { onSuccess: () => setFormOpen(false) });
    }
  };

  const handleDelete = (subject: SubjectDto) => {
    setDeleteSubject(subject);
  };

  const handleDeleteConfirm = () => {
    if (deleteSubject) {
      remove.mutate(deleteSubject.id, { onSuccess: () => setDeleteSubject(null) });
    }
  };

  const handleArchive = (subject: SubjectDto) => {
    archive.mutate(subject.id);
  };

  const handleRestore = (subject: SubjectDto) => {
    restore.mutate(subject.id);
  };

  const clearFilters = () => {
    setSearch('');
    setGradeFilter('');
    setPage(0);
  };

  const isActive = viewTab === 0;
  const currentLoading = isActive ? isLoading : archivedLoading;
  const displayContent = isActive ? filteredContent : (archivedData?.content || []);
  const currentPage = isActive ? page : archivedPage;
  const setCurrentPage = isActive ? setPage : setArchivedPage;
  const totalPages = isActive ? (data?.totalPages || 0) : (archivedData?.totalPages || 0);

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>{t('title')}</Typography>
          <Typography variant="body2" color="text.secondary">{t('subtitle')}</Typography>
        </Box>
        {isActive && isAdmin && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={handleCreate}
            sx={{ display: { xs: 'none', sm: 'flex' } }}
          >
            {t('create')}
          </Button>
        )}
      </Box>

      {isAdmin && (
        <Tabs
          value={viewTab}
          onChange={(_, v) => { setViewTab(v); }}
          sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
        >
          <Tab label={t('activeTab')} sx={{ textTransform: 'none' }} />
          <Tab label={t('archivedTab')} sx={{ textTransform: 'none' }} />
        </Tabs>
      )}

      {isActive && (
        <Box sx={{ display: 'flex', gap: 1.5, mb: 3, flexWrap: 'wrap', alignItems: 'center' }}>
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
            sx={{ minWidth: 220, flex: { xs: 1, sm: 'none' } }}
          />

          <TextField
            select
            size="small"
            label={t('form.gradeLevel')}
            value={gradeFilter}
            onChange={(e) => { setGradeFilter(e.target.value === '' ? '' : Number(e.target.value)); }}
            sx={{ minWidth: 120 }}
          >
            <MenuItem value="">{t('allGrades', 'All grades')}</MenuItem>
            {Array.from({ length: 11 }, (_, i) => i + 1).map((g) => (
              <MenuItem key={g} value={g}>{g}</MenuItem>
            ))}
          </TextField>

          {hasActiveFilters && (
            <Button size="small" onClick={clearFilters} startIcon={<FilterListIcon />}>
              {t('clearFilters', 'Clear')}
            </Button>
          )}
        </Box>
      )}

      {currentLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : displayContent.length > 0 ? (
        <>
          <Grid container spacing={2.5}>
            {displayContent.map((subject) => (
              <Grid item xs={12} sm={6} md={4} lg={3} key={subject.id}>
                <SubjectCard
                  subject={subject}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onArchive={handleArchive}
                  onRestore={handleRestore}
                />
              </Grid>
            ))}
          </Grid>
          {totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
              <Pagination
                count={totalPages}
                page={currentPage + 1}
                onChange={(_, p) => setCurrentPage(p - 1)}
                color="primary"
              />
            </Box>
          )}
        </>
      ) : (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <MenuBookIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            {hasActiveFilters
              ? t('emptyFiltered', 'No subjects match your filters')
              : isActive ? t('empty') : t('emptyArchived')}
          </Typography>
          <Typography variant="body2" color="text.disabled" sx={{ mb: 2 }}>
            {hasActiveFilters
              ? t('emptyFilteredDescription', 'Try adjusting your search or filters')
              : isActive ? t('emptyDescription') : t('emptyArchivedDescription')}
          </Typography>
          {hasActiveFilters ? (
            <Button variant="outlined" onClick={clearFilters} startIcon={<FilterListIcon />}>
              {t('clearFilters', 'Clear filters')}
            </Button>
          ) : isActive && isAdmin ? (
            <Button variant="contained" startIcon={<AddIcon />} onClick={handleCreate}>
              {t('create')}
            </Button>
          ) : null}
        </Box>
      )}

      {/* Mobile FAB */}
      {isActive && isAdmin && (
        <Fab
          color="primary"
          onClick={handleCreate}
          sx={{
            position: 'fixed',
            bottom: 32,
            right: 32,
            display: { xs: 'flex', sm: 'none' },
          }}
        >
          <AddIcon />
        </Fab>
      )}

      <SubjectFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleFormSubmit}
        subject={editSubject}
        isPending={create.isPending || update.isPending}
      />

      <SubjectDeleteDialog
        open={Boolean(deleteSubject)}
        onClose={() => setDeleteSubject(null)}
        onConfirm={handleDeleteConfirm}
        subject={deleteSubject}
        isPending={remove.isPending}
      />
    </Box>
  );
}
