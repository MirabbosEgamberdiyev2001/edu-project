import { useEffect, useRef, useCallback } from 'react';
import { testTakingApi } from '@/api/testTakingApi';
import type { SubmitAnswerRequest } from '@/types/testTaking';

const AUTO_SAVE_INTERVAL = 30000; // 30 seconds — periodic backup
const DEBOUNCE_DELAY = 2000; // 2 seconds — flush shortly after last answer change

function getStorageKey(id: string) {
  return `exam_pending_${id}`;
}

export function useAutoSave(attemptId: string, enabled = true) {
  const pendingAnswers = useRef<Map<string, SubmitAnswerRequest>>(new Map());
  const isSaving = useRef(false);
  const debounceTimer = useRef<ReturnType<typeof setTimeout>>();

  // Crash recovery: restore any in-flight answers from a previous session
  useEffect(() => {
    if (!attemptId) return;
    try {
      const raw = localStorage.getItem(getStorageKey(attemptId));
      if (raw) {
        const saved = JSON.parse(raw) as SubmitAnswerRequest[];
        saved.forEach((a) => pendingAnswers.current.set(a.questionId, a));
      }
    } catch {
      // Corrupt entry — ignore
    }
  }, [attemptId]);

  const flush = useCallback(async (): Promise<boolean> => {
    if (isSaving.current || pendingAnswers.current.size === 0 || !attemptId) return true;

    isSaving.current = true;
    const answers = Array.from(pendingAnswers.current.values());
    pendingAnswers.current.clear();

    try {
      await testTakingApi.batchSaveAnswers(attemptId, { answers });
      // Clear backup after confirmed server save
      try { localStorage.removeItem(getStorageKey(attemptId)); } catch { /* ignore */ }
      return true;
    } catch {
      // Re-add failed answers so the next interval retries them
      answers.forEach((a) => pendingAnswers.current.set(a.questionId, a));
      return false;
    } finally {
      isSaving.current = false;
    }
  }, [attemptId]);

  const addAnswer = useCallback((answer: SubmitAnswerRequest) => {
    pendingAnswers.current.set(answer.questionId, answer);

    // Persist to localStorage as crash / hard-refresh backup
    try {
      localStorage.setItem(
        getStorageKey(attemptId),
        JSON.stringify(Array.from(pendingAnswers.current.values())),
      );
    } catch {
      // Quota exceeded or private-mode — silently degrade
    }

    // Debounced flush: save 2 s after last answer change
    clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(flush, DEBOUNCE_DELAY);
  }, [flush, attemptId]);

  useEffect(() => {
    if (!enabled || !attemptId) return;

    const interval = setInterval(flush, AUTO_SAVE_INTERVAL);
    return () => {
      clearInterval(interval);
      clearTimeout(debounceTimer.current);
      flush(); // Best-effort flush on unmount
    };
  }, [enabled, attemptId, flush]);

  return { addAnswer, flush };
}
