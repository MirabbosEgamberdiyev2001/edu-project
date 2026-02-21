import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { Role } from '@/types/user';
import type { AdminUserDto } from '@/types/admin';

interface RoleChangeDialogProps {
  open: boolean;
  user: AdminUserDto | null;
  loading?: boolean;
  onClose: () => void;
  onConfirm: (role: Role) => void;
}

const ASSIGNABLE_ROLES = [
  Role.STUDENT,
  Role.PARENT,
  Role.TEACHER,
  Role.MODERATOR,
  Role.ADMIN,
];

export default function RoleChangeDialog({ open, user, loading, onClose, onConfirm }: RoleChangeDialogProps) {
  const { t } = useTranslation('admin');
  const [selectedRole, setSelectedRole] = useState<Role | ''>('');

  const handleEnter = () => {
    setSelectedRole(user?.role ?? '');
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth TransitionProps={{ onEnter: handleEnter }}>
      <DialogTitle>{t('changeRole.title')}</DialogTitle>
      <DialogContent>
        <p style={{ margin: '0 0 16px' }}>
          {t('changeRole.description', { name: `${user?.firstName} ${user?.lastName}` })}
        </p>
        <FormControl fullWidth size="small">
          <InputLabel>{t('changeRole.selectRole')}</InputLabel>
          <Select
            value={selectedRole}
            label={t('changeRole.selectRole')}
            onChange={(e) => setSelectedRole(e.target.value as Role)}
          >
            {ASSIGNABLE_ROLES.map((role) => (
              <MenuItem key={role} value={role}>
                {role}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common:cancel', 'Cancel')}</Button>
        <Button
          variant="contained"
          onClick={() => selectedRole && onConfirm(selectedRole as Role)}
          disabled={!selectedRole || selectedRole === user?.role || loading}
        >
          {t('changeRole.confirm')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
