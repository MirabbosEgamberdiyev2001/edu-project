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
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RefreshIcon from '@mui/icons-material/Refresh';
import SendIcon from '@mui/icons-material/Send';
import PublicIcon from '@mui/icons-material/Public';
import PublishedWithChangesIcon from '@mui/icons-material/PublishedWithChanges';
import AddLinkIcon from '@mui/icons-material/AddLink';
import PrintIcon from '@mui/icons-material/Print';
import DescriptionIcon from '@mui/icons-material/Description';
import { useTestDetail } from '../hooks/useTests';
import { useTestMutations } from '../hooks/useTestMutations';
import { useQuestionMutations } from '@/features/questions/hooks/useQuestionMutations';
import { MathText } from '@/components/math';
import { generateMultiVariantDocument, downloadFile, printHtml, type ExportVariant } from '@/utils/mathExport';
import { useQuestionsByIds } from '@/features/questions/hooks/useQuestions';
import { QuestionStatus } from '@/types/question';
import TestExportButtons from '../components/TestExportButtons';
import TestDeleteDialog from '../components/TestDeleteDialog';
import TestEditDialog from '../components/TestEditDialog';
import SubmitToModerationDialog from '../components/SubmitToModerationDialog';
import PublishTestDialog from '../components/PublishTestDialog';
import QuickPromoDialog from '../components/QuickPromoDialog';
import { testApi } from '@/api/testApi';
import { resolveTranslation } from '@/utils/i18nUtils';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { PageShell } from '@/components/ui';
import type { GlobalStatus } from '@/types/test';

const STATUS_COLORS: Record<string, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  CREATED: 'info',
  GENERATING: 'warning',
  READY: 'success',
  DOWNLOADED: 'success',
  DELETED: 'error',
};

const GLOBAL_STATUS_COLORS: Record<GlobalStatus, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  NONE: 'default',
  PENDING_MODERATION: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
};


