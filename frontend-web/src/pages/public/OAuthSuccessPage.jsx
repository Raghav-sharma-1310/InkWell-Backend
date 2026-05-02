/*
 * This file provides route-level UI and page state for the Inkwell frontend.
 * The comments explain what major functions, components, and helpers do and why they are used.
 */
import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../api/client';
import { resolveDashboardPath } from '../../utils/navigation';
import { LoadingSpinner } from '../../components/ui/LoadingSpinner';

// Provides oauth success page wiring so the framework can apply the expected runtime behavior.
export function OAuthSuccessPage() {
  const [searchParams] = useSearchParams();
  const { setUser } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const accessToken = searchParams.get('accessToken');
    const refreshToken = searchParams.get('refreshToken');

    if (!accessToken) {
      navigate('/login?error=oauth');
      return;
    }

    localStorage.setItem('inkwell.accessToken', accessToken);
    if (refreshToken) {
      localStorage.setItem('inkwell.refreshToken', refreshToken);
    }

    api.get('/api/auth/me')
      .then((response) => {
        const userData = response.data.data;
        setUser(userData);
        navigate(resolveDashboardPath(userData), { replace: true });
      })
      .catch(() => {
        localStorage.removeItem('inkwell.accessToken');
        localStorage.removeItem('inkwell.refreshToken');
        navigate('/login?error=oauth', { replace: true });
      });
  }, [navigate, searchParams, setUser]);

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="text-center">
        <LoadingSpinner size="lg" />
        <p className="mt-4 font-display text-lg font-semibold">Completing sign in...</p>
        <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Please wait while we verify your account.</p>
      </div>
    </div>
  );
}
