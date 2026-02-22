import { useState } from 'react';
import {
  Box,
  Typography,
  IconButton,
  Collapse,
  Paper,
  Button,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Chip,
  CircularProgress,
  Tooltip,
  Alert,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import QuizIcon from '@mui/icons-material/Quiz';
import FolderIcon from '@mui/icons-material/Folder';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import { useTranslation } from 'react-i18next';
import type { TopicTreeDto, CreateTopicRequest, UpdateTopicRequest } from '@/types/topic';
import { useTopicTree } from '../hooks/useTopicTree';
import { useTopicMutations } from '../hooks/useTopicMutations';
import { resolveTranslation } from '@/utils/i18nUtils';
import TopicFormDialog from './TopicFormDialog';
import TopicDeleteDialog from './TopicDeleteDialog';

interface TopicTreeViewProps {
  subjectId: string;
}

export default function TopicTreeView({ subjectId }: TopicTreeViewProps) {
  const { t } = useTranslation('topic');
  const { data: tree, isLoading, isError } = useTopicTree(subjectId);
  const { create, update, remove } = useTopicMutations(subjectId);

  const [formOpen, setFormOpen] = useState(false);
  const [editTopic, setEditTopic] = useState<TopicTreeDto | null>(null);
  const [parentIdForCreate, setParentIdForCreate] = useState<string | null>(null);
  const [deleteTopic, setDeleteTopic] = useState<TopicTreeDto | null>(null);

  const handleCreate = (parentId?: string) => {
    setEditTopic(null);
    setParentIdForCreate(parentId || null);
    setFormOpen(true);
  };

  const handleEdit = (topic: TopicTreeDto) => {
    setEditTopic(topic);
    setParentIdForCreate(null);
    setFormOpen(true);
  };

  const handleFormSubmit = (formData: CreateTopicRequest | UpdateTopicRequest) => {
    if (editTopic) {
      update.mutate({ id: editTopic.id, data: formData as UpdateTopicRequest }, { onSuccess: () => setFormOpen(false) });
    } else {
      create.mutate(formData as CreateTopicRequest, { onSuccess: () => setFormOpen(false) });
    }
  };

  const handleDeleteConfirm = () => {
    if (deleteTopic) {
      remove.mutate(deleteTopic.id, { onSuccess: () => setDeleteTopic(null) });
    }
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (isError) {
    return <Alert severity="error">{t('common:error')}</Alert>;
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6" fontWeight={600}>{t('title')}</Typography>
        <Button
          variant="outlined"
          size="small"
          startIcon={<AddIcon />}
          onClick={() => handleCreate()}
        >
          {t('addRoot')}
        </Button>
      </Box>

      {tree && tree.length > 0 ? (
        <Paper variant="outlined" sx={{ p: 1 }}>
          {tree.map((topic) => (
            <TopicNode
              key={topic.id}
              topic={topic}
              onEdit={handleEdit}
              onDelete={(t) => setDeleteTopic(t)}
              onAddChild={(parentId) => handleCreate(parentId)}
            />
          ))}
        </Paper>
      ) : (
        <Box sx={{ textAlign: 'center', py: 4 }}>
          <FolderIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
          <Typography variant="body1" color="text.secondary">{t('empty')}</Typography>
          <Typography variant="body2" color="text.disabled">{t('emptyDescription')}</Typography>
        </Box>
      )}

      <TopicFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleFormSubmit}
        topic={editTopic}
        parentId={parentIdForCreate}
        isPending={create.isPending || update.isPending}
      />

      <TopicDeleteDialog
        open={Boolean(deleteTopic)}
        onClose={() => setDeleteTopic(null)}
        onConfirm={handleDeleteConfirm}
        topic={deleteTopic}
        isPending={remove.isPending}
      />
    </Box>
  );
}

// --- Tree Node ---

interface TopicNodeProps {
  topic: TopicTreeDto;
  onEdit: (topic: TopicTreeDto) => void;
  onDelete: (topic: TopicTreeDto) => void;
  onAddChild: (parentId: string) => void;
}

function TopicNode({ topic, onEdit, onDelete, onAddChild }: TopicNodeProps) {
  const { t } = useTranslation('topic');
  const [expanded, setExpanded] = useState(true);
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);
  const hasChildren = topic.children && topic.children.length > 0;

  const displayName = resolveTranslation(topic.nameTranslations) || topic.name;

  return (
    <Box sx={{ ml: (topic.level - 1) * 3 }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          gap: 0.5,
          py: 0.75,
          px: 1,
          borderRadius: 1,
          '&:hover': { bgcolor: 'action.hover' },
        }}
      >
        {/* Expand/Collapse */}
        <IconButton
          size="small"
          onClick={() => setExpanded(!expanded)}
          sx={{ visibility: hasChildren ? 'visible' : 'hidden', width: 28, height: 28 }}
        >
          {expanded ? <ExpandMoreIcon fontSize="small" /> : <ChevronRightIcon fontSize="small" />}
        </IconButton>

        {/* Folder icon */}
        {hasChildren && expanded ? (
          <FolderOpenIcon fontSize="small" color="primary" />
        ) : (
          <FolderIcon fontSize="small" color="action" />
        )}

        {/* Name */}
        <Typography
          variant="body2"
          fontWeight={500}
          sx={{ flex: 1, opacity: topic.isActive ? 1 : 0.5 }}
        >
          {displayName}
        </Typography>

        {/* Question count */}
        {topic.questionCount > 0 && (
          <Chip
            icon={<QuizIcon sx={{ fontSize: 14 }} />}
            label={topic.questionCount}
            size="small"
            variant="outlined"
            sx={{ height: 22, '& .MuiChip-label': { px: 0.5, fontSize: 12 } }}
          />
        )}

        {/* Actions */}
        <Tooltip title={t('addSubtopic')}>
          <IconButton size="small" onClick={() => onAddChild(topic.id)} sx={{ width: 28, height: 28 }}>
            <AddIcon fontSize="small" />
          </IconButton>
        </Tooltip>

        <IconButton size="small" onClick={(e) => setAnchorEl(e.currentTarget)} sx={{ width: 28, height: 28 }}>
          <MoreVertIcon fontSize="small" />
        </IconButton>

        <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
          <MenuItem onClick={() => { setAnchorEl(null); onEdit(topic); }}>
            <ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>
            <ListItemText>{t('edit')}</ListItemText>
          </MenuItem>
          <MenuItem onClick={() => { setAnchorEl(null); onDelete(topic); }} sx={{ color: 'error.main' }}>
            <ListItemIcon><DeleteIcon fontSize="small" color="error" /></ListItemIcon>
            <ListItemText>{t('delete')}</ListItemText>
          </MenuItem>
        </Menu>
      </Box>

      {/* Children */}
      {hasChildren && (
        <Collapse in={expanded}>
          {topic.children.map((child) => (
            <TopicNode
              key={child.id}
              topic={child}
              onEdit={onEdit}
              onDelete={onDelete}
              onAddChild={onAddChild}
            />
          ))}
        </Collapse>
      )}
    </Box>
  );
}
