import { Paper, Typography, Box, Button, Chip } from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import TimerIcon from '@mui/icons-material/Timer';
import { useTranslation } from 'react-i18next';

interface AvailableAssignment {
  id: string;
  title: string;
  testTitle: string;
  groupName: string;
  durationMinutes: number | null;
  maxAttempts: number;
  endDate: string | null;
  remainingAttempts?: number;
}

interface AssignmentListCardProps {
  assignment: AvailableAssignment;
  onStart: (assignmentId: string) => void;
  isPending: boolean;
}

export default function AssignmentListCard({ assignment, onStart, isPending }: AssignmentListCardProps) {
  const { t } = useTranslation('testTaking');

  return (
    <Paper sx={{ p: 2.5, display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Typography variant="subtitle1" fontWeight={600} noWrap>
        {assignment.title}
      </Typography>
      <Typography variant="body2" color="text.secondary" noWrap sx={{ mb: 1 }}>
        {assignment.groupName}
      </Typography>

      <Box sx={{ display: 'flex', gap: 1, mb: 1.5, flexWrap: 'wrap' }}>
        {assignment.durationMinutes && (
          <Chip icon={<TimerIcon />} label={`${assignment.durationMinutes} ${t('minutesShort')}`} size="small" variant="outlined" />
        )}
        <Chip label={`${t('maxAttempts')}: ${assignment.maxAttempts}`} size="small" variant="outlined" />
      </Box>

      {assignment.endDate && (
        <Typography variant="caption" color="text.secondary" sx={{ mb: 1 }}>
          {t('deadline')}: {new Date(assignment.endDate).toLocaleString()}
        </Typography>
      )}

      <Box sx={{ flex: 1 }} />
      <Button
        variant="contained"
        startIcon={<PlayArrowIcon />}
        onClick={() => onStart(assignment.id)}
        disabled={isPending}
        fullWidth
        sx={{ mt: 1.5 }}
      >
        {t('startTest')}
      </Button>
    </Paper>
  );
}
