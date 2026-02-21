import { Table, TableHead, TableRow, TableCell, TableBody, Chip } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { PaymentDto } from '@/types/subscription';

interface PaymentHistoryTableProps {
  payments: PaymentDto[];
}

const STATUS_COLORS: Record<string, 'success' | 'error' | 'warning' | 'info' | 'default'> = {
  COMPLETED: 'success',
  FAILED: 'error',
  PENDING: 'warning',
  PROCESSING: 'info',
  REFUNDED: 'default',
};

export default function PaymentHistoryTable({ payments }: PaymentHistoryTableProps) {
  const { t } = useTranslation('subscription');

  return (
    <Table size="small">
      <TableHead>
        <TableRow>
          <TableCell>{t('date')}</TableCell>
          <TableCell>{t('amount')}</TableCell>
          <TableCell>{t('provider')}</TableCell>
          <TableCell>{t('paymentStatus')}</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {payments.map((payment) => (
          <TableRow key={payment.id}>
            <TableCell>{new Date(payment.createdAt).toLocaleDateString()}</TableCell>
            <TableCell>{payment.amount.toLocaleString()} UZS</TableCell>
            <TableCell>
              <Chip label={payment.provider} size="small" variant="outlined" />
            </TableCell>
            <TableCell>
              <Chip
                label={t(`paymentStatuses.${payment.status}`)}
                size="small"
                color={STATUS_COLORS[payment.status] || 'default'}
              />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
