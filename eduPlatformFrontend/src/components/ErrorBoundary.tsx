import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Box, Typography, Button } from '@mui/material';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import i18next from 'i18next';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('ErrorBoundary caught:', error, info);
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;

      return (
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: 300,
            gap: 2,
            p: 4,
          }}
        >
          <ErrorOutlineIcon sx={{ fontSize: 56, color: 'error.main' }} />
          <Typography variant="h5" fontWeight={600}>
            {i18next.t('common:errorBoundary.title')}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', maxWidth: 400 }}>
            {i18next.t('common:errorBoundary.description')}
          </Typography>
          <Button
            variant="contained"
            onClick={() => {
              this.setState({ hasError: false, error: undefined });
              window.location.reload();
            }}
          >
            {i18next.t('common:errorBoundary.reload')}
          </Button>
        </Box>
      );
    }
    return this.props.children;
  }
}
