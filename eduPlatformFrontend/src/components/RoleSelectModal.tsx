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
  Card,
  CardActionArea,
  CardContent,
  Grid,
} from '@mui/material';
import SchoolIcon from '@mui/icons-material/School';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import FamilyRestroomIcon from '@mui/icons-material/FamilyRestroom';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';

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
  },
  {
    key: 'student' as const,
    roleParam: 'STUDENT',
    Icon: MenuBookIcon,
    color: '#166534',
    bg: '#f0fdf4',
    border: '#bbf7d0',
  },
  {
    key: 'parent' as const,
    roleParam: 'PARENT',
    Icon: FamilyRestroomIcon,
    color: '#92400e',
    bg: '#fffbeb',
    border: '#fde68a',
  },
] as const;

export default function RoleSelectModal({ open, onClose }: RoleSelectModalProps) {
  const { t } = useTranslation('landing');
  const navigate = useNavigate();

  function handleSelect(roleParam: string) {
    onClose();
    navigate(`/auth/register?role=${roleParam}`);
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ textAlign: 'center', pt: 3, pb: 1, fontWeight: 700, fontSize: '1.25rem' }}>
        {t('roleModal.title')}
      </DialogTitle>
      <Typography variant="body2" color="text.secondary" textAlign="center" sx={{ pb: 2, px: 3 }}>
        {t('roleModal.subtitle')}
      </Typography>

      <DialogContent sx={{ pt: 0, pb: 2 }}>
        <Grid container spacing={2}>
          {ROLES.map(({ key, roleParam, Icon, color, bg, border }) => (
            <Grid item xs={12} sm={4} key={key}>
              <Card
                elevation={0}
                sx={{
                  border: `1.5px solid ${border}`,
                  borderRadius: 3,
                  height: '100%',
                  transition: 'box-shadow 0.18s, transform 0.18s',
                  '&:hover': {
                    boxShadow: '0 6px 24px rgba(0,0,0,0.10)',
                    transform: 'translateY(-2px)',
                  },
                }}
              >
                <CardActionArea
                  onClick={() => handleSelect(roleParam)}
                  sx={{
                    height: '100%',
                    p: 0,
                    borderRadius: 3,
                    '&:hover': { bgcolor: 'transparent' },
                  }}
                >
                  <CardContent
                    sx={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      textAlign: 'center',
                      gap: 1.5,
                      bgcolor: bg,
                      borderRadius: 3,
                      height: '100%',
                      py: 3,
                    }}
                  >
                    <Box
                      sx={{
                        width: 56,
                        height: 56,
                        borderRadius: '50%',
                        bgcolor: color,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                      }}
                    >
                      <Icon sx={{ fontSize: 30, color: 'white' }} />
                    </Box>
                    <Typography variant="subtitle1" fontWeight={700} color={color}>
                      {t(`roleModal.${key}.title`)}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" lineHeight={1.6}>
                      {t(`roleModal.${key}.description`)}
                    </Typography>
                    <Box
                      sx={{
                        mt: 'auto',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 0.5,
                        color,
                        fontWeight: 600,
                        fontSize: '0.8125rem',
                      }}
                    >
                      {t(`roleModal.${key}.cta`)}
                      <ArrowForwardIcon sx={{ fontSize: 16 }} />
                    </Box>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          ))}
        </Grid>
      </DialogContent>

      <DialogActions sx={{ justifyContent: 'center', pb: 2.5 }}>
        <Button onClick={onClose} color="inherit" size="small">
          {t('common:cancel', 'Bekor qilish')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
