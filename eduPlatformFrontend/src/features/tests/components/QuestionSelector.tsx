import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box,
  TextField,
  MenuItem,
  Checkbox,
  Typography,
  Chip,
  Paper,
  Pagination,
  CircularProgress,
  Alert,
  Button,
  IconButton,
  Divider,
  Collapse,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import CloseIcon from '@mui/icons-material/Close';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import DeleteSweepIcon from '@mui/icons-material/DeleteSweep';
import { useQuestionsForSelection } from '../hooks/useTests';
import { useQuestionsByIds } from '@/features/questions/hooks/useQuestions';
import { resolveTranslation } from '@/utils/i18nUtils';
import { Difficulty } from '@/types/question';

interface QuestionSelectorProps {
  subjectId: string;
  topicIds: string[];
  selectedIds: string[];
  onSelectionChange: (ids: string[]) => void;
  onCreateQuestion?: () => void;
}

const difficultyColor = {
  EASY: 'success' as const,
  MEDIUM: 'warning' as const,
  HARD: 'error' as const,
};

const statusColor = {
  DRAFT: 'default' as const,
  PENDING: 'info' as const,
  APPROVED: 'primary' as const,
  ACTIVE: 'success' as const,
  REJECTED: 'error' as const,
  ARCHIVED: 'default' as const,
};

