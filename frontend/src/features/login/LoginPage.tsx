import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import { Alerta } from '../../components/Estados';

export function LoginPage() {
  const { usuario, login, avisoSesion, limpiarAviso } = useAuth();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  if (usuario) return <Navigate to="/" replace />;

  const destino = (ubicacion.state as { desde?: string } | null)?.desde ?? '/';

  async function enviar(e: FormEvent) {
    e.preventDefault();
    setError(null);
    limpiarAviso();
    if (!correo.trim() || !contrasena) {
      setError('Ingrese su correo y contraseña.');
      return;
    }
    setEnviando(true);
    try {
      await login(correo.trim(), contrasena);
      navegar(destino, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No fue posible iniciar sesión.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="login">
      <section className="login__panel-marca" aria-hidden="true">
        <div className="login__logo">P</div>
        <h2>Autos Colombia</h2>
        <p>Control de entradas, salidas y ocupación del parqueadero por mensualidad.</p>
        <div className="login__franja" />
      </section>

      <section className="login__panel-form">
        <form className="tarjeta login__form" onSubmit={enviar} noValidate>
          <h1>Iniciar sesión</h1>
          <p className="texto-suave">Use su cuenta de administrador u operador.</p>

          {avisoSesion && <Alerta tipo="aviso">{avisoSesion}</Alerta>}
          {error && <Alerta tipo="error">{error}</Alerta>}

          <div className="campo">
            <label htmlFor="correo">Correo electrónico</label>
            <input
              id="correo"
              type="email"
              autoComplete="username"
              value={correo}
              onChange={(e) => setCorreo(e.target.value)}
              placeholder="usuario@autoscolombia.co"
              required
            />
          </div>
          <div className="campo">
            <label htmlFor="contrasena">Contraseña</label>
            <input
              id="contrasena"
              type="password"
              autoComplete="current-password"
              value={contrasena}
              onChange={(e) => setContrasena(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="boton boton--primario boton--bloque" disabled={enviando}>
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </button>

          <details className="login__demo">
            <summary>Credenciales de demostración</summary>
            <ul>
              <li>
                <strong>Operador:</strong> operador@autoscolombia.co · Operador123*
              </li>
              <li>
                <strong>Administrador:</strong> admin@autoscolombia.co · Admin123*
              </li>
            </ul>
          </details>
        </form>
      </section>
    </div>
  );
}
