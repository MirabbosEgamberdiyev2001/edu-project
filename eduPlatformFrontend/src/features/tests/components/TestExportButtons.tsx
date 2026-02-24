import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box, Button, ButtonGroup, Typography, CircularProgress, LinearProgress, Alert,
  Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import DescriptionIcon from '@mui/icons-material/Description';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { testApi } from '@/api/testApi';
import { useToast } from '@/hooks/useToast';
import type { ExportFormat } from '@/types/test';

interface TestExportButtonsProps {
  testId: string;
}

type ExportType = 'test' | 'answerKey' | 'combined' | 'proofs';

export default function TestExportButtons({ testId }: TestExportButtonsProps) {
  const { t } = useTranslation('test');
  const toast = useToast();
  const [loading, setLoading] = useState<string | null>(null);
  const [lastSuccess, setLastSuccess] = useState<string | null>(null);
  const [errorDialog, setErrorDialog] = useState({ open: false, message: '' });

  const handleExport = async (type: ExportType, format: ExportFormat) => {
    const key = `${type}_${format}`;
    setLoading(key);
    setLastSuccess(null);

    try {
      const exportFn = {
        test: testApi.exportTest,
        answerKey: testApi.exportAnswerKey,
        combined: testApi.exportCombined,
        proofs: testApi.exportProofs,
      }[type];

      const response = await exportFn(testId, format);
      const contentType = response.headers['content-type'] || (format === 'PDF' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.wordprocessingml.document');
      const blob = new Blob([response.data], { type: contentType });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${type}_${testId}.${format.toLowerCase()}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      // Show success
      setLastSuccess(key);
      toast.success(t('export.downloaded'));

      // Clear success indicator after 3 seconds
      setTimeout(() => setLastSuccess(null), 3000);
    } catch (error: unknown) {
      const axiosErr = error as { response?: { data?: Blob | string }; message?: string };
      let errorMessage = t('export.error');

      // Try to extract meaningful error message
      if (axiosErr.response?.data instanceof Blob) {
        try {
          const text = await axiosErr.response.data.text();
          const json = JSON.parse(text);
          errorMessage = json.message || json.error || errorMessage;
        } catch {
          errorMessage = t('export.error');
        }
      } else if (typeof axiosErr.response?.data === 'string') {
        errorMessage = axiosErr.response.data;
      } else if (axiosErr.message) {
        errorMessage = axiosErr.message;
      }

      // Show error dialog for better visibility
      if (errorMessage.length > 50) {
        setErrorDialog({ open: true, message: errorMessage });
      } else {
        toast.error(errorMessage);
      }
    } finally {
      setLoading(null);
    }
  };

  const exportTypes: { key: ExportType; label: string }[] = [
    { key: 'test', label: t('export.test') },
    { key: 'answerKey', label: t('export.answerKey') },
    { key: 'combined', label: t('export.combined') },
    { key: 'proofs', label: t('export.proofs') },
  ];

  const hasAnyLoading = loading !== null;

  return (
    <Box>
      <Typography variant="subtitle2" gutterBottom>{t('detail.export')}</Typography>

      {/* Global progress bar during any export */}
      {hasAnyLoading && <LinearProgress sx={{ mb: 2 }} />}

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
        {exportTypes.map(({ key, label }) => (
          <Box key={key} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2" sx={{ minWidth: 120 }}>{label}</Typography>
            <ButtonGroup size="small" variant="outlined">
              <Button
                startIcon={
                  loading === `${key}_PDF` ? (
                    <CircularProgress size={16} />
                  ) : lastSuccess === `${key}_PDF` ? (
                    <CheckCircleIcon fontSize="small" color="success" />
                  ) : (
                    <PictureAsPdfIcon />
                  )
                }
                onClick={() => handleExport(key, 'PDF')}
                disabled={hasAnyLoading}
                aria-label={`Export ${label} as PDF`}
              >
                PDF
              </Button>
              <Button
                startIcon={
                  loading === `${key}_DOCX` ? (
                    <CircularProgress size={16} />
                  ) : lastSuccess === `${key}_DOCX` ? (
                    <CheckCircleIcon fontSize="small" color="success" />
                  ) : (
                    <DescriptionIcon />
                  )
                }
                onClick={() => handleExport(key, 'DOCX')}
                disabled={hasAnyLoading}
                aria-label={`Export ${label} as DOCX`}
              >
                DOCX
              </Button>
            </ButtonGroup>
          </Box>
        ))}
      </Box>

      {/* Error dialog for detailed messages */}
      <Dialog
        open={errorDialog.open}
        onClose={() => setErrorDialog({ ...errorDialog, open: false })}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>{t('export.error')}</DialogTitle>
        <DialogContent>
          <Alert severity="error" sx={{ mt: 2 }}>
            {errorDialog.message}
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setErrorDialog({ ...errorDialog, open: false })}>
            OK
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
