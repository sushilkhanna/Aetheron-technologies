import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Users, Search, Filter, Edit2, Trash2, Eye, Ban, CheckCircle, XCircle,
  Download, RefreshCw, Shield, AlertCircle
} from 'lucide-react';
import AdminLayout from '../../components/AdminLayout';

const UserManagement = () => {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  const [filterRole, setFilterRole] = useState('all');
  const [showFilters, setShowFilters] = useState(false);

  useEffect(() => {
    const adminToken = localStorage.getItem('adminToken');
    if (!adminToken) {
      navigate('/');
      return;
    }
    
    loadUsers();
  }, [navigate]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      // TODO: Replace with actual API call
      // const response = await fetch('/api/admin/users', {
      //   headers: { 'Authorization': `Bearer ${localStorage.getItem('adminToken')}` }
      // });
      // const data = await response.json();
      // setUsers(data.users);
      
      // Temporary: Empty array until API is implemented
      setUsers([]);
    } catch (error) {
      console.error('Error loading users:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredUsers = users.filter(user => {
    const matchesSearch = user.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         user.phone?.includes(searchTerm);
    
    const matchesStatus = filterStatus === 'all' || user.status === filterStatus;
    const matchesRole = filterRole === 'all' || user.role === filterRole;
    
    return matchesSearch && matchesStatus && matchesRole;
  });

  const getStatusColor = (status) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-500/20 text-green-400 border-green-500/30';
      case 'INACTIVE': return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
      case 'SUSPENDED': return 'bg-red-500/20 text-red-400 border-red-500/30';
      default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
    }
  };

  const getKYCColor = (kycStatus) => {
    switch (kycStatus) {
      case 'VERIFIED': return 'bg-green-500/20 text-green-400';
      case 'PENDING': return 'bg-yellow-500/20 text-yellow-400';
      case 'FAILED': return 'bg-red-500/20 text-red-400';
      default: return 'bg-gray-500/20 text-gray-400';
    }
  };

  const handleUserAction = async (action, userId) => {
    try {
      // TODO: Implement API calls
      // await fetch(`/api/admin/users/${userId}/${action}`, {
      //   method: 'PUT',
      //   headers: { 'Authorization': `Bearer ${localStorage.getItem('adminToken')}` }
      // });
      loadUsers();
    } catch (error) {
      console.error('Error performing action:', error);
    }
  };

  const handleExportUsers = () => {
    // TODO: Implement export functionality
    console.log('Export users');
  };

  if (loading) {
    return (
      <AdminLayout>
        <div className="min-h-screen flex items-center justify-center">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500 mx-auto mb-4"></div>
            <p className="text-orange-300">Loading users...</p>
          </div>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      {/* Content */}
      <main className="p-6">
        {/* Page Header */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 mb-6">
          <div>
            <h1 className="text-2xl font-bold text-white mb-2">User Management</h1>
            <p className="text-orange-300">Manage all users and drivers</p>
          </div>
        </div>
        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-orange-500/20 rounded-lg">
                <Users size={24} className="text-orange-400" />
              </div>
              <div className="flex items-center gap-1 text-green-400 text-sm">
                <span className="text-lg">↑</span>
                +12%
              </div>
            </div>
            <h3 className="text-2xl font-bold text-white mb-1">{users.length}</h3>
            <p className="text-orange-300 text-sm">Total Users</p>
          </div>
          
          <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-green-500/20 rounded-lg">
                <CheckCircle size={24} className="text-green-400" />
              </div>
              <div className="flex items-center gap-1 text-green-400 text-sm">
                <span className="text-lg">↑</span>
                +8%
              </div>
            </div>
            <h3 className="text-2xl font-bold text-white mb-1">{users.filter(u => u.status === 'ACTIVE').length}</h3>
            <p className="text-orange-300 text-sm">Active Users</p>
          </div>
          
          <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-blue-500/20 rounded-lg">
                <Shield size={24} className="text-blue-400" />
              </div>
            </div>
            <h3 className="text-2xl font-bold text-white mb-1">{users.filter(u => u.role === 'DRIVER').length}</h3>
            <p className="text-orange-300 text-sm">Drivers</p>
          </div>
          
          <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-yellow-500/20 rounded-lg">
                <AlertCircle size={24} className="text-yellow-400" />
              </div>
            </div>
            <h3 className="text-2xl font-bold text-white mb-1">{users.filter(u => u.kycStatus === 'PENDING').length}</h3>
            <p className="text-orange-300 text-sm">Pending KYC</p>
          </div>
        </div>

        {/* Search and Filters */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 mb-6">
          <div className="flex flex-col lg:flex-row gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search size={20} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-orange-400" />
                <input
                  type="text"
                  placeholder="Search users by name, email, or phone..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white placeholder-orange-300"
                />
              </div>
            </div>

            <button
              onClick={() => setShowFilters(!showFilters)}
              className="inline-flex items-center gap-2 px-4 py-3 border border-orange-500/30 rounded-lg hover:bg-gray-700 transition-colors text-orange-300"
            >
              <Filter size={20} />
              Filters
              {showFilters && <XCircle size={16} />}
            </button>
          </div>

          {showFilters && (
            <div className="mt-4 pt-4 border-t border-orange-500/20 grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-orange-300 mb-2">Status</label>
                <select
                  value={filterStatus}
                  onChange={(e) => setFilterStatus(e.target.value)}
                  className="w-full px-3 py-2 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white"
                >
                  <option value="all">All Status</option>
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="SUSPENDED">Suspended</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-orange-300 mb-2">Role</label>
                <select
                  value={filterRole}
                  onChange={(e) => setFilterRole(e.target.value)}
                  className="w-full px-3 py-2 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white"
                >
                  <option value="all">All Roles</option>
                  <option value="USER">User</option>
                  <option value="DRIVER">Driver</option>
                </select>
              </div>
            </div>
          )}
        </div>

        {/* Users Table or Empty State */}
        {filteredUsers.length === 0 ? (
          <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-12 text-center">
            <Users size={48} className="mx-auto text-orange-400 mb-4" />
            <h3 className="text-lg font-medium text-white mb-2">No users found</h3>
            <p className="text-orange-300 mb-6">
              {searchTerm || filterStatus !== 'all' || filterRole !== 'all'
                ? 'Try adjusting your search or filters'
                : 'User data will appear here once API is implemented'
              }
            </p>
          </div>
        ) : (
          <div className="bg-gray-800 border border-orange-500/20 rounded-xl overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-900 border-b border-orange-500/20">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-orange-400 uppercase tracking-wider">User</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-orange-400 uppercase tracking-wider">Role</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-orange-400 uppercase tracking-wider">Status</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-orange-400 uppercase tracking-wider">KYC</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-orange-400 uppercase tracking-wider">Joined</th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-orange-400 uppercase tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-orange-500/10">
                  {filteredUsers.map((user) => (
                    <tr key={user.id} className="hover:bg-gray-700/50">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 bg-orange-500 rounded-full flex items-center justify-center">
                            <span className="text-white font-bold">{user.name?.charAt(0)}</span>
                          </div>
                          <div>
                            <div className="text-sm font-medium text-white">{user.name}</div>
                            <div className="text-sm text-orange-300">{user.email}</div>
                            <div className="text-xs text-orange-400">{user.phone}</div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${
                          user.role === 'DRIVER' ? 'bg-purple-500/20 text-purple-400' : 'bg-blue-500/20 text-blue-400'
                        }`}>
                          {user.role}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium border ${getStatusColor(user.status)}`}>
                          {user.status}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${getKYCColor(user.kycStatus)}`}>
                          {user.kycStatus}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-sm text-white">{new Date(user.joinDate).toLocaleDateString()}</div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <button className="p-1 hover:bg-gray-700 rounded-lg transition-colors" title="View Details">
                            <Eye size={16} className="text-orange-400" />
                          </button>
                          <button className="p-1 hover:bg-gray-700 rounded-lg transition-colors" title="Edit User">
                            <Edit2 size={16} className="text-orange-400" />
                          </button>
                          {user.status === 'ACTIVE' ? (
                            <button onClick={() => handleUserAction('suspend', user.id)} className="p-1 hover:bg-gray-700 rounded-lg transition-colors" title="Suspend User">
                              <Ban size={16} className="text-yellow-400" />
                            </button>
                          ) : (
                            <button onClick={() => handleUserAction('activate', user.id)} className="p-1 hover:bg-gray-700 rounded-lg transition-colors" title="Activate User">
                              <CheckCircle size={16} className="text-green-400" />
                            </button>
                          )}
                          <button onClick={() => {
                            if (window.confirm('Are you sure you want to delete this user?')) {
                              handleUserAction('delete', user.id);
                            }
                          }} className="p-1 hover:bg-gray-700 rounded-lg transition-colors" title="Delete User">
                            <Trash2 size={16} className="text-red-400" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </main>
    </AdminLayout>
  );
};

export default UserManagement;
