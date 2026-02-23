import React, { useEffect, useState, useMemo } from 'react';
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
  Typography,
  Tabs,
  Tab,
  Alert,
  Stepper,
  Step,
  StepLabel,
  IconButton,
  Radio,
  RadioGroup,
  FormControlLabel,
  Checkbox,
  Chip,
  Paper,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import { useTranslation } from 'react-i18next';
import type { QuestionDto, CreateQuestionRequest, UpdateQuestionRequest } from '@/types/question';
import { QuestionType, Difficulty } from '@/types/question';
import { useSubjects } from '@/features/subjects/hooks/useSubjects';
import { useTopicTree } from '@/features/topics/hooks/useTopicTree';
import { useTopicMutations } from '@/features/topics/hooks/useTopicMutations';
import TopicFormDialog from '@/features/topics/components/TopicFormDialog';
import { resolveTranslation } from '@/utils/i18nUtils';
import { SUPPORTED_LANGUAGES, LANGUAGE_LABELS } from '@/config';
import type { TopicTreeDto, CreateTopicRequest } from '@/types/topic';

interface QuestionFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateQuestionRequest | UpdateQuestionRequest) => void;
  question?: QuestionDto | null;
  isPending: boolean;
  defaultSubjectId?: string;
  defaultTopicId?: string;
}

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

const QUESTION_TYPES = Object.values(QuestionType);
const DIFFICULTY_OPTIONS = Object.values(Difficulty);

interface McqOption {
  text: Record<string, string>;
  isCorrect: boolean;
}

interface FlatTopic {
  id: string;
  name: string;
  nameTranslations: Record<string, string> | null;
  depth: number;
}

function flattenTopicTree(nodes: TopicTreeDto[], depth = 0): FlatTopic[] {
  const result: FlatTopic[] = [];
  for (const node of nodes) {
    result.push({
      id: node.id,
      name: node.name,
      nameTranslations: node.nameTranslations,
      depth,
    });
    if (node.children && node.children.length > 0) {
      result.push(...flattenTopicTree(node.children, depth + 1));
    }
  }
  return result;
}

const STEP_LABELS = [
  'form.stepSubjectTopic',
  'form.stepType',
  'form.stepQuestion',
  'form.stepOptions',
  'form.stepSettings',
  'form.stepProof',
];

