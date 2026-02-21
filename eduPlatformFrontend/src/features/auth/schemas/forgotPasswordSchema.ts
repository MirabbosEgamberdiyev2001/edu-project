import { z } from 'zod';

export const forgotPasswordSchema = z.object({
  method: z.enum(['email', 'phone']),
  email: z.string().optional(),
  phone: z.string().optional(),
}).superRefine((data, ctx) => {
  if (data.method === 'email') {
    if (!data.email) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'validation:required', path: ['email'] });
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email)) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'validation:email', path: ['email'] });
    }
  } else {
    if (!data.phone) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'validation:required', path: ['phone'] });
    } else if (!/^\+998[0-9]{9}$/.test(data.phone)) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'validation:phone', path: ['phone'] });
    }
  }
});

export type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;
