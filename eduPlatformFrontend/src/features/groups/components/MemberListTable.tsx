import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
  Typography,
  Box,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import PersonIcon from '@mui/icons-material/Person';
import { useTranslation } from 'react-i18next';
import type { GroupMemberDto } from '@/types/group';

interface MemberListTableProps {
  members: GroupMemberDto[];
  onRemove: (studentId: string) => void;
  isRemoving: boolean;
}

export default function MemberListTable({ members, onRemove, isRemoving }: MemberListTableProps) {
  const { t } = useTranslation('group');

  if (members.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 6 }}>
        <PersonIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
        <Typography variant="body1" color="text.secondary">
          {t('noStudentsFound')}
        </Typography>
      </Box>
    );
  }

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>#</TableCell>
            <TableCell>{t('common:name')}</TableCell>
            <TableCell>{t('common:email')}</TableCell>
            <TableCell>{t('common:phone')}</TableCell>
            <TableCell>{t('createdAt')}</TableCell>
            <TableCell align="right" />
          </TableRow>
        </TableHead>
        <TableBody>
          {members.map((member, index) => (
            <TableRow key={member.id} hover>
              <TableCell>{index + 1}</TableCell>
              <TableCell>
                <Typography variant="body2" fontWeight={500}>
                  {member.firstName} {member.lastName}
                </Typography>
              </TableCell>
              <TableCell>
                <Typography variant="body2" color="text.secondary">
                  {member.email || '---'}
                </Typography>
              </TableCell>
              <TableCell>
                <Typography variant="body2" color="text.secondary">
                  {member.phone || '---'}
                </Typography>
              </TableCell>
              <TableCell>
                <Typography variant="body2" color="text.secondary">
                  {new Date(member.joinedAt).toLocaleDateString()}
                </Typography>
              </TableCell>
              <TableCell align="right">
                <Tooltip title={t('removeMember')}>
                  <IconButton
                    size="small"
                    color="error"
                    onClick={() => onRemove(member.studentId)}
                    disabled={isRemoving}
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
