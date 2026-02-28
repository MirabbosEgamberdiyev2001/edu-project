import { useState } from 'react';
import {
  Card,
  CardContent,
  CardActions,
  Typography,
  Box,
  Chip,
  Avatar,
  Button,
  IconButton,
  Menu,
  MenuItem,
} from '@mui/material';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import LinkOffIcon from '@mui/icons-material/LinkOff';
import EmailIcon from '@mui/icons-material/Email';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useParentMutations } from '../hooks/useParentMutations';
import RevokePairingDialog from './RevokePairingDialog';
import type { ParentChildDto } from '@/types/parent';
import { PairingStatus } from '@/types/parent';

interface ChildCardProps {
  pairing: ParentChildDto;
}

const STATUS_COLOR: Record<PairingStatus, 'success' | 'warning' | 'error' | 'default'> = {
  [PairingStatus.ACTIVE]: 'success',
  [PairingStatus.PENDING]: 'warning',
  [PairingStatus.REVOKED]: 'error',
  [PairingStatus.EXPIRED]: 'default',
};

const AVATAR_COLORS = ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4'];

function getAvatarColor(name: string) {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
}

function getInitials(name: string) {
  return name
    .split(' ')
    .filter(Boolean)
    .map((n) => n[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

export default function ChildCard({ pairing }: ChildCardProps) {
  const { t } = useTranslation('parent');
  const navigate = useNavigate();
  const { revokePairing } = useParentMutations();
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);
  const [revokeOpen, setRevokeOpen] = useState(false);

  const isActive = pairing.status === PairingStatus.ACTIVE;

  const handleRevoke = () => {
    setMenuAnchor(null);
    setRevokeOpen(true);
  };

  const handleConfirmRevoke = () => {
    revokePairing.mutate(pairing.id, {
      onSuccess: () => setRevokeOpen(false),
    });
  };

  const avatarColor = getAvatarColor(pairing.childName || '?');
  const initials = getInitials(pairing.childName || '?');

  return (
    <>
      <Card
        sx={{
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          borderTop: '3px solid',
          borderColor: isActive ? 'primary.main' : 'divider',
          transition: 'box-shadow 0.2s',
          '&:hover': { boxShadow: 4 },
        }}
      >
        <CardContent sx={{ flex: 1, pb: 1 }}>
          {/* Header */}
          <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5, mb: 2 }}>
            <Avatar
              sx={{
                bgcolor: avatarColor,
                width: 50,
                height: 50,
                fontSize: '1rem',
                fontWeight: 700,
                flexShrink: 0,
              }}
            >
              {initials}
            </Avatar>

            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="h6" fontWeight={700} noWrap sx={{ lineHeight: 1.3, mb: 0.5 }}>
                {pairing.childName}
              </Typography>
              <Chip
                label={t(`pairingStatus.${pairing.status}`)}
                size="small"
                color={STATUS_COLOR[pairing.status]}
                variant="outlined"
              />
            </Box>

            {isActive && (
              <IconButton
                size="small"
                onClick={(e) => setMenuAnchor(e.currentTarget)}
                sx={{ mt: -0.5, mr: -0.5, color: 'text.secondary' }}
              >
                <MoreVertIcon fontSize="small" />
              </IconButton>
            )}
          </Box>

          {/* Email */}
          {pairing.childEmail && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.75 }}>
              <EmailIcon sx={{ fontSize: 15, color: 'text.disabled', flexShrink: 0 }} />
              <Typography variant="body2" color="text.secondary" noWrap>
                {pairing.childEmail}
              </Typography>
            </Box>
          )}

          {/* Paired date */}
          {pairing.pairedAt && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <CalendarTodayIcon sx={{ fontSize: 15, color: 'text.disabled', flexShrink: 0 }} />
              <Typography variant="body2" color="text.secondary">
                {t('pairedOn')}: {new Date(pairing.pairedAt).toLocaleDateString()}
              </Typography>
            </Box>
          )}
        </CardContent>

        {isActive && (
          <CardActions sx={{ px: 2, pb: 2 }}>
            <Button
              variant="contained"
              size="small"
              fullWidth
              endIcon={<ArrowForwardIcon />}
              onClick={() => navigate(`/my-children/${pairing.childId}`)}
            >
              {t('viewDetails')}
            </Button>
          </CardActions>
        )}
      </Card>

      {/* 3-dot menu */}
      <Menu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={() => setMenuAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      >
        <MenuItem onClick={handleRevoke} sx={{ color: 'error.main', gap: 1 }}>
          <LinkOffIcon fontSize="small" />
          {t('unlinkChild')}
        </MenuItem>
      </Menu>

      {/* Revoke confirm dialog */}
      <RevokePairingDialog
        open={revokeOpen}
        onClose={() => setRevokeOpen(false)}
        onConfirm={handleConfirmRevoke}
        childName={pairing.childName}
        isPending={revokePairing.isPending}
      />
    </>
  );
}
