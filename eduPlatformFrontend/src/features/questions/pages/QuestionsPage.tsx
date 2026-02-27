import { useState, useMemo, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Box,
  Typography,
  TextField,
  InputAdornment,
  Grid,
  Fab,
  CircularProgress,
  Pagination,
  MenuItem,
  Button,
  Checkbox,
  FormControl,
  Select,
  InputLabel,
  SelectChangeEvent,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import QuizIcon from '@mui/icons-material/Quiz';
import SendIcon from '@mui/icons-material/Send';
import FilterListIcon from '@mui/icons-material/FilterList';
import { useTranslation } from 'react-i18next';
import { PageShell, EmptyState } from '@/components/ui';
import { useQuestions } from '../hooks/useQuestions';
import { useQuestionMutations } from '../hooks/useQuestionMutations';
import QuestionCard from '../components/QuestionCard';
import QuestionFormDialog from '../components/QuestionFormDialog';
import QuestionDeleteDialog from '../components/QuestionDeleteDialog';
import { QuestionType, Difficulty, QuestionStatus } from '@/types/question';
import type { QuestionDto, CreateQuestionRequest, UpdateQuestionRequest, QuestionListParams } from '@/types/question';
import { useDebounce } from '@/features/subjects/hooks/useDebounce';

const PAGE_SIZES = [12, 24, 48];

// ─── Selection state management ───────────────────────────────────────────────
// Architecture:
//   selectedIds: string[]          — SINGLE source of truth (ID-based, global across pages)
//   selectedSet: Set<string>       — derived O(1) lookup (useMemo from selectedIds)
//   statusCache: Record<id,status> — populated from page data as user browses pages
//   submittableIds: string[]       — derived synchronously from selectedIds + statusCache
//
// No extra API calls. No async derivation. No stale state.
// ──────────────────────────────────────────────────────────────────────────────

export default function QuestionsPage() {
  const { t } = useTranslation('question');
  const [searchParams, setSearchParams] = useSearchParams();

  // ── Filter state (URL-synced) ──
  const [search, setSearch] = useState(searchParams.get('q') || '');
  const [questionType, setQuestionType] = useState<QuestionType | ''>(
    (searchParams.get('type') as QuestionType) || ''
  );
  const [difficulty, setDifficulty] = useState<Difficulty | ''>(
    (searchParams.get('diff') as Difficulty) || ''
  );
  const [status, setStatus] = useState<QuestionStatus | ''>(
    (searchParams.get('status') as QuestionStatus) || ''
  );
  const [page, setPage] = useState(Number(searchParams.get('page') || '0'));
  const [pageSize, setPageSize] = useState(Number(searchParams.get('size') || '12'));

  const debouncedSearch = useDebounce(search, 300);

  // Sync URL params when filters change
  useEffect(() => {
    const p = new URLSearchParams();
    if (debouncedSearch) p.set('q', debouncedSearch);
    if (questionType) p.set('type', questionType);
    if (difficulty) p.set('diff', difficulty);
    if (status) p.set('status', status);
    if (page > 0) p.set('page', String(page));
    if (pageSize !== 12) p.set('size', String(pageSize));
    setSearchParams(p, { replace: true });
  }, [debouncedSearch, questionType, difficulty, status, page, pageSize, setSearchParams]);

  const params = useMemo<QuestionListParams>(() => ({
    ...(debouncedSearch && { search: debouncedSearch }),
    ...(questionType && { questionType }),
    ...(difficulty && { difficulty }),
    ...(status && { status }),
    page,
    size: pageSize,
  }), [debouncedSearch, questionType, difficulty, status, page, pageSize]);

  const { data, isLoading } = useQuestions(params);
  const { create, update, remove, submitForModeration, bulkSubmit } = useQuestionMutations();

  // ── Selection state (single source of truth) ──
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [statusCache, setStatusCache] = useState<Record<string, QuestionStatus>>({});

  // O(1) lookup set — derived, never stored separately
  const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);

  // Populate status cache from page data as user browses
  useEffect(() => {
    if (!data?.content) return;
    setStatusCache(prev => {
      let changed = false;
      const updates: Record<string, QuestionStatus> = {};
      for (const q of data.content) {
        if (prev[q.id] !== q.status) {
          updates[q.id] = q.status;
          changed = true;
        }
      }
      return changed ? { ...prev, ...updates } : prev;
    });
  }, [data?.content]);

  // Clear selection when filters change (NOT on page change — cross-page selection is intentional)
  useEffect(() => {
    setSelectedIds([]);
  }, [debouncedSearch, questionType, difficulty, status]);

  // Submittable IDs — synchronous derivation, no API call, no loading flash
  const submittableIds = useMemo(() => {
    return selectedIds.filter(id => {
      const s = statusCache[id];
      return s === QuestionStatus.DRAFT || s === QuestionStatus.REJECTED;
    });
  }, [selectedIds, statusCache]);

  // ── Selection actions ──
  const toggleSelect = useCallback((id: string) => {
    setSelectedIds(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    );
  }, []);

  const toggleSelectAllOnPage = useCallback(() => {
    if (!data?.content) return;
    const pageIds = data.content.map(q => q.id);
    setSelectedIds(prev => {
      const prevSet = new Set(prev);
      const allPageSelected = pageIds.every(id => prevSet.has(id));
      if (allPageSelected) {
        // Deselect current page
        return prev.filter(id => !pageIds.includes(id));
      } else {
        // Add missing current page IDs
        const newIds = pageIds.filter(id => !prevSet.has(id));
        return [...prev, ...newIds];
      }
    });
  }, [data?.content]);

  const clearSelection = useCallback(() => {
    setSelectedIds([]);
  }, []);

  // ── Bulk action bar state — all derived from selectedSet ──
  const pageIds = useMemo(
    () => data?.content?.map(q => q.id) ?? [],
    [data?.content]
  );
  const pageSelectedCount = useMemo(
    () => pageIds.filter(id => selectedSet.has(id)).length,
    [pageIds, selectedSet]
  );
  const allPageSelected = pageIds.length > 0 && pageSelectedCount === pageIds.length;
  const somePageSelected = pageSelectedCount > 0 && !allPageSelected;

  // ── Other UI state ──
  const [formOpen, setFormOpen] = useState(false);
  const [editQuestion, setEditQuestion] = useState<QuestionDto | null>(null);
  const [deleteQuestion, setDeleteQuestion] = useState<QuestionDto | null>(null);

  const hasActiveFilters = Boolean(questionType || difficulty || status || debouncedSearch);

  const handleBulkSubmit = () => {
    if (submittableIds.length === 0) return;
    bulkSubmit.mutate(submittableIds, {
      onSuccess: () => {
        setSelectedIds([]);
        setStatusCache({});
      },
    });
  };

  const handleCreate = () => {
    setEditQuestion(null);
    setFormOpen(true);
  };

  const handleEdit = (question: QuestionDto) => {
    setEditQuestion(question);
    setFormOpen(true);
  };

  const handleFormSubmit = (formData: CreateQuestionRequest | UpdateQuestionRequest) => {
    if (editQuestion) {
      update.mutate(
        { id: editQuestion.id, data: formData as UpdateQuestionRequest },
        { onSuccess: () => setFormOpen(false) },
      );
    } else {
      create.mutate(
        formData as CreateQuestionRequest,
        { onSuccess: () => setFormOpen(false) },
      );
    }
  };

  const handleDelete = (question: QuestionDto) => {
    setDeleteQuestion(question);
  };

  const handleDeleteConfirm = () => {
    if (deleteQuestion) {
      remove.mutate(deleteQuestion.id, {
        onSuccess: () => {
          setDeleteQuestion(null);
          // Remove deleted question from selection if present
          setSelectedIds(prev => prev.filter(id => id !== deleteQuestion.id));
        },
      });
    }
  };

  const handleSubmitForModeration = (question: QuestionDto) => {
    submitForModeration.mutate(question.id);
  };

  const clearFilters = () => {
    setQuestionType('');
    setDifficulty('');
    setStatus('');
    setSearch('');
    setPage(0);
  };

  return (
    <PageShell
      title={t('title')}
      subtitle={t('subtitle')}
      actions={
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleCreate}
          sx={{ display: { xs: 'none', sm: 'flex' } }}
        >
          {t('create')}
        </Button>
      }
    >
    <Box>

      {/* Filters */}
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
          label={t('form.questionType')}
          value={questionType}
          onChange={(e) => { setQuestionType(e.target.value as QuestionType | ''); setPage(0); }}
          sx={{ minWidth: 150 }}
        >
          <MenuItem value="">{t('filters.allTypes')}</MenuItem>
          {Object.values(QuestionType).map((qt) => (
            <MenuItem key={qt} value={qt}>{t(`types.${qt}`)}</MenuItem>
          ))}
        </TextField>

        <TextField
          select
          size="small"
          label={t('form.difficulty')}
          value={difficulty}
          onChange={(e) => { setDifficulty(e.target.value as Difficulty | ''); setPage(0); }}
          sx={{ minWidth: 140 }}
        >
          <MenuItem value="">{t('filters.allDifficulties')}</MenuItem>
          {Object.values(Difficulty).map((d) => (
            <MenuItem key={d} value={d}>{t(`difficulties.${d}`)}</MenuItem>
          ))}
        </TextField>

        <TextField
          select
          size="small"
          label={t('filters.status')}
          value={status}
          onChange={(e) => { setStatus(e.target.value as QuestionStatus | ''); setPage(0); }}
          sx={{ minWidth: 140 }}
        >
          <MenuItem value="">{t('filters.allStatuses')}</MenuItem>
          {Object.values(QuestionStatus).map((s) => (
            <MenuItem key={s} value={s}>{t(`statuses.${s}`)}</MenuItem>
          ))}
        </TextField>

        {hasActiveFilters && (
          <Button size="small" onClick={clearFilters} startIcon={<FilterListIcon />}>
            {t('filters.clear', 'Clear')}
          </Button>
        )}
      </Box>

      {/* Bulk action bar — shown ONLY when selectedIds has items */}
      {selectedIds.length > 0 && (
        <Box sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 2,
          mb: 2,
          p: 1.5,
          bgcolor: 'primary.50',
          border: 1,
          borderColor: 'primary.200',
          borderRadius: 1,
        }}>
          <Checkbox
            checked={allPageSelected}
            indeterminate={somePageSelected}
            onChange={toggleSelectAllOnPage}
            size="small"
          />
          <Typography variant="body2" fontWeight={600}>
            {t('selectedCount', { count: selectedIds.length })}
          </Typography>
          <Button
            variant="contained"
            size="small"
            startIcon={bulkSubmit.isPending ? <CircularProgress size={16} /> : <SendIcon />}
            onClick={handleBulkSubmit}
            disabled={bulkSubmit.isPending || submittableIds.length === 0}
          >
            {t('bulkSubmit')}{submittableIds.length < selectedIds.length ? ` (${submittableIds.length})` : ''}
          </Button>
          <Button size="small" onClick={clearSelection}>
            {t('common:cancel', 'Cancel')}
          </Button>
        </Box>
      )}

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : data && data.content.length > 0 ? (
        <>
          {/* Total count */}
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            {t('totalCount', { count: data.totalElements, defaultValue: '{{count}} questions total' })}
          </Typography>

          <Grid container spacing={2}>
            {data.content.map((question) => (
              <Grid item xs={12} sm={6} md={4} lg={3} key={question.id}>
                <QuestionCard
                  question={question}
                  selected={selectedSet.has(question.id)}
                  selectable
                  onSelect={toggleSelect}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  onSubmit={handleSubmitForModeration}
                />
              </Grid>
            ))}
          </Grid>

          {/* Pagination with total & page size */}
          <Box sx={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            gap: 2,
            mt: 3,
            flexWrap: 'wrap',
          }}>
            {data.totalPages > 1 && (
              <Pagination
                count={data.totalPages}
                page={page + 1}
                onChange={(_, p) => setPage(p - 1)}
                color="primary"
                size="medium"
              />
            )}
            <FormControl size="small" sx={{ minWidth: 80 }}>
              <InputLabel>{t('filters.pageSize', 'Size')}</InputLabel>
              <Select
                value={String(pageSize)}
                label={t('filters.pageSize', 'Size')}
                onChange={(e: SelectChangeEvent) => {
                  setPageSize(Number(e.target.value));
                  setPage(0);
                }}
              >
                {PAGE_SIZES.map(s => (
                  <MenuItem key={s} value={String(s)}>{s}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </>
      ) : (
        <EmptyState
          icon={<QuizIcon sx={{ fontSize: 'inherit' }} />}
          title={hasActiveFilters ? t('emptyFiltered', 'No questions match your filters') : t('empty')}
          description={hasActiveFilters ? t('emptyFilteredDescription', 'Try adjusting your filters') : t('emptyDescription')}
          action={hasActiveFilters ? {
            label: t('filters.clear', 'Clear filters'),
            onClick: clearFilters,
            icon: <FilterListIcon />,
          } : {
            label: t('create'),
            onClick: handleCreate,
            icon: <AddIcon />,
          }}
        />
      )}

      {/* Mobile FAB */}
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

      <QuestionFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleFormSubmit}
        question={editQuestion}
        isPending={create.isPending || update.isPending}
      />

      <QuestionDeleteDialog
        open={Boolean(deleteQuestion)}
        onClose={() => setDeleteQuestion(null)}
        onConfirm={handleDeleteConfirm}
        question={deleteQuestion}
        isPending={remove.isPending}
      />
    </Box>
    </PageShell>
  );
}
