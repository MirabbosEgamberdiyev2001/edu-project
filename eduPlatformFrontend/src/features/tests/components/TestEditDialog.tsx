import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  MenuItem,
  Box,
  CircularProgress,
} from '@mui/material';
import { TestCategory } from '@/types/test';
import type { TestHistoryDto, HeaderConfig } from '@/types/test';
import { toLocaleKey } from '@/utils/i18nUtils';
import MultiLangInput from './MultiLangInput';

interface TestEditDialogProps {
  open: boolean;
  onClose: () => void;
  onSave: (data: {
    title?: string;
    titleTranslations?: Record<string, string>;
    category?: string;
    headerConfig?: HeaderConfig;
  }) => void;
  test: TestHistoryDto;
  isPending: boolean;
}

const LOCALE_TO_LANG: Record<string, string> = {
  uzl: 'uz',
  uzc: 'uz',
  en: 'en',
  ru: 'ru',
};

export default function TestEditDialog({ open, onClose, onSave, test, isPending }: TestEditDialogProps) {
  const { t, i18n } = useTranslation('test');
  const dateLang = LOCALE_TO_LANG[i18n.language] || 'uz';

  const [titleTranslations, setTitleTranslations] = useState<Record<string, string>>({});
  const [category, setCategory] = useState<string>('');
  const [schoolNameTranslations, setSchoolNameTranslations] = useState<Record<string, string>>({});
  const [className, setClassName] = useState('');
  const [teacherNameTranslations, setTeacherNameTranslations] = useState<Record<string, string>>({});
  const [date, setDate] = useState('');

  useEffect(() => {
    if (open && test) {
      setTitleTranslations(test.titleTranslations || (test.title ? { [toLocaleKey('uzl')]: test.title } : {}));
      setCategory(test.category || '');
      const hc = test.headerConfig || {};
      const snt = hc.schoolNameTranslations as Record<string, string> | undefined;
      setSchoolNameTranslations(
        snt || (hc.schoolName ? { [toLocaleKey('uzl')]: hc.schoolName as string } : {}),
      );
      setClassName((hc.className as string) || '');
      const tnt = hc.teacherNameTranslations as Record<string, string> | undefined;
      setTeacherNameTranslations(
        tnt || (hc.teacherName ? { [toLocaleKey('uzl')]: hc.teacherName as string } : {}),
      );
      setDate((hc.date as string) || '');
    }
  }, [open, test]);

  const handleSubmit = () => {
    const data: {
      title?: string;
      titleTranslations?: Record<string, string>;
      category?: string;
      headerConfig?: HeaderConfig;
    } = {};

    data.titleTranslations = titleTranslations;
    if (category !== (test.category || '')) data.category = category || undefined;

    const headerConfig: HeaderConfig = {};
    headerConfig.schoolNameTranslations = schoolNameTranslations;
    headerConfig.className = className;
    headerConfig.teacherNameTranslations = teacherNameTranslations;
    headerConfig.date = date;
    data.headerConfig = headerConfig;

    onSave(data);
  };

  const hasTitle = Boolean(titleTranslations[toLocaleKey('uzl')]?.trim());

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{t('edit.title')}</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <MultiLangInput
            label={t('form.title')}
            value={titleTranslations}
            onChange={setTitleTranslations}
            required
          />
          <TextField
            select
            label={t('form.category')}
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            fullWidth
            size="small"
          >
            <MenuItem value="">{t('form.noCategory')}</MenuItem>
            {Object.values(TestCategory).map((cat) => (
              <MenuItem key={cat} value={cat}>{t(`categories.${cat}`)}</MenuItem>
            ))}
          </TextField>
          <MultiLangInput
            label={t('form.schoolName')}
            value={schoolNameTranslations}
            onChange={setSchoolNameTranslations}
          />
          <TextField
            select
            label={t('form.className')}
            value={className}
            onChange={(e) => setClassName(e.target.value)}
            fullWidth
            size="small"
          >
            <MenuItem value="">{t('form.selectClass')}</MenuItem>
            {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11].map((grade) => (
              <MenuItem key={grade} value={String(grade)}>
                {grade}-{t('form.classLabel')}
              </MenuItem>
            ))}
          </TextField>
          <MultiLangInput
            label={t('form.teacherName')}
            value={teacherNameTranslations}
            onChange={setTeacherNameTranslations}
          />
          <TextField
            type="date"
            label={t('form.date')}
            value={date}
            onChange={(e) => setDate(e.target.value)}
            fullWidth
            size="small"
            InputLabelProps={{ shrink: true }}
            inputProps={{ lang: dateLang }}
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isPending}>{t('common:cancel', 'Cancel')}</Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={isPending || !hasTitle}
          startIcon={isPending ? <CircularProgress size={16} /> : undefined}
        >
          {isPending ? t('edit.saving') : t('edit.save')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
