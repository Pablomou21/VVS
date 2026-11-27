package es.udc.paproject.backend.model.services;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import es.udc.paproject.backend.model.entities.User;
import es.udc.paproject.backend.model.entities.UserDao;
import es.udc.paproject.backend.model.exceptions.DuplicateInstanceException;
import es.udc.paproject.backend.model.exceptions.IncorrectLoginException;
import es.udc.paproject.backend.model.exceptions.IncorrectPasswordException;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import es.udc.paproject.backend.testutil.PerformanceMeter;
import net.jqwik.api.Arbitraries;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    // Requisito no funcional (unidad): con el DAO y el cifrado simulados, la lógica del servicio debe ejecutarse de media
    // en menos de 1 ms por operación. Criterio: aquí BCrypt está simulado, así que lo que se mide es solo la lógica del
    // servicio; el coste real de BCrypt se mide aparte en UserServiceIntegrationTest
    private static final double SERVICE_LOGIC_THRESHOLD_MS = 1.0;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    private String randomText() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(60).sample();
    }

    private Long randomId() {
        return Arbitraries.longs().between(1, Long.MAX_VALUE).sample();
    }

    private User randomUser() {
        return new User(randomText(), randomText(), randomText(), randomText(), randomText());
    }

    // ========== signUp ==========

    // ---------- Partición válida: nombre de usuario libre ----------

    // Datos aleatorios: campos del usuario de 1 a 60 letras
    @Test
    public void signUpSavesGivenUser() throws DuplicateInstanceException {
        User user = randomUser();

        userService.signUp(user);

        verify(userDao).save(user);
    }

    // Datos aleatorios: contraseña de 1 a 60 letras
    @Test
    public void signUpStoresEncodedPassword() throws DuplicateInstanceException {
        User user = randomUser();
        when(passwordEncoder.encode(user.getPassword())).thenReturn("encoded");

        userService.signUp(user);

        assertEquals("encoded", user.getPassword());
    }

    // Partición: usuario que llega con rol CLERK, debe quedar como USER
    @Test
    public void signUpAssignsUserRole() throws DuplicateInstanceException {
        User user = randomUser();
        user.setRole(User.RoleType.CLERK);

        userService.signUp(user);

        assertEquals(User.RoleType.USER, user.getRole());
    }

    // ---------- Partición inválida: nombre de usuario ya existe ----------

    // Datos aleatorios: usuario con nombre ya registrado
    @Test
    public void signUpWithDuplicateUserNameThrows() {
        User user = randomUser();
        when(userDao.existsByUserName(user.getUserName())).thenReturn(true);

        assertThrows(DuplicateInstanceException.class, () -> userService.signUp(user));
    }

    // Datos aleatorios: usuario con nombre ya registrado
    @Test
    public void signUpWithDuplicateUserNameDoesNotSave() {
        User user = randomUser();
        when(userDao.existsByUserName(user.getUserName())).thenReturn(true);

        catchThrowable(() -> userService.signUp(user));

        verify(userDao, never()).save(any());
    }

    // Datos aleatorios: usuario con nombre ya registrado
    @Test
    public void duplicateExceptionKeyIsUserName() {
        User user = randomUser();
        when(userDao.existsByUserName(user.getUserName())).thenReturn(true);

        DuplicateInstanceException e = catchThrowableOfType(() -> userService.signUp(user),
                DuplicateInstanceException.class);

        assertEquals(user.getUserName(), e.getKey());
    }

    // ========== login ==========

    // ---------- Partición válida: usuario y contraseña correctos ----------

    // Datos aleatorios: nombre y contraseña de 1 a 60 letras
    @Test
    public void loginWithCorrectPasswordReturnsUser() throws IncorrectLoginException {
        User user = randomUser();
        String password = randomText();
        when(userDao.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

        User result = userService.login(user.getUserName(), password);

        assertSame(user, result);
    }

    // ---------- Partición inválida: usuario inexistente o contraseña incorrecta ----------

    // Dato aleatorio: nombre de usuario que no existe
    @Test
    public void loginWithNonExistentUserThrows() {
        String userName = randomText();
        when(userDao.findByUserName(userName)).thenReturn(Optional.empty());

        assertThrows(IncorrectLoginException.class, () -> userService.login(userName, randomText()));
    }

    // Datos aleatorios: usuario existente y contraseña que no coincide
    @Test
    public void loginWithIncorrectPasswordThrows() {
        User user = randomUser();
        String password = randomText();
        when(userDao.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(false);

        assertThrows(IncorrectLoginException.class, () -> userService.login(user.getUserName(), password));
    }

    // Dato aleatorio: nombre de usuario que no existe
    @Test
    public void incorrectLoginExceptionKeepsUserName() {
        String userName = randomText();
        when(userDao.findByUserName(userName)).thenReturn(Optional.empty());

        IncorrectLoginException e = catchThrowableOfType(() -> userService.login(userName, randomText()),
                IncorrectLoginException.class);

        assertEquals(userName, e.getUserName());
    }

    // ========== loginFromId ==========

    // Partición válida. Dato aleatorio: id entre 1 y Long.MAX_VALUE
    @Test
    public void loginFromIdReturnsCheckedUser() throws InstanceNotFoundException {
        Long id = randomId();
        User user = randomUser();
        when(permissionChecker.checkUser(id)).thenReturn(user);

        User result = userService.loginFromId(id);

        assertSame(user, result);
    }

    // Partición inválida. Dato aleatorio: id sin usuario
    @Test
    public void loginFromIdWithNonExistentUserThrows() throws InstanceNotFoundException {
        Long id = randomId();
        when(permissionChecker.checkUser(id)).thenThrow(new InstanceNotFoundException("project.entities.user", id));

        assertThrows(InstanceNotFoundException.class, () -> userService.loginFromId(id));
    }

    // ========== updateProfile ==========

    // ---------- Partición válida: usuario existente ----------

    // Datos aleatorios: nombre nuevo de 1 a 60 letras
    @Test
    public void updateProfileChangesFirstName() throws InstanceNotFoundException {
        Long id = randomId();
        User user = randomUser();
        String firstName = randomText();
        when(permissionChecker.checkUser(id)).thenReturn(user);

        User result = userService.updateProfile(id, firstName, randomText(), randomText());

        assertEquals(firstName, result.getFirstName());
    }

    // Datos aleatorios: apellidos nuevos de 1 a 60 letras
    @Test
    public void updateProfileChangesLastName() throws InstanceNotFoundException {
        Long id = randomId();
        User user = randomUser();
        String lastName = randomText();
        when(permissionChecker.checkUser(id)).thenReturn(user);

        User result = userService.updateProfile(id, randomText(), lastName, randomText());

        assertEquals(lastName, result.getLastName());
    }

    // Datos aleatorios: email nuevo de 1 a 60 letras
    @Test
    public void updateProfileChangesEmail() throws InstanceNotFoundException {
        Long id = randomId();
        User user = randomUser();
        String email = randomText();
        when(permissionChecker.checkUser(id)).thenReturn(user);

        User result = userService.updateProfile(id, randomText(), randomText(), email);

        assertEquals(email, result.getEmail());
    }

    // ---------- Partición inválida: usuario inexistente ----------

    // Dato aleatorio: id sin usuario
    @Test
    public void updateProfileWithNonExistentUserThrows() throws InstanceNotFoundException {
        Long id = randomId();
        when(permissionChecker.checkUser(id)).thenThrow(new InstanceNotFoundException("project.entities.user", id));

        assertThrows(InstanceNotFoundException.class,
                () -> userService.updateProfile(id, randomText(), randomText(), randomText()));
    }

    // ========== changePassword ==========

    // ---------- Partición válida: contraseña antigua correcta ----------

    // Datos aleatorios: contraseñas de 1 a 60 letras
    @Test
    public void changePasswordStoresEncodedNewPassword()
            throws InstanceNotFoundException, IncorrectPasswordException {
        Long id = randomId();
        User user = randomUser();
        String oldPassword = randomText();
        String newPassword = randomText();
        when(permissionChecker.checkUser(id)).thenReturn(user);
        when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded");

        userService.changePassword(id, oldPassword, newPassword);

        assertEquals("encoded", user.getPassword());
    }

    // ---------- Partición inválida: contraseña antigua incorrecta o usuario inexistente ----------

    // Datos aleatorios: contraseña antigua que no coincide
    @Test
    public void changePasswordWithIncorrectOldPasswordThrows() throws InstanceNotFoundException {
        Long id = randomId();
        User user = randomUser();
        String oldPassword = randomText();
        when(permissionChecker.checkUser(id)).thenReturn(user);
        when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(false);

        assertThrows(IncorrectPasswordException.class,
                () -> userService.changePassword(id, oldPassword, randomText()));
    }

    // Datos aleatorios: contraseña antigua que no coincide
    @Test
    public void changePasswordWithIncorrectOldPasswordKeepsPassword() throws InstanceNotFoundException {
        Long id = randomId();
        User user = randomUser();
        String storedPassword = user.getPassword();
        String oldPassword = randomText();
        when(permissionChecker.checkUser(id)).thenReturn(user);
        when(passwordEncoder.matches(oldPassword, storedPassword)).thenReturn(false);

        catchThrowable(() -> userService.changePassword(id, oldPassword, randomText()));

        assertEquals(storedPassword, user.getPassword());
    }

    // Dato aleatorio: id sin usuario
    @Test
    public void changePasswordWithNonExistentUserThrows() throws InstanceNotFoundException {
        Long id = randomId();
        when(permissionChecker.checkUser(id)).thenThrow(new InstanceNotFoundException("project.entities.user", id));

        assertThrows(InstanceNotFoundException.class,
                () -> userService.changePassword(id, randomText(), randomText()));
    }

    // ========== Rendimiento (JETM) con los colaboradores simulados ==========

    // Rendimiento: tiempo medio de login con credenciales correctas, con el DAO y el cifrado simulados
    @Test
    public void loginAverageTimeIsBelowThreshold() throws Exception {
        User user = randomUser();
        when(userDao.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", user.getPassword())).thenReturn(true);

        PerformanceMeter.Result result = PerformanceMeter.measure("UserServiceImpl:login",
                () -> userService.login(user.getUserName(), "password"));

        assertTrue(result.averageMillis() < SERVICE_LOGIC_THRESHOLD_MS);
    }

}
