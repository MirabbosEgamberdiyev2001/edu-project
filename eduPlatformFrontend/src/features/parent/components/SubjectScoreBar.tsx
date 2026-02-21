import {
  Box,
  Typography,
  LinearProgress,
} from '@mui/material';
import type { SubjectScoreDto } from '@/types/parent';

interface SubjectScoreBarProps {
  subject: SubjectScoreDto;
}

export default function SubjectScoreBar({ subject }: SubjectScoreBarProps) {
  const percentage = Math.round(subject.averageScore);

  const getColor = (value: number): 'success' | 'warning' | 'error' => {
    if (value >= 70) return 'success';
    if (value >= 40) return 'warning';
    return 'error';
  };

  return (
    <Box sx={{ mb: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
        <Typography variant="body2" fontWeight={500}>
          {subject.subjectName}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {percentage}%
        </Typography>
      </Box>
      <LinearProgress
        variant="determinate"
        value={percentage}
        color={getColor(percentage)}
        sx={{ height: 8, borderRadius: 4 }}
      />
      <Typography variant="caption" color="text.secondary">
        {subject.totalAttempts} attempt{subject.totalAttempts !== 1 ? 's' : ''}
      </Typography>
    </Box>
  );
}
