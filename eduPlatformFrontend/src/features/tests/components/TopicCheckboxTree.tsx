import { useTranslation } from 'react-i18next';
import { Box, Checkbox, FormControlLabel, Typography, CircularProgress } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { topicApi } from '@/api/topicApi';
import { resolveTranslation } from '@/utils/i18nUtils';
import type { TopicTreeDto } from '@/types/topic';

interface TopicCheckboxTreeProps {
  subjectId: string;
  selected: string[];
  onChange: (selected: string[]) => void;
}

export default function TopicCheckboxTree({ subjectId, selected, onChange }: TopicCheckboxTreeProps) {
  const { t } = useTranslation('test');

  const { data: topics, isLoading } = useQuery({
    queryKey: ['topics', 'tree', subjectId],
    queryFn: async () => {
      const { data } = await topicApi.getTopicTree(subjectId);
      return data.data;
    },
    enabled: !!subjectId,
  });

  const toggleTopic = (id: string) => {
    if (selected.includes(id)) {
      onChange(selected.filter(s => s !== id));
    } else {
      onChange([...selected, id]);
    }
  };

  const renderTree = (nodes: TopicTreeDto[], depth = 0) =>
    nodes.map((node) => (
      <Box key={node.id} sx={{ ml: depth * 3 }}>
        <FormControlLabel
          control={
            <Checkbox
              checked={selected.includes(node.id)}
              onChange={() => toggleTopic(node.id)}
              size="small"
            />
          }
          label={
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="body2">
                {resolveTranslation(node.nameTranslations)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                ({node.questionCount || 0})
              </Typography>
            </Box>
          }
        />
        {node.children && node.children.length > 0 && renderTree(node.children, depth + 1)}
      </Box>
    ));

  if (isLoading) return <CircularProgress size={24} />;
  if (!topics || topics.length === 0) {
    return <Typography color="text.secondary">{t('errors.noTopics')}</Typography>;
  }

  return <Box>{renderTree(topics)}</Box>;
}
