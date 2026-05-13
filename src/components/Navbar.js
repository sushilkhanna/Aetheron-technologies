import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Menu, X, Zap } from 'lucide-react';

const Navbar = ({ user, onLoginClick, onSignupClick, onLogout, onProfileClick }) => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const location = useLocation();

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const publicNavLinks = [
    { label: 'Home', to: '/' },
  ];

  const privateNavLinks = [
    { label: 'My Profile', to: '/profile' },
  ];

  const isActive = (path) => location.pathname === path;

  return (
    <nav className={`fixed top-0 w-full z-[60] transition-all duration-300 ${scrolled ? 'glass-dark shadow-2xl' : 'bg-transparent'}`}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-orange-DEFAULT flex items-center justify-center">
              <Zap size={16} className="text-white" fill="white" />
            </div>
            <span className="text-xl font-bold tracking-tight">
              <span className="text-white">Bike</span>
              <span className="gradient-text">Pool</span>
              <span className="text-white">ing</span>
            </span>
          </Link>

          {/* Right side - Menu and Auth */}
          <div className="flex items-center gap-4">
            {/* Desktop links */}
            <div className="hidden md:flex items-center gap-1">
              {/* Public links - always visible */}
              {publicNavLinks.map(({ label, to }) => (
                <Link
                  key={to}
                  to={to}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                    isActive(to)
                      ? 'text-orange-DEFAULT bg-orange-DEFAULT bg-opacity-10'
                      : 'text-gray-300 hover:text-white hover:bg-gray-700 hover:bg-opacity-50'
                  }`}
                >
                  {label}
                </Link>
              ))}
              
              {/* Private links - only when logged in */}
              {user && privateNavLinks.map(({ label, to }) => (
                <Link
                  key={to}
                  to={to}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                    isActive(to)
                      ? 'text-orange-DEFAULT bg-orange-DEFAULT bg-opacity-10'
                      : 'text-gray-300 hover:text-white hover:bg-gray-700 hover:bg-opacity-50'
                  }`}
                >
                  {label}
                </Link>
              ))}
            </div>

            {/* Auth buttons outside menu */}
            <div className="hidden md:flex items-center gap-3">
              {user ? (
                <div className="flex items-center gap-3">
                  <button
                    onClick={onLogout}
                    className="btn-outline px-4 py-1.5 text-sm"
                  >
                    Logout
                  </button>
                </div>
              ) : (
                <>
                  <button
                    onClick={onLoginClick}
                    className="btn-outline px-5 py-2 text-sm"
                  >
                    Login
                  </button>
                  <button
                    onClick={onSignupClick}
                    className="btn-primary px-5 py-2 text-sm"
                  >
                    <span>Sign Up Free</span>
                  </button>
                </>
              )}
            </div>

            {/* Mobile menu button */}
            <button className="md:hidden text-gray-300 hover:text-white" onClick={() => setIsMenuOpen(!isMenuOpen)}>
              {isMenuOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {isMenuOpen && (
        <div className="md:hidden glass-dark border-t border-white border-opacity-10 px-4 py-4 space-y-1 absolute top-16 left-0 right-0">
          {/* Public links - always visible */}
          {publicNavLinks.map(({ label, to }) => (
            <Link
              key={to}
              to={to}
              onClick={() => setIsMenuOpen(false)}
              className={`block px-4 py-2.5 rounded-xl text-sm font-medium transition-all ${
                isActive(to) ? 'text-orange-DEFAULT bg-orange-DEFAULT bg-opacity-10' : 'text-gray-300 hover:text-white hover:bg-gray-700 hover:bg-opacity-50'
              }`}
            >
              {label}
            </Link>
          ))}
          
          {/* Private links - only when logged in */}
          {user && privateNavLinks.map(({ label, to }) => (
            <Link
              key={to}
              to={to}
              onClick={() => setIsMenuOpen(false)}
              className={`block px-4 py-2.5 rounded-xl text-sm font-medium transition-all ${
                isActive(to) ? 'text-orange-DEFAULT bg-orange-DEFAULT bg-opacity-10' : 'text-gray-300 hover:text-white hover:bg-gray-700 hover:bg-opacity-50'
              }`}
            >
              {label}
            </Link>
          ))}
          <div className="pt-2 border-t border-white border-opacity-10 space-y-2">
            {user ? (
              <>
                <button onClick={() => { onLogout(); setIsMenuOpen(false); }} className="w-full btn-outline px-4 py-2.5 text-sm text-left">Logout</button>
              </>
            ) : (
              <>
                <button onClick={() => { onLoginClick(); setIsMenuOpen(false); }} className="w-full btn-outline px-4 py-2.5 text-sm">Login</button>
                <button onClick={() => { onSignupClick(); setIsMenuOpen(false); }} className="w-full btn-primary px-4 py-2.5 text-sm"><span>Sign Up Free</span></button>
              </>
            )}
          </div>
        </div>
      )}
    </nav>
  );
};

export default Navbar;
