import { Box, Typography, Paper, Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useParentMutations } from '../hooks/useParentMutations';
import PairingCodeDisplay from '../components/PairingCodeDisplay';
import ParentsList from '../components/ParentsList';

export default function StudentPairingPage() {
  const { t } = useTranslation('parent');
  const { generatePairingCode } = useParentMutations();

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 1 }}>{t('pairingTitle')}</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>{t('pairingSubtitle')}</Typography>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>{t('generateCode')}</Typography>
            <PairingCodeDisplay
              onGenerate={() => generatePairingCode.mutate()}
              data={generatePairingCode.data?.data?.data ?? null}
              isPending={generatePairingCode.isPending}
            />
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>{t('linkedParents')}</Typography>
            <ParentsList />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}
