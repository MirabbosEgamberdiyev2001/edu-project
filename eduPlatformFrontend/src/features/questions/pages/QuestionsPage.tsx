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
  MenuItem,
  Button,
  Checkbox,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddIcon from '@mui/icons-material/Add';
import QuizIcon from '@mui/icons-material/Quiz';
import SendIcon from '@mui/icons-material/Send';
import { useTranslation } from 'react-i18next';
import { useQuestions } from '../hooks/useQuestions';
import { useQuestionMutations } from '../hooks/useQuestionMutations';
import QuestionCard from '../components/QuestionCard';
import QuestionFormDialog from '../components/QuestionFormDialog';
import QuestionDeleteDialog from '../components/QuestionDeleteDialog';
import { QuestionType, Difficulty, QuestionStatus } from '@/types/question';
import type { QuestionDto, CreateQuestionRequest, UpdateQuestionRequest, QuestionListParams } from '@/types/question';
import { useDebounce } from '@/features/subjects/hooks/useDebounce';

export default function QuestionsPage() {
  const { t } = useTranslation('question');

  const [search, setSearch] = useState('');
  const [questionType, setQuestionType] = useState<QuestionType | ''>('');
  const [difficulty, setDifficulty] = useState<Difficulty | ''>('');
  const [status, setStatus] = useState<QuestionStatus | ''>('');
  const [page, setPage] = useState(0);

  const debouncedSearch = useDebounce(search, 300);

  const params = useMemo<QuestionListParams>(() => ({
    ...(debouncedSearch && { search: debouncedSearch }),
    ...(questionType && { questionType }),
    ...(difficulty && { difficulty }),
    ...(status && { status }),
    page,
    size: 12,
  }), [debouncedSearch, questionType, difficulty, status, page]);

  const { data, isLoading } = useQuestions(params);
  const { create, update, remove, submitForModeration, bulkSubmit } = useQuestionMutations();

  const [formOpen, setFormOpen] = useState(false);
  const [editQuestion, setEditQuestion] = useState<QuestionDto | null>(null);
  const [deleteQuestion, setDeleteQuestion] = useState<QuestionDto | null>(null);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  const toggleSelect = (id: string) => {
    setSelectedIds(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    );
  };

  const toggleSelectAll = () => {
    if (!data?.content) return;
    const pageIds = data.content.map(q => q.id);
    const allSelected = pageIds.every(id => selectedIds.includes(id));
    if (allSelected) {
      setSelectedIds(prev => prev.filter(id => !pageIds.includes(id)));
    } else {
      const newIds = pageIds.filter(id => !selectedIds.includes(id));
      setSelectedIds(prev => [...prev, ...newIds]);
    }
  };

  const submittableSelected = useMemo(() => {
    if (!data?.content) return [];
    return data.content
      .filter(q => selectedIds.includes(q.id) && (q.status === QuestionStatus.DRAFT || q.status === QuestionStatus.REJECTED))
      .map(q => q.id);
  }, [data?.content, selectedIds]);

  const handleBulkSubmit = () => {
    if (submittableSelected.length === 0) return;
    bulkSubmit.mutate(submittableSelected, {
      onSuccess: () => setSelectedIds([]),
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
      remove.mutate(deleteQuestion.id, { onSuccess: () => setDeleteQuestion(null) });
    }
  };

  const handleSubmitForModeration = (question: QuestionDto) => {
    submitForModeration.mutate(question.id);
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>{t('title')}</Typography>
          <Typography variant="body2" color="text.secondary">{t('subtitle')}</Typography>
        </Box>
      </Box>

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

        <TextField
          select
          size="small"
          value={questionType}
          onChange={(e) => { setQuestionType(e.target.value as QuestionType | ''); setPage(0); }}
          sx={{ minWidth: 160 }}
        >
          <MenuItem value="">{t('filters.allTypes')}</MenuItem>
          {Object.values(QuestionType).map((qt) => (
            <MenuItem key={qt} value={qt}>{t(`types.${qt}`)}</MenuItem>
          ))}
        </TextField>

        <TextField
          select
          size="small"
          value={difficulty}
          onChange={(e) => { setDifficulty(e.target.value as Difficulty | ''); setPage(0); }}
          sx={{ minWidth: 150 }}
        >
          <MenuItem value="">{t('filters.allDifficulties')}</MenuItem>
          {Object.values(Difficulty).map((d) => (
            <MenuItem key={d} value={d}>{t(`difficulties.${d}`)}</MenuItem>
          ))}
        </TextField>

        <TextField
          select
          size="small"
          value={status}
          onChange={(e) => { setStatus(e.target.value as QuestionStatus | ''); setPage(0); }}
          sx={{ minWidth: 150 }}
        >
          <MenuItem value="">{t('filters.allStatuses')}</MenuItem>
          {Object.values(QuestionStatus).map((s) => (
            <MenuItem key={s} value={s}>{t(`statuses.${s}`)}</MenuItem>
          ))}
        </TextField>
      </Box>

      {/* Bulk action bar */}
      {selectedIds.length > 0 && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2, p: 1.5, bgcolor: 'action.selected', borderRadius: 1 }}>
          <Checkbox
            checked={data?.content ? data.content.every(q => selectedIds.includes(q.id)) : false}
            indeterminate={data?.content ? data.content.some(q => selectedIds.includes(q.id)) && !data.content.every(q => selectedIds.includes(q.id)) : false}
            onChange={toggleSelectAll}
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
            disabled={bulkSubmit.isPending || submittableSelected.length === 0}
          >
            {t('bulkSubmit')}{submittableSelected.length < selectedIds.length ? ` (${submittableSelected.length})` : ''}
          </Button>
          <Button size="small" onClick={() => setSelectedIds([])}>
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
          <Grid container spacing={2.5}>
            {data.content.map((question) => (
              <Grid item xs={12} sm={6} md={4} lg={3} key={question.id}>
                <Box sx={{ position: 'relative' }}>
                  <Checkbox
                    checked={selectedIds.includes(question.id)}
                    onChange={() => toggleSelect(question.id)}
                    size="small"
                    sx={{ position: 'absolute', top: 4, left: 4, zIndex: 1 }}
                  />
                  <QuestionCard
                    question={question}
                    onEdit={handleEdit}
                    onDelete={handleDelete}
                    onSubmit={handleSubmitForModeration}
                  />
                </Box>
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
          <QuizIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            {t('empty')}
          </Typography>
          <Typography variant="body2" color="text.disabled">
            {t('emptyDescription')}
          </Typography>
        </Box>
      )}

      <Fab
        color="primary"
        onClick={handleCreate}
        sx={{ position: 'fixed', bottom: 32, right: 32 }}
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
  );
}
