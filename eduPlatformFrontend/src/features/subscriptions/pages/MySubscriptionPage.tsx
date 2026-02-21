import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, Paper, CircularProgress, Button } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useMySubscription, useMyUsage } from '../hooks/useMySubscription';
import { useSubscriptionMutations } from '../hooks/useSubscriptionMutations';
import CurrentSubscriptionCard from '../components/CurrentSubscriptionCard';
import UsageMeter from '../components/UsageMeter';
import SubscriptionCancelDialog from '../components/SubscriptionCancelDialog';

export default function MySubscriptionPage() {
  const { t } = useTranslation('subscription');
  const navigate = useNavigate();
  const { data: subscription, isLoading } = useMySubscription();
  const { data: usage } = useMyUsage();
  const { cancelSubscription } = useSubscriptionMutations();
  const [cancelOpen, setCancelOpen] = useState(false);

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 3 }}>{t('mySubscriptionTitle')}</Typography>

      {subscription ? (
        <>
          <CurrentSubscriptionCard
            subscription={subscription}
            onCancel={() => setCancelOpen(true)}
            isCancelling={cancelSubscription.isPending}
          />

          {usage && (
            <Paper sx={{ p: 3, mb: 3 }}>
              <Typography variant="h6" sx={{ mb: 2 }}>{t('usage')}</Typography>
              <UsageMeter usage={usage} />
            </Paper>
          )}

          <SubscriptionCancelDialog
            open={cancelOpen}
            onClose={() => setCancelOpen(false)}
            onConfirm={() => {
              cancelSubscription.mutate(subscription.id, { onSuccess: () => setCancelOpen(false) });
            }}
            isPending={cancelSubscription.isPending}
          />
        </>
      ) : (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" color="text.secondary" sx={{ mb: 2 }}>{t('noSubscription')}</Typography>
          <Button variant="contained" onClick={() => navigate('/pricing')}>
            {t('viewPlans')}
          </Button>
        </Paper>
      )}

      <Button variant="outlined" onClick={() => navigate('/payments')} sx={{ mt: 2 }}>
        {t('paymentHistory')}
      </Button>
    </Box>
  );
}
