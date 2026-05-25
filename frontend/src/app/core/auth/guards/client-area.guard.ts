import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ROLES } from '../constants/auth.constants';

/**
 * Espace client (sidebar) : uniquement les utilisateurs avec ROLE_CLIENT.
 * Les conseillers / admins sont redirigés vers leur espace.
 */
export const clientAreaGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.currentUser();
  if (!user?.roles?.length) {
    router.navigate(['/login']);
    return false;
  }
  if (user.roles.includes(ROLES.ADMIN)) {
    router.navigate(['/admin/dashboard']);
    return false;
  }
  if (user.roles.includes(ROLES.CONSEILLER)) {
    router.navigate(['/conseiller/dashboard']);
    return false;
  }
  if (user.roles.includes(ROLES.CLIENT)) {
    return true;
  }
  router.navigate(['/login']);
  return false;
};