export default function TestDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('test');
  const { t: tQ } = useTranslation('question');

  const { data: test, isLoading } = useTestDetail(id!);
  const { update, remove, regenerate } = useTestMutations();
  const { bulkSubmit } = useQuestionMutations();

  const queryClient = useQueryClient();
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [submitOpen, setSubmitOpen] = useState(false);
  const [publishOpen, setPublishOpen] = useState(false);
  const [submitGlobalOpen, setSubmitGlobalOpen] = useState(false);
  const [quickPromoOpen, setQuickPromoOpen] = useState(false);

  const publishMutation = useMutation({
    mutationFn: (durationMinutes?: number) => testApi.publishTest(id!, durationMinutes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tests', 'detail', id] });
      setPublishOpen(false);
    },
  });

  const unpublishMutation = useMutation({
    mutationFn: () => testApi.unpublishTest(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tests', 'detail', id] });
      setPublishOpen(false);
    },
  });

  const submitForGlobalMutation = useMutation({
    mutationFn: () => testApi.submitForGlobal(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tests', 'detail', id] });
      setSubmitGlobalOpen(false);
    },
  });

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

  // Build ExportVariant[] for client-side PDF/DOCX/Word generation.
  // Respects per-variant question order (questionIds) and option shuffle order (optionsOrder).
  const exportVariants = useMemo((): ExportVariant[] => {
    if (!questionsData || !test?.variants?.length) return [];
    const LABELS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
    const qMap = new Map(questionsData.map((q) => [q.id, q]));

    return test.variants.map((variant) => ({
      code: variant.code,
      questions: variant.questionIds
        .map((qId, idx) => {
          const q = qMap.get(qId);
          if (!q) return null;

          const rawOpts = Array.isArray(q.options)
            ? (q.options as Array<Record<string, unknown>>)
            : [];

          // Reorder options per variant's optionsOrder[idx] (array of option IDs)
          const orderIds: string[] = Array.isArray(variant.optionsOrder?.[idx])
            ? (variant.optionsOrder[idx] as string[])
            : [];
          const reorderedOpts = orderIds.length > 0
            ? (() => {
                const optMap = new Map(rawOpts.map((o) => [String(o.id ?? ''), o]));
                return orderIds.map((id) => optMap.get(id)).filter(Boolean) as typeof rawOpts;
              })()
            : rawOpts;

          // Resolve correctAnswerText for non-MCQ types (SHORT_ANSWER, FILL_BLANK, ESSAY, TRUE_FALSE etc.)
          // MCQ correct answers are determined by isCorrect flag on each option, not this field.
          const ca = q.correctAnswer;
          const correctAnswerText: string | undefined = (() => {
            if (ca == null) return undefined;
            if (typeof ca === 'string') return ca.trim() || undefined;
            if (Array.isArray(ca)) return undefined; // MCQ option ID arrays — handled via isCorrect
            if (typeof ca === 'object') {
              return resolveTranslation(ca as Record<string, string>) || undefined;
            }
            return String(ca).trim() || undefined;
          })();

          return {
            questionText: resolveTranslation(q.questionTextTranslations) || q.questionText || '',
            points: q.points,
            questionType: q.questionType,
            options: reorderedOpts.map((opt, i) => ({
              label: LABELS[i] ?? String(i + 1),
              text: typeof opt.text === 'object' && opt.text !== null
                ? resolveTranslation(opt.text as Record<string, string>) || ''
                : String(opt.text ?? ''),
              isCorrect: Boolean(opt.isCorrect),
            })),
            correctAnswerText,
            proof: resolveTranslation(q.proofTranslations) || q.proof || undefined,
          };
        })
        .filter(Boolean) as ExportVariant['questions'],
    }));
  }, [questionsData, test?.variants]);

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

  const displayTitle = resolveTranslation(test.titleTranslations) || test.title;
  const globalStatus = test.globalStatus || 'NONE';
  const canSubmitForGlobal = globalStatus === 'NONE' || globalStatus === 'REJECTED';

  return (
    <PageShell
      title={displayTitle}
      breadcrumbs={[
        { label: t('common:tests'), to: '/tests' },
        { label: displayTitle },
      ]}
      actions={
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="Print / PDF">
            <IconButton onClick={() => printHtml(generateMultiVariantDocument(exportVariants, displayTitle))}>
              <PrintIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Export to Word (.doc)">
            <IconButton
              onClick={() => {
                const safeName = displayTitle.replace(/[^\w\s-]/g, '').trim().slice(0, 60);
                downloadFile(`${safeName || 'test'}.doc`, generateMultiVariantDocument(exportVariants, displayTitle), 'application/msword');
              }}
            >
              <DescriptionIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title={t('edit.title')}>
            <IconButton onClick={() => setEditOpen(true)}>
              <EditIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title={t('publish.title')}>
            <IconButton color={test.isPublic ? 'success' : 'default'} onClick={() => setPublishOpen(true)}>
              <PublicIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title={t('quickPromo.title')}>
            <IconButton color="secondary" onClick={() => setQuickPromoOpen(true)}>
              <AddLinkIcon />
            </IconButton>
          </Tooltip>
          {canSubmitForGlobal && (
            <Tooltip title={t('globalStatus.submitTooltip')}>
              <IconButton
                color="primary"
                onClick={() => setSubmitGlobalOpen(true)}
                disabled={submitForGlobalMutation.isPending}
              >
                <PublishedWithChangesIcon />
              </IconButton>
            </Tooltip>
          )}
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
          <Tooltip title={t('delete.title')}>
            <IconButton color="error" onClick={() => setDeleteOpen(true)}>
              <DeleteIcon />
            </IconButton>
          </Tooltip>
        </Box>
      }
    >

      {/* Global Status Alert */}
      {globalStatus === 'PENDING_MODERATION' && (
        <Alert severity="info" sx={{ mb: 2 }}>
          {t('globalStatus.pendingAlert')}
        </Alert>
      )}
      {globalStatus === 'APPROVED' && (
        <Alert severity="success" sx={{ mb: 2 }}>
          {t('globalStatus.approvedAlert')}
        </Alert>
      )}
      {globalStatus === 'REJECTED' && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('globalStatus.rejectedAlert')}
          {test.globalRejectionReason && (
            <Typography variant="body2" sx={{ mt: 0.5 }}>
              {t('globalStatus.rejectedReason')} {test.globalRejectionReason}
            </Typography>
          )}
          {t('globalStatus.rejectedHint')}
        </Alert>
      )}

      {/* Test Info */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="subtitle2" gutterBottom>{t('detail.info')}</Typography>
        <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
          <Chip label={t(`status.${test.status}`)} color={STATUS_COLORS[test.status] || 'default'} />
          <Chip
            label={t(`globalStatus.${globalStatus}`)}
            color={GLOBAL_STATUS_COLORS[globalStatus]}
            variant="outlined"
          />
          {test.category && <Chip label={test.category} variant="outlined" color="primary" />}
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
          {test.gradeLevel && (
            <Box>
              <Typography variant="caption" color="text.secondary">{t('form.className')}</Typography>
              <Typography variant="body2">{t('globalStatus.grade', { level: test.gradeLevel })}</Typography>
            </Box>
          )}
          {test.globalSubmittedAt && (
            <Box>
              <Typography variant="caption" color="text.secondary">{t('globalStatus.submittedAt')}</Typography>
              <Typography variant="body2">{new Date(test.globalSubmittedAt).toLocaleString()}</Typography>
            </Box>
          )}
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

      {/* Questions List */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="subtitle2" gutterBottom>
          {t('detail.questionsList')} ({uniqueQuestionIds.length})
        </Typography>
        {questionsLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
            <CircularProgress size={24} />
            <Typography variant="body2" color="text.secondary" sx={{ ml: 1 }}>
              {t('detail.loadingQuestions')}
            </Typography>
          </Box>
        ) : questionsData && questionsData.length > 0 ? (
          <Box sx={{ maxHeight: 500, overflow: 'auto' }}>
            {questionsData.map((q, idx) => {
              const options = Array.isArray(q.options) ? q.options as Array<{ text?: string; isCorrect?: boolean }> : [];
              return (
                <Box
                  key={q.id}
                  sx={{
                    py: 1.5,
                    px: 1,
                    borderBottom: idx < questionsData.length - 1 ? '1px solid' : 'none',
                    borderColor: 'divider',
                  }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, mb: 0.5 }}>
                    <Typography variant="caption" color="text.disabled" sx={{ minWidth: 28, pt: 0.3 }}>
                      {idx + 1}.
                    </Typography>
                    <Box sx={{ flex: 1 }}>
                      <MathText
                        text={resolveTranslation(q.questionTextTranslations) || q.questionText || ''}
                        variant="body2"
                        sx={{ fontWeight: 600, wordBreak: 'break-word' }}
                      />
                      {options.length > 0 && (
                        <Box sx={{ mt: 0.5, pl: 1 }}>
                          {options.map((opt, oi) => {
                            const optText = typeof opt.text === 'object' && opt.text !== null
                              ? resolveTranslation(opt.text as Record<string, string>) || ''
                              : (String(opt.text ?? ''));
                            return (
                              <MathText
                                key={oi}
                                text={`${String.fromCharCode(65 + oi)}) ${optText}${opt.isCorrect ? ' ✓' : ''}`}
                                variant="caption"
                                sx={{
                                  display: 'block',
                                  color: opt.isCorrect ? 'success.main' : 'text.secondary',
                                  fontWeight: opt.isCorrect ? 700 : 400,
                                }}
                              />
                            );
                          })}
                        </Box>
                      )}
                    </Box>
                    <Box sx={{ display: 'flex', gap: 0.5, flexShrink: 0 }}>
                      <Chip
                        size="small"
                        label={tQ(`difficulties.${q.difficulty}`)}
                        color={q.difficulty === 'EASY' ? 'success' : q.difficulty === 'MEDIUM' ? 'warning' : 'error'}
                      />
                      <Chip
                        size="small"
                        label={tQ(`statuses.${q.status}`)}
                        variant="outlined"
                      />
                    </Box>
                  </Box>
                </Box>
              );
            })}
          </Box>
        ) : (
          <Typography variant="body2" color="text.secondary">
            {t('detail.questions')}: 0
          </Typography>
        )}
      </Paper>

      {/* Export */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <TestExportButtons testId={test.id} variants={exportVariants} testTitle={displayTitle} />
      </Paper>

      <Divider sx={{ my: 2 }} />

      {/* Edit Dialog */}
      <TestEditDialog
        open={editOpen}
        onClose={() => setEditOpen(false)}
        onSave={(data) => {
          update.mutate({ id: test.id, data }, {
            onSuccess: () => setEditOpen(false),
          });
        }}
        test={test}
        isPending={update.isPending}
      />

      {/* Delete Dialog */}
      <TestDeleteDialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        test={test}
        isPending={remove.isPending}
      />

      {/* Publish Dialog */}
      <PublishTestDialog
        open={publishOpen}
        onClose={() => setPublishOpen(false)}
        test={test}
        onPublish={(dur) => publishMutation.mutate(dur)}
        onUnpublish={() => unpublishMutation.mutate()}
        isPending={publishMutation.isPending || unpublishMutation.isPending}
      />

      {/* Submit to Question Moderation Dialog */}
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

      {/* Quick Promo Code Dialog */}
      <QuickPromoDialog
        open={quickPromoOpen}
        onClose={() => setQuickPromoOpen(false)}
        test={test}
      />

      {/* Submit Test for Global Moderation Dialog */}
      <Dialog open={submitGlobalOpen} onClose={() => setSubmitGlobalOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{t('submitGlobal.title')}</DialogTitle>
        <DialogContent>
          <Typography variant="body1" sx={{ mb: 2 }}>
            {t('submitGlobal.description')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {t('submitGlobal.moderatorNote')}
          </Typography>
          <Box component="ul" sx={{ mt: 1, color: 'text.secondary' }}>
            <li><Typography variant="body2">{t('submitGlobal.benefit1')}</Typography></li>
            <li><Typography variant="body2">{t('submitGlobal.benefit2')}</Typography></li>
            <li><Typography variant="body2">{t('submitGlobal.benefit3')}</Typography></li>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSubmitGlobalOpen(false)}>{t('submitGlobal.cancel')}</Button>
          <Button
            variant="contained"
            onClick={() => submitForGlobalMutation.mutate()}
            disabled={submitForGlobalMutation.isPending}
          >
            {submitForGlobalMutation.isPending ? t('submitGlobal.submitting') : t('submitGlobal.submit')}
          </Button>
        </DialogActions>
      </Dialog>
    </PageShell>
  );
}
