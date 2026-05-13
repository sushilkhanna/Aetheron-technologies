import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Shield, AlertCircle, Edit2, X, Save, Camera, Mail, Phone, Calendar, MessageSquare, Send, Smartphone, ExternalLink, Star, Bike, DollarSign, FileText, TrendingUp, Clock, MapPin } from 'lucide-react';
import authAPI from '../authAPI';

const MyProfile = ({ user, onUpdateUser }) => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('profile');
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState({});
  const [kycStatus, setKycStatus] = useState('PENDING');
  const [showComplaintForm, setShowComplaintForm] = useState(false);
  const [complaintForm, setComplaintForm] = useState({ title: '', description: '' });
  const [userComplaints, setUserComplaints] = useState([]);
  
  // New sections state
  const [userPosts, setUserPosts] = useState([]);
  const [userRatings, setUserRatings] = useState([]);
  const [userRides, setUserRides] = useState([]);
  const [userEarnings, setUserEarnings] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchUserData = useCallback(async () => {
    setLoading(true);
    try {
      await Promise.all([
        fetchUserPosts(),
        fetchUserRatings(),
        fetchUserRides(),
        fetchUserEarnings()
      ]);
    } catch (error) {
      console.error('Error fetching user data:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!user) {
      // Don't redirect immediately, show admin login option
      return;
    }
    setEditForm(user);
    fetchKycStatus();
    fetchUserData();
  }, [user, navigate, fetchUserData]);

  const fetchUserPosts = async () => {
    try {
      const response = await authAPI.getUserPosts();
      if (response.success) {
        setUserPosts(response.data || []);
      }
    } catch (error) {
      console.error('Error fetching user posts:', error);
    }
  };

  const fetchUserRatings = async () => {
    try {
      const response = await authAPI.getUserRatings();
      if (response.success) {
        setUserRatings(response.data || []);
      }
    } catch (error) {
      console.error('Error fetching user ratings:', error);
    }
  };

  const fetchUserRides = async () => {
    try {
      const response = await authAPI.getUserRides();
      if (response.success) {
        setUserRides(response.data || []);
      }
    } catch (error) {
      console.error('Error fetching user rides:', error);
    }
  };

  const fetchUserEarnings = async () => {
    try {
      const response = await authAPI.getUserEarnings();
      if (response.success) {
        setUserEarnings(response.data || []);
      }
    } catch (error) {
      console.error('Error fetching user earnings:', error);
    }
  };

  const fetchKycStatus = async () => {
    try {
      const response = await authAPI.getKycStatus();
      if (response.success) {
        setKycStatus(response.data.status || 'PENDING');
      }
    } catch (error) {
      console.error('Error fetching KYC status:', error);
    }
  };

  const handleSaveProfile = async () => {
    try {
      const response = await authAPI.updateProfile(editForm);
      if (response && response.success) {
        onUpdateUser(editForm);
        localStorage.setItem('user', JSON.stringify(editForm));
        setEditing(false);
      }
    } catch (error) {
      console.error('Error updating profile:', error);
    }
  };

  const handleSubmitComplaint = async (e) => {
    e.preventDefault();
    const newComplaint = {
      id: userComplaints.length + 1,
      ...complaintForm,
      status: 'Pending',
      date: new Date().toISOString().split('T')[0]
    };
    setUserComplaints([newComplaint, ...userComplaints]);
    setComplaintForm({ title: '', description: '' });
    setShowComplaintForm(false);
  };

  const tabs = [
    { id: 'profile', label: 'Profile Info', icon: User },
    { id: 'verification', label: 'Verification', icon: Shield },
    { id: 'posts', label: 'Posts', icon: FileText },
    { id: 'ratings', label: 'Ratings', icon: Star },
    { id: 'rides', label: 'Rides', icon: Bike },
    { id: 'earnings', label: 'Earnings', icon: DollarSign },
    { id: 'complaints', label: 'Support', icon: AlertCircle }
  ];

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 text-white pt-20 px-4 pb-12 flex items-center justify-center">
        <div className="max-w-md w-full bg-gray-800 rounded-2xl p-8 border border-orange-500/20">
          <div className="text-center mb-8">
            <div className="w-20 h-20 bg-gradient-to-br from-orange-500 to-orange-600 rounded-full flex items-center justify-center mx-auto mb-4">
              <Shield size={40} className="text-white" />
            </div>
            <h2 className="text-2xl font-bold text-white mb-2">Admin Access Required</h2>
            <p className="text-gray-300">You need to log in to access the admin panel</p>
          </div>
          
          <div className="space-y-4">
            <button 
              onClick={() => navigate('/')}
              className="w-full py-3 px-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-lg font-medium hover:from-orange-600 hover:to-orange-700 transition-all flex items-center justify-center gap-2"
            >
              <Shield size={20} />
              <span>Admin Login</span>
            </button>
            
            <div className="text-center">
              <p className="text-xs text-gray-400 mb-2">Demo Admin Credentials:</p>
              <p className="text-xs text-gray-500">Phone: 9999999999</p>
              <p className="text-xs text-gray-500">Password: Admin@123</p>
            </div>
            
            <button 
              onClick={() => navigate('/')}
              className="w-full py-2 px-4 text-gray-400 hover:text-white text-sm transition-colors"
            >
              ← Back to Home
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 text-white pt-20 px-4 pb-12">
      <div className="max-w-6xl mx-auto">
        <div className="bg-gradient-to-r from-orange-500 to-orange-600 rounded-2xl p-8 mb-6 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-64 h-64 bg-white opacity-5 rounded-full -mr-32 -mt-32"></div>
          <div className="relative z-10 flex flex-col md:flex-row items-center gap-6">
            <div className="relative">
              <div className="w-24 h-24 bg-white rounded-full flex items-center justify-center text-orange-500 text-3xl font-bold">
                {user?.fullName?.charAt(0) || 'U'}
              </div>
              <button className="absolute bottom-0 right-0 w-8 h-8 bg-white rounded-full flex items-center justify-center text-orange-500 hover:bg-gray-100 transition-colors">
                <Camera size={16} />
              </button>
            </div>
            <div className="flex-1 text-center md:text-left">
              <h1 className="text-3xl font-bold text-white mb-2">{user?.fullName || 'User'}</h1>
              <p className="text-orange-100 mb-3">{user?.email || 'No email'}</p>
              <div className="flex flex-wrap gap-2 justify-center md:justify-start">
                <span className="px-3 py-1 bg-white bg-opacity-20 rounded-full text-sm flex items-center gap-1">
                  <Phone size={14} /> {user?.phone || 'N/A'}
                </span>
                <span className={`px-3 py-1 rounded-full text-sm flex items-center gap-1 ${kycStatus === 'VERIFIED' ? 'bg-green-500' : 'bg-yellow-500'}`}>
                  <Shield size={14} /> {kycStatus}
                </span>
              </div>
            </div>
            <button onClick={() => setEditing(!editing)} className="px-6 py-3 bg-white text-orange-500 rounded-lg font-medium hover:bg-gray-100 transition-colors flex items-center gap-2">
              {editing ? <X size={20} /> : <Edit2 size={20} />}
              {editing ? 'Cancel' : 'Edit Profile'}
            </button>
          </div>
        </div>

        <div className="bg-gray-800 rounded-2xl p-2 mb-6 overflow-x-auto">
          <div className="flex gap-2 min-w-max">
            {tabs.map(tab => (
              <button key={tab.id} onClick={() => setActiveTab(tab.id)} className={`flex items-center gap-2 px-4 py-3 rounded-lg font-medium transition-all whitespace-nowrap ${activeTab === tab.id ? 'bg-gradient-to-r from-orange-500 to-orange-600 text-white shadow-lg' : 'text-gray-300 hover:bg-gray-700'}`}>
                <tab.icon size={18} />
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        <div className="bg-gray-800 rounded-2xl p-6 min-h-[400px]">
          {activeTab === 'profile' && (
            <div>
              <h3 className="text-2xl font-bold text-white mb-6 flex items-center gap-2">
                <User className="text-orange-500" />
                Profile Information
              </h3>
              {editing ? (
                <div className="space-y-4">
                  <div className="grid md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">Full Name</label>
                      <input type="text" value={editForm.fullName || ''} onChange={(e) => setEditForm({...editForm, fullName: e.target.value})} className="w-full bg-gray-700 text-white px-4 py-3 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">Email</label>
                      <input type="email" value={editForm.email || ''} onChange={(e) => setEditForm({...editForm, email: e.target.value})} className="w-full bg-gray-700 text-white px-4 py-3 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500" />
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-300 mb-2">Phone</label>
                    <input type="tel" value={editForm.phone || ''} onChange={(e) => setEditForm({...editForm, phone: e.target.value})} className="w-full bg-gray-700 text-white px-4 py-3 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500" />
                  </div>
                  <div className="flex gap-4 pt-4">
                    <button onClick={handleSaveProfile} className="flex items-center gap-2 bg-green-500 hover:bg-green-600 text-white px-6 py-3 rounded-lg font-medium transition-colors">
                      <Save size={20} />
                      Save Changes
                    </button>
                    <button onClick={() => setEditing(false)} className="bg-gray-600 hover:bg-gray-700 text-white px-6 py-3 rounded-lg font-medium transition-colors">
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <div className="grid md:grid-cols-2 gap-4">
                  {[
                    { label: 'Full Name', value: user?.fullName, icon: User },
                    { label: 'Email', value: user?.email, icon: Mail },
                    { label: 'Phone', value: user?.phone, icon: Phone },
                    { label: 'Member Since', value: user?.memberSince || 'N/A', icon: Calendar }
                  ].map((item, i) => (
                    <div key={i} className="flex items-center gap-4 p-4 bg-gray-700 rounded-lg">
                      <div className="p-3 bg-orange-500 bg-opacity-20 rounded-lg">
                        <item.icon size={24} className="text-orange-500" />
                      </div>
                      <div>
                        <p className="text-gray-400 text-sm">{item.label}</p>
                        <p className="text-white font-medium">{item.value || 'N/A'}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'verification' && (
            <div>
              <h3 className="text-2xl font-bold text-white mb-6 flex items-center gap-2">
                <Shield className="text-orange-500" />
                KYC Verification & Vehicle Management
              </h3>
              <div className="space-y-4">
                <div className="p-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl text-center">
                  <div className="w-20 h-20 bg-white bg-opacity-20 rounded-full flex items-center justify-center mx-auto mb-4">
                    <Smartphone size={40} className="text-white" />
                  </div>
                  <h4 className="text-2xl font-bold text-white mb-3">Use Our Mobile App</h4>
                  <p className="text-white text-opacity-90 mb-6 max-w-md mx-auto">
                    KYC verification and vehicle management are available exclusively on our mobile app for enhanced security and better document scanning.
                  </p>
                  <div className="flex flex-col sm:flex-row gap-3 justify-center">
                    <button className="flex items-center justify-center gap-2 bg-white text-blue-600 px-6 py-3 rounded-lg font-medium hover:bg-gray-100 transition-colors">
                      <ExternalLink size={20} />
                      Download App
                    </button>
                  </div>
                  <div className="mt-6 p-4 bg-white bg-opacity-10 rounded-lg">
                    <p className="text-white text-sm mb-2">Current KYC Status:</p>
                    <span className={`inline-block px-4 py-2 rounded-full text-sm font-medium ${kycStatus === 'VERIFIED' ? 'bg-green-500' : kycStatus === 'PENDING' ? 'bg-yellow-500' : 'bg-red-500'}`}>
                      {kycStatus}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'posts' && (
            <div>
              <h3 className="text-2xl font-bold text-white mb-6 flex items-center gap-2">
                <FileText className="text-orange-500" />
                My Posts
              </h3>
              {loading ? (
                <div className="text-center py-12">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500 mx-auto mb-4"></div>
                  <p className="text-gray-400">Loading posts...</p>
                </div>
              ) : userPosts.length === 0 ? (
                <div className="text-center py-12">
                  <FileText size={48} className="mx-auto text-gray-600 mb-4" />
                  <p className="text-gray-400">No posts yet</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {userPosts.map(post => (
                    <div key={post.id} className="bg-gray-700 rounded-lg p-6">
                      <div className="flex justify-between items-start mb-4">
                        <h4 className="text-xl font-semibold text-white">{post.title}</h4>
                        <span className="text-sm text-gray-400">{new Date(post.createdAt).toLocaleDateString()}</span>
                      </div>
                      <p className="text-gray-300 mb-4">{post.content}</p>
                      <div className="flex items-center gap-4 text-sm text-gray-400">
                        <span className="flex items-center gap-1">
                          <MessageSquare size={16} />
                          {post.comments || 0} comments
                        </span>
                        <span className="flex items-center gap-1">
                          <Star size={16} />
                          {post.likes || 0} likes
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'ratings' && (
            <div>
              <h3 className="text-2xl font-bold text-white mb-6 flex items-center gap-2">
                <Star className="text-orange-500" />
                My Ratings
              </h3>
              {loading ? (
                <div className="text-center py-12">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500 mx-auto mb-4"></div>
                  <p className="text-gray-400">Loading ratings...</p>
                </div>
              ) : userRatings.length === 0 ? (
                <div className="text-center py-12">
                  <Star size={48} className="mx-auto text-gray-600 mb-4" />
                  <p className="text-gray-400">No ratings yet</p>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="bg-gray-700 rounded-lg p-6 mb-6">
                    <div className="flex items-center justify-between">
                      <div>
                        <h4 className="text-3xl font-bold text-white">
                          {userRatings.reduce((acc, r) => acc + r.rating, 0) / userRatings.length || 0}
                        </h4>
                        <p className="text-gray-400">Average Rating</p>
                      </div>
                      <div className="flex items-center gap-1">
                        {[1, 2, 3, 4, 5].map(star => (
                          <Star
                            key={star}
                            size={24}
                            className={star <= (userRatings.reduce((acc, r) => acc + r.rating, 0) / userRatings.length || 0) ? 'text-yellow-400 fill-yellow-400' : 'text-gray-600'}
                          />
                        ))}
                      </div>
                    </div>
                  </div>
                  {userRatings.map(rating => (
                    <div key={rating.id} className="bg-gray-700 rounded-lg p-6">
                      <div className="flex justify-between items-start mb-4">
                        <div>
                          <h4 className="text-lg font-semibold text-white">{rating.fromUser}</h4>
                          <p className="text-sm text-gray-400">{new Date(rating.createdAt).toLocaleDateString()}</p>
                        </div>
                        <div className="flex items-center gap-1">
                          {[1, 2, 3, 4, 5].map(star => (
                            <Star
                              key={star}
                              size={16}
                              className={star <= rating.rating ? 'text-yellow-400 fill-yellow-400' : 'text-gray-600'}
                            />
                          ))}
                        </div>
                      </div>
                      <p className="text-gray-300">{rating.comment}</p>
                      <p className="text-sm text-gray-400 mt-2">Ride: {rating.rideId}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'rides' && (
            <div>
              <h3 className="text-2xl font-bold text-white mb-6 flex items-center gap-2">
                <Bike className="text-orange-500" />
                My Rides
              </h3>
              {loading ? (
                <div className="text-center py-12">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500 mx-auto mb-4"></div>
                  <p className="text-gray-400">Loading rides...</p>
                </div>
              ) : userRides.length === 0 ? (
                <div className="text-center py-12">
                  <Bike size={48} className="mx-auto text-gray-600 mb-4" />
                  <p className="text-gray-400">No rides yet</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {userRides.map(ride => (
                    <div key={ride.id} className="bg-gray-700 rounded-lg p-6">
                      <div className="flex justify-between items-start mb-4">
                        <div>
                          <h4 className="text-lg font-semibold text-white">
                            {ride.from} → {ride.to}
                          </h4>
                          <p className="text-sm text-gray-400">
                            {new Date(ride.date).toLocaleDateString()} at {new Date(ride.date).toLocaleTimeString()}
                          </p>
                        </div>
                        <span className={`px-3 py-1 rounded-full text-xs font-medium ${
                          ride.status === 'completed' ? 'bg-green-900 text-green-300' :
                          ride.status === 'cancelled' ? 'bg-red-900 text-red-300' :
                          'bg-yellow-900 text-yellow-300'
                        }`}>
                          {ride.status}
                        </span>
                      </div>
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                        <div className="flex items-center gap-2 text-gray-300">
                          <Clock size={16} />
                          {ride.duration}
                        </div>
                        <div className="flex items-center gap-2 text-gray-300">
                          <MapPin size={16} />
                          {ride.distance} km
                        </div>
                        <div className="flex items-center gap-2 text-gray-300">
                          <DollarSign size={16} />
                          ₹{ride.amount}
                        </div>
                        <div className="flex items-center gap-2 text-gray-300">
                          <User size={16} />
                          {ride.driverName}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'earnings' && (
            <div>
              <h3 className="text-2xl font-bold text-white mb-6 flex items-center gap-2">
                <DollarSign className="text-orange-500" />
                My Earnings
              </h3>
              {loading ? (
                <div className="text-center py-12">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500 mx-auto mb-4"></div>
                  <p className="text-gray-400">Loading earnings...</p>
                </div>
              ) : userEarnings.length === 0 ? (
                <div className="text-center py-12">
                  <DollarSign size={48} className="mx-auto text-gray-600 mb-4" />
                  <p className="text-gray-400">No earnings yet</p>
                </div>
              ) : (
                <div>
                  <div className="bg-gray-700 rounded-lg p-6 mb-6">
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                      <div className="text-center">
                        <h4 className="text-3xl font-bold text-green-400">
                          ₹{userEarnings.reduce((acc, e) => acc + e.amount, 0).toLocaleString()}
                        </h4>
                        <p className="text-gray-400">Total Earnings</p>
                      </div>
                      <div className="text-center">
                        <h4 className="text-3xl font-bold text-blue-400">
                          ₹{userEarnings.filter(e => new Date(e.date).getMonth() === new Date().getMonth()).reduce((acc, e) => acc + e.amount, 0).toLocaleString()}
                        </h4>
                        <p className="text-gray-400">This Month</p>
                      </div>
                      <div className="text-center">
                        <h4 className="text-3xl font-bold text-orange-400">
                          {userEarnings.length}
                        </h4>
                        <p className="text-gray-400">Total Trips</p>
                      </div>
                    </div>
                  </div>
                  <div className="space-y-4">
                    {userEarnings.map(earning => (
                      <div key={earning.id} className="bg-gray-700 rounded-lg p-6">
                        <div className="flex justify-between items-start">
                          <div>
                            <h4 className="text-lg font-semibold text-white">
                              {earning.from} → {earning.to}
                            </h4>
                            <p className="text-sm text-gray-400">
                              {new Date(earning.date).toLocaleDateString()} at {new Date(earning.date).toLocaleTimeString()}
                            </p>
                          </div>
                          <div className="text-right">
                            <p className="text-2xl font-bold text-green-400">₹{earning.amount}</p>
                            <p className="text-sm text-gray-400">{earning.type}</p>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {activeTab === 'complaints' && (
            <div>
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-2xl font-bold text-white flex items-center gap-2">
                  <AlertCircle className="text-orange-500" />
                  Support & Complaints
                </h3>
                <button onClick={() => setShowComplaintForm(!showComplaintForm)} className="flex items-center gap-2 bg-gradient-to-r from-orange-500 to-orange-600 hover:from-orange-600 hover:to-orange-700 text-white px-4 py-2 rounded-lg font-medium transition-colors">
                  <MessageSquare size={18} />
                  New Complaint
                </button>
              </div>

              {showComplaintForm && (
                <form onSubmit={handleSubmitComplaint} className="mb-6 p-6 bg-gray-700 rounded-lg">
                  <h4 className="text-white font-medium mb-4">File a Complaint</h4>
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">Title</label>
                      <input type="text" value={complaintForm.title} onChange={(e) => setComplaintForm({...complaintForm, title: e.target.value})} className="w-full bg-gray-600 text-white px-4 py-3 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500" required />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-2">Description</label>
                      <textarea value={complaintForm.description} onChange={(e) => setComplaintForm({...complaintForm, description: e.target.value})} className="w-full bg-gray-600 text-white px-4 py-3 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 h-32" required />
                    </div>
                    <div className="flex gap-3">
                      <button type="submit" className="flex items-center gap-2 bg-green-500 hover:bg-green-600 text-white px-6 py-3 rounded-lg font-medium transition-colors">
                        <Send size={18} />
                        Submit Complaint
                      </button>
                      <button type="button" onClick={() => setShowComplaintForm(false)} className="bg-gray-600 hover:bg-gray-500 text-white px-6 py-3 rounded-lg font-medium transition-colors">
                        Cancel
                      </button>
                    </div>
                  </div>
                </form>
              )}

              {userComplaints.length === 0 ? (
                <div className="text-center py-12">
                  <AlertCircle size={48} className="mx-auto text-gray-600 mb-4" />
                  <p className="text-gray-400">No complaints filed</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {userComplaints.map(complaint => (
                    <div key={complaint.id} className="p-4 bg-gray-700 rounded-lg">
                      <div className="flex justify-between items-start mb-2">
                        <div className="flex-1">
                          <h4 className="text-white font-medium text-lg">{complaint.title}</h4>
                          <p className="text-gray-300 text-sm mt-1">{complaint.description}</p>
                          <p className="text-gray-400 text-xs mt-2">{complaint.date}</p>
                        </div>
                        <span className={`px-3 py-1 rounded-full text-xs font-medium ${complaint.status === 'Resolved' ? 'bg-green-900 text-green-300' : complaint.status === 'Pending' ? 'bg-yellow-900 text-yellow-300' : 'bg-red-900 text-red-300'}`}>
                          {complaint.status}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default MyProfile;
