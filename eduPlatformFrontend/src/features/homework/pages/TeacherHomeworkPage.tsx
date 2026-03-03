import { useState } from 'react';
import {
  Box, Button, Card, CardContent, Typography, Chip, Grid,
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, MenuItem, CircularProgress,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import AssignmentIcon from '@mui/icons-material/Assignment';
import { useTranslation } from 'react-i18next';
import { PageShell } from '@/components/ui';
import { useTeacherHomeworks, useCreateHomework } from '../hooks/useHomework';
import HomeworkSubmissionsDialog from './HomeworkSubmissionsDialog';
import type { CreateHomeworkRequest, HomeworkDto } from '@/types/homework';
import { useGroups } from '@/features/groups/hooks/useGroups';

export default function TeacherHomeworkPage() {
  const { t } = useTranslation('homework');
  const { data, isLoading } = useTeacherHomeworks();
  const createMutation = useCreateHomework();
  const { data: groupsData } = useGroups({ page: 0, size: 100 });
  const groups = groupsData?.content ?? [];

  const [createOpen, setCreateOpen] = useState(false);
  const [viewSubmissionsId, setViewSubmissionsId] = useState<string | null>(null);

  const [form, setForm] = useState<CreateHomeworkRequest>({
    title: '', description: '', groupId: undefined,
    allowResubmission: true, fileRequired: false, maxFileSizeMb: 10,
  });
  const [deadline, setDeadline] = useState('');

  function handleCreate() {
    createMutation.mutate(
      { ...form, ...(deadline ? { deadline: new Date(deadline).toISOString() } : {}) },
      { onSuccess: () => { setCreateOpen(false); setForm({ title: '', allowResubmission: true, fileRequired: false, maxFileSizeMb: 10 }); setDeadline(''); } }
    );
  }

  const homeworks = data?.content ?? [];

  return (
    <PageShell
      title={t('title')}
      subtitle={t('teacherSubtitle')}
      actions={
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          {t('create')}
        </Button>
      }
    >
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}><CircularProgress /></Box>
      ) : homeworks.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <AssignmentIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography color="text.secondary">{t('noHomeworks')}</Typography>
        </Box>
      ) : (
        <Grid container spacing={2}>
          {homeworks.map((hw: HomeworkDto) => (
            <Grid item xs={12} md={6} key={hw.id}>
              <Card elevation={0} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2 }}>
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                    <Typography variant="subtitle1" fontWeight={600}>{hw.title}</Typography>
                    <Chip label={hw.status} size="small"
                      color={hw.status === 'ACTIVE' ? 'success' : 'default'} />
                  </Box>
                  {hw.description && (
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                      {hw.description}
                    </Typography>
                  )}
                  <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
                    {hw.deadline && (
                      <Chip size="small" label={`${t('deadline')}: ${new Date(hw.deadline).toLocaleDateString()}`} />
                    )}
                    {hw.fileRequired && <Chip size="small" label={t('fileRequired')} color="warning" />}
                    {hw.allowResubmission && <Chip size="small" label={t('allowResubmission')} />}
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="caption" color="text.secondary">
                      {hw.totalSubmissions ?? 0} {t('submissions')} / {hw.gradedSubmissions ?? 0} {t('graded')}
                    </Typography>
                    <Button size="small" onClick={() => setViewSubmissionsId(hw.id)}>
                      {t('view')}
                    </Button>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Create dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{t('dialog.createTitle')}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
          <TextField label={t('dialog.title')} value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })} fullWidth />
          <TextField label={t('dialog.description')} value={form.description ?? ''}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            fullWidth multiline rows={3} />
          <TextField select label={t('dialog.group')} value={form.groupId ?? ''}
            onChange={(e) => setForm({ ...form, groupId: e.target.value || undefined })} fullWidth>
            <MenuItem value="">{t('dialog.allGroups')}</MenuItem>
            {groups.map((g) => <MenuItem key={g.id} value={g.id}>{g.name}</MenuItem>)}
          </TextField>
          <TextField label={t('dialog.deadline')} type="datetime-local" value={deadline}
            onChange={(e) => setDeadline(e.target.value)} fullWidth
            InputLabelProps={{ shrink: true }} />
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField select label={t('dialog.resubmission')} value={String(form.allowResubmission ?? true)}
              onChange={(e) => setForm({ ...form, allowResubmission: e.target.value === 'true' })} fullWidth>
              <MenuItem value="true">{t('allowResubmissionLabel.yes')}</MenuItem>
              <MenuItem value="false">{t('allowResubmissionLabel.no')}</MenuItem>
            </TextField>
            <TextField select label={t('dialog.fileRequired')} value={String(form.fileRequired ?? false)}
              onChange={(e) => setForm({ ...form, fileRequired: e.target.value === 'true' })} fullWidth>
              <MenuItem value="false">{t('fileRequiredLabel.no')}</MenuItem>
              <MenuItem value="true">{t('fileRequiredLabel.yes')}</MenuItem>
            </TextField>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>{t('cancel')}</Button>
          <Button variant="contained" onClick={handleCreate}
            disabled={!form.title.trim() || createMutation.isPending}>
            {t('create')}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Submissions dialog */}
      {viewSubmissionsId && (
        <HomeworkSubmissionsDialog
          homeworkId={viewSubmissionsId}
          onClose={() => setViewSubmissionsId(null)}
        />
      )}
    </PageShell>
  );
}
