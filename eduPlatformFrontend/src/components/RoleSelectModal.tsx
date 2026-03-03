import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Grid,
} from '@mui/material';
import SchoolIcon from '@mui/icons-material/School';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import FamilyRestroomIcon from '@mui/icons-material/FamilyRestroom';
import LoginIcon from '@mui/icons-material/Login';
import PersonAddIcon from '@mui/icons-material/PersonAdd';

interface RoleSelectModalProps {
  open: boolean;
  onClose: () => void;
}

const ROLES = [
  {
    key: 'teacher' as const,
    roleParam: 'TEACHER',
    Icon: SchoolIcon,
    color: '#1565c0',
    bg: '#eff6ff',
    border: '#bfdbfe',
    selectedBorder: '#1565c0',
  },
  {
    key: 'student' as const,
    roleParam: 'STUDENT',
    Icon: MenuBookIcon,
    color: '#166534',
    bg: '#f0fdf4',
    border: '#bbf7d0',
    selectedBorder: '#166534',
  },
  {
    key: 'parent' as const,
    roleParam: 'PARENT',
    Icon: FamilyRestroomIcon,
    color: '#92400e',
    bg: '#fffbeb',
    border: '#fde68a',
    selectedBorder: '#92400e',
  },
] as const;

export default function RoleSelectModal({ open, onClose }: RoleSelectModalProps) {
  const { t } = useTranslation('landing');
  const navigate = useNavigate();
  const [selectedKey, setSelectedKey] = useState<'teacher' | 'student' | 'parent'>('student');

  const selected = ROLES.find((r) => r.key === selectedKey)!;

  function handleLogin() {
    onClose();
    navigate(`/auth/login?role=${selected.roleParam}`);
  }

  function handleRegister() {
    onClose();
    navigate(`/auth/register?role=${selected.roleParam}`);
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth PaperProps={{ sx: { borderRadius: 3 } }}>
      <DialogTitle sx={{ textAlign: 'center', pt: 3.5, pb: 0.5, fontWeight: 700, fontSize: '1.25rem' }}>
        {t('roleModal.title')}
      </DialogTitle>
      <Typography variant="body2" color="text.secondary" textAlign="center" sx={{ pb: 2.5, px: 3 }}>
        {t('roleModal.subtitle')}
      </Typography>

      <DialogContent sx={{ pt: 0, pb: 1, px: 3 }}>
        <Grid container spacing={1.5}>
          {ROLES.map(({ key, roleParam, Icon, color, bg, border, selectedBorder }) => {
            const isSelected = key === selectedKey;
            return (
              <Grid item xs={12} sm={4} key={key}>
                <Box
                  onClick={() => setSelectedKey(key)}
                  sx={{
                    border: `2px solid ${isSelected ? selectedBorder : border}`,
                    borderRadius: 2.5,
                    bgcolor: isSelected ? bg : 'background.paper',
                    cursor: 'pointer',
                    p: 2,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    textAlign: 'center',
                    gap: 1,
                    transition: 'all 0.18s ease',
                    boxShadow: isSelected ? `0 0 0 3px ${selectedBorder}22` : 'none',
                    '&:hover': {
                      borderColor: selectedBorder,
                      bgcolor: bg,
                      transform: 'translateY(-1px)',
                      boxShadow: `0 4px 16px ${color}22`,
                    },
                    userSelect: 'none',
                  }}
                >
                  <Box
                    sx={{
                      width: 48,
                      height: 48,
                      borderRadius: '50%',
                      bgcolor: isSelected ? color : `${color}22`,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      transition: 'background 0.18s',
                      flexShrink: 0,
                    }}
                  >
                    <Icon sx={{ fontSize: 26, color: isSelected ? 'white' : color }} />
                  </Box>
                  <Typography variant="subtitle2" fontWeight={700} color={color}>
                    {t(`roleModal.${key}.title`)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary" lineHeight={1.55}>
                    {t(`roleModal.${key}.description`)}
                  </Typography>
                </Box>
              </Grid>
            );
          })}
        </Grid>

        {/* Action buttons */}
        <Box sx={{ mt: 2.5, display: 'flex', gap: 1.5, flexDirection: { xs: 'column', sm: 'row' } }}>
          <Button
            variant="outlined"
            fullWidth
            size="large"
            startIcon={<LoginIcon />}
            onClick={handleLogin}
            sx={{
              borderRadius: 2,
              py: 1.2,
              fontWeight: 600,
              textTransform: 'none',
              fontSize: '0.9375rem',
              borderColor: selected.color,
              color: selected.color,
              '&:hover': { borderColor: selected.color, bgcolor: `${selected.color}0d` },
            }}
          >
            {t('roleModal.loginBtn')}
          </Button>
          <Button
            variant="contained"
            fullWidth
            size="large"
            startIcon={<PersonAddIcon />}
            onClick={handleRegister}
            sx={{
              borderRadius: 2,
              py: 1.2,
              fontWeight: 600,
              textTransform: 'none',
              fontSize: '0.9375rem',
              bgcolor: selected.color,
              '&:hover': { bgcolor: selected.color, filter: 'brightness(0.9)' },
            }}
          >
            {t('roleModal.registerBtn')}
          </Button>
        </Box>
      </DialogContent>

      <DialogActions sx={{ justifyContent: 'center', pb: 2 }}>
        <Button onClick={onClose} color="inherit" size="small" sx={{ textTransform: 'none', fontSize: '0.8125rem' }}>
          {t('common:cancel', 'Bekor qilish')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
