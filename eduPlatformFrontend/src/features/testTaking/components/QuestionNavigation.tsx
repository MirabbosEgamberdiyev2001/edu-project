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
            variant={isCurrent ? 'contained' : isAnswered ? 'outlined' : 'text'}
            size="small"
            onClick={() => onNavigate(i)}
            sx={{
              minWidth: 36,
              height: 36,
              p: 0,
              bgcolor: isCurrent ? 'primary.main' : isAnswered ? 'success.light' : undefined,
              color: isCurrent ? 'white' : isAnswered ? 'success.dark' : 'text.secondary',
              borderColor: isAnswered ? 'success.main' : undefined,
            }}
          >
            {i + 1}
          </Button>
        );
      })}
    </Box>
  );
}
