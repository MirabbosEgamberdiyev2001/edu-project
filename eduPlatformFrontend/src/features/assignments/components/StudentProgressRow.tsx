import { Box, LinearProgress, Typography, Chip } from '@mui/material';
import WarningIcon from '@mui/icons-material/Warning';
import type { LiveStudentDto } from '@/types/assignment';

interface StudentProgressRowProps {
  student: LiveStudentDto;
}

export default function StudentProgressRow({ student }: StudentProgressRowProps) {
  const progress = student.totalQuestions > 0
    ? (student.answeredQuestions / student.totalQuestions) * 100
    : 0;

  const timeFormatted = student.timeRemaining != null
    ? `${Math.floor(student.timeRemaining / 60)}:${String(student.timeRemaining % 60).padStart(2, '0')}`
    : null;

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 1 }}>
      <Typography variant="body2" sx={{ minWidth: 140 }}>
        {student.firstName} {student.lastName}
      </Typography>
      <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
        <LinearProgress
          variant="determinate"
          value={progress}
          sx={{ flex: 1, height: 8, borderRadius: 4 }}
        />
        <Typography variant="caption" sx={{ minWidth: 40 }}>
          {student.answeredQuestions}/{student.totalQuestions}
        </Typography>
      </Box>
      {student.tabSwitches > 0 && (
        <Chip icon={<WarningIcon />} label={student.tabSwitches} size="small" color="warning" />
      )}
      {timeFormatted && (
        <Typography
          variant="caption"
          sx={{ minWidth: 50, color: student.timeRemaining! < 300 ? 'error.main' : 'text.secondary' }}
        >
          {timeFormatted}
        </Typography>
      )}
      <Chip label={student.status} size="small" variant="outlined" />
    </Box>
  );
}
