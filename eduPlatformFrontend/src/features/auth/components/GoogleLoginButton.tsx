import { Button } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useGoogleAuth } from '../hooks/useGoogleAuth';

export default function GoogleLoginButton() {
  const { t } = useTranslation('auth');
  const googleAuth = useGoogleAuth();

  const handleGoogleLogin = () => {
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    if (!clientId) return;

    // Use Google's OAuth redirect flow
    const redirectUri = `${window.location.origin}/auth/google/callback`;
    const scope = 'openid email profile';
    const url = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=token&scope=${encodeURIComponent(scope)}`;
    window.location.href = url;
  };

  if (!import.meta.env.VITE_GOOGLE_CLIENT_ID) return null;

  return (
    <Button
      variant="outlined"
      fullWidth
      onClick={handleGoogleLogin}
      disabled={googleAuth.isPending}
      sx={{ textTransform: 'none' }}
    >
      <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="" width={18} height={18} style={{ marginRight: 8 }} />
      {t('login.google')}
    </Button>
  );
}
