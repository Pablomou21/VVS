package es.udc.paproject.backend.rest.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.udc.paproject.backend.model.entities.User;

public class UserControllerIntegrationTest extends ControllerIntegrationTestBase {

    private User user;
    private User clerk;

    @BeforeEach
    public void setUp() {
        user = createUser("viewer", User.RoleType.USER);
        clerk = createUser("clerk", User.RoleType.CLERK);
    }

    private static String signUpJson(String userName, String password, String firstName, String lastName, String email) {
        return """
            {"userName":"%s","password":"%s","firstName":"%s","lastName":"%s","email":"%s"}
            """.formatted(userName, password, firstName, lastName, email);
    }

    private static String validSignUpJson(String userName) {
        return signUpJson(userName, PASSWORD, "John", "Doe", "john@test.com");
    }

    private static String loginJson(String userName, String password) {
        return """
            {"userName":"%s","password":"%s"}
            """.formatted(userName, password);
    }

    private static String profileJson(String firstName, String lastName, String email) {
        return """
            {"firstName":"%s","lastName":"%s","email":"%s"}
            """.formatted(firstName, lastName, email);
    }

    private static String changePasswordJson(String oldPassword, String newPassword) {
        return """
            {"oldPassword":"%s","newPassword":"%s"}
            """.formatted(oldPassword, newPassword);
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(APPLICATION_JSON).content(body));
    }

    private ResultActions postJson(String url, String body, String authorization) throws Exception {
        return mockMvc.perform(post(url).contentType(APPLICATION_JSON).content(body).header(AUTHORIZATION, authorization));
    }

    private ResultActions putJson(String url, String body, String authorization) throws Exception {
        return mockMvc.perform(put(url).contentType(APPLICATION_JSON).content(body).header(AUTHORIZATION, authorization));
    }

    // -------------------------------------------------------------------------
    // Pruebas para POST /users/signUp
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que el registro correcto devuelve HTTP 201 Created */
    @Test
    public void testSignUpSuccessReturnsCreated() throws Exception {
        postJson("/users/signUp", validSignUpJson("john")).andExpect(status().isCreated());
    }

    /* Partición equivalente: Comprobación de que el registro devuelve un token de servicio */
    @Test
    public void testSignUpSuccessReturnsServiceToken() throws Exception {
        postJson("/users/signUp", validSignUpJson("john")).andExpect(jsonPath("$.serviceToken").isNotEmpty());
    }

    /* Partición equivalente: Comprobación de que el usuario registrado se guarda en la BD con rol USER */
    @Test
    public void testSignUpPersistsUserWithRoleUser() throws Exception {
        postJson("/users/signUp", validSignUpJson("john"));
        flushAndClear();

        assertEquals(User.RoleType.USER, userDao.findByUserName("john").get().getRole());
    }

    /* Partición equivalente / Error: Comprobación de que registrar un userName duplicado devuelve HTTP 400 Bad Request */
    @Test
    public void testSignUpDuplicateUserNameReturnsBadRequest() throws Exception {
        postJson("/users/signUp", validSignUpJson("viewer")).andExpect(status().isBadRequest());
    }

    /* Valor frontera: Comprobación de que se permite registrar un userName de 60 caracteres */
    @Test
    public void testSignUpUserNameOfMaximumLengthReturnsCreated() throws Exception {
        postJson("/users/signUp", validSignUpJson("a".repeat(60))).andExpect(status().isCreated());
    }

    /* Valor frontera: Comprobación de que un userName de 61 caracteres devuelve HTTP 400 Bad Request */
    @Test
    public void testSignUpUserNameTooLongReturnsBadRequest() throws Exception {
        postJson("/users/signUp", validSignUpJson("a".repeat(61))).andExpect(status().isBadRequest());
    }

    /* Valor frontera: Comprobación de que un userName vacío devuelve HTTP 400 Bad Request */
    @Test
    public void testSignUpEmptyUserNameReturnsBadRequest() throws Exception {
        postJson("/users/signUp", validSignUpJson("")).andExpect(status().isBadRequest());
    }

    /* Partición equivalente / Error: Comprobación de que un email con formato inválido devuelve HTTP 400 Bad Request */
    @Test
    public void testSignUpInvalidEmailReturnsBadRequest() throws Exception {
        postJson("/users/signUp", signUpJson("john", PASSWORD, "John", "Doe", "esto-no-es-un-email"))
            .andExpect(status().isBadRequest());
    }

    /* Partición equivalente / Error: Comprobación de que la respuesta informa del campo erróneo */
    @Test
    public void testSignUpInvalidEmailReportsFieldErrors() throws Exception {
        postJson("/users/signUp", signUpJson("john", PASSWORD, "John", "Doe", "esto-no-es-un-email"))
            .andExpect(jsonPath("$.fieldErrors[0].fieldName").value("email"));
    }

    /* Partición equivalente / Error: Comprobación de que la falta de un campo obligatorio devuelve HTTP 400 Bad Request */
    @Test
    public void testSignUpMissingFirstNameReturnsBadRequest() throws Exception {
        postJson("/users/signUp", """
            {"userName":"john","password":"password","lastName":"Doe","email":"john@test.com"}
            """).andExpect(status().isBadRequest());
    }

    /* Partición equivalente / Error: Comprobación de que un token inválido en ruta pública devuelve HTTP 401 Unauthorized */
    @Test
    public void testSignUpWithInvalidTokenReturnsUnauthorized() throws Exception {
        postJson("/users/signUp", validSignUpJson("john"), INVALID_TOKEN_HEADER).andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Pruebas para POST /users/login
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que un login correcto devuelve HTTP 200 OK */
    @Test
    public void testLoginSuccessReturnsOk() throws Exception {
        postJson("/users/login", loginJson("viewer", PASSWORD)).andExpect(status().isOk());
    }

    /* Partición equivalente: Comprobación de que el login devuelve un token de servicio */
    @Test
    public void testLoginSuccessReturnsServiceToken() throws Exception {
        postJson("/users/login", loginJson("viewer", PASSWORD)).andExpect(jsonPath("$.serviceToken").isNotEmpty());
    }

    /* Partición equivalente: Comprobación de que el login de un taquillero devuelve el rol CLERK */
    @Test
    public void testLoginClerkReturnsClerkRole() throws Exception {
        postJson("/users/login", loginJson("clerk", PASSWORD)).andExpect(jsonPath("$.user.role").value("CLERK"));
    }

    /* Partición equivalente / Error: Comprobación de que una contraseña incorrecta devuelve HTTP 404 Not Found */
    @Test
    public void testLoginWrongPasswordReturnsNotFound() throws Exception {
        postJson("/users/login", loginJson("viewer", "otraPassword")).andExpect(status().isNotFound());
    }

    /* Valor frontera / Error: Comprobación de que una contraseña errónea por un solo carácter devuelve HTTP 404 Not Found */
    @Test
    public void testLoginPasswordDifferingInLastCharacterReturnsNotFound() throws Exception {
        postJson("/users/login", loginJson("viewer", "passwore")).andExpect(status().isNotFound());
    }

    /* Partición equivalente / Error: Comprobación de que un usuario inexistente devuelve HTTP 404 Not Found */
    @Test
    public void testLoginNonExistentUserReturnsNotFound() throws Exception {
        postJson("/users/login", loginJson("nadie", PASSWORD)).andExpect(status().isNotFound());
    }

    /* Partición equivalente / Error: Comprobación de que omitir la contraseña devuelve HTTP 400 Bad Request */
    @Test
    public void testLoginMissingPasswordReturnsBadRequest() throws Exception {
        postJson("/users/login", """
            {"userName":"viewer"}
            """).andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Pruebas para POST /users/loginFromServiceToken
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que la reautenticación por token devuelve HTTP 200 OK */
    @Test
    public void testLoginFromServiceTokenSuccessReturnsOk() throws Exception {
        mockMvc.perform(post("/users/loginFromServiceToken").header(AUTHORIZATION, bearer(user)))
            .andExpect(status().isOk());
    }

    /* Partición equivalente: Comprobación de que la respuesta corresponde al usuario del token */
    @Test
    public void testLoginFromServiceTokenReturnsTokenOwner() throws Exception {
        mockMvc.perform(post("/users/loginFromServiceToken").header(AUTHORIZATION, bearer(user)))
            .andExpect(jsonPath("$.user.userName").value("viewer"));
    }

    /* Partición equivalente / Error: Comprobación de que un token inválido devuelve HTTP 401 Unauthorized */
    @Test
    public void testLoginFromServiceTokenInvalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/users/loginFromServiceToken").header(AUTHORIZATION, INVALID_TOKEN_HEADER))
            .andExpect(status().isUnauthorized());
    }

    /* Partición equivalente / Error: Comprobación de que un token de usuario eliminado devuelve HTTP 404 Not Found */
    @Test
    public void testLoginFromServiceTokenNonExistentUserReturnsNotFound() throws Exception {
        mockMvc.perform(post("/users/loginFromServiceToken").header(AUTHORIZATION, bearerFor(NON_EXISTENT_ID, "USER")))
            .andExpect(status().isNotFound());
    }

    /* Partición equivalente / Error: Comprobación de que realizar la petición sin token devuelve un error 4xx de cliente */
    @Test
    public void testLoginFromServiceTokenWithoutTokenReturnsClientError() throws Exception {
        mockMvc.perform(post("/users/loginFromServiceToken")).andExpect(status().is4xxClientError());
    }

    // -------------------------------------------------------------------------
    // Pruebas para PUT /users/{id}
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que un usuario puede actualizar su perfil con HTTP 200 OK */
    @Test
    public void testUpdateProfileSuccessReturnsOk() throws Exception {
        putJson("/users/" + user.getId(), profileJson("NewName", "NewLast", "new@test.com"), bearer(user))
            .andExpect(status().isOk());
    }

    /* Partición equivalente: Comprobación de que la respuesta contiene el nombre actualizado */
    @Test
    public void testUpdateProfileReturnsUpdatedFirstName() throws Exception {
        putJson("/users/" + user.getId(), profileJson("NewName", "NewLast", "new@test.com"), bearer(user))
            .andExpect(jsonPath("$.firstName").value("NewName"));
    }

    /* Partición equivalente: Comprobación de que el cambio de perfil se persiste en la BD */
    @Test
    public void testUpdateProfilePersistsEmail() throws Exception {
        putJson("/users/" + user.getId(), profileJson("NewName", "NewLast", "new@test.com"), bearer(user));
        flushAndClear();

        assertEquals("new@test.com", userDao.findById(user.getId()).get().getEmail());
    }

    /* Partición equivalente: Comprobación de que un taquillero (CLERK) puede actualizar su propio perfil */
    @Test
    public void testUpdateProfileClerkReturnsOk() throws Exception {
        putJson("/users/" + clerk.getId(), profileJson("NewName", "NewLast", "new@test.com"), bearer(clerk))
            .andExpect(status().isOk());
    }

    /* Partición equivalente / Error: Comprobación de que intentar actualizar el perfil de otro usuario devuelve HTTP 403 Forbidden */
    @Test
    public void testUpdateProfileOfAnotherUserReturnsForbidden() throws Exception {
        putJson("/users/" + clerk.getId(), profileJson("NewName", "NewLast", "new@test.com"), bearer(user))
            .andExpect(status().isForbidden());
    }

    /* Partición equivalente / Error: Comprobación de que actualizar perfil sin autenticación devuelve HTTP 403 Forbidden */
    @Test
    public void testUpdateProfileAnonymousReturnsForbidden() throws Exception {
        mockMvc.perform(put("/users/" + user.getId()).contentType(APPLICATION_JSON)
            .content(profileJson("NewName", "NewLast", "new@test.com"))).andExpect(status().isForbidden());
    }

    /* Partición equivalente / Error: Comprobación de que un token inválido devuelve HTTP 401 Unauthorized */
    @Test
    public void testUpdateProfileInvalidTokenReturnsUnauthorized() throws Exception {
        putJson("/users/" + user.getId(), profileJson("NewName", "NewLast", "new@test.com"), INVALID_TOKEN_HEADER)
            .andExpect(status().isUnauthorized());
    }

    /* Partición equivalente / Error: Comprobación de que omitir el prefijo Bearer devuelve HTTP 403 Forbidden */
    @Test
    public void testUpdateProfileHeaderWithoutBearerPrefixReturnsForbidden() throws Exception {
        putJson("/users/" + user.getId(), profileJson("NewName", "NewLast", "new@test.com"), "Basic abc123")
            .andExpect(status().isForbidden());
    }

    /* Partición equivalente / Error: Comprobación de que un email inválido devuelve HTTP 400 Bad Request */
    @Test
    public void testUpdateProfileInvalidEmailReturnsBadRequest() throws Exception {
        putJson("/users/" + user.getId(), profileJson("NewName", "NewLast", "esto-no-es-un-email"), bearer(user))
            .andExpect(status().isBadRequest());
    }

    /* Valor frontera: Comprobación de que un nombre de 61 caracteres devuelve HTTP 400 Bad Request */
    @Test
    public void testUpdateProfileFirstNameTooLongReturnsBadRequest() throws Exception {
        putJson("/users/" + user.getId(), profileJson("a".repeat(61), "NewLast", "new@test.com"), bearer(user))
            .andExpect(status().isBadRequest());
    }

    /* Partición equivalente / Error: Comprobación de que actualizar un usuario inexistente devuelve HTTP 404 Not Found */
    @Test
    public void testUpdateProfileNonExistentUserReturnsNotFound() throws Exception {
        putJson("/users/" + NON_EXISTENT_ID, profileJson("NewName", "NewLast", "new@test.com"),
            bearerFor(NON_EXISTENT_ID, "USER")).andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Pruebas para POST /users/{id}/changePassword
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que el cambio correcto de contraseña devuelve HTTP 204 No Content */
    @Test
    public void testChangePasswordSuccessReturnsNoContent() throws Exception {
        postJson("/users/" + user.getId() + "/changePassword", changePasswordJson(PASSWORD, "newPassword"), bearer(user))
            .andExpect(status().isNoContent());
    }

    /* Partición equivalente: Comprobación de que la nueva contraseña permite realizar login posteriormente */
    @Test
    public void testChangePasswordNewPasswordAllowsLogin() throws Exception {
        postJson("/users/" + user.getId() + "/changePassword", changePasswordJson(PASSWORD, "newPassword"), bearer(user));
        flushAndClear();

        postJson("/users/login", loginJson("viewer", "newPassword")).andExpect(status().isOk());
    }

    /* Partición equivalente: Comprobación de que la contraseña antigua deja de ser válida tras el cambio */
    @Test
    public void testChangePasswordOldPasswordStopsWorking() throws Exception {
        postJson("/users/" + user.getId() + "/changePassword", changePasswordJson(PASSWORD, "newPassword"), bearer(user));
        flushAndClear();

        postJson("/users/login", loginJson("viewer", PASSWORD)).andExpect(status().isNotFound());
    }

    /* Partición equivalente / Error: Comprobación de que una contraseña antigua incorrecta devuelve HTTP 404 Not Found */
    @Test
    public void testChangePasswordWrongOldPasswordReturnsNotFound() throws Exception {
        postJson("/users/" + user.getId() + "/changePassword", changePasswordJson("incorrecta", "newPassword"), bearer(user))
            .andExpect(status().isNotFound());
    }

    /* Partición equivalente / Error: Comprobación de que cambiar la contraseña de otro usuario devuelve HTTP 403 Forbidden */
    @Test
    public void testChangePasswordOfAnotherUserReturnsForbidden() throws Exception {
        postJson("/users/" + clerk.getId() + "/changePassword", changePasswordJson(PASSWORD, "newPassword"), bearer(user))
            .andExpect(status().isForbidden());
    }

    /* Partición equivalente / Error: Comprobación de que cambiar contraseña sin autenticación devuelve HTTP 403 Forbidden */
    @Test
    public void testChangePasswordAnonymousReturnsForbidden() throws Exception {
        postJson("/users/" + user.getId() + "/changePassword", changePasswordJson(PASSWORD, "newPassword"))
            .andExpect(status().isForbidden());
    }

    /* Valor frontera: Comprobación de que se permite una nueva contraseña de 60 caracteres */
    @Test
    public void testChangePasswordNewPasswordOfMaximumLengthReturnsNoContent() throws Exception {
        postJson("/users/" + user.getId() + "/changePassword", changePasswordJson(PASSWORD, "a".repeat(60)), bearer(user))
            .andExpect(status().isNoContent());
    }

    /* Valor frontera: Comprobación de que una nueva contraseña de 61 caracteres devuelve HTTP 400 Bad Request */
    @Test
    public void testChangePasswordNewPasswordTooLongReturnsBadRequest() throws Exception {
        postJson("/users/" + user.getId() + "/changePassword", changePasswordJson(PASSWORD, "a".repeat(61)), bearer(user))
            .andExpect(status().isBadRequest());
    }

    /* Valor frontera: Comprobación de que una nueva contraseña vacía devuelve HTTP 400 Bad Request */
    @Test
    public void testChangePasswordEmptyNewPasswordReturnsBadRequest() throws Exception {
        postJson("/users/" + user.getId() + "/changePassword", changePasswordJson(PASSWORD, ""), bearer(user))
            .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // Pruebas para rutas no declaradas
    // -------------------------------------------------------------------------

    /* Partición equivalente: Comprobación de que una ruta no permitida (GET /users/{id}) devuelve HTTP 403 Forbidden */
    @Test
    public void testGetUserByIdAuthenticatedReturnsForbidden() throws Exception {
        mockMvc.perform(get("/users/" + user.getId()).header(AUTHORIZATION, bearer(user)))
            .andExpect(status().isForbidden());
    }

    /* Partición equivalente: Comprobación de que un método no permitido (DELETE /users/{id}) devuelve HTTP 403 Forbidden */
    @Test
    public void testDeleteUserReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/users/" + user.getId()).header(AUTHORIZATION, bearer(user)))
            .andExpect(status().isForbidden());
    }
}