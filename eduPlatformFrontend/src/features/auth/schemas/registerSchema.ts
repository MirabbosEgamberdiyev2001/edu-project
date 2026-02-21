import { z } from 'zod';
import { PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH } from '@/config';
import { Role } from '@/types/user';

export const registerSchema = z.object({
  method: z.enum(['email', 'phone']),
  email: z.string().optional(),
  phone: z.string().optional(),
  firstName: z.string().min(1, 'validation:firstName.required').max(100, 'validation:firstName.max'),
  lastName: z.string().min(1, 'validation:lastName.required').max(100, 'validation:lastName.max'),
  password: z
    .string()
    .min(PASSWORD_MIN_LENGTH, 'validation:password.min')
    .max(PASSWORD_MAX_LENGTH, 'validation:password.max')
    .regex(/[a-z]/, 'validation:password.lowercase')
    .regex(/[A-Z]/, 'validation:password.uppercase')
    .regex(/\d/, 'validation:password.digit'),
  confirmPassword: z.string().min(1, 'validation:required'),
  role: z.nativeEnum(Role, { message: 'validation:role' }),
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
  if (data.password !== data.confirmPassword) {
    ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'validation:confirmPassword', path: ['confirmPassword'] });
  }
});

export type RegisterFormData = z.infer<typeof registerSchema>;
