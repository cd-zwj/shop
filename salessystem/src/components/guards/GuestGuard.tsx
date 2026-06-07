import { type ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function GuestGuard({ children }: { children: ReactNode }) {
  const { isAuthenticated, currentRole } = useAuth();

  if (isAuthenticated) {
    if (currentRole === 'merchant') {
      return <Navigate to="/merchant" replace />;
    }
    if (currentRole === 'admin') {
      return <Navigate to="/admin" replace />;
    }
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
