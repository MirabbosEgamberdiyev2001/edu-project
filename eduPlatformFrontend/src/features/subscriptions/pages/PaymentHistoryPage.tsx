import { useState, useMemo } from 'react';
import { Box, Typography, Paper, CircularProgress, Pagination } from '@mui/material';
import PaymentIcon from '@mui/icons-material/Payment';
import { useTranslation } from 'react-i18next';
import { useMyPayments } from '../hooks/usePayments';
import PaymentHistoryTable from '../components/PaymentHistoryTable';

export default function PaymentHistoryPage() {
  const { t } = useTranslation('subscription');
  const [page, setPage] = useState(0);
  const params = useMemo(() => ({ page, size: 20 }), [page]);
  const { data, isLoading } = useMyPayments(params);

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 1 }}>{t('paymentHistoryTitle')}</Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>{t('paymentHistorySubtitle')}</Typography>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : data && data.content.length > 0 ? (
        <>
          <Paper sx={{ overflow: 'auto' }}>
            <PaymentHistoryTable payments={data.content} />
          </Paper>
          {data.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
              <Pagination count={data.totalPages} page={page + 1} onChange={(_, p) => setPage(p - 1)} color="primary" />
            </Box>
          )}
        </>
      ) : (
        <Box sx={{ textAlign: 'center', py: 8 }}>
          <PaymentIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">{t('noPayments')}</Typography>
        </Box>
      )}
    </Box>
  );
}
