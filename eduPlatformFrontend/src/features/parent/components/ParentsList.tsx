import {
  Box,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  Avatar,
  Chip,
  CircularProgress,
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import { useTranslation } from 'react-i18next';
import { useParents } from '../hooks/useParent';

export default function ParentsList() {
  const { t } = useTranslation('parent');
  const { data: parents, isLoading } = useParents();

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress size={24} />
      </Box>
    );
  }

  if (!parents || parents.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary">{t('noParents')}</Typography>
    );
  }

  return (
    <List>
      {parents.map((p) => (
        <ListItem key={p.id}>
          <ListItemAvatar>
            <Avatar><PersonIcon /></Avatar>
          </ListItemAvatar>
          <ListItemText
            primary={p.parentName}
            secondary={new Date(p.pairingDate).toLocaleDateString()}
          />
          <Chip
            label={t(`pairingStatus.${p.pairingStatus}`)}
            size="small"
            color={p.pairingStatus === 'ACTIVE' ? 'success' : 'default'}
          />
        </ListItem>
      ))}
    </List>
  );
}
