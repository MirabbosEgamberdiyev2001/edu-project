import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box, Button, Typography, CircularProgress, LinearProgress,
  Tooltip,
} from '@mui/material';
import PictureAsPdfIcon from '@mui/icons-material/PictureAsPdf';
import DescriptionIcon from '@mui/icons-material/Description';
import HtmlIcon from '@mui/icons-material/Html';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import {
  generateExportDocument,
  downloadFile,
  downloadAsPdf,
  type ExportVariant,
  type ExportMode,
} from '@/utils/mathExport';
import { generateDocxBlob } from '@/utils/docxExport';

interface TestExportButtonsProps {
  testId: string;
  variants?: ExportVariant[];
  testTitle?: string;
}

type ExportFormat = 'PDF' | 'DOCX' | 'HTML';

// Map export type key → ExportMode
const MODE_MAP: Record<string, ExportMode> = {
  test:      'test',
  answerKey: 'answerKey',
  combined:  'combined',
  proofs:    'proofs',
};

const EXPORT_TYPES = ['test', 'answerKey', 'combined', 'proofs'] as const;

function safeName(title: string) {
  return title.replace(/[^\w\s\u0400-\u04FF\u0100-\u024F-]/g, '').trim().slice(0, 60) || 'export';
}

export default function TestExportButtons({ variants = [], testTitle = '' }: TestExportButtonsProps) {
  const { t } = useTranslation('test');
  const [loading, setLoading]       = useState<string | null>(null);
  const [lastSuccess, setLastSuccess] = useState<string | null>(null);

  const isEmpty = variants.length === 0;

  const exportLabels = {
    proofLabel:     t('export.proofLabel'),
    answerKeyTitle: t('export.answerKeyTitle'),
    noQuestions:    t('export.noQuestions'),
    variantPrefix:  t('export.variantPrefix'),
  };

  const handleExport = async (type: string, format: ExportFormat) => {
    const key = `${type}_${format}`;
    setLoading(key);
    setLastSuccess(null);

    try {
      const mode  = MODE_MAP[type] ?? 'test';
      const title = testTitle || t(`export.${type}`);
      const name  = safeName(title);

      if (format === 'PDF') {
        // Direct download — no print dialog
        const html = generateExportDocument(variants, title, mode, exportLabels);
        await downloadAsPdf(html, `${name}_${type}.pdf`);
      } else if (format === 'DOCX') {
        // Real .docx — OOXML, all text + math editable in Word
        const blob = await generateDocxBlob(variants, title, mode, exportLabels);
        const url  = URL.createObjectURL(blob);
        const a    = document.createElement('a');
        a.href     = url;
        a.download = `${name}_${type}.docx`;
        a.click();
        URL.revokeObjectURL(url);
      } else {
        // HTML — download and open in any browser
        const html = generateExportDocument(variants, title, mode, exportLabels);
        downloadFile(`${name}_${type}.html`, html, 'text/html');
      }

      setLastSuccess(key);
      setTimeout(() => setLastSuccess(null), 3000);
    } catch {
      // silent — loading state will clear
    } finally {
      setLoading(null);
    }
  };

  // No tooltips on format buttons — column headers already show PDF / DOCX / HTML
  const formats: { fmt: ExportFormat; icon: React.ReactNode }[] = [
    { fmt: 'PDF',  icon: <PictureAsPdfIcon /> },
    { fmt: 'DOCX', icon: <DescriptionIcon  /> },
    { fmt: 'HTML', icon: <HtmlIcon         /> },
  ];

  const hasAnyLoading = loading !== null;

  return (
    <Box>
      <Typography variant="subtitle2" gutterBottom>
        {t('detail.export')}
      </Typography>

      {hasAnyLoading && <LinearProgress sx={{ mb: 2, borderRadius: 1 }} />}

      {/* Column headers */}
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: '160px 1fr',
          gap: 1,
          mb: 0.5,
          px: 0.5,
        }}
      >
        <span />
        <Box sx={{ display: 'flex', gap: '2px' }}>
          {formats.map(({ fmt }) => (
            <Typography
              key={fmt}
              variant="caption"
              color="text.secondary"
              sx={{ width: 72, textAlign: 'center', fontWeight: 600 }}
            >
              {fmt}
            </Typography>
          ))}
        </Box>
      </Box>

      {/* Export rows */}
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
        {EXPORT_TYPES.map((type) => {
          const label = t(`export.${type}`);
          const desc  = t(`export.desc${type.charAt(0).toUpperCase() + type.slice(1)}`, '');
          return (
            <Box
              key={type}
              sx={{
                display: 'grid',
                gridTemplateColumns: '160px 1fr',
                alignItems: 'center',
                gap: 1,
              }}
            >
              {/* Row label — tooltip only if a description key is defined */}
              <Tooltip title={desc} placement="right" arrow disableHoverListener={!desc}>
                <Typography
                  variant="body2"
                  sx={{
                    fontWeight: 500,
                    color: 'text.primary',
                    pr: 0.5,
                    cursor: desc ? 'help' : 'default',
                  }}
                >
                  {label}
                </Typography>
              </Tooltip>

              <Box sx={{ display: 'flex', gap: '2px' }}>
                {formats.map(({ fmt, icon }) => {
                  const key       = `${type}_${fmt}`;
                  const isLoading = loading === key;
                  const isDone    = lastSuccess === key;
                  return (
                    <Button
                      key={fmt}
                      size="small"
                      variant="outlined"
                      onClick={() => handleExport(type, fmt)}
                      disabled={hasAnyLoading || isEmpty}
                      sx={{
                        minWidth: 68,
                        height: 32,
                        fontSize: '0.75rem',
                        px: 1,
                        gap: 0.5,
                        color:       isDone ? 'success.main' : undefined,
                        borderColor: isDone ? 'success.main' : undefined,
                      }}
                      startIcon={
                        isLoading ? (
                          <CircularProgress size={14} />
                        ) : isDone ? (
                          <CheckCircleIcon sx={{ fontSize: 15 }} />
                        ) : (
                          icon
                        )
                      }
                      aria-label={`${label} — ${fmt}`}
                    >
                      {fmt}
                    </Button>
                  );
                })}
              </Box>
            </Box>
          );
        })}
      </Box>

      {isEmpty && (
        <Typography variant="caption" color="text.disabled" sx={{ mt: 1, display: 'block' }}>
          {t('detail.loadingQuestions')}
        </Typography>
      )}
    </Box>
  );
}
