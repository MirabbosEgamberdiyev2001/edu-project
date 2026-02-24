import { useEffect, useRef, useCallback } from 'react';
import { testTakingApi } from '@/api/testTakingApi';
import type { SubmitAnswerRequest } from '@/types/testTaking';

const AUTO_SAVE_INTERVAL = 30000; // 30 seconds — periodic backup
const DEBOUNCE_DELAY = 2000; // 2 seconds — flush shortly after last answer change

export function useAutoSave(attemptId: string, enabled = true) {
  const pendingAnswers = useRef<Map<string, SubmitAnswerRequest>>(new Map());
  const isSaving = useRef(false);
  const debounceTimer = useRef<ReturnType<typeof setTimeout>>();

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

  const addAnswer = useCallback((answer: SubmitAnswerRequest) => {
    pendingAnswers.current.set(answer.questionId, answer);

    // Debounced flush: save 2s after last answer change
    clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(flush, DEBOUNCE_DELAY);
  }, [flush]);

  useEffect(() => {
    if (!enabled || !attemptId) return;

    const interval = setInterval(flush, AUTO_SAVE_INTERVAL);
    return () => {
      clearInterval(interval);
      clearTimeout(debounceTimer.current);
      flush(); // Flush on unmount
    };
  }, [enabled, attemptId, flush]);

  return { addAnswer, flush };
}
