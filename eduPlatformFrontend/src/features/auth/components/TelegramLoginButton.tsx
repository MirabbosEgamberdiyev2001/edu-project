import { useEffect, useRef } from 'react';
import { Box } from '@mui/material';
import { useTelegramAuth } from '../hooks/useTelegramAuth';

export default function TelegramLoginButton() {
  const containerRef = useRef<HTMLDivElement>(null);
  const telegramAuth = useTelegramAuth();
  const botUsername = import.meta.env.VITE_TELEGRAM_BOT_USERNAME;

  useEffect(() => {
    if (!botUsername || !containerRef.current) return;

    // Define the callback globally
    (window as unknown as Record<string, unknown>).onTelegramAuth = (user: {
      id: number; first_name: string; last_name?: string;
      username?: string; photo_url?: string; auth_date: number; hash: string;
    }) => {
      telegramAuth.mutate({
        id: user.id,
        firstName: user.first_name,
        lastName: user.last_name,
        username: user.username,
        photoUrl: user.photo_url,
        authDate: user.auth_date,
        hash: user.hash,
      });
    };

    // Inject Telegram widget script
    const script = document.createElement('script');
    script.src = 'https://telegram.org/js/telegram-widget.js?22';
    script.setAttribute('data-telegram-login', botUsername);
    script.setAttribute('data-size', 'large');
    script.setAttribute('data-onauth', 'onTelegramAuth(user)');
    script.setAttribute('data-request-access', 'write');
    script.async = true;
    containerRef.current.appendChild(script);

    return () => {
      delete (window as unknown as Record<string, unknown>).onTelegramAuth;
    };
  }, [botUsername, telegramAuth]);

  if (!botUsername) return null;

  return <Box ref={containerRef} sx={{ display: 'flex', justifyContent: 'center' }} />;
}
