/**
 * docxExport.ts — Production-ready .docx generation (OOXML format)
 *
 * ALL content is native Word document elements — 100% editable in Microsoft Word:
 *   Headings   → HeadingLevel paragraphs (proper outline structure)
 *   Body text  → Paragraph + TextRun (Times New Roman, sized)
 *   Math       → OMML via latexToMath() — fractions stacked, radicals with
 *                vincula, superscripts / subscripts as Word equation objects
 *   Options    → Indented paragraphs with optional ✓ marker
 *   Tables     → docx Table / TableRow / TableCell (answer-key grid)
 *
 * Math pipeline:
 *   "$...$ / $$..$$"  →  latexToMath(latex)  →  MathEl (OMML)
 *   The MathEl is inserted directly into Paragraph.children alongside TextRun.
 *   Word renders it as a native inline equation — fully editable.
 *
 * Export modes:
 *   'test'      → all variants, questions only
 *   'answerKey' → compact answer-key table
 *   'combined'  → questions + answer-key table
 *   'proofs'    → questions with correct answers highlighted + proof text
 */

import {
  Document,
  Paragraph,
  TextRun,
  HeadingLevel,
  Table,
  TableRow,
  TableCell,
  WidthType,
  AlignmentType,
  Packer,
  ShadingType,
  BorderStyle,
  convertInchesToTwip,
  Math as MathEl,
} from 'docx';
import type { ExportVariant, ExportMode, ExportLabels } from './mathExport';
import { latexToMath } from './latexToOmml';

// ─── Formula segment parser ────────────────────────────────────────────────────

/** Matches $$display$$ or $inline$ LaTeX delimiters */
const FORMULA_RE = /\$\$([\s\S]+?)\$\$|\$((?:[^$]|\\\$)+?)\$/g;

/**
 * Splits a mixed text+LaTeX string into TextRun and MathEl elements.
 *
 * - Plain text  → TextRun (Times New Roman, specified size/bold/color)
 * - $...$       → latexToMath() → MathEl (OMML, editable in Word)
 * - $$...$$     → latexToMath() → MathEl (display, still inline in paragraph)
 *
 * Returns Array<TextRun | MathEl>, both valid Paragraph children.
 */
function toMixed(
  str: string,
  opts: { bold?: boolean; color?: string; size?: number } = {},
): Array<TextRun | MathEl> {
  const sz   = opts.size  ?? 24;
  const bold = opts.bold  ?? false;
  const runProps = {
    font: 'Times New Roman',
    size: sz,
    bold,
    ...(opts.color ? { color: opts.color } : {}),
  };

  if (!str) return [];
  if (!str.includes('$')) return [new TextRun({ text: str, ...runProps })];

  FORMULA_RE.lastIndex = 0;
  const out: Array<TextRun | MathEl> = [];
  let last = 0;
  let m: RegExpExecArray | null;

  while ((m = FORMULA_RE.exec(str)) !== null) {
    if (m.index > last) {
      out.push(new TextRun({ text: str.slice(last, m.index), ...runProps }));
    }
    // m[1] = display ($$...$$), m[2] = inline ($...$)
    const latex = m[1] !== undefined ? m[1] : m[2];
    out.push(latexToMath(latex));
    last = m.index + m[0].length;
  }

  if (last < str.length) {
    out.push(new TextRun({ text: str.slice(last), ...runProps }));
  }
  return out;
}

// ─── Table borders ────────────────────────────────────────────────────────────

const BORDER_THIN = {
  top:    { style: BorderStyle.SINGLE, size: 4, color: 'CCCCCC' },
  bottom: { style: BorderStyle.SINGLE, size: 4, color: 'CCCCCC' },
  left:   { style: BorderStyle.SINGLE, size: 4, color: 'CCCCCC' },
  right:  { style: BorderStyle.SINGLE, size: 4, color: 'CCCCCC' },
};

const BORDER_HEADER = {
  top:    { style: BorderStyle.SINGLE, size: 6, color: '1E3A5F' },
  bottom: { style: BorderStyle.SINGLE, size: 6, color: '1E3A5F' },
  left:   { style: BorderStyle.SINGLE, size: 4, color: '1E3A5F' },
  right:  { style: BorderStyle.SINGLE, size: 4, color: '1E3A5F' },
};

// ─── Question structure (matches ExportQuestion in mathExport.ts) ─────────────

interface DocxQuestion {
  questionText:      string;
  points?:           number;
  questionType?:     string;
  options?:          Array<{ label: string; text: string; isCorrect?: boolean }>;
  correctAnswerText?: string;
  proof?:            string;
}

