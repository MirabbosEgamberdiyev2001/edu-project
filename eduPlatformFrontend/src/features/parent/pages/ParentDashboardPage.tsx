import { useState } from 'react';
import { Box, CircularProgress, Button } from '@mui/material';
import LinkIcon from '@mui/icons-material/Link';
import { useTranslation } from 'react-i18next';
import { PageShell } from '@/components/ui';
import { useChildren } from '../hooks/useParent';
import { useParentMutations } from '../hooks/useParentMutations';
import ChildrenList from '../components/ChildrenList';
import PairingCodeDisplay from '../components/PairingCodeDisplay';
import PairWithCodeForm from '../components/PairWithCodeForm';
import { useAuth } from '@/hooks/useAuth';
import { Role } from '@/types/user';

export default function ParentDashboardPage() {
  const { t } = useTranslation('parent');
  const { user } = useAuth();
  const { data: children, isLoading } = useChildren();
  const { generatePairingCode, pairWithCode } = useParentMutations();
  const [showPairing, setShowPairing] = useState(false);

  const isParent = user?.role === Role.PARENT;

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
        <Button variant="outlined" startIcon={<LinkIcon />} onClick={() => setShowPairing(true)}>
          {t('pairChild')}
        </Button>
      ) : undefined}
    >
      {isParent && showPairing && (
        <Box sx={{ mb: 3 }}>
          <PairWithCodeForm
            onSubmit={(code: string) => pairWithCode.mutate({ code }, { onSuccess: () => setShowPairing(false) })}
            isPending={pairWithCode.isPending}
          />
        </Box>
      )}

      <ChildrenList
        children={children}
        isLoading={isLoading}
      />
    </PageShell>
  );
}
