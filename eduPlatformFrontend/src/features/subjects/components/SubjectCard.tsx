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
  Avatar,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ArchiveIcon from '@mui/icons-material/Archive';
import UnarchiveIcon from '@mui/icons-material/Unarchive';
import VisibilityIcon from '@mui/icons-material/Visibility';
import TopicIcon from '@mui/icons-material/Topic';
import QuizIcon from '@mui/icons-material/Quiz';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import type { SubjectDto } from '@/types/subject';
import { resolveTranslation } from '@/utils/i18nUtils';

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

  // Resolve name/description from translations map (frontend-side i18n)
  const displayName = resolveTranslation(subject.nameTranslations) || subject.name;
  const displayDescription = resolveTranslation(subject.descriptionTranslations) || subject.description;

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        borderTop: 3,
        borderColor: subject.color || 'primary.main',
        opacity: subject.isArchived ? 0.7 : 1,
        transition: 'box-shadow 0.2s',
        '&:hover': { boxShadow: 4 },
      }}
    >
      <CardActionArea onClick={() => navigate(`/subjects/${subject.id}`)}>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, minWidth: 0, flex: 1 }}>
              {subject.icon ? (
                isImageUrl(subject.icon) ? (
                  <Avatar
                    src={subject.icon}
                    variant="rounded"
                    sx={{ width: 40, height: 40, bgcolor: subject.color || 'primary.main' }}
                  >
                    <MenuBookIcon />
                  </Avatar>
                ) : (
                  <Typography variant="h5" component="span" sx={{ lineHeight: 1 }}>
                    {subject.icon}
                  </Typography>
                )
              ) : (
                <Avatar
                  variant="rounded"
                  sx={{ width: 40, height: 40, bgcolor: subject.color || 'primary.main' }}
                >
                  <MenuBookIcon sx={{ fontSize: 24 }} />
                </Avatar>
              )}
              <Typography variant="h6" fontWeight={600} noWrap sx={{ flex: 1 }}>
                {displayName}
              </Typography>
            </Box>
          </Box>

          {displayDescription && (
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
              {displayDescription}
            </Typography>
          )}

          {subject.category && (
            <Chip label={t(`categories.${subject.category}`)} size="small" sx={{ mb: 1.5 }} />
          )}

          <Box sx={{ display: 'flex', gap: 2, mt: 'auto' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <TopicIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                {subject.topicCount}
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <QuizIcon fontSize="small" color="action" />
              <Typography variant="body2" color="text.secondary">
                {subject.questionCount}
              </Typography>
            </Box>
          </Box>
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
        <MenuItem onClick={() => { setAnchorEl(null); navigate(`/subjects/${subject.id}`); }}>
          <ListItemIcon><VisibilityIcon fontSize="small" /></ListItemIcon>
          <ListItemText>{t('view')}</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => { setAnchorEl(null); onEdit(subject); }}>
          <ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>
          <ListItemText>{t('edit')}</ListItemText>
        </MenuItem>
        {subject.isArchived ? (
          <MenuItem onClick={() => { setAnchorEl(null); onRestore(subject); }}>
            <ListItemIcon><UnarchiveIcon fontSize="small" /></ListItemIcon>
            <ListItemText>{t('restore')}</ListItemText>
          </MenuItem>
        ) : (
          <MenuItem onClick={() => { setAnchorEl(null); onArchive(subject); }}>
            <ListItemIcon><ArchiveIcon fontSize="small" /></ListItemIcon>
            <ListItemText>{t('archive')}</ListItemText>
          </MenuItem>
        )}
        <MenuItem onClick={() => { setAnchorEl(null); onDelete(subject); }} sx={{ color: 'error.main' }}>
          <ListItemIcon><DeleteIcon fontSize="small" color="error" /></ListItemIcon>
          <ListItemText>{t('delete')}</ListItemText>
        </MenuItem>
      </Menu>
    </Card>
  );
}
