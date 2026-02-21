import { useState } from 'react';
import { Button, Menu, MenuItem, ListItemText } from '@mui/material';
import LanguageIcon from '@mui/icons-material/Language';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import { useTranslation } from 'react-i18next';
import { SUPPORTED_LANGUAGES, LANGUAGE_LABELS, LANGUAGE_SHORT_LABELS } from '@/config';

export default function LanguageSwitcher() {
  const { i18n } = useTranslation();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const currentLang = SUPPORTED_LANGUAGES.includes(i18n.language as typeof SUPPORTED_LANGUAGES[number])
    ? i18n.language
    : 'uzl';

  return (
    <>
      <Button
        onClick={(e) => setAnchorEl(e.currentTarget)}
        size="small"
        startIcon={<LanguageIcon sx={{ fontSize: 18 }} />}
        endIcon={<KeyboardArrowDownIcon sx={{ fontSize: 16 }} />}
        sx={{
          color: 'text.secondary',
          borderRadius: 2,
          px: 1.5,
          minWidth: 0,
          fontSize: '0.8125rem',
          '&:hover': { bgcolor: 'action.hover' },
        }}
      >
        {LANGUAGE_SHORT_LABELS[currentLang]}
      </Button>
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
        slotProps={{ paper: { sx: { minWidth: 160, borderRadius: 2, mt: 0.5 } } }}
      >
        {SUPPORTED_LANGUAGES.map((lang) => (
          <MenuItem
            key={lang}
            selected={i18n.language === lang}
            onClick={() => {
              i18n.changeLanguage(lang);
              setAnchorEl(null);
            }}
            sx={{ borderRadius: 1, mx: 0.5, fontSize: '0.875rem' }}
          >
            <ListItemText>{LANGUAGE_LABELS[lang]}</ListItemText>
          </MenuItem>
        ))}
      </Menu>
    </>
  );
}
