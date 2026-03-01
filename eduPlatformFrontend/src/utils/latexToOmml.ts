/**
 * latexToOmml.ts — LaTeX math formula → native Word equations (OMML)
 *
 * Converts LaTeX math strings (from $...$ / $$...$$) into docx Math elements
 * using the `docx` library's OMML classes. The result is 100% editable in
 * Microsoft Word — not images, not Unicode tricks.
 *
 * Supported constructs:
 *   \frac{a}{b}         → MathFraction  (proper stacked fraction)
 *   \sqrt{x}            → MathRadical   (√ with vinculum)
 *   \sqrt[n]{x}         → MathRadical   with degree
 *   x^2 / x^{abc}       → MathSuperScript
 *   x_1 / x_{abc}       → MathSubScript
 *   x_1^2               → MathSubSuperScript
 *   \left( … \right)    → MathRoundBrackets
 *   \left[ … \right]    → MathSquareBrackets
 *   \left\{ … \right\}  → MathCurlyBrackets
 *   \left\langle…\right\rangle → MathAngledBrackets
 *   \alpha, \beta, …    → Unicode Greek (MathRun)
 *   \div, \pm, \leq, …  → Unicode math operators (MathRun)
 *   \sin, \cos, \lim, … → Named function text (MathRun, roman font)
 *   Unknown commands    → command name as plain text (safe fallback)
 */

import {
  Math as MathEl,
  MathRun,
  MathFraction,
  MathRadical,
  MathSuperScript,
  MathSubScript,
  MathSubSuperScript,
  MathRoundBrackets,
  MathSquareBrackets,
  MathAngledBrackets,
  MathCurlyBrackets,
  type MathComponent,
} from 'docx';

// ─── Symbol table ─────────────────────────────────────────────────────────────
/** Maps LaTeX command names (no backslash) → Unicode character(s) */
const SYM: Record<string, string> = {
  // Arithmetic operators
  div: '÷', pm: '±', mp: '∓', times: '×', cdot: '·', ast: '∗',
  oplus: '⊕', ominus: '⊖', otimes: '⊗', circ: '∘', bullet: '•', star: '⋆',

  // Relations
  leq: '≤', le: '≤', geq: '≥', ge: '≥', neq: '≠', ne: '≠',
  approx: '≈', equiv: '≡', cong: '≅', sim: '∼', simeq: '≃',
  ll: '≪', gg: '≫', propto: '∝', parallel: '∥', perp: '⊥',

  // Set & logic
  in: '∈', notin: '∉', subset: '⊂', supset: '⊃',
  subseteq: '⊆', supseteq: '⊇', cup: '∪', cap: '∩',
  emptyset: '∅', varnothing: '∅', forall: '∀', exists: '∃',
  wedge: '∧', vee: '∨', neg: '¬',

  // Arrows
  rightarrow: '→', to: '→', leftarrow: '←', gets: '←',
  Rightarrow: '⇒', Leftarrow: '⇐', Leftrightarrow: '⇔', leftrightarrow: '↔',
  uparrow: '↑', downarrow: '↓', mapsto: '↦', nearrow: '↗', searrow: '↘',

  // Greek lowercase
  alpha: 'α', beta: 'β', gamma: 'γ', delta: 'δ',
  epsilon: 'ε', varepsilon: 'ε', zeta: 'ζ', eta: 'η',
  theta: 'θ', vartheta: 'ϑ', iota: 'ι', kappa: 'κ',
  lambda: 'λ', mu: 'μ', nu: 'ν', xi: 'ξ',
  pi: 'π', varpi: 'ϖ', rho: 'ρ', varrho: 'ϱ',
  sigma: 'σ', varsigma: 'ς', tau: 'τ', upsilon: 'υ',
  phi: 'φ', varphi: 'φ', chi: 'χ', psi: 'ψ', omega: 'ω',

  // Greek uppercase
  Gamma: 'Γ', Delta: 'Δ', Theta: 'Θ', Lambda: 'Λ',
  Xi: 'Ξ', Pi: 'Π', Sigma: 'Σ', Upsilon: 'Υ',
  Phi: 'Φ', Psi: 'Ψ', Omega: 'Ω',

  // Misc math
  infty: '∞', partial: '∂', nabla: '∇', hbar: 'ℏ',
  sum: 'Σ', prod: 'Π', int: '∫', oint: '∮',
  iint: '∬', iiint: '∭',
  ldots: '…', cdots: '⋯', vdots: '⋮', ddots: '⋱',
  angle: '∠', triangle: '△', square: '□', diamond: '◇',
  prime: '′', backslash: '\\',

  // Named functions (rendered as roman text in math)
  sin: 'sin', cos: 'cos', tan: 'tan', cot: 'cot',
  sec: 'sec', csc: 'csc',
  arcsin: 'arcsin', arccos: 'arccos', arctan: 'arctan',
  sinh: 'sinh', cosh: 'cosh', tanh: 'tanh',
  log: 'log', ln: 'ln', exp: 'exp', lg: 'lg',
  lim: 'lim', limsup: 'lim sup', liminf: 'lim inf',
  max: 'max', min: 'min', sup: 'sup', inf: 'inf',
  gcd: 'gcd', lcm: 'lcm', det: 'det', mod: 'mod', deg: 'deg',
  ker: 'ker', dim: 'dim', rank: 'rank',

  // Escaped chars
  '%': '%', '#': '#', '&': '&', '_': '_',
  '{': '{', '}': '}',

  // Spacing (collapse to nothing in OMML — Word manages its own math spacing)
  ',': '', ';': '', ':': '', '!': '', ' ': ' ',
  quad: ' ', qquad: '  ', enspace: ' ', thinspace: '',
};

