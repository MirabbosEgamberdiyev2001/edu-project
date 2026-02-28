import katex from 'katex';
import 'katex/dist/katex.min.css';
import { Box } from '@mui/material';
import type { SxProps, Theme } from '@mui/material';

interface MathTextProps {
  text: string;
  /** MUI Typography variant — applied to the wrapper Box via sx */
  variant?: 'h6' | 'h5' | 'body1' | 'body2' | 'subtitle1' | 'subtitle2' | 'caption';
  sx?: SxProps<Theme>;
  component?: React.ElementType;
}

type Segment =
  | { kind: 'text'; content: string }
  | { kind: 'math'; latex: string; displayMode: boolean };

/**
 * Parse a string that may contain LaTeX delimiters into ordered segments.
 *
 * Supported delimiters (in priority order):
 *   $$...$$ → display math (block, centred)
 *   $...$   → inline math
 *
 * Text outside delimiters is returned as plain text segments.
 */
function parseSegments(raw: string): Segment[] {
  const segments: Segment[] = [];
  // Combined regex: $$...$$ first, then $...$
  // Use non-greedy matching to handle multiple formulas
  const FORMULA_RE = /\$\$([\s\S]+?)\$\$|\$((?:[^$]|\\\$)+?)\$/g;

  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = FORMULA_RE.exec(raw)) !== null) {
    // Text before this match
    if (match.index > lastIndex) {
      segments.push({ kind: 'text', content: raw.slice(lastIndex, match.index) });
    }

    if (match[1] !== undefined) {
      // $$...$$ → display
      segments.push({ kind: 'math', latex: match[1], displayMode: true });
    } else if (match[2] !== undefined) {
      // $...$ → inline
      segments.push({ kind: 'math', latex: match[2], displayMode: false });
    }

    lastIndex = match.index + match[0].length;
  }

  // Remaining text after last match
  if (lastIndex < raw.length) {
    segments.push({ kind: 'text', content: raw.slice(lastIndex) });
  }

  return segments;
}

function renderMath(latex: string, displayMode: boolean): string {
  try {
    return katex.renderToString(latex, {
      throwOnError: false,
      displayMode,
      // 'htmlAndMathml': HTML for screen + MathML for print/PDF/Word
      output: 'htmlAndMathml',
      trust: false,
    });
  } catch {
    return latex; // fallback: show raw latex on unexpected error
  }
}

const VARIANT_STYLES: Record<string, SxProps<Theme>> = {
  h5: { fontSize: '1.5rem', fontWeight: 700, lineHeight: 1.4 },
  h6: { fontSize: '1.125rem', fontWeight: 600, lineHeight: 1.5 },
  subtitle1: { fontSize: '1rem', fontWeight: 500, lineHeight: 1.75 },
  subtitle2: { fontSize: '0.875rem', fontWeight: 500, lineHeight: 1.57 },
  body1: { fontSize: '1rem', lineHeight: 1.5 },
  body2: { fontSize: '0.875rem', lineHeight: 1.43 },
  caption: { fontSize: '0.75rem', lineHeight: 1.66 },
};

/**
 * Renders mixed text + LaTeX (inline and display) using KaTeX.
 *
 * Usage:
 *   <MathText text="Solve: $x^2 + 2x + 1 = 0$" variant="body1" />
 *   <MathText text="$$\frac{a}{b} = c$$" variant="h6" />
 *   <MathText text="Plain text — no math" variant="body2" />
 */
export default function MathText({ text, variant = 'body1', sx, component = 'div' }: MathTextProps) {
  if (!text) return null;

  const hasMath = text.includes('$');

  if (!hasMath) {
    // Fast path: no math delimiters — render as plain text
    return (
      <Box component={component} sx={{ ...VARIANT_STYLES[variant], ...sx }}>
        {text}
      </Box>
    );
  }

  const segments = parseSegments(text);

  return (
    <Box component={component} sx={{ ...VARIANT_STYLES[variant], wordBreak: 'break-word', ...sx }}>
      {segments.map((seg, i) => {
        if (seg.kind === 'text') {
          return <span key={i}>{seg.content}</span>;
        }
        const html = renderMath(seg.latex, seg.displayMode);
        return (
          <span
            key={i}
            // eslint-disable-next-line react/no-danger
            dangerouslySetInnerHTML={{ __html: html }}
            style={seg.displayMode ? { display: 'block', textAlign: 'center', margin: '8px 0' } : undefined}
          />
        );
      })}
    </Box>
  );
}
