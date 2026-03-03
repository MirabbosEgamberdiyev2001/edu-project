import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Paper,
  Chip,
  Avatar,
  Divider,
  Tab,
  Tabs,
  Grid,
  Button,
  Skeleton,
  Alert,
  Stack,
  LinearProgress,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AssignmentIcon from '@mui/icons-material/Assignment';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import PersonIcon from '@mui/icons-material/Person';
import GroupsIcon from '@mui/icons-material/Groups';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import TimerIcon from '@mui/icons-material/Timer';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import HomeWorkIcon from '@mui/icons-material/HomeWork';
import UploadIcon from '@mui/icons-material/Upload';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { useTranslation } from 'react-i18next';
import { groupApi } from '@/api/groupApi';
import { testTakingApi } from '@/api/testTakingApi';
import { homeworkApi } from '@/api/homeworkApi';
import { useAttemptMutations } from '@/features/testTaking/hooks/useAttemptMutations';
import { PageShell } from '@/components/ui';
import type { AssignmentDto } from '@/types/assignment';
import type { HomeworkDto } from '@/types/homework';

export default function StudentGroupDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation(['testTaking', 'assignment', 'homework']);
  const [tab, setTab] = useState(0);

  const { data: group, isLoading: groupLoading, isError: groupError } = useQuery({
    queryKey: ['group', id],
    queryFn: () => groupApi.getGroup(id!).then(r => r.data.data),
    enabled: !!id,
  });

  const { data: assignmentsPage, isLoading: assignmentsLoading } = useQuery({
    queryKey: ['test-taking', 'available', { size: 100 }],
    queryFn: async ({ signal }) => {
      const { data } = await testTakingApi.getAvailableAssignments({ size: 100 }, signal);
      return data.data;
    },
    enabled: !!id,
  });

  const assignments = (assignmentsPage?.content ?? []).filter(a => a.groupId === id);

  const { data: homeworksPage, isLoading: homeworksLoading } = useQuery({
    queryKey: ['homeworks', 'student', id],
    queryFn: () => homeworkApi.getStudentHomeworks(id, 0, 50).then(r => r.data.data),
    enabled: !!id,
  });

  const homeworks = homeworksPage?.content ?? [];

  const { startAttempt } = useAttemptMutations();

  const handleStart = (assignmentId: string) => {
    startAttempt.mutate(assignmentId, {
      onSuccess: ({ data: resp }) => {
        navigate(`/exam/${resp.data.id}`);
      },
    });
  };

  if (groupLoading) {
    return (
      <PageShell title="" subtitle="">
        <Skeleton variant="rounded" height={160} sx={{ mb: 3 }} />
        <Skeleton variant="rounded" height={400} />
      </PageShell>
    );
  }

  if (groupError || !group) {
    return (
      <PageShell title="" subtitle="">
        <Alert severity="error">{t('groups.errorLoading', { ns: 'testTaking' })}</Alert>
      </PageShell>
    );
  }

  const isActive = group.status === 'ACTIVE';

  return (
    <PageShell
      title={group.name}
      subtitle={group.subjectName ?? ''}
      actions={
        <Button
          variant="outlined"
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/my-groups')}
          size="small"
        >
          {t('groups.backToGroups', { ns: 'testTaking' })}
        </Button>
      }
    >
      {/* Group info card */}
      <Paper sx={{ p: 3, mb: 3, border: '1px solid', borderColor: 'divider' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          <Avatar sx={{ bgcolor: 'primary.main', width: 56, height: 56, fontSize: '1.5rem' }}>
            {group.name[0]?.toUpperCase() || 'G'}
          </Avatar>
          <Box>
            <Typography variant="h6" fontWeight={700}>{group.name}</Typography>
            <Chip
              label={isActive ? t('groups.activeStatus', { ns: 'testTaking' }) : t('groups.archiveStatus', { ns: 'testTaking' })}
              size="small"
              color={isActive ? 'success' : 'default'}
            />
          </Box>
        </Box>

        {group.description && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {group.description}
          </Typography>
        )}

        <Divider sx={{ my: 2 }} />

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} flexWrap="wrap">
          {group.subjectName && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <MenuBookIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                {t('groups.subjectLabel', { ns: 'testTaking' })}: <strong>{group.subjectName}</strong>
              </Typography>
            </Box>
          )}
          {group.teacherName && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <PersonIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                {t('groups.teacherLabel', { ns: 'testTaking' })}: <strong>{group.teacherName}</strong>
              </Typography>
            </Box>
          )}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <GroupsIcon fontSize="small" color="action" />
            <Typography variant="body2" color="text.secondary">
              {t('groups.membersLabel', { ns: 'testTaking', count: group.memberCount })}
            </Typography>
          </Box>
        </Stack>
      </Paper>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)}>
          <Tab
            icon={<AssignmentIcon />}
            iconPosition="start"
            label={`${t('availableTitle', { ns: 'testTaking' })} (${assignmentsLoading ? '…' : assignments.length})`}
          />
          <Tab
            icon={<HomeWorkIcon />}
            iconPosition="start"
            label={`${t('myHomework', { ns: 'testTaking', defaultValue: 'Homework' })} (${homeworksLoading ? '…' : homeworks.length})`}
          />
        </Tabs>
      </Box>

      {/* Assignments tab */}
      {tab === 0 && (
        <>
          {assignmentsLoading ? (
            <Grid container spacing={2}>
              {[0, 1, 2].map(i => <Grid item xs={12} sm={6} md={4} key={i}><Skeleton variant="rounded" height={200} /></Grid>)}
            </Grid>
          ) : assignments.length === 0 ? (
            <Paper sx={{ p: 6, textAlign: 'center' }}>
              <AssignmentIcon sx={{ fontSize: 56, color: 'text.disabled', mb: 2 }} />
              <Typography color="text.secondary">{t('noAvailable', { ns: 'testTaking' })}</Typography>
            </Paper>
          ) : (
            <Grid container spacing={2}>
              {assignments.map(a => (
                <Grid item xs={12} sm={6} md={4} key={a.id}>
                  <AssignmentCard
                    assignment={a}
                    onStart={handleStart}
                    isPending={startAttempt.isPending}
                    t={t}
                  />
                </Grid>
              ))}
            </Grid>
          )}
        </>
      )}

      {/* Homework tab */}
      {tab === 1 && (
        <>
          {homeworksLoading ? (
            <Stack spacing={2}>
              {[0, 1, 2].map(i => <Skeleton key={i} variant="rounded" height={100} />)}
            </Stack>
          ) : homeworks.length === 0 ? (
            <Paper sx={{ p: 6, textAlign: 'center' }}>
              <HomeWorkIcon sx={{ fontSize: 56, color: 'text.disabled', mb: 2 }} />
              <Typography color="text.secondary">{t('noHomework', { ns: 'testTaking', defaultValue: 'No homework yet' })}</Typography>
            </Paper>
          ) : (
            <Stack spacing={2}>
              {homeworks.map(hw => (
                <HomeworkCard key={hw.id} homework={hw} t={t} />
              ))}
            </Stack>
          )}
        </>
      )}
    </PageShell>
  );
}

