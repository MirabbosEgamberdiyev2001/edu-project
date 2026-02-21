import { z } from 'zod';
import { PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH } from '@/config';

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'validation:required'),
    newPassword: z
      .string()
      .min(PASSWORD_MIN_LENGTH, 'validation:password.min')
      .max(PASSWORD_MAX_LENGTH, 'validation:password.max')
      .regex(/[a-z]/, 'validation:password.lowercase')
      .regex(/[A-Z]/, 'validation:password.uppercase')
      .regex(/\d/, 'validation:password.digit'),
    confirmPassword: z.string().min(1, 'validation:required'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'validation:confirmPassword',
    path: ['confirmPassword'],
  });

export type ChangePasswordFormData = z.infer<typeof changePasswordSchema>;
