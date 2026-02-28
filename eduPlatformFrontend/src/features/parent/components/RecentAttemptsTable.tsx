import { Box, Typography, Chip, Stack } from '@mui/material';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import { useTranslation } from 'react-i18next';
import type { RecentAttemptDto } from '@/types/parent';

interface RecentAttemptsTableProps {
  attempts: RecentAttemptDto[];
}

function getScoreColor(pct: number | null): 'success' | 'warning' | 'error' | 'default' {
  if (pct === null) return 'default';
  if (pct >= 70) return 'success';
  if (pct >= 40) return 'warning';
  return 'error';
}

export default function RecentAttemptsTable({ attempts }: RecentAttemptsTableProps) {
  const { t } = useTranslation('parent');

  if (attempts.length === 0) {
    return (
      <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
        {t('noAttempts')}
      </Typography>
    );
  }

  return (
    <Stack spacing={0} divider={<Box sx={{ borderBottom: '1px solid', borderColor: 'divider' }} />}>
      {attempts.map((attempt) => {
        const submitted = attempt.status === 'SUBMITTED' || attempt.status === 'GRADED';
        const pct = attempt.percentage != null ? Math.round(Number(attempt.percentage)) : null;

        return (
          <Box key={attempt.attemptId} sx={{ py: 1.5 }}>
            {/* Title row */}
            <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, mb: 0.75 }}>
              {submitted ? (
                <CheckCircleOutlineIcon sx={{ fontSize: 16, color: 'success.main', mt: 0.25, flexShrink: 0 }} />
              ) : (
                <RadioButtonUncheckedIcon sx={{ fontSize: 16, color: 'text.disabled', mt: 0.25, flexShrink: 0 }} />
              )}
              <Typography variant="body2" fontWeight={600} sx={{ flex: 1, lineHeight: 1.3 }}>
                {attempt.assignmentTitle}
              </Typography>
            </Box>

            {/* Score + date row */}
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, pl: 3 }}>
              {pct !== null ? (
                <Chip
                  label={`${pct}%`}
                  size="small"
                  color={getScoreColor(pct)}
                  variant="outlined"
                  sx={{ fontSize: '0.7rem', height: 20 }}
                />
              ) : (
                <Chip
                  label={attempt.status}
                  size="small"
                  variant="outlined"
                  sx={{ fontSize: '0.7rem', height: 20 }}
                />
              )}
              {attempt.submittedAt && (
                <Typography variant="caption" color="text.disabled">
                  {new Date(attempt.submittedAt).toLocaleDateString()}
                </Typography>
              )}
            </Box>
          </Box>
        );
      })}
    </Stack>
  );
}
