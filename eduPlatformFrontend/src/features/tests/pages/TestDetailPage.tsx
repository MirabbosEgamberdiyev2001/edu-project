import { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Typography,
  Paper,
  Button,
  CircularProgress,
  Chip,
  Divider,
  IconButton,
  Tooltip,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import DeleteIcon from '@mui/icons-material/Delete';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import RefreshIcon from '@mui/icons-material/Refresh';
import SendIcon from '@mui/icons-material/Send';
import { useTestDetail } from '../hooks/useTests';
import { useTestMutations } from '../hooks/useTestMutations';
import { useQuestionMutations } from '@/features/questions/hooks/useQuestionMutations';
import { useQuestionsByIds } from '@/features/questions/hooks/useQuestions';
import { QuestionStatus } from '@/types/question';
import TestExportButtons from '../components/TestExportButtons';
import TestDeleteDialog from '../components/TestDeleteDialog';
import SubmitToModerationDialog from '../components/SubmitToModerationDialog';

const STATUS_COLORS: Record<string, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  CREATED: 'info',
  GENERATING: 'warning',
  READY: 'success',
  DOWNLOADED: 'success',
  DELETED: 'error',
};

export default function TestDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('test');

  const { data: test, isLoading } = useTestDetail(id!);
  const { remove, duplicate, regenerate } = useTestMutations();
  const { bulkSubmit } = useQuestionMutations();

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [submitOpen, setSubmitOpen] = useState(false);

  const uniqueQuestionIds = useMemo(() => {
    if (!test?.variants) return [];
    const ids = new Set<string>();
    test.variants.forEach(v => v.questionIds.forEach(qid => ids.add(qid)));
    return Array.from(ids);
  }, [test?.variants]);

  const { data: questionsData, isLoading: questionsLoading } = useQuestionsByIds(uniqueQuestionIds);

  const submittableQuestionIds = useMemo(() => {
    if (!questionsData) return [];
    return questionsData
      .filter(q => q.status === QuestionStatus.DRAFT || q.status === QuestionStatus.REJECTED)
      .map(q => q.id);
  }, [questionsData]);

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!test) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography variant="h6" color="text.secondary">{t('empty.title')}</Typography>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/tests')} sx={{ mt: 2 }}>
          {t('common:back')}
        </Button>
      </Box>
    );
  }

  const handleDeleteConfirm = () => {
    remove.mutate(test.id, {
      onSuccess: () => navigate('/tests'),
    });
  };

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
        <IconButton onClick={() => navigate('/tests')}>
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h5" fontWeight={700} sx={{ flex: 1 }}>
          {test.title}
        </Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          {uniqueQuestionIds.length > 0 && (
            <Tooltip title={t('submitToModeration')}>
              <IconButton color="primary" onClick={() => setSubmitOpen(true)} disabled={bulkSubmit.isPending}>
                <SendIcon />
              </IconButton>
            </Tooltip>
          )}
          <Tooltip title={t('regenerate')}>
            <IconButton onClick={() => regenerate.mutate(test.id)} disabled={regenerate.isPending}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title={t('duplicate')}>
            <IconButton onClick={() => duplicate.mutate(test.id)} disabled={duplicate.isPending}>
              <ContentCopyIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title={t('delete.title')}>
            <IconButton color="error" onClick={() => setDeleteOpen(true)}>
              <DeleteIcon />
            </IconButton>
          </Tooltip>
        </Box>
      </Box>

      {/* Test Info */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="subtitle2" gutterBottom>{t('detail.info')}</Typography>
        <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
          <Chip label={t(`status.${test.status}`)} color={STATUS_COLORS[test.status] || 'default'} />
          <Chip label={t('card.questions', { count: test.questionCount })} variant="outlined" />
          <Chip label={t('card.variants', { count: test.variantCount })} variant="outlined" />
        </Box>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1.5 }}>
          <Box>
            <Typography variant="caption" color="text.secondary">{t('detail.subject')}</Typography>
            <Typography variant="body2">{test.subjectName}</Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">{t('detail.questions')}</Typography>
            <Typography variant="body2">{test.questionCount}</Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">{t('detail.variantCount')}</Typography>
            <Typography variant="body2">{test.variantCount}</Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">{t('detail.seed')}</Typography>
            <Typography variant="body2">{test.randomSeed}</Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">{t('detail.shuffle')}</Typography>
            <Typography variant="body2">
              {test.shuffleQuestions ? t('form.shuffleQuestions') : '-'}
              {test.shuffleOptions ? `, ${t('form.shuffleOptions')}` : ''}
            </Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">{t('card.created')}</Typography>
            <Typography variant="body2">{new Date(test.createdAt).toLocaleString()}</Typography>
          </Box>
        </Box>

        {test.difficultyDistribution && (
          <Box sx={{ mt: 2 }}>
            <Typography variant="caption" color="text.secondary">{t('form.difficulty')}</Typography>
            <Box sx={{ display: 'flex', gap: 0.5, mt: 0.5 }}>
              {Object.entries(test.difficultyDistribution).map(([key, val]) => (
                <Chip
                  key={key}
                  size="small"
                  label={`${t(`form.${key.toLowerCase()}`, key)}: ${val}%`}
                  color={key === 'easy' ? 'success' : key === 'medium' ? 'warning' : 'error'}
                />
              ))}
            </Box>
          </Box>
        )}
      </Paper>

      {/* Variants */}
      {test.variants && test.variants.length > 0 && (
        <Paper sx={{ p: 3, mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom>{t('detail.variants')}</Typography>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            {test.variants.map((v) => (
              <Chip key={v.code} label={`${t('detail.variants')} ${v.code}`} variant="outlined" />
            ))}
          </Box>
        </Paper>
      )}

      {/* Export */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <TestExportButtons testId={test.id} />
      </Paper>

      <Divider sx={{ my: 2 }} />

      {/* Delete Dialog */}
      <TestDeleteDialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        test={test}
        isPending={remove.isPending}
      />

      {/* Submit to Moderation Dialog */}
      <SubmitToModerationDialog
        open={submitOpen}
        onClose={() => setSubmitOpen(false)}
        onConfirm={() => {
          bulkSubmit.mutate(submittableQuestionIds, {
            onSuccess: () => setSubmitOpen(false),
          });
        }}
        isPending={bulkSubmit.isPending}
        totalQuestions={uniqueQuestionIds.length}
        submittableCount={submittableQuestionIds.length}
        isLoadingStatuses={questionsLoading}
      />
    </Box>
  );
}