export default function QuestionSelector({ topicIds, selectedIds, onSelectionChange, onCreateQuestion }: QuestionSelectorProps) {
  const { t } = useTranslation('test');
  const { t: tQ } = useTranslation('question');

  const [filterDifficulty, setFilterDifficulty] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [page, setPage] = useState(0);
  const [selectedExpanded, setSelectedExpanded] = useState(true);

  const { data, isLoading, isError, error } = useQuestionsForSelection({
    topicIds,
    difficulty: filterDifficulty || undefined,
    status: filterStatus || undefined,
    page,
    size: 50,
  });

  const handleToggle = (questionId: string) => {
    if (selectedIds.includes(questionId)) {
      onSelectionChange(selectedIds.filter(id => id !== questionId));
    } else {
      onSelectionChange([...selectedIds, questionId]);
    }
  };

  const handleSelectAll = () => {
    if (!data?.content) return;
    const pageIds = data.content.map(q => q.id);
    const allSelected = pageIds.every(id => selectedIds.includes(id));
    if (allSelected) {
      onSelectionChange(selectedIds.filter(id => !pageIds.includes(id)));
    } else {
      const newIds = pageIds.filter(id => !selectedIds.includes(id));
      onSelectionChange([...selectedIds, ...newIds]);
    }
  };

  // Fetch all selected questions across pages for accurate counts and display
  const { data: allSelectedQuestions, isLoading: selectedLoading } = useQuestionsByIds(selectedIds);

  const allSelectedOnPage = data?.content
    ? data.content.length > 0 && data.content.every(q => selectedIds.includes(q.id))
    : false;

  // Count difficulties from all selected questions (cross-page accurate)
  const easySel = allSelectedQuestions?.filter(q => q.difficulty === Difficulty.EASY).length ?? 0;
  const medSel = allSelectedQuestions?.filter(q => q.difficulty === Difficulty.MEDIUM).length ?? 0;
  const hardSel = allSelectedQuestions?.filter(q => q.difficulty === Difficulty.HARD).length ?? 0;

  // Build ordered list of selected questions maintaining selection order
  const selectedQuestionsOrdered = selectedIds
    .map(id => allSelectedQuestions?.find(q => q.id === id))
    .filter(Boolean) as NonNullable<typeof allSelectedQuestions>[number][];

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {/* Selected Questions Panel — always visible at top */}
      <Paper
        variant="outlined"
        sx={{
          border: selectedIds.length > 0 ? '2px solid' : '1px solid',
          borderColor: selectedIds.length > 0 ? 'primary.main' : 'divider',
        }}
      >
        <Box
          sx={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            px: 2, py: 1, bgcolor: selectedIds.length > 0 ? 'primary.50' : 'action.hover',
            cursor: 'pointer',
          }}
          onClick={() => setSelectedExpanded(!selectedExpanded)}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {selectedExpanded ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
            <Typography variant="subtitle2" fontWeight={700}>
              {t('manual.selectedQuestions')}
            </Typography>
            <Chip
              size="small"
              label={selectedIds.length}
              color={selectedIds.length > 0 ? 'primary' : 'default'}
            />
            {selectedIds.length > 0 && (
              <Box sx={{ display: 'flex', gap: 0.5, ml: 1 }}>
                <Chip size="small" label={`${t('form.easy')}: ${easySel}`} color="success" variant="outlined" />
                <Chip size="small" label={`${t('form.medium')}: ${medSel}`} color="warning" variant="outlined" />
                <Chip size="small" label={`${t('form.hard')}: ${hardSel}`} color="error" variant="outlined" />
              </Box>
            )}
          </Box>
          {selectedIds.length > 0 && (
            <Button
              size="small"
              color="error"
              startIcon={<DeleteSweepIcon />}
              onClick={(e) => { e.stopPropagation(); onSelectionChange([]); }}
            >
              {t('manual.clearAll')}
            </Button>
          )}
        </Box>

        <Collapse in={selectedExpanded}>
          {selectedIds.length === 0 ? (
            <Box sx={{ px: 2, py: 3, textAlign: 'center' }}>
              <Typography variant="body2" color="text.secondary">
                {t('manual.noSelected')}
              </Typography>
            </Box>
          ) : selectedLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
              <CircularProgress size={24} />
            </Box>
          ) : (
            <Box sx={{ maxHeight: 300, overflow: 'auto' }}>
              {selectedQuestionsOrdered.map((question, idx) => (
                <Box
                  key={question.id}
                  sx={{
                    display: 'flex', alignItems: 'flex-start', gap: 1,
                    px: 2, py: 1, borderTop: '1px solid', borderColor: 'divider',
                    '&:hover': { bgcolor: 'action.hover' },
                  }}
                >
                  <Typography
                    variant="caption"
                    color="text.disabled"
                    sx={{ minWidth: 24, pt: 0.3, textAlign: 'right' }}
                  >
                    {idx + 1}.
                  </Typography>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography variant="body2" sx={{ wordBreak: 'break-word' }}>
                      {resolveTranslation(question.questionTextTranslations) || question.questionText}
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 0.5, mt: 0.5, flexWrap: 'wrap' }}>
                      <Chip
                        size="small"
                        label={tQ('difficulties.' + question.difficulty)}
                        color={difficultyColor[question.difficulty] || 'default'}
                        variant="outlined"
                      />
                      {question.topicName && (
                        <Chip
                          size="small"
                          label={resolveTranslation(question.topicNameTranslations) || question.topicName}
                          variant="outlined"
                          color="primary"
                        />
                      )}
                    </Box>
                  </Box>
                  <IconButton
                    size="small"
                    onClick={() => onSelectionChange(selectedIds.filter(id => id !== question.id))}
                    sx={{ flexShrink: 0, color: 'error.main' }}
                  >
                    <CloseIcon fontSize="small" />
                  </IconButton>
                </Box>
              ))}
            </Box>
          )}
        </Collapse>
      </Paper>

      <Divider />

      {/* Filters */}
      <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
        <TextField
          select
          label={t('manual.filterDifficulty')}
          value={filterDifficulty}
          onChange={(e) => { setFilterDifficulty(e.target.value); setPage(0); }}
          size="small"
          sx={{ minWidth: 150 }}
        >
          <MenuItem value="">{t('manual.filterAll')}</MenuItem>
          <MenuItem value="EASY">{t('form.easy')}</MenuItem>
          <MenuItem value="MEDIUM">{t('form.medium')}</MenuItem>
          <MenuItem value="HARD">{t('form.hard')}</MenuItem>
        </TextField>

        <TextField
          select
          label={t('manual.filterStatus')}
          value={filterStatus}
          onChange={(e) => { setFilterStatus(e.target.value); setPage(0); }}
          size="small"
          sx={{ minWidth: 150 }}
        >
          <MenuItem value="">{t('manual.filterAll')}</MenuItem>
          <MenuItem value="ACTIVE">{tQ('statuses.ACTIVE')}</MenuItem>
          <MenuItem value="DRAFT">{tQ('statuses.DRAFT')}</MenuItem>
          <MenuItem value="PENDING">{tQ('statuses.PENDING')}</MenuItem>
        </TextField>

        {onCreateQuestion && (
          <Button
            variant="outlined"
            size="small"
            startIcon={<AddIcon />}
            onClick={onCreateQuestion}
          >
            {t('manual.createQuestion')}
          </Button>
        )}
      </Box>

      {/* Question list — browse and select */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      ) : isError ? (
        <Alert severity="error">
          {(error as import('axios').AxiosError<{ message?: string }>)?.response?.data?.message
            || (error as Error)?.message
            || t('manual.noQuestionsFound')}
        </Alert>
      ) : !data?.content?.length ? (
        <Alert severity="warning">{t('manual.noQuestionsFound')}</Alert>
      ) : (
        <Paper variant="outlined" sx={{ maxHeight: 400, overflow: 'auto' }}>
          {/* Select All header */}
          <Box
            sx={{
              display: 'flex', alignItems: 'center', gap: 1,
              px: 2, py: 1, borderBottom: '1px solid', borderColor: 'divider',
              bgcolor: 'action.hover', position: 'sticky', top: 0, zIndex: 1,
            }}
          >
            <Checkbox
              checked={allSelectedOnPage}
              indeterminate={data.content.some(q => selectedIds.includes(q.id)) && !allSelectedOnPage}
              onChange={handleSelectAll}
              size="small"
            />
            <Typography variant="body2" fontWeight={600}>
              {t('manual.selectAllPage')}
            </Typography>
          </Box>

          {data.content.map((question) => (
            <Box
              key={question.id}
              sx={{
                display: 'flex', alignItems: 'flex-start', gap: 1,
                px: 2, py: 1.5, borderBottom: '1px solid', borderColor: 'divider',
                cursor: 'pointer',
                '&:hover': { bgcolor: 'action.hover' },
                bgcolor: selectedIds.includes(question.id) ? 'action.selected' : undefined,
              }}
              onClick={() => handleToggle(question.id)}
            >
              <Checkbox
                checked={selectedIds.includes(question.id)}
                size="small"
                sx={{ mt: -0.5 }}
              />
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="body2" sx={{ wordBreak: 'break-word' }}>
                  {resolveTranslation(question.questionTextTranslations) || question.questionText}
                </Typography>
                <Box sx={{ display: 'flex', gap: 0.5, mt: 0.5, flexWrap: 'wrap' }}>
                  <Chip
                    size="small"
                    label={tQ('difficulties.' + question.difficulty)}
                    color={difficultyColor[question.difficulty] || 'default'}
                    variant="outlined"
                  />
                  <Chip
                    size="small"
                    label={tQ('statuses.' + question.status)}
                    color={statusColor[question.status] || 'default'}
                    variant="outlined"
                  />
                  <Chip
                    size="small"
                    label={tQ('types.' + question.questionType)}
                    variant="outlined"
                  />
                  {question.topicName && (
                    <Chip
                      size="small"
                      label={resolveTranslation(question.topicNameTranslations) || question.topicName}
                      variant="outlined"
                      color="primary"
                    />
                  )}
                </Box>
              </Box>
            </Box>
          ))}
        </Paper>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 1 }}>
          <Pagination
            count={data.totalPages}
            page={page + 1}
            onChange={(_, value) => setPage(value - 1)}
            color="primary"
            size="small"
          />
        </Box>
      )}
    </Box>
  );
}
