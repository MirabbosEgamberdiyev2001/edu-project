import { useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  MenuItem,
  Paper,
  CircularProgress,
  Avatar,
  Alert,
} from '@mui/material';
import TopicIcon from '@mui/icons-material/Topic';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import { useTranslation } from 'react-i18next';
import { useSubjects } from '@/features/subjects/hooks/useSubjects';
import TopicTreeView from '../components/TopicTreeView';
import { resolveTranslation } from '@/utils/i18nUtils';

export default function TopicsPage() {
  const { t } = useTranslation('topic');
  const { data: subjectsData, isLoading: subjectsLoading, isError: subjectsError } = useSubjects({ size: 100 });
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>('');

  const subjects = subjectsData?.content || [];

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" fontWeight={700}>{t('pageTitle')}</Typography>
        <Typography variant="body2" color="text.secondary">{t('pageSubtitle')}</Typography>
      </Box>

      {/* Subject selector */}
      <Paper sx={{ p: 2, mb: 3 }}>
        {subjectsLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
            <CircularProgress size={24} />
          </Box>
        ) : subjectsError ? (
          <Alert severity="error">{t('common:error')}</Alert>
        ) : subjects.length > 0 ? (
          <TextField
            select
            label={t('selectSubject')}
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value)}
            fullWidth
            size="small"
          >
            {subjects.map((subject) => {
              const name = resolveTranslation(subject.nameTranslations) || subject.name;
              return (
                <MenuItem key={subject.id} value={subject.id}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    {subject.icon ? (
                      /^https?:\/\/.+/i.test(subject.icon) ? (
                        <Avatar src={subject.icon} sx={{ width: 24, height: 24 }} variant="rounded" />
                      ) : (
                        <Typography component="span" sx={{ fontSize: 18, lineHeight: 1 }}>
                          {subject.icon}
                        </Typography>
                      )
                    ) : (
                      <MenuBookIcon fontSize="small" color="action" />
                    )}
                    <Typography variant="body2">{name}</Typography>
                    {subject.category && (
                      <Typography variant="caption" color="text.secondary">
                        ({subject.category})
                      </Typography>
                    )}
                  </Box>
                </MenuItem>
              );
            })}
          </TextField>
        ) : (
          <Box sx={{ textAlign: 'center', py: 2 }}>
            <MenuBookIcon sx={{ fontSize: 40, color: 'text.disabled', mb: 1 }} />
            <Typography variant="body2" color="text.secondary">{t('noSubjects')}</Typography>
          </Box>
        )}
      </Paper>

      {/* Topic tree */}
      {selectedSubjectId ? (
        <Paper sx={{ p: 2 }}>
          <TopicTreeView subjectId={selectedSubjectId} />
        </Paper>
      ) : subjects.length > 0 ? (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <TopicIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">{t('selectSubjectHint')}</Typography>
          <Typography variant="body2" color="text.disabled">{t('selectSubjectDescription')}</Typography>
        </Box>
      ) : null}
    </Box>
  );
}
