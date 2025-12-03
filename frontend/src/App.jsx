import { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './components/Login';
import Signup from './components/Signup';
import Navigation from './components/Navigation';
import UserList from './components/UserList';
import UserForm from './components/UserForm';
import News from './components/News';
import { userService } from './services/api';
import './styles/App.css';

function AppContent() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const [view, setView] = useState('login'); // 'login', 'signup', 'users', 'news'
  const [currentView, setCurrentView] = useState('users');
  const [users, setUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (isAuthenticated && currentView === 'users') {
      loadUsers();
    }
  }, [isAuthenticated, currentView]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await userService.getAllUsers();
      setUsers(data);
    } catch (err) {
      setError('Error al cargar los usuarios: ' + err.message);
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveUser = async (userData) => {
    try {
      setLoading(true);
      setError(null);

      if (selectedUser) {
        await userService.updateUser(selectedUser.id, userData);
      } else {
        await userService.createUser(userData);
      }

      await loadUsers();
      setShowForm(false);
      setSelectedUser(null);
    } catch (err) {
      setError(err.response?.data?.error || 'Error al guardar el usuario');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleEditUser = (user) => {
    setSelectedUser(user);
    setShowForm(true);
  };

  const handleDeleteUser = async (id) => {
    if (window.confirm('¿Estás seguro de que deseas eliminar este usuario?')) {
      try {
        setLoading(true);
        setError(null);
        await userService.deleteUser(id);
        await loadUsers();
      } catch (err) {
        setError('Error al eliminar el usuario: ' + err.message);
        console.error('Error:', err);
      } finally {
        setLoading(false);
      }
    }
  };

  const handleNewUser = () => {
    setSelectedUser(null);
    setShowForm(true);
  };

  const handleCancelForm = () => {
    setShowForm(false);
    setSelectedUser(null);
  };

  const handleNavigate = (newView) => {
    setCurrentView(newView);
    setShowForm(false);
    setSelectedUser(null);
  };

  if (authLoading) {
    return <div className="loading-screen">Cargando...</div>;
  }

  if (!isAuthenticated) {
    if (view === 'signup') {
      return (
        <Signup
          onSwitchToLogin={() => setView('login')}
          onSuccess={() => {
            setView('login');
            alert('¡Registro exitoso! Ahora puedes iniciar sesión.');
          }}
        />
      );
    }
    return <Login onSwitchToSignup={() => setView('signup')} />;
  }

  return (
    <div className="App">
      <Navigation currentView={currentView} onNavigate={handleNavigate} />

      <main className="App-main">
        {error && (
          <div className="error-message">
            {error}
            <button onClick={() => setError(null)}>✕</button>
          </div>
        )}

        {loading && <div className="loading">Cargando...</div>}

        {currentView === 'users' && (
          <>
            {!showForm ? (
              <>
                <button className="btn btn-primary btn-new" onClick={handleNewUser}>
                  + Nuevo Usuario
                </button>
                <UserList
                  users={users}
                  onEdit={handleEditUser}
                  onDelete={handleDeleteUser}
                />
              </>
            ) : (
              <UserForm
                user={selectedUser}
                onSave={handleSaveUser}
                onCancel={handleCancelForm}
              />
            )}
          </>
        )}

        {currentView === 'news' && <News />}
      </main>
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