function AssignmentCard({
  assignment,
  onStart,
  isPending,
  t,
}: {
  assignment: AssignmentDto;
  onStart: (id: string) => void;
  isPending: boolean;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  t: any;
}) {
  const now = new Date();
  const endDate = assignment.endDate ? new Date(assignment.endDate) : null;
  const isExpired = endDate ? endDate < now : false;
  const status = assignment.status;
  const canStart = status === 'ACTIVE' && !isExpired;

  return (
    <Paper sx={{ p: 2.5, display: 'flex', flexDirection: 'column', height: '100%', border: '1px solid', borderColor: 'divider' }}>
      <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 0.5 }}>
        {assignment.title}
      </Typography>
      <Typography variant="body2" color="text.secondary" noWrap sx={{ mb: 1.5 }}>
        {assignment.testTitle}
      </Typography>

      <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 1.5 }}>
        {assignment.durationMinutes && (
          <Chip icon={<TimerIcon />} label={`${assignment.durationMinutes} ${t('minutesShort', { ns: 'testTaking' })}`} size="small" variant="outlined" />
        )}
        <Chip
          label={t(`status.${status}`, { ns: 'assignment', defaultValue: status })}
          size="small"
          color={status === 'ACTIVE' ? 'success' : status === 'COMPLETED' ? 'default' : 'warning'}
        />
      </Box>

      {endDate && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 1 }}>
          <CalendarTodayIcon fontSize="small" color="action" />
          <Typography variant="caption" color={isExpired ? 'error' : 'text.secondary'}>
            {t('deadline', { ns: 'testTaking' })}: {endDate.toLocaleDateString()}
          </Typography>
        </Box>
      )}

      <Box sx={{ flex: 1 }} />

      <Button
        variant={canStart ? 'contained' : 'outlined'}
        startIcon={<PlayArrowIcon />}
        onClick={() => onStart(assignment.id)}
        disabled={!canStart || isPending}
        fullWidth
        sx={{ mt: 1.5 }}
      >
        {t('startTest', { ns: 'testTaking' })}
      </Button>
    </Paper>
  );
}

