import { type ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import type { AuthRole } from '../../types/auth';
import {
  getAdminSession,
  getCurrentAuthRole,
  getMerchantSession,
  getPlatformUserProfile,
} from '../../utils/authSession';
import {
  hasMerchantPermission,
  type MerchantPermission,
} from '../../utils/merchantPermissions';

interface RoleGuardProps {
  children: ReactNode;
  allowedRoles: AuthRole[];
  merchantPermission?: MerchantPermission;
}

export default function RoleGuard({ children, allowedRoles, merchantPermission }: RoleGuardProps) {
  const { currentRole, currentUser, merchantSession, adminSession } = useAuth();
  const effectiveRole = currentRole ?? getCurrentAuthRole();
  const storedSession =
    effectiveRole === 'user'
      ? getPlatformUserProfile()
      : effectiveRole === 'merchant'
        ? getMerchantSession()
        : effectiveRole === 'admin'
          ? getAdminSession()
          : null;

  if (!effectiveRole || !allowedRoles.includes(effectiveRole)) {
    // Redirect to the default home page for the current role
    if (effectiveRole === 'merchant') {
      return <Navigate to="/merchant" replace />;
    }
    if (effectiveRole === 'admin') {
      return <Navigate to="/admin" replace />;
    }
    return <Navigate to="/" replace />;
  }

  // --- Require a server-confirmed session for every role ---
  // currentRole comes from localStorage and can be forged.  Only when a session
  // object (currentUser / merchantSession / adminSession) is present do we know
  // the server accepted the token.  If no session object exists, the role is
  // unverified -- redirect to login so the server can re-validate.
  if (effectiveRole === 'user' && !currentUser && !storedSession) {
    return <Navigate to="/login" replace />;
  }
  if (effectiveRole === 'merchant' && !merchantSession && !storedSession) {
    return <Navigate to="/login" replace />;
  }
  if (effectiveRole === 'admin' && !adminSession && !storedSession) {
    return <Navigate to="/login" replace />;
  }

  if (effectiveRole === 'merchant' && merchantPermission) {
    const session = merchantSession ?? (storedSession && 'employeeRole' in storedSession ? storedSession : null);
    if (!hasMerchantPermission(session?.employeeRole, merchantPermission)) {
      return <Navigate to="/merchant" replace />;
    }
  }

  return <>{children}</>;
}
