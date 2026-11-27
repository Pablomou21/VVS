package es.udc.paproject.backend.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import es.udc.paproject.backend.model.entities.User;
import es.udc.paproject.backend.model.exceptions.DuplicateInstanceException;
import es.udc.paproject.backend.model.exceptions.IncorrectLoginException;
import es.udc.paproject.backend.model.exceptions.IncorrectPasswordException;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceIntegrationTest {

    private static final Long NON_EXISTENT_ID = -1L;

    @Autowired
    private UserService userService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private User createTestUser(String userName) {
        return new User(userName, "password", "Name", "LastName", userName + "@test.com");
    }

    // -------------------------------------------------------------------------
    // Pruebas para signUp y login
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación del flujo completo de registro e inicio de sesión correcto */
    @Test
    public void testSignUpAndLoginSuccess() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);
        flushAndClear();

        User loggedUser = userService.login("john", "password");

        assertEquals(user.getId(), loggedUser.getId());
    }

    /* Partición equivalente: Comprobación de que un usuario registrado sin rol queda guardado en la BD con el rol USER */
    @Test
    public void testSignUpWithoutRoleStoresUserRole() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);
        flushAndClear();

        assertEquals(User.RoleType.USER, userService.loginFromId(user.getId()).getRole());
    }

    /* Partición equivalente / Caso de error: Comprobación de que un usuario que llega con rol CLERK queda guardado en la BD con el rol USER */
    @Test
    public void testSignUpWithClerkRoleStoresUserRole() throws Exception {
        User user = createTestUser("john");
        user.setRole(User.RoleType.CLERK);
        userService.signUp(user);
        flushAndClear();

        assertEquals(User.RoleType.USER, userService.loginFromId(user.getId()).getRole());
    }

    /* Partición equivalente: Comprobación de que la contraseña almacenada en la BD no es la contraseña en claro */
    @Test
    public void testSignUpDoesNotStorePlainPassword() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);
        flushAndClear();

        assertNotEquals("password", userService.loginFromId(user.getId()).getPassword());
    }

    /* Partición equivalente: Comprobación de que la contraseña almacenada en la BD es el cifrado BCrypt de la original */
    @Test
    public void testSignUpStoresEncryptedPassword() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);
        flushAndClear();

        assertTrue(passwordEncoder.matches("password", userService.loginFromId(user.getId()).getPassword()));
    }

    /* Caso de error / Duplicados: Comprobación de que no se permite registrar dos usuarios con el mismo userName */
    @Test
    public void testSignUpDuplicateUserName() throws Exception {
        User user1 = createTestUser("john");
        userService.signUp(user1);

        User user2 = createTestUser("john");

        assertThrows(DuplicateInstanceException.class, () ->
            userService.signUp(user2)
        );
    }

    /* Caso de error / Autenticación: Comprobación de que login con contraseña incorrecta lanza IncorrectLoginException */
    @Test
    public void testLoginIncorrectPassword() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);

        assertThrows(IncorrectLoginException.class, () ->
            userService.login("john", "wrongPassword")
        );
    }

    /* Caso de error / Autenticación: Comprobación de que login con usuario inexistente lanza IncorrectLoginException */
    @Test
    public void testLoginNonExistentUser() {
        assertThrows(IncorrectLoginException.class, () ->
            userService.login("nonexistent", "password")
        );
    }

    /* Valor frontera / Caso de error: Comprobación de que login con contraseña vacía lanza IncorrectLoginException */
    @Test
    public void testLoginEmptyPassword() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);

        assertThrows(IncorrectLoginException.class, () ->
            userService.login("john", "")
        );
    }

    // -------------------------------------------------------------------------
    // Pruebas para loginFromId y updateProfile
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de la recuperación de sesión por ID de usuario persistido */
    @Test
    public void testLoginFromIdSuccess() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);
        flushAndClear();

        User loggedUser = userService.loginFromId(user.getId());

        assertEquals(user.getId(), loggedUser.getId());
    }

    /* Caso de error / ID Inexistente: Comprobación de que loginFromId con ID inexistente lanza InstanceNotFoundException */
    @Test
    public void testLoginFromIdNonExistent() {
        assertThrows(InstanceNotFoundException.class, () ->
            userService.loginFromId(NON_EXISTENT_ID)
        );
    }

    /* Partición equivalente: Comprobación de la actualización correcta de los datos del perfil de usuario */
    @Test
    public void testUpdateProfileSuccess() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);

        userService.updateProfile(user.getId(), "NewName", "NewLastName", "newemail@test.com");
        flushAndClear();

        User updatedUser = userService.loginFromId(user.getId());

        assertEquals("NewName", updatedUser.getFirstName());
    }

    /* Caso de error / ID Inexistente: Comprobación de que updateProfile en usuario inexistente lanza InstanceNotFoundException */
    @Test
    public void testUpdateProfileNonExistentUser() {
        assertThrows(InstanceNotFoundException.class, () ->
            userService.updateProfile(NON_EXISTENT_ID, "NewName", "NewLastName", "newemail@test.com")
        );
    }

    // -------------------------------------------------------------------------
    // Pruebas para changePassword
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación del cambio correcto de contraseña y posterior login con la nueva clave */
    @Test
    public void testChangePasswordSuccess() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);

        userService.changePassword(user.getId(), "password", "newPassword");
        flushAndClear();

        User loggedUser = userService.login("john", "newPassword");

        assertNotNull(loggedUser);
    }

    /* Caso de error / Autenticación: Comprobación de que cambiar contraseña con la clave antigua incorrecta lanza IncorrectPasswordException */
    @Test
    public void testChangePasswordIncorrectOldPassword() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);

        assertThrows(IncorrectPasswordException.class, () ->
            userService.changePassword(user.getId(), "wrongOldPassword", "newPassword")
        );
    }

    /* Caso de error / ID Inexistente: Comprobación de que changePassword en usuario inexistente lanza InstanceNotFoundException */
    @Test
    public void testChangePasswordNonExistentUser() {
        assertThrows(InstanceNotFoundException.class, () ->
            userService.changePassword(NON_EXISTENT_ID, "password", "newPassword")
        );
    }

    /* Partición equivalente / Caso de error: Comprobación de que, tras cambiar la contraseña, la antigua ya no permite iniciar sesión */
    @Test
    public void testChangePasswordOldPasswordNoLongerValid() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);
        userService.changePassword(user.getId(), "password", "newPassword");
        flushAndClear();

        assertThrows(IncorrectLoginException.class, () ->
            userService.login("john", "password")
        );
    }

    /* Partición equivalente: Comprobación de que la nueva contraseña también se almacena cifrada en la BD */
    @Test
    public void testChangePasswordStoresEncryptedNewPassword() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);
        userService.changePassword(user.getId(), "password", "newPassword");
        flushAndClear();

        assertNotEquals("newPassword", userService.loginFromId(user.getId()).getPassword());
    }
}