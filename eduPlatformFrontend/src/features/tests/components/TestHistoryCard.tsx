import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  Card, CardContent, CardActions, Typography, Chip, Box, IconButton, Tooltip, CardActionArea,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import DeleteIcon from '@mui/icons-material/Delete';
import PublicIcon from '@mui/icons-material/Public';
import type { TestHistoryDto } from '@/types/test';
import { resolveTranslation } from '@/utils/i18nUtils';

interface TestHistoryCardProps {
  test: TestHistoryDto;
  onDelete: (test: TestHistoryDto) => void;
}

const STATUS_COLORS: Record<string, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  CREATED: 'info',
  GENERATING: 'warning',
  READY: 'success',
  DOWNLOADED: 'success',
  DELETED: 'error',
};

const STATUS_BORDER: Record<string, string> = {
  CREATED: '#2196f3',
  GENERATING: '#ff9800',
  READY: '#4caf50',
  DOWNLOADED: '#4caf50',
  DELETED: '#f44336',
};

export default function TestHistoryCard({ test, onDelete }: TestHistoryCardProps) {
  const { t } = useTranslation('test');
  const navigate = useNavigate();
  const borderColor = STATUS_BORDER[test.status] || '#9e9e9e';

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        borderTop: `4px solid ${borderColor}`,
        transition: 'transform 0.2s, box-shadow 0.2s',
        '&:hover': {
          transform: 'translateY(-2px)',
          boxShadow: 8,
        },
      }}
    >
      <CardActionArea onClick={() => navigate(`/tests/${test.id}`)} sx={{ flexGrow: 1 }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
            <Typography variant="h6" noWrap sx={{ flex: 1, fontWeight: 600 }}>
              {resolveTranslation(test.titleTranslations) || test.title}
            </Typography>
            {test.isPublic && (
              <Chip
                icon={<PublicIcon sx={{ fontSize: 14 }} />}
                label={t('publish.publicBadge')}
                size="small"
                color="success"
                variant="outlined"
              />
            )}
          </Box>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            {test.subjectName}
          </Typography>
          <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 1 }}>
            <Chip size="small" label={t('card.questions', { count: test.questionCount })} variant="outlined" />
            <Chip size="small" label={t('card.variants', { count: test.variantCount })} variant="outlined" />
            <Chip
              size="small"
              color={STATUS_COLORS[test.status] || 'default'}
              label={t(`status.${test.status}`)}
            />
            {test.category && (
              <Chip size="small" label={t(`categories.${test.category}`)} variant="outlined" color="secondary" />
            )}
          </Box>
          <Typography variant="caption" color="text.secondary" sx={{ mt: 1.5, display: 'block' }}>
            {t('card.created')}: {new Date(test.createdAt).toLocaleDateString()}
          </Typography>
        </CardContent>
      </CardActionArea>
      <CardActions sx={{ justifyContent: 'flex-end' }}>
        <Tooltip title={t('detail.title')}>
          <IconButton size="small" onClick={() => navigate(`/tests/${test.id}`)}>
            <VisibilityIcon fontSize="small" />
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