// ─── Tokenizer ─────────────────────────────────────────────────────────────────

type Tok =
  | { t: 'cmd'; v: string }   // \frac, \alpha, {, \\, etc.
  | { t: 'lb' }               // {
  | { t: 'rb' }               // }
  | { t: 'lbk' }             // [
  | { t: 'rbk' }             // ]
  | { t: 'sup' }             // ^
  | { t: 'sub' }             // _
  | { t: 'chr'; v: string }; // any other character

function tokenize(s: string): Tok[] {
  const toks: Tok[] = [];
  let i = 0;
  while (i < s.length) {
    const c = s[i];
    if (c === '\\') {
      const j0 = i + 1;
      if (j0 >= s.length) { i++; continue; }
      if (/[a-zA-Z@]/.test(s[j0])) {
        // Multi-char command: \alpha, \frac, etc.
        let j = j0;
        while (j < s.length && /[a-zA-Z@]/.test(s[j])) j++;
        toks.push({ t: 'cmd', v: s.slice(j0, j) });
        // skip trailing whitespace after alpha command
        while (j < s.length && /[ \t]/.test(s[j])) j++;
        i = j;
      } else {
        // Single non-alpha char: \\ \{ \} \, etc.
        toks.push({ t: 'cmd', v: s[j0] });
        i = j0 + 1;
      }
    } else if (c === '{') { toks.push({ t: 'lb' }); i++; }
    else if (c === '}') { toks.push({ t: 'rb' }); i++; }
    else if (c === '[') { toks.push({ t: 'lbk' }); i++; }
    else if (c === ']') { toks.push({ t: 'rbk' }); i++; }
    else if (c === '^') { toks.push({ t: 'sup' }); i++; }
    else if (c === '_') { toks.push({ t: 'sub' }); i++; }
    else if (/[ \t\n\r]/.test(c)) { i++; } // skip whitespace in math
    else { toks.push({ t: 'chr', v: c }); i++; }
  }
  return toks;
}

// ─── AST ─────────────────────────────────────────────────────────────────────

type MNode =
  | { k: 'seq'; c: MNode[] }
  | { k: 'run'; v: string }
  | { k: 'frac'; n: MNode; d: MNode }
  | { k: 'sqrt'; deg?: MNode; body: MNode }
  | { k: 'sup'; base: MNode; exp: MNode }
  | { k: 'sub'; base: MNode; s: MNode }
  | { k: 'ss'; base: MNode; s: MNode; e: MNode }
  | { k: 'round'; c: MNode }
  | { k: 'square'; c: MNode }
  | { k: 'angled'; c: MNode }
  | { k: 'curly'; c: MNode };

