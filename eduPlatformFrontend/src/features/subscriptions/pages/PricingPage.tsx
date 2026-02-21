import { useState } from 'react';
import { Box, Typography, CircularProgress } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { usePlans } from '../hooks/usePlans';
import { useMySubscription } from '../hooks/useMySubscription';
import { useSubscriptionMutations } from '../hooks/useSubscriptionMutations';
import PricingGrid from '../components/PricingGrid';
import PaymentProviderSelect from '../components/PaymentProviderSelect';
import type { PaymentProvider } from '@/types/subscription';

export default function PricingPage() {
  const { t } = useTranslation('subscription');
  const { data: plans, isLoading } = usePlans();
  const { data: subscription } = useMySubscription();
  const { initiatePayment } = useSubscriptionMutations();

  const [selectedPlanId, setSelectedPlanId] = useState<string | null>(null);

  const handleSelectPlan = (planId: string) => {
    setSelectedPlanId(planId);
  };

  const handleSelectProvider = (provider: PaymentProvider) => {
    if (selectedPlanId) {
      initiatePayment.mutate({ planId: selectedPlanId, paymentProvider: provider });
    }
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ textAlign: 'center', mb: 4 }}>
        <Typography variant="h4" fontWeight={700}>{t('pricingTitle')}</Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 1 }}>{t('pricingSubtitle')}</Typography>
      </Box>

      {plans && (
        <PricingGrid
          plans={plans}
          currentPlanId={subscription?.planId}
          onSelect={handleSelectPlan}
        />
      )}

      <PaymentProviderSelect
        open={!!selectedPlanId}
        onClose={() => setSelectedPlanId(null)}
        onSelect={handleSelectProvider}
        isPending={initiatePayment.isPending}
      />
    </Box>
  );
}
