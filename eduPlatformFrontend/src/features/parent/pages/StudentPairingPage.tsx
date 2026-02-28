import { Box, Grid, Paper, Typography } from '@mui/material';
import QrCode2Icon from '@mui/icons-material/QrCode2';
import IosShareIcon from '@mui/icons-material/IosShare';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import { useTranslation } from 'react-i18next';
import { useParentMutations } from '../hooks/useParentMutations';
import { PageShell } from '@/components/ui';
import PairingCodeDisplay from '../components/PairingCodeDisplay';
import ParentsList from '../components/ParentsList';

const STEP_ICONS = [
  <QrCode2Icon sx={{ fontSize: 22 }} />,
  <IosShareIcon sx={{ fontSize: 22 }} />,
  <CheckCircleOutlineIcon sx={{ fontSize: 22 }} />,
];

export default function StudentPairingPage() {
  const { t } = useTranslation('parent');
  const { generatePairingCode } = useParentMutations();

  const hasCode = !!generatePairingCode.data?.data?.data;

  const steps = [
    { label: t('step1'), desc: t('step1desc'), done: hasCode },
    { label: t('step2'), desc: t('step2desc'), done: hasCode },
    { label: t('step3'), desc: t('step3desc'), done: false },
  ];

  return (
    <PageShell title={t('pairingTitle')} subtitle={t('pairingSubtitle')}>

      {/* How-to steps guide */}
      <Paper sx={{ p: 2.5, mb: 3 }}>
        <Typography
          variant="overline"
          color="text.secondary"
          sx={{ display: 'block', mb: 2, letterSpacing: 1 }}
        >
          {t('howToPair')}
        </Typography>
        <Box
          sx={{
            display: 'flex',
            flexDirection: { xs: 'column', sm: 'row' },
            gap: { xs: 2, sm: 0 },
          }}
        >
          {steps.map((step, i) => (
            <Box
              key={i}
              sx={{
                flex: 1,
                display: 'flex',
                flexDirection: { xs: 'row', sm: 'column' },
                alignItems: { xs: 'center', sm: 'center' },
                textAlign: { xs: 'left', sm: 'center' },
                position: 'relative',
                px: { sm: 2 },
                gap: { xs: 1.5, sm: 0 },
              }}
            >
              {/* connector line (desktop only) */}
              {i < steps.length - 1 && (
                <Box
                  sx={{
                    display: { xs: 'none', sm: 'block' },
                    position: 'absolute',
                    top: 24,
                    left: '60%',
                    width: '80%',
                    height: 2,
                    bgcolor: step.done ? 'primary.main' : 'divider',
                    transition: 'background-color 0.3s',
                    zIndex: 0,
                  }}
                />
              )}

              {/* Step circle */}
              <Box
                sx={{
                  width: 48,
                  height: 48,
                  borderRadius: '50%',
                  flexShrink: 0,
                  bgcolor: step.done ? 'primary.main' : 'action.hover',
                  color: step.done ? 'white' : 'text.secondary',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  zIndex: 1,
                  position: 'relative',
                  mb: { sm: 1.5 },
                  transition: 'background-color 0.3s',
                  border: '2px solid',
                  borderColor: step.done ? 'primary.main' : 'divider',
                }}
              >
                {STEP_ICONS[i]}
              </Box>

              {/* Step text */}
              <Box>
                <Typography variant="body2" fontWeight={700} sx={{ mb: 0.25 }}>
                  {step.label}
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ lineHeight: 1.4, display: 'block' }}>
                  {step.desc}
                </Typography>
              </Box>
            </Box>
          ))}
        </Box>
      </Paper>

      {/* Main content */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={7}>
          <PairingCodeDisplay
            onGenerate={() => generatePairingCode.mutate()}
            data={generatePairingCode.data?.data?.data ?? null}
            isPending={generatePairingCode.isPending}
          />
        </Grid>
        <Grid item xs={12} md={5}>
          <ParentsList />
        </Grid>
      </Grid>
    </PageShell>
  );
}
