import {
  Paper,
  Typography,
  Box,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  ListItemIcon,
  LinearProgress,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import CancelIcon from '@mui/icons-material/Cancel';
import MonitorIcon from '@mui/icons-material/Monitor';
import BarChartIcon from '@mui/icons-material/BarChart';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { AssignmentDto } from '@/types/assignment';
import { AssignmentStatus } from '@/types/assignment';

interface AssignmentCardProps {
  assignment: AssignmentDto;
  onEdit: (assignment: AssignmentDto) => void;
  onDelete: (assignment: AssignmentDto) => void;
  onActivate: (assignment: AssignmentDto) => void;
  onCancel: (assignment: AssignmentDto) => void;
  onMonitor: (assignment: AssignmentDto) => void;
  onResults: (assignment: AssignmentDto) => void;
}

const STATUS_COLORS: Record<string, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  DRAFT: 'default',
  ACTIVE: 'info',
  COMPLETED: 'success',
  CANCELLED: 'error',
};

export default function AssignmentCard({
  assignment,
  onEdit,
  onDelete,
  onActivate,
  onCancel,
  onMonitor,
  onResults,
}: AssignmentCardProps) {
  const { t } = useTranslation('assignment');
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const progress = assignment.totalStudents > 0
    ? (assignment.completedStudents / assignment.totalStudents) * 100
    : 0;

  return (
    <Paper sx={{ p: 2.5, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography variant="subtitle1" fontWeight={600} noWrap>
            {assignment.title}
          </Typography>
          <Typography variant="body2" color="text.secondary" noWrap>
            {assignment.groupName}
          </Typography>
        </Box>
        <IconButton size="small" onClick={(e) => setAnchorEl(e.currentTarget)}>
          <MoreVertIcon fontSize="small" />
        </IconButton>
      </Box>

      <Box sx={{ display: 'flex', gap: 1, mb: 1.5, flexWrap: 'wrap' }}>
        <Chip
          label={t(`status.${assignment.status}`)}
          size="small"
          color={STATUS_COLORS[assignment.status] || 'default'}
        />
        {assignment.durationMinutes && (
          <Chip label={`${assignment.durationMinutes} min`} size="small" variant="outlined" />
        )}
      </Box>

      <Box sx={{ flex: 1 }} />

      <Box sx={{ mb: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
          <Typography variant="caption" color="text.secondary">
            {t('progress')}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {assignment.completedStudents}/{assignment.totalStudents}
          </Typography>
        </Box>
        <LinearProgress variant="determinate" value={progress} sx={{ borderRadius: 1 }} />
      </Box>

      {assignment.endDate && (
        <Typography variant="caption" color="text.secondary">
          {t('dueDate')}: {new Date(assignment.endDate).toLocaleDateString()}
        </Typography>
      )}

      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
        {assignment.status === AssignmentStatus.DRAFT && (
          <MenuItem onClick={() => { onActivate(assignment); setAnchorEl(null); }}>
            <ListItemIcon><PlayArrowIcon fontSize="small" /></ListItemIcon>
            {t('activate')}
          </MenuItem>
        )}
        {assignment.status === AssignmentStatus.ACTIVE && (
          <MenuItem onClick={() => { onMonitor(assignment); setAnchorEl(null); }}>
            <ListItemIcon><MonitorIcon fontSize="small" /></ListItemIcon>
            {t('monitor')}
          </MenuItem>
        )}
        {assignment.status === AssignmentStatus.ACTIVE && (
          <MenuItem onClick={() => { onCancel(assignment); setAnchorEl(null); }}>
            <ListItemIcon><CancelIcon fontSize="small" /></ListItemIcon>
            {t('cancelAssignment')}
          </MenuItem>
        )}
        {(assignment.status === AssignmentStatus.COMPLETED || assignment.status === AssignmentStatus.CANCELLED) && (
          <MenuItem onClick={() => { onResults(assignment); setAnchorEl(null); }}>
            <ListItemIcon><BarChartIcon fontSize="small" /></ListItemIcon>
            {t('viewResults')}
          </MenuItem>
        )}
        {assignment.status === AssignmentStatus.DRAFT && (
          <MenuItem onClick={() => { onEdit(assignment); setAnchorEl(null); }}>
            <ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>
            {t('edit')}
          </MenuItem>
        )}
        <MenuItem onClick={() => { onDelete(assignment); setAnchorEl(null); }} sx={{ color: 'error.main' }}>
          <ListItemIcon><DeleteIcon fontSize="small" color="error" /></ListItemIcon>
          {t('deleteAssignment')}
        </MenuItem>
      </Menu>
    </Paper>
  );
}
