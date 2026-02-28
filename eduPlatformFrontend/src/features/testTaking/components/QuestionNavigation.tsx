import { Box, Button, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

interface QuestionNavigationProps {
  totalQuestions: number;
  currentIndex: number;
  answeredSet: Set<string>;
  questionIds: string[];
  onNavigate: (index: number) => void;
}

/** Three semantic states for a question button */
type QState = 'current' | 'answered' | 'unanswered';

const STATE_STYLES: Record<QState, object> = {
  current: {
    bgcolor: 'primary.main',
    color: 'primary.contrastText',
    borderColor: 'primary.dark',
    fontWeight: 700,
    boxShadow: 1,
  },
  answered: {
    bgcolor: 'rgba(46,125,50,0.10)',
    color: 'success.dark',
    borderColor: 'success.light',
    fontWeight: 500,
    boxShadow: 0,
  },
  unanswered: {
    bgcolor: 'background.paper',
    color: 'text.secondary',
    borderColor: 'divider',
    fontWeight: 400,
    boxShadow: 0,
  },
};

/** Small dot + label legend item */
function LegendDot({ state, label }: { state: QState; label: string }) {
  const dotStyles = {
    current: { bgcolor: 'primary.main', borderColor: 'primary.dark' },
    answered: { bgcolor: 'rgba(46,125,50,0.10)', borderColor: 'success.light' },
    unanswered: { bgcolor: 'background.paper', borderColor: 'divider' },
  }[state];

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
      <Box
        sx={{
          width: 10,
          height: 10,
          borderRadius: 0.5,
          border: '1.5px solid',
          flexShrink: 0,
          ...dotStyles,
        }}
      />
      <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.6875rem' }}>
        {label}
      </Typography>
    </Box>
  );
}

export default function QuestionNavigation({
  totalQuestions,
  currentIndex,
  answeredSet,
  questionIds,
  onNavigate,
}: QuestionNavigationProps) {
  const { t } = useTranslation('testTaking');

  return (
    <Box>
      {/* Scrollable button grid */}
      <Box
        role="navigation"
        aria-label={t('exam.navLabel')}
        sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}
      >
        {Array.from({ length: totalQuestions }, (_, i) => {
          const isAnswered = answeredSet.has(questionIds[i]);
          const isCurrent = i === currentIndex;
          const state: QState = isCurrent ? 'current' : isAnswered ? 'answered' : 'unanswered';
          const styles = STATE_STYLES[state];

          return (
            <Button
              key={i}
              size="small"
              onClick={() => onNavigate(i)}
              aria-label={t('exam.questionBtn', { number: i + 1 })}
              aria-current={isCurrent ? 'step' : undefined}
              aria-pressed={isAnswered}
              sx={{
                minWidth: 34,
                height: 34,
                p: 0,
                border: '1.5px solid',
                borderRadius: 1,
                fontSize: '0.8125rem',
                transition: 'background-color 0.15s ease, border-color 0.15s ease',
                '&:hover': { opacity: 0.8, filter: 'brightness(0.96)' },
                ...styles,
              }}
            >
              {i + 1}
            </Button>
          );
        })}
      </Box>

      {/* Legend */}
      <Box sx={{ display: 'flex', gap: { xs: 1.5, sm: 2 }, mt: 0.75, flexWrap: 'wrap' }}>
        <LegendDot state="current" label={t('exam.legend.current')} />
        <LegendDot state="answered" label={t('exam.legend.answered')} />
        <LegendDot state="unanswered" label={t('exam.legend.unanswered')} />
      </Box>
    </Box>
  );
}
