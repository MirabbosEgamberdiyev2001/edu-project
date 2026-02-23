import { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  IconButton,
  Tooltip,
  Alert,
  CircularProgress,
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { useTranslation } from 'react-i18next';
import type { TestHistoryDto } from '@/types/test';

interface PublishTestDialogProps {
  open: boolean;
  onClose: () => void;
  test: TestHistoryDto;
  onPublish: (durationMinutes?: number) => void;
  onUnpublish: () => void;
  isPending: boolean;
}

export default function PublishTestDialog({ open, onClose, test, onPublish, onUnpublish, isPending }: PublishTestDialogProps) {
  const { t } = useTranslation('test');
  const [duration, setDuration] = useState<number>(test.publicDurationMinutes || 45);
  const [copied, setCopied] = useState(false);

  const publicUrl = test.publicSlug
    ? `${window.location.origin}/test/${test.publicSlug}`
    : null;

  const handleCopy = async () => {
    if (publicUrl) {
      await navigator.clipboard.writeText(publicUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{t('publish.title')}</DialogTitle>
      <DialogContent>
        {test.isPublic && publicUrl ? (
          <Box>
            <Alert severity="success" sx={{ mb: 2 }}>
              {t('publish.published')}
            </Alert>
            <Typography variant="subtitle2" gutterBottom>
              {t('publish.shareLink')}
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
              <TextField
                fullWidth
                size="small"
                value={publicUrl}
                InputProps={{ readOnly: true }}
              />
              <Tooltip title={copied ? t('publish.linkCopied') : t('publish.copyLink')}>
                <IconButton onClick={handleCopy} color={copied ? 'success' : 'default'}>
                  <ContentCopyIcon />
                </IconButton>
              </Tooltip>
            </Box>
          </Box>
        ) : (
          <Box>
            <TextField
              label={t('publish.duration')}
              type="number"
              fullWidth
              size="small"
              value={duration}
              onChange={(e) => setDuration(Number(e.target.value))}
              inputProps={{ min: 1, max: 600 }}
              sx={{ mt: 1 }}
            />
          </Box>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} disabled={isPending}>
          {t('common:cancel')}
        </Button>
        {test.isPublic ? (
          <Button
            variant="outlined"
            color="error"
            onClick={onUnpublish}
            disabled={isPending}
          >
            {isPending ? <CircularProgress size={20} /> : t('publish.unpublish')}
          </Button>
        ) : (
          <Button
            variant="contained"
            onClick={() => onPublish(duration)}
            disabled={isPending}
          >
            {isPending ? <CircularProgress size={20} /> : t('publish.publish')}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
