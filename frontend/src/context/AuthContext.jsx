import { createContext, useContext, useState, useCallback } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('rjs_user');
    return saved ? JSON.parse(saved) : null;
  });

  const login = useCallback((username, role, creds) => {
    const userData = { username, role };
    localStorage.setItem('rjs_auth', creds);
    localStorage.setItem('rjs_user', JSON.stringify(userData));
    setUser(userData);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('rjs_auth');
    localStorage.removeItem('rjs_user');
    setUser(null);
  }, []);

  const isAdmin = user?.role === 'ADMIN';
  const isTech = user?.role === 'TECHNICIAN';

  return (
    <AuthContext.Provider value={{ user, login, logout, isAdmin, isTech, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
