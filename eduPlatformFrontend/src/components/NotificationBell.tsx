import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Badge,
  IconButton,
  Popover,
  Box,
  Typography,
  List,
  ListItemButton,
  ListItemText,
  Divider,
  Button,
  CircularProgress,
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '@/api/notificationApi';
import type { InAppNotificationDto } from '@/types/notification';

function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const minutes = Math.floor(diff / 60_000);
  if (minutes < 1) return 'hozir';
  if (minutes < 60) return `${minutes} daqiqa oldin`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} soat oldin`;
  const days = Math.floor(hours / 24);
  return `${days} kun oldin`;
}

export default function NotificationBell() {
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null);
  const qc = useQueryClient();
  const navigate = useNavigate();

  const { data: countData } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => notificationApi.getUnreadCount().then((r) => r.data.data),
    refetchInterval: 30_000, // poll every 30s
  });

  const { data: listData, isLoading } = useQuery({
    queryKey: ['notifications', 'list'],
    queryFn: () => notificationApi.getAll(0, 10).then((r) => r.data.data),
    enabled: Boolean(anchorEl),
  });

  const markAll = useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const markOne = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const unreadCount = countData?.count ?? 0;
  const notifications: InAppNotificationDto[] = listData?.content ?? [];

  function handleOpen(e: React.MouseEvent<HTMLButtonElement>) {
    setAnchorEl(e.currentTarget);
  }

  function handleClose() {
    setAnchorEl(null);
  }

  function handleMarkOne(n: InAppNotificationDto) {
    if (!n.isRead) markOne.mutate(n.id);
  }

  return (
    <>
      <IconButton onClick={handleOpen} size="small">
        <Badge badgeContent={unreadCount > 0 ? unreadCount : undefined} color="error" max={99}>
          <NotificationsIcon />
        </Badge>
      </IconButton>

      <Popover
        open={Boolean(anchorEl)}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        PaperProps={{ sx: { width: 360, maxHeight: 480, display: 'flex', flexDirection: 'column' } }}
      >
        <Box sx={{ px: 2, py: 1.5, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="subtitle1" fontWeight={600}>
            Bildirishnomalar
          </Typography>
          {unreadCount > 0 && (
            <Button size="small" onClick={() => markAll.mutate()}>
              Hammasini o'qildi
            </Button>
          )}
        </Box>
        <Divider />

        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress size={24} />
          </Box>
        ) : notifications.length === 0 ? (
          <Box sx={{ py: 4, textAlign: 'center' }}>
            <Typography variant="body2" color="text.secondary">
              Bildirishnomalar yo'q
            </Typography>
          </Box>
        ) : (
          <List disablePadding sx={{ overflowY: 'auto', flexGrow: 1 }}>
            {notifications.map((n, i) => (
              <Box key={n.id}>
                <ListItemButton
                  onClick={() => handleMarkOne(n)}
                  sx={{
                    px: 2, py: 1.5,
                    bgcolor: n.isRead ? 'transparent' : 'action.hover',
                    alignItems: 'flex-start',
                  }}
                >
                  <ListItemText
                    primary={
                      <Typography variant="body2" fontWeight={n.isRead ? 400 : 600}>
                        {n.title}
                      </Typography>
                    }
                    secondary={
                      <>
                        {n.body && (
                          <Typography variant="caption" color="text.secondary" display="block">
                            {n.body}
                          </Typography>
                        )}
                        <Typography variant="caption" color="text.disabled">
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
        )}

        <Divider />
        <Box sx={{ p: 1 }}>
          <Button
            size="small"
            fullWidth
            onClick={() => { handleClose(); navigate('/notifications'); }}
          >
            Barchasini ko'rish
          </Button>
        </Box>
      </Popover>
    </>
  );
}
