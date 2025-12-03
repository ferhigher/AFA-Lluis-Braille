const UserList = ({ users, onEdit, onDelete }) => {
  return (
    <div className="user-list">
      <h2>Lista de Usuarios</h2>
      {users.length === 0 ? (
        <p className="no-users">No hay usuarios registrados</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>Email</th>
              <th>Teléfono</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.id}</td>
                <td>{user.name}</td>
                <td>{user.email}</td>
                <td>{user.phone || '-'}</td>
                <td className="actions">
                  <button
                    className="btn btn-edit"
                    onClick={() => onEdit(user)}
                  >
                    Editar
                  </button>
                  <button
                    className="btn btn-delete"
                    onClick={() => onDelete(user.id)}
                  >
                    Eliminar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default UserList;
