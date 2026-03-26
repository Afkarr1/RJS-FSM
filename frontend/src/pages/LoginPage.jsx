import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { User, Lock, LogIn, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api/client';

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.1, delayChildren: 0.2 },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' } },
};

const shakeVariants = {
  shake: {
    x: [0, -12, 12, -8, 8, -4, 4, 0],
    transition: { duration: 0.5 },
  },
};

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [shakeKey, setShakeKey] = useState(0);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('Username dan password harus diisi');
      setShakeKey((k) => k + 1);
      return;
    }

    setLoading(true);
    setError('');

    try {
      const { creds, role } = await authApi.login(username, password);
      login(username, role, creds);
      navigate(role === 'ADMIN' ? '/admin' : '/tech', { replace: true });
    } catch (err) {
      setError(err.message || 'Login gagal. Silakan coba lagi.');
      setShakeKey((k) => k + 1);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-neutral-50 p-4">
      {/* Subtle background decoration */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-1/2 -right-1/4 w-[800px] h-[800px] rounded-full bg-primary-500/5 blur-3xl" />
        <div className="absolute -bottom-1/2 -left-1/4 w-[600px] h-[600px] rounded-full bg-primary-500/3 blur-3xl" />
      </div>

      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.6, ease: 'easeOut' }}
        className="relative w-full max-w-4xl glass-card overflow-hidden flex flex-col lg:flex-row"
      >
        {/* Left Branding Panel */}
        <motion.div
          initial={{ opacity: 0, x: -40 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.7, ease: 'easeOut', delay: 0.1 }}
          className="relative lg:w-[45%] bg-gradient-to-br from-primary-500 to-primary-700 px-8 py-10 lg:px-10 lg:py-14 flex flex-col justify-center items-center text-center overflow-hidden"
        >
          {/* Decorative circles */}
          <div className="absolute top-0 right-0 w-40 h-40 rounded-full bg-white/5 -translate-y-1/2 translate-x-1/2" />
          <div className="absolute bottom-0 left-0 w-56 h-56 rounded-full bg-white/5 translate-y-1/3 -translate-x-1/3" />
          <div className="absolute top-1/2 left-1/2 w-32 h-32 rounded-full bg-white/3 -translate-x-1/2 -translate-y-1/2" />

          <motion.div
            variants={containerVariants}
            initial="hidden"
            animate="visible"
            className="relative z-10 flex flex-col items-center"
          >
            {/* Logo mark */}
            <motion.div
              variants={itemVariants}
              className="w-16 h-16 lg:w-20 lg:h-20 bg-white/15 backdrop-blur-sm rounded-2xl flex items-center justify-center mb-5 border border-white/20"
            >
              <span className="text-white font-bold text-2xl lg:text-3xl tracking-tight">RJS</span>
            </motion.div>

            <motion.h1
              variants={itemVariants}
              className="text-white font-bold text-xl lg:text-2xl tracking-tight"
            >
              Restu Jaya Sentosa
            </motion.h1>

            <motion.div
              variants={itemVariants}
              className="mt-2 h-px w-12 bg-white/30 rounded-full"
            />

            <motion.p
              variants={itemVariants}
              className="mt-3 text-white/80 font-medium text-sm lg:text-base tracking-wide"
            >
              Field Service Management
            </motion.p>

            <motion.p
              variants={itemVariants}
              className="mt-6 text-white/50 text-xs max-w-[220px] leading-relaxed hidden lg:block"
            >
              Kelola dan pantau seluruh operasi servis lapangan dalam satu platform terpadu.
            </motion.p>
          </motion.div>
        </motion.div>

        {/* Right Form Panel */}
        <div className="flex-1 px-8 py-10 lg:px-12 lg:py-14 flex flex-col justify-center">
          <motion.div
            variants={containerVariants}
            initial="hidden"
            animate="visible"
          >
            <motion.div variants={itemVariants}>
              <h2 className="text-2xl font-bold text-neutral-800">Masuk</h2>
              <p className="mt-1.5 text-neutral-400 text-sm">
                Masukkan kredensial Anda untuk melanjutkan
              </p>
            </motion.div>

            <motion.form
              variants={itemVariants}
              onSubmit={handleSubmit}
              className="mt-8 space-y-5"
            >
              {/* Username */}
              <motion.div variants={itemVariants}>
                <label className="block text-sm font-medium text-neutral-600 mb-1.5">
                  Username
                </label>
                <div className="relative">
                  <User
                    size={18}
                    className="absolute left-3.5 top-1/2 -translate-y-1/2 text-neutral-400 pointer-events-none"
                  />
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="Masukkan username"
                    autoComplete="username"
                    disabled={loading}
                    className="input-field pl-10"
                  />
                </div>
              </motion.div>

              {/* Password */}
              <motion.div variants={itemVariants}>
                <label className="block text-sm font-medium text-neutral-600 mb-1.5">
                  Password
                </label>
                <div className="relative">
                  <Lock
                    size={18}
                    className="absolute left-3.5 top-1/2 -translate-y-1/2 text-neutral-400 pointer-events-none"
                  />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Masukkan password"
                    autoComplete="current-password"
                    disabled={loading}
                    className="input-field pl-10 pr-11"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    tabIndex={-1}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600 transition-colors"
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </motion.div>

              {/* Error message */}
              <AnimatePresence mode="wait">
                {error && (
                  <motion.div
                    key={shakeKey}
                    variants={shakeVariants}
                    animate="shake"
                    className="flex items-center gap-2 px-4 py-3 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm font-medium"
                  >
                    <svg
                      className="w-4 h-4 shrink-0"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                      strokeWidth={2}
                    >
                      <circle cx="12" cy="12" r="10" />
                      <line x1="12" y1="8" x2="12" y2="12" />
                      <line x1="12" y1="16" x2="12.01" y2="16" />
                    </svg>
                    {error}
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Login button */}
              <motion.div variants={itemVariants}>
                <motion.button
                  type="submit"
                  disabled={loading}
                  whileTap={{ scale: 0.97 }}
                  whileHover={{ y: -2 }}
                  className="btn-primary w-full flex items-center justify-center gap-2 py-3 text-base"
                >
                  {loading ? (
                    <>
                      <svg
                        className="w-5 h-5 animate-spin"
                        viewBox="0 0 24 24"
                        fill="none"
                      >
                        <circle
                          className="opacity-25"
                          cx="12"
                          cy="12"
                          r="10"
                          stroke="currentColor"
                          strokeWidth="4"
                        />
                        <path
                          className="opacity-75"
                          fill="currentColor"
                          d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
                        />
                      </svg>
                      Memproses...
                    </>
                  ) : (
                    <>
                      <LogIn size={18} />
                      Masuk
                    </>
                  )}
                </motion.button>
              </motion.div>
            </motion.form>

            <motion.p
              variants={itemVariants}
              className="mt-8 text-center text-xs text-neutral-300"
            >
              &copy; {new Date().getFullYear()} Restu Jaya Sentosa
            </motion.p>
          </motion.div>
        </div>
      </motion.div>
    </div>
  );
}
