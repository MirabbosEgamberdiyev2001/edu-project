import { Table, TableHead, TableRow, TableCell, TableBody, Chip, Typography } from '@mui/material';
import WarningIcon from '@mui/icons-material/Warning';
import { useTranslation } from 'react-i18next';
import type { AtRiskStudentDto } from '@/types/analytics';

interface AtRiskStudentsTableProps {
  students: AtRiskStudentDto[];
}

export default function AtRiskStudentsTable({ students }: AtRiskStudentsTableProps) {
  const { t } = useTranslation('analytics');

  return (
    <Table size="small">
      <TableHead>
        <TableRow>
          <TableCell>{t('student')}</TableCell>
          <TableCell align="center">{t('avgScore')}</TableCell>
          <TableCell align="center">{t('missedAssignments')}</TableCell>
          <TableCell>{t('lastActivity')}</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {students.map((s) => (
          <TableRow key={s.studentId}>
            <TableCell>
              <Typography variant="body2" fontWeight={500}>{s.firstName} {s.lastName}</Typography>
            </TableCell>
            <TableCell align="center">
              <Chip label={`${Math.round(s.averageScore)}%`} size="small" color="error" />
            </TableCell>
            <TableCell align="center">
              {s.missedAssignments > 0 && (
                <Chip icon={<WarningIcon />} label={s.missedAssignments} size="small" color="warning" />
              )}
            </TableCell>
            <TableCell>
              {s.lastActivityAt ? new Date(s.lastActivityAt).toLocaleDateString() : '-'}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
