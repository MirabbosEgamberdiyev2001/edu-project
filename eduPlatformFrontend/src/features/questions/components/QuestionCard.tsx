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
  Checkbox,
  Divider,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import SendIcon from '@mui/icons-material/Send';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { useTranslation } from 'react-i18next';
import type { QuestionDto } from '@/types/question';
import { QuestionStatus } from '@/types/question';
import { resolveTranslation } from '@/utils/i18nUtils';
import { MathText } from '@/components/math';

interface QuestionCardProps {
  question: QuestionDto;
  selected?: boolean;
  selectable?: boolean;
  onSelect?: (id: string) => void;
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

export default function QuestionCard({
  question,
  selected,
  selectable,
  onSelect,
  onEdit,
  onDelete,
  onSubmit,
}: QuestionCardProps) {
  const { t } = useTranslation('question');
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const displayText = resolveTranslation(question.questionTextTranslations) || question.questionText;
  const canSubmit = question.status === QuestionStatus.DRAFT || question.status === QuestionStatus.REJECTED;

  return (
    <Card
      onClick={() => navigate(`/questions/${question.id}`)}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        cursor: 'pointer',
        borderRadius: 2,
        borderLeft: 4,
        borderColor: selected ? 'primary.main' : 'grey.300',
        bgcolor: selected ? 'primary.50' : 'background.paper',
        transition: 'all 0.15s ease-in-out',
        '&:hover': {
          boxShadow: 4,
          transform: 'translateY(-1px)',
        },
      }}
    >
      <CardContent sx={{ p: 2, '&:last-child': { pb: 1.5 }, flex: 1, display: 'flex', flexDirection: 'column' }}>
        {/* Header: Checkbox + Chips + Menu — inline, no overlap */}
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1.5 }}>
          {selectable && (
            <Checkbox
              checked={!!selected}
              onChange={() => onSelect?.(question.id)}
              size="small"
              onClick={(e) => e.stopPropagation()}
              sx={{ p: 0.5, mr: 0.5, ml: -0.75 }}
            />
          )}
          <Stack direction="row" spacing={0.5} useFlexGap sx={{ flex: 1, flexWrap: 'wrap' }}>
            <Chip
              label={t(`statuses.${question.status}`)}
              size="small"
              color={STATUS_COLORS[question.status] || 'default'}
              sx={{ height: 24, fontSize: '0.75rem', fontWeight: 500 }}
            />
            <Chip
              label={t(`difficulties.${question.difficulty}`)}
              size="small"
              color={DIFFICULTY_COLORS[question.difficulty] || 'default'}
              variant="outlined"
              sx={{ height: 24, fontSize: '0.75rem' }}
            />
            <Chip
              label={t(`types.${question.questionType}`)}
              size="small"
              variant="outlined"
              sx={{ height: 24, fontSize: '0.75rem' }}
            />
          </Stack>
          <IconButton
            size="small"
            onClick={(e) => { e.stopPropagation(); setAnchorEl(e.currentTarget); }}
            sx={{ ml: 0.5, mr: -0.75 }}
          >
            <MoreVertIcon fontSize="small" />
          </IconButton>
        </Box>

        {/* Question text — MathText renders LaTeX; maxHeight limits card preview height */}
        <MathText
          text={displayText}
          variant="body1"
          sx={{
            mb: 1.5,
            fontWeight: 600,
            maxHeight: '3.4em',
            overflow: 'hidden',
            lineHeight: 1.6,
          }}
        />

        {/* Subject · Topic */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          {resolveTranslation(question.subjectNameTranslations) || question.subjectName}
          {' · '}
          {resolveTranslation(question.topicNameTranslations) || question.topicName}
        </Typography>

        {/* Rejection reason */}
        {question.status === QuestionStatus.REJECTED && question.rejectionReason && (
          <Typography
            variant="caption"
            color="error.main"
            display="block"
            sx={{
              mb: 1,
              py: 0.5,
              px: 1,
              bgcolor: 'error.50',
              borderRadius: 1,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            {question.rejectionReason}
          </Typography>
        )}

        {/* Spacer to push footer down */}
        <Box sx={{ flex: 1 }} />

        {/* Footer */}
        <Divider sx={{ mb: 1 }} />
        <Stack direction="row" alignItems="center" spacing={1}>
          <Typography variant="caption" color="text.secondary" fontWeight={500}>
            {question.points} {t('form.points').toLowerCase()}
          </Typography>
          {question.timeLimitSeconds && (
            <Typography variant="caption" color="text.secondary">
              · {question.timeLimitSeconds}s
            </Typography>
          )}
          {question.userName && (
            <Typography variant="caption" color="text.disabled" sx={{ ml: 'auto !important' }}>
              {question.userName}
            </Typography>
          )}
        </Stack>
      </CardContent>

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
