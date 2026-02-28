import { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  Avatar,
  Chip,
  CircularProgress,
  IconButton,
  Tooltip,
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import PeopleOutlineIcon from '@mui/icons-material/PeopleOutline';
import LinkOffIcon from '@mui/icons-material/LinkOff';
import { useTranslation } from 'react-i18next';
import { useParents } from '../hooks/useParent';
import { useParentMutations } from '../hooks/useParentMutations';
import RevokePairingDialog from './RevokePairingDialog';
import type { ParentChildDto } from '@/types/parent';
import { PairingStatus } from '@/types/parent';

export default function ParentsList() {
  const { t } = useTranslation('parent');
  const { data: parents, isLoading } = useParents();
  const { revokePairing } = useParentMutations();
  const [revokeTarget, setRevokeTarget] = useState<ParentChildDto | null>(null);

  const handleConfirmRevoke = () => {
    if (!revokeTarget) return;
    revokePairing.mutate(revokeTarget.id, {
      onSuccess: () => setRevokeTarget(null),
    });
  };

  return (
    <>
      <Paper sx={{ p: 3, height: '100%' }}>
        <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>
          {t('linkedParents')}
        </Typography>

        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress size={24} />
          </Box>
        ) : !parents || parents.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 5 }}>
            <PeopleOutlineIcon sx={{ fontSize: 52, color: 'text.disabled', mb: 1.5 }} />
            <Typography variant="body2" color="text.secondary" fontWeight={500}>
              {t('noParents')}
            </Typography>
            <Typography variant="caption" color="text.disabled" sx={{ display: 'block', mt: 0.5 }}>
              {t('generateCodePrompt')}
            </Typography>
          </Box>
        ) : (
          <List disablePadding>
            {parents.map((p, i) => (
              <ListItem
                key={p.id}
                sx={{
                  px: 0,
                  py: 1.5,
                  borderBottom: i < parents.length - 1 ? '1px solid' : 'none',
                  borderColor: 'divider',
                }}
                secondaryAction={
                  p.status === PairingStatus.ACTIVE ? (
                    <Tooltip title={t('unlinkChild')} placement="left">
                      <IconButton
                        edge="end"
                        size="small"
                        color="error"
                        onClick={() => setRevokeTarget(p)}
                      >
                        <LinkOffIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  ) : null
                }
              >
                <ListItemAvatar>
                  <Avatar sx={{ bgcolor: 'secondary.main', width: 40, height: 40 }}>
                    <PersonIcon fontSize="small" />
                  </Avatar>
                </ListItemAvatar>
                <ListItemText
                  primary={
                    <Typography variant="body2" fontWeight={600}>
                      {p.parentName}
                    </Typography>
                  }
                  secondary={
                    <Box component="span" sx={{ display: 'flex', alignItems: 'center', gap: 0.75, mt: 0.5, flexWrap: 'wrap' }}>
                      <Chip
                        label={t(`pairingStatus.${p.status}`)}
                        size="small"
                        color={p.status === PairingStatus.ACTIVE ? 'success' : 'default'}
                        variant="outlined"
                      />
                      {p.pairedAt && (
                        <Typography component="span" variant="caption" color="text.disabled">
                          {new Date(p.pairedAt).toLocaleDateString()}
                        </Typography>
                      )}
                    </Box>
                  }
                />
              </ListItem>
            ))}
          </List>
        )}
      </Paper>

      <RevokePairingDialog
        open={!!revokeTarget}
        onClose={() => setRevokeTarget(null)}
        onConfirm={handleConfirmRevoke}
        childName={revokeTarget?.parentName}
        isPending={revokePairing.isPending}
      />
    </>
  );
}
