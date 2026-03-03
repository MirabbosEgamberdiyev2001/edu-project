import { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, Box, Typography, Chip, CircularProgress,
  Table, TableHead, TableRow, TableCell, TableBody,
  TextField, IconButton, Tooltip,
} from '@mui/material';
import GradeIcon from '@mui/icons-material/Grade';
import DownloadIcon from '@mui/icons-material/Download';
import { useTranslation } from 'react-i18next';
import { useHomeworkSubmissions, useGradeSubmission } from '../hooks/useHomework';
import { homeworkApi } from '@/api/homeworkApi';
import type { HomeworkSubmissionDto } from '@/types/homework';

interface Props {
  homeworkId: string;
  onClose: () => void;
}

export default function HomeworkSubmissionsDialog({ homeworkId, onClose }: Props) {
  const { t } = useTranslation('homework');
  const { data, isLoading } = useHomeworkSubmissions(homeworkId);
  const gradeMutation = useGradeSubmission(homeworkId);

  const [gradingId, setGradingId] = useState<string | null>(null);
  const [gradeValue, setGradeValue] = useState('');
  const [comment, setComment] = useState('');
  const [exporting, setExporting] = useState(false);

  const submissions = data?.content ?? [];

  function handleGrade(sub: HomeworkSubmissionDto) {
    setGradingId(sub.id);
    setGradeValue(sub.grade != null ? String(sub.grade) : '');
    setComment(sub.teacherComment ?? '');
  }

  async function handleExport() {
    setExporting(true);
    try {
      const res = await homeworkApi.exportSubmissions(homeworkId);
      const url = URL.createObjectURL(new Blob([res.data]));
      const a = document.createElement('a');
      a.href = url;
      a.download = `homework_${homeworkId}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } finally {
      setExporting(false);
    }
  }

  function handleSaveGrade() {
    if (!gradingId) return;
    gradeMutation.mutate(
      { submissionId: gradingId, data: { grade: parseFloat(gradeValue), teacherComment: comment } },
      { onSuccess: () => { setGradingId(null); setGradeValue(''); setComment(''); } }
    );
  }

  return (
    <Dialog open onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{t('dialog.submissionsTitle')}</DialogTitle>
      <DialogContent>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>
        ) : submissions.length === 0 ? (
          <Typography color="text.secondary" sx={{ py: 4, textAlign: 'center' }}>
            {t('noSubmissions')}
          </Typography>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('tableHeaders.student')}</TableCell>
                <TableCell>{t('tableHeaders.answer')}</TableCell>
                <TableCell>{t('tableHeaders.file')}</TableCell>
                <TableCell>{t('tableHeaders.grade')}</TableCell>
                <TableCell>{t('tableHeaders.status')}</TableCell>
                <TableCell />
              </TableRow>
            </TableHead>
            <TableBody>
              {submissions.map((sub) => (
                <TableRow key={sub.id}>
                  <TableCell>{sub.studentName ?? sub.studentId}</TableCell>
                  <TableCell>
                    {sub.textAnswer
                      ? <Typography variant="caption">{sub.textAnswer.slice(0, 60)}{sub.textAnswer.length > 60 ? '...' : ''}</Typography>
                      : '—'}
                  </TableCell>
                  <TableCell>
                    {sub.fileUrl
                      ? <Button size="small" href={sub.fileUrl} target="_blank">{sub.fileName ?? t('tableHeaders.file')}</Button>
                      : '—'}
                  </TableCell>
                  <TableCell>
                    {sub.grade != null ? `${sub.grade}/100` : '—'}
                  </TableCell>
                  <TableCell>
                    <Chip size="small" label={sub.status}
                      color={sub.status === 'GRADED' ? 'success' : 'default'} />
                  </TableCell>
                  <TableCell>
                    <Tooltip title={t('gradeLabel')}>
                      <IconButton size="small" onClick={() => handleGrade(sub)}>
                        <GradeIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        {/* Grading inline form */}
        {gradingId && (
          <Box sx={{ mt: 3, p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>{t('gradeLabel')}</Typography>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField size="small" label={t('grade')} type="number" value={gradeValue}
                onChange={(e) => setGradeValue(e.target.value)} inputProps={{ min: 0, max: 100 }}
                sx={{ width: 120 }} />
              <TextField size="small" label={t('commentOptional')} value={comment}
                onChange={(e) => setComment(e.target.value)} fullWidth />
            </Box>
            <Box sx={{ display: 'flex', gap: 1, mt: 1 }}>
              <Button size="small" onClick={() => setGradingId(null)}>{t('cancel')}</Button>
              <Button size="small" variant="contained" onClick={handleSaveGrade}
                disabled={!gradeValue || gradeMutation.isPending}>
                {t('save')}
              </Button>
            </Box>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button
          startIcon={<DownloadIcon />}
          onClick={handleExport}
          disabled={exporting || submissions.length === 0}
          color="success"
        >
          {exporting ? t('downloading') : t('excelExport')}
        </Button>
        <Box sx={{ flexGrow: 1 }} />
        <Button onClick={onClose}>{t('close')}</Button>
      </DialogActions>
    </Dialog>
  );
}
