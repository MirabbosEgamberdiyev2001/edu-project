import { useEffect, useRef, useCallback } from 'react';
import { testTakingApi } from '@/api/testTakingApi';
import type { SubmitAnswerRequest } from '@/types/testTaking';

const AUTO_SAVE_INTERVAL = 30000; // 30 seconds

export function useAutoSave(attemptId: string, enabled = true) {
  const pendingAnswers = useRef<Map<string, SubmitAnswerRequest>>(new Map());
  const isSaving = useRef(false);

  const addAnswer = useCallback((answer: SubmitAnswerRequest) => {
    pendingAnswers.current.set(answer.questionId, answer);
  }, []);

  const flush = useCallback(async () => {
    if (isSaving.current || pendingAnswers.current.size === 0 || !attemptId) return;

    isSaving.current = true;
    const answers = Array.from(pendingAnswers.current.values());
    pendingAnswers.current.clear();

    try {
      await testTakingApi.batchSaveAnswers(attemptId, { answers });
    } catch {
      // Re-add failed answers for next save
      answers.forEach((a) => pendingAnswers.current.set(a.questionId, a));
    } finally {
      isSaving.current = false;
    }
  }, [attemptId]);

  useEffect(() => {
    if (!enabled || !attemptId) return;

    const interval = setInterval(flush, AUTO_SAVE_INTERVAL);
    return () => {
      clearInterval(interval);
      flush(); // Flush on unmount
    };
  }, [enabled, attemptId, flush]);

  return { addAnswer, flush };
}
