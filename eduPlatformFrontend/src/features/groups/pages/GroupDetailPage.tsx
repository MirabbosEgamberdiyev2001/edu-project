import { useState, useMemo } from 'react';
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
import EditIcon from '@mui/icons-material/Edit';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import ArchiveIcon from '@mui/icons-material/Archive';
import AssignmentIcon from '@mui/icons-material/Assignment';
import GroupIcon from '@mui/icons-material/Group';
import SchoolIcon from '@mui/icons-material/School';
import { useTranslation } from 'react-i18next';
import { useGroup } from '../hooks/useGroups';
import { useGroupMembers, useRemoveMember, useRemoveMembersBatch } from '../hooks/useGroupMembers';
import { useGroupMutations } from '../hooks/useGroupMutations';
import { useTests } from '@/features/tests/hooks/useTests';
import { useAssignmentMutations } from '@/features/assignments/hooks/useAssignmentMutations';
import MemberListTable from '../components/MemberListTable';
import GroupFormDialog from '../components/GroupFormDialog';
import AddMembersDialog from '../components/AddMembersDialog';
import AssignmentFormDialog from '@/features/assignments/components/AssignmentFormDialog';
import { GroupStatus, type CreateGroupRequest, type UpdateGroupRequest } from '@/types/group';
import type { CreateAssignmentRequest } from '@/types/assignment';
import { resolveTranslation } from '@/utils/i18nUtils';
import { PageShell } from '@/components/ui';

export default function GroupDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('group');
  const { t: tA } = useTranslation('assignment');

  const { data: group, isLoading } = useGroup(id);
  const { data: members, isLoading: membersLoading, refetch: refetchMembers } = useGroupMembers(id);
  const removeMember = useRemoveMember(id!);
  const removeMembersBatch = useRemoveMembersBatch(id!);
  const { update, archive } = useGroupMutations();
  const { data: testsData } = useTests({ size: 200 });
  const { create: createAssignment } = useAssignmentMutations();

  const [formOpen, setFormOpen] = useState(false);
  const [addMembersOpen, setAddMembersOpen] = useState(false);
  const [assignmentOpen, setAssignmentOpen] = useState(false);

  const isArchived = group?.status === GroupStatus.ARCHIVED;

  const tests = useMemo(() =>
    testsData?.content?.map((item) => ({
      id: item.id,
      title: resolveTranslation(item.titleTranslations) || item.title,
    })) || [],
    [testsData],
  );

  const handleFormSubmit = (data: CreateGroupRequest | UpdateGroupRequest) => {
    update.mutate({ id: id!, data }, { onSuccess: () => setFormOpen(false) });
  };

  const handleRemoveMember = (studentId: string) => {
    removeMember.mutate(studentId);
  };

  const handleBatchRemove = (studentIds: string[]) => {
    removeMembersBatch.mutate(studentIds);
  };

  const handleAssignmentSubmit = (data: CreateAssignmentRequest) => {
    createAssignment.mutate(data, {
      onSuccess: () => setAssignmentOpen(false),
    });
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
    <PageShell
      title={group.name}
      subtitle={group.description || undefined}
      breadcrumbs={[
        { label: t('common:groups'), to: '/groups' },
        { label: group.name },
      ]}
      actions={
        !isArchived ? (
          <Box sx={{ display: 'flex', gap: 0.5 }}>
            <Tooltip title={tA('createAssignment')}>
              <IconButton onClick={() => setAssignmentOpen(true)} color="primary">
                <AssignmentIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title={t('editGroup')}>
              <IconButton onClick={() => setFormOpen(true)}>
                <EditIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title={t('archiveGroup')}>
              <IconButton onClick={() => archive.mutate(id!)} color="warning">
                <ArchiveIcon />
              </IconButton>
            </Tooltip>
          </Box>
        ) : undefined
      }
    >

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
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <Chip
            label={isArchived ? t('archived') : t('active')}
            color={isArchived ? 'default' : 'success'}
            size="small"
          />
          {group.subjectName && (
            <Chip
              icon={<SchoolIcon />}
              label={group.subjectName}
              color="primary"
              size="small"
              variant="outlined"
            />
          )}
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

      {!isArchived && (
        <Box sx={{ mb: 3 }}>
          <Button
            variant="outlined"
            startIcon={<AssignmentIcon />}
            onClick={() => setAssignmentOpen(true)}
          >
            {tA('createAssignment')}
          </Button>
        </Box>
      )}

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
          onBatchRemove={handleBatchRemove}
          isRemoving={removeMember.isPending}
          isBatchRemoving={removeMembersBatch.isPending}
          isEditable={!isArchived}
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

      <AssignmentFormDialog
        open={assignmentOpen}
        onClose={() => setAssignmentOpen(false)}
        onSubmit={handleAssignmentSubmit}
        isPending={createAssignment.isPending}
        groups={[]}
        tests={tests}
        defaultGroupId={id}
      />
    </PageShell>
  );
}
