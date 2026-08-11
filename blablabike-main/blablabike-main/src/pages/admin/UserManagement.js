import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Users, Search, Filter, Eye, Ban, CheckCircle, XCircle,
  RefreshCw, Shield, AlertCircle, ChevronLeft, ChevronRight,
  ArrowUpDown, ArrowUp, ArrowDown, Loader2, UserCheck, Bike, Mail, Phone,
  ToggleLeft, ToggleRight, ChevronDown, Check, Send, MessageSquare, CheckSquare, Square, Bell, Smartphone
} from 'lucide-react';
import AdminLayout from '../../components/AdminLayout';
import getApiConfig from '../../config/api';

const UserManagement = () => {
  const navigate = useNavigate();

  // ─── Data state ────────────────────────────────────────
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // ─── Pagination state ──────────────────────────────────
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // ─── Search & filter state ─────────────────────────────
  const [searchInput, setSearchInput] = useState('');   // raw input value
  const [searchTerm, setSearchTerm] = useState('');     // debounced value sent to API
  const [filterActive, setFilterActive] = useState('all');   // 'all' | 'true' | 'false'
  const [filterRole, setFilterRole] = useState('all');       // 'all' | 'GUEST' | 'USER' | 'RIDER' | 'DRIVER' | 'ADMIN'
  const [showFilters, setShowFilters] = useState(false);

  // ─── Sort state ────────────────────────────────────────
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');

  // ─── User selection & MSG91/Push messaging state ───────
  const [selectedUserIds, setSelectedUserIds] = useState([]);
  const [selectAllFiltered, setSelectAllFiltered] = useState(false);
  const [showMessageModal, setShowMessageModal] = useState(false);
  const [messageForm, setMessageForm] = useState({
    title: 'Update from BikePooling',
    message: '',
    sendPush: true,
    sendSms: true,
  });
  const [sendingMessage, setSendingMessage] = useState(false);

  // ─── User detail modal ────────────────────────────────
  const [selectedUser, setSelectedUser] = useState(null);
  const [actionLoading, setActionLoading] = useState(null); // 'status' | 'role' | null
  const [toast, setToast] = useState(null); // { message, type: 'success' | 'error' }

  // ─── Debounce timer ref ────────────────────────────────
  const debounceRef = useRef(null);
  const toastTimeoutRef = useRef(null);

  // ─── Build API URL ─────────────────────────────────────
  const buildUrl = useCallback(() => {
    const { baseURL } = getApiConfig();
    const params = new URLSearchParams();
    params.set('page', page.toString());
    params.set('size', size.toString());
    params.set('sortBy', sortBy);
    params.set('sortDir', sortDir);
    if (searchTerm.trim()) params.set('search', searchTerm.trim());
    if (filterActive !== 'all') params.set('active', filterActive);
    if (filterRole !== 'all') params.set('role', filterRole);
    return `${baseURL}/admin/users?${params.toString()}`;
  }, [page, size, sortBy, sortDir, searchTerm, filterActive, filterRole]);

  // ─── Fetch users from backend ──────────────────────────
  const fetchUsers = useCallback(async () => {
    const token = localStorage.getItem('adminToken');
    if (!token) { navigate('/'); return; }

    setLoading(true);
    setError(null);

    try {
      const res = await fetch(buildUrl(), {
        headers: { 'Authorization': `Bearer ${token}` }
      });

      if (res.status === 401 || res.status === 403) {
        localStorage.removeItem('adminToken');
        localStorage.removeItem('adminUser');
        navigate('/');
        return;
      }

      if (!res.ok) throw new Error(`Server error: ${res.status}`);

      const json = await res.json();
      // ApiResponse<PagedResponse<UserDTO>>
      const pagedData = json.data;

      setUsers(pagedData.content || []);
      setTotalPages(pagedData.totalPages || 0);
      setTotalElements(pagedData.totalElements || 0);
    } catch (err) {
      console.error('Failed to fetch users:', err);
      setError(err.message);
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }, [buildUrl, navigate]);

  // ─── Helper: make an admin API call ─────────────────────
  const adminApiFetch = async (url, method, body) => {
    const token = localStorage.getItem('adminToken');
    if (!token) throw new Error('No admin token');

    console.log(`[Admin API] ${method} ${url}`, body);

    const res = await fetch(url, {
      method,
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body)
    });

    // Try to parse response body regardless of status
    let json;
    try { json = await res.json(); } catch { json = null; }

    console.log(`[Admin API] Response ${res.status}:`, json);

    if (!res.ok) {
      const serverMsg = json?.message || json?.error || `HTTP ${res.status}`;
      throw new Error(serverMsg);
    }

    return json;
  };

  // ─── Update user status (PATCH /api/admin/users/{id}/status) ──
  const updateUserStatus = async (userId, active) => {
    setActionLoading('status');
    try {
      const { baseURL } = getApiConfig();
      const json = await adminApiFetch(
        `${baseURL}/admin/users/${userId}/status`,
        'PATCH',
        { active }
      );
      const updatedUser = json.data;
      // Update the selected user in the modal
      setSelectedUser(prev => prev ? { ...prev, ...updatedUser } : null);
      // Update the user in the table list
      setUsers(prev => prev.map(u => u.id === userId ? { ...u, ...updatedUser } : u));
      showToast(json.message || `User ${active ? 'activated' : 'deactivated'} successfully`, 'success');
    } catch (err) {
      console.error('Failed to update status:', err);
      // Network/CORS errors have no response — the message will be "Failed to fetch"
      const msg = err.message === 'Failed to fetch'
        ? 'Network error — check CORS config on backend (PATCH not allowed?)'
        : err.message;
      showToast(msg, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  // ─── Update user role (PATCH /api/admin/users/{id}/role) ──
  const updateUserRole = async (userId, role) => {
    setActionLoading('role');
    try {
      const { baseURL } = getApiConfig();
      const json = await adminApiFetch(
        `${baseURL}/admin/users/${userId}/role`,
        'PATCH',
        { role }
      );
      const updatedUser = json.data;
      setSelectedUser(prev => prev ? { ...prev, ...updatedUser } : null);
      setUsers(prev => prev.map(u => u.id === userId ? { ...u, ...updatedUser } : u));
      showToast(json.message || `Role changed to ${role} successfully`, 'success');
    } catch (err) {
      console.error('Failed to update role:', err);
      const msg = err.message === 'Failed to fetch'
        ? 'Network error — check CORS config on backend (PATCH not allowed?)'
        : err.message;
      showToast(msg, 'error');
    } finally {
      setActionLoading(null);
    }
  };

  // ─── Selection Helpers ─────────────────────────────────
  const isUserSelected = (id) => selectedUserIds.includes(id);

  const toggleSelectUser = (id) => {
    setSelectAllFiltered(false);
    setSelectedUserIds(prev =>
      prev.includes(id) ? prev.filter(item => item !== id) : [...prev, id]
    );
  };

  const toggleSelectAllPage = () => {
    setSelectAllFiltered(false);
    const currentPageIds = users.map(u => u.id).filter(Boolean);
    const allPageSelected = currentPageIds.every(id => selectedUserIds.includes(id));

    if (allPageSelected) {
      setSelectedUserIds(prev => prev.filter(id => !currentPageIds.includes(id)));
    } else {
      setSelectedUserIds(prev => Array.from(new Set([...prev, ...currentPageIds])));
    }
  };

  const handleSelectAllFiltered = () => {
    setSelectAllFiltered(true);
    setSelectedUserIds(users.map(u => u.id));
  };

  const clearSelection = () => {
    setSelectedUserIds([]);
    setSelectAllFiltered(false);
  };

  const isAllPageSelected = users.length > 0 && users.map(u => u.id).every(id => selectedUserIds.includes(id));

  // ─── Send Notification / SMS (MSG91) Handler ────────────
  const handleSendAdminMessage = async (e) => {
    if (e) e.preventDefault();
    if (!messageForm.message.trim()) {
      showToast('Please enter message content', 'error');
      return;
    }
    if (!messageForm.sendPush && !messageForm.sendSms) {
      showToast('Please select at least one delivery channel (Push or SMS)', 'error');
      return;
    }

    setSendingMessage(true);
    try {
      const { baseURL } = getApiConfig();
      const payload = {
        userIds: selectAllFiltered ? null : selectedUserIds,
        targetAllFiltered: selectAllFiltered,
        search: searchTerm.trim() || null,
        active: filterActive !== 'all' ? (filterActive === 'true') : null,
        role: filterRole !== 'all' ? filterRole : null,
        sendPush: messageForm.sendPush,
        sendSms: messageForm.sendSms,
        title: messageForm.title.trim() || 'Update from BikePooling',
        message: messageForm.message.trim(),
      };

      const res = await adminApiFetch(`${baseURL}/admin/users/send-message`, 'POST', payload);
      const data = res.data;
      showToast(
        data?.statusMessage || `Message sent successfully! (Push: ${data?.sentPushCount || 0}, SMS: ${data?.sentSmsCount || 0})`,
        'success'
      );
      setShowMessageModal(false);
      clearSelection();
      setMessageForm({
        title: 'Update from BikePooling',
        message: '',
        sendPush: true,
        sendSms: true,
      });
    } catch (err) {
      console.error('Failed to send admin message:', err);
      showToast(err.message || 'Failed to send message', 'error');
    } finally {
      setSendingMessage(false);
    }
  };

  // ─── Toast helper ──────────────────────────────────────
  const showToast = (message, type) => {
    setToast({ message, type });
    if (toastTimeoutRef.current) clearTimeout(toastTimeoutRef.current);
    toastTimeoutRef.current = setTimeout(() => setToast(null), 3500);
  };

  // ─── Initial load ───────────────────────────────────
  useEffect(() => {
    // Backend API handles auth — fetchUsers will handle 401/403
    fetchUsers();
  }, [fetchUsers]);

  // ─── Debounced search ──────────────────────────────────
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      setPage(0);           // reset to first page on new search
      setSearchTerm(searchInput);
    }, 400);
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current); };
  }, [searchInput]);

  // ─── Reset page when filters change ────────────────────
  const handleFilterActiveChange = (v) => { setFilterActive(v); setPage(0); };
  const handleFilterRoleChange = (v) => { setFilterRole(v); setPage(0); };

  // ─── Sort toggle ───────────────────────────────────────
  const handleSort = (column) => {
    if (sortBy === column) {
      setSortDir(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortDir('asc');
    }
    setPage(0);
  };

  // ─── Clear all filters ────────────────────────────────
  const clearFilters = () => {
    setSearchInput('');
    setSearchTerm('');
    setFilterActive('all');
    setFilterRole('all');
    setSortBy('createdAt');
    setSortDir('desc');
    setPage(0);
  };

  const hasActiveFilters = searchTerm || filterActive !== 'all' || filterRole !== 'all';

  // ─── Helper: role badge colors ─────────────────────────
  const getRoleBadge = (role) => {
    const map = {
      ADMIN:  'bg-red-500/20 text-red-400 border-red-500/30',
      DRIVER: 'bg-purple-500/20 text-purple-400 border-purple-500/30',
      RIDER:  'bg-blue-500/20 text-blue-400 border-blue-500/30',
      USER:   'bg-cyan-500/20 text-cyan-400 border-cyan-500/30',
      GUEST:  'bg-gray-500/20 text-gray-400 border-gray-500/30',
    };
    return map[role] || map.GUEST;
  };

  const getRoleIcon = (role) => {
    switch (role) {
      case 'ADMIN':  return <Shield size={12} />;
      case 'DRIVER': return <Bike size={12} />;
      case 'RIDER':  return <UserCheck size={12} />;
      default:       return null;
    }
  };

  // ─── Sort icon ─────────────────────────────────────────
  const SortIcon = ({ column }) => {
    if (sortBy !== column) return <ArrowUpDown size={14} className="text-orange-400/40" />;
    return sortDir === 'asc'
      ? <ArrowUp size={14} className="text-orange-400" />
      : <ArrowDown size={14} className="text-orange-400" />;
  };

  // ─── Pagination helpers ────────────────────────────────
  const pageNumbers = () => {
    const pages = [];
    const maxVisible = 5;
    let start = Math.max(0, page - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible);
    if (end - start < maxVisible) start = Math.max(0, end - maxVisible);
    for (let i = start; i < end; i++) pages.push(i);
    return pages;
  };

  // ─── User Detail Modal (with Change Status & Change Role) ──
  const UserDetailModal = ({ user, onClose }) => {
    const [showRoleDropdown, setShowRoleDropdown] = useState(false);
    const roles = ['GUEST', 'USER', 'RIDER', 'DRIVER', 'ADMIN'];

    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
        <div className="relative bg-gray-800 border border-orange-500/30 rounded-2xl max-w-lg w-full max-h-[90vh] overflow-y-auto p-6 shadow-2xl shadow-orange-500/10" style={{ animation: 'fadeInUp 0.3s ease-out' }}>
          <button onClick={onClose} className="absolute top-4 right-4 p-1.5 hover:bg-gray-700 rounded-lg transition-colors z-10">
            <XCircle size={20} className="text-orange-400" />
          </button>

          {/* ── User Header ────────────────────── */}
          <div className="flex items-center gap-4 mb-6">
            <div className="w-16 h-16 bg-gradient-to-br from-orange-500 to-orange-600 rounded-full flex items-center justify-center shadow-lg shadow-orange-500/20 flex-shrink-0">
              <span className="text-white font-bold text-xl">
                {user.fullName?.charAt(0)?.toUpperCase() || '?'}
              </span>
            </div>
            <div className="min-w-0">
              <h2 className="text-xl font-bold text-white truncate">{user.fullName || 'N/A'}</h2>
              <div className="flex items-center gap-2 mt-1">
                <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium border ${getRoleBadge(user.role)}`}>
                  {getRoleIcon(user.role)} {user.role}
                </span>
                {user.active ? (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-green-500/15 text-green-400 border border-green-500/25">
                    <span className="w-1.5 h-1.5 bg-green-400 rounded-full" /> Active
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-red-500/15 text-red-400 border border-red-500/25">
                    <span className="w-1.5 h-1.5 bg-red-400 rounded-full" /> Inactive
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* ── User Details ───────────────────── */}
          <div className="space-y-2.5 mb-6">
            <InfoRow icon={<Mail size={16} />} label="Email" value={user.email || '—'} />
            <InfoRow icon={<Phone size={16} />} label="Phone" value={user.phone || '—'} />
            <InfoRow icon={<AlertCircle size={16} />} label="KYC Verified" value={user.kycVerified ? 'Yes' : 'No'} />
            {user.createdAt && (
              <InfoRow icon={<Users size={16} />} label="Joined" value={new Date(user.createdAt).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' })} />
            )}
          </div>

          {/* ── Divider ────────────────────────── */}
          <div className="border-t border-orange-500/20 mb-6" />
          <h3 className="text-sm font-semibold text-orange-400 uppercase tracking-wider mb-4">Admin Actions</h3>

          {/* ── Change Status Toggle ────────────── */}
          <div className="bg-gray-900/60 rounded-xl p-4 mb-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-white">Account Status</p>
                <p className="text-xs text-orange-300/60 mt-0.5">
                  {user.active ? 'User can access the platform' : 'User is blocked from the platform'}
                </p>
              </div>
              <button
                onClick={() => updateUserStatus(user.id, !user.active)}
                disabled={actionLoading === 'status'}
                className="flex items-center gap-2 transition-all disabled:opacity-50"
              >
                {actionLoading === 'status' ? (
                  <Loader2 size={28} className="text-orange-400 animate-spin" />
                ) : user.active ? (
                  <ToggleRight size={36} className="text-green-400 hover:text-green-300 transition-colors cursor-pointer" />
                ) : (
                  <ToggleLeft size={36} className="text-gray-500 hover:text-gray-400 transition-colors cursor-pointer" />
                )}
              </button>
            </div>
          </div>

          {/* ── Change Role Dropdown ────────────── */}
          <div className="bg-gray-900/60 rounded-xl p-4">
            <p className="text-sm font-medium text-white mb-1">Change Role</p>
            <p className="text-xs text-orange-300/60 mb-3">Assign a different role to this user</p>
            <div className="relative">
              <button
                onClick={() => setShowRoleDropdown(!showRoleDropdown)}
                disabled={actionLoading === 'role'}
                className="w-full flex items-center justify-between px-4 py-3 bg-gray-700 border border-orange-500/30 rounded-lg hover:border-orange-500/50 transition-all text-white disabled:opacity-50"
              >
                <span className="flex items-center gap-2">
                  {getRoleIcon(user.role)}
                  <span className="font-medium">{user.role || 'Select role'}</span>
                </span>
                {actionLoading === 'role' ? (
                  <Loader2 size={16} className="animate-spin text-orange-400" />
                ) : (
                  <ChevronDown size={16} className={`text-orange-400 transition-transform ${showRoleDropdown ? 'rotate-180' : ''}`} />
                )}
              </button>

              {showRoleDropdown && (
                <div className="absolute top-full left-0 right-0 mt-1 bg-gray-700 border border-orange-500/30 rounded-lg shadow-xl shadow-black/30 z-10 overflow-hidden">
                  {roles.map(role => (
                    <button
                      key={role}
                      onClick={() => {
                        if (role !== user.role) {
                          updateUserRole(user.id, role);
                        }
                        setShowRoleDropdown(false);
                      }}
                      className={`w-full flex items-center justify-between px-4 py-3 text-sm transition-colors ${
                        role === user.role
                          ? 'bg-orange-500/15 text-orange-400'
                          : 'text-white hover:bg-gray-600'
                      }`}
                    >
                      <span className="flex items-center gap-2">
                        {getRoleIcon(role)}
                        <span>{role}</span>
                        <span className={`inline-block w-2 h-2 rounded-full ${
                          role === 'ADMIN' ? 'bg-red-400' :
                          role === 'DRIVER' ? 'bg-purple-400' :
                          role === 'RIDER' ? 'bg-blue-400' :
                          role === 'USER' ? 'bg-cyan-400' : 'bg-gray-400'
                        }`} />
                      </span>
                      {role === user.role && <Check size={16} className="text-orange-400" />}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    );
  };

  const InfoRow = ({ icon, label, value }) => (
    <div className="flex items-center gap-3 p-3 bg-gray-900/60 rounded-lg">
      <div className="text-orange-400">{icon}</div>
      <div className="flex-1">
        <p className="text-xs text-orange-400/70">{label}</p>
        <p className="text-sm font-medium text-white">{value}</p>
      </div>
    </div>
  );

  // ─── Loading skeleton ──────────────────────────────────
  const TableSkeleton = () => (
    <div className="space-y-3 p-6">
      {[...Array(8)].map((_, i) => (
        <div key={i} className="flex items-center gap-4 animate-pulse">
          <div className="w-10 h-10 bg-gray-700 rounded-full" />
          <div className="flex-1 space-y-2">
            <div className="h-4 bg-gray-700 rounded w-1/3" />
            <div className="h-3 bg-gray-700/60 rounded w-1/4" />
          </div>
          <div className="h-6 w-16 bg-gray-700 rounded-full" />
          <div className="h-6 w-14 bg-gray-700 rounded-full" />
        </div>
      ))}
    </div>
  );

  // ─────────────────────── RENDER ────────────────────────
  return (
    <AdminLayout>
      <main className="p-6">
        {/* ── Page Header ─────────────────────────────── */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 mb-6">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <h1 className="text-2xl font-bold text-white mb-1">User Management</h1>
              <p className="text-orange-300 text-sm">
                {totalElements > 0
                  ? `${totalElements.toLocaleString()} user${totalElements !== 1 ? 's' : ''} total`
                  : 'Manage all users and drivers'}
              </p>
            </div>
            <button
              onClick={() => { setPage(0); fetchUsers(); }}
              disabled={loading}
              className="inline-flex items-center gap-2 px-4 py-2.5 bg-orange-500/10 hover:bg-orange-500/20 text-orange-400 rounded-lg transition-colors border border-orange-500/20 disabled:opacity-50"
            >
              <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
              <span className="font-medium text-sm">Refresh</span>
            </button>
          </div>
        </div>

        {/* ── Search & Filters ────────────────────────── */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 mb-6">
          <div className="flex flex-col lg:flex-row gap-4">
            {/* Search input */}
            <div className="flex-1">
              <div className="relative">
                <Search size={20} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-orange-400" />
                <input
                  id="user-search-input"
                  type="text"
                  placeholder="Search users by name, email, or phone..."
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white placeholder-orange-300/50 transition-all"
                />
                {searchInput && (
                  <button
                    onClick={() => setSearchInput('')}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-orange-400/60 hover:text-orange-400"
                  >
                    <XCircle size={16} />
                  </button>
                )}
              </div>
            </div>

            {/* Filter toggle button */}
            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`inline-flex items-center gap-2 px-4 py-3 border rounded-lg transition-colors ${
                showFilters || hasActiveFilters
                  ? 'border-orange-500/60 bg-orange-500/10 text-orange-400'
                  : 'border-orange-500/30 hover:bg-gray-700 text-orange-300'
              }`}
            >
              <Filter size={20} />
              <span className="font-medium">Filters</span>
              {hasActiveFilters && (
                <span className="w-2 h-2 bg-orange-500 rounded-full animate-pulse" />
              )}
            </button>

            {/* Clear filters */}
            {hasActiveFilters && (
              <button
                onClick={clearFilters}
                className="inline-flex items-center gap-2 px-4 py-3 border border-red-500/30 text-red-400 rounded-lg hover:bg-red-500/10 transition-colors"
              >
                <XCircle size={16} />
                <span className="text-sm font-medium">Clear</span>
              </button>
            )}
          </div>

          {/* Filter dropdowns */}
          {showFilters && (
            <div className="mt-4 pt-4 border-t border-orange-500/20 grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-orange-300 mb-2">Status</label>
                <select
                  id="filter-active"
                  value={filterActive}
                  onChange={(e) => handleFilterActiveChange(e.target.value)}
                  className="w-full px-3 py-2.5 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white"
                >
                  <option value="all">All Status</option>
                  <option value="true">Active</option>
                  <option value="false">Inactive</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-orange-300 mb-2">Role</label>
                <select
                  id="filter-role"
                  value={filterRole}
                  onChange={(e) => handleFilterRoleChange(e.target.value)}
                  className="w-full px-3 py-2.5 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white"
                >
                  <option value="all">All Roles</option>
                  <option value="GUEST">Guest</option>
                  <option value="USER">User</option>
                  <option value="RIDER">Rider</option>
                  <option value="DRIVER">Driver</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
            </div>
          )}
        </div>

        {/* ── Error banner ────────────────────────────── */}
        {error && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 mb-6 flex items-center gap-3">
            <AlertCircle size={20} className="text-red-400 flex-shrink-0" />
            <p className="text-red-400 text-sm">{error}</p>
            <button
              onClick={fetchUsers}
              className="ml-auto px-3 py-1.5 bg-red-500/20 text-red-400 rounded-lg text-sm hover:bg-red-500/30 transition-colors"
            >
              Retry
            </button>
          </div>
        )}

        {/* ── Sticky Selection & Action Bar ── */}
        {(selectedUserIds.length > 0 || selectAllFiltered) && (
          <div className="sticky top-4 z-30 mb-6 bg-gradient-to-r from-orange-600 to-orange-500 rounded-xl p-4 shadow-xl text-white flex flex-col sm:flex-row items-center justify-between gap-4 border border-orange-400/40">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-white/20 flex items-center justify-center font-bold">
                <CheckSquare size={20} />
              </div>
              <div>
                <p className="font-bold text-sm">
                  {selectAllFiltered ? `All ${totalElements} filtered users selected` : `${selectedUserIds.length} user(s) selected`}
                </p>
                <p className="text-xs text-white/80">
                  Ready to dispatch FCM Push or MSG91 Text SMS
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2 w-full sm:w-auto">
              <button
                onClick={() => setShowMessageModal(true)}
                className="flex-1 sm:flex-none flex items-center justify-center gap-2 px-4 py-2 bg-white text-orange-600 rounded-lg font-bold text-sm hover:bg-orange-50 transition-all shadow-md"
              >
                <Send size={16} />
                <span>Send Notification / SMS</span>
              </button>
              {!selectAllFiltered && totalElements > selectedUserIds.length && (
                <button
                  onClick={handleSelectAllFiltered}
                  className="px-3 py-2 bg-black/20 hover:bg-black/30 text-white rounded-lg text-xs font-semibold transition-colors"
                >
                  Select All {totalElements} Filtered
                </button>
              )}
              <button
                onClick={clearSelection}
                className="p-2 hover:bg-black/20 text-white rounded-lg transition-colors"
                title="Deselect All"
              >
                <XCircle size={18} />
              </button>
            </div>
          </div>
        )}

        {/* ── Users Table / Empty / Loading ───────────── */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl overflow-hidden">
          {loading ? (
            <TableSkeleton />
          ) : users.length === 0 ? (
            /* Empty state */
            <div className="p-12 text-center">
              <div className="w-20 h-20 mx-auto mb-4 bg-orange-500/10 rounded-full flex items-center justify-center">
                <Users size={36} className="text-orange-400" />
              </div>
              <h3 className="text-lg font-medium text-white mb-2">No users found</h3>
              <p className="text-orange-300/70 max-w-md mx-auto mb-6">
                {hasActiveFilters
                  ? 'No users match your current search or filters. Try adjusting them.'
                  : 'No users registered yet.'}
              </p>
              {hasActiveFilters && (
                <button
                  onClick={clearFilters}
                  className="px-4 py-2 bg-orange-500/10 text-orange-400 rounded-lg hover:bg-orange-500/20 transition-colors border border-orange-500/20"
                >
                  Clear Filters
                </button>
              )}
            </div>
          ) : (
            <>
              {/* Table */}
              <div className="overflow-x-auto">
                <table className="w-full" id="users-table">
                  <thead className="bg-gray-900/80 border-b border-orange-500/20">
                    <tr>
                      <th className="px-4 py-4 text-center w-12">
                        <button
                          onClick={toggleSelectAllPage}
                          className="text-orange-400 hover:text-orange-300 transition-colors p-1"
                          title={isAllPageSelected ? "Unselect all on page" : "Select all on page"}
                        >
                          {isAllPageSelected ? <CheckSquare size={18} /> : <Square size={18} />}
                        </button>
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-semibold text-orange-400 uppercase tracking-wider">
                        User
                      </th>
                      <th
                        className="px-6 py-4 text-left text-xs font-semibold text-orange-400 uppercase tracking-wider cursor-pointer hover:text-orange-300 transition-colors"
                        onClick={() => handleSort('role')}
                      >
                        <span className="inline-flex items-center gap-1">
                          Role <SortIcon column="role" />
                        </span>
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-semibold text-orange-400 uppercase tracking-wider">
                        Status
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-semibold text-orange-400 uppercase tracking-wider">
                        Phone
                      </th>
                      <th
                        className="px-6 py-4 text-left text-xs font-semibold text-orange-400 uppercase tracking-wider cursor-pointer hover:text-orange-300 transition-colors"
                        onClick={() => handleSort('createdAt')}
                      >
                        <span className="inline-flex items-center gap-1">
                          Joined <SortIcon column="createdAt" />
                        </span>
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-semibold text-orange-400 uppercase tracking-wider">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-orange-500/10">
                    {users.map((user, idx) => {
                      const selected = isUserSelected(user.id);
                      return (
                      <tr
                        key={user.id || idx}
                        className={`transition-colors group ${selected ? 'bg-orange-500/10' : 'hover:bg-gray-700/30'}`}
                      >
                        {/* Checkbox cell */}
                        <td className="px-4 py-4 text-center">
                          <button
                            onClick={() => toggleSelectUser(user.id)}
                            className="text-orange-400 hover:text-orange-300 transition-colors p-1"
                          >
                            {selected ? <CheckSquare size={18} /> : <Square size={18} className="text-gray-500" />}
                          </button>
                        </td>
                        {/* User info cell */}
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 bg-gradient-to-br from-orange-500 to-orange-600 rounded-full flex items-center justify-center flex-shrink-0 shadow-md shadow-orange-500/10 group-hover:shadow-orange-500/20 transition-shadow">
                              <span className="text-white font-bold text-sm">
                                {user.fullName?.charAt(0)?.toUpperCase() || '?'}
                              </span>
                            </div>
                            <div className="min-w-0">
                              <div className="text-sm font-medium text-white truncate">{user.fullName || 'N/A'}</div>
                              <div className="text-xs text-orange-300/70 truncate flex items-center gap-1">
                                <Mail size={10} />
                                {user.email || '—'}
                              </div>
                            </div>
                          </div>
                        </td>

                        {/* Role */}
                        <td className="px-6 py-4">
                          <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium border ${getRoleBadge(user.role)}`}>
                            {getRoleIcon(user.role)}
                            {user.role || '—'}
                          </span>
                        </td>

                        {/* Active status */}
                        <td className="px-6 py-4">
                          {user.active ? (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-green-500/15 text-green-400 border border-green-500/25">
                              <span className="w-1.5 h-1.5 bg-green-400 rounded-full" />
                              Active
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-red-500/15 text-red-400 border border-red-500/25">
                              <span className="w-1.5 h-1.5 bg-red-400 rounded-full" />
                              Inactive
                            </span>
                          )}
                        </td>

                        {/* Phone */}
                        <td className="px-6 py-4">
                          <span className="text-sm text-white/80">{user.phone || '—'}</span>
                        </td>

                        {/* Joined */}
                        <td className="px-6 py-4">
                          <span className="text-sm text-white/70">
                            {user.createdAt
                              ? new Date(user.createdAt).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' })
                              : '—'}
                          </span>
                        </td>

                        {/* Actions */}
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-1.5">
                            <button
                              onClick={() => setSelectedUser(user)}
                              className="p-2 hover:bg-orange-500/10 rounded-lg transition-colors"
                              title="View Details"
                            >
                              <Eye size={16} className="text-orange-400" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                  </tbody>
                </table>
              </div>

              {/* ── Pagination ──────────────────────────── */}
              <div className="px-6 py-4 border-t border-orange-500/20 flex flex-col sm:flex-row items-center justify-between gap-4">
                {/* Page info & size selector */}
                <div className="flex items-center gap-4 text-sm text-orange-300/70">
                  <span>
                    Page <span className="font-medium text-white">{page + 1}</span> of{' '}
                    <span className="font-medium text-white">{totalPages || 1}</span>
                  </span>
                  <span className="text-orange-500/30">|</span>
                  <div className="flex items-center gap-2">
                    <span>Show</span>
                    <select
                      id="page-size-select"
                      value={size}
                      onChange={(e) => { setSize(Number(e.target.value)); setPage(0); }}
                      className="px-2 py-1 bg-gray-700 border border-orange-500/30 rounded text-white text-sm focus:outline-none focus:ring-1 focus:ring-orange-500"
                    >
                      <option value={10}>10</option>
                      <option value={20}>20</option>
                      <option value={50}>50</option>
                      <option value={100}>100</option>
                    </select>
                    <span>per page</span>
                  </div>
                </div>

                {/* Page buttons */}
                <div className="flex items-center gap-1.5">
                  <button
                    onClick={() => setPage(0)}
                    disabled={page === 0}
                    className="px-2.5 py-1.5 text-sm border border-orange-500/20 rounded-lg text-orange-300 hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    First
                  </button>
                  <button
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="p-1.5 border border-orange-500/20 rounded-lg text-orange-300 hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    <ChevronLeft size={18} />
                  </button>

                  {pageNumbers().map(p => (
                    <button
                      key={p}
                      onClick={() => setPage(p)}
                      className={`w-9 h-9 text-sm rounded-lg font-medium transition-all ${
                        p === page
                          ? 'bg-orange-500 text-white shadow-lg shadow-orange-500/25'
                          : 'text-orange-300 hover:bg-gray-700 border border-orange-500/20'
                      }`}
                    >
                      {p + 1}
                    </button>
                  ))}

                  <button
                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="p-1.5 border border-orange-500/20 rounded-lg text-orange-300 hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    <ChevronRight size={18} />
                  </button>
                  <button
                    onClick={() => setPage(totalPages - 1)}
                    disabled={page >= totalPages - 1}
                    className="px-2.5 py-1.5 text-sm border border-orange-500/20 rounded-lg text-orange-300 hover:bg-gray-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    Last
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </main>

      {/* ── Send Notification & MSG91 SMS Modal ───────────── */}
      {showMessageModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={() => !sendingMessage && setShowMessageModal(false)} />
          <div className="relative bg-gray-800 border border-orange-500/30 rounded-2xl max-w-lg w-full p-6 shadow-2xl z-10">
            <button
              onClick={() => !sendingMessage && setShowMessageModal(false)}
              className="absolute top-4 right-4 p-1.5 hover:bg-gray-700 rounded-lg text-gray-400 hover:text-white transition-colors"
            >
              <XCircle size={20} />
            </button>

            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-xl bg-orange-500/20 text-orange-400 flex items-center justify-center">
                <Send size={22} />
              </div>
              <div>
                <h2 className="text-xl font-bold text-white">Send Notification / SMS</h2>
                <p className="text-xs text-orange-300/70">
                  {selectAllFiltered
                    ? `Targeting ALL ${totalElements} filtered users`
                    : `Targeting ${selectedUserIds.length} selected user(s)`}
                </p>
              </div>
            </div>

            <form onSubmit={handleSendAdminMessage} className="space-y-4">
              {/* Delivery Channels */}
              <div>
                <label className="block text-xs font-semibold text-orange-300 uppercase tracking-wider mb-2">Delivery Channels</label>
                <div className="grid grid-cols-2 gap-3">
                  <label className={`flex items-center gap-2 p-3 rounded-xl border cursor-pointer transition-all ${messageForm.sendPush ? 'bg-orange-500/15 border-orange-500/50 text-white' : 'bg-gray-700/50 border-gray-600 text-gray-400'}`}>
                    <input
                      type="checkbox"
                      checked={messageForm.sendPush}
                      onChange={(e) => setMessageForm({ ...messageForm, sendPush: e.target.checked })}
                      className="hidden"
                    />
                    <Bell size={18} className={messageForm.sendPush ? 'text-orange-400' : 'text-gray-400'} />
                    <div>
                      <p className="text-sm font-semibold">In-App Push</p>
                      <p className="text-[10px] text-gray-400">Via FCM</p>
                    </div>
                  </label>

                  <label className={`flex items-center gap-2 p-3 rounded-xl border cursor-pointer transition-all ${messageForm.sendSms ? 'bg-orange-500/15 border-orange-500/50 text-white' : 'bg-gray-700/50 border-gray-600 text-gray-400'}`}>
                    <input
                      type="checkbox"
                      checked={messageForm.sendSms}
                      onChange={(e) => setMessageForm({ ...messageForm, sendSms: e.target.checked })}
                      className="hidden"
                    />
                    <Smartphone size={18} className={messageForm.sendSms ? 'text-orange-400' : 'text-gray-400'} />
                    <div>
                      <p className="text-sm font-semibold">MSG91 SMS</p>
                      <p className="text-[10px] text-gray-400">Text Message</p>
                    </div>
                  </label>
                </div>
              </div>

              {/* Title */}
              {messageForm.sendPush && (
                <div>
                  <label className="block text-xs font-semibold text-orange-300 uppercase tracking-wider mb-1">Notification Title</label>
                  <input
                    type="text"
                    value={messageForm.title}
                    onChange={(e) => setMessageForm({ ...messageForm, title: e.target.value })}
                    placeholder="Title for push notification..."
                    className="w-full px-3 py-2.5 bg-gray-700 border border-orange-500/30 rounded-lg text-white text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                    required={messageForm.sendPush}
                  />
                </div>
              )}

              {/* Message Body */}
              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="block text-xs font-semibold text-orange-300 uppercase tracking-wider">Message Content</label>
                  <span className="text-[11px] text-gray-400">{messageForm.message.length} chars</span>
                </div>
                <textarea
                  rows={4}
                  value={messageForm.message}
                  onChange={(e) => setMessageForm({ ...messageForm, message: e.target.value })}
                  placeholder="Type your announcement or update message here..."
                  className="w-full px-3 py-2.5 bg-gray-700 border border-orange-500/30 rounded-lg text-white text-sm focus:outline-none focus:ring-2 focus:ring-orange-500"
                  required
                />
              </div>

              {/* Live Message Preview */}
              {messageForm.message.trim() && (
                <div className="p-3 rounded-xl bg-gray-900/80 border border-orange-500/20 text-xs space-y-1">
                  <p className="text-orange-400 font-semibold flex items-center gap-1.5">
                    <MessageSquare size={14} /> Message Preview
                  </p>
                  {messageForm.sendPush && <p className="text-white font-medium">{messageForm.title}</p>}
                  <p className="text-gray-300 leading-relaxed">{messageForm.message}</p>
                </div>
              )}

              {/* Buttons */}
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowMessageModal(false)}
                  disabled={sendingMessage}
                  className="flex-1 py-2.5 bg-gray-700 text-gray-300 rounded-lg font-medium hover:bg-gray-600 transition-colors text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={sendingMessage || !messageForm.message.trim()}
                  className="flex-1 py-2.5 bg-gradient-to-r from-orange-600 to-orange-500 text-white rounded-lg font-bold hover:from-orange-500 hover:to-orange-400 transition-all text-sm flex items-center justify-center gap-2 shadow-lg disabled:opacity-50"
                >
                  {sendingMessage ? (
                    <>
                      <Loader2 size={16} className="animate-spin" />
                      <span>Sending...</span>
                    </>
                  ) : (
                    <>
                      <Send size={16} />
                      <span>Dispatch Message</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── User Detail Modal ───────────────────────────── */}
      {selectedUser && <UserDetailModal user={selectedUser} onClose={() => setSelectedUser(null)} />}

      {/* ── Toast Notification ─────────────────────────── */}
      {toast && (
        <div className={`fixed bottom-6 right-6 z-[60] flex items-center gap-3 px-5 py-3 rounded-xl shadow-2xl border transition-all ${
          toast.type === 'success'
            ? 'bg-green-500/15 border-green-500/30 text-green-400 shadow-green-500/10'
            : 'bg-red-500/15 border-red-500/30 text-red-400 shadow-red-500/10'
        }`} style={{ animation: 'fadeInUp 0.3s ease-out', backdropFilter: 'blur(12px)' }}>
          {toast.type === 'success' ? <CheckCircle size={18} /> : <AlertCircle size={18} />}
          <span className="text-sm font-medium">{toast.message}</span>
          <button onClick={() => setToast(null)} className="ml-2 hover:opacity-70 transition-opacity">
            <XCircle size={14} />
          </button>
        </div>
      )}
    </AdminLayout>
  );
};

export default UserManagement;
