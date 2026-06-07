import { type ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import type { AuthRole } from '../../types/auth';

interface RoleGuardProps {
  children: ReactNode;
  allowedRoles: AuthRole[];
}

export default function RoleGuard({ children, allowedRoles }: RoleGuardProps) {
  const { currentRole, currentUser, merchantSession, adminSession } = useAuth();

  if (!currentRole || !allowedRoles.includes(currentRole)) {
    // Redirect to the default home page for the current role
    if (currentRole === 'merchant') {
      return <Navigate to="/merchant" replace />;
    }
    if (currentRole === 'admin') {
      return <Navigate to="/admin" replace />;
    }
    return <Navigate to="/" replace />;
  }

  // --- Require a server-confirmed session for every role ---
  // currentRole comes from localStorage and can be forged.  Only when a session
  // object (currentUser / merchantSession / adminSession) is present do we know
  // the server accepted the token.  If no session object exists, the role is
  // unverified -- redirect to login so the server can re-validate.
  if (currentRole === 'user' && !currentUser) {
    return <Navigate to="/login" replace />;
  }
  if (currentRole === 'merchant' && !merchantSession) {
    return <Navigate to="/login" replace />;
  }
  if (currentRole === 'admin' && !adminSession) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}
