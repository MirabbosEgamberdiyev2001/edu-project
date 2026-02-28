import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  Chip,
  Avatar,
  Divider,
  Alert,
  Skeleton,
  Stack,
} from '@mui/material';
import GroupsIcon from '@mui/icons-material/Groups';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import PersonIcon from '@mui/icons-material/Person';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import { useTranslation } from 'react-i18next';
import { groupApi } from '@/api/groupApi';
import type { GroupDto } from '@/types/group';
import { PageShell } from '@/components/ui';

export default function MyGroupsPage() {
  const { t } = useTranslation('testTaking');
  const { data, isLoading, isError } = useQuery({
    queryKey: ['my-groups-student'],
    queryFn: () => groupApi.getMyGroups({ size: 50 }).then(r => {
      const content = r.data.data?.content;
      return Array.isArray(content) ? content : [];
    }),
    staleTime: 60_000,
  });

  const groups = Array.isArray(data) ? data : [];

  return (
    <PageShell title={t('groups.title')} subtitle={t('groups.subtitle')}>

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {t('groups.errorLoading')}
        </Alert>
      )}

      {isLoading ? (
        <Grid container spacing={2}>
          {Array.from({ length: 4 }).map((_, i) => (
            <Grid item xs={12} sm={6} key={i}>
              <Skeleton variant="rounded" height={160} />
            </Grid>
          ))}
        </Grid>
      ) : isError ? null : groups.length === 0 ? (
        <Paper sx={{ p: 6, textAlign: 'center' }}>
          <GroupsIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            {t('groups.empty')}
          </Typography>
          <Typography variant="body2" color="text.disabled" sx={{ mt: 1 }}>
            {t('groups.emptyHint')}
          </Typography>
        </Paper>
      ) : (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {t('groups.countDisplay', { count: groups.length })}
          </Typography>
          <Grid container spacing={2}>
            {groups.filter(Boolean).map((group) => (
              <Grid item xs={12} sm={6} md={4} key={group.id}>
                <GroupCard group={group} />
              </Grid>
            ))}
          </Grid>
        </>
      )}
    </PageShell>
  );
}

function GroupCard({ group }: { group: GroupDto }) {
  const { t } = useTranslation('testTaking');

  // Defensive: coerce any unexpected type to a safe renderable string
  const name = group.name != null && typeof group.name === 'string' ? group.name : '';
  const initial = name[0]?.toUpperCase() || 'G';
  const isActive = group.status === 'ACTIVE';
  const memberCount = typeof group.memberCount === 'number' ? group.memberCount : 0;
  const subjectName = typeof group.subjectName === 'string' ? group.subjectName : null;
  const teacherName = typeof group.teacherName === 'string' ? group.teacherName : null;
  const description = typeof group.description === 'string' ? group.description : null;
  let formattedDate = '';
  try {
    if (group.createdAt) formattedDate = new Date(group.createdAt).toLocaleDateString();
  } catch { /* invalid date — skip */ }

  return (
    <Card
      sx={{
        height: '100%',
        border: '1px solid',
        borderColor: 'divider',
        transition: 'box-shadow 0.2s',
        '&:hover': { boxShadow: 3 },
      }}
    >
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          <Avatar sx={{ bgcolor: 'primary.main', width: 48, height: 48, fontSize: '1.2rem' }}>
            {initial}
          </Avatar>
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography variant="subtitle1" fontWeight={700} noWrap>
              {name}
            </Typography>
            <Chip
              label={isActive ? t('groups.activeStatus') : t('groups.archiveStatus')}
              size="small"
              color={isActive ? 'success' : 'default'}
            />
          </Box>
        </Box>

        {description && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            {description}
          </Typography>
        )}

        <Divider sx={{ my: 1.5 }} />

        <Stack spacing={1}>
          {subjectName && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <MenuBookIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                {t('groups.subjectLabel')}: <strong>{subjectName}</strong>
              </Typography>
            </Box>
          )}
          {teacherName && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <PersonIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                {t('groups.teacherLabel')}: <strong>{teacherName}</strong>
              </Typography>
            </Box>
          )}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <GroupsIcon fontSize="small" color="action" />
            <Typography variant="body2" color="text.secondary">
              {t('groups.membersLabel', { count: memberCount })}
            </Typography>
          </Box>
          {formattedDate && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <CalendarTodayIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                {formattedDate}
              </Typography>
            </Box>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}
