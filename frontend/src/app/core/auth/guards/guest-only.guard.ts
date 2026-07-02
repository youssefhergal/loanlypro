import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ROLES } from '../constants/auth.constants';

/** Page publique : redirige les utilisateurs déjà connectés vers leur espace. */
export const guestOnlyGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    return true;
  }

  const user = auth.currentUser();
  if (user?.roles?.includes(ROLES.ADMIN)) {
    router.navigate(['/admin/dashboard']);
  } else if (user?.roles?.includes(ROLES.CONSEILLER)) {
    router.navigate(['/conseiller/dashboard']);
  } else {
    router.navigate(['/dashboard']);
  }
  return false;
};
