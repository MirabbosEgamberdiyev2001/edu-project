import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  LinearProgress,
  Divider,
} from '@mui/material';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { useTranslation } from 'react-i18next';

interface SubmitConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  answeredCount: number;
  totalQuestions: number;
  isPending: boolean;
}

export default function SubmitConfirmDialog({
  open,
  onClose,
  onConfirm,
  answeredCount,
  totalQuestions,
  isPending,
}: SubmitConfirmDialogProps) {
  const { t } = useTranslation('testTaking');
  const unanswered = totalQuestions - answeredCount;
  const allAnswered = unanswered === 0;
  const progress = totalQuestions > 0 ? (answeredCount / totalQuestions) * 100 : 0;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="xs"
      fullWidth
      aria-labelledby="submit-confirm-title"
    >
      <DialogTitle id="submit-confirm-title" sx={{ pb: 1 }}>
        {t('submitConfirm.title')}
      </DialogTitle>

      <DialogContent sx={{ pt: 0 }}>
        {/* Progress bar */}
        <LinearProgress
          variant="determinate"
          value={progress}
          sx={{
            mb: 2,
            height: 6,
            borderRadius: 3,
            bgcolor: 'grey.100',
            '& .MuiLinearProgress-bar': {
              bgcolor: allAnswered ? 'success.main' : 'primary.main',
              borderRadius: 3,
            },
          }}
        />

        {/* Answered row */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1 }}>
          <CheckCircleOutlineIcon
            sx={{ color: allAnswered ? 'success.main' : 'primary.main', flexShrink: 0 }}
          />
          <Box sx={{ flex: 1 }}>
            <Typography variant="body2" fontWeight={600}>
              {t('submitConfirm.answered')}
            </Typography>
          </Box>
          <Typography variant="body2" fontWeight={700} color={allAnswered ? 'success.main' : 'text.primary'}>
            {answeredCount} / {totalQuestions}
          </Typography>
        </Box>

        {/* Unanswered row — only shown if there are gaps */}
        {unanswered > 0 && (
          <>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 0.5 }}>
              <RadioButtonUncheckedIcon sx={{ color: 'text.disabled', flexShrink: 0 }} />
              <Typography variant="body2" color="text.secondary" sx={{ flex: 1 }}>
                {t('submitConfirm.unanswered', { count: unanswered })}
              </Typography>
              <Typography variant="body2" fontWeight={700} color="text.secondary">
                {unanswered}
              </Typography>
            </Box>

            <Divider sx={{ my: 1.5 }} />

            {/* Warning notice */}
            <Box
              sx={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: 1,
                px: 1.5,
                py: 1,
                bgcolor: 'warning.50',
                border: '1px solid',
                borderColor: 'warning.light',
                borderRadius: 1.5,
              }}
            >
              <WarningAmberIcon sx={{ color: 'warning.main', fontSize: 18, mt: 0.1, flexShrink: 0 }} />
              <Typography variant="caption" color="warning.dark">
                {t('exam.submitWarning', { count: unanswered })}
              </Typography>
            </Box>
          </>
        )}
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 1.5, gap: 1 }}>
        <Button onClick={onClose} disabled={isPending} color="inherit">
          {t('submitConfirm.cancel')}
        </Button>
        <Button
          onClick={onConfirm}
          variant="contained"
          disabled={isPending}
          color={allAnswered ? 'success' : 'primary'}
          sx={{ minWidth: 120 }}
        >
          {t('submitConfirm.submit')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
