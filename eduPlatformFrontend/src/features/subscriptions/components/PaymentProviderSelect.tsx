import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  List,
  ListItemButton,
  ListItemText,
  ListItemIcon,
  Radio,
} from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { PaymentProvider } from '@/types/subscription';

interface PaymentProviderSelectProps {
  open: boolean;
  onClose: () => void;
  onSelect: (provider: PaymentProvider) => void;
  isPending: boolean;
}

const PROVIDERS = [
  { value: PaymentProvider.PAYME, label: 'Payme' },
  { value: PaymentProvider.CLICK, label: 'Click' },
  { value: PaymentProvider.UZUM, label: 'Uzum' },
];

export default function PaymentProviderSelect({ open, onClose, onSelect, isPending }: PaymentProviderSelectProps) {
  const { t } = useTranslation('subscription');
  const [selected, setSelected] = useState<PaymentProvider>(PaymentProvider.PAYME);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('selectProvider')}</DialogTitle>
      <DialogContent>
        <List>
          {PROVIDERS.map((provider) => (
            <ListItemButton
              key={provider.value}
              selected={selected === provider.value}
              onClick={() => setSelected(provider.value)}
            >
              <ListItemIcon>
                <Radio checked={selected === provider.value} />
              </ListItemIcon>
              <ListItemText primary={provider.label} />
            </ListItemButton>
          ))}
        </List>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isPending}>{t('cancel')}</Button>
        <Button
          variant="contained"
          onClick={() => onSelect(selected)}
          disabled={isPending}
        >
          {t('proceedToPayment')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
