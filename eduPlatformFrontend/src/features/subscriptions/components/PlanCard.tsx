import { Paper, Typography, Box, Button, Chip, Divider } from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import { useTranslation } from 'react-i18next';
import type { SubscriptionPlanDto } from '@/types/subscription';

interface PlanCardProps {
  plan: SubscriptionPlanDto;
  isCurrentPlan?: boolean;
  onSelect: (planId: string) => void;
}

export default function PlanCard({ plan, isCurrentPlan, onSelect }: PlanCardProps) {
  const { t } = useTranslation('subscription');

  return (
    <Paper
      sx={{
        p: 3,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        border: isCurrentPlan ? 2 : 1,
        borderColor: isCurrentPlan ? 'primary.main' : 'divider',
        position: 'relative',
      }}
    >
      {isCurrentPlan && (
        <Chip label={t('currentPlan')} color="primary" size="small" sx={{ position: 'absolute', top: -10, right: 16 }} />
      )}
      <Typography variant="h6" fontWeight={700}>{plan.name}</Typography>
      <Chip label={plan.type} size="small" variant="outlined" sx={{ alignSelf: 'flex-start', mt: 0.5 }} />

      <Box sx={{ my: 2 }}>
        <Typography variant="h3" fontWeight={700} color="primary.main">
          {plan.monthlyPrice.toLocaleString()}
          <Typography component="span" variant="body2" color="text.secondary"> UZS/{t('month')}</Typography>
        </Typography>
      </Box>

      <Divider sx={{ mb: 2 }} />

      <Box sx={{ flex: 1, mb: 2 }}>
        <Feature label={t('maxTests', { count: plan.maxTests })} />
        <Feature label={t('maxExports', { count: plan.maxExports })} />
        <Feature label={t('maxQuestions', { count: plan.maxQuestionsPerTest })} />
        {plan.features.map((feature) => (
          <Feature key={feature} label={feature} />
        ))}
      </Box>

      <Button
        variant={isCurrentPlan ? 'outlined' : 'contained'}
        disabled={isCurrentPlan}
        onClick={() => onSelect(plan.id)}
        fullWidth
      >
        {isCurrentPlan ? t('active') : t('subscribe')}
      </Button>
    </Paper>
  );
}

function Feature({ label }: { label: string }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
      <CheckIcon sx={{ fontSize: 16, color: 'success.main' }} />
      <Typography variant="body2">{label}</Typography>
    </Box>
  );
}
