import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useDebounce } from '@/features/subjects/hooks/useDebounce';
import {
  Box,
  Typography,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Tooltip,
  Pagination,
  CircularProgress,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import VisibilityIcon from '@mui/icons-material/Visibility';
import EditIcon from '@mui/icons-material/Edit';
import SecurityIcon from '@mui/icons-material/Security';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import DeleteIcon from '@mui/icons-material/Delete';
import InputAdornment from '@mui/material/InputAdornment';
import { Role, UserStatus } from '@/types/user';
import type { AdminUserDto } from '@/types/admin';
import { useAuth } from '@/hooks/useAuth';
import {
  useAdminUsers,
  useChangeRole,
  useChangeStatus,
  useUnlockUser,
  useDeleteUser,
} from '../hooks/useAdminUsers';
import RoleChangeDialog from '../components/RoleChangeDialog';
import StatusChangeDialog from '../components/StatusChangeDialog';
import DeleteUserDialog from '../components/DeleteUserDialog';
import UnlockUserDialog from '../components/UnlockUserDialog';
import { PageShell } from '@/components/ui';

const STATUS_COLORS: Record<string, 'success' | 'default' | 'error' | 'warning'> = {
  ACTIVE: 'success',
  INACTIVE: 'default',
  BLOCKED: 'error',
  PENDING_VERIFICATION: 'warning',
};

const ROLE_COLORS: Record<string, string> = {
  SUPER_ADMIN: '#9c27b0',
  ADMIN: '#1976d2',
  MODERATOR: '#ed6c02',
  TEACHER: '#2e7d32',
  PARENT: '#0288d1',
  STUDENT: '#607d8b',
};

export default function AdminUsersPage() {
  const { t } = useTranslation('admin');
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();

  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<Role | ''>('');
  const [statusFilter, setStatusFilter] = useState<UserStatus | ''>('');
  const [page, setPage] = useState(0);

  const debouncedSearch = useDebounce(search, 300);

  const params = useMemo(() => ({
    search: debouncedSearch || undefined,
    role: roleFilter || undefined,
    status: statusFilter || undefined,
    page,
    size: 20,
  }), [debouncedSearch, roleFilter, statusFilter, page]);

  const { data, isLoading } = useAdminUsers(params);
  const changeRole = useChangeRole();
  const changeStatus = useChangeStatus();
  const unlockUser = useUnlockUser();
  const deleteUser = useDeleteUser();

  // Dialog state
  const [roleDialogUser, setRoleDialogUser] = useState<AdminUserDto | null>(null);
  const [statusDialogUser, setStatusDialogUser] = useState<AdminUserDto | null>(null);
  const [deleteDialogUser, setDeleteDialogUser] = useState<AdminUserDto | null>(null);
  const [unlockDialogUser, setUnlockDialogUser] = useState<AdminUserDto | null>(null);

  const isSuperAdmin = currentUser?.role === Role.SUPER_ADMIN;

  const handleRoleChange = (role: Role) => {
    if (!roleDialogUser) return;
    changeRole.mutate(
      { id: roleDialogUser.id, data: { role } },
      { onSuccess: () => setRoleDialogUser(null) },
    );
  };

  const handleStatusChange = (status: UserStatus, reason?: string) => {
    if (!statusDialogUser) return;
    changeStatus.mutate(
      { id: statusDialogUser.id, data: { status, reason } },
      { onSuccess: () => setStatusDialogUser(null) },
    );
  };

  const handleUnlock = () => {
    if (!unlockDialogUser) return;
    unlockUser.mutate(unlockDialogUser.id, {
      onSuccess: () => setUnlockDialogUser(null),
    });
  };

  const handleDelete = () => {
    if (!deleteDialogUser) return;
    deleteUser.mutate(deleteDialogUser.id, {
      onSuccess: () => setDeleteDialogUser(null),
    });
  };

  return (
    <PageShell title={t('users.title')} subtitle={t('users.subtitle')}>

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          <TextField
            size="small"
            placeholder={t('users.search')}
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            sx={{ minWidth: 250 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment>
              ),
            }}
          />
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel>{t('users.filterRole')}</InputLabel>
            <Select
              value={roleFilter}
              label={t('users.filterRole')}
              onChange={(e) => { setRoleFilter(e.target.value as Role | ''); setPage(0); }}
            >
              <MenuItem value="">{t('users.all')}</MenuItem>
              {Object.values(Role).map((role) => (
                <MenuItem key={role} value={role}>{role}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel>{t('users.filterStatus')}</InputLabel>
            <Select
              value={statusFilter}
              label={t('users.filterStatus')}
              onChange={(e) => { setStatusFilter(e.target.value as UserStatus | ''); setPage(0); }}
            >
              <MenuItem value="">{t('users.all')}</MenuItem>
              {Object.values(UserStatus).map((status) => (
                <MenuItem key={status} value={status}>
                  {t(`usersByStatus.${status === UserStatus.PENDING_VERIFICATION ? 'pendingVerification' : status.toLowerCase()}`)}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>
      </Paper>

      {/* Table */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : !data || data.content.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">{t('users.noUsers')}</Typography>
        </Paper>
      ) : (
        <>
          <TableContainer component={Paper}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>{t('users.name')}</TableCell>
                  <TableCell>{t('users.emailPhone')}</TableCell>
                  <TableCell>{t('users.role')}</TableCell>
                  <TableCell>{t('users.status')}</TableCell>
                  <TableCell>{t('users.lastLogin')}</TableCell>
                  <TableCell align="right">{t('users.actions')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.content.map((user) => (
                  <TableRow key={user.id} hover>
                    <TableCell>
                      <Typography variant="body2" fontWeight={500}>
                        {user.firstName} {user.lastName}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{user.email || user.phone || '—'}</Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={user.role}
                        size="small"
                        sx={{ bgcolor: ROLE_COLORS[user.role] || '#607d8b', color: '#fff', fontWeight: 600, fontSize: '0.7rem' }}
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={t(`usersByStatus.${user.status === UserStatus.PENDING_VERIFICATION ? 'pendingVerification' : user.status.toLowerCase()}`)}
                        size="small"
                        color={STATUS_COLORS[user.status] || 'default'}
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption" color="text.secondary">
                        {user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : '—'}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title={t('users.viewDetails')}>
                        <IconButton size="small" onClick={() => navigate(`/admin/users/${user.id}`)}>
                          <VisibilityIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {user.role !== Role.SUPER_ADMIN && (
                        <>
                          <Tooltip title={t('users.changeRole')}>
                            <IconButton size="small" onClick={() => setRoleDialogUser(user)}>
                              <SecurityIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title={t('users.changeStatus')}>
                            <IconButton size="small" onClick={() => setStatusDialogUser(user)}>
                              <EditIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          {user.failedLoginAttempts > 0 && (
                            <Tooltip title={t('users.unlock')}>
                              <IconButton size="small" color="warning" onClick={() => setUnlockDialogUser(user)}>
                                <LockOpenIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                          {isSuperAdmin && (
                            <Tooltip title={t('users.deleteUser')}>
                              <IconButton size="small" color="error" onClick={() => setDeleteDialogUser(user)}>
                                <DeleteIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                        </>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          {data.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
              <Pagination
                count={data.totalPages}
                page={page + 1}
                onChange={(_, p) => setPage(p - 1)}
                color="primary"
              />
            </Box>
          )}
        </>
      )}

      {/* Dialogs */}
      <RoleChangeDialog
        open={!!roleDialogUser}
        user={roleDialogUser}
        loading={changeRole.isPending}
        onClose={() => setRoleDialogUser(null)}
        onConfirm={handleRoleChange}
      />
      <StatusChangeDialog
        open={!!statusDialogUser}
        user={statusDialogUser}
        loading={changeStatus.isPending}
        onClose={() => setStatusDialogUser(null)}
        onConfirm={handleStatusChange}
      />
      <UnlockUserDialog
        open={!!unlockDialogUser}
        user={unlockDialogUser}
        loading={unlockUser.isPending}
        onClose={() => setUnlockDialogUser(null)}
        onConfirm={handleUnlock}
      />
      <DeleteUserDialog
        open={!!deleteDialogUser}
        user={deleteDialogUser}
        loading={deleteUser.isPending}
        onClose={() => setDeleteDialogUser(null)}
        onConfirm={handleDelete}
      />
    </PageShell>
  );
}