export default function QuestionFormDialog({
  open,
  onClose,
  onSubmit,
  question,
  isPending,
  defaultSubjectId,
  defaultTopicId,
}: QuestionFormDialogProps) {
  const { t } = useTranslation('question');
  const { t: tTopic } = useTranslation('topic');
  const isEdit = Boolean(question);

  // Stepper
  const [activeStep, setActiveStep] = useState(0);

  // Inline topic creation
  const [topicFormOpen, setTopicFormOpen] = useState(false);

  // Step 1: Subject & Topic & Grade
  const [subjectId, setSubjectId] = useState('');
  const [gradeLevel, setGradeLevel] = useState<number | null>(null);
  const [topicId, setTopicId] = useState('');

  // Step 2: Question Type
  const [questionType, setQuestionType] = useState<QuestionType | ''>('');

  // Step 3: Question Text (language tabs)
  const [questionLangTab, setQuestionLangTab] = useState(0);
  const [questionText, setQuestionText] = useState<Record<string, string>>({});

  // Step 4: Answer Options
  const [mcqOptions, setMcqOptions] = useState<McqOption[]>([
    { text: {}, isCorrect: false },
    { text: {}, isCorrect: false },
  ]);
  const [correctAnswer, setCorrectAnswer] = useState<Record<string, string>>({});
  const [optionsLangTab, setOptionsLangTab] = useState(0);
  const [trueFalseAnswer, setTrueFalseAnswer] = useState('');

  // Step 5: Settings
  const [difficulty, setDifficulty] = useState<Difficulty>(Difficulty.MEDIUM);
  const [points, setPoints] = useState(1);
  const [timeLimitSeconds, setTimeLimitSeconds] = useState<string>('');

  // Step 6: Proof (language tabs)
  const [proofLangTab, setProofLangTab] = useState(0);
  const [proof, setProof] = useState<Record<string, string>>({});
  const [changeReason, setChangeReason] = useState('');

  // Load subjects
  const { data: subjectsData } = useSubjects({ size: 100 });
  const subjects = subjectsData?.content || [];

  // Load topics for selected subject + grade
  const { data: topicTree } = useTopicTree(subjectId || undefined, gradeLevel);
  const { create: createTopic } = useTopicMutations(subjectId || undefined);

  const flatTopics = useMemo(() => {
    if (!topicTree) return [];
    return flattenTopicTree(topicTree);
  }, [topicTree]);

  // Reset form when dialog opens
  useEffect(() => {
    if (open) {
      if (question) {
        // Edit mode: populate from question
        setSubjectId(question.subjectId || '');
        setTopicId(question.topicId || '');
        setQuestionType(question.questionType || '');
        setQuestionText(
          question.questionTextTranslations
            ? { ...question.questionTextTranslations }
            : {},
        );
        setDifficulty(question.difficulty || Difficulty.MEDIUM);
        setPoints(question.points || 1);
        setTimeLimitSeconds(
          question.timeLimitSeconds != null
            ? String(question.timeLimitSeconds)
            : '',
        );
        setProof(
          question.proofTranslations ? { ...question.proofTranslations } : {},
        );
        setChangeReason('');

        // Populate answer options based on type
        if (
          question.questionType === QuestionType.MCQ_SINGLE ||
          question.questionType === QuestionType.MCQ_MULTI
        ) {
          const opts = question.options as Array<Record<string, unknown>> | null;
          if (Array.isArray(opts) && opts.length > 0) {
            setMcqOptions(opts.map((o) => ({
              text: (o.textTranslations as Record<string, string>) || (typeof o.text === 'string' ? { uz_latn: o.text } : (o.text as Record<string, string>) || {}),
              isCorrect: Boolean(o.isCorrect),
            })));
          } else {
            setMcqOptions([
              { text: {}, isCorrect: false },
              { text: {}, isCorrect: false },
            ]);
          }
          setCorrectAnswer({});
          setTrueFalseAnswer('');
        } else if (question.questionType === QuestionType.TRUE_FALSE) {
          setTrueFalseAnswer(String(question.correctAnswer || ''));
          setMcqOptions([
            { text: {}, isCorrect: false },
            { text: {}, isCorrect: false },
          ]);
          setCorrectAnswer({});
        } else {
          if (typeof question.correctAnswer === 'object' && question.correctAnswer !== null) {
            setCorrectAnswer(question.correctAnswer as Record<string, string>);
          } else {
            setCorrectAnswer(question.correctAnswer != null ? { uz_latn: String(question.correctAnswer) } : {});
          }
          setTrueFalseAnswer('');
          setMcqOptions([
            { text: {}, isCorrect: false },
            { text: {}, isCorrect: false },
          ]);
        }
      } else {
        // Create mode: start fresh (use defaults if provided)
        setSubjectId(defaultSubjectId || '');
        setGradeLevel(null);
        setTopicId(defaultTopicId || '');
        setQuestionType('');
        setQuestionText({});
        setMcqOptions([
          { text: {}, isCorrect: false },
          { text: {}, isCorrect: false },
        ]);
        setCorrectAnswer({});
        setTrueFalseAnswer('');
        setDifficulty(Difficulty.MEDIUM);
        setPoints(1);
        setTimeLimitSeconds('');
        setProof({});
        setChangeReason('');
      }
      setActiveStep(0);
      setQuestionLangTab(0);
      setOptionsLangTab(0);
      setProofLangTab(0);
    }
  }, [open, question, defaultSubjectId, defaultTopicId]);

  // Clear grade+topic when subject changes (only in create mode)
  useEffect(() => {
    if (!isEdit) {
      setGradeLevel(null);
      setTopicId('');
    }
  }, [subjectId, isEdit]);

  // Clear topic when grade changes (only in create mode)
  useEffect(() => {
    if (!isEdit) {
      setTopicId('');
    }
  }, [gradeLevel, isEdit]);

  // -- Helpers --

  function cleanMap(map: Record<string, string>): Record<string, string> {
    const result: Record<string, string> = {};
    Object.entries(map).forEach(([k, v]) => {
      if (v.trim()) result[k] = v.trim();
    });
    return result;
  }

  function isLangFilled(
    map: Record<string, string>,
    frontLang: string,
  ): boolean {
    const key = langKey(frontLang);
    return Boolean(map[key]?.trim());
  }

  // -- Step validation --

  function isStepValid(step: number): boolean {
    switch (step) {
      case 0:
        if (isEdit) return Boolean(subjectId) && Boolean(topicId);
        return Boolean(subjectId) && gradeLevel !== null && Boolean(topicId);
      case 1:
        return Boolean(questionType);
      case 2:
        return Boolean(questionText['uz_latn']?.trim());
      case 3:
        return isAnswerStepValid();
      case 4:
        return Boolean(difficulty) && points > 0;
      case 5:
        return true; // proof is optional
      default:
        return true;
    }
  }

  function isAnswerStepValid(): boolean {
    if (!questionType) return false;
    switch (questionType) {
      case QuestionType.MCQ_SINGLE: {
        const hasText = mcqOptions.some((o) => o.text['uz_latn']?.trim());
        const hasCorrect = mcqOptions.filter((o) => o.isCorrect).length === 1;
        return hasText && hasCorrect;
      }
      case QuestionType.MCQ_MULTI: {
        const hasText = mcqOptions.some((o) => o.text['uz_latn']?.trim());
        const hasCorrect = mcqOptions.filter((o) => o.isCorrect).length >= 1;
        return hasText && hasCorrect;
      }
      case QuestionType.TRUE_FALSE:
        return Boolean(trueFalseAnswer);
      case QuestionType.SHORT_ANSWER:
      case QuestionType.FILL_BLANK:
        return Boolean(correctAnswer['uz_latn']?.trim());
      case QuestionType.ESSAY:
        return true; // no answer needed
      case QuestionType.MATCHING:
      case QuestionType.ORDERING:
        return Boolean(correctAnswer['uz_latn']?.trim());
      default:
        return true;
    }
  }

  // -- Submit --

  function handleSubmit() {
    const cleanQuestion = cleanMap(questionText);
    const cleanProof = cleanMap(proof);

    // Build options and correctAnswer based on type
    let finalOptions: unknown = null;
    let finalCorrectAnswer: unknown = null;

    if (
      questionType === QuestionType.MCQ_SINGLE ||
      questionType === QuestionType.MCQ_MULTI
    ) {
      const labels = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];
      const filtered = mcqOptions.filter((o) =>
        Object.values(o.text).some((v) => v.trim()),
      );
      finalOptions = filtered.map((o, i) => ({
        id: String(i + 1),
        text: cleanMap(o.text),
        isCorrect: o.isCorrect,
      }));
      const correctLabels = filtered
        .map((o, i) => (o.isCorrect ? labels[i] : null))
        .filter(Boolean);
      finalCorrectAnswer = correctLabels.join(',');
    } else if (questionType === QuestionType.TRUE_FALSE) {
      finalCorrectAnswer = trueFalseAnswer;
    } else if (
      questionType === QuestionType.SHORT_ANSWER ||
      questionType === QuestionType.FILL_BLANK
    ) {
      finalCorrectAnswer = cleanMap(correctAnswer);
    } else if (
      questionType === QuestionType.MATCHING ||
      questionType === QuestionType.ORDERING
    ) {
      finalCorrectAnswer = cleanMap(correctAnswer);
    }

    if (isEdit) {
      const data: UpdateQuestionRequest = {
        questionText: cleanQuestion,
        questionType: questionType as QuestionType,
        difficulty,
        points,
        timeLimitSeconds: timeLimitSeconds ? parseInt(timeLimitSeconds) : null,
        options: finalOptions ?? [],
        correctAnswer: finalCorrectAnswer ?? '',
        proof: cleanProof,
        ...(changeReason.trim() ? { changeReason: changeReason.trim() } : {}),
      };
      onSubmit(data);
    } else {
      const data: CreateQuestionRequest = {
        topicId,
        questionText: cleanQuestion,
        questionType: questionType as QuestionType,
        difficulty,
        points,
        ...(timeLimitSeconds ? { timeLimitSeconds: parseInt(timeLimitSeconds) } : {}),
        options: finalOptions ?? [],
        correctAnswer: finalCorrectAnswer ?? '',
        ...(Object.keys(cleanProof).length > 0 ? { proof: cleanProof } : {}),
      };
      onSubmit(data);
    }
  }

  // -- Navigation --

  function handleNext() {
    if (activeStep === STEP_LABELS.length - 1) {
      handleSubmit();
    } else {
      setActiveStep((prev) => prev + 1);
    }
  }

  function handleBack() {
    setActiveStep((prev) => prev - 1);
  }

  // -- MCQ helpers --

  function handleOptionTextChange(index: number, key: string, value: string) {
    setMcqOptions((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], text: { ...next[index].text, [key]: value } };
      return next;
    });
  }

  function handleOptionCorrectChange(index: number, checked: boolean) {
    setMcqOptions((prev) => {
      const next = [...prev];
      if (questionType === QuestionType.MCQ_SINGLE) {
        // Radio behavior: only one correct
        return next.map((o, i) => ({ ...o, isCorrect: i === index }));
      }
      // Checkbox behavior
      next[index] = { ...next[index], isCorrect: checked };
      return next;
    });
  }

  function handleAddOption() {
    setMcqOptions((prev) => [...prev, { text: {}, isCorrect: false }]);
  }

  function handleRemoveOption(index: number) {
    setMcqOptions((prev) => prev.filter((_, i) => i !== index));
  }

  // -- Render Steps --

  function renderStepSubjectTopic() {
    const grades = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <TextField
          select
          label={t('form.selectSubject')}
          value={subjectId}
          onChange={(e) => setSubjectId(e.target.value)}
          fullWidth
          required
          disabled={isEdit}
        >
          <MenuItem value="">{t('form.selectSubject')}</MenuItem>
          {subjects.map((s) => (
            <MenuItem key={s.id} value={s.id}>
              {resolveTranslation(s.nameTranslations) || s.name}
            </MenuItem>
          ))}
        </TextField>

        {subjectId && !isEdit && (
          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>{t('form.gradeLevel')}</Typography>
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              {grades.map((grade) => (
                <Chip
                  key={grade}
                  label={`${grade}`}
                  color={gradeLevel === grade ? 'primary' : 'default'}
                  variant={gradeLevel === grade ? 'filled' : 'outlined'}
                  onClick={() => setGradeLevel(grade)}
                  sx={{ minWidth: 40 }}
                />
              ))}
            </Box>
          </Box>
        )}

        <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
          <TextField
            select
            label={t('form.selectTopic')}
            value={topicId}
            onChange={(e) => setTopicId(e.target.value)}
            fullWidth
            required
            disabled={isEdit || !subjectId || gradeLevel === null}
          >
            <MenuItem value="">{t('form.selectTopic')}</MenuItem>
            {flatTopics.map((topic) => (
              <MenuItem key={topic.id} value={topic.id}>
                <Box component="span" sx={{ pl: topic.depth * 2 }}>
                  {topic.depth > 0 ? '-- ' : ''}
                  {resolveTranslation(topic.nameTranslations) || topic.name}
                </Box>
              </MenuItem>
            ))}
          </TextField>
          {!isEdit && subjectId && gradeLevel !== null && (
            <Button
              variant="outlined"
              size="small"
              onClick={() => setTopicFormOpen(true)}
              sx={{ mt: 1, minWidth: 'auto', whiteSpace: 'nowrap' }}
              startIcon={<AddIcon />}
            >
              {tTopic('create')}
            </Button>
          )}
        </Box>

        {!isEdit && subjectId && gradeLevel !== null && flatTopics.length === 0 && (
          <Alert severity="info" sx={{ py: 0.5 }}>
            {tTopic('empty')}
          </Alert>
        )}

        <TopicFormDialog
          open={topicFormOpen}
          onClose={() => setTopicFormOpen(false)}
          onSubmit={(data) => {
            const createData = { ...data, gradeLevel: gradeLevel! } as CreateTopicRequest;
            createTopic.mutate(createData, {
              onSuccess: (response) => {
                setTopicFormOpen(false);
                setTopicId(response.data.data.id);
              },
            });
          }}
          isPending={createTopic.isPending}
        />
      </Box>
    );
  }

  function renderStepType() {
    return (
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: 'repeat(2, 1fr)',
          gap: 1.5,
        }}
      >
        {QUESTION_TYPES.map((qt) => (
          <Paper
            key={qt}
            variant="outlined"
            onClick={() => setQuestionType(qt)}
            sx={{
              p: 2,
              cursor: 'pointer',
              textAlign: 'center',
              borderColor: questionType === qt ? 'primary.main' : 'divider',
              borderWidth: questionType === qt ? 2 : 1,
              bgcolor: questionType === qt ? 'action.selected' : 'background.paper',
              '&:hover': { borderColor: 'primary.light' },
            }}
          >
            <Typography variant="body2" fontWeight={questionType === qt ? 600 : 400}>
              {t(`types.${qt}`)}
            </Typography>
          </Paper>
        ))}
      </Box>
    );
  }

  function renderStepQuestionText() {
    const currentFrontendLang = SUPPORTED_LANGUAGES[questionLangTab] || 'uzl';
    const currentKey = langKey(currentFrontendLang);

    return (
      <Box>
        <Alert severity="info" sx={{ mb: 2, py: 0.5 }}>
          {t('form.langHint')}
        </Alert>

        <Tabs
          value={questionLangTab}
          onChange={(_, v) => setQuestionLangTab(v)}
          sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
        >
          {SUPPORTED_LANGUAGES.map((lang) => (
            <Tab
              key={lang}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  {isLangFilled(questionText, lang) ? (
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
          label={`${t('form.questionText')} (${LANGUAGE_LABELS[currentFrontendLang]})`}
          placeholder={t('form.questionTextPlaceholder')}
          value={questionText[currentKey] || ''}
          onChange={(e) =>
            setQuestionText({ ...questionText, [currentKey]: e.target.value })
          }
          fullWidth
          multiline
          rows={4}
          required={currentFrontendLang === 'uzl'}
        />
      </Box>
    );
  }

  function renderStepOptions() {
    if (!questionType) {
      return (
        <Typography color="text.secondary">
          {t('form.questionType')}
        </Typography>
      );
    }

    switch (questionType) {
      case QuestionType.MCQ_SINGLE:
      case QuestionType.MCQ_MULTI:
        return renderMcqOptions();
      case QuestionType.TRUE_FALSE:
        return renderTrueFalse();
      case QuestionType.SHORT_ANSWER:
      case QuestionType.FILL_BLANK:
        return renderShortAnswer();
      case QuestionType.ESSAY:
        return renderEssayNote();
      case QuestionType.MATCHING:
      case QuestionType.ORDERING:
        return renderAdvancedAnswer();
      default:
        return null;
    }
  }

  function renderMcqOptions() {
    const isSingle = questionType === QuestionType.MCQ_SINGLE;
    const currentFrontendLang = SUPPORTED_LANGUAGES[optionsLangTab] || 'uzl';
    const currentKey = langKey(currentFrontendLang);

    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
        <Alert severity="info" sx={{ mb: 0.5, py: 0.5 }}>
          {t('form.langHint')}
        </Alert>

        <Tabs
          value={optionsLangTab}
          onChange={(_, v) => setOptionsLangTab(v)}
          sx={{ mb: 1, borderBottom: 1, borderColor: 'divider' }}
        >
          {SUPPORTED_LANGUAGES.map((lang) => (
            <Tab
              key={lang}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  {mcqOptions.some((o) => o.text[langKey(lang)]?.trim()) ? (
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

        <Typography variant="body2" color="text.secondary" sx={{ mb: 0.5 }}>
          {t('form.options')}
        </Typography>
        {mcqOptions.map((option, index) => (
          <Box
            key={index}
            sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
          >
            {isSingle ? (
              <Radio
                checked={option.isCorrect}
                onChange={() => handleOptionCorrectChange(index, true)}
                size="small"
              />
            ) : (
              <Checkbox
                checked={option.isCorrect}
                onChange={(e) =>
                  handleOptionCorrectChange(index, e.target.checked)
                }
                size="small"
              />
            )}
            <TextField
              value={option.text[currentKey] || ''}
              onChange={(e) => handleOptionTextChange(index, currentKey, e.target.value)}
              placeholder={`${t('form.optionText')} ${index + 1}`}
              size="small"
              fullWidth
            />
            <IconButton
              onClick={() => handleRemoveOption(index)}
              disabled={mcqOptions.length <= 2}
              size="small"
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Box>
        ))}
        <Button onClick={handleAddOption} size="small" sx={{ alignSelf: 'flex-start' }}>
          {t('form.addOption')}
        </Button>
      </Box>
    );
  }

  function renderTrueFalse() {
    return (
      <Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          {t('form.correctAnswer')}
        </Typography>
        <RadioGroup
          value={trueFalseAnswer}
          onChange={(e) => setTrueFalseAnswer(e.target.value)}
        >
          <FormControlLabel value="true" control={<Radio />} label={t('form.true')} />
          <FormControlLabel value="false" control={<Radio />} label={t('form.false')} />
        </RadioGroup>
      </Box>
    );
  }

  function renderShortAnswer() {
    const currentFrontendLang = SUPPORTED_LANGUAGES[optionsLangTab] || 'uzl';
    const currentKey = langKey(currentFrontendLang);

    return (
      <Box>
        <Alert severity="info" sx={{ mb: 1, py: 0.5 }}>
          {t('form.langHint')}
        </Alert>

        <Tabs
          value={optionsLangTab}
          onChange={(_, v) => setOptionsLangTab(v)}
          sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
        >
          {SUPPORTED_LANGUAGES.map((lang) => (
            <Tab
              key={lang}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  {isLangFilled(correctAnswer, lang) ? (
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
          label={`${t('form.correctAnswer')} (${LANGUAGE_LABELS[currentFrontendLang]})`}
          value={correctAnswer[currentKey] || ''}
          onChange={(e) => setCorrectAnswer({ ...correctAnswer, [currentKey]: e.target.value })}
          fullWidth
          required={currentFrontendLang === 'uzl'}
        />
      </Box>
    );
  }

  function renderEssayNote() {
    return (
      <Alert severity="info">
        {t('types.ESSAY')} -- {t('form.correctAnswer')}: N/A
      </Alert>
    );
  }

  function renderAdvancedAnswer() {
    const currentFrontendLang = SUPPORTED_LANGUAGES[optionsLangTab] || 'uzl';
    const currentKey = langKey(currentFrontendLang);

    return (
      <Box>
        <Tabs
          value={optionsLangTab}
          onChange={(_, v) => setOptionsLangTab(v)}
          sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
        >
          {SUPPORTED_LANGUAGES.map((lang) => (
            <Tab
              key={lang}
              label={LANGUAGE_LABELS[lang]}
              sx={{ textTransform: 'none', minWidth: 0, px: 1.5 }}
            />
          ))}
        </Tabs>

        <TextField
          label={`${t('form.correctAnswer')} (${LANGUAGE_LABELS[currentFrontendLang]})`}
          value={correctAnswer[currentKey] || ''}
          onChange={(e) => setCorrectAnswer({ ...correctAnswer, [currentKey]: e.target.value })}
          fullWidth
          multiline
          rows={3}
          placeholder="JSON / text"
        />
      </Box>
    );
  }

  function renderStepSettings() {
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
        <TextField
          select
          label={t('form.difficulty')}
          value={difficulty}
          onChange={(e) => setDifficulty(e.target.value as Difficulty)}
          fullWidth
        >
          {DIFFICULTY_OPTIONS.map((d) => (
            <MenuItem key={d} value={d}>
              {t(`difficulties.${d}`)}
            </MenuItem>
          ))}
        </TextField>

        <TextField
          label={t('form.points')}
          type="number"
          value={points}
          onChange={(e) => setPoints(Math.max(1, parseInt(e.target.value) || 1))}
          fullWidth
          inputProps={{ min: 1 }}
        />

        <TextField
          label={t('form.timeLimit')}
          type="number"
          value={timeLimitSeconds}
          onChange={(e) => setTimeLimitSeconds(e.target.value)}
          fullWidth
          inputProps={{ min: 0 }}
        />
      </Box>
    );
  }

  function renderStepProof() {
    const currentFrontendLang = SUPPORTED_LANGUAGES[proofLangTab] || 'uzl';
    const currentKey = langKey(currentFrontendLang);

    return (
      <Box>
        <Tabs
          value={proofLangTab}
          onChange={(_, v) => setProofLangTab(v)}
          sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
        >
          {SUPPORTED_LANGUAGES.map((lang) => (
            <Tab
              key={lang}
              label={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                  {isLangFilled(proof, lang) ? (
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
          label={`${t('form.proof')} (${LANGUAGE_LABELS[currentFrontendLang]})`}
          placeholder={t('form.proofPlaceholder')}
          value={proof[currentKey] || ''}
          onChange={(e) =>
            setProof({ ...proof, [currentKey]: e.target.value })
          }
          fullWidth
          multiline
          rows={3}
        />

        {isEdit && (
          <TextField
            label={t('form.changeReason')}
            value={changeReason}
            onChange={(e) => setChangeReason(e.target.value)}
            fullWidth
            sx={{ mt: 2 }}
          />
        )}
      </Box>
    );
  }

  function renderActiveStep() {
    switch (activeStep) {
      case 0:
        return renderStepSubjectTopic();
      case 1:
        return renderStepType();
      case 2:
        return renderStepQuestionText();
      case 3:
        return renderStepOptions();
      case 4:
        return renderStepSettings();
      case 5:
        return renderStepProof();
      default:
        return null;
    }
  }

  const isLastStep = activeStep === STEP_LABELS.length - 1;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {isEdit ? t('editTitle') : t('createTitle')}
      </DialogTitle>

      <DialogContent>
        <Stepper
          activeStep={activeStep}
          alternativeLabel
          sx={{ mb: 3, mt: 1 }}
        >
          {STEP_LABELS.map((label) => (
            <Step key={label}>
              <StepLabel>
                <Typography variant="caption">{t(label)}</Typography>
              </StepLabel>
            </Step>
          ))}
        </Stepper>

        <Box sx={{ minHeight: 200 }}>{renderActiveStep()}</Box>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>{t('common:cancel')}</Button>
        <Box sx={{ flex: 1 }} />
        <Button onClick={handleBack} disabled={activeStep === 0}>
          {t('common:back', 'Back')}
        </Button>
        <Button
          variant="contained"
          onClick={handleNext}
          disabled={isPending || !isStepValid(activeStep)}
        >
          {isPending ? (
            <CircularProgress size={20} />
          ) : isLastStep ? (
            isEdit ? t('common:save') : t('create')
          ) : (
            t('common:next', 'Next')
          )}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
