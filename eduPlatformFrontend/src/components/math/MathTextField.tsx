import { useRef, useState, useCallback } from 'react';
import { TextField, IconButton, InputAdornment, Box, Tooltip } from '@mui/material';
import type { TextFieldProps } from '@mui/material';
import FunctionsIcon from '@mui/icons-material/Functions';
import { useTranslation } from 'react-i18next';
import FormulaDialog from './FormulaDialog';
import MathText from './MathText';

export interface MathTextFieldProps
  extends Omit<TextFieldProps, 'onChange' | 'value' | 'inputRef'> {
  value: string;
  /** Receives the updated string directly (not a ChangeEvent) */
  onChange: (value: string) => void;
}

/**
 * Drop-in replacement for `<TextField>` with:
 *  - Σ adornment button → opens FormulaDialog (MathLive WYSIWYG)
 *  - Formula inserted at textarea/input cursor position
 *  - Live KaTeX preview rendered below the field when text contains `$`
 */
export default function MathTextField({
  value,
  onChange,
  InputProps,
  ...rest
}: MathTextFieldProps) {
  const { t } = useTranslation('common');
  const inputRef = useRef<HTMLInputElement | HTMLTextAreaElement | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  const handleFormulaInsert = useCallback(
    (latex: string) => {
      const el = inputRef.current;
      if (!el) {
        onChange(value + latex);
        return;
      }
      const start = el.selectionStart ?? value.length;
      const end = el.selectionEnd ?? value.length;
      const newValue = value.slice(0, start) + latex + value.slice(end);
      onChange(newValue);
      // Restore cursor to position just after the inserted latex
      setTimeout(() => {
        el.focus();
        const cursor = start + latex.length;
        el.setSelectionRange(cursor, cursor);
      }, 0);
    },
    [value, onChange],
  );

  const sigmaButton = (
    <InputAdornment position="end">
      <Tooltip title={t('formula.addTooltip')} placement="top" arrow>
        <IconButton
          size="small"
          onClick={() => setDialogOpen(true)}
          tabIndex={-1}
          sx={{
            color: 'primary.main',
            '&:hover': { bgcolor: 'rgba(37,99,235,0.08)' },
          }}
        >
          <FunctionsIcon fontSize="small" />
        </IconButton>
      </Tooltip>
    </InputAdornment>
  );

  const hasMath = value.includes('$');

  return (
    <>
      <TextField
        {...rest}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        inputRef={inputRef}
        InputProps={{
          ...InputProps,
          endAdornment: sigmaButton,
        }}
      />

      {/* Live KaTeX preview — only when the value contains math delimiters */}
      {hasMath && (
        <Box
          sx={{
            mt: 0.5,
            px: 1.5,
            py: 0.875,
            bgcolor: 'grey.50',
            border: '1px solid',
            borderColor: 'divider',
            borderRadius: 1,
          }}
        >
          <MathText text={value} variant="body2" />
        </Box>
      )}

      <FormulaDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onInsert={handleFormulaInsert}
      />
    </>
  );
}
