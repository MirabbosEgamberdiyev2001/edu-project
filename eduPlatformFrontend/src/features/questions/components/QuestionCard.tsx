import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Card,
  CardContent,
  Typography,
  Box,
  IconButton,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Chip,
  Stack,
  CardActionArea,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import SendIcon from '@mui/icons-material/Send';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { useTranslation } from 'react-i18next';
import type { QuestionDto } from '@/types/question';
import { QuestionStatus } from '@/types/question';
import { resolveTranslation } from '@/utils/i18nUtils';

interface QuestionCardProps {
  question: QuestionDto;
  onEdit: (question: QuestionDto) => void;
  onDelete: (question: QuestionDto) => void;
  onSubmit: (question: QuestionDto) => void;
}

const DIFFICULTY_COLORS: Record<string, 'success' | 'warning' | 'error'> = {
  EASY: 'success',
  MEDIUM: 'warning',
  HARD: 'error',
};

const STATUS_COLORS: Record<string, 'default' | 'primary' | 'success' | 'error' | 'warning' | 'info'> = {
  DRAFT: 'default',
  PENDING: 'info',
  APPROVED: 'primary',
  ACTIVE: 'success',
  REJECTED: 'error',
  ARCHIVED: 'warning',
};

export default function QuestionCard({ question, onEdit, onDelete, onSubmit }: QuestionCardProps) {
  const { t } = useTranslation('question');
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const displayText = resolveTranslation(question.questionTextTranslations) || question.questionText;
  const canSubmit = question.status === QuestionStatus.DRAFT || question.status === QuestionStatus.REJECTED;

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        transition: 'box-shadow 0.2s',
        '&:hover': { boxShadow: 4 },
      }}
    >
      <CardActionArea onClick={() => navigate(`/questions/${question.id}`)} sx={{ flex: 1 }}>
        <CardContent sx={{ pb: 1 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
            <Typography
              variant="body1"
              fontWeight={500}
              sx={{
                flex: 1,
                mr: 1,
                display: '-webkit-box',
                WebkitLineClamp: 3,
                WebkitBoxOrient: 'vertical',
                overflow: 'hidden',
              }}
            >
              {displayText}
            </Typography>
          </Box>

          <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap sx={{ mb: 1.5 }}>
            <Chip label={t(`types.${question.questionType}`)} size="small" variant="outlined" />
            <Chip
              label={t(`difficulties.${question.difficulty}`)}
              size="small"
              color={DIFFICULTY_COLORS[question.difficulty] || 'default'}
            />
            <Chip
              label={t(`statuses.${question.status}`)}
              size="small"
              color={STATUS_COLORS[question.status] || 'default'}
              variant="outlined"
            />
          </Stack>

          <Typography variant="caption" color="text.secondary" display="block">
            {resolveTranslation(question.subjectNameTranslations) || question.subjectName} &bull; {resolveTranslation(question.topicNameTranslations) || question.topicName}
          </Typography>

          {/* Rejection reason */}
          {question.status === QuestionStatus.REJECTED && question.rejectionReason && (
            <Typography
              variant="caption"
              color="error"
              display="block"
              sx={{
                mt: 0.5,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {question.rejectionReason}
            </Typography>
          )}

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 1 }}>
            <Typography variant="caption" color="text.secondary">
              {question.points} {t('form.points').toLowerCase()}
            </Typography>
            {question.timeLimitSeconds && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.3 }}>
                <AccessTimeIcon sx={{ fontSize: 14, color: 'text.disabled' }} />
                <Typography variant="caption" color="text.secondary">
                  {question.timeLimitSeconds}s
                </Typography>
              </Box>
            )}
            {question.userName && (
              <Typography variant="caption" color="text.disabled">
                {question.userName}
              </Typography>
            )}
          </Box>
        </CardContent>
      </CardActionArea>

      {/* Menu button positioned absolutely */}
      <IconButton
        size="small"
        onClick={(e) => { e.stopPropagation(); setAnchorEl(e.currentTarget); }}
        sx={{ position: 'absolute', top: 8, right: 8, zIndex: 1 }}
      >
        <MoreVertIcon fontSize="small" />
      </IconButton>

      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
        <MenuItem onClick={() => { setAnchorEl(null); navigate(`/questions/${question.id}`); }}>
          <ListItemIcon><VisibilityIcon fontSize="small" /></ListItemIcon>
          <ListItemText>{t('detail.view')}</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => { setAnchorEl(null); onEdit(question); }}>
          <ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>
          <ListItemText>{t('edit')}</ListItemText>
        </MenuItem>
        {canSubmit && (
          <MenuItem onClick={() => { setAnchorEl(null); onSubmit(question); }}>
            <ListItemIcon><SendIcon fontSize="small" /></ListItemIcon>
            <ListItemText>{t('submit')}</ListItemText>
          </MenuItem>
        )}
        <MenuItem onClick={() => { setAnchorEl(null); onDelete(question); }} sx={{ color: 'error.main' }}>
          <ListItemIcon><DeleteIcon fontSize="small" color="error" /></ListItemIcon>
          <ListItemText>{t('delete')}</ListItemText>
        </MenuItem>
      </Menu>
    </Card>
  );
}
