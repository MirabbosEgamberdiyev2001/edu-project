import { Box, Typography } from '@mui/material';
import SchoolIcon from '@mui/icons-material/School';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import FamilyRestroomIcon from '@mui/icons-material/FamilyRestroom';
import { useTranslation } from 'react-i18next';
import { Role } from '@/types/user';

interface RoleChipsProps {
  value: Role;
  onChange: (role: Role) => void;
  sx?: object;
}

const ROLE_OPTIONS = [
  {
    role: Role.TEACHER,
    Icon: SchoolIcon,
    color: '#1565c0',
    bg: '#eff6ff',
    border: '#bfdbfe',
  },
  {
    role: Role.STUDENT,
    Icon: MenuBookIcon,
    color: '#166534',
    bg: '#f0fdf4',
    border: '#bbf7d0',
  },
  {
    role: Role.PARENT,
    Icon: FamilyRestroomIcon,
    color: '#92400e',
    bg: '#fffbeb',
    border: '#fde68a',
  },
] as const;

export default function RoleChips({ value, onChange, sx }: RoleChipsProps) {
  const { t } = useTranslation('auth');

  return (
    <Box sx={{ display: 'flex', gap: 1, ...sx }}>
      {ROLE_OPTIONS.map(({ role, Icon, color, bg, border }) => {
        const isSelected = value === role;
        return (
          <Box
            key={role}
            onClick={() => onChange(role)}
            sx={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 0.75,
              py: 1.25,
              px: 0.5,
              border: `1.5px solid ${isSelected ? color : border}`,
              borderRadius: 2,
              bgcolor: isSelected ? bg : 'background.paper',
              cursor: 'pointer',
              transition: 'all 0.15s ease',
              boxShadow: isSelected ? `0 0 0 2px ${color}33` : 'none',
              userSelect: 'none',
              '&:hover': {
                borderColor: color,
                bgcolor: bg,
              },
            }}
          >
            <Icon
              sx={{
                fontSize: 22,
                color: isSelected ? color : 'text.disabled',
                transition: 'color 0.15s',
              }}
            />
            <Typography
              variant="caption"
              fontWeight={isSelected ? 700 : 400}
              color={isSelected ? color : 'text.secondary'}
              lineHeight={1.25}
              textAlign="center"
            >
              {t(`roles.${role}`)}
            </Typography>
          </Box>
        );
      })}
    </Box>
  );
}
