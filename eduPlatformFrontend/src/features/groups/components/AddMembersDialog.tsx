import { useState, useMemo } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  InputAdornment,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Checkbox,
  Typography,
  Box,
  CircularProgress,
  Chip,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { groupApi } from '@/api/groupApi';
import { useAddMembers, useGroupMembers } from '../hooks/useGroupMembers';
import { useDebounce } from '@/features/subjects/hooks/useDebounce';

interface AddMembersDialogProps {
  open: boolean;
  onClose: () => void;
  groupId: string;
  onSuccess: () => void;
}

export default function AddMembersDialog({ open, onClose, groupId, onSuccess }: AddMembersDialogProps) {
  const { t } = useTranslation('group');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<string[]>([]);
  const debouncedSearch = useDebounce(search, 300);

  const addMembers = useAddMembers(groupId);
  const { data: existingMembers } = useGroupMembers(open ? groupId : undefined);

  const { data: studentsData, isLoading } = useQuery({
    queryKey: ['groups', 'students', 'search', debouncedSearch],
    queryFn: async () => {
      const { data } = await groupApi.searchStudents({
        search: debouncedSearch || undefined,
        size: 50,
      });
      return data.data;
    },
    enabled: open,
  });

  // Filter out students who are already members of this group
  const existingMemberIds = useMemo(
    () => new Set(existingMembers?.map((m) => m.studentId) || []),
    [existingMembers],
  );

  const students = useMemo(
    () => (studentsData?.content || []).filter((s) => !existingMemberIds.has(s.id)),
    [studentsData, existingMemberIds],
  );

  const handleToggle = (studentId: string) => {
    setSelected((prev) =>
      prev.includes(studentId)
        ? prev.filter((id) => id !== studentId)
        : [...prev, studentId],
    );
  };

  const handleSubmit = () => {
    if (selected.length === 0) return;
    addMembers.mutate(
      { studentIds: selected },
      {
        onSuccess: () => {
          setSelected([]);
          setSearch('');
          onSuccess();
          onClose();
        },
      },
    );
  };

  const handleClose = () => {
    setSelected([]);
    setSearch('');
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>{t('addMembers')}</DialogTitle>
      <DialogContent>
        <TextField
          size="small"
          placeholder={t('searchStudents')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          fullWidth
          sx={{ mb: 2, mt: 1 }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon fontSize="small" />
              </InputAdornment>
            ),
          }}
        />

        {selected.length > 0 && (
          <Box sx={{ mb: 2 }}>
            <Chip
              label={t('selectedStudents', { count: selected.length })}
              color="primary"
              size="small"
            />
          </Box>
        )}

        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        ) : students.length > 0 ? (
          <List dense sx={{ maxHeight: 300, overflow: 'auto' }}>
            {students.map((student) => (
              <ListItem key={student.id} disablePadding>
                <ListItemButton onClick={() => handleToggle(student.id)} dense>
                  <ListItemIcon sx={{ minWidth: 36 }}>
                    <Checkbox
                      edge="start"
                      checked={selected.includes(student.id)}
                      tabIndex={-1}
                      disableRipple
                      size="small"
                    />
                  </ListItemIcon>
                  <ListItemText
                    primary={`${student.firstName} ${student.lastName}`}
                    secondary={student.email || student.phone || ''}
                  />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        ) : (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Typography variant="body2" color="text.secondary">
              {t('noStudentsFound')}
            </Typography>
          </Box>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={handleClose}>{t('common:cancel')}</Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={addMembers.isPending || selected.length === 0}
        >
          {addMembers.isPending ? (
            <CircularProgress size={20} />
          ) : (
            t('addMembers')
          )}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