// ─── Parser ───────────────────────────────────────────────────────────────────

type Pos = { i: number };

type StopAt = 'rb' | 'rbk' | null;

function parseSeq(toks: Tok[], pos: Pos, stopAt: StopAt = null): MNode {
  const items: MNode[] = [];
  while (pos.i < toks.length) {
    const cur = toks[pos.i];
    if (stopAt === 'rb' && cur.t === 'rb') break;
    if (stopAt === 'rbk' && cur.t === 'rbk') break;

    const atom = parseAtom(toks, pos);

    // Collect postfix _ and ^ (at most one of each)
    let sub: MNode | undefined;
    let sup: MNode | undefined;
    let guard = 2;
    while (pos.i < toks.length && guard-- > 0) {
      if (toks[pos.i].t === 'sub' && !sub) {
        pos.i++;
        sub = parseGroup(toks, pos);
      } else if (toks[pos.i].t === 'sup' && !sup) {
        pos.i++;
        sup = parseGroup(toks, pos);
      } else break;
    }

    if (sub && sup) items.push({ k: 'ss', base: atom, s: sub, e: sup });
    else if (sup)   items.push({ k: 'sup', base: atom, exp: sup });
    else if (sub)   items.push({ k: 'sub', base: atom, s: sub });
    else            items.push(atom);
  }

  // Coalesce adjacent 'run' nodes to reduce OMML element count
  const coalesced: MNode[] = [];
  for (const item of items) {
    const last = coalesced[coalesced.length - 1];
    if (item.k === 'run' && last?.k === 'run') {
      (last as { k: 'run'; v: string }).v += item.v;
    } else {
      coalesced.push(item);
    }
  }

  if (coalesced.length === 0) return { k: 'run', v: '' };
  if (coalesced.length === 1) return coalesced[0];
  return { k: 'seq', c: coalesced };
}

/** Parse a group: either {seq} or a single atom */
function parseGroup(toks: Tok[], pos: Pos): MNode {
  if (pos.i >= toks.length) return { k: 'run', v: '' };
  if (toks[pos.i].t === 'lb') {
    pos.i++; // consume {
    const node = parseSeq(toks, pos, 'rb');
    if (pos.i < toks.length && toks[pos.i].t === 'rb') pos.i++; // consume }
    return node;
  }
  return parseAtom(toks, pos);
}

function parseAtom(toks: Tok[], pos: Pos): MNode {
  if (pos.i >= toks.length) return { k: 'run', v: '' };
  const tok = toks[pos.i];

  // ── Plain character
  if (tok.t === 'chr') { pos.i++; return { k: 'run', v: tok.v }; }

  // ── Grouped expression {…}
  if (tok.t === 'lb') {
    pos.i++;
    const node = parseSeq(toks, pos, 'rb');
    if (pos.i < toks.length && toks[pos.i].t === 'rb') pos.i++;
    return node;
  }

  // ── Command
  if (tok.t === 'cmd') {
    pos.i++;
    const name = tok.v;

    // Line break \\ → space
    if (name === '\\') return { k: 'run', v: ' ' };

    // ── \frac{num}{den}
    if (name === 'frac' || name === 'dfrac' || name === 'tfrac') {
      return { k: 'frac', n: parseGroup(toks, pos), d: parseGroup(toks, pos) };
    }

    // ── \sqrt[n]{x} or \sqrt{x}
    if (name === 'sqrt') {
      let deg: MNode | undefined;
      if (pos.i < toks.length && toks[pos.i].t === 'lbk') {
        pos.i++; // consume [
        deg = parseSeq(toks, pos, 'rbk');
        if (pos.i < toks.length && toks[pos.i].t === 'rbk') pos.i++; // consume ]
      }
      return { k: 'sqrt', deg, body: parseGroup(toks, pos) };
    }

    // ── \left … \right
    if (name === 'left') {
      return parseLeftRight(toks, pos);
    }

    // ── \begin{env}…\end{env}  (matrices, cases — render placeholder)
    if (name === 'begin') {
      return parseEnvironment(toks, pos);
    }

    // ── Decoration modifiers: pass through to argument
    if ([
      'text', 'mathrm', 'mathbf', 'mathit', 'mathbb', 'mathcal', 'mathsf',
      'textbf', 'textit', 'textrm', 'textup', 'operatorname',
      'overline', 'underline', 'hat', 'bar', 'vec', 'tilde', 'dot', 'ddot',
      'widehat', 'widetilde', 'overrightarrow', 'overleftarrow',
      'overbrace', 'underbrace', 'not', 'cancel', 'bcancel', 'xcancel',
    ].includes(name)) {
      if (pos.i < toks.length && toks[pos.i].t === 'lb') {
        return parseGroup(toks, pos);
      }
      return { k: 'run', v: '' };
    }

    // ── Symbol lookup
    if (name in SYM) {
      const v = SYM[name];
      return { k: 'run', v };
    }

    // ── Unknown command: render as plain text (safe fallback)
    return { k: 'run', v: name };
  }

  // Skip unexpected token types (lbk, rbk, sub, sup outside expected positions)
  pos.i++;
  return { k: 'run', v: '' };
}

