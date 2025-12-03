import { useAuth } from '../context/AuthContext';
import '../styles/Navigation.css';

const Navigation = ({ currentView, onNavigate }) => {
  const { user, logout } = useAuth();

  return (
    <nav className="navigation">
      <div className="nav-brand">
        <h1>Mi Aplicación</h1>
      </div>
      
      <div className="nav-links">
        <button
          className={currentView === 'users' ? 'active' : ''}
          onClick={() => onNavigate('users')}
        >
          Usuarios
        </button>
        <button
          className={currentView === 'news' ? 'active' : ''}
          onClick={() => onNavigate('news')}
        >
          Noticias
        </button>
      </div>

      <div className="nav-user">
        <span className="user-name">Hola, {user?.name || user?.username}</span>
        <button onClick={logout} className="btn btn-logout">
          Cerrar Sesión
        </button>
      </div>
    </nav>
  );
};

export default Navigation;
