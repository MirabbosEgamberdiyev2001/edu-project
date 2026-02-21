import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Button, ButtonGroup, Typography, CircularProgress } from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import DescriptionIcon from '@mui/icons-material/Description';
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

  const handleExport = async (type: ExportType, format: ExportFormat) => {
    const key = `${type}_${format}`;
    setLoading(key);
    try {
      const exportFn = {
        test: testApi.exportTest,
        answerKey: testApi.exportAnswerKey,
        combined: testApi.exportCombined,
        proofs: testApi.exportProofs,
      }[type];

      const response = await exportFn(testId, format);
      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${type}_${testId}.${format.toLowerCase()}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch {
      toast.error(t('errors.generateFailed'));
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

  return (
    <Box>
      <Typography variant="subtitle2" gutterBottom>{t('detail.export')}</Typography>
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
        {exportTypes.map(({ key, label }) => (
          <Box key={key} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2" sx={{ minWidth: 120 }}>{label}</Typography>
            <ButtonGroup size="small" variant="outlined">
              <Button
                startIcon={loading === `${key}_PDF` ? <CircularProgress size={16} /> : <PictureAsPdfIcon />}
                onClick={() => handleExport(key, 'PDF')}
                disabled={loading !== null}
              >
                PDF
              </Button>
              <Button
                startIcon={loading === `${key}_DOCX` ? <CircularProgress size={16} /> : <DescriptionIcon />}
                onClick={() => handleExport(key, 'DOCX')}
                disabled={loading !== null}
              >
                DOCX
              </Button>
            </ButtonGroup>
          </Box>
        ))}
      </Box>
    </Box>
  );
}
