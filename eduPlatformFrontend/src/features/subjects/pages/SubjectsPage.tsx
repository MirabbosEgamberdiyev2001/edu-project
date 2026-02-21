import { useState, useMemo } from 'react';
import {
  Box,
  Typography,
  TextField,
  InputAdornment,
  ToggleButtonGroup,
  ToggleButton,
  Grid,
  Fab,
  CircularProgress,
  Pagination,
  Tabs,
  Tab,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import { useTranslation } from 'react-i18next';
import { useSubjects, useArchivedSubjects } from '../hooks/useSubjects';
import { useSubjectMutations } from '../hooks/useSubjectMutations';
import SubjectCard from '../components/SubjectCard';
import SubjectFormDialog from '../components/SubjectFormDialog';
import SubjectDeleteDialog from '../components/SubjectDeleteDialog';
import { SubjectCategory, type SubjectDto, type CreateSubjectRequest, type UpdateSubjectRequest } from '@/types/subject';
import { useDebounce } from '../hooks/useDebounce';

const CATEGORIES = ['', ...Object.values(SubjectCategory)] as const;

export default function SubjectsPage() {
  const { t } = useTranslation('subject');

  const [viewTab, setViewTab] = useState(0); // 0 = active, 1 = archived
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState<SubjectCategory | ''>('');
  const [page, setPage] = useState(0);
  const [archivedPage, setArchivedPage] = useState(0);

  const debouncedSearch = useDebounce(search, 300);

  const params = useMemo(() => ({
    ...(debouncedSearch && { search: debouncedSearch }),
    ...(category && { category }),
    page,
    size: 12,
  }), [debouncedSearch, category, page]);

  const { data, isLoading } = useSubjects(params);
  const { data: archivedData, isLoading: archivedLoading } = useArchivedSubjects({ page: archivedPage, size: 12 });
  const { create, update, remove, archive, restore } = useSubjectMutations();

  const [formOpen, setFormOpen] = useState(false);
  const [editSubject, setEditSubject] = useState<SubjectDto | null>(null);
  const [deleteSubject, setDeleteSubject] = useState<SubjectDto | null>(null);

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
        onChange={(_, v) => { setViewTab(v); }}
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
          <ToggleButtonGroup
            value={category}
            exclusive
            onChange={(_, val) => { setCategory(val); setPage(0); }}
            size="small"
          >
            {CATEGORIES.map((cat) => (
              <ToggleButton key={cat || 'all'} value={cat} sx={{ textTransform: 'none', px: 2 }}>
                {cat ? t(`categories.${cat}`) : t('allCategories')}
              </ToggleButton>
            ))}
          </ToggleButtonGroup>
        </Box>
      )}

      {currentLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : currentData && currentData.content.length > 0 ? (
        <>
          <Grid container spacing={2.5}>
            {currentData.content.map((subject) => (
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
          <MenuBookIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
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
