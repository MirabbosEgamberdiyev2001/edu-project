import {
  Card,
  CardActionArea,
  CardContent,
  Typography,
  Box,
  Chip,
  Avatar,
} from '@mui/material';
import SchoolIcon from '@mui/icons-material/School';
import QuizIcon from '@mui/icons-material/Quiz';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import type { ParentChildDto } from '@/types/parent';

interface ChildCardProps {
  pairing: ParentChildDto;
}

export default function ChildCard({ pairing }: ChildCardProps) {
  const { t } = useTranslation('parent');
  const navigate = useNavigate();

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        borderTop: 3,
        borderColor: 'primary.main',
        transition: 'box-shadow 0.2s',
        '&:hover': { boxShadow: 4 },
      }}
    >
      <CardActionArea onClick={() => navigate(`/parent/children/${pairing.childId}`)}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
            <Avatar sx={{ bgcolor: 'primary.main', width: 44, height: 44 }}>
              <SchoolIcon />
            </Avatar>
            <Box sx={{ minWidth: 0, flex: 1 }}>
              <Typography variant="h6" fontWeight={600} noWrap>
                {pairing.childName}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {new Date(pairing.pairingDate).toLocaleDateString()}
              </Typography>
            </Box>
          </Box>

          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Chip
              icon={<QuizIcon />}
              label={`${t('averageScore')}`}
              size="small"
              color="primary"
              variant="outlined"
            />
            <Chip
              label={pairing.pairingStatus}
              size="small"
              color={pairing.pairingStatus === 'ACTIVE' ? 'success' : 'default'}
              variant="outlined"
            />
          </Box>
        </CardContent>
      </CardActionArea>
    </Card>
  );
}
