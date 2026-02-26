import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  Chip,
  CircularProgress,
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
import { groupApi } from '@/api/groupApi';
import type { GroupDto } from '@/types/group';
import PageBreadcrumbs from '@/components/PageBreadcrumbs';

export default function MyGroupsPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['my-groups-student'],
    queryFn: () => groupApi.getMyGroups({ size: 50 }).then(r => r.data.data?.content || []),
    staleTime: 60_000,
  });

  const groups = data || [];

  return (
    <Box>
      <PageBreadcrumbs items={[{ label: 'Mening guruhlarim' }]} />

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
        <GroupsIcon sx={{ fontSize: 32, color: 'primary.main' }} />
        <Box>
          <Typography variant="h5" fontWeight={700}>
            Mening Guruhlarim
          </Typography>
          <Typography variant="body2" color="text.secondary">
            O'qituvchi tomonidan biriktirilgan guruhlaringiz
          </Typography>
        </Box>
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Guruhlarni yuklashda xato yuz berdi.
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
      ) : groups.length === 0 ? (
        <Paper sx={{ p: 6, textAlign: 'center' }}>
          <GroupsIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            Hali birorta guruhga qo'shilmagan
          </Typography>
          <Typography variant="body2" color="text.disabled" sx={{ mt: 1 }}>
            O'qituvchingiz sizni guruhga qo'shgach, bu yerda ko'rinadi
          </Typography>
        </Paper>
      ) : (
        <>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {groups.length} ta guruh
          </Typography>
          <Grid container spacing={2}>
            {groups.map((group) => (
              <Grid item xs={12} sm={6} md={4} key={group.id}>
                <GroupCard group={group} />
              </Grid>
            ))}
          </Grid>
        </>
      )}
    </Box>
  );
}

function GroupCard({ group }: { group: GroupDto }) {
  const initial = group.name?.[0]?.toUpperCase() || 'G';

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
              {group.name}
            </Typography>
            <Chip
              label={group.status === 'ACTIVE' ? 'Faol' : 'Arxiv'}
              size="small"
              color={group.status === 'ACTIVE' ? 'success' : 'default'}
            />
          </Box>
        </Box>

        {group.description && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            {group.description}
          </Typography>
        )}

        <Divider sx={{ my: 1.5 }} />

        <Stack spacing={1}>
          {group.subjectName && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <MenuBookIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                Fan: <strong>{group.subjectName}</strong>
              </Typography>
            </Box>
          )}
          {group.teacherName && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <PersonIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                O'qituvchi: <strong>{group.teacherName}</strong>
              </Typography>
            </Box>
          )}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <GroupsIcon fontSize="small" color="action" />
            <Typography variant="body2" color="text.secondary">
              {group.memberCount} ta o'quvchi
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <CalendarTodayIcon fontSize="small" color="action" />
            <Typography variant="body2" color="text.secondary">
              {new Date(group.createdAt).toLocaleDateString('uz-UZ')}
            </Typography>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
}
