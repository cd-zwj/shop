import { useNavigate } from 'react-router-dom';
import { ShieldX, ArrowLeft, Home } from 'lucide-react';
import type { AuthRole } from '../types/auth';

const ROLE_LABELS: Record<AuthRole, string> = {
  admin: 'Administrator',
  merchant: 'Merchant',
  user: 'User',
};

interface RoleDeniedProps {
  requiredRole: AuthRole;
  currentRole: AuthRole;
}

/**
 * Focused "access denied" screen shown when a logged-in user tries to open a route
 * belonging to a different role. Provides clear feedback and a way back.
 */
export default function RoleDenied({ requiredRole, currentRole }: RoleDeniedProps) {
  const navigate = useNavigate();

  return (
    <div className="flex items-center justify-center min-h-[70vh] px-4">
      <div className="flex flex-col items-center gap-5 text-center max-w-sm">
        <div className="w-16 h-16 rounded-full bg-error/10 flex items-center justify-center">
          <ShieldX className="w-8 h-8 text-error" />
        </div>

        <div className="space-y-1.5">
          <h2 className="text-xl font-bold text-on-surface">Access Denied</h2>
          <p className="text-sm text-on-surface-variant leading-relaxed">
            This page requires <span className="font-semibold">{ROLE_LABELS[requiredRole]}</span> access.
            You are currently logged in as <span className="font-semibold">{ROLE_LABELS[currentRole]}</span>.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold rounded-xl
                       bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest
                       transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Go Back
          </button>
          <button
            onClick={() => navigate('/')}
            className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold rounded-xl
                       bg-primary text-on-primary hover:bg-primary-container
                       transition-colors"
          >
            <Home className="w-4 h-4" />
            Home
          </button>
        </div>
      </div>
    </div>
  );
}
