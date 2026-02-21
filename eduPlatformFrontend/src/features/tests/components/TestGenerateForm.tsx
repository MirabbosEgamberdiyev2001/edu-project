import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
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
} from '@mui/material';
import { useSubjects } from '@/features/subjects/hooks/useSubjects';
import { useAvailableQuestions } from '../hooks/useTests';
import { useTestMutations } from '../hooks/useTestMutations';
import TopicCheckboxTree from './TopicCheckboxTree';
import DifficultySliders from './DifficultySliders';
import HeaderConfigForm from './HeaderConfigForm';
import type { GenerateTestRequest, DifficultyDistribution, HeaderConfig } from '@/types/test';
import { resolveTranslation } from '@/utils/i18nUtils';

export default function TestGenerateForm() {
  const { t } = useTranslation('test');
  const navigate = useNavigate();
  const { generate } = useTestMutations();

  const [activeStep, setActiveStep] = useState(0);

  // Form state
  const [title, setTitle] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [topicIds, setTopicIds] = useState<string[]>([]);
  const [questionCount, setQuestionCount] = useState('20');
  const [variantCount, setVariantCount] = useState('4');
  const [difficulty, setDifficulty] = useState<DifficultyDistribution>({ easy: 30, medium: 50, hard: 20 });
  const [shuffleQuestions, setShuffleQuestions] = useState(true);
  const [shuffleOptions, setShuffleOptions] = useState(true);
  const [headerConfig, setHeaderConfig] = useState<HeaderConfig>({});

  const parsedQuestionCount = parseInt(questionCount) || 0;
  const parsedVariantCount = parseInt(variantCount) || 0;

  const { data: subjects } = useSubjects({ size: 100 });
  const { data: available } = useAvailableQuestions(topicIds);

  const steps = [
    t('form.step1Title'),
    t('form.step2Title'),
    t('form.step3Title'),
    t('form.step4Title'),
  ];

  const canNext = () => {
    switch (activeStep) {
      case 0: return !!subjectId && topicIds.length > 0 && title.trim().length > 0;
      case 1: return parsedQuestionCount > 0 && parsedVariantCount > 0
        && (!available || (available.totalAvailable > 0 && parsedQuestionCount <= available.maxPossibleQuestions));
      case 2: return true;
      case 3: return true;
      default: return false;
    }
  };

  const handleGenerate = () => {
    const request: GenerateTestRequest = {
      title,
      subjectId,
      topicIds,
      questionCount: parsedQuestionCount,
      variantCount: parsedVariantCount,
      difficultyDistribution: difficulty,
      shuffleQuestions,
      shuffleOptions,
      headerConfig,
    };
    generate.mutate(request, {
      onSuccess: () => navigate('/tests'),
    });
  };

  const selectedSubject = subjects?.content?.find(s => s.id === subjectId);

  const renderStep = () => {
    switch (activeStep) {
      case 0:
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <TextField
              label={t('form.title')}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder={t('form.titlePlaceholder')}
              fullWidth
              required
            />
            <TextField
              select
              label={t('form.selectSubject')}
              value={subjectId}
              onChange={(e) => {
                setSubjectId(e.target.value);
                setTopicIds([]);
                setQuestionCount('20');
                setVariantCount('4');
                setDifficulty({ easy: 30, medium: 50, hard: 20 });
                setHeaderConfig({});
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
            {subjectId && (
              <Box>
                <Typography variant="subtitle2" gutterBottom>{t('form.selectTopics')}</Typography>
                <Paper variant="outlined" sx={{ p: 2, maxHeight: 300, overflow: 'auto' }}>
                  <TopicCheckboxTree
                    subjectId={subjectId}
                    selected={topicIds}
                    onChange={setTopicIds}
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
                {t('errors.noActiveQuestions')}
                <Typography variant="caption" display="block" sx={{ mt: 0.5 }}>
                  {t('errors.noActiveExplain')}
                </Typography>
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
              value={questionCount}
              onChange={(e) => setQuestionCount(e.target.value)}
              onBlur={() => {
                const n = parseInt(questionCount);
                setQuestionCount(String(Math.max(1, isNaN(n) ? 1 : n)));
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
              value={variantCount}
              onChange={(e) => setVariantCount(e.target.value)}
              onBlur={() => {
                const n = parseInt(variantCount);
                setVariantCount(String(Math.max(1, Math.min(10, isNaN(n) ? 1 : n))));
              }}
              inputProps={{ min: 1, max: 10 }}
              fullWidth
            />
            <DifficultySliders value={difficulty} onChange={setDifficulty} />
          </Box>
        );

      case 2:
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Typography variant="subtitle2">{t('form.shuffle')}</Typography>
            <FormControlLabel
              control={<Switch checked={shuffleQuestions} onChange={(e) => setShuffleQuestions(e.target.checked)} />}
              label={t('form.shuffleQuestions')}
            />
            <FormControlLabel
              control={<Switch checked={shuffleOptions} onChange={(e) => setShuffleOptions(e.target.checked)} />}
              label={t('form.shuffleOptions')}
            />
            <Typography variant="subtitle2" sx={{ mt: 2 }}>{t('form.header')}</Typography>
            <HeaderConfigForm value={headerConfig} onChange={setHeaderConfig} />
          </Box>
        );

      case 3:
        return (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Typography variant="subtitle2">{t('form.preview')}</Typography>
            <Paper variant="outlined" sx={{ p: 2 }}>
              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1.5 }}>
                <Box>
                  <Typography variant="caption" color="text.secondary">{t('form.title')}</Typography>
                  <Typography variant="body2">{title}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">{t('form.selectSubject')}</Typography>
                  <Typography variant="body2">
                    {selectedSubject ? (resolveTranslation(selectedSubject.nameTranslations) || selectedSubject.name) : '-'}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">{t('form.selectTopics')}</Typography>
                  <Typography variant="body2">{topicIds.length} {t('common:topics', 'topics')}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">{t('form.questionCount')}</Typography>
                  <Typography variant="body2">{parsedQuestionCount}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">{t('form.variantCount')}</Typography>
                  <Typography variant="body2">{parsedVariantCount}</Typography>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">{t('form.difficulty')}</Typography>
                  <Box sx={{ display: 'flex', gap: 0.5 }}>
                    <Chip size="small" label={`${t('form.easy')}: ${difficulty.easy}%`} color="success" />
                    <Chip size="small" label={`${t('form.medium')}: ${difficulty.medium}%`} color="warning" />
                    <Chip size="small" label={`${t('form.hard')}: ${difficulty.hard}%`} color="error" />
                  </Box>
                </Box>
                <Box>
                  <Typography variant="caption" color="text.secondary">{t('form.shuffle')}</Typography>
                  <Typography variant="body2">
                    {shuffleQuestions ? t('form.shuffleQuestions') : '-'}
                    {shuffleOptions ? `, ${t('form.shuffleOptions')}` : ''}
                  </Typography>
                </Box>
              </Box>
            </Paper>
          </Box>
        );

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
          onClick={() => setActiveStep((prev) => prev - 1)}
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
            onClick={handleGenerate}
            disabled={generate.isPending}
            startIcon={generate.isPending ? <CircularProgress size={20} /> : undefined}
          >
            {generate.isPending ? t('form.generating') : t('form.generate')}
          </Button>
        )}
      </Box>
    </Box>
  );
}
