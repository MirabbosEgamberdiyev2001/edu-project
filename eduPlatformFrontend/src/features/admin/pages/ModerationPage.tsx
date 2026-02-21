import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Checkbox,
  IconButton,
  Tooltip,
  Button,
  Pagination,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Collapse,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import VisibilityIcon from '@mui/icons-material/Visibility';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import { resolveTranslation } from '@/utils/i18nUtils';
import type { QuestionDto } from '@/types/question';
import {
  usePendingQuestions,
  useApproveQuestion,
  useRejectQuestion,
  useBulkApprove,
  useBulkReject,
} from '../hooks/useModeration';

const DIFFICULTY_COLORS: Record<string, 'success' | 'warning' | 'error'> = {
  EASY: 'success',
  MEDIUM: 'warning',
  HARD: 'error',
};

export default function ModerationPage() {
  const { t } = useTranslation('admin');
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [rejectDialogId, setRejectDialogId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [bulkRejectOpen, setBulkRejectOpen] = useState(false);
  const [bulkRejectReason, setBulkRejectReason] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const params = useMemo(() => ({ page, size: 20 }), [page]);
  const { data, isLoading } = usePendingQuestions(params);

  const approveQuestion = useApproveQuestion();
  const rejectQuestion = useRejectQuestion();
  const bulkApprove = useBulkApprove();
  const bulkReject = useBulkReject();

  const toggleSelect = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (!data) return;
    if (selected.size === data.content.length) {
      setSelected(new Set());
    } else {
      setSelected(new Set(data.content.map((q) => q.id)));
    }
  };

  const handleApprove = (id: string) => {
    approveQuestion.mutate(id, {
      onSuccess: () => {
        setSelected((prev) => { const n = new Set(prev); n.delete(id); return n; });
      },
    });
  };

  const handleReject = () => {
    if (!rejectDialogId || !rejectReason.trim()) return;
    rejectQuestion.mutate(
      { id: rejectDialogId, reason: rejectReason },
      {
        onSuccess: () => {
          setRejectDialogId(null);
          setRejectReason('');
          setSelected((prev) => { const n = new Set(prev); n.delete(rejectDialogId); return n; });
        },
      },
    );
  };

  const handleBulkApprove = () => {
    bulkApprove.mutate(
      { questionIds: Array.from(selected) },
      { onSuccess: () => setSelected(new Set()) },
    );
  };

  const handleBulkReject = () => {
    if (!bulkRejectReason.trim()) return;
    bulkReject.mutate(
      { questionIds: Array.from(selected), reason: bulkRejectReason },
      {
        onSuccess: () => {
          setSelected(new Set());
          setBulkRejectOpen(false);
          setBulkRejectReason('');
        },
      },
    );
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>{t('moderation.title')}</Typography>
          <Typography variant="body2" color="text.secondary">{t('moderation.subtitle')}</Typography>
        </Box>
        {data && (
          <Chip
            label={t('moderation.pendingCount', { count: data.totalElements })}
            color="warning"
            variant="outlined"
          />
        )}
      </Box>

      {/* Bulk Actions */}
      {selected.size > 0 && (
        <Paper sx={{ p: 1.5, mb: 2, display: 'flex', alignItems: 'center', gap: 2, bgcolor: 'action.selected' }}>
          <Typography variant="body2" fontWeight={600}>
            {t('moderation.selected', { count: selected.size })}
          </Typography>
          <Button
            size="small"
            variant="contained"
            color="success"
            startIcon={<CheckCircleIcon />}
            onClick={handleBulkApprove}
            disabled={bulkApprove.isPending}
          >
            {t('moderation.bulkApprove')}
          </Button>
          <Button
            size="small"
            variant="contained"
            color="error"
            startIcon={<CancelIcon />}
            onClick={() => setBulkRejectOpen(true)}
            disabled={bulkReject.isPending}
          >
            {t('moderation.bulkReject')}
          </Button>
          <Button size="small" onClick={() => setSelected(new Set())}>
            {t('moderation.deselectAll')}
          </Button>
        </Paper>
      )}

      {/* Table */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : !data || data.content.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">{t('moderation.noPending')}</Typography>
        </Paper>
      ) : (
        <>
          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell padding="checkbox">
                    <Checkbox
                      checked={selected.size === data.content.length && data.content.length > 0}
                      indeterminate={selected.size > 0 && selected.size < data.content.length}
                      onChange={toggleSelectAll}
                    />
                  </TableCell>
                  <TableCell width={40} />
                  <TableCell>{t('moderation.question')}</TableCell>
                  <TableCell>{t('moderation.subject')}</TableCell>
                  <TableCell>{t('moderation.topic')}</TableCell>
                  <TableCell>{t('moderation.difficulty')}</TableCell>
                  <TableCell>{t('moderation.type')}</TableCell>
                  <TableCell>{t('moderation.author')}</TableCell>
                  <TableCell align="right">{t('users.actions')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.content.map((q) => (
                  <QuestionRow
                    key={q.id}
                    question={q}
                    selected={selected.has(q.id)}
                    expanded={expandedId === q.id}
                    onToggleSelect={() => toggleSelect(q.id)}
                    onToggleExpand={() => setExpandedId(expandedId === q.id ? null : q.id)}
                    onApprove={() => handleApprove(q.id)}
                    onReject={() => { setRejectDialogId(q.id); setRejectReason(''); }}
                    onView={() => navigate(`/questions/${q.id}`)}
                    approving={approveQuestion.isPending}
                    t={t}
                  />
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          {data.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
              <Pagination
                count={data.totalPages}
                page={page + 1}
                onChange={(_, p) => setPage(p - 1)}
                color="primary"
              />
            </Box>
          )}
        </>
      )}

      {/* Reject Dialog (Single) */}
      <Dialog open={!!rejectDialogId} onClose={() => setRejectDialogId(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{t('moderation.rejectTitle')}</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>{t('moderation.rejectDescription')}</Typography>
          <TextField
            fullWidth
            size="small"
            label={t('moderation.rejectReason')}
            placeholder={t('moderation.rejectReasonPlaceholder')}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            multiline
            rows={3}
            autoFocus
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialogId(null)}>{t('common:cancel', 'Cancel')}</Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleReject}
            disabled={!rejectReason.trim() || rejectQuestion.isPending}
          >
            {t('moderation.reject')}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Bulk Reject Dialog */}
      <Dialog open={bulkRejectOpen} onClose={() => setBulkRejectOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{t('moderation.bulkReject')}</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            {t('moderation.bulkRejectConfirm', { count: selected.size })}
          </Typography>
          <TextField
            fullWidth
            size="small"
            label={t('moderation.rejectReason')}
            placeholder={t('moderation.rejectReasonPlaceholder')}
            value={bulkRejectReason}
            onChange={(e) => setBulkRejectReason(e.target.value)}
            multiline
            rows={3}
            autoFocus
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setBulkRejectOpen(false)}>{t('common:cancel', 'Cancel')}</Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleBulkReject}
            disabled={!bulkRejectReason.trim() || bulkReject.isPending}
          >
            {t('moderation.bulkReject')}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

interface QuestionRowProps {
  question: QuestionDto;
  selected: boolean;
  expanded: boolean;
  onToggleSelect: () => void;
  onToggleExpand: () => void;
  onApprove: () => void;
  onReject: () => void;
  onView: () => void;
  approving: boolean;
  t: (key: string) => string;
}

function QuestionRow({
  question: q,
  selected,
  expanded,
  onToggleSelect,
  onToggleExpand,
  onApprove,
  onReject,
  onView,
  approving,
  t,
}: QuestionRowProps) {
  const questionText = resolveTranslation(q.questionTextTranslations, q.questionText);
  const subjectName = resolveTranslation(q.subjectNameTranslations, q.subjectName);
  const topicName = resolveTranslation(q.topicNameTranslations, q.topicName);
  const proof = resolveTranslation(q.proofTranslations, q.proof ?? undefined);

  return (
    <>
      <TableRow hover>
        <TableCell padding="checkbox">
          <Checkbox checked={selected} onChange={onToggleSelect} />
        </TableCell>
        <TableCell>
          <IconButton size="small" onClick={onToggleExpand}>
            {expanded ? <KeyboardArrowUpIcon fontSize="small" /> : <KeyboardArrowDownIcon fontSize="small" />}
          </IconButton>
        </TableCell>
        <TableCell>
          <Typography variant="body2" fontWeight={500} sx={{ maxWidth: 250, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {questionText}
          </Typography>
        </TableCell>
        <TableCell>
          <Typography variant="caption">{subjectName}</Typography>
        </TableCell>
        <TableCell>
          <Typography variant="caption">{topicName}</Typography>
        </TableCell>
        <TableCell>
          <Chip label={q.difficulty} size="small" color={DIFFICULTY_COLORS[q.difficulty] || 'default'} variant="outlined" />
        </TableCell>
        <TableCell>
          <Chip label={q.questionType} size="small" variant="outlined" sx={{ fontSize: '0.65rem' }} />
        </TableCell>
        <TableCell>
          <Typography variant="caption">{q.userName}</Typography>
        </TableCell>
        <TableCell align="right">
          <Tooltip title={t('moderation.approve')}>
            <IconButton size="small" color="success" onClick={onApprove} disabled={approving}>
              <CheckCircleIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title={t('moderation.reject')}>
            <IconButton size="small" color="error" onClick={onReject}>
              <CancelIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title={t('moderation.viewQuestion')}>
            <IconButton size="small" onClick={onView}>
              <VisibilityIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </TableCell>
      </TableRow>

      {/* Expanded content */}
      <TableRow>
        <TableCell colSpan={9} sx={{ py: 0 }}>
          <Collapse in={expanded} timeout="auto" unmountOnExit>
            <Box sx={{ p: 2 }}>
              <Typography variant="subtitle2" gutterBottom>{t('moderation.question')}</Typography>
              <Typography variant="body2" sx={{ mb: 2, whiteSpace: 'pre-wrap' }}>
                {questionText}
              </Typography>
              {proof && (
                <>
                  <Typography variant="subtitle2" gutterBottom>{t('moderation.proof')}</Typography>
                  <Typography variant="body2" sx={{ mb: 2, whiteSpace: 'pre-wrap' }}>
                    {proof}
                  </Typography>
                </>
              )}
              {q.options != null && (
                <>
                  <Typography variant="subtitle2" gutterBottom>Options</Typography>
                  <Paper variant="outlined" sx={{ p: 1.5, bgcolor: 'grey.50' }}>
                    <pre style={{ margin: 0, fontSize: '0.75rem', whiteSpace: 'pre-wrap' }}>
                      {JSON.stringify(q.options, null, 2)}
                    </pre>
                  </Paper>
                </>
              )}
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
}
