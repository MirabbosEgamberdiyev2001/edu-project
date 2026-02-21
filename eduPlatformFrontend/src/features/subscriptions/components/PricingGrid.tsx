import { Grid } from '@mui/material';
import type { SubscriptionPlanDto } from '@/types/subscription';
import PlanCard from './PlanCard';

interface PricingGridProps {
  plans: SubscriptionPlanDto[];
  currentPlanId?: string;
  onSelect: (planId: string) => void;
}

export default function PricingGrid({ plans, currentPlanId, onSelect }: PricingGridProps) {
  return (
    <Grid container spacing={3} alignItems="stretch">
      {plans.map((plan) => (
        <Grid item xs={12} sm={6} md={4} lg={3} key={plan.id}>
          <PlanCard
            plan={plan}
            isCurrentPlan={plan.id === currentPlanId}
            onSelect={onSelect}
          />
        </Grid>
      ))}
    </Grid>
  );
}
