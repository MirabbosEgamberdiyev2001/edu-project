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
  Tabs,
  Tab,
  Alert,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import VisibilityIcon from '@mui/icons-material/Visibility';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import QuizIcon from '@mui/icons-material/Quiz';
import AssignmentIcon from '@mui/icons-material/Assignment';
import { resolveTranslation } from '@/utils/i18nUtils';
import type { QuestionDto } from '@/types/question';
import type { TestHistoryDto } from '@/types/test';
import {
  usePendingQuestions,
  useApproveQuestion,
  useRejectQuestion,
  useBulkApprove,
  useBulkReject,
} from '../hooks/useModeration';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { testApi } from '@/api/testApi';

const DIFFICULTY_COLORS: Record<string, 'success' | 'warning' | 'error'> = {
  EASY: 'success',
  MEDIUM: 'warning',
  HARD: 'error',
};

const CATEGORY_LABELS: Record<string, string> = {
  DTM: '🎓 DTM',
  SCHOOL: '🏫 Maktab',
  OLYMPIAD: '🏆 Olimpiada',
  CERTIFICATE: '📜 Sertifikat',
  ATTESTATSIYA: '📋 Attestatsiya',
};

export default function ModerationPage() {
  const { t } = useTranslation('admin');
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState(0);

  // ===== Question Moderation State =====
  const [questionPage, setQuestionPage] = useState(0);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [rejectDialogId, setRejectDialogId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [bulkRejectOpen, setBulkRejectOpen] = useState(false);
  const [bulkRejectReason, setBulkRejectReason] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const questionParams = useMemo(() => ({ page: questionPage, size: 20 }), [questionPage]);
  const { data: questionData, isLoading: questionsLoading } = usePendingQuestions(questionParams);

  const approveQuestion = useApproveQuestion();
  const rejectQuestion = useRejectQuestion();
  const bulkApprove = useBulkApprove();
  const bulkReject = useBulkReject();

  // ===== Global Test Moderation State =====
  const [testPage, setTestPage] = useState(0);
  const [rejectTestId, setRejectTestId] = useState<string | null>(null);
  const [testRejectReason, setTestRejectReason] = useState('');

  const { data: pendingTestsData, isLoading: testsLoading } = useQuery({
    queryKey: ['pending-global-tests', testPage],
    queryFn: () => testApi.getPendingGlobalTests({ page: testPage, size: 15 }).then(r => r.data.data),
    enabled: activeTab === 1,
    staleTime: 30_000,
  });

  const approveTestMutation = useMutation({
    mutationFn: (id: string) => testApi.approveGlobalTest(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['pending-global-tests'] }),
  });

  const rejectTestMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => testApi.rejectGlobalTest(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pending-global-tests'] });
      setRejectTestId(null);
      setTestRejectReason('');
    },
  });

  // ===== Question handlers =====
  const toggleSelect = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (!questionData) return;
    if (selected.size === questionData.content.length) {
      setSelected(new Set());
    } else {
      setSelected(new Set(questionData.content.map((q) => q.id)));
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

  const pendingTests = pendingTestsData?.content || [];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>{t('moderation.title')}</Typography>
          <Typography variant="body2" color="text.secondary">{t('moderation.subtitle')}</Typography>
        </Box>
      </Box>

      {/* Tabs */}
      <Paper sx={{ mb: 3 }}>
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)}>
          <Tab
            icon={<QuizIcon />}
            iconPosition="start"
            label={`Savollar${questionData ? ` (${questionData.totalElements})` : ''}`}
          />
          <Tab
            icon={<AssignmentIcon />}
            iconPosition="start"
            label={`Global Testlar${pendingTestsData ? ` (${pendingTestsData.totalElements})` : ''}`}
          />
        </Tabs>
      </Paper>

      {/* ===== TAB 0: Questions ===== */}
      {activeTab === 0 && (
        <Box>
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

          {questionsLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
              <CircularProgress />
            </Box>
          ) : !questionData || questionData.content.length === 0 ? (
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
                          checked={selected.size === questionData.content.length && questionData.content.length > 0}
                          indeterminate={selected.size > 0 && selected.size < questionData.content.length}
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
                    {questionData.content.map((q) => (
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

              {questionData.totalPages > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                  <Pagination
                    count={questionData.totalPages}
                    page={questionPage + 1}
                    onChange={(_, p) => setQuestionPage(p - 1)}
                    color="primary"
                  />
                </Box>
              )}
            </>
          )}
        </Box>
      )}

      {/* ===== TAB 1: Global Tests ===== */}
      {activeTab === 1 && (
        <Box>
          {testsLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
              <CircularProgress />
            </Box>
          ) : pendingTests.length === 0 ? (
            <Paper sx={{ p: 4, textAlign: 'center' }}>
              <AssignmentIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
              <Typography color="text.secondary">
                Hozircha tekshiruv kutayotgan global test yo'q
              </Typography>
            </Paper>
          ) : (
            <>
              <TableContainer component={Paper}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Test nomi</TableCell>
                      <TableCell>Fan</TableCell>
                      <TableCell>Kategoriya</TableCell>
                      <TableCell>Savollar</TableCell>
                      <TableCell>O'qituvchi</TableCell>
                      <TableCell>Yuborildi</TableCell>
                      <TableCell align="right">Amallar</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {pendingTests.map((test) => (
                      <GlobalTestRow
                        key={test.id}
                        test={test}
                        onApprove={() => approveTestMutation.mutate(test.id)}
                        onReject={() => { setRejectTestId(test.id); setTestRejectReason(''); }}
                        approving={approveTestMutation.isPending}
                      />
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>

              {pendingTestsData && pendingTestsData.totalPages > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                  <Pagination
                    count={pendingTestsData.totalPages}
                    page={testPage + 1}
                    onChange={(_, p) => setTestPage(p - 1)}
                    color="primary"
                  />
                </Box>
              )}
            </>
          )}

          {/* Test Reject Dialog */}
          <Dialog open={!!rejectTestId} onClose={() => setRejectTestId(null)} maxWidth="sm" fullWidth>
            <DialogTitle>Testni rad etish</DialogTitle>
            <DialogContent>
              <Typography variant="body2" sx={{ mb: 2 }}>
                Rad etish sababini kiriting. O'qituvchi bu sababni ko'radi.
              </Typography>
              <TextField
                fullWidth
                size="small"
                label="Rad etish sababi"
                placeholder="Masalan: Savollar sifatsiz, to'g'ri javob yo'q..."
                value={testRejectReason}
                onChange={(e) => setTestRejectReason(e.target.value)}
                multiline
                rows={3}
                autoFocus
              />
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setRejectTestId(null)}>Bekor qilish</Button>
              <Button
                variant="contained"
                color="error"
                onClick={() => rejectTestId && rejectTestMutation.mutate({ id: rejectTestId, reason: testRejectReason })}
                disabled={!testRejectReason.trim() || rejectTestMutation.isPending}
              >
                Rad etish
              </Button>
            </DialogActions>
          </Dialog>
        </Box>
      )}

      {/* Single Question Reject Dialog */}
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

// ===== GlobalTestRow Component =====
function GlobalTestRow({ test, onApprove, onReject, approving }: {
  test: TestHistoryDto;
  onApprove: () => void;
  onReject: () => void;
  approving: boolean;
}) {
  const { t } = useTranslation('admin');
  const category = test.category || '';
  return (
    <TableRow hover>
      <TableCell>
        <Typography variant="body2" fontWeight={600}>{test.title}</Typography>
        {test.difficultyDistribution && (
          <Box sx={{ display: 'flex', gap: 0.5, mt: 0.5 }}>
            {Object.entries(test.difficultyDistribution).map(([key, val]) => (
              <Chip
                key={key}
                size="small"
                label={`${key === 'easy' ? 'O' : key === 'medium' ? "O'" : 'Q'}: ${val}%`}
                color={key === 'easy' ? 'success' : key === 'medium' ? 'warning' : 'error'}
                sx={{ fontSize: '0.6rem' }}
              />
            ))}
          </Box>
        )}
      </TableCell>
      <TableCell>
        <Typography variant="caption">{test.subjectName}</Typography>
      </TableCell>
      <TableCell>
        {category && (
          <Chip label={CATEGORY_LABELS[category] || category} size="small" variant="outlined" />
        )}
      </TableCell>
      <TableCell>
        <Typography variant="body2">{test.questionCount}</Typography>
      </TableCell>
      <TableCell>
        <Typography variant="caption">{test.teacherName || '—'}</Typography>
      </TableCell>
      <TableCell>
        <Typography variant="caption">
          {test.globalSubmittedAt ? new Date(test.globalSubmittedAt).toLocaleString() : '—'}
        </Typography>
      </TableCell>
      <TableCell align="right">
        <Tooltip title={t('moderation.approveGlobal')}>
          <IconButton size="small" color="success" onClick={onApprove} disabled={approving}>
            <CheckCircleIcon fontSize="small" />
          </IconButton>
        </Tooltip>
        <Tooltip title={t('moderation.reject')}>
          <IconButton size="small" color="error" onClick={onReject}>
            <CancelIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </TableCell>
    </TableRow>
  );
}

// ===== QuestionRow Component (existing) =====
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
