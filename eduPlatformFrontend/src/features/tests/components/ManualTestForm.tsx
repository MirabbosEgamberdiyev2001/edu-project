import { useState, useMemo, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';
import {
  Box,
  Stepper,
  Step,
  StepLabel,
  Button,
  Typography,
  TextField,
  MenuItem,
  FormControlLabel,
  Switch,
  Paper,
  Chip,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import { useQueryClient } from '@tanstack/react-query';
import { useSubjects } from '@/features/subjects/hooks/useSubjects';
import { useTestMutations } from '../hooks/useTestMutations';
import { useTestPreview } from '../hooks/useTestPreview';
import { useQuestionMutations } from '@/features/questions/hooks/useQuestionMutations';
import { useQuestionsByIds } from '@/features/questions/hooks/useQuestions';
import { useFormPersistence } from '../hooks/useFormPersistence';
import TopicCheckboxTree from './TopicCheckboxTree';
import HeaderConfigForm from './HeaderConfigForm';
import MultiLangInput from './MultiLangInput';
import QuestionSelector from './QuestionSelector';
import QuestionFormDialog from '@/features/questions/components/QuestionFormDialog';
import { TestCategory } from '@/types/test';
import type { GenerateTestRequest, HeaderConfig } from '@/types/test';
import type { CreateQuestionRequest } from '@/types/question';
import { resolveTranslation, toLocaleKey } from '@/utils/i18nUtils';

const GRADES = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];

interface FormState {
  titleTranslations: Record<string, string>;
  category: TestCategory | '';
  subjectId: string;
  gradeLevel: number | null;
  topicIds: string[];
  selectedQuestionIds: string[];
  variantCount: string;
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  headerConfig: HeaderConfig;
}

const INITIAL_FORM: FormState = {
  titleTranslations: {},
  category: '',
  subjectId: '',
  gradeLevel: null,
  topicIds: [],
  selectedQuestionIds: [],
  variantCount: '4',
  shuffleQuestions: true,
  shuffleOptions: true,
  headerConfig: {},
};

function reorderOptions(
  options: Array<{ id?: string; text?: string; isCorrect?: boolean }>,
  optionsOrder: string[] | null,
) {
  if (!optionsOrder?.length) return options;
  const optionsMap = new Map(options.map((o) => [o.id, o]));
  return optionsOrder.map((id) => optionsMap.get(id)).filter(Boolean) as typeof options;
}

export default function ManualTestForm() {
  const { t } = useTranslation('test');
  const { t: tSubject } = useTranslation('subject');
  const { t: tQ } = useTranslation('question');
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { generate } = useTestMutations();
  const preview = useTestPreview();
  const { create: createQuestion } = useQuestionMutations();

  const [activeStep, setActiveStep] = useState(0);
  const [questionDialogOpen, setQuestionDialogOpen] = useState(false);
  const [randomSeed, setRandomSeed] = useState<number | null>(null);
  const [selectedVariant, setSelectedVariant] = useState(0);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const [form, setForm, clearForm] = useFormPersistence<FormState>('test-manual-form', INITIAL_FORM);

  const updateForm = useCallback(<K extends keyof FormState>(key: K, val: FormState[K]) => {
    setForm((prev) => ({ ...prev, [key]: val }));
  }, [setForm]);

  const parsedVariantCount = parseInt(form.variantCount) || 0;

  const { data: subjects } = useSubjects({ size: 100 });

  // Collect unique question IDs from preview
  const uniqueQuestionIds = useMemo(() => {
    if (!preview.data?.variants) return [];
    const ids = new Set<string>();
    preview.data.variants.forEach((v) => v.questionIds.forEach((qid) => ids.add(qid)));
    return Array.from(ids);
  }, [preview.data?.variants]);

  const { data: questionsData } = useQuestionsByIds(uniqueQuestionIds);

  const steps = [
    t('manual.step1Title'),
    t('manual.step2Title'),
    t('manual.step3Title'),
    t('preview.title'),
  ];

  // Trigger preview when entering step 3
  useEffect(() => {
    if (activeStep === 3 && !randomSeed) {
      const seed = Date.now();
      setRandomSeed(seed);
      const request = buildRequest(seed);
      preview.mutate(request);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeStep]);

  function buildRequest(seed?: number): GenerateTestRequest {
    const translations = form.titleTranslations || {};
    const resolvedTitle = resolveTranslation(translations) || '';
    return {
      title: resolvedTitle,
      titleTranslations: translations,
      ...(form.category && { category: form.category as TestCategory }),
      subjectId: form.subjectId,
      topicIds: form.topicIds,
      questionIds: form.selectedQuestionIds,
      variantCount: parsedVariantCount,
      shuffleQuestions: form.shuffleQuestions,
      shuffleOptions: form.shuffleOptions,
      headerConfig: form.headerConfig,
      ...(seed && { randomSeed: seed }),
    };
  }

  const canNext = () => {
    switch (activeStep) {
      case 0: return !!form.subjectId && Boolean(form.titleTranslations?.[toLocaleKey('uzl')]?.trim());
      case 1: return form.selectedQuestionIds.length > 0;
      case 2: return true;
      case 3: return !!preview.data && !preview.isPending;
      default: return false;
    }
  };

  const handleBack = () => {
    if (activeStep === 3) {
      preview.reset();
      setRandomSeed(null);
      setSelectedVariant(0);
    }
    setActiveStep((prev) => prev - 1);
  };

  const handleGenerate = () => {
    if (!randomSeed) return;
    const request = buildRequest(randomSeed);
    generate.mutate(request, {
      onSuccess: () => {
        clearForm();
        navigate('/tests');
      },
    });
    setConfirmOpen(false);
  };

  const selectedSubject = subjects?.content?.find((s) => s.id === form.subjectId);

  const renderPreviewStep = () => {
    if (preview.isPending) {
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 4, gap: 2 }}>
          <CircularProgress />
          <Typography color="text.secondary">{t('preview.loading')}</Typography>
        </Box>
      );
    }

    if (preview.isError) {
      const axiosErr = preview.error as AxiosError<ApiError>;
      const errMsg = axiosErr?.response?.data?.message || t('preview.error');
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Alert severity="error">{errMsg}</Alert>
          <Button
            variant="outlined"
            onClick={() => {
              const seed = Date.now();
              setRandomSeed(seed);
              preview.mutate(buildRequest(seed));
            }}
          >
            {t('common:retry', 'Retry')}
          </Button>
        </Box>
      );
    }

    if (!preview.data || !preview.data.variants?.length) {
      return <Alert severity="warning">{t('preview.noQuestions')}</Alert>;
    }

    const previewData = preview.data;
    const currentVariant = previewData.variants[selectedVariant];

    if (!currentVariant) return null;

    const questionsMap = new Map(questionsData?.map((q) => [q.id, q]) ?? []);

    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="subtitle2">{t('preview.title')}</Typography>
          <Chip
            label={t('preview.questionsCount', { count: previewData.questionCount })}
            size="small"
            variant="outlined"
          />
        </Box>

        {/* Difficulty distribution */}
        {previewData.difficultyDistribution && (
          <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
            {Object.entries(previewData.difficultyDistribution).map(([key, val]) => (
              <Chip
                key={key}
                size="small"
                label={`${t(`form.${key.toLowerCase()}`, key)}: ${val}`}
                color={key === 'easy' ? 'success' : key === 'medium' ? 'warning' : 'error'}
              />
            ))}
          </Box>
        )}

        {/* Variant tabs */}
        {previewData.variants.length > 1 && (
          <Tabs
            value={selectedVariant}
            onChange={(_, v) => setSelectedVariant(v)}
            variant="scrollable"
            scrollButtons="auto"
          >
            {previewData.variants.map((v, i) => (
              <Tab key={v.code} label={`${t('preview.variant')} ${v.code}`} value={i} />
            ))}
          </Tabs>
        )}

        {/* Questions for current variant */}
        <Box sx={{ maxHeight: 500, overflow: 'auto' }}>
          {currentVariant.questionIds.map((qId, idx) => {
            const question = questionsMap.get(qId);
            if (!question) return null;

            const rawOptions = Array.isArray(question.options)
              ? (question.options as Array<{ id?: string; text?: string; isCorrect?: boolean }>)
              : [];
            const optionsOrderForQ = currentVariant.optionsOrder?.[idx] ?? null;
            const reorderedOpts = reorderOptions(rawOptions, optionsOrderForQ);

            return (
              <Paper key={`${qId}-${idx}`} variant="outlined" sx={{ p: 2, mb: 1 }}>
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
                  <Typography variant="caption" color="text.disabled" sx={{ minWidth: 28, pt: 0.3 }}>
                    {idx + 1}.
                  </Typography>
                  <Box sx={{ flex: 1 }}>
                    <Typography variant="body2" sx={{ fontWeight: 600, wordBreak: 'break-word' }}>
                      {resolveTranslation(question.questionTextTranslations) || question.questionText}
                    </Typography>
                    {reorderedOpts.length > 0 && (
                      <Box sx={{ mt: 0.5, pl: 1 }}>
                        {reorderedOpts.map((opt, oi) => (
                          <Typography
                            key={opt.id || oi}
                            variant="caption"
                            sx={{
                              display: 'block',
                              color: opt.isCorrect ? 'success.main' : 'text.secondary',
                              fontWeight: opt.isCorrect ? 700 : 400,
                            }}
                          >
                            {String.fromCharCode(65 + oi)}) {opt.text}
                            {opt.isCorrect && ' ✓'}
                          </Typography>
                        ))}
                      </Box>
                    )}
                  </Box>
                  <Chip
                    size="small"
                    label={tQ(`difficulties.${question.difficulty}`)}
                    color={question.difficulty === 'EASY' ? 'success' : question.difficulty === 'MEDIUM' ? 'warning' : 'error'}
                    sx={{ flexShrink: 0 }}
                  />
                </Box>
              </Paper>
            );
          })}
        </Box>
      </Box>
    );
  };

  const renderStep = () => {
    switch (activeStep) {
      case 0:
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <MultiLangInput
              label={t('form.title')}
              value={form.titleTranslations || {}}
              onChange={(v) => updateForm('titleTranslations', v)}
              placeholder={t('form.titlePlaceholder')}
              required
            />
            <TextField
              select
              label={t('form.category')}
              value={form.category}
              onChange={(e) => updateForm('category', e.target.value as TestCategory)}
              fullWidth
            >
              <MenuItem value="">{t('form.noCategory')}</MenuItem>
              {Object.values(TestCategory).map((cat) => (
                <MenuItem key={cat} value={cat}>{t(`categories.${cat}`)}</MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label={t('form.selectSubject')}
              value={form.subjectId}
              onChange={(e) => {
                setForm((prev) => ({
                  ...prev,
                  subjectId: e.target.value,
                  gradeLevel: null,
                  topicIds: [],
                  selectedQuestionIds: [],
                  variantCount: '4',
                  headerConfig: {},
                  titleTranslations: prev.titleTranslations,
                }));
              }}
              fullWidth
              required
            >
              {subjects?.content?.map((s) => (
                <MenuItem key={s.id} value={s.id}>
                  {resolveTranslation(s.nameTranslations) || s.name}
                </MenuItem>
              ))}
            </TextField>
            {form.subjectId && (
              <Box>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>
                  {tSubject('form.gradeLevel')}
                  <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                    ({t('common:optional', 'ixtiyoriy')})
                  </Typography>
                </Typography>
                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                  {GRADES.map((grade) => (
                    <Chip
                      key={grade}
                      label={`${grade}`}
                      color={form.gradeLevel === grade ? 'primary' : 'default'}
                      variant={form.gradeLevel === grade ? 'filled' : 'outlined'}
                      onClick={() => setForm((prev) => ({
                        ...prev,
                        gradeLevel: prev.gradeLevel === grade ? null : grade,
                        topicIds: [],
                        selectedQuestionIds: [],
                      }))}
                      sx={{ minWidth: 40 }}
                    />
                  ))}
                </Box>
              </Box>
            )}
            {form.subjectId && (
              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  {t('form.selectTopics')}
                  <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                    ({t('common:optional', 'ixtiyoriy')})
                  </Typography>
                </Typography>
                <Paper variant="outlined" sx={{ p: 2, maxHeight: 300, overflow: 'auto' }}>
                  <TopicCheckboxTree
                    subjectId={form.subjectId}
                    gradeLevel={form.gradeLevel}
                    selected={form.topicIds}
                    onChange={(ids) => setForm((prev) => ({ ...prev, topicIds: ids, selectedQuestionIds: [] }))}
                  />
                </Paper>
              </Box>
            )}
          </Box>
        );

      case 1:
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <QuestionSelector
              subjectId={form.subjectId}
              topicIds={form.topicIds}
              selectedIds={form.selectedQuestionIds}
              onSelectionChange={(ids) => updateForm('selectedQuestionIds', ids)}
              onCreateQuestion={() => setQuestionDialogOpen(true)}
            />
          </Box>
        );

      case 2:
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <TextField
              type="number"
              label={t('form.variantCount')}
              value={form.variantCount}
              onChange={(e) => updateForm('variantCount', e.target.value)}
              onBlur={() => {
                const n = parseInt(form.variantCount);
                updateForm('variantCount', String(Math.max(1, Math.min(10, isNaN(n) ? 1 : n))));
              }}
              inputProps={{ min: 1, max: 10 }}
              fullWidth
            />
            <Typography variant="subtitle2">{t('form.shuffle')}</Typography>
            <FormControlLabel
              control={<Switch checked={form.shuffleQuestions} onChange={(e) => updateForm('shuffleQuestions', e.target.checked)} />}
              label={t('form.shuffleQuestions')}
            />
            <FormControlLabel
              control={<Switch checked={form.shuffleOptions} onChange={(e) => updateForm('shuffleOptions', e.target.checked)} />}
              label={t('form.shuffleOptions')}
            />
            <Typography variant="subtitle2" sx={{ mt: 2 }}>{t('form.header')}</Typography>
            <HeaderConfigForm value={form.headerConfig} onChange={(v) => updateForm('headerConfig', v)} />
          </Box>
        );

      case 3:
        return renderPreviewStep();

      default:
        return null;
    }
  };

  return (
    <Box>
      <Stepper activeStep={activeStep} sx={{ mb: 4 }}>
        {steps.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      <Paper sx={{ p: 3, mb: 3 }}>
        {renderStep()}
      </Paper>

      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Button
          disabled={activeStep === 0}
          onClick={handleBack}
        >
          {t('common:back')}
        </Button>
        {activeStep < steps.length - 1 ? (
          <Button
            variant="contained"
            disabled={!canNext()}
            onClick={() => setActiveStep((prev) => prev + 1)}
          >
            {t('common:next')}
          </Button>
        ) : (
          <Button
            variant="contained"
            onClick={() => setConfirmOpen(true)}
            disabled={!canNext() || generate.isPending}
            startIcon={generate.isPending ? <CircularProgress size={20} /> : undefined}
          >
            {generate.isPending ? t('form.generating') : t('preview.confirmCreate')}
          </Button>
        )}
      </Box>

      {/* Confirm Dialog */}
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle>{t('preview.confirmTitle')}</DialogTitle>
        <DialogContent>
          <Typography>{t('preview.confirmDesc')}</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>{t('common:cancel', 'Cancel')}</Button>
          <Button
            variant="contained"
            onClick={handleGenerate}
            disabled={generate.isPending}
            startIcon={generate.isPending ? <CircularProgress size={16} /> : undefined}
          >
            {generate.isPending ? t('form.generating') : t('preview.confirmCreate')}
          </Button>
        </DialogActions>
      </Dialog>

      <QuestionFormDialog
        open={questionDialogOpen}
        onClose={() => setQuestionDialogOpen(false)}
        onSubmit={(data) => {
          createQuestion.mutate(data as CreateQuestionRequest, {
            onSuccess: () => {
              setQuestionDialogOpen(false);
              queryClient.invalidateQueries({ queryKey: ['questions'] });
              queryClient.invalidateQueries({ queryKey: ['tests', 'questions-selection'] });
            },
          });
        }}
        isPending={createQuestion.isPending}
        defaultSubjectId={form.subjectId}
        defaultTopicId={form.topicIds[0]}
      />
    </Box>
  );
}
