import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Checkbox, FormControlLabel, Typography, CircularProgress, Button } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useQuery } from '@tanstack/react-query';
import { topicApi } from '@/api/topicApi';
import { resolveTranslation } from '@/utils/i18nUtils';
import type { TopicTreeDto } from '@/types/topic';
import type { CreateTopicRequest } from '@/types/topic';
import TopicFormDialog from '@/features/topics/components/TopicFormDialog';
import { useTopicMutations } from '@/features/topics/hooks/useTopicMutations';

interface TopicCheckboxTreeProps {
  subjectId: string;
  gradeLevel: number | null;
  selected: string[];
  onChange: (selected: string[]) => void;
}

export default function TopicCheckboxTree({ subjectId, gradeLevel, selected, onChange }: TopicCheckboxTreeProps) {
  const { t } = useTranslation('test');
  const { t: tTopic } = useTranslation('topic');

  const [topicFormOpen, setTopicFormOpen] = useState(false);

  const { create: createTopic } = useTopicMutations(subjectId);

  const { data: topics, isLoading } = useQuery({
    queryKey: ['topics', subjectId, gradeLevel],
    queryFn: async () => {
      const { data } = await topicApi.getTopicTree(subjectId, gradeLevel);
      return data.data;
    },
    enabled: !!subjectId,
  });

  // Collect all descendant IDs (including self)
  const getAllIds = (node: TopicTreeDto): string[] => {
    const ids = [node.id];
    if (node.children) {
      node.children.forEach(child => ids.push(...getAllIds(child)));
    }
    return ids;
  };

  // Find a node in the tree by ID
  const findNode = (nodes: TopicTreeDto[], id: string): TopicTreeDto | null => {
    for (const n of nodes) {
      if (n.id === id) return n;
      if (n.children) {
        const found = findNode(n.children, id);
        if (found) return found;
      }
    }
    return null;
  };

  const toggleTopic = (id: string) => {
    const node = findNode(topics || [], id);
    if (!node) return;

    const allIds = getAllIds(node);

    if (selected.includes(id)) {
      // Deselect this node and all its descendants
      onChange(selected.filter(s => !allIds.includes(s)));
    } else {
      // Select this node and all its descendants
      const newIds = allIds.filter(i => !selected.includes(i));
      onChange([...selected, ...newIds]);
    }
  };

  // Check if some (but not all) descendants are selected — for indeterminate state
  const getCheckState = (node: TopicTreeDto): 'checked' | 'unchecked' | 'indeterminate' => {
    const allIds = getAllIds(node);
    const selectedCount = allIds.filter(id => selected.includes(id)).length;
    if (selectedCount === 0) return 'unchecked';
    if (selectedCount === allIds.length) return 'checked';
    return 'indeterminate';
  };

  const renderTree = (nodes: TopicTreeDto[], depth = 0) =>
    nodes.map((node) => {
      const state = getCheckState(node);
      return (
        <Box key={node.id} sx={{ ml: depth * 3 }}>
          <FormControlLabel
            control={
              <Checkbox
                checked={state === 'checked'}
                indeterminate={state === 'indeterminate'}
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
      );
    });

  if (isLoading) return <CircularProgress size={24} />;

  if (!topics || topics.length === 0) {
    return (
      <Box>
        <Typography color="text.secondary" sx={{ mb: 1 }}>{tTopic('empty')}</Typography>
        <Button
          variant="outlined"
          size="small"
          startIcon={<AddIcon />}
          onClick={() => setTopicFormOpen(true)}
        >
          {tTopic('create')}
        </Button>
        <TopicFormDialog
          open={topicFormOpen}
          onClose={() => setTopicFormOpen(false)}
          onSubmit={(data) => {
            const createData = { ...data, gradeLevel } as CreateTopicRequest;
            createTopic.mutate(createData, {
              onSuccess: () => {
                setTopicFormOpen(false);
              },
            });
          }}
          isPending={createTopic.isPending}
        />
      </Box>
    );
  }

  return (
    <Box>
      {renderTree(topics)}
      <Button
        size="small"
        startIcon={<AddIcon />}
        onClick={() => setTopicFormOpen(true)}
        sx={{ mt: 1 }}
      >
        {tTopic('create')}
      </Button>
      <TopicFormDialog
        open={topicFormOpen}
        onClose={() => setTopicFormOpen(false)}
        onSubmit={(data) => {
          const createData = { ...data, gradeLevel } as CreateTopicRequest;
          createTopic.mutate(createData, {
            onSuccess: () => {
              setTopicFormOpen(false);
            },
          });
        }}
        isPending={createTopic.isPending}
      />
    </Box>
  );
}
