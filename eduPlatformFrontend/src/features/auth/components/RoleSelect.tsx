import { FormControl, InputLabel, MenuItem, Select, FormHelperText, type SelectProps } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Role } from '@/types/user';

interface RoleSelectProps {
  value: string;
  onChange: SelectProps['onChange'];
  error?: boolean;
  helperText?: string;
}

const SELECTABLE_ROLES = [Role.STUDENT, Role.TEACHER, Role.PARENT];

export default function RoleSelect({ value, onChange, error, helperText }: RoleSelectProps) {
  const { t } = useTranslation('auth');

  return (
    <FormControl fullWidth error={error}>
      <InputLabel>{t('register.role')}</InputLabel>
      <Select value={value} onChange={onChange} label={t('register.role')}>
        {SELECTABLE_ROLES.map((role) => (
          <MenuItem key={role} value={role}>
            {t(`roles.${role}`)}
          </MenuItem>
        ))}
      </Select>
      {helperText && <FormHelperText>{helperText}</FormHelperText>}
    </FormControl>
  );
}
