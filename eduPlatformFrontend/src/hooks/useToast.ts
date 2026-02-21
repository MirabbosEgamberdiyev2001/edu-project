import { useSnackbar } from 'notistack';
import { useCallback } from 'react';

export function useToast() {
  const { enqueueSnackbar } = useSnackbar();

  const success = useCallback(
    (message: string) => enqueueSnackbar(message, { variant: 'success' }),
    [enqueueSnackbar],
  );

  const error = useCallback(
    (message: string) => enqueueSnackbar(message, { variant: 'error' }),
    [enqueueSnackbar],
  );

  const info = useCallback(
    (message: string) => enqueueSnackbar(message, { variant: 'info' }),
    [enqueueSnackbar],
  );

  const warning = useCallback(
    (message: string) => enqueueSnackbar(message, { variant: 'warning' }),
    [enqueueSnackbar],
  );

  return { success, error, info, warning };
}
