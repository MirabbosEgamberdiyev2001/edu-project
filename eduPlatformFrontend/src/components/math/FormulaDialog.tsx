import { useRef, useState, useEffect, useCallback, useMemo } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
  Typography,
  Box,
  Tabs,
  Tab,
  Tooltip,
  Grid,
  Paper,
  Divider,
} from '@mui/material';
import FunctionsIcon from '@mui/icons-material/Functions';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import { useTranslation } from 'react-i18next';
import katex from 'katex';
import type { MathfieldElement } from 'mathlive';
import 'mathlive';

interface FormulaDialogProps {
  open: boolean;
  onClose: () => void;
  /** Called with the wrapped latex string, e.g. "$x^2$" or "$$\frac{a}{b}$$" */
  onInsert: (latex: string) => void;
  initialLatex?: string;
}

// ─── Symbol categories ────────────────────────────────────────────────────────

interface SymbolItem {
  label: string;
  latex: string;
  title: string;
}

interface Category {
  key: string;
  labelKey: string;
  symbols: SymbolItem[];
}

const CATEGORIES: Category[] = [
  {
    key: 'basic',
    labelKey: 'formula.catBasic',
    symbols: [
      { label: 'x²', latex: 'x^2', title: 'x^2' },
      { label: 'xⁿ', latex: 'x^n', title: 'x^n' },
      { label: 'xₙ', latex: 'x_n', title: 'x_n' },
      { label: '±', latex: '\\pm', title: '\\pm' },
      { label: '×', latex: '\\times', title: '\\times' },
      { label: '÷', latex: '\\div', title: '\\div' },
      { label: '≤', latex: '\\leq', title: '\\leq' },
      { label: '≥', latex: '\\geq', title: '\\geq' },
      { label: '≠', latex: '\\neq', title: '\\neq' },
      { label: '≈', latex: '\\approx', title: '\\approx' },
      { label: '∞', latex: '\\infty', title: '\\infty' },
      { label: 'π', latex: '\\pi', title: '\\pi' },
      { label: '°', latex: '^\\circ', title: '^\\circ' },
      { label: '|x|', latex: '|x|', title: '|x|' },
    ],
  },
  {
    key: 'fractions',
    labelKey: 'formula.catFractions',
    symbols: [
      { label: 'a/b', latex: '\\frac{a}{b}', title: '\\frac{a}{b}' },
      { label: 'a/b²', latex: '\\frac{a}{b^2}', title: '\\frac{a}{b^2}' },
      { label: '1/2', latex: '\\frac{1}{2}', title: '\\frac{1}{2}' },
      { label: 'a/(b+c)', latex: '\\frac{a}{b+c}', title: '\\frac{a}{b+c}' },
      { label: 'a²/b', latex: '\\frac{a^2}{b}', title: '\\frac{a^2}{b}' },
    ],
  },
  {
    key: 'roots',
    labelKey: 'formula.catRoots',
    symbols: [
      { label: '√x', latex: '\\sqrt{x}', title: '\\sqrt{x}' },
      { label: 'ⁿ√x', latex: '\\sqrt[n]{x}', title: '\\sqrt[n]{x}' },
      { label: '²√', latex: '\\sqrt{a^2+b^2}', title: '\\sqrt{a^2+b^2}' },
      { label: '³√x', latex: '\\sqrt[3]{x}', title: '\\sqrt[3]{x}' },
    ],
  },
  {
    key: 'greek',
    labelKey: 'formula.catGreek',
    symbols: [
      { label: 'α', latex: '\\alpha', title: '\\alpha' },
      { label: 'β', latex: '\\beta', title: '\\beta' },
      { label: 'γ', latex: '\\gamma', title: '\\gamma' },
      { label: 'δ', latex: '\\delta', title: '\\delta' },
      { label: 'ε', latex: '\\varepsilon', title: '\\varepsilon' },
      { label: 'θ', latex: '\\theta', title: '\\theta' },
      { label: 'λ', latex: '\\lambda', title: '\\lambda' },
      { label: 'μ', latex: '\\mu', title: '\\mu' },
      { label: 'σ', latex: '\\sigma', title: '\\sigma' },
      { label: 'φ', latex: '\\varphi', title: '\\varphi' },
      { label: 'ω', latex: '\\omega', title: '\\omega' },
      { label: 'Σ', latex: '\\Sigma', title: '\\Sigma' },
      { label: 'Δ', latex: '\\Delta', title: '\\Delta' },
      { label: 'Π', latex: '\\Pi', title: '\\Pi' },
      { label: 'Ω', latex: '\\Omega', title: '\\Omega' },
    ],
  },
  {
    key: 'advanced',
    labelKey: 'formula.catAdvanced',
    symbols: [
      { label: '∑', latex: '\\sum_{i=1}^{n}', title: '\\sum_{i=1}^{n}' },
      { label: '∫', latex: '\\int_0^\\infty', title: '\\int_0^\\infty' },
      { label: '∂', latex: '\\partial', title: '\\partial' },
      { label: '∇', latex: '\\nabla', title: '\\nabla' },
      { label: 'lim', latex: '\\lim_{x\\to\\infty}', title: '\\lim_{x\\to\\infty}' },
      { label: '∈', latex: '\\in', title: '\\in' },
      { label: '∉', latex: '\\notin', title: '\\notin' },
      { label: '⊂', latex: '\\subset', title: '\\subset' },
      { label: '∩', latex: '\\cap', title: '\\cap' },
      { label: '∪', latex: '\\cup', title: '\\cup' },
      { label: '∀', latex: '\\forall', title: '\\forall' },
      { label: '∃', latex: '\\exists', title: '\\exists' },
      { label: 'vec', latex: '\\vec{v}', title: '\\vec{v}' },
      { label: 'log', latex: '\\log_{a}b', title: '\\log_{a}b' },
    ],
  },
];

