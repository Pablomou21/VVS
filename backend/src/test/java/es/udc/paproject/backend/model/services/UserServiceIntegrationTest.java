package es.udc.paproject.backend.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import es.udc.paproject.backend.model.entities.User;
import es.udc.paproject.backend.model.exceptions.DuplicateInstanceException;
import es.udc.paproject.backend.model.exceptions.IncorrectLoginException;
import es.udc.paproject.backend.model.exceptions.IncorrectPasswordException;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceIntegrationTest {

    private static final String NON_EXISTENT_ID = "-1";

    @Autowired
    private UserService userService;

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

        User loggedUser = userService.login("john", "password");

        assertEquals(user.getId(), loggedUser.getId());
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

    // -------------------------------------------------------------------------
    // Pruebas para loginFromId y updateProfile
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de la recuperación de sesión por ID de usuario persistido */
    @Test
    public void testLoginFromIdSuccess() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);

        User loggedUser = userService.loginFromId(user.getId());

        assertEquals(user.getId(), loggedUser.getId());
    }

    /* Caso de error / ID Inexistente: Comprobación de que loginFromId con ID inexistente lanza InstanceNotFoundException */
    @Test
    public void testLoginFromIdNonExistent() {
        assertThrows(InstanceNotFoundException.class, () ->
            userService.loginFromId(Long.parseLong(NON_EXISTENT_ID))
        );
    }

    /* Partición equivalente: Comprobación de la actualización correcta de los datos del perfil de usuario */
    @Test
    public void testUpdateProfileSuccess() throws Exception {
        User user = createTestUser("john");
        userService.signUp(user);

        userService.updateProfile(user.getId(), "NewName", "NewLastName", "newemail@test.com");

        User updatedUser = userService.loginFromId(user.getId());

        assertEquals("NewName", updatedUser.getFirstName());
    }

    /* Caso de error / ID Inexistente: Comprobación de que updateProfile en usuario inexistente lanza InstanceNotFoundException */
    @Test
    public void testUpdateProfileNonExistentUser() {
        assertThrows(InstanceNotFoundException.class, () ->
            userService.updateProfile(Long.parseLong(NON_EXISTENT_ID), "NewName", "NewLastName", "newemail@test.com")
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
}
