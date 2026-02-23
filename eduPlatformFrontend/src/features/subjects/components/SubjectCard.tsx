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
  Avatar,
  Chip,
  Divider,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ArchiveIcon from '@mui/icons-material/Archive';
import UnarchiveIcon from '@mui/icons-material/Unarchive';
import VisibilityIcon from '@mui/icons-material/Visibility';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import type { SubjectDto } from '@/types/subject';
import { resolveTranslation } from '@/utils/i18nUtils';
import { useAuthStore } from '@/stores/authStore';

interface SubjectCardProps {
  subject: SubjectDto;
  onEdit: (subject: SubjectDto) => void;
  onDelete: (subject: SubjectDto) => void;
  onArchive: (subject: SubjectDto) => void;
  onRestore: (subject: SubjectDto) => void;
}

function isImageUrl(value: string | null): boolean {
  if (!value) return false;
  return /^https?:\/\/.+/i.test(value) || value.startsWith('/');
}

export default function SubjectCard({ subject, onEdit, onDelete, onArchive, onRestore }: SubjectCardProps) {
  const { t } = useTranslation('subject');
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN';

  const displayName = resolveTranslation(subject.nameTranslations) || subject.name;
  const displayDescription = resolveTranslation(subject.descriptionTranslations) || subject.description;

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        borderLeft: 3,
        borderColor: subject.color || 'primary.main',
        opacity: subject.isArchived ? 0.7 : 1,
        transition: 'transform 0.2s, box-shadow 0.2s',
        '&:hover': {
          transform: 'translateY(-2px)',
          boxShadow: 6,
        },
      }}
    >
      <CardActionArea onClick={() => navigate(`/subjects/${subject.id}`)}>
        <CardContent sx={{ pb: 1.5 }}>
          {/* Header: icon + name */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1, minWidth: 0, pr: 3 }}>
            {subject.icon ? (
              isImageUrl(subject.icon) ? (
                <Avatar
                  src={subject.icon}
                  variant="rounded"
                  sx={{ width: 36, height: 36, bgcolor: subject.color || 'primary.main', flexShrink: 0 }}
                >
                  <MenuBookIcon />
                </Avatar>
              ) : (
                <Typography variant="h5" component="span" sx={{ lineHeight: 1, flexShrink: 0 }}>
                  {subject.icon}
                </Typography>
              )
            ) : (
              <Avatar
                variant="rounded"
                sx={{ width: 36, height: 36, bgcolor: subject.color || 'primary.main', flexShrink: 0 }}
              >
                <MenuBookIcon sx={{ fontSize: 22 }} />
              </Avatar>
            )}
            <Box sx={{ minWidth: 0, flex: 1 }}>
              <Typography variant="subtitle1" fontWeight={600} noWrap>
                {displayName}
              </Typography>
              {subject.gradeLevel && (
                <Typography variant="caption" color="text.secondary">
                  {t('form.gradeLevel')}: {subject.gradeLevel}
                </Typography>
              )}
            </Box>
          </Box>

          {/* Description */}
          {displayDescription && (
            <Typography
              variant="body2"
              color="text.secondary"
              sx={{
                mb: 1.5,
                display: '-webkit-box',
                WebkitLineClamp: 2,
                WebkitBoxOrient: 'vertical',
                overflow: 'hidden',
              }}
            >
              {displayDescription}
            </Typography>
          )}

          <Divider sx={{ mb: 1 }} />

          {/* Labeled stats */}
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Chip
              label={`${subject.topicCount} ${t('topicCount').toLowerCase()}`}
              size="small"
              variant="outlined"
              sx={{ height: 24, fontSize: '0.75rem' }}
            />
            <Chip
              label={`${subject.questionCount} ${t('questionCount').toLowerCase()}`}
              size="small"
              variant="outlined"
              sx={{ height: 24, fontSize: '0.75rem' }}
            />
            {subject.testCount > 0 && (
              <Chip
                label={`${subject.testCount} ${t('testCount', 'tests')}`}
                size="small"
                variant="outlined"
                sx={{ height: 24, fontSize: '0.75rem' }}
              />
            )}
          </Box>
        </CardContent>
      </CardActionArea>

      {/* Actions menu button */}
      <IconButton
        size="small"
        onClick={(e) => { e.stopPropagation(); setAnchorEl(e.currentTarget); }}
        sx={{ position: 'absolute', top: 8, right: 8 }}
      >
        <MoreVertIcon fontSize="small" />
      </IconButton>

      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
        <MenuItem onClick={() => { setAnchorEl(null); navigate(`/subjects/${subject.id}`); }}>
          <ListItemIcon><VisibilityIcon fontSize="small" /></ListItemIcon>
          <ListItemText>{t('view')}</ListItemText>
        </MenuItem>
        {isAdmin && (
          <MenuItem onClick={() => { setAnchorEl(null); onEdit(subject); }}>
            <ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>
            <ListItemText>{t('edit')}</ListItemText>
          </MenuItem>
        )}
        {isAdmin && (subject.isArchived ? (
          <MenuItem onClick={() => { setAnchorEl(null); onRestore(subject); }}>
            <ListItemIcon><UnarchiveIcon fontSize="small" /></ListItemIcon>
            <ListItemText>{t('restore')}</ListItemText>
          </MenuItem>
        ) : (
          <MenuItem onClick={() => { setAnchorEl(null); onArchive(subject); }}>
            <ListItemIcon><ArchiveIcon fontSize="small" /></ListItemIcon>
            <ListItemText>{t('archive')}</ListItemText>
          </MenuItem>
        ))}
        {isAdmin && (
          <MenuItem onClick={() => { setAnchorEl(null); onDelete(subject); }} sx={{ color: 'error.main' }}>
            <ListItemIcon><DeleteIcon fontSize="small" color="error" /></ListItemIcon>
            <ListItemText>{t('delete')}</ListItemText>
          </MenuItem>
        )}
      </Menu>
    </Card>
  );
}
