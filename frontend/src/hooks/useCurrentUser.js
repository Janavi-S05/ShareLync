import { useMemo } from 'react';
import { useCookies } from 'react-cookie';

export function useCurrentUser() {
  const [cookies] = useCookies(['user']);
  return useMemo(() => {
    if (!cookies.user) return null;
    try {
      return typeof cookies.user === 'string'
        ? JSON.parse(cookies.user)
        : cookies.user;
    } catch (e) {
      return null;
    }
  }, [cookies.user]);
}
