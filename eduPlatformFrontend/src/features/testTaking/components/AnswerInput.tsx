import {
  TextField,
  Box,
  Typography,
  Paper,
  Select,
  MenuItem,
  FormControl,
  IconButton,
} from '@mui/material';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import { useTranslation } from 'react-i18next';
import type { AttemptQuestionDto } from '@/types/testTaking';
import { resolveTranslation } from '@/utils/i18nUtils';
import { MathText } from '@/components/math';

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
      component="div"
      role={multi ? 'checkbox' : 'radio'}
      aria-checked={selected}
      tabIndex={disabled ? -1 : 0}
      variant="outlined"
      onClick={() => !disabled && onClick()}
      onKeyDown={(e: React.KeyboardEvent) => {
        if (!disabled && (e.key === 'Enter' || e.key === ' ')) {
          e.preventDefault();
          onClick();
        }
      }}
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 1.5,
        p: '10px 14px',
        borderRadius: 1.5,
        borderWidth: selected ? 2 : 1,
        borderColor: selected ? 'primary.main' : 'divider',
        bgcolor: selected ? 'rgba(25, 118, 210, 0.07)' : 'background.paper',
        cursor: disabled ? 'default' : 'pointer',
        userSelect: 'none',
        outline: 'none',
        transition: 'border-color 0.15s ease, background-color 0.15s ease, box-shadow 0.15s ease',
        '&:hover': !disabled
          ? {
              borderColor: selected ? 'primary.main' : 'primary.light',
              bgcolor: selected ? 'rgba(25, 118, 210, 0.10)' : 'rgba(25, 118, 210, 0.03)',
            }
          : {},
        '&:focus-visible': {
          boxShadow: '0 0 0 3px rgba(25,118,210,0.28)',
          borderColor: 'primary.main',
        },
      }}
    >
      {/* Letter badge — circle for single, rounded square for multi */}
      <Box
        aria-hidden="true"
        sx={{
          width: 28,
          height: 28,
          borderRadius: multi ? 1 : '50%',
          border: '2px solid',
          borderColor: selected ? 'primary.main' : 'grey.300',
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
            fontSize: '0.75rem',
            fontWeight: 700,
            lineHeight: 1,
            color: selected ? 'white' : 'text.secondary',
          }}
        >
          {selected && multi ? '✓' : label}
        </Typography>
      </Box>

      <MathText text={text} variant="body1" sx={{ flex: 1, wordBreak: 'break-word' }} />
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

    case 'MATCHING': {
      type MatchItem = { id: string; text: string | Record<string, string> };
      const matchingData = question.options as { premises: MatchItem[]; options: MatchItem[] } | null;
      if (!matchingData?.premises?.length) return null;
      const currentMap = (value as Record<string, string>) || {};
      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
          {matchingData.premises.map((premise) => (
            <Box key={premise.id} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Box
                sx={{
                  flex: 1,
                  p: '10px 14px',
                  borderRadius: 1.5,
                  border: '1px solid',
                  borderColor: 'divider',
                  bgcolor: 'background.paper',
                }}
              >
                <MathText text={resolveOptionText(premise.text)} variant="body1" />
              </Box>
              <Typography color="text.secondary" sx={{ flexShrink: 0 }}>→</Typography>
              <FormControl sx={{ flex: 1 }} size="small">
                <Select
                  value={currentMap[premise.id] || ''}
                  onChange={(e) => onChange({ ...currentMap, [premise.id]: e.target.value })}
                  disabled={disabled}
                  displayEmpty
                >
                  <MenuItem value="" disabled>
                    {t('matchSelectPlaceholder')}
                  </MenuItem>
                  {matchingData.options.map((opt) => (
                    <MenuItem key={opt.id} value={opt.id}>
                      {resolveOptionText(opt.text)}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Box>
          ))}
        </Box>
      );
    }

    case 'ORDERING': {
      type OrderItem = { id: string; text: string | Record<string, string> };
      const items = Array.isArray(question.options) ? (question.options as OrderItem[]) : [];
      const currentOrder: string[] =
        Array.isArray(value) && (value as string[]).length > 0
          ? (value as string[])
          : items.map((item) => item.id);
      const itemById = new Map(items.map((item) => [item.id, item]));

      const moveUp = (index: number) => {
        if (index === 0 || disabled) return;
        const next = [...currentOrder];
        [next[index - 1], next[index]] = [next[index], next[index - 1]];
        onChange(next);
      };
      const moveDown = (index: number) => {
        if (index === currentOrder.length - 1 || disabled) return;
        const next = [...currentOrder];
        [next[index], next[index + 1]] = [next[index + 1], next[index]];
        onChange(next);
      };

      return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mt: 2 }}>
          <Typography variant="caption" color="text.secondary">
            {t('orderInstruction')}
          </Typography>
          {currentOrder.map((id, index) => {
            const item = itemById.get(id);
            if (!item) return null;
            return (
              <Box
                key={id}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1.5,
                  p: '10px 14px',
                  borderRadius: 1.5,
                  border: '1px solid',
                  borderColor: 'divider',
                  bgcolor: 'background.paper',
                }}
              >
                <Box
                  sx={{
                    width: 28,
                    height: 28,
                    borderRadius: '50%',
                    bgcolor: 'primary.main',
                    color: 'white',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                    fontWeight: 700,
                    fontSize: '0.8rem',
                  }}
                >
                  {index + 1}
                </Box>
                <MathText text={resolveOptionText(item.text)} variant="body1" sx={{ flex: 1 }} />
                <Box sx={{ display: 'flex', flexDirection: 'column' }}>
                  <IconButton
                    size="small"
                    onClick={() => moveUp(index)}
                    disabled={disabled || index === 0}
                    aria-label="Move up"
                  >
                    <ArrowUpwardIcon fontSize="small" />
                  </IconButton>
                  <IconButton
                    size="small"
                    onClick={() => moveDown(index)}
                    disabled={disabled || index === currentOrder.length - 1}
                    aria-label="Move down"
                  >
                    <ArrowDownwardIcon fontSize="small" />
                  </IconButton>
                </Box>
              </Box>
            );
          })}
        </Box>
      );
    }

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
