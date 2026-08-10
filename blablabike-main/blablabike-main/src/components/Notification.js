import React, { useEffect } from 'react';
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react';

const Notification = ({ message, type = 'info', onClose, duration = 3000 }) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, duration);

    return () => clearTimeout(timer);
  }, [duration, onClose]);

  const getIcon = () => {
    switch (type) {
      case 'success':
        return <CheckCircle size={20} className="text-green-400" />;
      case 'error':
        return <AlertCircle size={20} className="text-red-400" />;
      case 'warning':
        return <AlertCircle size={20} className="text-yellow-400" />;
      default:
        return <Info size={20} className="text-blue-400" />;
    }
  };
 
  const getStyles = () => {
    switch (type) {
      case 'success':
        return {
          background: 'linear-gradient(135deg, rgba(34,197,94,0.1), rgba(34,197,94,0.05))',
          border: '1px solid rgba(34,197,94,0.2)',
          iconBg: 'rgba(34,197,94,0.2)'
        };
      case 'error':
        return {
          background: 'linear-gradient(135deg, rgba(239,68,68,0.1), rgba(239,68,68,0.05))',
          border: '1px solid rgba(239,68,68,0.2)',
          iconBg: 'rgba(239,68,68,0.2)'
        };
      case 'warning':
        return {
          background: 'linear-gradient(135deg, rgba(245,158,11,0.1), rgba(245,158,11,0.05))',
          border: '1px solid rgba(245,158,11,0.2)',
          iconBg: 'rgba(245,158,11,0.2)'
        };
      default:
        return {
          background: 'linear-gradient(135deg, rgba(59,130,246,0.1), rgba(59,130,246,0.05))',
          border: '1px solid rgba(59,130,246,0.2)',
          iconBg: 'rgba(59,130,246,0.2)'
        };
    }
  };

  const styles = getStyles();

  return (
    <div className="fixed top-4 right-4 z-50 flex items-center gap-3 px-4 py-3 rounded-xl shadow-lg transition-all duration-300 transform"
      style={styles}>
      <div className="flex items-center justify-center w-8 h-8 rounded-full"
        style={{ background: styles.iconBg }}>
        {getIcon()}
      </div>
      <p className="text-white text-sm font-medium">{message}</p>
      <button
        onClick={onClose}
        className="text-gray-400 hover:text-white transition-colors"
      >
        <X size={16} />
      </button>
    </div>
  );
};

export default Notification;
