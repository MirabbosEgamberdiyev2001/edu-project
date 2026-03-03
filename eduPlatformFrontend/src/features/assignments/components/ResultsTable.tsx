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

export default function ResultsTable({ students, onGradeAttempt }: ResultsTableProps) {
  const { t } = useTranslation('assignment');

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
              <Chip label={student.status} size="small" variant="outlined" />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
