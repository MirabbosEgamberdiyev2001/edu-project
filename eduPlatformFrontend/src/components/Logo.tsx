import { Typography, Box } from '@mui/material';
import SchoolIcon from '@mui/icons-material/School';

interface LogoProps {
  size?: 'small' | 'medium' | 'large';
}

export default function Logo({ size = 'medium' }: LogoProps) {
  const sizes = { small: 24, medium: 36, large: 48 };
  const fontSizes = { small: 'h6', medium: 'h5', large: 'h4' } as const;

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <SchoolIcon color="primary" sx={{ fontSize: sizes[size] }} />
      <Typography variant={fontSizes[size]} fontWeight={700} color="primary">
        EduPlatform
      </Typography>
    </Box>
  );
}