// ─── Question block ───────────────────────────────────────────────────────────

function buildQuestion(
  q: DocxQuestion,
  num: number,
  showAns: boolean,
  showProof: boolean,
  lbl: ExportLabels,
): Paragraph[] {
  const paras: Paragraph[] = [];

  // ── Question number + text
  paras.push(
    new Paragraph({
      children: [
        new TextRun({ text: `${num}. `, font: 'Times New Roman', size: 24, bold: true }),
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        ...toMixed(q.questionText) as any,
      ],
      spacing: { before: 160, after: 60 },
    }),
  );

  // ── MCQ options
  for (const opt of q.options ?? []) {
    const correct = showAns && !!opt.isCorrect;
    paras.push(
      new Paragraph({
        children: [
          new TextRun({
            text: `     ${opt.label})  `,
            font: 'Times New Roman',
            size: 24,
            bold: correct,
            ...(correct ? { color: '1A6B1A' } : {}),
          }),
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          ...toMixed(opt.text, correct ? { bold: true, color: '1A6B1A' } : {}) as any,
          ...(correct
            ? [new TextRun({ text: '  ✓', font: 'Times New Roman', size: 24, bold: true, color: '1A6B1A' })]
            : []),
        ],
        indent:  { left: convertInchesToTwip(0.2) },
        spacing: { before: 30, after: 30 },
      }),
    );
  }

  // ── Non-MCQ correct answer line
  const MCQ = ['MCQ_SINGLE', 'MCQ_MULTI', 'TRUE_FALSE'];
  if (showAns && q.correctAnswerText && q.questionType && !MCQ.includes(q.questionType)) {
    paras.push(
      new Paragraph({
        children: [
          new TextRun({ text: '✓  ', font: 'Times New Roman', size: 24, bold: true, color: '1A6B1A' }),
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          ...toMixed(q.correctAnswerText, { color: '1A6B1A' }) as any,
        ],
        indent:  { left: convertInchesToTwip(0.3) },
        spacing: { before: 60, after: 60 },
        shading: { type: ShadingType.SOLID, color: 'E8F5E9', fill: 'E8F5E9' },
      }),
    );
  }

  // ── Proof / explanation
  if (showProof && q.proof) {
    paras.push(
      new Paragraph({
        children: [
          new TextRun({
            text: `${lbl.proofLabel}: `,
            font: 'Times New Roman',
            size: 22,
            bold: true,
            color: '444444',
          }),
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          ...toMixed(q.proof, { color: '333333', size: 22 }) as any,
        ],
        indent:  { left: convertInchesToTwip(0.3) },
        spacing: { before: 60, after: 120 },
        shading: { type: ShadingType.SOLID, color: 'F5F5F5', fill: 'F5F5F5' },
      }),
    );
  }

  return paras;
}

// ─── Answer-key table ─────────────────────────────────────────────────────────

function buildAnswerKey(variants: ExportVariant[], lbl: ExportLabels): Table {
  const maxQ = Math.max(...variants.map((v) => v.questions.length), 0);

  const mkText = (text: string, bold = false, center = false) =>
    new Paragraph({
      children: [new TextRun({ text, bold, font: 'Times New Roman', size: 22 })],
      alignment: center ? AlignmentType.CENTER : AlignmentType.LEFT,
    });

  // Header row: № | Variant A | Variant B | …
  const headerRow = new TableRow({
    tableHeader: true,
    children: [
      new TableCell({
        children: [mkText('№', true, true)],
        width:    { size: 700, type: WidthType.DXA },
        borders:  BORDER_HEADER,
        shading:  { type: ShadingType.SOLID, color: '1E3A5F', fill: '1E3A5F' },
      }),
      ...variants.map((v) =>
        new TableCell({
          children: [mkText(`${lbl.variantPrefix} ${v.code}`, true, true)],
          borders:  BORDER_HEADER,
          shading:  { type: ShadingType.SOLID, color: '1E3A5F', fill: '1E3A5F' },
        }),
      ),
    ],
  });

  // Data rows: question index → answers per variant
  const dataRows = Array.from({ length: maxQ }, (_, i) =>
    new TableRow({
      children: [
        new TableCell({
          children: [mkText(String(i + 1), false, true)],
          borders:  BORDER_THIN,
          shading:  { type: ShadingType.SOLID, color: 'EEF3FA', fill: 'EEF3FA' },
        }),
        ...variants.map((v) => {
          const q = v.questions[i];
          let ans = '—';
          if (q) {
            const cor = q.options?.find((o) => o.isCorrect);
            if (cor) {
              ans = cor.label;
            } else if (q.correctAnswerText) {
              ans = q.correctAnswerText.replace(/\$[^$]+\$/g, '…').slice(0, 22);
            }
          }
          return new TableCell({
            children: [mkText(ans, false, true)],
            borders:  BORDER_THIN,
          });
        }),
      ],
    }),
  );

  return new Table({
    rows:  [headerRow, ...dataRows],
    width: { size: 100, type: WidthType.PERCENTAGE },
  });
}

