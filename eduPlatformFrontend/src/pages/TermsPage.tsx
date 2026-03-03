import { Box, Typography, Accordion, AccordionSummary, AccordionDetails, Container, Button } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

const SECTIONS = [
  {
    key: 'acceptance',
    title: 'Shartlarni qabul qilish',
    body: "Test-Pro platformasidan foydalanish orqali siz ushbu foydalanish shartlarini to'liq qabul qilasiz. Agar shartlar bilan rozi bo'lmasangiz, platformadan foydalanmang.",
  },
  {
    key: 'account',
    title: 'Foydalanuvchi akkaunti',
    body: "Ro'yxatdan o'tish uchun to'g'ri va to'liq ma'lumot taqdim etishingiz shart. Akkount xavfsizligi uchun siz javobgarsiz. Akkountingizning ruxsatsiz ishlatilishi haqida darhol xabar bering.",
  },
  {
    key: 'acceptable_use',
    title: "Maqbul foydalanish",
    body: "Platformani faqat ta'lim maqsadlarida foydalaning. Quyidagilar taqiqlanadi: boshqa foydalanuvchilarni bezovta qilish, tizimga noqonuniy kirish urinishi, zararli kontent tarqatish, test natijalarini soxtalashtirish.",
  },
  {
    key: 'content',
    title: 'Kontent va mulkiy huquqlar',
    body: "O'qituvchilar tomonidan yaratilgan kontent ularning mulki hisoblanadi. Test-Pro platformada joylashtirilgan kontent uchun litsenziya oladi, lekin mulk huquqlari kontent egasida qoladi.",
  },
  {
    key: 'termination',
    title: 'Akkountni bekor qilish',
    body: "Test-Pro ushbu shartlarni buzgan foydalanuvchilarning akkountini ogohlantirishsiz bekor qilish huquqini saqlab qoladi. Foydalanuvchi ham istalgan vaqtda akkountini o'chirib tashlash huquqiga ega.",
  },
  {
    key: 'disclaimer',
    title: "Mas'uliyatni cheklash",
    body: "Test-Pro platforma uzluksiz ishlashini kafolatlashga harakat qiladi, lekin vaqtinchalik uzilishlar uchun mas'uliyatni olmaydi. Platforma xizmatlari 'mavjud holda' taqdim etiladi.",
  },
  {
    key: 'changes',
    title: 'Shartlardagi o\'zgarishlar',
    body: "Test-Pro ushbu shartlarni istalgan vaqtda o'zgartirish huquqini saqlab qoladi. O'zgarishlar haqida foydalanuvchilar email orqali xabardor qilinadi.",
  },
];

export default function TermsPage() {
  const { t: tCommon } = useTranslation('common');
  const navigate = useNavigate();

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Box sx={{ bgcolor: 'primary.main', color: 'white', py: { xs: 4, md: 6 }, px: 2 }}>
        <Container maxWidth="md">
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate('/')}
            sx={{ color: 'rgba(255,255,255,0.8)', mb: 2, '&:hover': { color: 'white' } }}
            size="small"
          >
            {tCommon('back', 'Orqaga')}
          </Button>
          <Typography variant="h4" fontWeight={700}>
            Foydalanish shartlari
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.85, mt: 1 }}>
            Oxirgi yangilanish: 2026 yil 1 mart
          </Typography>
        </Container>
      </Box>

      <Container maxWidth="md" sx={{ py: { xs: 4, md: 6 } }}>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4, lineHeight: 1.75 }}>
          Ushbu foydalanish shartlari Test-Pro platformasidan foydalanish qoidalarini belgilaydi.
          Iltimos, platformadan foydalanishdan oldin ushbu shartlarni diqqat bilan o'qib chiqing.
        </Typography>

        {SECTIONS.map((section) => (
          <Accordion key={section.key} defaultExpanded={section.key === 'acceptance'} sx={{ mb: 1.5, borderRadius: 2, '&:before': { display: 'none' }, border: '1px solid', borderColor: 'divider' }} elevation={0}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ px: 3 }}>
              <Typography fontWeight={600}>{section.title}</Typography>
            </AccordionSummary>
            <AccordionDetails sx={{ px: 3, pb: 2.5 }}>
              <Typography variant="body2" color="text.secondary" lineHeight={1.75}>
                {section.body}
              </Typography>
            </AccordionDetails>
          </Accordion>
        ))}

        <Typography variant="body2" color="text.disabled" sx={{ mt: 4, textAlign: 'center' }}>
          Savollar uchun: support@testpro.uz
        </Typography>
      </Container>
    </Box>
  );
}
