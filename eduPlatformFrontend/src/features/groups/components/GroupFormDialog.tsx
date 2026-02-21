import { useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  CircularProgress,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { groupSchema, type GroupFormData } from '../schemas/groupSchema';
import type { GroupDto, CreateGroupRequest, UpdateGroupRequest } from '@/types/group';

interface GroupFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateGroupRequest | UpdateGroupRequest) => void;
  group?: GroupDto | null;
  isPending: boolean;
}

export default function GroupFormDialog({ open, onClose, onSubmit, group, isPending }: GroupFormDialogProps) {
  const { t } = useTranslation('group');
  const isEdit = Boolean(group);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<GroupFormData>({
    resolver: zodResolver(groupSchema),
    defaultValues: {
      name: '',
      description: '',
    },
  });

  useEffect(() => {
    if (open) {
      if (group) {
        reset({
          name: group.name,
          description: group.description || '',
        });
      } else {
        reset({
          name: '',
          description: '',
        });
      }
    }
  }, [open, group, reset]);

  const onFormSubmit = (data: GroupFormData) => {
    const payload: CreateGroupRequest | UpdateGroupRequest = {
      name: data.name,
      ...(data.description?.trim() && { description: data.description.trim() }),
    };
    onSubmit(payload);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? t('editGroup') : t('createGroup')}</DialogTitle>
      <DialogContent>
        <TextField
          label={t('name')}
          placeholder={t('name')}
          fullWidth
          required
          error={Boolean(errors.name)}
          helperText={errors.name?.message}
          sx={{ mt: 1, mb: 2 }}
          {...register('name')}
        />

        <TextField
          label={t('description')}
          placeholder={t('description')}
          fullWidth
          multiline
          rows={3}
          error={Boolean(errors.description)}
          helperText={errors.description?.message}
          {...register('description')}
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>{t('common:cancel')}</Button>
        <Button
          variant="contained"
          onClick={handleSubmit(onFormSubmit)}
          disabled={isPending}
        >
          {isPending ? <CircularProgress size={20} /> : (isEdit ? t('common:save') : t('createGroup'))}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
