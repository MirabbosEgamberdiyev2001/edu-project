import { Tabs, Tab } from '@mui/material';
import { useTranslation } from 'react-i18next';
import EmailIcon from '@mui/icons-material/Email';
import PhoneIcon from '@mui/icons-material/Phone';

interface AuthMethodTabsProps {
  value: 'email' | 'phone';
  onChange: (method: 'email' | 'phone') => void;
}

export default function AuthMethodTabs({ value, onChange }: AuthMethodTabsProps) {
  const { t } = useTranslation('auth');

  return (
    <Tabs
      value={value}
      onChange={(_, v) => onChange(v)}
      variant="fullWidth"
      sx={{ mb: 3 }}
    >
      <Tab
        value="email"
        label={t('method.email')}
        icon={<EmailIcon />}
        iconPosition="start"
      />
      <Tab
        value="phone"
        label={t('method.phone')}
        icon={<PhoneIcon />}
        iconPosition="start"
      />
    </Tabs>
  );
}
