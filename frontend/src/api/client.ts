import type { ErrorApi } from './types';

/**
 * Cliente HTTP mínimo sobre fetch.
 * - Agrega la cabecera Authorization con el token guardado.
 * - Convierte las respuestas de error del backend en una ApiError con código y mensaje en español.
 * - Si el backend responde 401 (sesión vencida), avisa para cerrar la sesión.
 */

const CLAVE_TOKEN = 'autos-colombia.token';

export class ApiError extends Error {
  readonly status: number;
  readonly codigo: string;
  readonly errores?: Record<string, string>;

  constructor(datos: ErrorApi) {
    super(datos.mensaje);
    this.name = 'ApiError';
    this.status = datos.status;
    this.codigo = datos.codigo;
    this.errores = datos.errores;
  }
}

export const almacenToken = {
  leer: (): string | null => sessionStorage.getItem(CLAVE_TOKEN),
  guardar: (token: string) => sessionStorage.setItem(CLAVE_TOKEN, token),
  borrar: () => sessionStorage.removeItem(CLAVE_TOKEN),
};

type OyenteNoAutorizado = () => void;
let alNoAutorizado: OyenteNoAutorizado | null = null;

/** El AuthProvider se registra aquí para cerrar sesión cuando el token deja de ser válido. */
export function registrarOyenteNoAutorizado(oyente: OyenteNoAutorizado | null) {
  alNoAutorizado = oyente;
}

export interface OpcionesPeticion {
  method?: 'GET' | 'POST' | 'PUT';
  body?: unknown;
  query?: Record<string, string | undefined | null>;
}

export async function peticion<T>(ruta: string, opciones: OpcionesPeticion = {}): Promise<T> {
  const url = construirUrl(ruta, opciones.query);
  const cabeceras: Record<string, string> = { Accept: 'application/json' };
  const token = almacenToken.leer();
  if (token) cabeceras.Authorization = `Bearer ${token}`;
  if (opciones.body !== undefined) cabeceras['Content-Type'] = 'application/json';

  let respuesta: Response;
  try {
    respuesta = await fetch(url, {
      method: opciones.method ?? 'GET',
      headers: cabeceras,
      body: opciones.body !== undefined ? JSON.stringify(opciones.body) : undefined,
    });
  } catch {
    throw new ApiError({
      status: 0,
      codigo: 'SIN_CONEXION',
      mensaje: 'No fue posible conectar con el servidor. Verifique que el backend esté en ejecución.',
    });
  }

  if (respuesta.status === 204) return undefined as T;

  const texto = await respuesta.text();
  const datos = texto ? intentarJson(texto) : null;

  if (!respuesta.ok) {
    const error = esErrorApi(datos)
      ? datos
      : { status: respuesta.status, codigo: 'ERROR_HTTP', mensaje: mensajePorEstado(respuesta.status) };
    if (respuesta.status === 401 && token && alNoAutorizado) alNoAutorizado();
    throw new ApiError({ ...error, status: respuesta.status });
  }
  return datos as T;
}

function construirUrl(ruta: string, query?: OpcionesPeticion['query']): string {
  if (!query) return ruta;
  const params = new URLSearchParams();
  Object.entries(query).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== null && valor !== '') params.append(clave, valor);
  });
  const qs = params.toString();
  return qs ? `${ruta}?${qs}` : ruta;
}

function intentarJson(texto: string): unknown {
  try {
    return JSON.parse(texto);
  } catch {
    return null;
  }
}

function esErrorApi(valor: unknown): valor is ErrorApi {
  return typeof valor === 'object' && valor !== null && 'codigo' in valor && 'mensaje' in valor;
}

function mensajePorEstado(status: number): string {
  if (status === 401) return 'Debe iniciar sesión.';
  if (status === 403) return 'No tiene permisos para esta operación.';
  if (status === 404) return 'Recurso no encontrado.';
  if (status >= 500) return 'El servidor tuvo un problema. Intente de nuevo.';
  return 'No se pudo completar la operación.';
}
