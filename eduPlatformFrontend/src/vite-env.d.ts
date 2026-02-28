/// <reference types="vite/client" />

// MathLive web component — needed for JSX type-checking
declare namespace JSX {
  interface IntrinsicElements {
    'math-field': React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement> & {
      value?: string;
      'default-mode'?: string;
      'letter-shape-style'?: string;
      /** Legacy (pre-0.98) keyboard mode */
      'virtual-keyboard-mode'?: string;
      /** MathLive 0.98+ keyboard policy: 'auto' | 'manual' | 'sandboxed' */
      'math-virtual-keyboard-policy'?: 'auto' | 'manual' | 'sandboxed';
      'virtual-keyboard-target-origin'?: string;
    };
  }
}
