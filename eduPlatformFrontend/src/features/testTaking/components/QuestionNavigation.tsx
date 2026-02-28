import { Box, Button } from '@mui/material';

interface QuestionNavigationProps {
  totalQuestions: number;
  currentIndex: number;
  answeredSet: Set<string>;
  questionIds: string[];
  onNavigate: (index: number) => void;
}

export default function QuestionNavigation({
  totalQuestions,
  currentIndex,
  answeredSet,
  questionIds,
  onNavigate,
}: QuestionNavigationProps) {
  return (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
      {Array.from({ length: totalQuestions }, (_, i) => {
        const isAnswered = answeredSet.has(questionIds[i]);
        const isCurrent = i === currentIndex;
        return (
          <Button
            key={i}
            size="small"
            onClick={() => onNavigate(i)}
            sx={{
              minWidth: 36,
              height: 36,
              p: 0,
              fontWeight: isCurrent ? 800 : 400,
              bgcolor: isCurrent ? 'primary.main' : isAnswered ? '#e8f5e9' : 'background.paper',
              color: isCurrent ? 'white' : isAnswered ? 'success.dark' : 'text.disabled',
              border: '2px solid',
              borderColor: isCurrent ? 'primary.dark' : isAnswered ? 'success.main' : 'divider',
              boxShadow: isCurrent ? 2 : 0,
              '&:hover': { opacity: 0.85 },
            }}
          >
            {i + 1}
          </Button>
        );
      })}
    </Box>
  );
}
