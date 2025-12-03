import { useState, useEffect } from 'react';
import { telegramService } from '../services/api';
import '../styles/News.css';

const News = () => {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [manualText, setManualText] = useState('');

  useEffect(() => {
    loadMessages();
  }, []);

  const loadMessages = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await telegramService.getMessages();
      setMessages(data);
    } catch (err) {
      setError('Error al cargar las noticias');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleFetchMessages = async () => {
    try {
      setLoading(true);
      setError(null);
      await telegramService.fetchMessages();
      await loadMessages();
    } catch (err) {
      setError('Error al actualizar las noticias');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateManualMessage = async (e) => {
    e.preventDefault();
    if (!manualText.trim()) return;

    try {
      setLoading(true);
      setError(null);
      await telegramService.createManualMessage(manualText);
      setManualText('');
      await loadMessages();
    } catch (err) {
      setError('Error al crear la noticia');
      console.error('Error:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleString('es-ES', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="news-container">
      <div className="news-header">
        <h2>Noticias - AFA Lluís Braille</h2>
        <button 
          onClick={handleFetchMessages} 
          className="btn btn-primary"
          disabled={loading}
        >
          {loading ? 'Actualizando...' : 'Actualizar Noticias'}
        </button>
      </div>

      {error && (
        <div className="error-message">
          {error}
          <button onClick={() => setError(null)}>✕</button>
        </div>
      )}

      <div className="manual-message-form">
        <h3>Crear Noticia Manual (Para Pruebas)</h3>
        <form onSubmit={handleCreateManualMessage}>
          <textarea
            value={manualText}
            onChange={(e) => setManualText(e.target.value)}
            placeholder="Escribe una noticia..."
            rows="3"
          />
          <button 
            type="submit" 
            className="btn btn-secondary"
            disabled={loading || !manualText.trim()}
          >
            Crear Noticia
          </button>
        </form>
      </div>

      {loading && messages.length === 0 ? (
        <div className="loading">Cargando noticias...</div>
      ) : messages.length === 0 ? (
        <div className="no-messages">
          <p>No hay noticias disponibles</p>
          <p className="note">
            Nota: Para ver noticias del canal de Telegram, necesitas configurar
            un bot de Telegram y añadir el token en application.properties
          </p>
        </div>
      ) : (
        <div className="messages-list">
          {messages.map((message) => (
            <div key={message.id} className="message-card">
              <div className="message-date">
                {formatDate(message.messageDate)}
              </div>
              <div className="message-text">{message.text}</div>
              <div className="message-footer">
                Canal: {message.channelUsername}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default News;
