import {
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Chip,
  Typography,
  Button,
} from '@mui/material';
import GradingIcon from '@mui/icons-material/Grading';
import { useTranslation } from 'react-i18next';
import type { StudentResultDto } from '@/types/assignment';

interface ResultsTableProps {
  students: StudentResultDto[];
  onGradeAttempt?: (attemptId: string, studentName: string) => void;
}

const statusColor = (status: string): 'default' | 'success' | 'error' | 'warning' | 'info' => {
  switch (status) {
    case 'AUTO_GRADED':
    case 'GRADED': return 'success';
    case 'NEEDS_REVIEW': return 'warning';
    case 'SUBMITTED': return 'info';
    case 'IN_PROGRESS': return 'default';
    case 'EXPIRED': return 'error';
    default: return 'default';
  }
};

export default function ResultsTable({ students, onGradeAttempt }: ResultsTableProps) {
  const { t } = useTranslation('assignment');

  const hasGradingColumn = students.some(
    (s) => s.status === 'NEEDS_REVIEW' && s.latestAttemptId,
  );

  return (
    <Table size="small">
      <TableHead>
        <TableRow>
          <TableCell>#</TableCell>
          <TableCell>{t('student')}</TableCell>
          <TableCell align="center">{t('score')}</TableCell>
          <TableCell align="center">{t('percentage')}</TableCell>
          <TableCell align="center">{t('attempts')}</TableCell>
          <TableCell align="center">{t('tabSwitches')}</TableCell>
          <TableCell>{t('submittedAt')}</TableCell>
          <TableCell>{t('status')}</TableCell>
          {hasGradingColumn && <TableCell align="center">{t('grading')}</TableCell>}
        </TableRow>
      </TableHead>
      <TableBody>
        {students.map((student, index) => (
          <TableRow key={student.studentId}>
            <TableCell>{index + 1}</TableCell>
            <TableCell>
              <Typography variant="body2" fontWeight={500}>
                {student.firstName} {student.lastName}
              </Typography>
            </TableCell>
            <TableCell align="center">
              {student.score != null ? `${student.score}/${student.maxScore}` : '-'}
            </TableCell>
            <TableCell align="center">
              {student.percentage != null ? (
                <Chip
                  label={`${Math.round(student.percentage)}%`}
                  size="small"
                  color={student.percentage >= 70 ? 'success' : student.percentage >= 40 ? 'warning' : 'error'}
                />
              ) : '-'}
            </TableCell>
            <TableCell align="center">{student.attemptCount}</TableCell>
            <TableCell align="center">
              {student.tabSwitches > 0 ? (
                <Chip label={student.tabSwitches} size="small" color="warning" />
              ) : '0'}
            </TableCell>
            <TableCell>
              {student.submittedAt ? new Date(student.submittedAt).toLocaleString() : '-'}
            </TableCell>
            <TableCell>
              <Chip
                label={t(`attemptStatus.${student.status}`, { defaultValue: student.status })}
                size="small"
                color={statusColor(student.status)}
                variant={student.status === 'NEEDS_REVIEW' ? 'filled' : 'outlined'}
              />
            </TableCell>
            {hasGradingColumn && (
              <TableCell align="center">
                {student.status === 'NEEDS_REVIEW' && student.latestAttemptId && onGradeAttempt ? (
                  <Button
                    size="small"
                    variant="outlined"
                    color="warning"
                    startIcon={<GradingIcon />}
                    onClick={() =>
                      onGradeAttempt(
                        student.latestAttemptId!,
                        `${student.firstName} ${student.lastName}`,
                      )
                    }
                    sx={{ whiteSpace: 'nowrap', textTransform: 'none' }}
                  >
                    {t('grade')}
                  </Button>
                ) : null}
              </TableCell>
            )}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
