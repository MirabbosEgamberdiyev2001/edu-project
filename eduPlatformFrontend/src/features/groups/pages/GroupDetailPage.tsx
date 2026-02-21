import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Paper,
  Chip,
  CircularProgress,
  IconButton,
  Tooltip,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon from '@mui/icons-material/Edit';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import ArchiveIcon from '@mui/icons-material/Archive';
import GroupIcon from '@mui/icons-material/Group';
import { useTranslation } from 'react-i18next';
import { useGroup } from '../hooks/useGroups';
import { useGroupMembers, useRemoveMember } from '../hooks/useGroupMembers';
import { useGroupMutations } from '../hooks/useGroupMutations';
import MemberListTable from '../components/MemberListTable';
import GroupFormDialog from '../components/GroupFormDialog';
import AddMembersDialog from '../components/AddMembersDialog';
import { GroupStatus, type GroupDto, type CreateGroupRequest, type UpdateGroupRequest } from '@/types/group';

export default function GroupDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('group');

  const { data: group, isLoading } = useGroup(id);
  const { data: members, isLoading: membersLoading, refetch: refetchMembers } = useGroupMembers(id);
  const removeMember = useRemoveMember(id!);
  const { update, archive } = useGroupMutations();

  const [formOpen, setFormOpen] = useState(false);
  const [addMembersOpen, setAddMembersOpen] = useState(false);

  const isArchived = group?.status === GroupStatus.ARCHIVED;

  const handleFormSubmit = (data: CreateGroupRequest | UpdateGroupRequest) => {
    update.mutate({ id: id!, data }, { onSuccess: () => setFormOpen(false) });
  };

  const handleRemoveMember = (studentId: string) => {
    removeMember.mutate(studentId);
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!group) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography variant="h6" color="text.secondary">{t('notFound')}</Typography>
        <Button onClick={() => navigate('/groups')} sx={{ mt: 2 }}>{t('common:back')}</Button>
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
        <IconButton onClick={() => navigate('/groups')}>
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h5" fontWeight={700} sx={{ flex: 1 }}>
          {group.name}
        </Typography>
        {!isArchived && (
          <>
            <Tooltip title={t('editGroup')}>
              <IconButton onClick={() => setFormOpen(true)}>
                <EditIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title={t('archiveGroup')}>
              <IconButton onClick={() => archive.mutate(id!)}>
                <ArchiveIcon />
              </IconButton>
            </Tooltip>
          </>
        )}
      </Box>

      <Paper sx={{ p: 3, mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          <GroupIcon color="primary" sx={{ fontSize: 40 }} />
          <Box>
            <Typography variant="h6" fontWeight={600}>{group.name}</Typography>
            {group.description && (
              <Typography variant="body2" color="text.secondary">{group.description}</Typography>
            )}
          </Box>
        </Box>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          <Chip
            label={isArchived ? t('archived') : t('active')}
            color={isArchived ? 'default' : 'success'}
            size="small"
          />
          <Chip
            label={t('memberCount', { count: group.memberCount })}
            variant="outlined"
            size="small"
          />
          <Typography variant="caption" color="text.secondary" sx={{ display: 'flex', alignItems: 'center' }}>
            {t('createdAt')}: {new Date(group.createdAt).toLocaleDateString()}
          </Typography>
        </Box>
      </Paper>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6" fontWeight={600}>{t('members')}</Typography>
        {!isArchived && (
          <Button
            variant="contained"
            size="small"
            startIcon={<PersonAddIcon />}
            onClick={() => setAddMembersOpen(true)}
          >
            {t('addMembers')}
          </Button>
        )}
      </Box>

      {membersLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <MemberListTable
          members={members || []}
          onRemove={handleRemoveMember}
          isRemoving={removeMember.isPending}
        />
      )}

      <GroupFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleFormSubmit}
        group={group}
        isPending={update.isPending}
      />

      {id && (
        <AddMembersDialog
          open={addMembersOpen}
          onClose={() => setAddMembersOpen(false)}
          groupId={id}
          onSuccess={() => refetchMembers()}
        />
      )}
    </Box>
  );
}
