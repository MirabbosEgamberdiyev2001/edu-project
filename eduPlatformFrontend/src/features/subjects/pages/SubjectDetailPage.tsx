import { useParams, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  Chip,
  Button,
  CircularProgress,
  Avatar,
  IconButton,
  Tooltip,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ArchiveIcon from '@mui/icons-material/Archive';
import UnarchiveIcon from '@mui/icons-material/Unarchive';
import TopicIcon from '@mui/icons-material/Topic';
import QuizIcon from '@mui/icons-material/Quiz';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { useTranslation } from 'react-i18next';
import { useSubject } from '../hooks/useSubject';
import { useSubjectMutations } from '../hooks/useSubjectMutations';
import { resolveTranslation } from '@/utils/i18nUtils';
import SubjectFormDialog from '../components/SubjectFormDialog';
import SubjectDeleteDialog from '../components/SubjectDeleteDialog';
import TopicTreeView from '@/features/topics/components/TopicTreeView';
import type { CreateSubjectRequest, UpdateSubjectRequest } from '@/types/subject';
import { useAuthStore } from '@/stores/authStore';
import { PageShell } from '@/components/ui';

const GRADES = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];

function isImageUrl(value: string | null): boolean {
  if (!value) return false;
  return /^https?:\/\/.+/i.test(value) || value.startsWith('/');
}

export default function SubjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('subject');
  const { t: tTopic } = useTranslation('topic');
  const { data: subject, isLoading, isError } = useSubject(id);
  const { update, remove, archive, restore } = useSubjectMutations();
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN';

  const [formOpen, setFormOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [selectedGrade, setSelectedGrade] = useState<number | null>(null);

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (isError || !subject) {
    return (
      <Box sx={{ textAlign: 'center', py: 8 }}>
        <Typography variant="h6" color="text.secondary">{t('notFound')}</Typography>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/subjects')} sx={{ mt: 2 }}>
          {t('common:back')}
        </Button>
      </Box>
    );
  }

  const displayName = resolveTranslation(subject.nameTranslations) || subject.name;
  const displayDescription = resolveTranslation(subject.descriptionTranslations) || subject.description;

  const handleFormSubmit = (formData: CreateSubjectRequest | UpdateSubjectRequest) => {
    update.mutate({ id: subject.id, data: formData }, { onSuccess: () => setFormOpen(false) });
  };

  const handleDeleteConfirm = () => {
    remove.mutate(subject.id, { onSuccess: () => { setDeleteOpen(false); navigate('/subjects'); } });
  };

  const handleArchive = () => {
    archive.mutate(subject.id);
  };

  const handleRestore = () => {
    restore.mutate(subject.id);
  };

  return (
    <PageShell
      title={displayName}
      subtitle={displayDescription || undefined}
      breadcrumbs={[
        { label: t('common:subjects'), to: '/subjects' },
        { label: displayName },
      ]}
      actions={
        isAdmin ? (
          <Box sx={{ display: 'flex', gap: 0.5 }}>
            <Tooltip title={t('edit')}>
              <IconButton color="primary" onClick={() => setFormOpen(true)}>
                <EditIcon />
              </IconButton>
            </Tooltip>
            {subject.isArchived ? (
              <Tooltip title={t('restore')}>
                <IconButton color="success" onClick={handleRestore}>
                  <UnarchiveIcon />
                </IconButton>
              </Tooltip>
            ) : (
              <Tooltip title={t('archive')}>
                <IconButton color="warning" onClick={handleArchive}>
                  <ArchiveIcon />
                </IconButton>
              </Tooltip>
            )}
            <Tooltip title={t('delete')}>
              <IconButton color="error" onClick={() => setDeleteOpen(true)}>
                <DeleteIcon />
              </IconButton>
            </Tooltip>
          </Box>
        ) : undefined
      }
    >
      {/* Subject icon + Stats */}
      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap', alignItems: 'center' }}>
        {subject.icon && (
          isImageUrl(subject.icon) ? (
            <Avatar src={subject.icon} variant="rounded" sx={{ width: 40, height: 40, bgcolor: subject.color || 'primary.main' }}>
              <MenuBookIcon />
            </Avatar>
          ) : (
            <Typography variant="h4" component="span">{subject.icon}</Typography>
          )
        )}
        <Chip
          icon={subject.isActive ? <CheckCircleIcon /> : <CancelIcon />}
          label={subject.isActive ? t('statusActive') : t('statusInactive')}
          color={subject.isActive ? 'success' : 'default'}
          variant="outlined"
        />
        {subject.isArchived && (
          <Chip label={t('statusArchived')} color="warning" variant="outlined" />
        )}
        <Chip icon={<TopicIcon />} label={`${t('topicCount')}: ${subject.topicCount}`} variant="outlined" />
        <Chip icon={<QuizIcon />} label={`${t('questionCount')}: ${subject.questionCount}`} variant="outlined" />
      </Box>

      {/* Grade Selector + Topics Tree */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>{tTopic('title')}</Typography>
        <Typography variant="subtitle2" sx={{ mb: 1 }}>{t('form.gradeLevel')}</Typography>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
          {GRADES.map((grade) => (
            <Chip
              key={grade}
              label={`${grade}`}
              color={selectedGrade === grade ? 'primary' : 'default'}
              variant={selectedGrade === grade ? 'filled' : 'outlined'}
              onClick={() => setSelectedGrade(grade)}
              sx={{ minWidth: 40 }}
            />
          ))}
        </Box>
        {selectedGrade !== null ? (
          <TopicTreeView subjectId={subject.id} gradeLevel={selectedGrade} />
        ) : (
          <Box sx={{ textAlign: 'center', py: 3 }}>
            <Typography variant="body2" color="text.secondary">{tTopic('selectGrade')}</Typography>
          </Box>
        )}
      </Paper>

      {/* Details */}
      <Paper sx={{ p: 2 }}>
        <Typography variant="h6" fontWeight={600} sx={{ mb: 2 }}>{t('details')}</Typography>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
          <Box>
            <Typography variant="body2" color="text.secondary">{t('form.icon')}</Typography>
            <Typography variant="body1">
              {subject.icon ? (
                isImageUrl(subject.icon) ? (
                  <Avatar src={subject.icon} variant="rounded" sx={{ width: 32, height: 32 }} />
                ) : (
                  subject.icon
                )
              ) : '—'}
            </Typography>
          </Box>
          <Box>
            <Typography variant="body2" color="text.secondary">{t('form.color')}</Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Box sx={{ width: 24, height: 24, borderRadius: '50%', bgcolor: subject.color || '#ccc' }} />
              <Typography variant="body1">{subject.color || '—'}</Typography>
            </Box>
          </Box>
          <Box>
            <Typography variant="body2" color="text.secondary">{t('createdAt')}</Typography>
            <Typography variant="body1">{new Date(subject.createdAt).toLocaleDateString()}</Typography>
          </Box>
          <Box>
            <Typography variant="body2" color="text.secondary">{t('updatedAt')}</Typography>
            <Typography variant="body1">{new Date(subject.updatedAt).toLocaleDateString()}</Typography>
          </Box>
        </Box>
      </Paper>

      <SubjectFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleFormSubmit}
        subject={subject}
        isPending={update.isPending}
      />

      <SubjectDeleteDialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={handleDeleteConfirm}
        subject={subject}
        isPending={remove.isPending}
      />
    </PageShell>
  );
}
