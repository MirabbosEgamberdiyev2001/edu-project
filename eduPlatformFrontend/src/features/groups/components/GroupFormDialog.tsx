import { useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  CircularProgress,
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { groupSchema, type GroupFormData } from '../schemas/groupSchema';
import type { GroupDto, CreateGroupRequest, UpdateGroupRequest } from '@/types/group';
import MultiLangInput from '@/features/tests/components/MultiLangInput';

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
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<GroupFormData>({
    resolver: zodResolver(groupSchema),
    defaultValues: {
      nameTranslations: {},
      descriptionTranslations: {},
    },
  });

  useEffect(() => {
    if (open) {
      if (group) {
        reset({
          nameTranslations: group.nameTranslations ?? {},
          descriptionTranslations: group.descriptionTranslations ?? {},
        });
      } else {
        reset({
          nameTranslations: {},
          descriptionTranslations: {},
        });
      }
    }
  }, [open, group, reset]);

  const onFormSubmit = (data: GroupFormData) => {
    const payload: CreateGroupRequest | UpdateGroupRequest = {
      nameTranslations: data.nameTranslations,
      ...(data.descriptionTranslations &&
        Object.values(data.descriptionTranslations).some((v) => v.trim()) && {
          descriptionTranslations: data.descriptionTranslations,
        }),
    };
    onSubmit(payload);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? t('editGroup') : t('createGroup')}</DialogTitle>
      <DialogContent sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Controller
          name="nameTranslations"
          control={control}
          render={({ field }) => (
            <MultiLangInput
              label={t('name')}
              value={field.value ?? {}}
              onChange={field.onChange}
              required
            />
          )}
        />

        <Controller
          name="descriptionTranslations"
          control={control}
          render={({ field }) => (
            <MultiLangInput
              label={t('description')}
              value={field.value ?? {}}
              onChange={field.onChange}
            />
          )}
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
