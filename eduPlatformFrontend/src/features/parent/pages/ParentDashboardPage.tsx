import { useMemo, useState } from 'react';
import { Box, CircularProgress, Button, Grid } from '@mui/material';
import LinkIcon from '@mui/icons-material/Link';
import PeopleIcon from '@mui/icons-material/People';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import { useTranslation } from 'react-i18next';
import { PageShell, KpiWidget } from '@/components/ui';
import { useChildren } from '../hooks/useParent';
import { useParentMutations } from '../hooks/useParentMutations';
import ChildrenList from '../components/ChildrenList';
import PairWithCodeForm from '../components/PairWithCodeForm';
import { useAuth } from '@/hooks/useAuth';
import { Role } from '@/types/user';
import { PairingStatus } from '@/types/parent';

export default function ParentDashboardPage() {
  const { t } = useTranslation('parent');
  const { user } = useAuth();
  const { data: children, isLoading } = useChildren();
  const { pairWithCode } = useParentMutations();
  const [showPairing, setShowPairing] = useState(false);

  const isParent = user?.role === Role.PARENT;

  const activeCount = useMemo(
    () => children?.filter(c => c.status === PairingStatus.ACTIVE).length ?? 0,
    [children]
  );
  const pendingCount = useMemo(
    () => children?.filter(c => c.status === PairingStatus.PENDING).length ?? 0,
    [children]
  );
  const totalCount = children?.length ?? 0;

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <PageShell
      title={isParent ? t('myChildrenTitle') : t('familyTitle')}
      subtitle={isParent ? t('myChildrenSubtitle') : t('familySubtitle')}
      actions={isParent ? (
        <Button
          variant="outlined"
          startIcon={<LinkIcon />}
          onClick={() => setShowPairing(prev => !prev)}
        >
          {t('pairChild')}
        </Button>
      ) : undefined}
    >
      {/* KPI Summary — only show when there are children */}
      {totalCount > 0 && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12} sm={4}>
            <KpiWidget
              label={t('kpi.total')}
              value={totalCount}
              icon={<PeopleIcon sx={{ color: '#3b82f6' }} />}
              iconBg="#eff6ff"
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <KpiWidget
              label={t('kpi.active')}
              value={activeCount}
              icon={<CheckCircleOutlineIcon sx={{ color: '#10b981' }} />}
              iconBg="#ecfdf5"
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <KpiWidget
              label={t('kpi.pending')}
              value={pendingCount}
              icon={<HourglassEmptyIcon sx={{ color: '#f59e0b' }} />}
              iconBg="#fffbeb"
            />
          </Grid>
        </Grid>
      )}

      {/* Pairing form (toggle) */}
      {isParent && showPairing && (
        <Box sx={{ mb: 3 }}>
          <PairWithCodeForm
            onSubmit={(code: string) =>
              pairWithCode.mutate({ code }, { onSuccess: () => setShowPairing(false) })
            }
            isPending={pairWithCode.isPending}
          />
        </Box>
      )}

      <ChildrenList children={children} isLoading={isLoading} />
    </PageShell>
  );
}
