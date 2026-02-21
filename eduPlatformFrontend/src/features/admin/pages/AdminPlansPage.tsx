import { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  IconButton,
  Chip,
  Fab,
  CircularProgress,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { useTranslation } from 'react-i18next';
import { useAllPlans } from '@/features/subscriptions/hooks/usePlans';
import { usePlanMutations } from '@/features/subscriptions/hooks/useSubscriptionMutations';
import AdminPlanFormDialog from '../components/AdminPlanFormDialog';
import type { SubscriptionPlanDto, CreatePlanRequest } from '@/types/subscription';

export default function AdminPlansPage() {
  const { t } = useTranslation('subscription');
  const { data, isLoading } = useAllPlans({ size: 50 });
  const { createPlan, updatePlan, deletePlan } = usePlanMutations();

  const [formOpen, setFormOpen] = useState(false);
  const [editPlan, setEditPlan] = useState<SubscriptionPlanDto | null>(null);

  const handleCreate = () => {
    setEditPlan(null);
    setFormOpen(true);
  };

  const handleEdit = (plan: SubscriptionPlanDto) => {
    setEditPlan(plan);
    setFormOpen(true);
  };

  const handleSubmit = (formData: CreatePlanRequest) => {
    if (editPlan) {
      updatePlan.mutate({ id: editPlan.id, data: formData }, { onSuccess: () => setFormOpen(false) });
    } else {
      createPlan.mutate(formData, { onSuccess: () => setFormOpen(false) });
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
      <Typography variant="h5" fontWeight={700} sx={{ mb: 3 }}>{t('adminPlansTitle')}</Typography>

      <Paper sx={{ overflow: 'auto' }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{t('planName')}</TableCell>
              <TableCell>{t('type')}</TableCell>
              <TableCell align="right">{t('price')}</TableCell>
              <TableCell align="center">{t('maxTests')}</TableCell>
              <TableCell align="center">{t('maxExports')}</TableCell>
              <TableCell>{t('status')}</TableCell>
              <TableCell align="center">{t('actions')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data?.content.map((plan) => (
              <TableRow key={plan.id}>
                <TableCell>{plan.name}</TableCell>
                <TableCell><Chip label={plan.type} size="small" variant="outlined" /></TableCell>
                <TableCell align="right">{plan.monthlyPrice.toLocaleString()} UZS</TableCell>
                <TableCell align="center">{plan.maxTests}</TableCell>
                <TableCell align="center">{plan.maxExports}</TableCell>
                <TableCell>
                  <Chip label={plan.isActive ? t('active') : t('inactive')} size="small" color={plan.isActive ? 'success' : 'default'} />
                </TableCell>
                <TableCell align="center">
                  <IconButton size="small" onClick={() => handleEdit(plan)}>
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton size="small" color="error" onClick={() => deletePlan.mutate(plan.id)}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      <Fab color="primary" onClick={handleCreate} sx={{ position: 'fixed', bottom: 32, right: 32 }}>
        <AddIcon />
      </Fab>

      <AdminPlanFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleSubmit}
        plan={editPlan}
        isPending={createPlan.isPending || updatePlan.isPending}
      />
    </Box>
  );
}
