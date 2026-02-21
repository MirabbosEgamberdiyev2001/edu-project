import { Table, TableHead, TableRow, TableCell, TableBody, Chip, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { TopStudentDto } from '@/types/analytics';

interface TopStudentsTableProps {
  students: TopStudentDto[];
}

export default function TopStudentsTable({ students }: TopStudentsTableProps) {
  const { t } = useTranslation('analytics');

  return (
    <Table size="small">
      <TableHead>
        <TableRow>
          <TableCell>#</TableCell>
          <TableCell>{t('student')}</TableCell>
          <TableCell align="center">{t('avgScore')}</TableCell>
          <TableCell align="center">{t('attempts')}</TableCell>
          <TableCell align="center">{t('completionRate')}</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {students.map((s, i) => (
          <TableRow key={s.studentId}>
            <TableCell>{i + 1}</TableCell>
            <TableCell>
              <Typography variant="body2" fontWeight={500}>{s.firstName} {s.lastName}</Typography>
            </TableCell>
            <TableCell align="center">
              <Chip
                label={`${Math.round(s.averageScore)}%`}
                size="small"
                color={s.averageScore >= 70 ? 'success' : 'warning'}
              />
            </TableCell>
            <TableCell align="center">{s.totalAttempts}</TableCell>
            <TableCell align="center">{Math.round(s.completionRate)}%</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
