import axios from 'axios';

const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor para añadir el token a todas las peticiones
api.interceptors.request.use(
  (config) => {
    console.log('🚀 REQUEST:', config.method.toUpperCase(), config.url);
    console.log('📦 Data:', config.data);
    
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('🔐 Token añadido (primeros 20 chars):', token.substring(0, 20) + '...');
    } else {
      console.log('⚠️ No hay token disponible');
    }
    return config;
  },
  (error) => {
    console.error('❌ Error en request interceptor:', error);
    return Promise.reject(error);
  }
);

// Interceptor para manejar errores de autenticación
api.interceptors.response.use(
  (response) => {
    console.log('✅ RESPONSE:', response.config.method.toUpperCase(), response.config.url);
    console.log('📊 Status:', response.status);
    console.log('📦 Data:', response.data);
    return response;
  },
  (error) => {
    console.error('========================================');
    console.error('❌ ERROR EN RESPONSE');
    console.error('========================================');
    
    if (error.response) {
      // El servidor respondió con un código de estado fuera del rango 2xx
      console.error('📍 URL:', error.config?.url);
      console.error('📍 Método:', error.config?.method?.toUpperCase());
      console.error('📊 Status:', error.response.status);
      console.error('📊 Status Text:', error.response.statusText);
      console.error('📦 Response Data:', error.response.data);
      console.error('🔍 Headers:', error.response.headers);
      
      if (error.response.status === 401) {
        console.warn('🚫 Error 401 - No autorizado');
        console.warn('🔄 Limpiando sesión y redirigiendo al login...');
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login';
      }
    } else if (error.request) {
      // La petición se hizo pero no hubo respuesta
      console.error('📡 No se recibió respuesta del servidor');
      console.error('🔍 Request:', error.request);
      console.error('💡 Posibles causas:');
      console.error('   - El servidor no está corriendo');
      console.error('   - Problemas de red');
      console.error('   - CORS no configurado correctamente');
    } else {
      // Algo pasó al configurar la petición
      console.error('⚙️ Error al configurar la petición');
      console.error('📝 Mensaje:', error.message);
    }
    
    console.error('🔧 Config completo:', error.config);
    console.error('========================================');
    
    return Promise.reject(error);
  }
);

// Servicio de autenticación
export const authService = {
  login: async (username, password) => {
    console.log('========================================');
    console.log('🔐 AUTH SERVICE - LOGIN');
    console.log('========================================');
    console.log('👤 Username:', username);
    console.log('🔑 Password length:', password?.length);
    
    try {
      const response = await api.post('/auth/login', { username, password });
      console.log('✅ Login exitoso');
      console.log('👤 Usuario:', response.data.username);
      console.log('🎫 Token recibido (primeros 20 chars):', response.data.token.substring(0, 20) + '...');
      return response.data;
    } catch (error) {
      console.error('❌ Error en login');
      throw error;
    }
  },

  signup: async (userData) => {
    console.log('========================================');
    console.log('📝 AUTH SERVICE - SIGNUP');
    console.log('========================================');
    console.log('📋 Datos a enviar:', {
      name: userData.name,
      username: userData.username,
      email: userData.email,
      phone: userData.phone,
      passwordLength: userData.password?.length
    });
    
    try {
      const response = await api.post('/auth/signup', userData);
      console.log('✅ Registro exitoso');
      console.log('📊 Response:', response.data);
      return response.data;
    } catch (error) {
      console.error('❌ Error en registro');
      if (error.response?.data) {
        console.error('📦 Detalles del error:', error.response.data);
      }
      throw error;
    }
  },
};

// Servicio para usuarios
export const userService = {
  // Obtener todos los usuarios
  getAllUsers: async () => {
    console.log('📋 USER SERVICE - Get All Users');
    const response = await api.get('/users');
    console.log('✅ Usuarios obtenidos:', response.data.length);
    return response.data;
  },

  // Obtener un usuario por ID
  getUserById: async (id) => {
    console.log('👤 USER SERVICE - Get User by ID:', id);
    const response = await api.get(`/users/${id}`);
    return response.data;
  },

  // Crear un nuevo usuario
  createUser: async (userData) => {
    console.log('➕ USER SERVICE - Create User');
    console.log('📋 Data:', userData);
    const response = await api.post('/users', userData);
    console.log('✅ Usuario creado:', response.data);
    return response.data;
  },

  // Actualizar un usuario
  updateUser: async (id, userData) => {
    console.log('✏️ USER SERVICE - Update User:', id);
    console.log('📋 Data:', userData);
    const response = await api.put(`/users/${id}`, userData);
    console.log('✅ Usuario actualizado:', response.data);
    return response.data;
  },

  // Eliminar un usuario
  deleteUser: async (id) => {
    console.log('🗑️ USER SERVICE - Delete User:', id);
    const response = await api.delete(`/users/${id}`);
    console.log('✅ Usuario eliminado');
    return response.data;
  },
};

// Servicio para Telegram
export const telegramService = {
  // Obtener mensajes del canal
  getMessages: async () => {
    console.log('📰 TELEGRAM SERVICE - Get Messages');
    const response = await api.get('/telegram/messages');
    console.log('✅ Mensajes obtenidos:', response.data.length);
    return response.data;
  },

  // Forzar actualización de mensajes
  fetchMessages: async () => {
    console.log('🔄 TELEGRAM SERVICE - Fetch Messages');
    const response = await api.post('/telegram/fetch');
    console.log('✅ Mensajes actualizados');
    return response.data;
  },

  // Crear mensaje manual (para pruebas)
  createManualMessage: async (text) => {
    console.log('✍️ TELEGRAM SERVICE - Create Manual Message');
    console.log('📝 Text:', text);
    const response = await api.post('/telegram/manual', { text });
    console.log('✅ Mensaje creado:', response.data);
    return response.data;
  },
};

export default api;
