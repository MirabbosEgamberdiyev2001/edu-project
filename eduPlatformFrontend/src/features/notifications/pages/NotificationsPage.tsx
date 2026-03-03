import { useState } from 'react';
import {
  Box,
  Typography,
  List,
  ListItemButton,
  ListItemText,
  Divider,
  Button,
  CircularProgress,
  Pagination,
  Chip,
} from '@mui/material';
import NotificationsNoneIcon from '@mui/icons-material/NotificationsNone';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '@/api/notificationApi';
import { PageShell } from '@/components/ui';
import type { InAppNotificationDto } from '@/types/notification';

function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60_000);
  if (minutes < 1) return 'Hozir';
  if (minutes < 60) return `${minutes} daqiqa oldin`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} soat oldin`;
  return `${Math.floor(hours / 24)} kun oldin`;
}

export default function NotificationsPage() {
  const [page, setPage] = useState(0);
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['notifications', 'page', page],
    queryFn: () => notificationApi.getAll(page, 20).then((r) => r.data.data),
  });

  const markAll = useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });

  const markOne = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });

  const notifications: InAppNotificationDto[] = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;
  const unreadCount = notifications.filter((n) => !n.isRead).length;

  return (
    <PageShell
      title="Bildirishnomalar"
      subtitle={unreadCount > 0 ? `${unreadCount} ta o'qilmagan` : undefined}
      actions={
        unreadCount > 0 ? (
          <Button
            startIcon={<DoneAllIcon />}
            onClick={() => markAll.mutate()}
            disabled={markAll.isPending}
            size="small"
            variant="outlined"
          >
            Hammasini o'qildi deb belgilash
          </Button>
        ) : undefined
      }
    >
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : notifications.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 10 }}>
          <NotificationsNoneIcon sx={{ fontSize: 72, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary" gutterBottom>
            Bildirishnomalar yo'q
          </Typography>
          <Typography variant="body2" color="text.disabled">
            Yangi bildirishnomalar kelganda bu yerda ko'rinadi
          </Typography>
        </Box>
      ) : (
        <>
          <Box
            sx={{
              bgcolor: 'background.paper',
              borderRadius: 2,
              border: '1px solid',
              borderColor: 'divider',
              overflow: 'hidden',
            }}
          >
            <List disablePadding>
              {notifications.map((n, i) => (
                <Box key={n.id}>
                  <ListItemButton
                    onClick={() => { if (!n.isRead) markOne.mutate(n.id); }}
                    sx={{
                      px: 3,
                      py: 2,
                      bgcolor: n.isRead ? 'transparent' : 'action.hover',
                      alignItems: 'flex-start',
                      cursor: n.isRead ? 'default' : 'pointer',
                    }}
                  >
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                          {!n.isRead && (
                            <Chip
                              label="Yangi"
                              size="small"
                              color="primary"
                              sx={{ height: 18, fontSize: 10, px: 0.5 }}
                            />
                          )}
                          <Typography variant="body1" fontWeight={n.isRead ? 400 : 600}>
                            {n.title}
                          </Typography>
                        </Box>
                      }
                      secondary={
                        <>
                          {n.body && (
                            <Typography
                              variant="body2"
                              color="text.secondary"
                              display="block"
                              sx={{ mt: 0.25 }}
                            >
                              {n.body}
                            </Typography>
                          )}
                          <Typography
                            variant="caption"
                            color="text.disabled"
                            display="block"
                            sx={{ mt: 0.5 }}
                          >
                            {timeAgo(n.createdAt)}
                          </Typography>
                        </>
                      }
                    />
                  </ListItemButton>
                  {i < notifications.length - 1 && <Divider component="li" />}
                </Box>
              ))}
            </List>
          </Box>

          {totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
              <Pagination
                count={totalPages}
                page={page + 1}
                onChange={(_, p) => setPage(p - 1)}
                color="primary"
              />
            </Box>
          )}
        </>
      )}
    </PageShell>
  );
}
