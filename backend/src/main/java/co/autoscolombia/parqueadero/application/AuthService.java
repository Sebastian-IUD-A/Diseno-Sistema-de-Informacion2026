package co.autoscolombia.parqueadero.application;

import co.autoscolombia.parqueadero.application.dto.Dtos.SesionDto;
import co.autoscolombia.parqueadero.application.dto.Dtos.UsuarioDto;
import co.autoscolombia.parqueadero.domain.exception.AccesoDenegadoException;
import co.autoscolombia.parqueadero.domain.exception.CredencialesInvalidasException;
import co.autoscolombia.parqueadero.domain.model.Usuario;
import co.autoscolombia.parqueadero.infrastructure.persistence.UsuarioRepository;
import co.autoscolombia.parqueadero.security.TokenService;
import co.autoscolombia.parqueadero.security.TokenService.TokenEmitido;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Inicio de sesión: verifica correo + contraseña contra el hash BCrypt y emite un JWT. */
@Service
public class AuthService {

    /** Hash de relleno: se compara aunque el usuario no exista, para que el tiempo de respuesta no lo delate. */
    private static final String HASH_RELLENO = "$2a$10$1DM8vf/LzoKE5TZRusRy5.0ARnKXcCqKcPSXrkF.L9DlBzOKMm2rC";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public SesionDto login(String correo, String contrasena) {
        String correoNormalizado = correo == null ? "" : correo.strip().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correoNormalizado).orElse(null);
        String hash = usuario != null ? usuario.getPasswordHash() : HASH_RELLENO;
        boolean claveCorrecta = passwordEncoder.matches(contrasena == null ? "" : contrasena, hash);
        if (usuario == null || !claveCorrecta || !usuario.isActivo()) {
            throw new CredencialesInvalidasException();
        }
        TokenEmitido token = tokenService.emitir(usuario);
        return new SesionDto(token.valor(), "Bearer", token.expiraEn(), Mapeador.usuario(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioDto perfil(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .filter(Usuario::isActivo)
                .map(Mapeador::usuario)
                .orElseThrow(() -> new AccesoDenegadoException("USUARIO_INACTIVO", "Su usuario no está activo."));
    }
}
