import api from '@/lib/axios';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type {
  GroupDto,
  GroupMemberDto,
  CreateGroupRequest,
  UpdateGroupRequest,
  AddMembersRequest,
  BatchRemoveMembersRequest,
  GroupStatus,
  StudentSearchDto,
} from '@/types/group';

const GROUPS = '/groups';

export interface GroupListParams {
  status?: GroupStatus;
  search?: string;
  page?: number;
  size?: number;
}

export interface MyGroupsParams {
  page?: number;
  size?: number;
}

export const groupApi = {
  createGroup: (data: CreateGroupRequest) =>
    api.post<ApiResponse<GroupDto>>(GROUPS, data),

  getGroups: (params?: GroupListParams) =>
    api.get<ApiResponse<PagedResponse<GroupDto>>>(GROUPS, { params }),

  getMyGroups: (params?: MyGroupsParams) =>
    api.get<ApiResponse<PagedResponse<GroupDto>>>(`${GROUPS}/my`, { params }),

  getGroup: (id: string) =>
    api.get<ApiResponse<GroupDto>>(`${GROUPS}/${id}`),

  updateGroup: (id: string, data: UpdateGroupRequest) =>
    api.put<ApiResponse<GroupDto>>(`${GROUPS}/${id}`, data),

  deleteGroup: (id: string) =>
    api.delete<ApiResponse<void>>(`${GROUPS}/${id}`),

  archiveGroup: (id: string) =>
    api.post<ApiResponse<GroupDto>>(`${GROUPS}/${id}/archive`),

  getMembers: (groupId: string) =>
    api.get<ApiResponse<GroupMemberDto[]>>(`${GROUPS}/${groupId}/members`),

  addMembers: (groupId: string, data: AddMembersRequest) =>
    api.post<ApiResponse<void>>(`${GROUPS}/${groupId}/members`, data),

  removeMember: (groupId: string, studentId: string) =>
    api.delete<ApiResponse<void>>(`${GROUPS}/${groupId}/members/${studentId}`),

  removeMembersBatch: (groupId: string, data: BatchRemoveMembersRequest) =>
    api.delete<ApiResponse<void>>(`${GROUPS}/${groupId}/members/batch`, { data }),

  searchStudents: (params?: { search?: string; page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<StudentSearchDto>>>(`${GROUPS}/students/search`, { params }),
};
