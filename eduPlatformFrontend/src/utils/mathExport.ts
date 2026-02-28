/**
 * mathExport.ts — Professional client-side export with full LaTeX support
 *
 * Formats:
 *   PDF  — new window → browser print dialog → "Save as PDF"
 *   DOCX — Word-compatible .html (Word 2013+ imports MathML as Office Math)
 *   HTML — download the same .html, open in any browser
 *
 * Export modes (ExportMode):
 *   'test'      — All variants, questions only (no answers)
 *   'answerKey' — Compact answer-key table for all variants
 *   'combined'  — Test questions + answer-key table appended at the end
 *   'proofs'    — Test questions with correct answers highlighted + proofs
 *
 * Multi-variant:
 *   Each variant is its own page-section. Options are already reordered
 *   per variant before being passed in (handled in TestDetailPage).
 *
 * Formula strategy:
 *   KaTeX output:'htmlAndMathml' → visual HTML + embedded MathML.
 *   Word reads the <math> elements as native Office Math equation objects.
 */

import katex from 'katex';

// ─── Public types ─────────────────────────────────────────────────────────────

export interface ExportOption {
  label: string;       // A, B, C, D …
  text: string;        // Resolved, may contain $…$ LaTeX
  isCorrect?: boolean;
}

export interface ExportQuestion {
  questionText: string;
  points?: number;
  questionType?: string;
  options?: ExportOption[];
  correctAnswerText?: string; // For SHORT_ANSWER / FILL_BLANK / ESSAY
  proof?: string;             // Explanation/proof text (may contain $…$ LaTeX)
}

export interface ExportVariant {
  code: string;        // "A", "B", "C", "D" …
  questions: ExportQuestion[];
}

/**
 * 'test'      → questions only, no answers shown
 * 'answerKey' → compact table: rows = question #, columns = variant
 * 'combined'  → questions (no answers) + answer-key table appended
 * 'proofs'    → questions with correct answers highlighted + proof text
 */
export type ExportMode = 'test' | 'answerKey' | 'combined' | 'proofs';

// ─── LaTeX rendering ──────────────────────────────────────────────────────────

const FORMULA_RE = /\$\$([\s\S]+?)\$\$|\$((?:[^$]|\\\$)+?)\$/g;

