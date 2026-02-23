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
  Typography,
  Tabs,
  Tab,
  Alert,
  MenuItem,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import { useTranslation } from 'react-i18next';
import { type SubjectDto, type CreateSubjectRequest, type UpdateSubjectRequest } from '@/types/subject';
import { SUPPORTED_LANGUAGES, LANGUAGE_LABELS } from '@/config';

interface SubjectFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateSubjectRequest | UpdateSubjectRequest) => void;
  subject?: SubjectDto | null;
  isPending: boolean;
}

const COLORS = ['#1976d2', '#388e3c', '#f57c00', '#d32f2f', '#7b1fa2', '#0288d1', '#455a64'];

// Maps frontend language codes to backend JSONB keys
const LANG_KEY_MAP: Record<string, string> = {
  uzl: 'uz_latn',
  uzc: 'uz_cyrl',
  en: 'en',
  ru: 'ru',
};

function langKey(frontendLang: string): string {
  return LANG_KEY_MAP[frontendLang] || 'uz_latn';
}

export default function SubjectFormDialog({ open, onClose, onSubmit, subject, isPending }: SubjectFormDialogProps) {
  const { t } = useTranslation('subject');
  const isEdit = Boolean(subject);

  const [langTab, setLangTab] = useState(0);
  const [name, setName] = useState<Record<string, string>>({});
  const [description, setDescription] = useState<Record<string, string>>({});
  const [icon, setIcon] = useState('');
  const [color, setColor] = useState('#1976d2');
  const [gradeLevel, setGradeLevel] = useState<number | ''>('');

  useEffect(() => {
    if (open) {
      if (subject) {
        // Edit mode: populate ALL languages from translations map
        setName(subject.nameTranslations ? { ...subject.nameTranslations } : {});
        setDescription(subject.descriptionTranslations ? { ...subject.descriptionTranslations } : {});
        setIcon(subject.icon || '');
        setColor(subject.color || '#1976d2');
        setGradeLevel(subject.gradeLevel || '');
      } else {
        // Create mode: start fresh
        setName({});
        setDescription({});
        setIcon('');
        setColor('#1976d2');
        setGradeLevel('');
      }
      setLangTab(0);
    }
  }, [open, subject]);

  const handleSubmit = () => {
    // Filter out empty string values
    const cleanName: Record<string, string> = {};
    const cleanDesc: Record<string, string> = {};

    Object.entries(name).forEach(([k, v]) => {
      if (v.trim()) cleanName[k] = v.trim();
    });
    Object.entries(description).forEach(([k, v]) => {
      if (v.trim()) cleanDesc[k] = v.trim();
    });

    const data: CreateSubjectRequest = {
      name: cleanName,
      ...(Object.keys(cleanDesc).length > 0 && { description: cleanDesc }),
      ...(icon && { icon }),
      ...(color && { color }),
      ...(gradeLevel !== '' && { gradeLevel: Number(gradeLevel) }),
    };
    onSubmit(data);
  };

  const currentFrontendLang = SUPPORTED_LANGUAGES[langTab] || 'uzl';
  const currentKey = langKey(currentFrontendLang);

  // Check which languages have name filled
  const isLangFilled = (frontLang: string) => {
    const key = langKey(frontLang);
    return Boolean(name[key]?.trim());
  };

  const isDefaultFilled = Boolean(name['uz_latn']?.trim());

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? t('editTitle') : t('createTitle')}</DialogTitle>
      <DialogContent>
        <Alert severity="info" sx={{ mb: 2, py: 0.5 }}>
          {t('form.langHint')}
        </Alert>

        <Tabs
          value={langTab}
          onChange={(_, v) => !isPending && setLangTab(v)}
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
          disabled={isPending}
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
          disabled={isPending}
          sx={{ mb: 2 }}
        />

        <TextField
          label={t('form.icon')}
          placeholder={t('form.iconPlaceholder')}
          value={icon}
          onChange={(e) => setIcon(e.target.value)}
          fullWidth
          disabled={isPending}
          sx={{ mb: 2 }}
          helperText={icon && /^https?:\/\/.+/i.test(icon) ? t('form.iconPreview') : undefined}
          InputProps={{
            endAdornment: icon && /^https?:\/\/.+/i.test(icon) ? (
              <Box
                component="img"
                src={icon}
                sx={{ width: 32, height: 32, objectFit: 'contain', borderRadius: 1 }}
                onError={(e: React.SyntheticEvent<HTMLImageElement>) => { e.currentTarget.style.display = 'none'; }}
              />
            ) : icon ? (
              <Typography variant="h6" component="span">{icon}</Typography>
            ) : undefined,
          }}
        />

        <TextField
          select
          label={t('form.gradeLevel')}
          value={gradeLevel}
          onChange={(e) => setGradeLevel(e.target.value === '' ? '' : Number(e.target.value))}
          fullWidth
          disabled={isPending}
          sx={{ mb: 2 }}
        >
          <MenuItem value="">{t('form.noGrade', 'Not specified')}</MenuItem>
          {Array.from({ length: 11 }, (_, i) => i + 1).map((g) => (
            <MenuItem key={g} value={g}>{g}</MenuItem>
          ))}
        </TextField>

        <Box sx={{ mb: 1 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            {t('form.color')}
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            {COLORS.map((c) => (
              <Box
                key={c}
                onClick={() => setColor(c)}
                sx={{
                  width: 32,
                  height: 32,
                  borderRadius: '50%',
                  bgcolor: c,
                  cursor: 'pointer',
                  border: color === c ? '3px solid' : '2px solid transparent',
                  borderColor: color === c ? 'text.primary' : 'transparent',
                }}
              />
            ))}
          </Box>
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} disabled={isPending}>{t('common:cancel')}</Button>
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
