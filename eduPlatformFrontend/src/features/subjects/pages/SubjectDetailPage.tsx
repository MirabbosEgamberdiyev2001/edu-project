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
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableRow,
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
import { LANGUAGE_LABELS } from '@/config';
import SubjectFormDialog from '../components/SubjectFormDialog';
import SubjectDeleteDialog from '../components/SubjectDeleteDialog';
import TopicTreeView from '@/features/topics/components/TopicTreeView';
import type { SubjectDto, CreateSubjectRequest, UpdateSubjectRequest } from '@/types/subject';

const LOCALE_KEY_TO_FRONTEND: Record<string, string> = {
  uz_latn: 'uzl',
  uz_cyrl: 'uzc',
  en: 'en',
  ru: 'ru',
};

const LOCALE_KEYS = ['uz_latn', 'uz_cyrl', 'en', 'ru'] as const;

function isImageUrl(value: string | null): boolean {
  if (!value) return false;
  return /^https?:\/\/.+/i.test(value) || value.startsWith('/');
}

export default function SubjectDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation('subject');
  const { data: subject, isLoading, isError } = useSubject(id);
  const { update, remove, archive, restore } = useSubjectMutations();

  const [formOpen, setFormOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

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
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
        <IconButton onClick={() => navigate('/subjects')}>
          <ArrowBackIcon />
        </IconButton>
        <Box sx={{ flex: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            {subject.icon ? (
              isImageUrl(subject.icon) ? (
                <Avatar
                  src={subject.icon}
                  variant="rounded"
                  sx={{ width: 48, height: 48, bgcolor: subject.color || 'primary.main' }}
                >
                  <MenuBookIcon />
                </Avatar>
              ) : (
                <Typography variant="h4" component="span">{subject.icon}</Typography>
              )
            ) : (
              <Avatar
                variant="rounded"
                sx={{ width: 48, height: 48, bgcolor: subject.color || 'primary.main' }}
              >
                <MenuBookIcon />
              </Avatar>
            )}
            <Box>
              <Typography variant="h5" fontWeight={700}>{displayName}</Typography>
              {displayDescription && (
                <Typography variant="body2" color="text.secondary">{displayDescription}</Typography>
              )}
            </Box>
          </Box>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
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
      </Box>

      {/* Stats */}
      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        {subject.category && (
          <Chip label={t(`categories.${subject.category}`)} color="primary" variant="outlined" />
        )}
        {subject.gradeLevel && (
          <Chip label={`${t('form.gradeLevel')}: ${subject.gradeLevel}`} variant="outlined" />
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

      {/* Translations table */}
      <Paper sx={{ mb: 3 }}>
        <Box sx={{ p: 2, pb: 1 }}>
          <Typography variant="h6" fontWeight={600}>{t('translations')}</Typography>
          <Typography variant="body2" color="text.secondary">{t('translationsHint')}</Typography>
        </Box>
        <Divider />
        <TableContainer>
          <Table>
            <TableBody>
              {LOCALE_KEYS.map((localeKey) => {
                const frontendLang = LOCALE_KEY_TO_FRONTEND[localeKey] || localeKey;
                const langLabel = LANGUAGE_LABELS[frontendLang] || localeKey;
                const nameVal = subject.nameTranslations?.[localeKey];
                const descVal = subject.descriptionTranslations?.[localeKey];
                const isFilled = Boolean(nameVal?.trim());

                return (
                  <TableRow key={localeKey}>
                    <TableCell sx={{ width: 160, fontWeight: 600 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        {isFilled ? (
                          <CheckCircleIcon sx={{ fontSize: 16, color: 'success.main' }} />
                        ) : (
                          <CancelIcon sx={{ fontSize: 16, color: 'text.disabled' }} />
                        )}
                        {langLabel}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body1" fontWeight={500}>
                        {nameVal || <Typography component="span" color="text.disabled" fontStyle="italic">{t('form.notFilled')}</Typography>}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {descVal || <Typography component="span" color="text.disabled" fontStyle="italic">{t('form.notFilled')}</Typography>}
                      </Typography>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Topics Tree */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <TopicTreeView subjectId={subject.id} />
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
    </Box>
  );
}
