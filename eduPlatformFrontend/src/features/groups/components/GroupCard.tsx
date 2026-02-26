import {
  Card,
  CardContent,
  CardActionArea,
  Typography,
  Box,
  IconButton,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Chip,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ArchiveIcon from '@mui/icons-material/Archive';
import VisibilityIcon from '@mui/icons-material/Visibility';
import GroupIcon from '@mui/icons-material/Group';
import PeopleIcon from '@mui/icons-material/People';
import SchoolIcon from '@mui/icons-material/School';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import type { GroupDto } from '@/types/group';
import { GroupStatus } from '@/types/group';

interface GroupCardProps {
  group: GroupDto;
  onEdit: (group: GroupDto) => void;
  onDelete: (group: GroupDto) => void;
  onArchive: (group: GroupDto) => void;
}

export default function GroupCard({ group, onEdit, onDelete, onArchive }: GroupCardProps) {
  const { t } = useTranslation('group');
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const isArchived = group.status === GroupStatus.ARCHIVED;

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        borderTop: 3,
        borderColor: isArchived ? 'grey.400' : 'primary.main',
        opacity: isArchived ? 0.7 : 1,
        transition: 'box-shadow 0.2s',
        '&:hover': { boxShadow: 4 },
      }}
    >
      <CardActionArea onClick={() => navigate(`/groups/${group.id}`)}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, minWidth: 0, flex: 1 }}>
              <GroupIcon color={isArchived ? 'disabled' : 'primary'} sx={{ fontSize: 32 }} />
              <Typography variant="h6" fontWeight={600} noWrap sx={{ flex: 1 }}>
                {group.name}
              </Typography>
            </Box>
          </Box>

          {group.description && (
            <Typography
              variant="body2"
              color="text.secondary"
              sx={{
                mb: 2,
                display: '-webkit-box',
                WebkitLineClamp: 2,
                WebkitBoxOrient: 'vertical',
                overflow: 'hidden',
              }}
            >
              {group.description}
            </Typography>
          )}

          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mt: 'auto', flexWrap: 'wrap' }}>
            <Chip
              label={isArchived ? t('archived') : t('active')}
              size="small"
              color={isArchived ? 'default' : 'success'}
              variant="outlined"
            />
            {group.subjectName && (
              <Chip
                icon={<SchoolIcon />}
                label={group.subjectName}
                size="small"
                color="primary"
                variant="outlined"
              />
            )}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <PeopleIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                {t('memberCount', { count: group.memberCount })}
              </Typography>
            </Box>
          </Box>

          <Typography variant="caption" color="text.disabled" sx={{ display: 'block', mt: 1.5 }}>
            {t('createdAt')}: {new Date(group.createdAt).toLocaleDateString()}
          </Typography>
        </CardContent>
      </CardActionArea>

      {/* Actions menu button - outside CardActionArea */}
      <IconButton
        size="small"
        onClick={(e) => { e.stopPropagation(); setAnchorEl(e.currentTarget); }}
        sx={{ position: 'absolute', top: 8, right: 8 }}
      >
        <MoreVertIcon fontSize="small" />
      </IconButton>

      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
        <MenuItem onClick={() => { setAnchorEl(null); navigate(`/groups/${group.id}`); }}>
          <ListItemIcon><VisibilityIcon fontSize="small" /></ListItemIcon>
          <ListItemText>{t('groupDetail')}</ListItemText>
        </MenuItem>
        {!isArchived && (
          <MenuItem onClick={() => { setAnchorEl(null); onEdit(group); }}>
            <ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>
            <ListItemText>{t('editGroup')}</ListItemText>
          </MenuItem>
        )}
        {!isArchived && (
          <MenuItem onClick={() => { setAnchorEl(null); onArchive(group); }}>
            <ListItemIcon><ArchiveIcon fontSize="small" /></ListItemIcon>
            <ListItemText>{t('archiveGroup')}</ListItemText>
          </MenuItem>
        )}
        <MenuItem onClick={() => { setAnchorEl(null); onDelete(group); }} sx={{ color: 'error.main' }}>
          <ListItemIcon><DeleteIcon fontSize="small" color="error" /></ListItemIcon>
          <ListItemText>{t('deleteGroup')}</ListItemText>
        </MenuItem>
      </Menu>
    </Card>
  );
}
