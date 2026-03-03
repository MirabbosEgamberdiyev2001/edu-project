import { useState } from 'react';
import {
  Box, Card, CardContent, Typography, Chip, Grid,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, TextField, CircularProgress,
} from '@mui/material';
import AssignmentIcon from '@mui/icons-material/Assignment';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import { useTranslation } from 'react-i18next';
import { PageShell } from '@/components/ui';
import { useStudentHomeworks, useSubmitHomework } from '../hooks/useHomework';
import type { HomeworkDto } from '@/types/homework';

export default function StudentHomeworkPage() {
  const { t } = useTranslation('homework');
  const { data, isLoading } = useStudentHomeworks();
  const submitMutation = useSubmitHomework();

  const [submitHw, setSubmitHw] = useState<HomeworkDto | null>(null);
  const [textAnswer, setTextAnswer] = useState('');
  const [file, setFile] = useState<File | null>(null);

  function handleSubmit() {
    if (!submitHw) return;
    submitMutation.mutate(
      { homeworkId: submitHw.id, textAnswer: textAnswer || undefined, file: file || undefined },
      { onSuccess: () => { setSubmitHw(null); setTextAnswer(''); setFile(null); } }
    );
  }

  const homeworks = data?.content ?? [];
  const isPastDeadline = (hw: HomeworkDto) =>
    hw.deadline && new Date(hw.deadline) < new Date();

  return (
    <PageShell title={t('title')} subtitle={t('studentSubtitle')}>
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}><CircularProgress /></Box>
      ) : homeworks.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <AssignmentIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography color="text.secondary">{t('noStudentHomeworks')}</Typography>
        </Box>
      ) : (
        <Grid container spacing={2}>
          {homeworks.map((hw: HomeworkDto) => {
            const sub = hw.mySubmission;
            const canSubmit = hw.status === 'ACTIVE' && !isPastDeadline(hw) &&
              (sub == null || hw.allowResubmission);
            return (
              <Grid item xs={12} md={6} key={hw.id}>
                <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                  <CardContent>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                      <Typography variant="subtitle1" fontWeight={600}>{hw.title}</Typography>
                      {sub ? (
                        <Chip label={sub.status === 'GRADED' ? `${sub.grade}/100` : t('submitted')}
                          color={sub.status === 'GRADED' ? 'success' : 'info'} size="small" />
                      ) : (
                        <Chip label={t('notSubmitted')} color="warning" size="small" />
                      )}
                    </Box>
                    {hw.description && (
                      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                        {hw.description}
                      </Typography>
                    )}
                    {hw.deadline && (
                      <Typography variant="caption" color={isPastDeadline(hw) ? 'error' : 'text.secondary'}>
                        {t('deadline')}: {new Date(hw.deadline).toLocaleString()}
                        {isPastDeadline(hw) && ` ${t('overdue')}`}
                      </Typography>
                    )}
                    {sub?.teacherComment && (
                      <Box sx={{ mt: 1, p: 1, bgcolor: 'info.lighter', borderRadius: 1 }}>
                        <Typography variant="caption" color="info.dark">
                          {t('teacherComment')}: {sub.teacherComment}
                        </Typography>
                      </Box>
                    )}
                    {canSubmit && (
                      <Button size="small" variant="outlined" sx={{ mt: 2 }}
                        onClick={() => { setSubmitHw(hw); setTextAnswer(sub?.textAnswer ?? ''); }}>
                        {sub ? t('resubmit') : t('submit')}
                      </Button>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      )}

      {/* Submit dialog */}
      {submitHw && (
        <Dialog open onClose={() => setSubmitHw(null)} maxWidth="sm" fullWidth>
          <DialogTitle>{submitHw.title}</DialogTitle>
          <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
            <TextField label={t('yourAnswer')} value={textAnswer}
              onChange={(e) => setTextAnswer(e.target.value)}
              fullWidth multiline rows={5}
              placeholder={t('answerPlaceholder')} />

            <Box>
              <Button component="label" startIcon={<UploadFileIcon />} variant="outlined" size="small">
                {file ? file.name : t('attachFile')}
                <input type="file" hidden onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
              </Button>
              {submitHw.fileRequired && !file && (
                <Typography variant="caption" color="error" sx={{ ml: 1 }}>
                  {t('fileRequiredError')}
                </Typography>
              )}
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setSubmitHw(null)}>{t('cancel')}</Button>
            <Button variant="contained" onClick={handleSubmit}
              disabled={submitMutation.isPending ||
                (!textAnswer.trim() && !file) ||
                (submitHw.fileRequired && !file)}>
              {t('submit')}
            </Button>
          </DialogActions>
        </Dialog>
      )}
    </PageShell>
  );
}
