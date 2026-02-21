import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Box,
  Chip,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { SubscriptionPlanDto, CreatePlanRequest } from '@/types/subscription';
import { PlanType } from '@/types/subscription';

interface AdminPlanFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreatePlanRequest) => void;
  plan?: SubscriptionPlanDto | null;
  isPending: boolean;
}

export default function AdminPlanFormDialog({ open, onClose, onSubmit, plan, isPending }: AdminPlanFormDialogProps) {
  const { t } = useTranslation('subscription');
  const [formData, setFormData] = useState<CreatePlanRequest>({
    name: '', type: PlanType.BASIC, monthlyPrice: 0, durationDays: 30,
    maxTests: 10, maxExports: 10, maxQuestionsPerTest: 50, features: [],
  });
  const [featureInput, setFeatureInput] = useState('');

  useEffect(() => {
    if (plan) {
      setFormData({
        name: plan.name, type: plan.type, monthlyPrice: plan.monthlyPrice,
        durationDays: plan.durationDays, maxTests: plan.maxTests, maxExports: plan.maxExports,
        maxQuestionsPerTest: plan.maxQuestionsPerTest, features: plan.features,
      });
    } else {
      setFormData({
        name: '', type: PlanType.BASIC, monthlyPrice: 0, durationDays: 30,
        maxTests: 10, maxExports: 10, maxQuestionsPerTest: 50, features: [],
      });
    }
  }, [plan, open]);

  const addFeature = () => {
    if (featureInput.trim()) {
      setFormData({ ...formData, features: [...formData.features, featureInput.trim()] });
      setFeatureInput('');
    }
  };

  const removeFeature = (index: number) => {
    setFormData({ ...formData, features: formData.features.filter((_, i) => i !== index) });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{plan ? t('editPlan') : t('createPlan')}</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField label={t('planName')} value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} fullWidth required />
          <FormControl fullWidth>
            <InputLabel>{t('type')}</InputLabel>
            <Select value={formData.type} onChange={(e) => setFormData({ ...formData, type: e.target.value as PlanType })} label={t('type')}>
              {Object.values(PlanType).map((type) => (<MenuItem key={type} value={type}>{type}</MenuItem>))}
            </Select>
          </FormControl>
          <TextField label={t('price')} type="number" value={formData.monthlyPrice} onChange={(e) => setFormData({ ...formData, monthlyPrice: Number(e.target.value) })} fullWidth />
          <TextField label={t('durationDays')} type="number" value={formData.durationDays} onChange={(e) => setFormData({ ...formData, durationDays: Number(e.target.value) })} fullWidth />
          <TextField label={t('maxTests')} type="number" value={formData.maxTests} onChange={(e) => setFormData({ ...formData, maxTests: Number(e.target.value) })} fullWidth />
          <TextField label={t('maxExports')} type="number" value={formData.maxExports} onChange={(e) => setFormData({ ...formData, maxExports: Number(e.target.value) })} fullWidth />
          <TextField label={t('maxQuestions')} type="number" value={formData.maxQuestionsPerTest} onChange={(e) => setFormData({ ...formData, maxQuestionsPerTest: Number(e.target.value) })} fullWidth />

          <Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField label={t('addFeature')} value={featureInput} onChange={(e) => setFeatureInput(e.target.value)} size="small" sx={{ flex: 1 }} onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addFeature())} />
              <Button onClick={addFeature} variant="outlined" size="small">{t('add')}</Button>
            </Box>
            <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 1 }}>
              {formData.features.map((f, i) => (<Chip key={i} label={f} size="small" onDelete={() => removeFeature(i)} />))}
            </Box>
          </Box>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isPending}>{t('cancel')}</Button>
        <Button onClick={() => onSubmit(formData)} variant="contained" disabled={isPending || !formData.name}>
          {plan ? t('save') : t('createPlan')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
