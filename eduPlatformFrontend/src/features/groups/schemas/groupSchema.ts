import { z } from 'zod';

export const groupSchema = z.object({
  name: z.string().min(1).max(100),
  description: z.string().max(500).optional(),
});

export type GroupFormData = z.infer<typeof groupSchema>;