// ─── Document defaults ────────────────────────────────────────────────────────

const DEFAULT_LABELS: ExportLabels = {
  proofLabel:    'Proof / Explanation',
  answerKeyTitle: 'Answer Key',
  noQuestions:   'No questions found.',
  variantPrefix: 'Variant',
};

// ─── Main export ──────────────────────────────────────────────────────────────

/**
 * Generates a real `.docx` Blob (OOXML / WordprocessingML).
 *
 * Key properties of the output:
 * - 100% editable in Microsoft Word 2013+ and LibreOffice Writer
 * - Math formulas are native OMML equations (not images, not Unicode text)
 * - Fractions render as stacked fractions, radicals have proper vinculum,
 *   superscripts/subscripts are Word equation objects
 * - Supports all 4 app languages (Uzbek Latin/Cyrillic, Russian, English) —
 *   Unicode text in TextRun and MathRun handles all scripts correctly
 * - Document structure: Heading 1 (title) → Heading 2 (variant) → paragraphs
 * - Times New Roman font, 12pt body, page margins 2.5 cm left/right
 *
 * @param variants  Variants with pre-sorted questions
 * @param title     Document title (test name)
 * @param mode      'test' | 'answerKey' | 'combined' | 'proofs'
 * @param labels    Locale-aware UI labels (from useTranslation)
 */
export async function generateDocxBlob(
  variants: ExportVariant[],
  title: string,
  mode: ExportMode,
  labels?: Partial<ExportLabels>,
): Promise<Blob> {
  const lbl:      ExportLabels = { ...DEFAULT_LABELS, ...labels };
  const showAns   = mode === 'proofs';
  const showProof = mode === 'proofs';
  const showQs    = mode !== 'answerKey';
  const showAK    = mode === 'answerKey' || mode === 'combined';

  const body: Array<Paragraph | Table> = [
    new Paragraph({
      text:    title,
      heading: HeadingLevel.HEADING_1,
      spacing: { before: 0, after: 240 },
    }),
  ];

  if (variants.length === 0) {
    body.push(
      new Paragraph({
        children: [new TextRun({ text: lbl.noQuestions, font: 'Times New Roman', size: 24 })],
      }),
    );
  } else {
    // ── Question sections (one per variant, page-break between)
    if (showQs) {
      for (let vi = 0; vi < variants.length; vi++) {
        const v = variants[vi];
        body.push(
          new Paragraph({
            text:           `${lbl.variantPrefix} ${v.code}`,
            heading:        HeadingLevel.HEADING_2,
            pageBreakBefore: vi > 0,
            spacing:        { before: vi === 0 ? 0 : 320, after: 120 },
          }),
        );
        for (let qi = 0; qi < v.questions.length; qi++) {
          body.push(...buildQuestion(v.questions[qi], qi + 1, showAns, showProof, lbl));
        }
      }
    }

    // ── Answer-key table
    if (showAK) {
      body.push(
        new Paragraph({
          text:           lbl.answerKeyTitle,
          heading:        HeadingLevel.HEADING_2,
          pageBreakBefore: mode === 'combined',
          spacing:        { before: 320, after: 120 },
        }),
      );
      body.push(buildAnswerKey(variants, lbl));
    }
  }

  // ── Document styles
  const doc = new Document({
    styles: {
      default: {
        document: {
          run: { font: 'Times New Roman', size: 24, color: '000000' },
        },
        heading1: {
          run:       { font: 'Times New Roman', size: 32, bold: true, color: '000000' },
          paragraph: { spacing: { before: 240, after: 160 }, alignment: AlignmentType.CENTER },
        },
        heading2: {
          run:       { font: 'Times New Roman', size: 26, bold: true, color: '1E3A5F' },
          paragraph: { spacing: { before: 200, after: 100 } },
        },
      },
    },
    sections: [
      {
        properties: {
          page: {
            margin: {
              top:    convertInchesToTwip(1),
              bottom: convertInchesToTwip(1),
              left:   convertInchesToTwip(1.25),
              right:  convertInchesToTwip(1.25),
            },
          },
        },
        children: body,
      },
    ],
  });

  return Packer.toBlob(doc);
}
