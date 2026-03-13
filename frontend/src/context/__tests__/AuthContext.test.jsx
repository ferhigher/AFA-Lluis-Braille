import { render, screen, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AuthProvider, useAuth } from '../AuthContext'

const RoleDisplay = () => {
  const { user, isAdmin, isAuthenticated } = useAuth()
  return (
    <div>
      <span data-testid="role">{user?.role}</span>
      <span data-testid="isAdmin">{isAdmin ? 'true' : 'false'}</span>
      <span data-testid="isAuthenticated">{isAuthenticated ? 'true' : 'false'}</span>
    </div>
  )
}

vi.mock('../../services/api', () => ({
  authService: {
    login: vi.fn(),
    signup: vi.fn(),
  }
}))

describe('AuthContext — CA-2.1.6', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('test_CA_2_1_6_sinLogin_isAdmin_esFalse', () => {
    render(
      <AuthProvider>
        <RoleDisplay />
      </AuthProvider>
    )

    expect(screen.getByTestId('isAdmin').textContent).toBe('false')
    expect(screen.getByTestId('isAuthenticated').textContent).toBe('false')
  })

  it('test_CA_2_1_6_usuarioADMIN_enLocalStorage_isAdmin_esTrue', () => {
    const adminResponse = {
      token: 'test-token',
      id: 1,
      username: 'adminafa',
      email: 'admin@afa.org',
      name: 'Admin AFA',
      role: 'ADMIN'
    }
    localStorage.setItem('token', adminResponse.token)
    localStorage.setItem('user', JSON.stringify(adminResponse))

    render(
      <AuthProvider>
        <RoleDisplay />
      </AuthProvider>
    )

    expect(screen.getByTestId('role').textContent).toBe('ADMIN')
    expect(screen.getByTestId('isAdmin').textContent).toBe('true')
    expect(screen.getByTestId('isAuthenticated').textContent).toBe('true')
  })

  it('test_CA_2_1_6_usuarioUSER_enLocalStorage_isAdmin_esFalse', () => {
    const userResponse = {
      token: 'test-token',
      id: 2,
      username: 'user1',
      email: 'user@afa.org',
      name: 'Usuario Regular',
      role: 'USER'
    }
    localStorage.setItem('token', userResponse.token)
    localStorage.setItem('user', JSON.stringify(userResponse))

    render(
      <AuthProvider>
        <RoleDisplay />
      </AuthProvider>
    )

    expect(screen.getByTestId('role').textContent).toBe('USER')
    expect(screen.getByTestId('isAdmin').textContent).toBe('false')
  })

  it('test_CA_2_1_6_rolePersisteEnLocalStorage_trasSalvarLogin', async () => {
    const { authService } = await import('../../services/api')
    authService.login.mockResolvedValue({
      token: 'jwt-token',
      id: 1,
      username: 'adminafa',
      email: 'admin@afa.org',
      name: 'Admin AFA',
      role: 'ADMIN'
    })

    let loginFn
    const CaptureLogin = () => {
      const { login } = useAuth()
      loginFn = login
      return null
    }

    render(
      <AuthProvider>
        <CaptureLogin />
        <RoleDisplay />
      </AuthProvider>
    )

    await act(async () => {
      await loginFn('adminafa', 'Password1')
    })

    const storedUser = JSON.parse(localStorage.getItem('user'))
    expect(storedUser.role).toBe('ADMIN')
    expect(screen.getByTestId('isAdmin').textContent).toBe('true')
  })
})
