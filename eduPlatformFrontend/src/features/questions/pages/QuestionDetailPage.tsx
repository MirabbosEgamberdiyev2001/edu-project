import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Chip,
  Paper,
  Button,
  CircularProgress,
  Divider,
  Alert,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import SendIcon from '@mui/icons-material/Send';
import HistoryIcon from '@mui/icons-material/History';
import RestoreIcon from '@mui/icons-material/Restore';
import PersonIcon from '@mui/icons-material/Person';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { useTranslation } from 'react-i18next';
import { useQuestion, useQuestionVersions } from '../hooks/useQuestions';
import { useQuestionMutations } from '../hooks/useQuestionMutations';
import { QuestionStatus } from '@/types/question';
import type { QuestionDto, CreateQuestionRequest, UpdateQuestionRequest } from '@/types/question';
import { resolveTranslation } from '@/utils/i18nUtils';
import { LANGUAGE_LABELS } from '@/config';
import QuestionFormDialog from '../components/QuestionFormDialog';
import QuestionDeleteDialog from '../components/QuestionDeleteDialog';

const KNOWN_LOCALE_KEYS = ['uz_latn', 'uz_cyrl', 'en', 'ru'] as const;

const LOCALE_KEY_TO_FRONTEND: Record<string, string> = {
  uz_latn: 'uzl',
  uz_cyrl: 'uzc',
  en: 'en',
  ru: 'ru',
};

const DIFFICULTY_COLORS: Record<string, 'success' | 'warning' | 'error'> = {
  EASY: 'success',
  MEDIUM: 'warning',
  HARD: 'error',
};

const STATUS_COLORS: Record<string, 'default' | 'primary' | 'success' | 'error' | 'warning' | 'info'> = {
  DRAFT: 'default',
  PENDING: 'info',
  APPROVED: 'primary',
  ACTIVE: 'success',
  REJECTED: 'error',
  ARCHIVED: 'warning',
};

