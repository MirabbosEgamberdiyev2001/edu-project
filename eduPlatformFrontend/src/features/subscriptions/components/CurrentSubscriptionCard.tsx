import { Paper, Typography, Box, Chip, Button } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { UserSubscriptionDto } from '@/types/subscription';
import { SubscriptionStatus } from '@/types/subscription';

interface CurrentSubscriptionCardProps {
  subscription: UserSubscriptionDto;
  onCancel: () => void;
  isCancelling: boolean;
}

const STATUS_COLORS: Record<string, 'success' | 'error' | 'warning' | 'default'> = {
  ACTIVE: 'success',
  EXPIRED: 'error',
  CANCELLED: 'error',
  SUSPENDED: 'warning',
  PENDING_PAYMENT: 'warning',
};

export default function CurrentSubscriptionCard({ subscription, onCancel, isCancelling }: CurrentSubscriptionCardProps) {
  const { t } = useTranslation('subscription');

  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <Box>
          <Typography variant="h6" fontWeight={700}>{subscription.planName}</Typography>
          <Chip
            label={t(`status.${subscription.status}`)}
            size="small"
            color={STATUS_COLORS[subscription.status] || 'default'}
            sx={{ mt: 0.5 }}
          />
        </Box>
        <Box sx={{ textAlign: 'right' }}>
          <Typography variant="body2" color="text.secondary">{t('remainingDays')}</Typography>
          <Typography variant="h4" fontWeight={700} color="primary.main">{subscription.remainingDays}</Typography>
        </Box>
      </Box>

      <Box sx={{ display: 'flex', gap: 4, mt: 2 }}>
        <Box>
          <Typography variant="caption" color="text.secondary">{t('startDate')}</Typography>
          <Typography variant="body2">{new Date(subscription.startDate).toLocaleDateString()}</Typography>
        </Box>
        <Box>
          <Typography variant="caption" color="text.secondary">{t('endDate')}</Typography>
          <Typography variant="body2">{new Date(subscription.endDate).toLocaleDateString()}</Typography>
        </Box>
      </Box>

      {subscription.status === SubscriptionStatus.ACTIVE && (
        <Button
          variant="outlined"
          color="error"
          size="small"
          onClick={onCancel}
          disabled={isCancelling}
          sx={{ mt: 2 }}
        >
          {t('cancelSubscription')}
        </Button>
      )}
    </Paper>
  );
}