function esc(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/**
 * Renders a mixed text+LaTeX string to an HTML fragment.
 * Produces both visual KaTeX HTML and embedded MathML (for Word/PDF).
 */
export function renderMixedToHtml(text: string): string {
  if (!text) return '';
  if (!text.includes('$')) return esc(text);

  FORMULA_RE.lastIndex = 0;
  let out = '';
  let last = 0;
  let m: RegExpExecArray | null;

  while ((m = FORMULA_RE.exec(text)) !== null) {
    if (m.index > last) out += esc(text.slice(last, m.index));
    const display = m[1] !== undefined;
    const latex = display ? m[1] : m[2];
    try {
      const rendered = katex.renderToString(latex, {
        throwOnError: false,
        displayMode: display,
        output: 'htmlAndMathml',
        trust: false,
      });
      out += display ? `<div class="dmath">${rendered}</div>` : rendered;
    } catch {
      out += esc(display ? `$$${latex}$$` : `$${latex}$`);
    }
    last = m.index + m[0].length;
  }
  if (last < text.length) out += esc(text.slice(last));
  return out;
}

// ─── CSS ──────────────────────────────────────────────────────────────────────

const CSS = `
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

  body {
    font-family: "Times New Roman", Times, serif;
    font-size: 12pt;
    line-height: 1.7;
    color: #000;
    background: #fff;
    padding: 2cm 2.5cm;
  }

  /* ── Main title ─────────────────────── */
  .doc-title {
    font-size: 16pt;
    font-weight: bold;
    text-align: center;
    padding-bottom: 8pt;
    margin-bottom: 20pt;
    border-bottom: 2pt solid #000;
    letter-spacing: 0.03em;
  }

  /* ── Section subtitle (Answer Key etc.) */
  .section-title {
    font-size: 14pt;
    font-weight: bold;
    text-align: center;
    margin: 0 0 16pt 0;
    padding: 8pt 0;
    border-top: 2pt solid #000;
    border-bottom: 1pt solid #555;
    letter-spacing: 0.04em;
    text-transform: uppercase;
  }

  /* ── Variant page section ───────────── */
  .variant-section {
    break-before: page;
    page-break-before: always;
  }
  .variant-section:first-child,
  .variant-section.no-break {
    break-before: avoid;
    page-break-before: avoid;
  }
  .variant-header {
    font-size: 13pt;
    font-weight: bold;
    text-align: center;
    padding: 7pt 0;
    margin-bottom: 14pt;
    border-top: 2pt solid #000;
    border-bottom: 1pt solid #000;
    letter-spacing: 0.06em;
    text-transform: uppercase;
  }

  /* ── Question block ─────────────────── */
  .question {
    break-inside: avoid;
    page-break-inside: avoid;
    margin-bottom: 14pt;
  }
  .q-row {
    display: flex;
    gap: 6pt;
    align-items: baseline;
    margin-bottom: 4pt;
  }
  .q-num {
    min-width: 22pt;
    font-weight: bold;
    flex-shrink: 0;
  }
  .q-text { font-weight: bold; flex: 1; }
  .q-pts {
    font-weight: normal;
    font-size: 9.5pt;
    color: #555;
    white-space: nowrap;
    margin-left: 4pt;
  }

  /* ── Options ────────────────────────── */
  .options {
    margin: 4pt 0 0 28pt;
    list-style: none;
  }
  .opt {
    display: flex;
    gap: 5pt;
    align-items: baseline;
    padding: 1.5pt 0;
  }
  .opt-lbl {
    min-width: 18pt;
    font-weight: normal;
    flex-shrink: 0;
  }
  .opt-txt { flex: 1; }
  .opt.correct .opt-lbl,
  .opt.correct .opt-txt { color: #15803d; font-weight: 700; }
  .check { margin-left: 4pt; color: #15803d; font-weight: bold; }

  /* ── Correct answer line (non-MCQ) ──── */
  .correct-ans {
    margin: 5pt 0 0 28pt;
    color: #15803d;
    font-weight: 600;
    font-size: 10.5pt;
  }

  /* ── Proof / explanation block ──────── */
  .proof {
    margin: 7pt 0 0 0;
    padding: 7pt 12pt;
    border-left: 3.5pt solid #16a34a;
    background: #f0fdf4;
    font-size: 10.5pt;
    color: #14532d;
    break-inside: avoid;
    page-break-inside: avoid;
  }
  .proof-label {
    font-weight: 700;
    font-size: 9.5pt;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    margin-bottom: 3pt;
    color: #166534;
  }

  /* ── Answer-key table ───────────────── */
  .ak-section { margin-top: 24pt; }
  .ak-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 11pt;
    margin-top: 10pt;
  }
  .ak-table th {
    background: #1e3a5f;
    color: #fff;
    font-weight: 700;
    text-align: center;
    padding: 6pt 8pt;
    border: 1pt solid #1e3a5f;
    font-size: 11pt;
    letter-spacing: 0.04em;
  }
  .ak-table th.num-col {
    background: #2d4f7c;
    min-width: 28pt;
  }
  .ak-table td {
    text-align: center;
    padding: 4pt 6pt;
    border: 1pt solid #c8d5e8;
    font-size: 11pt;
  }
  .ak-table tr:nth-child(even) td { background: #f0f4fa; }
  .ak-table tr:hover td { background: #dbeafe; }
  .ak-table td.num-col {
    font-weight: 600;
    color: #1e3a5f;
    background: #eef3fa;
  }
  .ak-table tr:nth-child(even) td.num-col { background: #dce6f4; }
  .ak-correct { color: #15803d; font-weight: 700; }

  /* ── Math ───────────────────────────── */
  .dmath {
    text-align: center;
    margin: 6pt 0;
  }
  .katex-display { margin: 0.3em 0; }
  .katex .katex-mathml {
    position: absolute;
    clip: rect(1px,1px,1px,1px);
    overflow: hidden;
    height: 1px; width: 1px;
  }

  /* ── Print rules ────────────────────── */
  @media print {
    body { padding: 0; }
    .variant-section { break-before: page; page-break-before: always; }
    .variant-section:first-child,
    .variant-section.no-break { break-before: avoid; page-break-before: avoid; }
    .question { break-inside: avoid; page-break-inside: avoid; }
    .proof { break-inside: avoid; page-break-inside: avoid;
      -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    .ak-section { break-before: page; page-break-before: always; }
    .ak-section.no-break { break-before: avoid; page-break-before: avoid; }
    .ak-table th { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    .ak-table td { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    .ak-table tr:nth-child(even) td {
      -webkit-print-color-adjust: exact; print-color-adjust: exact;
    }
    .opt.correct .opt-lbl,
    .opt.correct .opt-txt { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    .correct-ans { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  }
`;

// ─── HTML shell ───────────────────────────────────────────────────────────────

function shell(title: string, body: string): string {
  return `<!DOCTYPE html>
<html
  xmlns:o="urn:schemas-microsoft-com:office:office"
  xmlns:w="urn:schemas-microsoft-com:office:word"
  xmlns="http://www.w3.org/TR/REC-html40"
  lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="ProgId" content="Word.Document">
  <title>${esc(title)}</title>
  <link rel="stylesheet"
    href="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css">
  <style>${CSS}</style>
</head>
<body>
  <h1 class="doc-title">${esc(title)}</h1>
  ${body}
</body>
</html>`;
}

// ─── Answer helpers ───────────────────────────────────────────────────────────

function getCorrectLabel(q: ExportQuestion): string {
  if (q.options?.length) {
    const correct = q.options.filter((o) => o.isCorrect);
    if (correct.length > 0) return correct.map((o) => o.label).join(', ');
  }
  if (q.correctAnswerText) {
    // Truncate long text answers for the table cell
    const t = q.correctAnswerText.replace(/\$[^$]*\$/g, '…').trim();
    return t.length > 20 ? t.slice(0, 18) + '…' : t;
  }
  return '—';
}

// ─── Block builders ───────────────────────────────────────────────────────────

/** Renders a single question block (with or without answer/proof). */
function questionBlock(q: ExportQuestion, num: number, showAnswers: boolean, showProofs: boolean): string {
  const MCQ = ['MCQ_SINGLE', 'MCQ_MULTI'];
  const pts = q.points != null ? `<span class="q-pts">(${q.points} ball)</span>` : '';

  // Options
  let optsHtml = '';
  if (q.options?.length) {
    const items = q.options.map((opt) => {
      const correct = showAnswers && opt.isCorrect;
      const cls = correct ? ' class="opt correct"' : ' class="opt"';
      const chk = correct ? '<span class="check">✓</span>' : '';
      return `<li${cls}><span class="opt-lbl">${esc(opt.label)})</span><span class="opt-txt">${renderMixedToHtml(opt.text)}</span>${chk}</li>`;
    }).join('\n        ');
    optsHtml = `\n      <ul class="options">\n        ${items}\n      </ul>`;
  }

  // Correct answer for non-MCQ
  let ansHtml = '';
  if (showAnswers && q.correctAnswerText && q.questionType && !MCQ.includes(q.questionType)) {
    ansHtml = `\n      <div class="correct-ans">✓ ${renderMixedToHtml(q.correctAnswerText)}</div>`;
  }

  // Proof
  let proofHtml = '';
  if (showProofs && q.proof) {
    proofHtml = `\n      <div class="proof"><div class="proof-label">Isbot / Izoh</div>${renderMixedToHtml(q.proof)}</div>`;
  }

  return `<div class="question">
    <div class="q-row">
      <span class="q-num">${num}.</span>
      <span class="q-text">${renderMixedToHtml(q.questionText)}${pts ? ' ' + pts : ''}</span>
    </div>${optsHtml}${ansHtml}${proofHtml}
  </div>`;
}

/** Renders one variant section (page-break + header + questions). */
function variantSection(variant: ExportVariant, isFirst: boolean, showAnswers: boolean, showProofs: boolean): string {
  const breakClass = isFirst ? ' no-break' : '';
  const qs = variant.questions
    .map((q, i) => questionBlock(q, i + 1, showAnswers, showProofs))
    .join('\n  ');
  return `<section class="variant-section${breakClass}">
  <h2 class="variant-header">Variant ${esc(variant.code)}</h2>
  ${qs}
</section>`;
}

/** Renders the answer-key table (all variants side by side). */
function answerKeyTable(variants: ExportVariant[], noBreak = false): string {
  if (!variants.length) return '';
  const maxQ = Math.max(...variants.map((v) => v.questions.length));
  const breakClass = noBreak ? ' no-break' : '';

  const headerCells = variants
    .map((v) => `<th>${esc(v.code)}</th>`)
    .join('');

  const rows = Array.from({ length: maxQ }, (_, i) => {
    const cells = variants
      .map((v) => {
        const q = v.questions[i];
        if (!q) return '<td>—</td>';
        const label = getCorrectLabel(q);
        return `<td class="ak-correct">${esc(label)}</td>`;
      })
      .join('');
    return `<tr><td class="num-col">${i + 1}</td>${cells}</tr>`;
  }).join('\n      ');

  return `<section class="ak-section${breakClass}">
  <h2 class="section-title">Javoblar Kaliti</h2>
  <table class="ak-table">
    <thead>
      <tr>
        <th class="num-col">#</th>
        ${headerCells}
      </tr>
    </thead>
    <tbody>
      ${rows}
    </tbody>
  </table>
</section>`;
}

// ─── Main export entry point ──────────────────────────────────────────────────

/**
 * Generates a complete export HTML document.
 *
 * @param variants  Variant list (each with questions in per-variant order)
 * @param title     Document title (test name)
 * @param mode      Export mode:
 *                    'test'      → questions only (all variants)
 *                    'answerKey' → answer-key table only
 *                    'combined'  → questions + answer-key table appended
 *                    'proofs'    → questions with correct answers + proofs
 */
export function generateExportDocument(
  variants: ExportVariant[],
  title: string,
  mode: ExportMode,
): string {
  if (variants.length === 0) return shell(title, '<p>Savollar topilmadi.</p>');

  switch (mode) {
    case 'test': {
      const sections = variants
        .map((v, i) => variantSection(v, i === 0, false, false))
        .join('\n');
      return shell(title, sections);
    }

    case 'answerKey': {
      return shell(title, answerKeyTable(variants, true));
    }

    case 'combined': {
      const sections = variants
        .map((v, i) => variantSection(v, i === 0, false, false))
        .join('\n');
      const akTable = answerKeyTable(variants);
      return shell(title, sections + '\n' + akTable);
    }

    case 'proofs': {
      const sections = variants
        .map((v, i) => variantSection(v, i === 0, true, true))
        .join('\n');
      return shell(title, sections);
    }
  }
}

// ─── Legacy helpers (used by QuestionDetailPage) ──────────────────────────────

/** @deprecated Use generateExportDocument instead. */
export interface WordExportOptions {
  showCorrectAnswers?: boolean;
  showProofs?: boolean;
}

/** @deprecated Use generateExportDocument instead. */
export function generateWordDocument(
  questions: ExportQuestion[],
  title: string,
  exportOptions: WordExportOptions = {},
): string {
  const { showCorrectAnswers = false, showProofs = false } = exportOptions;
  const qs = questions
    .map((q, i) => questionBlock(q, i + 1, showCorrectAnswers, showProofs))
    .join('\n');
  return shell(title, qs);
}

/** @deprecated Use generateExportDocument instead. */
export function generateMultiVariantDocument(
  variants: ExportVariant[],
  title: string,
  exportOptions: WordExportOptions = {},
): string {
  const { showCorrectAnswers = false, showProofs = false } = exportOptions;
  if (variants.length === 0) return shell(title, '<p>Savollar topilmadi.</p>');
  if (variants.length === 1) return generateWordDocument(variants[0].questions, title, exportOptions);
  const sections = variants
    .map((v, i) => variantSection(v, i === 0, showCorrectAnswers, showProofs))
    .join('\n');
  return shell(title, sections);
}

// ─── File download ────────────────────────────────────────────────────────────

export function downloadFile(filename: string, content: string, mimeType = 'text/html'): void {
  const blob = new Blob([content], { type: `${mimeType};charset=utf-8` });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// ─── Print / open in new window ──────────────────────────────────────────────

/**
 * Opens the HTML in a new window and auto-triggers the print dialog.
 * 700 ms delay lets KaTeX CSS load so formulas render correctly in the PDF.
 * Falls back to download if popup is blocked.
 */
export function printHtml(html: string): void {
  const win = window.open('', '_blank', 'width=960,height=720,scrollbars=yes');
  if (!win) { downloadFile('export.html', html); return; }
  win.document.write(html);
  win.document.close();
  setTimeout(() => {
    try { win.focus(); win.print(); } catch { /* user can Ctrl+P manually */ }
  }, 700);
}

export function printPage(): void { window.print(); }
