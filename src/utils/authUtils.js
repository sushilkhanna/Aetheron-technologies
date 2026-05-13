// Role-based access control utilities

export const ROLES = {
  USER: 'USER',
  DRIVER: 'DRIVER',
  ADMIN: 'ADMIN'
};

export const checkUserRole = (user) => {
  if (!user) return ROLES.USER;
  return user.role || ROLES.USER;
};

export const isAdmin = (user) => {
  return checkUserRole(user) === ROLES.ADMIN;
};

export const canAccessAdminPanel = (user) => {
  return isAdmin(user);
};

export const getAdminAccessRoute = () => {
  // Check if user has admin session
  const adminToken = localStorage.getItem('adminToken');
  const adminUser = localStorage.getItem('adminUser');
  
  if (adminToken && adminUser) {
    try {
      const admin = JSON.parse(adminUser);
      return admin.role === ROLES.ADMIN;
    } catch (error) {
      return false;
    }
  }
  
  return false;
};

export const protectAdminRoute = (navigate, user) => {
  // Check if user has admin access
  const hasAdminAccess = getAdminAccessRoute();
  
  if (!hasAdminAccess) {
    // Redirect to login page
    navigate('/');
    return false;
  }
  
  return true;
};

export const hideAdminPanelFromUsers = (user) => {
  // Only show admin panel if user is admin or has admin session
  return isAdmin(user) || getAdminAccessRoute();
};
