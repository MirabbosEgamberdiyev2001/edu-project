export enum GroupStatus {
  ACTIVE = 'ACTIVE',
  ARCHIVED = 'ARCHIVED',
}

export interface GroupDto {
  id: string;
  teacherId: string;
  teacherName: string | null;

  /** Resolved name for the current locale */
  name: string;
  /** Full translations map — populated for edit forms */
  nameTranslations: Record<string, string> | null;

  /** Resolved description for the current locale */
  description: string | null;
  /** Full translations map — populated for edit forms */
  descriptionTranslations: Record<string, string> | null;

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
  nameTranslations: Record<string, string>;
  descriptionTranslations?: Record<string, string>;
}

export interface UpdateGroupRequest {
  nameTranslations?: Record<string, string>;
  descriptionTranslations?: Record<string, string>;
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