// ─── Main component ───────────────────────────────────────────────────────────

export default function FormulaDialog({
  open,
  onClose,
  onInsert,
  initialLatex = '',
}: FormulaDialogProps) {
  const { t } = useTranslation('common');
  const mathFieldRef = useRef<MathfieldElement | null>(null);
  const [mode, setMode] = useState<'inline' | 'display'>('inline');
  const [latexPreview, setLatexPreview] = useState(initialLatex);
  const [activeCat, setActiveCat] = useState(0);

  // ── Validation ──────────────────────────────────────────────────────────────
  const validationError = useMemo(() => {
    if (!latexPreview.trim()) return null;
    try {
      katex.renderToString(latexPreview, { throwOnError: true });
      return null;
    } catch (e) {
      // Extract a short, readable message from KaTeX error
      const msg = e instanceof Error ? e.message : '';
      const clean = msg.replace(/^KaTeX parse error:\s*/i, '').split('\n')[0];
      return clean || t('formula.errorInvalid');
    }
  }, [latexPreview, t]);

  // ── KaTeX preview HTML ───────────────────────────────────────────────────────
  const previewHtml = useMemo(() => {
    if (!latexPreview.trim()) return null;
    try {
      return katex.renderToString(latexPreview, {
        throwOnError: false,
        displayMode: mode === 'display',
        output: 'html',
      });
    } catch {
      return null;
    }
  }, [latexPreview, mode]);

  // ── On open: configure math-field, reset state, focus ───────────────────────
  useEffect(() => {
    if (!open) return;

    const el = mathFieldRef.current;
    if (!el) return;

    try {
      el.mathVirtualKeyboardPolicy = 'manual';
    } catch {/* ignore */}

    el.setValue(initialLatex);
    setLatexPreview(initialLatex);
    setActiveCat(0);
    setTimeout(() => el.focus(), 150);
  }, [open, initialLatex]);

  // ── Hide virtual keyboard on close ──────────────────────────────────────────
  useEffect(() => {
    if (!open) {
      try {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (window as any).mathVirtualKeyboard?.hide?.();
      } catch {/* ignore */}
    }
  }, [open]);

  // ── Track latex from native input event ─────────────────────────────────────
  useEffect(() => {
    if (!open) return;
    const el = mathFieldRef.current;
    if (!el) return;
    const handler = () => setLatexPreview(el.getValue() ?? '');
    el.addEventListener('input', handler);
    return () => el.removeEventListener('input', handler);
  }, [open]);

  // ── Handlers ────────────────────────────────────────────────────────────────
  const handleInsert = useCallback(() => {
    const latex = mathFieldRef.current?.getValue()?.trim() ?? '';
    if (!latex) { onClose(); return; }
    const wrapped = mode === 'display' ? `$$${latex}$$` : `$${latex}$`;
    onInsert(wrapped);
    setLatexPreview('');
    onClose();
  }, [mode, onInsert, onClose]);

  const handleClose = useCallback(() => {
    setLatexPreview('');
    onClose();
  }, [onClose]);

  const insertSymbol = useCallback((symbolLatex: string) => {
    const el = mathFieldRef.current;
    if (!el) return;
    el.insert(symbolLatex, { insertionMode: 'insertAfter' });
    setLatexPreview(el.getValue() ?? '');
    el.focus();
  }, []);

  const canInsert = latexPreview.trim().length > 0 && !validationError;

  // ── Render ───────────────────────────────────────────────────────────────────
  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="md"
      fullWidth
      sx={{ zIndex: 1600 }}
      PaperProps={{ sx: { m: { xs: 1, sm: 2 }, maxHeight: '92vh' } }}
      aria-labelledby="formula-dialog-title"
    >
      <DialogTitle
        id="formula-dialog-title"
        sx={{ display: 'flex', alignItems: 'center', gap: 1, pb: 1, pt: 2 }}
      >
        <FunctionsIcon color="primary" fontSize="small" />
        <Typography variant="h6" component="span">
          {t('formula.title')}
        </Typography>
      </DialogTitle>

      <DialogContent dividers sx={{ p: { xs: 1.5, sm: 2 }, overflow: 'auto' }}>
        <Grid container spacing={2}>

          {/* ── LEFT COLUMN: Toolbar + Editor ────────────────────────────── */}
          <Grid item xs={12} md={7}>

            {/* Symbol toolbar tabs */}
            <Box
              sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1.5,
                overflow: 'hidden',
                mb: 1.5,
              }}
            >
              <Tabs
                value={activeCat}
                onChange={(_, v) => setActiveCat(v)}
                variant="scrollable"
                scrollButtons="auto"
                aria-label={t('formula.catBasic')}
                sx={{
                  minHeight: 36,
                  bgcolor: 'grey.50',
                  borderBottom: '1px solid',
                  borderColor: 'divider',
                  '& .MuiTab-root': {
                    minHeight: 36,
                    py: 0,
                    px: 1.5,
                    fontSize: '0.75rem',
                    fontWeight: 500,
                    textTransform: 'none',
                  },
                }}
              >
                {CATEGORIES.map((cat) => (
                  <Tab key={cat.key} label={t(cat.labelKey)} />
                ))}
              </Tabs>

              {/* Symbol grid */}
              <Box
                sx={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: 0.5,
                  p: 1,
                  minHeight: 60,
                  bgcolor: 'background.paper',
                }}
                role="toolbar"
                aria-label={t(CATEGORIES[activeCat].labelKey)}
              >
                {CATEGORIES[activeCat].symbols.map((sym) => (
                  <Tooltip
                    key={sym.latex}
                    title={
                      <Box component="span" sx={{ fontFamily: 'monospace', fontSize: '0.75rem' }}>
                        {sym.title}
                      </Box>
                    }
                    placement="top"
                    arrow
                  >
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => insertSymbol(sym.latex)}
                      aria-label={sym.title}
                      sx={{
                        minWidth: 36,
                        height: 32,
                        px: 0.5,
                        fontFamily: 'Georgia, serif',
                        fontSize: '0.875rem',
                        fontWeight: 400,
                        lineHeight: 1,
                        textTransform: 'none',
                        borderColor: 'grey.300',
                        color: 'text.primary',
                        '&:hover': {
                          borderColor: 'primary.main',
                          bgcolor: 'rgba(37,99,235,0.06)',
                          color: 'primary.main',
                        },
                      }}
                    >
                      {sym.label}
                    </Button>
                  </Tooltip>
                ))}
              </Box>
            </Box>

            {/* MathLive WYSIWYG editor */}
            <Box
              sx={{
                border: '2px solid',
                borderColor: validationError
                  ? 'error.main'
                  : latexPreview.trim()
                    ? 'primary.main'
                    : 'grey.300',
                borderRadius: 1.5,
                overflow: 'hidden',
                bgcolor: '#fafafa',
                transition: 'border-color 0.2s',
              }}
            >
              <math-field
                ref={mathFieldRef as React.RefObject<HTMLElement>}
                math-virtual-keyboard-policy="manual"
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  fontSize: '1.25rem',
                  display: 'block',
                  boxSizing: 'border-box',
                  minHeight: 60,
                  outline: 'none',
                  background: 'transparent',
                }}
              />
            </Box>

            {/* Hint / validation status */}
            <Box sx={{ mt: 0.75, minHeight: 22, display: 'flex', alignItems: 'center', gap: 0.5 }}>
              {!latexPreview.trim() ? (
                <Typography variant="caption" color="text.secondary">
                  {t('formula.hint')}
                </Typography>
              ) : validationError ? (
                <>
                  <ErrorOutlineIcon sx={{ fontSize: 14, color: 'error.main', flexShrink: 0 }} />
                  <Typography variant="caption" color="error.main" sx={{ wordBreak: 'break-word' }}>
                    {validationError}
                  </Typography>
                </>
              ) : (
                <>
                  <CheckCircleOutlineIcon sx={{ fontSize: 14, color: 'success.main', flexShrink: 0 }} />
                  <Typography variant="caption" color="success.main">
                    {t('formula.validFormula')}
                  </Typography>
                </>
              )}
            </Box>

            {/* LaTeX source preview — shown when non-empty */}
            {latexPreview.trim() ? (
              <Box
                sx={{
                  mt: 0.75,
                  px: 1.25,
                  py: 0.5,
                  bgcolor: 'grey.100',
                  borderRadius: 1,
                  display: 'flex',
                  alignItems: 'baseline',
                  gap: 0.75,
                  flexWrap: 'wrap',
                }}
              >
                <Typography
                  variant="caption"
                  color="text.secondary"
                  fontWeight={600}
                  flexShrink={0}
                  sx={{ fontSize: '0.7rem' }}
                >
                  {t('formula.previewLabel')}
                </Typography>
                <Typography
                  variant="caption"
                  sx={{ fontFamily: 'monospace', color: 'primary.dark', wordBreak: 'break-all', fontSize: '0.7rem' }}
                >
                  {latexPreview}
                </Typography>
              </Box>
            ) : null}
          </Grid>

          {/* ── RIGHT COLUMN: Live Preview + Mode selector ───────────────── */}
          <Grid item xs={12} md={5}>
            <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', gap: 1.5 }}>

              {/* Live KaTeX preview */}
              <Box>
                <Typography
                  variant="caption"
                  fontWeight={600}
                  color="text.secondary"
                  sx={{ display: 'block', mb: 0.5, textTransform: 'uppercase', fontSize: '0.6875rem', letterSpacing: '0.06em' }}
                >
                  {t('formula.previewSection')}
                </Typography>
                <Paper
                  variant="outlined"
                  sx={{
                    minHeight: 100,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: mode === 'display' ? 'center' : 'flex-start',
                    px: 2,
                    py: 1.5,
                    bgcolor: 'grey.50',
                    borderRadius: 1.5,
                    overflow: 'auto',
                  }}
                  aria-label={t('formula.previewSection')}
                  aria-live="polite"
                >
                  {previewHtml ? (
                    <Box
                      sx={{
                        '& .katex': { fontSize: mode === 'display' ? '1.4rem' : '1.1rem' },
                        '& .katex-display': { margin: 0 },
                      }}
                      dangerouslySetInnerHTML={{ __html: previewHtml }}
                    />
                  ) : (
                    <Typography variant="body2" color="text.disabled" fontStyle="italic">
                      {t('formula.emptyPreview')}
                    </Typography>
                  )}
                </Paper>
              </Box>

              <Divider />

              {/* Mode selector */}
              <FormControl component="fieldset">
                <FormLabel
                  component="legend"
                  sx={{ mb: 0.75, fontSize: '0.6875rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'text.secondary' }}
                >
                  {t('formula.modeLabel')}
                </FormLabel>
                <RadioGroup
                  aria-label={t('formula.modeLabel')}
                  value={mode}
                  onChange={(e) => setMode(e.target.value as 'inline' | 'display')}
                >
                  <FormControlLabel
                    value="inline"
                    control={<Radio size="small" />}
                    sx={{ mb: 0.25 }}
                    label={
                      <Box>
                        <Typography variant="body2" fontWeight={mode === 'inline' ? 600 : 400}>
                          {t('formula.inline')}
                        </Typography>
                        <Typography variant="caption" color="text.secondary" display="block">
                          {t('formula.inlineDesc')}
                        </Typography>
                      </Box>
                    }
                  />
                  <FormControlLabel
                    value="display"
                    control={<Radio size="small" />}
                    label={
                      <Box>
                        <Typography variant="body2" fontWeight={mode === 'display' ? 600 : 400}>
                          {t('formula.display')}
                        </Typography>
                        <Typography variant="caption" color="text.secondary" display="block">
                          {t('formula.blockDesc')}
                        </Typography>
                      </Box>
                    }
                  />
                </RadioGroup>
              </FormControl>
            </Box>
          </Grid>
        </Grid>
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 1.5 }}>
        <Button onClick={handleClose} color="inherit">{t('cancel')}</Button>
        <Tooltip
          title={
            !latexPreview.trim()
              ? t('formula.errorEmpty')
              : validationError
                ? t('formula.errorInvalid')
                : ''
          }
          placement="top"
          arrow
          disableHoverListener={canInsert}
          disableFocusListener={canInsert}
          disableTouchListener={canInsert}
        >
          {/* span needed so tooltip works on disabled button */}
          <span>
            <Button
              variant="contained"
              onClick={handleInsert}
              startIcon={<FunctionsIcon />}
              disabled={!canInsert}
            >
              {t('formula.insert')}
            </Button>
          </span>
        </Tooltip>
      </DialogActions>
    </Dialog>
  );
}
