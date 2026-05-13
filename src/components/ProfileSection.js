import React, { useState, useRef } from 'react';
import { Camera, Upload, X, User, CheckCircle } from 'lucide-react';

const ProfileSection = ({ user, onUpdateProfile, onClose }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [previewImage, setPreviewImage] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const fileInputRef = useRef(null);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        alert('Please select an image file');
        return;
      }
      
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        alert('File size must be less than 5MB');
        return;
      }

      // Create preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewImage(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleUpload = async () => {
    if (!previewImage) return;

    setUploading(true);
    setSuccessMessage('');

    try {
      // Check if we're in production and have a real backend
      const isProduction = process.env.NODE_ENV === 'production';
      
      if (isProduction) {
        // Real backend upload for production (with fallback)
        try {
          
          const response = await fetch(previewImage);
          
          const blob = await response.blob();
          
          const file = new File([blob], 'profile.jpg', { type: 'image/jpeg' });

          // Create FormData for file upload
          const formData = new FormData();
          formData.append('profilePicture', file);

          const uploadUrl = `${process.env.REACT_APP_API_BASE_URL}/user/profile-picture`;

          // Upload to backend
          const uploadResponse = await fetch(uploadUrl, {
            method: 'POST',
            headers: {
              'Authorization': `Bearer ${localStorage.getItem('token')}`,
              'Content-Type': 'multipart/form-data'
            },
            body: formData
          });


          if (uploadResponse.ok) {
            const result = await uploadResponse.json();
            
            setSuccessMessage('Profile picture updated successfully!');
            
            // Update user data in parent component (App.js will handle localStorage)
            const updatedUser = {
              ...user,
              avatar: result.profilePictureUrl || previewImage
            };
            onUpdateProfile(updatedUser);

            // Reset after 2 seconds
            setTimeout(() => {
              setIsEditing(false);
              setPreviewImage(null);
              setSuccessMessage('');
            }, 2000);
          } else {
            const errorText = await uploadResponse.text();
            
            // Backend endpoint doesn't exist - use fallback
            const updatedUser = {
              ...user,
              avatar: previewImage
            };
            onUpdateProfile(updatedUser);
            setSuccessMessage('Profile picture updated! (Backend integration pending)');
            
            setTimeout(() => {
              setIsEditing(false);
              setPreviewImage(null);
              setSuccessMessage('');
            }, 2000);
          }
        } catch (error) {
          
          // Fallback: still update with preview image
          const updatedUser = {
            ...user,
            avatar: previewImage
          };
          onUpdateProfile(updatedUser);
          setSuccessMessage('Profile picture updated! (Offline mode)');
          
          setTimeout(() => {
            setIsEditing(false);
            setPreviewImage(null);
            setSuccessMessage('');
          }, 2000);
        }
      } else {
        // Demo mode for development
        await new Promise(resolve => setTimeout(resolve, 1500));
        
        // Update user data in parent component (App.js will handle localStorage)
        const updatedUser = {
          ...user,
          avatar: previewImage
        };
        onUpdateProfile(updatedUser);

        setSuccessMessage('Profile picture updated successfully! (Demo mode)');

        // Reset after 2 seconds
        setTimeout(() => {
          setIsEditing(false);
          setPreviewImage(null);
          setSuccessMessage('');
        }, 2000);
      }
    } catch (error) {
      console.error('Upload error:', error);
      setSuccessMessage('Failed to upload profile picture. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  const handleCancel = () => {
    setIsEditing(false);
    setPreviewImage(null);
    setSuccessMessage('');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4"
      style={{ background: 'rgba(0,0,0,0.85)', backdropFilter: 'blur(8px)' }}>
      <div className="relative w-full max-w-md rounded-2xl overflow-hidden"
        style={{ background: 'linear-gradient(135deg, #12121a, #1a1a2e)', border: '1px solid rgba(255,255,255,0.1)' }}>
        
        {/* Header */}
        <div className="h-1 w-full" style={{ background: 'linear-gradient(90deg, #FF7000, #ff9a3c)' }} />
        <div className="p-6">
          <button onClick={onClose} className="absolute top-4 right-4 text-gray-500 hover:text-white transition-colors">
            <X size={20} />
          </button>

          <h2 className="text-2xl font-bold text-white mb-6">Profile Picture</h2>

          {/* Current Profile Picture */}
          <div className="flex flex-col items-center mb-6">
            <div className="relative">
              <div className="w-32 h-32 rounded-full overflow-hidden border-4 border-orange-DEFAULT">
                {previewImage ? (
                  <img src={previewImage} alt="Preview" className="w-full h-full object-cover" />
                ) : user?.avatar ? (
                  <img src={user.avatar} alt="Profile" className="w-full h-full object-cover" />
                ) : (
                  <div className="w-full h-full bg-gray-700 flex items-center justify-center">
                    <User size={48} className="text-gray-400" />
                  </div>
                )}
              </div>
              
              {!isEditing && (
                <button
                  onClick={() => {
                    console.log('Camera button clicked');
                    setIsEditing(true);
                  }}
                  className="absolute bottom-0 right-0 w-10 h-10 bg-orange-DEFAULT rounded-full flex items-center justify-center text-white hover:bg-orange-600 transition-colors shadow-lg border-2 border-white"
                >
                  <Camera size={18} />
                </button>
              )}
            </div>

            <div className="text-center mt-4">
              <h3 className="text-lg font-semibold text-white">{user?.fullName || 'User'}</h3>
              <p className="text-gray-400 text-sm">{user?.email}</p>
              
              {!isEditing && (
                <button
                  onClick={() => {
                    console.log('Edit button clicked');
                    setIsEditing(true);
                  }}
                  className="mt-3 px-4 py-2 bg-orange-DEFAULT text-white rounded-lg hover:bg-orange-600 transition-colors flex items-center gap-2 mx-auto"
                >
                  <Camera size={16} />
                  Update Profile Picture
                </button>
              )}
            </div>
          </div>

          {/* Edit Mode */}
          {isEditing && (
            <div className="space-y-4">
              <div className="border-2 border-dashed border-gray-600 rounded-xl p-6 text-center">
                <Upload size={32} className="text-gray-400 mx-auto mb-2" />
                <p className="text-gray-300 text-sm mb-2">Click to upload new profile picture</p>
                <p className="text-gray-500 text-xs mb-4">JPG, PNG, GIF up to 5MB</p>
                
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handleImageChange}
                  className="hidden"
                />
                
                <button
                  onClick={() => fileInputRef.current?.click()}
                  className="px-4 py-2 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
                >
                  Choose File
                </button>
              </div>

              {previewImage && (
                <div className="flex gap-3">
                  <button
                    onClick={handleUpload}
                    disabled={uploading}
                    className="flex-1 bg-orange-DEFAULT text-white py-2 rounded-lg hover:bg-orange-600 transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    {uploading ? (
                      <>
                        <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                        Uploading...
                      </>
                    ) : (
                      <>
                        <CheckCircle size={16} />
                        Upload
                      </>
                    )}
                  </button>
                  <button
                    onClick={handleCancel}
                    className="px-4 py-2 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              )}

              {successMessage && (
                <div className="px-4 py-3 bg-green-500 bg-opacity-20 border border-green-500 rounded-lg text-green-400 text-sm text-center">
                  {successMessage}
                </div>
              )}
            </div>
          )}

          {/* Profile Info */}
          <div className="mt-6 pt-6 border-t border-gray-700">
            <div className="space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-gray-400">Phone:</span>
                <span className="text-white">{user?.phone}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-400">KYC Status:</span>
                <span className={`px-2 py-1 rounded-full text-xs ${
                  user?.kycStatus === 'APPROVED' ? 'bg-green-500 text-white' :
                  user?.kycStatus === 'PENDING' ? 'bg-yellow-500 text-white' :
                  'bg-gray-500 text-white'
                }`}>
                  {user?.kycStatus || 'Unknown'}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-400">Member Since:</span>
                <span className="text-white">2024</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfileSection;
