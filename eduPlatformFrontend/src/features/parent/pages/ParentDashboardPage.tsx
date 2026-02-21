import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, CircularProgress, Button } from '@mui/material';
import LinkIcon from '@mui/icons-material/Link';
import { useTranslation } from 'react-i18next';
import { useChildren } from '../hooks/useParent';
import { useParentMutations } from '../hooks/useParentMutations';
import ChildrenList from '../components/ChildrenList';
import PairingCodeDisplay from '../components/PairingCodeDisplay';
import PairWithCodeForm from '../components/PairWithCodeForm';
import { useAuth } from '@/hooks/useAuth';
import { Role } from '@/types/user';

export default function ParentDashboardPage() {
  const { t } = useTranslation('parent');
  const navigate = useNavigate();
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
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5" fontWeight={700}>
            {isParent ? t('myChildrenTitle') : t('familyTitle')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {isParent ? t('myChildrenSubtitle') : t('familySubtitle')}
          </Typography>
        </Box>
        {isParent && (
          <Button variant="outlined" startIcon={<LinkIcon />} onClick={() => setShowPairing(true)}>
            {t('pairChild')}
          </Button>
        )}
      </Box>

      {isParent && showPairing && (
        <Box sx={{ mb: 3 }}>
          <PairWithCodeForm
            onPair={(code) => pairWithCode.mutate(code, { onSuccess: () => setShowPairing(false) })}
            isPending={pairWithCode.isPending}
          />
        </Box>
      )}

      {children && (
        <ChildrenList
          children={children}
          onSelect={(childId) => navigate(`/my-children/${childId}`)}
        />
      )}
    </Box>
  );
}
