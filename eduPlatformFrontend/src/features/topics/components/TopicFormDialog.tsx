import React, { useEffect, useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import { useTranslation } from 'react-i18next';
import type { TopicTreeDto, CreateTopicRequest, UpdateTopicRequest } from '@/types/topic';
import { SUPPORTED_LANGUAGES, LANGUAGE_LABELS } from '@/config';
import { toLocaleKey } from '@/utils/i18nUtils';

interface TopicFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateTopicRequest | UpdateTopicRequest) => void;
  topic?: TopicTreeDto | null;
  parentId?: string | null;
  isPending: boolean;
}

export default function TopicFormDialog({ open, onClose, onSubmit, topic, parentId, isPending }: TopicFormDialogProps) {
  const { t } = useTranslation('topic');
  const isEdit = Boolean(topic);

  const [langTab, setLangTab] = useState(0);
  const [name, setName] = useState<Record<string, string>>({});
  const [description, setDescription] = useState<Record<string, string>>({});

  useEffect(() => {
    if (open) {
      if (topic) {
        setName(topic.nameTranslations ? { ...topic.nameTranslations } : {});
        setDescription(topic.descriptionTranslations ? { ...topic.descriptionTranslations } : {});
      } else {
        setName({});
        setDescription({});
      }
      setLangTab(0);
    }
  }, [open, topic]);

  const handleSubmit = () => {
    const cleanName: Record<string, string> = {};
    const cleanDesc: Record<string, string> = {};

    Object.entries(name).forEach(([k, v]) => {
      if (v.trim()) cleanName[k] = v.trim();
    });
    Object.entries(description).forEach(([k, v]) => {
      if (v.trim()) cleanDesc[k] = v.trim();
    });

    if (isEdit) {
      const data: UpdateTopicRequest = {
        name: cleanName,
        ...(Object.keys(cleanDesc).length > 0 && { description: cleanDesc }),
      };
      onSubmit(data);
    } else {
      const data: CreateTopicRequest = {
        name: cleanName,
        ...(Object.keys(cleanDesc).length > 0 && { description: cleanDesc }),
        ...(parentId && { parentId }),
      };
      onSubmit(data);
    }
  };

  const currentFrontendLang = SUPPORTED_LANGUAGES[langTab] || 'uzl';
  const currentKey = toLocaleKey(currentFrontendLang);

  const isLangFilled = (frontLang: string) => Boolean(name[toLocaleKey(frontLang)]?.trim());
  const isDefaultFilled = Boolean(name['uz_latn']?.trim());

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? t('editTitle') : (parentId ? t('createSubtopic') : t('createTitle'))}</DialogTitle>
      <DialogContent>
        <Alert severity="info" sx={{ mb: 2, py: 0.5 }}>
          {t('form.langHint')}
        </Alert>

        <Tabs
          value={langTab}
          onChange={(_, v) => setLangTab(v)}
          sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
        >
          {SUPPORTED_LANGUAGES.map((lang) => (
            <Tab
              key={lang}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  {isLangFilled(lang) ? (
                    <CheckCircleIcon sx={{ fontSize: 14, color: 'success.main' }} />
                  ) : (
                    <RadioButtonUncheckedIcon sx={{ fontSize: 14, color: 'text.disabled' }} />
                  )}
                  {LANGUAGE_LABELS[lang]}
                </Box>
              }
              sx={{ textTransform: 'none', minWidth: 0, px: 1.5 }}
            />
          ))}
        </Tabs>

        <TextField
          label={`${t('form.name')} (${LANGUAGE_LABELS[currentFrontendLang]})`}
          placeholder={t('form.namePlaceholder')}
          value={name[currentKey] || ''}
          onChange={(e) => setName({ ...name, [currentKey]: e.target.value })}
          fullWidth
          required={currentFrontendLang === 'uzl'}
          sx={{ mb: 2 }}
        />

        <TextField
          label={`${t('form.description')} (${LANGUAGE_LABELS[currentFrontendLang]})`}
          placeholder={t('form.descriptionPlaceholder')}
          value={description[currentKey] || ''}
          onChange={(e) => setDescription({ ...description, [currentKey]: e.target.value })}
          fullWidth
          multiline
          rows={3}
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>{t('common:cancel')}</Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={isPending || !isDefaultFilled}
        >
          {isPending ? <CircularProgress size={20} /> : (isEdit ? t('common:save') : t('create'))}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