function HomeworkCard({
  homework,
  t,
}: {
  homework: HomeworkDto;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  t: any;
}) {
  const deadline = homework.deadline ? new Date(homework.deadline) : null;
  const now = new Date();
  const isOverdue = deadline ? deadline < now && !homework.mySubmission : false;
  const submitted = !!homework.mySubmission;
  const graded = homework.mySubmission?.status === 'GRADED';

  return (
    <Paper sx={{ p: 2.5, border: '1px solid', borderColor: submitted ? 'success.light' : isOverdue ? 'error.light' : 'divider' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 2 }}>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography variant="subtitle1" fontWeight={600} noWrap>
            {homework.title}
          </Typography>
          {homework.description && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 1 }}>
              {homework.description}
            </Typography>
          )}

          <Stack direction="row" spacing={1} flexWrap="wrap">
            {deadline && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <CalendarTodayIcon fontSize="small" color={isOverdue ? 'error' : 'action'} />
                <Typography variant="caption" color={isOverdue ? 'error' : 'text.secondary'}>
                  {t('deadline', { ns: 'testTaking' })}: {deadline.toLocaleDateString()}
                </Typography>
              </Box>
            )}
          </Stack>
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 1 }}>
          {graded ? (
            <Chip
              icon={<CheckCircleIcon />}
              label={`${homework.mySubmission!.grade ?? 0} ${t('points', { ns: 'assignment' })}`}
              color="success"
              size="small"
            />
          ) : submitted ? (
            <Chip
              icon={<UploadIcon />}
              label={t('submitted', { ns: 'testTaking', defaultValue: 'Submitted' })}
              color="info"
              size="small"
            />
          ) : isOverdue ? (
            <Chip label={t('overdue', { ns: 'testTaking', defaultValue: 'Overdue' })} color="error" size="small" />
          ) : (
            <Chip label={t('pending', { ns: 'testTaking', defaultValue: 'Pending' })} color="warning" size="small" />
          )}
        </Box>
      </Box>

      {graded && homework.mySubmission?.teacherComment && (
        <Box sx={{ mt: 1.5, p: 1.5, bgcolor: 'success.50', borderRadius: 1, border: '1px solid', borderColor: 'success.light' }}>
          <Typography variant="caption" color="success.dark">
            {t('feedback', { ns: 'assignment' })}: {homework.mySubmission.teacherComment}
          </Typography>
        </Box>
      )}

      {/* Submission progress indicator */}
      {!submitted && !isOverdue && deadline && (
        (() => {
          const created = new Date(homework.createdAt);
          const total = deadline.getTime() - created.getTime();
          const elapsed = now.getTime() - created.getTime();
          const pct = Math.min(100, Math.max(0, (elapsed / total) * 100));
          const daysLeft = Math.ceil((deadline.getTime() - now.getTime()) / 86400000);
          return (
            <Box sx={{ mt: 1.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                <Typography variant="caption" color="text.secondary">
                  {daysLeft} {t('daysLeft', { ns: 'testTaking', defaultValue: 'days left' })}
                </Typography>
                <Typography variant="caption" color="text.secondary">{Math.round(pct)}%</Typography>
              </Box>
              <LinearProgress
                variant="determinate"
                value={pct}
                color={pct > 80 ? 'error' : pct > 50 ? 'warning' : 'primary'}
                sx={{ height: 4, borderRadius: 2 }}
              />
            </Box>
          );
        })()
      )}
    </Paper>
  );
}
