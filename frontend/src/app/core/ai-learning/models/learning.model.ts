export interface LearningStep {
  index: number;
  title: string;
  description: string;
  estimatedMinutes: number;
}

export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED';

export interface LearningSession {
  id: number;
  objective: string;
  steps: LearningStep[];
  completedSteps: number[];
  status: SessionStatus;
  completedCount: number;
  totalSteps: number;
  createdAt: string;
  updatedAt: string;
}
