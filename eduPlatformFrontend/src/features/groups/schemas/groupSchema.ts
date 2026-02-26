import { z } from 'zod';

const translationsMap = z.record(z.string()).refine(
  (map) => Object.values(map).some((v) => v.trim().length > 0),
  { message: 'At least one language is required' },
);

export const groupSchema = z.object({
  nameTranslations: translationsMap,
  descriptionTranslations: z.record(z.string()).optional(),
});

export type GroupFormData = z.infer<typeof groupSchema>;
