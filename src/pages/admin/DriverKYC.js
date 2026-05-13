import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserCheck, Search, RefreshCw } from 'lucide-react';
import AdminLayout from '../../components/AdminLayout';

const DriverKYC = () => {
  const navigate = useNavigate();
  const [drivers, setDrivers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    const adminToken = localStorage.getItem('adminToken');
    if (!adminToken) {
      navigate('/');
      return;
    }
    
    loadDrivers();
  }, [navigate]);

  const loadDrivers = async () => {
    try {
      setLoading(true);
      // TODO: Replace with actual API call
      // const response = await fetch('/api/admin/drivers/kyc', {
      //   headers: {
      //     'Authorization': `Bearer ${localStorage.getItem('adminToken')}`
      //   }
      // });
      // const data = await response.json();
      // setDrivers(data.drivers);
      
      // Temporary empty state until API is implemented
      setDrivers([]);
    } catch (error) {
      console.error('Error loading drivers:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <AdminLayout>
        <div className="min-h-screen flex items-center justify-center">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-orange-500 mx-auto mb-4"></div>
            <p className="text-orange-300">Loading driver KYC requests...</p>
          </div>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <main className="p-6">
        {/* Page Header */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 mb-8">
          <div>
            <h1 className="text-2xl font-bold text-white mb-2">Driver KYC Approval</h1>
            <p className="text-orange-300">Review and approve driver verification requests</p>
          </div>
        </div>
        
        {/* Search Section */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6 mb-6">
          <div className="flex flex-col lg:flex-row gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search size={20} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-orange-400" />
                <input
                  type="text"
                  placeholder="Search drivers by name, email, or phone..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 bg-gray-700 border border-orange-500/30 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 text-white placeholder-orange-300"
                />
              </div>
            </div>
          </div>
        </div>

        {/* KYC Requests Section */}
        <div className="bg-gray-800 border border-orange-500/20 rounded-xl p-6">
          <div className="text-center py-12">
            <UserCheck size={48} className="text-orange-400 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-white mb-2">No KYC Requests</h3>
            <p className="text-orange-300">Driver KYC requests will appear here once API is implemented</p>
          </div>
        </div>
      </main>
    </AdminLayout>
  );
};

export default DriverKYC;
