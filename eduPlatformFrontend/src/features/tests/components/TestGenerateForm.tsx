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
import { useSubjects } from '@/features/subjects/hooks/useSubjects';
import { useAvailableQuestions } from '../hooks/useTests';
import { useTestMutations } from '../hooks/useTestMutations';
import { useTestPreview } from '../hooks/useTestPreview';
import { useQuestionsByIds } from '@/features/questions/hooks/useQuestions';
import { useFormPersistence } from '../hooks/useFormPersistence';
import TopicCheckboxTree from './TopicCheckboxTree';
import DifficultySliders from './DifficultySliders';
import HeaderConfigForm from './HeaderConfigForm';
import MultiLangInput from './MultiLangInput';
import { TestCategory } from '@/types/test';
import type { GenerateTestRequest, DifficultyDistribution, HeaderConfig } from '@/types/test';
import { resolveTranslation, toLocaleKey } from '@/utils/i18nUtils';
import { MathText } from '@/components/math';

const GRADES = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];

interface FormState {
  titleTranslations: Record<string, string>;
  category: TestCategory | '';
  subjectId: string;
  gradeLevel: number | null;
  topicIds: string[];
  questionCount: string;
  variantCount: string;
  difficulty: DifficultyDistribution;
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
  questionCount: '20',
  variantCount: '4',
  difficulty: { easy: 30, medium: 50, hard: 20 },
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

export default function TestGenerateForm() {
  const { t } = useTranslation('test');
  const { t: tSubject } = useTranslation('subject');
  const { t: tQ } = useTranslation('question');
  const navigate = useNavigate();
  const { generate } = useTestMutations();
  const preview = useTestPreview();

  const [activeStep, setActiveStep] = useState(0);
  const [randomSeed, setRandomSeed] = useState<number | null>(null);
  const [selectedVariant, setSelectedVariant] = useState(0);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const [form, setForm, clearForm] = useFormPersistence<FormState>('test-generate-form', INITIAL_FORM);

  const updateForm = useCallback(<K extends keyof FormState>(key: K, val: FormState[K]) => {
    setForm((prev) => ({ ...prev, [key]: val }));
  }, [setForm]);

  const parsedQuestionCount = parseInt(form.questionCount) || 0;
  const parsedVariantCount = parseInt(form.variantCount) || 0;

  const { data: subjects } = useSubjects({ size: 100 });
  const { data: available } = useAvailableQuestions(form.topicIds, form.subjectId || undefined);

  const steps = [
    t('form.step1Title'),
    t('form.step2Title'),
    t('form.step3Title'),
    t('preview.title'),
  ];

  // Collect unique question IDs from preview
  const uniqueQuestionIds = useMemo(() => {
    if (!preview.data?.variants) return [];
    const ids = new Set<string>();
    preview.data.variants.forEach((v) => v.questionIds.forEach((qid) => ids.add(qid)));
    return Array.from(ids);
  }, [preview.data?.variants]);

  const { data: questionsData } = useQuestionsByIds(uniqueQuestionIds);

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
      ...(form.gradeLevel != null && { gradeLevel: form.gradeLevel }),
      subjectId: form.subjectId,
      topicIds: form.topicIds,
      questionCount: parsedQuestionCount,
      variantCount: parsedVariantCount,
      difficultyDistribution: form.difficulty,
      shuffleQuestions: form.shuffleQuestions,
      shuffleOptions: form.shuffleOptions,
      headerConfig: form.headerConfig,
      ...(seed && { randomSeed: seed }),
    };
  }

  const canNext = () => {
    switch (activeStep) {
      case 0: return !!form.subjectId && Boolean(form.titleTranslations?.[toLocaleKey('uzl')]?.trim());
      case 1: return parsedQuestionCount > 0 && parsedVariantCount > 0
        && (!available || (available.totalAvailable > 0 && parsedQuestionCount <= available.maxPossibleQuestions));
      case 2: return true;
      case 3: return !!preview.data && !preview.isPending;
      default: return false;
    }
  };

  const handleBack = () => {
    if (activeStep === 3) {
      // Clear preview cache when going back
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
                    <MathText
                      text={resolveTranslation(question.questionTextTranslations) || question.questionText || ''}
                      variant="body2"
                      sx={{ fontWeight: 600, wordBreak: 'break-word' }}
                    />
                    {reorderedOpts.length > 0 && (
                      <Box sx={{ mt: 0.5, pl: 1 }}>
                        {reorderedOpts.map((opt, oi) => {
                          const optText = typeof opt.text === 'object' && opt.text !== null
                            ? resolveTranslation(opt.text as Record<string, string>) || ''
                            : (opt.text || '');
                          return (
                            <MathText
                              key={opt.id || oi}
                              text={`${String.fromCharCode(65 + oi)}) ${optText}${opt.isCorrect ? ' ✓' : ''}`}
                              variant="caption"
                              sx={{
                                display: 'block',
                                color: opt.isCorrect ? 'success.main' : 'text.secondary',
                                fontWeight: opt.isCorrect ? 700 : 400,
                              }}
                            />
                          );
                        })}
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
                  questionCount: '20',
                  variantCount: '4',
                  difficulty: { easy: 30, medium: 50, hard: 20 },
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
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <Typography variant="subtitle2">{tSubject('form.gradeLevel')}</Typography>
                  <Typography variant="caption" color="text.disabled">({t('form.optional', 'ixtiyoriy')})</Typography>
                </Box>
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
                      }))}
                      sx={{ minWidth: 40 }}
                    />
                  ))}
                </Box>
              </Box>
            )}
            {form.subjectId && (
              <Box>
                <Typography variant="subtitle2" gutterBottom>{t('form.selectTopics')}</Typography>
                <Paper variant="outlined" sx={{ p: 2, maxHeight: 300, overflow: 'auto' }}>
                  <TopicCheckboxTree
                    subjectId={form.subjectId}
                    gradeLevel={form.gradeLevel}
                    selected={form.topicIds}
                    onChange={(ids) => updateForm('topicIds', ids)}
                  />
                </Paper>
              </Box>
            )}
          </Box>
        );

      case 1:
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            {available && available.totalAvailable === 0 && (
              <Alert severity="warning">
                {t('errors.noQuestions')}
              </Alert>
            )}
            {available && available.totalAvailable > 0 && (
              <Alert severity="info">
                {t('form.totalAvailable')}: {available.totalAvailable}
                {' ('}
                {t('form.easy')}: {available.easyCount},
                {' '}{t('form.medium')}: {available.mediumCount},
                {' '}{t('form.hard')}: {available.hardCount}
                {')'}
              </Alert>
            )}
            <TextField
              type="number"
              label={t('form.questionCount')}
              value={form.questionCount}
              onChange={(e) => updateForm('questionCount', e.target.value)}
              onBlur={() => {
                const n = parseInt(form.questionCount);
                updateForm('questionCount', String(Math.max(1, isNaN(n) ? 1 : n)));
              }}
              inputProps={{ min: 1, max: available?.maxPossibleQuestions || 100 }}
              fullWidth
              error={!!available && parsedQuestionCount > available.maxPossibleQuestions}
              helperText={available && parsedQuestionCount > available.maxPossibleQuestions
                ? t('errors.noQuestions')
                : undefined
              }
            />
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
            <DifficultySliders value={form.difficulty} onChange={(v) => updateForm('difficulty', v)} />
          </Box>
        );

      case 2:
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
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
    </Box>
  );
}