/** Parse \left<bracket> … \right<bracket> */
function parseLeftRight(toks: Tok[], pos: Pos): MNode {
  type BrType = 'round' | 'square' | 'angled' | 'curly';
  let kind: BrType = 'round';

  if (pos.i < toks.length) {
    const bt = toks[pos.i];
    if (bt.t === 'lbk') {
      kind = 'square'; pos.i++;
    } else if (bt.t === 'lb') {
      kind = 'curly'; pos.i++;
    } else if (bt.t === 'cmd' && (bt.v === 'langle' || bt.v === 'lvert' || bt.v === 'lVert')) {
      kind = 'angled'; pos.i++;
    } else if (bt.t === 'cmd' && bt.v === 'lbrace') {
      kind = 'curly'; pos.i++;
    } else if (bt.t === 'cmd' && bt.v === 'lbrack') {
      kind = 'square'; pos.i++;
    } else if (bt.t === 'chr' && bt.v === '[') {
      kind = 'square'; pos.i++;
    } else if (bt.t === 'chr' && bt.v === '(') {
      kind = 'round'; pos.i++;
    } else if (bt.t === 'chr' && bt.v === '{') {
      kind = 'curly'; pos.i++;
    } else if (bt.t === 'cmd' && bt.v === '.') {
      // \left. = invisible bracket — still consume
      pos.i++;
    } else if (bt.t === 'chr' || bt.t === 'cmd') {
      pos.i++; // consume unknown bracket char
    }
  }

  const inner = parseUntilRight(toks, pos);
  return { k: kind, c: inner };
}

/** Parse content until \right is encountered */
function parseUntilRight(toks: Tok[], pos: Pos): MNode {
  const items: MNode[] = [];
  while (pos.i < toks.length) {
    const cur = toks[pos.i];
    if (cur.t === 'cmd' && cur.v === 'right') {
      pos.i++; // consume \right
      // consume the closing bracket char
      if (pos.i < toks.length) {
        const bt = toks[pos.i];
        if (bt.t !== 'cmd' || bt.v !== 'right') pos.i++;
      }
      break;
    }

    const atom = parseAtom(toks, pos);
    let sub: MNode | undefined;
    let sup: MNode | undefined;
    let guard = 2;
    while (pos.i < toks.length && guard-- > 0) {
      if (toks[pos.i].t === 'sub' && !sub) { pos.i++; sub = parseGroup(toks, pos); }
      else if (toks[pos.i].t === 'sup' && !sup) { pos.i++; sup = parseGroup(toks, pos); }
      else break;
    }
    if (sub && sup) items.push({ k: 'ss', base: atom, s: sub, e: sup });
    else if (sup)   items.push({ k: 'sup', base: atom, exp: sup });
    else if (sub)   items.push({ k: 'sub', base: atom, s: sub });
    else            items.push(atom);
  }

  // Coalesce runs
  const coalesced: MNode[] = [];
  for (const item of items) {
    const last = coalesced[coalesced.length - 1];
    if (item.k === 'run' && last?.k === 'run') {
      (last as { k: 'run'; v: string }).v += item.v;
    } else coalesced.push(item);
  }

  if (coalesced.length === 0) return { k: 'run', v: '' };
  if (coalesced.length === 1) return coalesced[0];
  return { k: 'seq', c: coalesced };
}

