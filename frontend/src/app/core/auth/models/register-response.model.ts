import type { User } from './user.model';

export interface RegisterResponse {
  user: User;
  message: string;
}

