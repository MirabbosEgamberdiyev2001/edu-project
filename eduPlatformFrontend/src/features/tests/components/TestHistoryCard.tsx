import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  Card, CardContent, CardActions, Typography, Chip, Box, IconButton, Tooltip,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DeleteIcon from '@mui/icons-material/Delete';
import type { TestHistoryDto } from '@/types/test';

interface TestHistoryCardProps {
  test: TestHistoryDto;
  onDuplicate: (id: string) => void;
  onDelete: (test: TestHistoryDto) => void;
}

const STATUS_COLORS: Record<string, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  CREATED: 'info',
  GENERATING: 'warning',
  READY: 'success',
  DOWNLOADED: 'success',
  DELETED: 'error',
};

export default function TestHistoryCard({ test, onDuplicate, onDelete }: TestHistoryCardProps) {
  const { t } = useTranslation('test');
  const navigate = useNavigate();

  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ flexGrow: 1 }}>
        <Typography variant="h6" noWrap gutterBottom>
          {test.title}
        </Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          {test.subjectName}
        </Typography>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mt: 1 }}>
          <Chip size="small" label={t('card.questions', { count: test.questionCount })} />
          <Chip size="small" label={t('card.variants', { count: test.variantCount })} />
          <Chip
            size="small"
            color={STATUS_COLORS[test.status] || 'default'}
            label={t(`status.${test.status}`)}
          />
        </Box>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
          {t('card.created')}: {new Date(test.createdAt).toLocaleDateString()}
        </Typography>
      </CardContent>
      <CardActions sx={{ justifyContent: 'flex-end' }}>
        <Tooltip title={t('detail.title')}>
          <IconButton size="small" onClick={() => navigate(`/tests/${test.id}`)}>
            <VisibilityIcon fontSize="small" />
          </IconButton>
        </Tooltip>
        <Tooltip title={t('duplicate')}>
          <IconButton size="small" onClick={() => onDuplicate(test.id)}>
            <ContentCopyIcon fontSize="small" />
          </IconButton>
        </Tooltip>
        <Tooltip title={t('delete.title')}>
          <IconButton size="small" color="error" onClick={() => onDelete(test)}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </CardActions>
    </Card>
  );
}
