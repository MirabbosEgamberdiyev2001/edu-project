import { z } from 'zod';
import { PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH } from '@/config';

export const loginSchema = z.object({
  method: z.enum(['email', 'phone']),
  email: z.string().optional(),
  phone: z.string().optional(),
  password: z
    .string()
    .min(PASSWORD_MIN_LENGTH, 'validation:password.min')
    .max(PASSWORD_MAX_LENGTH, 'validation:password.max'),
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

export type LoginFormData = z.infer<typeof loginSchema>;
