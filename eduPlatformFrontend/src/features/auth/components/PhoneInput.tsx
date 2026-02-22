import { TextField, InputAdornment, type TextFieldProps } from '@mui/material';
import { PHONE_PREFIX } from '@/config';

type PhoneInputProps = Omit<TextFieldProps, 'onChange'> & {
  onChange: (value: string) => void;
};

export default function PhoneInput({ value, onChange, ...props }: PhoneInputProps) {
  const displayValue = typeof value === 'string' ? value.replace(PHONE_PREFIX, '') : '';

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const digits = e.target.value.replace(/\D/g, '').slice(0, 9);
    onChange(digits ? `${PHONE_PREFIX}${digits}` : '');
  };

  return (
    <TextField
      {...props}
      value={displayValue}
      onChange={handleChange}
      InputProps={{
        ...props.InputProps,
        startAdornment: (
          <InputAdornment position="start">{PHONE_PREFIX}</InputAdornment>
        ),
      }}
      inputProps={{
        inputMode: 'numeric',
        maxLength: 9,
      }}
    />
  );
}
