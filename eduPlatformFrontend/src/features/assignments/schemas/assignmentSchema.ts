import { z } from 'zod';

export const assignmentSchema = z.object({
  title: z.string().min(1, 'Title is required').max(200),
  description: z.string().max(1000).optional(),
  groupId: z.string().min(1, 'Group is required'),
  testHistoryId: z.string().min(1, 'Test is required'),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  durationMinutes: z.number().min(1).max(600).optional(),
  maxAttempts: z.number().min(1).max(10).optional(),
  shuffleQuestions: z.boolean().optional(),
  shuffleOptions: z.boolean().optional(),
  showResults: z.boolean().optional(),
  preventTabSwitch: z.boolean().optional(),
  preventCopyPaste: z.boolean().optional(),
});

export type AssignmentFormData = z.infer<typeof assignmentSchema>;
