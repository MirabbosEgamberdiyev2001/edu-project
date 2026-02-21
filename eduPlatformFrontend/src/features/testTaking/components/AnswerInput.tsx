import {
  RadioGroup,
  Radio,
  FormControlLabel,
  Checkbox,
  TextField,
  FormGroup,
  Box,
} from '@mui/material';
import type { AttemptQuestionDto } from '@/types/testTaking';

interface AnswerInputProps {
  question: AttemptQuestionDto;
  value: unknown;
  onChange: (value: unknown) => void;
  disabled?: boolean;
}

export default function AnswerInput({ question, value, onChange, disabled }: AnswerInputProps) {
  const options = Array.isArray(question.options) ? question.options as { key: string; text: string }[] : [];

  switch (question.questionType) {
    case 'MCQ_SINGLE':
    case 'TRUE_FALSE':
      return (
        <RadioGroup
          value={(value as string) || ''}
          onChange={(e) => onChange(e.target.value)}
        >
          {options.map((opt) => (
            <FormControlLabel
              key={opt.key}
              value={opt.key}
              control={<Radio disabled={disabled} />}
              label={opt.text}
              sx={{ mb: 0.5 }}
            />
          ))}
        </RadioGroup>
      );

    case 'MCQ_MULTI': {
      const selected = Array.isArray(value) ? value as string[] : [];
      return (
        <FormGroup>
          {options.map((opt) => (
            <FormControlLabel
              key={opt.key}
              control={
                <Checkbox
                  checked={selected.includes(opt.key)}
                  onChange={(e) => {
                    const newSelected = e.target.checked
                      ? [...selected, opt.key]
                      : selected.filter((k) => k !== opt.key);
                    onChange(newSelected);
                  }}
                  disabled={disabled}
                />
              }
              label={opt.text}
              sx={{ mb: 0.5 }}
            />
          ))}
        </FormGroup>
      );
    }

    case 'SHORT_ANSWER':
    case 'FILL_BLANK':
      return (
        <Box sx={{ mt: 1 }}>
          <TextField
            fullWidth
            value={(value as string) || ''}
            onChange={(e) => onChange(e.target.value)}
            placeholder="Your answer..."
            disabled={disabled}
          />
        </Box>
      );

    case 'ESSAY':
      return (
        <Box sx={{ mt: 1 }}>
          <TextField
            fullWidth
            multiline
            rows={6}
            value={(value as string) || ''}
            onChange={(e) => onChange(e.target.value)}
            placeholder="Write your answer..."
            disabled={disabled}
          />
        </Box>
      );

    default:
      return (
        <TextField
          fullWidth
          value={(value as string) || ''}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
        />
      );
  }
}
