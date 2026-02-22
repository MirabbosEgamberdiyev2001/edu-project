import { useRef, useCallback, type ClipboardEvent, type KeyboardEvent, useEffect } from 'react';
import { Box } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { OTP_LENGTH } from '@/config';

interface OtpInputProps {
  value: string;
  onChange: (value: string) => void;
  onComplete?: (value: string) => void;
  error?: boolean;
  disabled?: boolean;
}

export default function OtpInput({ value, onChange, onComplete, error, disabled }: OtpInputProps) {
  const theme = useTheme();
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
  const digits = Array.from({ length: OTP_LENGTH }, (_, i) => value[i] || '');

  const primaryColor = theme.palette.primary.main;
  const errorColor = theme.palette.error.main;
  const borderColor = theme.palette.divider;
  const textColor = theme.palette.text.primary;
  const disabledBg = theme.palette.action.disabledBackground;

  const focusInput = useCallback((index: number) => {
    const clamped = Math.max(0, Math.min(index, OTP_LENGTH - 1));
    setTimeout(() => inputRefs.current[clamped]?.focus(), 0);
  }, []);

  // Auto-focus first input on mount
  useEffect(() => {
    if (!disabled) {
      focusInput(0);
    }
  }, [disabled, focusInput]);

  const handleChange = useCallback(
    (index: number, inputValue: string) => {
      const char = inputValue.replace(/\D/g, '').slice(-1);
      if (!char && inputValue !== '') return;

      const newDigits = [...digits];
      newDigits[index] = char;
      const newValue = newDigits.join('').replace(/ /g, '');
      onChange(newValue);

      if (char && index < OTP_LENGTH - 1) {
        focusInput(index + 1);
      } else if (newValue.length === OTP_LENGTH && onComplete) {
        onComplete(newValue);
      }
    },
    [digits, onChange, focusInput, onComplete],
  );

  const handleKeyDown = useCallback(
    (index: number, e: KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Backspace') {
        e.preventDefault();
        const newDigits = [...digits];
        if (digits[index]) {
          newDigits[index] = '';
          onChange(newDigits.join('').replace(/ /g, ''));
        } else if (index > 0) {
          newDigits[index - 1] = '';
          onChange(newDigits.join('').replace(/ /g, ''));
          focusInput(index - 1);
        }
      } else if (e.key === 'ArrowLeft' && index > 0) {
        e.preventDefault();
        focusInput(index - 1);
      } else if (e.key === 'ArrowRight' && index < OTP_LENGTH - 1) {
        e.preventDefault();
        focusInput(index + 1);
      }
    },
    [digits, onChange, focusInput],
  );

  const handlePaste = useCallback(
    (e: ClipboardEvent<HTMLInputElement>) => {
      e.preventDefault();
      const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH);
      if (pasted) {
        onChange(pasted);
        focusInput(Math.min(pasted.length, OTP_LENGTH - 1));
        if (pasted.length === OTP_LENGTH && onComplete) {
          onComplete(pasted);
        }
      }
    },
    [onChange, focusInput, onComplete],
  );

  const handleFocus = useCallback((e: React.FocusEvent<HTMLInputElement>) => {
    e.target.select();
  }, []);

  return (
    <Box
      sx={{ display: 'flex', gap: { xs: 1, sm: 1.5 }, justifyContent: 'center' }}
      role="group"
      aria-label="OTP input"
    >
      {digits.map((digit, index) => (
        <Box
          key={index}
          sx={{
            position: 'relative',
            width: { xs: 44, sm: 52 },
            height: { xs: 52, sm: 60 },
          }}
        >
          <input
            ref={(ref) => { inputRefs.current[index] = ref; }}
            type="text"
            inputMode="numeric"
            autoComplete={index === 0 ? 'one-time-code' : 'off'}
            maxLength={1}
            value={digit.trim()}
            onChange={(e) => handleChange(index, e.target.value)}
            onKeyDown={(e) => handleKeyDown(index, e)}
            onPaste={handlePaste}
            onFocus={handleFocus}
            disabled={disabled}
            aria-label={`Digit ${index + 1} of ${OTP_LENGTH}`}
            style={{
              width: '100%',
              height: '100%',
              border: `2px solid ${error ? errorColor : digit.trim() ? primaryColor : borderColor}`,
              borderRadius: 12,
              fontSize: '1.5rem',
              fontWeight: 700,
              textAlign: 'center',
              outline: 'none',
              background: disabled ? disabledBg : '#fff',
              color: textColor,
              caretColor: primaryColor,
              transition: 'all 0.2s ease',
              cursor: disabled ? 'not-allowed' : 'text',
            }}
            onFocusCapture={(e) => {
              const el = e.target as HTMLInputElement;
              el.style.borderColor = error ? errorColor : primaryColor;
              el.style.boxShadow = error
                ? `0 0 0 3px ${errorColor}25`
                : `0 0 0 3px ${primaryColor}25`;
            }}
            onBlurCapture={(e) => {
              const el = e.target as HTMLInputElement;
              el.style.borderColor = error ? errorColor : digit.trim() ? primaryColor : borderColor;
              el.style.boxShadow = 'none';
            }}
          />
        </Box>
      ))}
    </Box>
  );
}
