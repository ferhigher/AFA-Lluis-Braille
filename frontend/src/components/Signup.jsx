import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import '../styles/Auth.css';

const Signup = ({ onSwitchToLogin, onSuccess }) => {
  const [formData, setFormData] = useState({
    name: '',
    username: '',
    email: '',
    password: '',
    phone: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { signup } = useAuth();

  const handleChange = (e) => {
    const { name, value } = e.target;
    console.log(`📝 Campo '${name}' cambiado:`, value);
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    console.log('========================================');
    console.log('📝 SIGNUP FORM - SUBMIT');
    console.log('========================================');
    console.log('📋 Datos del formulario:', {
      name: formData.name,
      username: formData.username,
      email: formData.email,
      phone: formData.phone,
      passwordLength: formData.password?.length,
      passwordEmpty: !formData.password || formData.password.trim() === ''
    });

    // Validaciones en frontend
    const validationErrors = [];
    if (!formData.name || formData.name.trim() === '') {
      validationErrors.push('El nombre es obligatorio');
    }
    if (!formData.username || formData.username.trim() === '') {
      validationErrors.push('El username es obligatorio');
    }
    if (!formData.email || formData.email.trim() === '') {
      validationErrors.push('El email es obligatorio');
    }
    if (!formData.password || formData.password.trim() === '') {
      validationErrors.push('La contraseña es obligatoria');
    } else if (formData.password.length < 6) {
      validationErrors.push('La contraseña debe tener al menos 6 caracteres');
    }

    if (validationErrors.length > 0) {
      console.error('❌ Errores de validación en frontend:');
      validationErrors.forEach(err => console.error('  -', err));
      setError(validationErrors.join('. '));
      setLoading(false);
      return;
    }

    console.log('✅ Validación frontend exitosa');
    console.log('🚀 Enviando datos al backend...');

    try {
      await signup(formData);
      console.log('✅ Signup exitoso');
      console.log('🎉 Llamando a onSuccess()');
      onSuccess();
    } catch (err) {
      console.error('========================================');
      console.error('❌ ERROR EN SIGNUP');
      console.error('========================================');
      console.error('🔍 Error completo:', err);
      console.error('📊 Response:', err.response);
      console.error('📊 Response data:', err.response?.data);
      console.error('📊 Response status:', err.response?.status);
      console.error('📊 Response headers:', err.response?.headers);
      
      let errorMessage = 'Error al registrarse';
      
      if (err.response?.data) {
        if (typeof err.response.data === 'string') {
          errorMessage = err.response.data;
        } else if (err.response.data.error) {
          errorMessage = err.response.data.error;
        } else if (err.response.data.message) {
          errorMessage = err.response.data.message;
        } else {
          // Si es un objeto con errores de validación
          const errors = Object.values(err.response.data);
          if (errors.length > 0) {
            errorMessage = errors.join('. ');
          }
        }
      } else if (err.message) {
        errorMessage = err.message;
      }
      
      console.error('💬 Mensaje de error mostrado al usuario:', errorMessage);
      setError(errorMessage);
    } finally {
      setLoading(false);
      console.log('========================================');
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Crear Cuenta</h2>
        {error && <div className="error-message">{error}</div>}
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Nombre completo:</label>
            <input
              type="text"
              id="name"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="username">Usuario:</label>
            <input
              type="text"
              id="username"
              name="username"
              value={formData.username}
              onChange={handleChange}
              required
              minLength="4"
              maxLength="20"
              autoComplete="username"
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email:</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              autoComplete="email"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Contraseña:</label>
            <input
              type="password"
              id="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              minLength="6"
              autoComplete="new-password"
            />
          </div>

          <div className="form-group">
            <label htmlFor="phone">Teléfono (opcional):</label>
            <input
              type="tel"
              id="phone"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              autoComplete="tel"
            />
          </div>

          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Registrando...' : 'Registrarse'}
          </button>
        </form>

        <p className="auth-switch">
          ¿Ya tienes cuenta?{' '}
          <button onClick={onSwitchToLogin} className="link-button">
            Inicia sesión aquí
          </button>
        </p>
      </div>
    </div>
  );
};

export default Signup;
