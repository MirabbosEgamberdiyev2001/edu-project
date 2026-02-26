import { useState } from 'react';
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
  Checkbox,
  Button,
  Chip,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import PersonIcon from '@mui/icons-material/Person';
import { useTranslation } from 'react-i18next';
import type { GroupMemberDto } from '@/types/group';
import RemoveMemberConfirmDialog from './RemoveMemberConfirmDialog';

interface MemberListTableProps {
  members: GroupMemberDto[];
  onRemove: (studentId: string) => void;
  onBatchRemove?: (studentIds: string[]) => void;
  isRemoving: boolean;
  isBatchRemoving?: boolean;
  isEditable?: boolean;
}

export default function MemberListTable({
  members,
  onRemove,
  onBatchRemove,
  isRemoving,
  isBatchRemoving = false,
  isEditable = true,
}: MemberListTableProps) {
  const { t } = useTranslation('group');
  const { t: tc } = useTranslation('common');
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pendingRemoval, setPendingRemoval] = useState<{
    type: 'single' | 'batch';
    studentId?: string;
    studentName?: string;
    studentIds?: string[];
  } | null>(null);

  const allSelected = members.length > 0 && selectedIds.length === members.length;
  const someSelected = selectedIds.length > 0 && selectedIds.length < members.length;

  const handleToggleAll = () => {
    if (allSelected) {
      setSelectedIds([]);
    } else {
      setSelectedIds(members.map((m) => m.studentId));
    }
  };

  const handleToggle = (studentId: string) => {
    setSelectedIds((prev) =>
      prev.includes(studentId)
        ? prev.filter((id) => id !== studentId)
        : [...prev, studentId],
    );
  };

  const handleSingleRemoveClick = (member: GroupMemberDto) => {
    setPendingRemoval({
      type: 'single',
      studentId: member.studentId,
      studentName: `${member.firstName} ${member.lastName}`,
    });
    setConfirmOpen(true);
  };

  const handleBatchRemoveClick = () => {
    setPendingRemoval({
      type: 'batch',
      studentIds: selectedIds,
    });
    setConfirmOpen(true);
  };

  const handleConfirmRemove = () => {
    if (!pendingRemoval) return;

    if (pendingRemoval.type === 'single' && pendingRemoval.studentId) {
      onRemove(pendingRemoval.studentId);
    } else if (pendingRemoval.type === 'batch' && pendingRemoval.studentIds && onBatchRemove) {
      onBatchRemove(pendingRemoval.studentIds);
      setSelectedIds([]);
    }

    setConfirmOpen(false);
    setPendingRemoval(null);
  };

  const handleConfirmClose = () => {
    setConfirmOpen(false);
    setPendingRemoval(null);
  };

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
    <Box>
      {isEditable && selectedIds.length > 0 && onBatchRemove && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2, p: 1.5, bgcolor: 'action.hover', borderRadius: 1 }}>
          <Chip
            label={`${selectedIds.length} ${t('selected')}`}
            color="primary"
            size="small"
          />
          <Button
            variant="outlined"
            color="error"
            size="small"
            startIcon={<DeleteIcon />}
            onClick={handleBatchRemoveClick}
            disabled={isBatchRemoving}
          >
            {t('removeSelected', { count: selectedIds.length })}
          </Button>
        </Box>
      )}

      <TableContainer component={Paper} variant="outlined">
        <Table>
          <TableHead>
            <TableRow>
              {isEditable && onBatchRemove && (
                <TableCell padding="checkbox">
                  <Checkbox
                    indeterminate={someSelected}
                    checked={allSelected}
                    onChange={handleToggleAll}
                    size="small"
                  />
                </TableCell>
              )}
              <TableCell>#</TableCell>
              <TableCell>{tc('name')}</TableCell>
              <TableCell>{tc('email')}</TableCell>
              <TableCell>{tc('phone')}</TableCell>
              <TableCell>{t('createdAt')}</TableCell>
              {isEditable && <TableCell align="right" />}
            </TableRow>
          </TableHead>
          <TableBody>
            {members.map((member, index) => (
              <TableRow key={member.id} hover selected={selectedIds.includes(member.studentId)}>
                {isEditable && onBatchRemove && (
                  <TableCell padding="checkbox">
                    <Checkbox
                      checked={selectedIds.includes(member.studentId)}
                      onChange={() => handleToggle(member.studentId)}
                      size="small"
                    />
                  </TableCell>
                )}
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
                {isEditable && (
                  <TableCell align="right">
                    <Tooltip title={t('removeMember')}>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleSingleRemoveClick(member)}
                        disabled={isRemoving}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                )}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <RemoveMemberConfirmDialog
        open={confirmOpen}
        onClose={handleConfirmClose}
        onConfirm={handleConfirmRemove}
        memberCount={pendingRemoval?.type === 'batch' ? (pendingRemoval.studentIds?.length ?? 0) : 1}
        memberName={pendingRemoval?.studentName}
        isPending={isRemoving || isBatchRemoving}
      />
    </Box>
  );
}
