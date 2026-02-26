import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Stepper,
  Step,
  StepLabel,
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Switch,
  Typography,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { CreateAssignmentRequest, AssignmentDto } from '@/types/assignment';
import MultiLangInput from '@/features/tests/components/MultiLangInput';

interface AssignmentFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateAssignmentRequest) => void;
  assignment?: AssignmentDto | null;
  isPending: boolean;
  groups: { id: string; name: string }[];
  tests: { id: string; title: string }[];
}

const STEPS = ['selectTest', 'schedule', 'settings', 'assign'];

export default function AssignmentFormDialog({
  open,
  onClose,
  onSubmit,
  assignment,
  isPending,
  groups,
  tests,
}: AssignmentFormDialogProps) {
  const { t } = useTranslation('assignment');
  const { t: tc } = useTranslation('common');
  const [activeStep, setActiveStep] = useState(0);

  const [formData, setFormData] = useState<CreateAssignmentRequest>({
    titleTranslations: {},
    descriptionTranslations: {},
    testHistoryId: '',
    groupId: '',
    durationMinutes: 60,
    maxAttempts: 1,
    shuffleQuestions: false,
    shuffleOptions: false,
    showResults: true,
    preventTabSwitch: false,
    preventCopyPaste: false,
  });

  useEffect(() => {
    if (assignment) {
      setFormData({
        titleTranslations: assignment.titleTranslations || { uz_latn: assignment.title },
        descriptionTranslations: assignment.descriptionTranslations || (assignment.description ? { uz_latn: assignment.description } : {}),
        testHistoryId: assignment.testHistoryId,
        groupId: assignment.groupId,
        startDate: assignment.startDate || undefined,
        endDate: assignment.endDate || undefined,
        durationMinutes: assignment.durationMinutes || 60,
        maxAttempts: assignment.maxAttempts,
        shuffleQuestions: assignment.shuffleQuestions,
        shuffleOptions: assignment.shuffleOptions,
        showResults: assignment.showResults,
        preventTabSwitch: assignment.preventTabSwitch,
        preventCopyPaste: assignment.preventCopyPaste,
      });
    } else {
      setFormData({
        titleTranslations: {},
        descriptionTranslations: {},
        testHistoryId: '',
        groupId: '',
        durationMinutes: 60,
        maxAttempts: 1,
        shuffleQuestions: false,
        shuffleOptions: false,
        showResults: true,
        preventTabSwitch: false,
        preventCopyPaste: false,
      });
    }
    setActiveStep(0);
  }, [assignment, open]);

  const handleNext = () => setActiveStep((s) => Math.min(s + 1, STEPS.length - 1));
  const handleBack = () => setActiveStep((s) => Math.max(s - 1, 0));

  const handleSubmit = () => {
    onSubmit(formData);
  };

  const canProceed = () => {
    if (activeStep === 0) {
      const hasTitle = Object.values(formData.titleTranslations || {}).some(v => v?.trim());
      return !!formData.testHistoryId && hasTitle;
    }
    if (activeStep === 3) return !!formData.groupId;
    return true;
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{assignment ? t('editAssignment') : t('createAssignment')}</DialogTitle>
      <DialogContent>
        <Stepper activeStep={activeStep} sx={{ mb: 3, mt: 1 }}>
          {STEPS.map((label) => (
            <Step key={label}>
              <StepLabel>{t(`steps.${label}`)}</StepLabel>
            </Step>
          ))}
        </Stepper>

        {activeStep === 0 && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <MultiLangInput
              label={t('assignmentTitle')}
              value={formData.titleTranslations || {}}
              onChange={(v) => setFormData({ ...formData, titleTranslations: v })}
              required
            />
            <FormControl fullWidth required>
              <InputLabel>{t('selectTest')}</InputLabel>
              <Select
                value={formData.testHistoryId}
                onChange={(e) => setFormData({ ...formData, testHistoryId: e.target.value })}
                label={t('selectTest')}
              >
                {tests.map((test) => (
                  <MenuItem key={test.id} value={test.id}>{test.title}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <MultiLangInput
              label={t('description')}
              value={formData.descriptionTranslations || {}}
              onChange={(v) => setFormData({ ...formData, descriptionTranslations: v })}
            />
          </Box>
        )}

        {activeStep === 1 && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label={t('startDate')}
              type="datetime-local"
              value={formData.startDate || ''}
              onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
              fullWidth
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label={t('endDate')}
              type="datetime-local"
              value={formData.endDate || ''}
              onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
              fullWidth
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              label={t('durationMinutes')}
              type="number"
              value={formData.durationMinutes || ''}
              onChange={(e) => setFormData({ ...formData, durationMinutes: Number(e.target.value) })}
              fullWidth
              inputProps={{ min: 1, max: 600 }}
            />
          </Box>
        )}

        {activeStep === 2 && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <TextField
              label={t('maxAttempts')}
              type="number"
              value={formData.maxAttempts || 1}
              onChange={(e) => setFormData({ ...formData, maxAttempts: Number(e.target.value) })}
              fullWidth
              inputProps={{ min: 1, max: 10 }}
              sx={{ mb: 1 }}
            />
            <Typography variant="subtitle2" sx={{ mt: 1, mb: 0.5 }}>{t('antiCheat')}</Typography>
            <FormControlLabel
              control={<Switch checked={formData.shuffleQuestions} onChange={(e) => setFormData({ ...formData, shuffleQuestions: e.target.checked })} />}
              label={t('shuffleQuestions')}
            />
            <FormControlLabel
              control={<Switch checked={formData.shuffleOptions} onChange={(e) => setFormData({ ...formData, shuffleOptions: e.target.checked })} />}
              label={t('shuffleOptions')}
            />
            <FormControlLabel
              control={<Switch checked={formData.preventTabSwitch} onChange={(e) => setFormData({ ...formData, preventTabSwitch: e.target.checked })} />}
              label={t('preventTabSwitch')}
            />
            <FormControlLabel
              control={<Switch checked={formData.preventCopyPaste} onChange={(e) => setFormData({ ...formData, preventCopyPaste: e.target.checked })} />}
              label={t('preventCopyPaste')}
            />
            <FormControlLabel
              control={<Switch checked={formData.showResults} onChange={(e) => setFormData({ ...formData, showResults: e.target.checked })} />}
              label={t('showResults')}
            />
          </Box>
        )}

        {activeStep === 3 && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <FormControl fullWidth required>
              <InputLabel>{t('selectGroup')}</InputLabel>
              <Select
                value={formData.groupId}
                onChange={(e) => setFormData({ ...formData, groupId: e.target.value })}
                label={t('selectGroup')}
              >
                {groups.map((group) => (
                  <MenuItem key={group.id} value={group.id}>{group.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isPending}>{tc('cancel')}</Button>
        {activeStep > 0 && (
          <Button onClick={handleBack} disabled={isPending}>{tc('back')}</Button>
        )}
        {activeStep < STEPS.length - 1 ? (
          <Button onClick={handleNext} variant="contained" disabled={!canProceed()}>
            {tc('next')}
          </Button>
        ) : (
          <Button onClick={handleSubmit} variant="contained" disabled={isPending || !canProceed()}>
            {assignment ? tc('save') : t('createAssignment')}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