export default function QuestionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('question');

  const { data: question, isLoading } = useQuestion(id);
  const { data: versions, isLoading: versionsLoading } = useQuestionVersions(id);
  const { update, remove, submitForModeration, rollback } = useQuestionMutations();

  const [formOpen, setFormOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [versionsOpen, setVersionsOpen] = useState(false);

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!question) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography variant="h6" color="text.secondary">{t('empty')}</Typography>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/questions')} sx={{ mt: 2 }}>
          {t('detail.backToList')}
        </Button>
      </Box>
    );
  }

  const displayText = resolveTranslation(question.questionTextTranslations) || question.questionText;
  const displayProof = resolveTranslation(question.proofTranslations) || question.proof;
  const canSubmit = question.status === QuestionStatus.DRAFT || question.status === QuestionStatus.REJECTED;
  const canEdit = question.status !== QuestionStatus.ACTIVE;

  const handleFormSubmit = (formData: CreateQuestionRequest | UpdateQuestionRequest) => {
    update.mutate(
      { id: question.id, data: formData as UpdateQuestionRequest },
      { onSuccess: () => setFormOpen(false) },
    );
  };

  const handleDeleteConfirm = () => {
    remove.mutate(question.id, {
      onSuccess: () => navigate('/questions'),
    });
  };

  const handleRollback = (version: number) => {
    rollback.mutate(
      { id: question.id, version },
      { onSuccess: () => setVersionsOpen(false) },
    );
  };

  // Render options based on question type
  function renderOptions() {
    const options = question!.options;
    if (!options) return null;

    // Array format: [{id, text, isCorrect, textTranslations}, ...]
    if (Array.isArray(options) && options.length === 0) return null;
    if (Array.isArray(options)) {
      const labels = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
          {options.map((opt: Record<string, unknown>, i: number) => {
            const isCorrect = Boolean(opt.isCorrect);
            const text = opt.textTranslations
              ? (resolveTranslation(opt.textTranslations as Record<string, string>) || '')
              : typeof opt.text === 'object' && opt.text !== null
                ? (resolveTranslation(opt.text as Record<string, string>) || '')
                : String(opt.text || '');
            return (
              <Paper
                key={i}
                variant="outlined"
                sx={{
                  p: 1.5,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1.5,
                  borderColor: isCorrect ? 'success.main' : 'divider',
                  bgcolor: isCorrect ? 'success.50' : 'background.paper',
                }}
              >
                <Chip label={labels[i]} size="small" color={isCorrect ? 'success' : 'default'} />
                <Typography variant="body2" sx={{ flex: 1 }}>{text}</Typography>
                {isCorrect && <CheckCircleIcon color="success" fontSize="small" />}
              </Paper>
            );
          })}
        </Box>
      );
    }

    // Object format: {"A": {text, textTranslations}, "B": ...} or {"A": {"uz_latn": "...", ...}, ...}
    if (typeof options === 'object' && options !== null) {
      const entries = Object.entries(options as Record<string, unknown>);
      if (entries.length > 0) {
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {entries.map(([label, value]) => {
              let text = '';
              if (typeof value === 'string') {
                text = value;
              } else if (typeof value === 'object' && value !== null) {
                const obj = value as Record<string, unknown>;
                if (obj.textTranslations) {
                  // Prefer textTranslations (all languages) over pre-resolved text
                  text = resolveTranslation(obj.textTranslations as Record<string, string>) || '';
                } else if (typeof obj.text === 'object' && obj.text !== null) {
                  // text is a multilingual map itself
                  text = resolveTranslation(obj.text as Record<string, string>) || '';
                } else if (typeof obj.text === 'string') {
                  text = obj.text;
                } else {
                  // Raw multilingual map: {"uz_latn": "...", "en": "..."}
                  text = resolveTranslation(obj as unknown as Record<string, string>) || '';
                }
              }
              return (
                <Paper
                  key={label}
                  variant="outlined"
                  sx={{ p: 1.5, display: 'flex', alignItems: 'center', gap: 1.5 }}
                >
                  <Chip label={label} size="small" />
                  <Typography variant="body2" sx={{ flex: 1 }}>{text}</Typography>
                </Paper>
              );
            })}
          </Box>
        );
      }
    }

    return null;
  }

  function renderCorrectAnswer() {
    const answer = question!.correctAnswer;
    if (answer === null || answer === undefined) return null;

    // String answer (e.g., "A,C" for MCQ or "true"/"false")
    if (typeof answer === 'string') {
      // Translate true/false for TRUE_FALSE questions
      if (answer === 'true') return <Typography variant="body2">{t('form.true')}</Typography>;
      if (answer === 'false') return <Typography variant="body2">{t('form.false')}</Typography>;
      return <Typography variant="body2">{answer}</Typography>;
    }

    // Object answer - could be multilingual map {"uz_latn": "...", "en": "..."}
    if (typeof answer === 'object') {
      const resolved = resolveTranslation(answer as Record<string, string>);
      if (resolved) {
        return <Typography variant="body2">{resolved}</Typography>;
      }
      // Fallback: show as readable text
      return (
        <Typography variant="body2">
          {Object.values(answer as Record<string, unknown>)
            .filter(v => typeof v === 'string' && (v as string).trim())
            .join(' / ')}
        </Typography>
      );
    }

    return <Typography variant="body2">{String(answer)}</Typography>;
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
        <IconButton onClick={() => navigate('/questions')}>
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h5" fontWeight={700} sx={{ flex: 1 }}>
          {t('detail.title')}
        </Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title={t('detail.versionHistory')}>
            <IconButton onClick={() => setVersionsOpen(true)}>
              <HistoryIcon />
            </IconButton>
          </Tooltip>
          {canEdit && (
            <Button startIcon={<EditIcon />} variant="outlined" size="small" onClick={() => setFormOpen(true)}>
              {t('edit')}
            </Button>
          )}
          {canSubmit && (
            <Button
              startIcon={<SendIcon />}
              variant="contained"
              size="small"
              onClick={() => submitForModeration.mutate(question.id)}
              disabled={submitForModeration.isPending}
            >
              {t('submit')}
            </Button>
          )}
          <IconButton color="error" onClick={() => setDeleteOpen(true)}>
            <DeleteIcon />
          </IconButton>
        </Box>
      </Box>

      {/* Rejection reason alert */}
      {question.status === QuestionStatus.REJECTED && question.rejectionReason && (
        <Alert severity="error" icon={<CancelIcon />} sx={{ mb: 3 }}>
          <Typography variant="subtitle2">{t('detail.rejectionReason')}</Typography>
          <Typography variant="body2">{question.rejectionReason}</Typography>
        </Alert>
      )}

      {/* Main content */}
      <Paper sx={{ p: 3, mb: 3 }}>
        {/* Status chips */}
        <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
          <Chip label={t(`statuses.${question.status}`)} color={STATUS_COLORS[question.status]} />
          <Chip label={t(`types.${question.questionType}`)} variant="outlined" />
          <Chip label={t(`difficulties.${question.difficulty}`)} color={DIFFICULTY_COLORS[question.difficulty]} />
          <Chip label={`${question.points} ${t('form.points')}`} variant="outlined" />
          {question.timeLimitSeconds && (
            <Chip icon={<AccessTimeIcon />} label={`${question.timeLimitSeconds}s`} variant="outlined" />
          )}
          <Chip label={`v${question.version}`} size="small" variant="outlined" />
        </Box>

        {/* Question text */}
        <Typography variant="h6" sx={{ mb: 2, lineHeight: 1.6 }}>
          {displayText}
        </Typography>

        {/* Translations */}
        {question.questionTextTranslations && Object.keys(question.questionTextTranslations).length > 1 && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="caption" color="text.secondary" fontWeight={600}>
              {t('detail.translations')}
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, mt: 0.5 }}>
              {KNOWN_LOCALE_KEYS.map((localeKey) => {
                const text = question.questionTextTranslations![localeKey];
                if (!text?.trim()) return null;
                const frontLang = LOCALE_KEY_TO_FRONTEND[localeKey] || localeKey;
                const label = LANGUAGE_LABELS[frontLang] || localeKey;
                return (
                  <Typography key={localeKey} variant="body2" color="text.secondary">
                    <Chip label={label} size="small" sx={{ mr: 1, fontSize: 10 }} /> {text}
                  </Typography>
                );
              })}
            </Box>
          </Box>
        )}

        <Divider sx={{ my: 2 }} />

        {/* Options */}
        <Typography variant="subtitle2" sx={{ mb: 1 }}>
          {t('form.options')}
        </Typography>
        {renderOptions()}

        {/* Correct answer (for non-MCQ types or TRUE_FALSE) */}
        {(!Array.isArray(question.options) || (Array.isArray(question.options) && question.options.length === 0)) && question.correctAnswer != null && (
          <Box sx={{ mt: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
              {t('form.correctAnswer')}
            </Typography>
            {renderCorrectAnswer()}
          </Box>
        )}

        {/* Proof */}
        {displayProof && (
          <>
            <Divider sx={{ my: 2 }} />
            <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
              {t('form.proof')}
            </Typography>
            <Paper variant="outlined" sx={{ p: 2, bgcolor: 'action.hover' }}>
              <Typography variant="body2">{displayProof}</Typography>
            </Paper>
          </>
        )}
      </Paper>

      {/* Meta info */}
      <Paper sx={{ p: 3 }}>
        <Typography variant="subtitle2" sx={{ mb: 1.5 }}>{t('detail.info')}</Typography>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1.5 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <PersonIcon fontSize="small" color="action" />
            <Box>
              <Typography variant="caption" color="text.secondary">{t('detail.author')}</Typography>
              <Typography variant="body2">{question.userName}</Typography>
            </Box>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">{t('detail.subject')}</Typography>
            <Typography variant="body2">{resolveTranslation(question.subjectNameTranslations) || question.subjectName}</Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">{t('detail.topic')}</Typography>
            <Typography variant="body2">{resolveTranslation(question.topicNameTranslations) || question.topicName}</Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">{t('detail.createdAt')}</Typography>
            <Typography variant="body2">{new Date(question.createdAt).toLocaleString()}</Typography>
          </Box>
          <Box>
            <Typography variant="caption" color="text.secondary">{t('detail.updatedAt')}</Typography>
            <Typography variant="body2">{new Date(question.updatedAt).toLocaleString()}</Typography>
          </Box>
          {question.publishedAt && (
            <Box>
              <Typography variant="caption" color="text.secondary">{t('detail.publishedAt')}</Typography>
              <Typography variant="body2">{new Date(question.publishedAt).toLocaleString()}</Typography>
            </Box>
          )}
          <Box>
            <Typography variant="caption" color="text.secondary">{t('detail.timesUsed')}</Typography>
            <Typography variant="body2">{question.timesUsed}</Typography>
          </Box>
          {question.correctRate != null && (
            <Box>
              <Typography variant="caption" color="text.secondary">{t('detail.correctRate')}</Typography>
              <Typography variant="body2">{question.correctRate}%</Typography>
            </Box>
          )}
        </Box>
      </Paper>

      {/* Version History Dialog */}
      <Dialog open={versionsOpen} onClose={() => setVersionsOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{t('detail.versionHistory')}</DialogTitle>
        <DialogContent>
          {versionsLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : versions && versions.length > 0 ? (
            <List>
              {versions.map((v) => (
                <ListItem key={v.id} divider>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Chip label={`v${v.version}`} size="small" />
                        <Typography variant="body2">
                          {resolveTranslation(v.questionTextTranslations) || v.questionText}
                        </Typography>
                      </Box>
                    }
                    secondary={
                      <Box sx={{ mt: 0.5 }}>
                        {v.changeReason && (
                          <Typography variant="caption" display="block">
                            {v.changeReason}
                          </Typography>
                        )}
                        <Typography variant="caption" color="text.disabled">
                          {new Date(v.createdAt).toLocaleString()}
                        </Typography>
                      </Box>
                    }
                  />
                  <ListItemSecondaryAction>
                    <Tooltip title={t('detail.rollbackTo')}>
                      <IconButton
                        edge="end"
                        size="small"
                        onClick={() => handleRollback(v.version)}
                        disabled={rollback.isPending}
                      >
                        <RestoreIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </ListItemSecondaryAction>
                </ListItem>
              ))}
            </List>
          ) : (
            <Typography color="text.secondary" sx={{ py: 2, textAlign: 'center' }}>
              {t('detail.noVersions')}
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setVersionsOpen(false)}>{t('common:close', 'Close')}</Button>
        </DialogActions>
      </Dialog>

      {/* Edit Dialog */}
      <QuestionFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleFormSubmit}
        question={question}
        isPending={update.isPending}
      />

      {/* Delete Dialog */}
      <QuestionDeleteDialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        question={question}
        isPending={remove.isPending}
      />
    </Box>
  );
}
