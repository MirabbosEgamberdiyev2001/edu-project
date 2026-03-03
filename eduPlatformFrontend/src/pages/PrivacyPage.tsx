import { useTranslation } from 'react-i18next';
import { Box, Typography, Accordion, AccordionSummary, AccordionDetails, Container, Button } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { useNavigate } from 'react-router-dom';

const SECTIONS = [
  {
    key: 'collection',
    title: "Ma'lumotlarni to'plash",
    body: "Test-Pro foydalanuvchilardan quyidagi ma'lumotlarni to'playdi: ism va familiya, elektron pochta manzili yoki telefon raqami, parol (shifrlangan holda), testlarda berilgan javoblar va natijalar. Ushbu ma'lumotlar faqat xizmat ko'rsatish maqsadida ishlatiladi.",
  },
  {
    key: 'usage',
    title: "Ma'lumotlardan foydalanish",
    body: "To'plangan ma'lumotlar faqat platforma xizmatlarini taqdim etish, foydalanuvchi tajribasini yaxshilash va xavfsizlikni ta'minlash uchun ishlatiladi. Biz ma'lumotlarni uchinchi shaxslarga sotmaymiz.",
  },
  {
    key: 'security',
    title: 'Xavfsizlik',
    body: "Barcha ma'lumotlar SSL/TLS protokoli bilan shifrlangan holda uzatiladi. Parollar bcrypt algoritmi bilan xashlangan. JWT tokenlar qisqa muddatli bo'lib, havfsiz saqlanadi.",
  },
  {
    key: 'cookies',
    title: 'Cookie fayllar',
    body: "Test-Pro autentifikatsiya va tizim ishlashi uchun minimal cookie fayllardan foydalanadi. Uchinchi tomon reklama cookie'lari ishlatilmaydi.",
  },
  {
    key: 'rights',
    title: 'Foydalanuvchi huquqlari',
    body: "Siz istalgan vaqtda ma'lumotlaringizni ko'rish, tahrirlash yoki o'chirish huquqiga egasiz. Buning uchun support@testpro.uz manziliga murojaat qiling.",
  },
  {
    key: 'contact',
    title: "Bog'lanish",
    body: "Maxfiylik siyosatiga oid savollaringiz bo'lsa: support@testpro.uz",
  },
];

export default function PrivacyPage() {
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
            Maxfiylik siyosati
          </Typography>
          <Typography variant="body1" sx={{ opacity: 0.85, mt: 1 }}>
            Oxirgi yangilanish: 2026 yil 1 mart
          </Typography>
        </Container>
      </Box>

      <Container maxWidth="md" sx={{ py: { xs: 4, md: 6 } }}>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4, lineHeight: 1.75 }}>
          Test-Pro platformasi foydalanuvchilarning shaxsiy ma'lumotlarini himoya qilishni eng muhim
          vazifalardan biri deb biladi. Ushbu siyosat qanday ma'lumotlar to'planishi, ular qanday
          ishlatilishi va himoya qilinishi haqida to'liq ma'lumot beradi.
        </Typography>

        {SECTIONS.map((section) => (
          <Accordion key={section.key} defaultExpanded={section.key === 'collection'} sx={{ mb: 1.5, borderRadius: 2, '&:before': { display: 'none' }, border: '1px solid', borderColor: 'divider' }} elevation={0}>
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
      </Container>
    </Box>
  );
}
