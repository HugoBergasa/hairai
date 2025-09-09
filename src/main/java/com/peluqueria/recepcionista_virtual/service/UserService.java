package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.User;
import com.peluqueria.recepcionista_virtual.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User save(User user) {
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public void updateLastAccess(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setUltimoAcceso(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * EXTENSION DEL USUARIO SERVICE ORIGINAL
     * Agrega validaciones de seguridad avanzadas manteniendo compatibilidad
     */
    @Service
    @Transactional
    public class UsuarioService extends UserService {

        private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

        @Autowired
        private TenantConfigService tenantConfigService;

        /**
         * CRITICO: Validar permisos especificos por operacion
         * MULTITENANT: Configuracion de permisos por tenant
         */
        public boolean tienePermisoOperacion(String usuarioId, String tenantId,
                                             String operacion, String recursoId) {
            try {
                User usuario = findById(usuarioId).orElse(null);
                if (usuario == null || !usuario.getTenant().getId().equals(tenantId)) {
                    logger.warn("Usuario {} no encontrado o no pertenece al tenant {}",
                            usuarioId, tenantId);
                    return false;
                }

                // ZERO HARDCODING: Obtener permisos desde configuracion
                String permisos = tenantConfigService.obtenerValor(tenantId,
                        "permisos_" + operacion, "ADMIN");

                boolean tienePermiso = permisos.contains(usuario.getRole());
                logger.debug("Validacion permiso operacion {}: usuario {} con role {} -> {}",
                        operacion, usuarioId, usuario.getRole(), tienePermiso);

                return tienePermiso;

            } catch (Exception e) {
                logger.error("Error validando permisos para usuario {}: {}", usuarioId, e.getMessage());
                return false;
            }
        }

        /**
         * CRITICO: Validar permisos especiales (cierres de emergencia)
         */
        public boolean tienePermisoEmergencia(String usuarioId, String tenantId) {
            try {
                String rolesPermitidos = tenantConfigService.obtenerValor(tenantId,
                        "roles_cierre_emergencia", "ADMIN");

                return findById(usuarioId)
                        .map(user -> {
                            boolean perteneceTenant = user.getTenant().getId().equals(tenantId);
                            boolean tieneRole = rolesPermitidos.contains(user.getRole());

                            logger.debug("Validacion permiso emergencia: usuario {} -> tenant:{}, role:{}",
                                    usuarioId, perteneceTenant, tieneRole);

                            return perteneceTenant && tieneRole;
                        })
                        .orElse(false);

            } catch (Exception e) {
                logger.error("Error validando permisos de emergencia para usuario {}: {}",
                        usuarioId, e.getMessage());
                return false;
            }
        }

        /**
         * CRITICO: Validar si es administrador del tenant
         */
        public boolean esAdministrador(String usuarioId, String tenantId) {
            try {
                return findById(usuarioId)
                        .map(user -> user.getTenant().getId().equals(tenantId) &&
                                "ADMIN".equals(user.getRole()))
                        .orElse(false);

            } catch (Exception e) {
                logger.error("Error validando administrador para usuario {}: {}",
                        usuarioId, e.getMessage());
                return false;
            }
        }

        /**
         * CRITICO: Validar contexto de sesion completo
         */
        public void validarContextoSesion(String usuarioId, String tenantId,
                                          String operacion, HttpServletRequest request) {
            try {
                // 1. Validar usuario-tenant
                User usuario = findById(usuarioId)
                        .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

                if (!usuario.getTenant().getId().equals(tenantId)) {
                    registrarIntentoAccesoIlegal(usuarioId, tenantId, operacion, request);
                    throw new SecurityException("Usuario no pertenece al tenant");
                }

                // 2. Validar sesion activa
                validarSesionActiva(usuario, tenantId);

                // 3. Validar rate limiting personalizado
                validarRateLimitPersonalizado(usuarioId, tenantId, operacion);

                // 4. Actualizar ultimo acceso
                usuario.setUltimoAcceso(LocalDateTime.now());
                save(usuario);

            } catch (SecurityException e) {
                throw e; // Re-lanzar excepciones de seguridad
            } catch (Exception e) {
                logger.error("Error validando contexto de sesion: {}", e.getMessage());
                throw new SecurityException("Error en validacion de sesion");
            }
        }

        /**
         * UTIL: Validar que usuario pertenece al tenant (version rapida)
         */
        public void validarUsuarioTenant(String usuarioId, String tenantId) {
            boolean perteneceAlTenant = findById(usuarioId)
                    .map(user -> user.getTenant().getId().equals(tenantId))
                    .orElse(false);

            if (!perteneceAlTenant) {
                throw new SecurityException("Usuario no pertenece al tenant especificado");
            }
        }

        /**
         * UTIL: Validar permisos para modificar citas
         */
        public boolean puedeModificarCita(String usuarioId, String citaId, String tenantId) {
            String rolesPermitidos = tenantConfigService.obtenerValor(tenantId,
                    "roles_modificar_citas", "ADMIN,MANAGER");

            return findById(usuarioId)
                    .map(user -> user.getTenant().getId().equals(tenantId) &&
                            rolesPermitidos.contains(user.getRole()))
                    .orElse(false);
        }

        /**
         * UTIL: Obtener informacion basica del usuario para logs
         */
        public String obtenerInfoUsuario(String usuarioId) {
            try {
                return findById(usuarioId)
                        .map(user -> String.format("Usuario: %s (%s) - Tenant: %s",
                                user.getNombre(), user.getRole(), user.getTenant().getId()))
                        .orElse("Usuario no encontrado: " + usuarioId);
            } catch (Exception e) {
                return "Error obteniendo info usuario: " + usuarioId;
            }
        }

        // ========================================
        // METODOS PRIVADOS DE VALIDACION
        // ========================================

        private void validarSesionActiva(User usuario, String tenantId) {
            if (usuario.getUltimoAcceso() == null) {
                return; // Primera sesion o sin registro previo
            }

            String minutosExpiracionConfig = tenantConfigService.obtenerValor(tenantId,
                    "minutos_expiracion_sesion", "480"); // 8 horas por defecto

            try {
                int minutosExpiracion = Integer.parseInt(minutosExpiracionConfig);

                if (usuario.getUltimoAcceso().isBefore(
                        LocalDateTime.now().minusMinutes(minutosExpiracion))) {

                    logger.warn("Sesion expirada para usuario {} en tenant {}",
                            usuario.getId(), tenantId);
                    throw new SecurityException("Sesion expirada");
                }

            } catch (NumberFormatException e) {
                logger.warn("Configuracion de expiracion de sesion invalida para tenant {}: {}",
                        tenantId, minutosExpiracionConfig);
                // Si no se puede parsear, no validar expiracion
            }
        }

        private void validarRateLimitPersonalizado(String usuarioId, String tenantId, String operacion) {
            String limitConfig = tenantConfigService.obtenerValor(tenantId,
                    "rate_limit_" + operacion, "60");

            try {
                int limite = Integer.parseInt(limitConfig);

                // TODO: Implementar conteo real de operaciones cuando este disponible
                // Por ahora solo logear la configuracion
                logger.debug("Rate limit para operacion {}: {} por minuto (usuario: {}, tenant: {})",
                        operacion, limite, usuarioId, tenantId);

            } catch (NumberFormatException e) {
                logger.warn("Configuracion de rate limit invalida para {}@{}: {}",
                        operacion, tenantId, limitConfig);
            }
        }

        private void registrarIntentoAccesoIlegal(String usuarioId, String tenantId,
                                                  String operacion, HttpServletRequest request) {
            String ip = obtenerIPReal(request);
            String userAgent = request.getHeader("User-Agent");

            logger.error("ACCESO ILEGAL DETECTADO: usuario={}, tenant={}, operacion={}, ip={}, ua={}",
                    usuarioId, tenantId, operacion, ip, userAgent);

            // TODO: Integrar con sistema de alertas de seguridad
            // TODO: Considerar bloqueo temporal del usuario/IP
        }

        private String obtenerIPReal(HttpServletRequest request) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        }

        /**
         * UTIL: Validar fuerza de contraseña personalizada por tenant
         */
        public boolean validarFuerzaPassword(String password, String tenantId) {
            String longitudMinima = tenantConfigService.obtenerValor(tenantId,
                    "password_longitud_minima", "8");
            String requiereNumeros = tenantConfigService.obtenerValor(tenantId,
                    "password_requiere_numeros", "true");
            String requiereMayusculas = tenantConfigService.obtenerValor(tenantId,
                    "password_requiere_mayusculas", "true");

            try {
                int longitud = Integer.parseInt(longitudMinima);

                if (password.length() < longitud) {
                    return false;
                }

                if ("true".equals(requiereNumeros) && !password.matches(".*\\d.*")) {
                    return false;
                }

                if ("true".equals(requiereMayusculas) && !password.matches(".*[A-Z].*")) {
                    return false;
                }

                return true;

            } catch (NumberFormatException e) {
                logger.warn("Configuracion de longitud de password invalida: {}", longitudMinima);
                return password.length() >= 8; // Fallback basico
            }
        }

        /**
         * UTIL: Crear usuario con validaciones de tenant
         */
        @Override
        public User save(User user) {
            // Validaciones adicionales antes de guardar
            if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
                // Validar fuerza de contraseña si es nuevo o se cambia
                if (user.getTenant() != null &&
                        !validarFuerzaPassword(user.getPassword(), user.getTenant().getId())) {
                    throw new RuntimeException("La contraseña no cumple los requisitos de seguridad");
                }

                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            return super.save(user);
        }
    }
}