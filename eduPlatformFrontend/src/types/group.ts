export enum GroupStatus {
  ACTIVE = 'ACTIVE',
  ARCHIVED = 'ARCHIVED',
}

export interface GroupDto {
  id: string;
  teacherId: string;
  teacherName: string | null;
  name: string;
  description: string | null;
  subjectId: string | null;
  subjectName: string | null;
  memberCount: number;
  status: GroupStatus;
  createdAt: string;
  updatedAt: string;
}

export interface GroupMemberDto {
  id: string;
  studentId: string;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  joinedAt: string;
}

export interface CreateGroupRequest {
  name: string;
  description?: string;
}

export interface UpdateGroupRequest {
  name?: string;
  description?: string;
}

export interface AddMembersRequest {
  studentIds: string[];
}

export interface StudentSearchDto {
  id: string;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
}

export interface BatchRemoveMembersRequest {
  studentIds: string[];
}
