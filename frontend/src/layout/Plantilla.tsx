import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Icono } from '../components/Icono';
import { opcionesPara } from './navegacion';

/** Estructura común: barra lateral de navegación + encabezado + contenido de la pantalla. */
export function Plantilla() {
  const { usuario, logout } = useAuth();
  const [menuAbierto, setMenuAbierto] = useState(false);
  const ubicacion = useLocation();

  if (!usuario) return null;
  const opciones = opcionesPara(usuario.rol);
  const actual = opciones.find((o) => (o.ruta === '/' ? ubicacion.pathname === '/' : ubicacion.pathname.startsWith(o.ruta)));

  return (
    <div className={`app ${menuAbierto ? 'app--menu-abierto' : ''}`}>
      <a className="saltar-contenido" href="#contenido">
        Saltar al contenido
      </a>
      <aside className="barra-lateral" aria-label="Menú principal">
        <div className="marca">
          <span className="marca__logo" aria-hidden="true">
            P
          </span>
          <div>
            <span className="marca__nombre">Autos Colombia</span>
            <span className="marca__sub">Parqueadero · Mensualidades</span>
          </div>
        </div>
        <nav>
          <ul className="menu">
            {opciones.map((o) => (
              <li key={o.ruta}>
                <NavLink
                  to={o.ruta}
                  end={o.ruta === '/'}
                  className={({ isActive }) => `menu__enlace ${isActive ? 'menu__enlace--activo' : ''}`}
                  onClick={() => setMenuAbierto(false)}
                >
                  <Icono nombre={o.icono} />
                  <span>{o.etiqueta}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <div className="barra-lateral__pie">Iteración 1</div>
      </aside>
      <div className="velo" onClick={() => setMenuAbierto(false)} aria-hidden="true" />

      <div className="principal">
        <header className="cabecera">
          <button
            type="button"
            className="cabecera__menu"
            aria-label="Abrir menú"
            aria-expanded={menuAbierto}
            onClick={() => setMenuAbierto((v) => !v)}
          >
            <Icono nombre="menu" />
          </button>
          <span className="cabecera__seccion">{actual?.etiqueta ?? 'Autos Colombia'}</span>
          <div className="cabecera__usuario">
            <div className="cabecera__datos">
              <span className="cabecera__nombre">{usuario.nombre}</span>
              <span className="cabecera__rol">{usuario.rol === 'ADMINISTRADOR' ? 'Administrador' : 'Operador'}</span>
            </div>
            <button type="button" className="boton boton--fantasma" onClick={() => logout()}>
              <Icono nombre="salir" tamano={18} />
              <span>Cerrar sesión</span>
            </button>
          </div>
        </header>
        <main id="contenido" className="contenido" tabIndex={-1}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
