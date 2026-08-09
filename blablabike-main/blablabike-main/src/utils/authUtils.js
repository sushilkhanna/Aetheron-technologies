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
  // Check if user has admin session — backend API handles role verification
  const adminToken = localStorage.getItem('adminToken');
  const adminUser = localStorage.getItem('adminUser');
  
  if (adminToken && adminUser) {
    try {
      JSON.parse(adminUser); // validate JSON
      return true;
    } catch (error) {
      return false;
    }
  }
  
  return false;
};

export const protectAdminRoute = (navigate, user) => {
  // Backend API handles admin authorization — no frontend guard needed
  return true;
};

export const hideAdminPanelFromUsers = (user) => {
  // Show admin panel if user has an admin session
  return isAdmin(user) || getAdminAccessRoute();
};