/** Skip \begin{env}…\end{env} and return a placeholder */
function parseEnvironment(toks: Tok[], pos: Pos): MNode {
  // skip {envname}
  if (pos.i < toks.length && toks[pos.i].t === 'lb') {
    pos.i++;
    while (pos.i < toks.length && toks[pos.i].t !== 'rb') pos.i++;
    if (pos.i < toks.length) pos.i++; // consume }
  }
  // skip body until \end{...}
  let depth = 1;
  while (pos.i < toks.length && depth > 0) {
    const t = toks[pos.i++];
    if (t.t === 'cmd' && t.v === 'begin') depth++;
    if (t.t === 'cmd' && t.v === 'end') {
      depth--;
      if (depth === 0) {
        // skip {envname}
        if (pos.i < toks.length && toks[pos.i].t === 'lb') {
          pos.i++;
          while (pos.i < toks.length && toks[pos.i].t !== 'rb') pos.i++;
          if (pos.i < toks.length) pos.i++;
        }
      }
    }
  }
  return { k: 'run', v: '[…]' };
}

// ─── OMML builder ─────────────────────────────────────────────────────────────

function toOmml(node: MNode): MathComponent[] {
  switch (node.k) {
    case 'seq':
      return node.c.flatMap(toOmml);

    case 'run':
      return node.v ? [new MathRun(node.v)] : [];

    case 'frac':
      return [new MathFraction({
        numerator:   safe(toOmml(node.n)),
        denominator: safe(toOmml(node.d)),
      })];

    case 'sqrt':
      return [new MathRadical(
        node.deg
          ? { children: safe(toOmml(node.body)), degree: safe(toOmml(node.deg)) }
          : { children: safe(toOmml(node.body)) },
      )];

    case 'sup':
      return [new MathSuperScript({
        children:    safe(toOmml(node.base)),
        superScript: safe(toOmml(node.exp)),
      })];

    case 'sub':
      return [new MathSubScript({
        children:  safe(toOmml(node.base)),
        subScript: safe(toOmml(node.s)),
      })];

    case 'ss':
      return [new MathSubSuperScript({
        children:    safe(toOmml(node.base)),
        subScript:   safe(toOmml(node.s)),
        superScript: safe(toOmml(node.e)),
      })];

    case 'round':
      return [new MathRoundBrackets({ children: safe(toOmml(node.c)) })];

    case 'square':
      return [new MathSquareBrackets({ children: safe(toOmml(node.c)) })];

    case 'angled':
      return [new MathAngledBrackets({ children: safe(toOmml(node.c)) })];

    case 'curly':
      return [new MathCurlyBrackets({ children: safe(toOmml(node.c)) })];
  }
}

/** Ensures a MathComponent array is never empty (docx requires ≥ 1 child) */
function safe(arr: MathComponent[]): MathComponent[] {
  return arr.length ? arr : [new MathRun(' ')];
}

// ─── Public API ───────────────────────────────────────────────────────────────

/**
 * Converts a LaTeX formula string to a docx `Math` element (OMML).
 *
 * The returned element is a native Word equation — fully editable, structurally
 * correct (fractions are stacked, radicals have vincula, etc.), and works in all
 * Word-compatible software that supports OOXML (Word 2013+, LibreOffice, etc.).
 *
 * @param latex  The LaTeX source, WITHOUT outer delimiters (no `$` or `$$`).
 */
export function latexToMath(latex: string): MathEl {
  try {
    const toks = tokenize(latex.trim());
    const pos: Pos = { i: 0 };
    const ast = parseSeq(toks, pos);
    const children = toOmml(ast);
    return new MathEl({ children: children.length ? children : [new MathRun(latex)] });
  } catch {
    // Safe fallback: raw text in a math container
    return new MathEl({ children: [new MathRun(latex)] });
  }
}
