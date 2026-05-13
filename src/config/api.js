// API Configuration for different environments
const getApiConfig = () => {
  // Use local server for development, remote for production
  const isDevelopment = process.env.NODE_ENV === 'development';
  const baseURL = process.env.REACT_APP_API_URL?.trim() || 
    (isDevelopment ? 'http://localhost:5000/api' : 'https://bike-cytc.onrender.com/api');
  const timeout = 20000; // allow more time for backend response

  return {
    baseURL,
    timeout,
  };
};

export default getApiConfig;