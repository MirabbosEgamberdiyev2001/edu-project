import { z } from 'zod';
import { OTP_LENGTH } from '@/config';

export const otpSchema = z.object({
  code: z
    .string()
    .min(1, 'validation:otp.required')
    .length(OTP_LENGTH, 'validation:otp.length'),
});

export type OtpFormData = z.infer<typeof otpSchema>;
