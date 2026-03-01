import { useEffect } from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';

/**
 * Opened as a popup by GoogleLoginButton.
 * Reads the access_token from the URL hash, sends it to the opener via postMessage, then closes.
 */
export default function GoogleCallbackPage() {
  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.substring(1));
    const accessToken = params.get('access_token');

    if (accessToken && window.opener) {
      window.opener.postMessage(
        { type: 'GOOGLE_AUTH', access_token: accessToken },
        window.location.origin,
      );
      window.close();
    }
  }, []);

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      minHeight="100vh"
      gap={2}
    >
      <CircularProgress />
      <Typography variant="body2" color="text.secondary">
        Google orqali autentifikatsiya...
      </Typography>
    </Box>
  );
}
