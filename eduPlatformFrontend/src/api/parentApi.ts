import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type {
  ParentChildDto,
  GeneratePairingCodeResponse,
  PairWithCodeRequest,
  ChildDashboardDto,
} from '@/types/parent';

const ENDPOINT = '/parent';

export const parentApi = {
  generatePairingCode: () =>
    api.post<ApiResponse<GeneratePairingCodeResponse>>(`${ENDPOINT}/pairing-code`),

  pairWithCode: (data: PairWithCodeRequest) =>
    api.post<ApiResponse<ParentChildDto>>(`${ENDPOINT}/pair`, data),

  revokePairing: (id: string) =>
    api.post<ApiResponse<void>>(`${ENDPOINT}/pairings/${id}/revoke`),

  getChildren: () =>
    api.get<ApiResponse<ParentChildDto[]>>(`${ENDPOINT}/children`),

  getParents: () =>
    api.get<ApiResponse<ParentChildDto[]>>(`${ENDPOINT}/parents`),

  getChildDashboard: (childId: string) =>
    api.get<ApiResponse<ChildDashboardDto>>(`${ENDPOINT}/children/${childId}/dashboard`),
};
