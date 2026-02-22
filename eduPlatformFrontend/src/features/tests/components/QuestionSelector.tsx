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
} from '@mui/material';
import { useQuestionsForSelection } from '../hooks/useTests';
import { resolveTranslation } from '@/utils/i18nUtils';
import { Difficulty, QuestionStatus } from '@/types/question';

interface QuestionSelectorProps {
  subjectId: string;
  topicIds: string[];
  selectedIds: string[];
  onSelectionChange: (ids: string[]) => void;
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

export default function QuestionSelector({ topicIds, selectedIds, onSelectionChange }: QuestionSelectorProps) {
  const { t } = useTranslation('test');

  const [filterDifficulty, setFilterDifficulty] = useState('');
  const [filterStatus, setFilterStatus] = useState('ACTIVE');
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuestionsForSelection({
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

  // Summary counts
  const selectedQuestions = data?.content?.filter(q => selectedIds.includes(q.id)) ?? [];
  const allSelectedOnPage = data?.content
    ? data.content.length > 0 && data.content.every(q => selectedIds.includes(q.id))
    : false;

  // Count difficulties in selected (across all pages we can only count what we have info for)
  const easySel = selectedIds.length > 0 ? selectedQuestions.filter(q => q.difficulty === Difficulty.EASY).length : 0;
  const medSel = selectedIds.length > 0 ? selectedQuestions.filter(q => q.difficulty === Difficulty.MEDIUM).length : 0;
  const hardSel = selectedIds.length > 0 ? selectedQuestions.filter(q => q.difficulty === Difficulty.HARD).length : 0;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      {/* Filters */}
      <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
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
          <MenuItem value="ACTIVE">ACTIVE</MenuItem>
          <MenuItem value="DRAFT">DRAFT</MenuItem>
          <MenuItem value="PENDING">PENDING</MenuItem>
        </TextField>
      </Box>

      {/* Summary */}
      <Alert severity="info" sx={{ py: 0.5 }}>
        {t('manual.selectedCount', { count: selectedIds.length })}
        {selectedIds.length > 0 && (
          <Box component="span" sx={{ ml: 1 }}>
            ({t('form.easy')}: {easySel}, {t('form.medium')}: {medSel}, {t('form.hard')}: {hardSel})
          </Box>
        )}
      </Alert>

      {/* Question list */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
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
                    label={question.difficulty}
                    color={difficultyColor[question.difficulty] || 'default'}
                    variant="outlined"
                  />
                  <Chip
                    size="small"
                    label={question.status}
                    color={statusColor[question.status] || 'default'}
                    variant="outlined"
                  />
                  <Chip
                    size="small"
                    label={question.questionType}
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
