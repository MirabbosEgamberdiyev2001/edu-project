import {
  TextField,
  Box,
  Typography,
  Paper,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { AttemptQuestionDto } from '@/types/testTaking';
import { resolveTranslation } from '@/utils/i18nUtils';

interface AnswerInputProps {
  question: AttemptQuestionDto;
  value: unknown;
  onChange: (value: unknown) => void;
  disabled?: boolean;
}

/** Backend sends options with `id` field; legacy may use `key`. */
type OptionItem = { id?: string; key?: string; text: string | Record<string, string>; isCorrect?: boolean };

/** Get the canonical identifier for an option (id first, then key). */
function getOptionId(opt: OptionItem): string {
  return (opt.id ?? opt.key) ?? '';
}

function resolveOptionText(text: string | Record<string, string>): string {
  if (typeof text === 'string') return text;
  return resolveTranslation(text) ?? '';
}

/** Sort raw options by optionsOrder when provided (shuffled exams). */
function sortOptions(options: OptionItem[], optionsOrder: string[] | null | undefined): OptionItem[] {
  if (!optionsOrder || optionsOrder.length === 0) return options;
  const byId = new Map(options.map((o) => [getOptionId(o), o]));
  const ordered: OptionItem[] = [];
  optionsOrder.forEach((id) => {
    const opt = byId.get(id);
    if (opt) ordered.push(opt);
  });
  options.forEach((o) => {
    if (!optionsOrder.includes(getOptionId(o))) ordered.push(o);
  });
  return ordered;
}

/** A single selectable option row with letter label (A, B, C …). */
function OptionRow({
  label,
  text,
  selected,
  disabled,
  onClick,
  multi,
}: {
  label: string;
  text: string;
  selected: boolean;
  disabled?: boolean;
  onClick: () => void;
  multi?: boolean;
}) {
  return (
    <Paper
      variant="outlined"
      onClick={() => !disabled && onClick()}
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 1.5,
        p: '11px 16px',
        borderRadius: 2,
        borderWidth: selected ? 2 : 1,
        borderColor: selected ? 'primary.main' : 'divider',
        bgcolor: selected ? 'rgba(25, 118, 210, 0.07)' : 'background.paper',
        cursor: disabled ? 'default' : 'pointer',
        userSelect: 'none',
        transition: 'border-color 0.15s ease, background-color 0.15s ease',
        '&:hover': !disabled
          ? {
              borderColor: selected ? 'primary.main' : 'primary.light',
              bgcolor: selected ? 'rgba(25, 118, 210, 0.10)' : 'rgba(25, 118, 210, 0.03)',
            }
          : {},
      }}
    >
      {/* Letter badge — circle for single, rounded square for multi */}
      <Box
        sx={{
          width: 30,
          height: 30,
          borderRadius: multi ? 1 : '50%',
          border: '2px solid',
          borderColor: selected ? 'primary.main' : 'grey.400',
          flexShrink: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          bgcolor: selected ? 'primary.main' : 'transparent',
          transition: 'all 0.15s ease',
        }}
      >
        <Typography
          sx={{
            fontSize: '0.78rem',
            fontWeight: 700,
            lineHeight: 1,
            color: selected ? 'white' : 'text.secondary',
          }}
        >
          {selected && multi ? '✓' : label}
        </Typography>
      </Box>

      <Typography variant="body1" sx={{ flex: 1, wordBreak: 'break-word' }}>
        {text}
      </Typography>
    </Paper>
  );
}

export default function AnswerInput({ question, value, onChange, disabled }: AnswerInputProps) {
  const { t } = useTranslation('testTaking');
  const rawOptions = Array.isArray(question.options) ? (question.options as OptionItem[]) : [];
  const options = sortOptions(rawOptions, question.optionsOrder);

  switch (question.questionType) {
    case 'TRUE_FALSE': {
      const current = (value as string) || '';
      const tfOptions = [
        { val: 'true', label: t('trueFalseTrue'), borderColor: 'success.main', bg: 'rgba(46,125,50,0.07)' },
        { val: 'false', label: t('trueFalseFalse'), borderColor: 'error.main', bg: 'rgba(211,47,47,0.07)' },
      ] as const;
      return (
        <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
          {tfOptions.map(({ val, label, borderColor, bg }) => {
            const isSelected = current === val;
            return (
              <Paper
                key={val}
                variant="outlined"
                onClick={() => !disabled && onChange(val)}
                sx={{
                  flex: 1,
                  p: '16px',
                  borderRadius: 2,
                  borderWidth: isSelected ? 2 : 1,
                  borderColor: isSelected ? borderColor : 'divider',
                  bgcolor: isSelected ? bg : 'background.paper',
                  cursor: disabled ? 'default' : 'pointer',
                  userSelect: 'none',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  transition: 'border-color 0.15s ease, background-color 0.15s ease',
                  '&:hover': !disabled ? { borderColor: isSelected ? borderColor : 'action.hover' } : {},
                }}
              >
                <Typography variant="body1" fontWeight={isSelected ? 700 : 400} color={isSelected ? borderColor : 'text.primary'}>
                  {label}
                </Typography>
              </Paper>
            );
          })}
        </Box>
      );
    }

    case 'MCQ_SINGLE': {
      const current = (value as string) || '';
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mt: 2 }}>
          {options.map((opt, idx) => {
            const id = getOptionId(opt);
            return (
              <OptionRow
                key={id || idx}
                label={String.fromCharCode(65 + idx)}
                text={resolveOptionText(opt.text)}
                selected={current === id}
                disabled={disabled}
                onClick={() => onChange(id)}
              />
            );
          })}
        </Box>
      );
    }

    case 'MCQ_MULTI': {
      const selected = Array.isArray(value) ? (value as string[]) : [];
      const toggle = (id: string) => {
        const next = selected.includes(id)
          ? selected.filter((k) => k !== id)
          : [...selected, id];
        onChange(next);
      };
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mt: 2 }}>
          {options.map((opt, idx) => {
            const id = getOptionId(opt);
            return (
              <OptionRow
                key={id || idx}
                label={String.fromCharCode(65 + idx)}
                text={resolveOptionText(opt.text)}
                selected={selected.includes(id)}
                disabled={disabled}
                onClick={() => toggle(id)}
                multi
              />
            );
          })}
        </Box>
      );
    }

    case 'SHORT_ANSWER':
    case 'FILL_BLANK':
      return (
        <Box sx={{ mt: 2 }}>
          <TextField
            fullWidth
            value={(value as string) || ''}
            onChange={(e) => onChange(e.target.value)}
            placeholder={t('answerPlaceholder')}
            disabled={disabled}
            variant="outlined"
          />
        </Box>
      );

    case 'ESSAY':
      return (
        <Box sx={{ mt: 2 }}>
          <TextField
            fullWidth
            multiline
            rows={6}
            value={(value as string) || ''}
            onChange={(e) => onChange(e.target.value)}
            placeholder={t('essayPlaceholder')}
            disabled={disabled}
            variant="outlined"
          />
        </Box>
      );

    default:
      return (
        <Box sx={{ mt: 2 }}>
          <TextField
            fullWidth
            value={(value as string) || ''}
            onChange={(e) => onChange(e.target.value)}
            disabled={disabled}
          />
        </Box>
      );
  }
}
