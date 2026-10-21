package es.udc.paproject.backend.model.entities;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UserTest {

    // Partición equivalente: constructor almacena el nombre de usuario
    @Property
    public void constructorStoresUserName(@ForAll @StringLength(min = 1, max = 60) String userName) {
        User user = new User(userName, "password", "first", "last", "user@example.com");

        assertEquals(userName, user.getUserName());
    }

    // Partición equivalente: constructor almacena la contraseña
    @Property
    public void constructorStoresPassword(@ForAll @StringLength(min = 1, max = 60) String password) {
        User user = new User("username", password, "first", "last", "user@example.com");

        assertEquals(password, user.getPassword());
    }

    // Partición equivalente: constructor almacena el nombre
    @Property
    public void constructorStoresFirstName(@ForAll @StringLength(min = 1, max = 60) String firstName) {
        User user = new User("username", "password", firstName, "last", "user@example.com");

        assertEquals(firstName, user.getFirstName());
    }

    // Partición equivalente: constructor almacena los apellidos
    @Property
    public void constructorStoresLastName(@ForAll @StringLength(min = 1, max = 60) String lastName) {
        User user = new User("username", "password", "first", lastName, "user@example.com");

        assertEquals(lastName, user.getLastName());
    }

    // Partición equivalente: constructor almacena el correo
    @Property
    public void constructorStoresEmail(@ForAll @StringLength(min = 1, max = 60) String email) {
        User user = new User("username", "password", "first", "last", email);

        assertEquals(email, user.getEmail());
    }

    // Comportamiento documentado: el constructor no asigna el rol
    @Example
    public void constructorLeavesRoleUnassigned() {
        User user = new User("username", "password", "first", "last", "user@example.com");

        assertNull(user.getRole());
    }

    // Partición equivalente: el setter sustituye el nombre de usuario
    @Property
    public void setterUpdatesUserName(@ForAll @StringLength(min = 1, max = 60) String userName) {
        User user = new User();
        user.setUserName(userName);

        assertEquals(userName, user.getUserName());
    }

    // Partición equivalente: el setter sustituye la contraseña
    @Property
    public void setterUpdatesPassword(@ForAll @StringLength(min = 1, max = 60) String password) {
        User user = new User();
        user.setPassword(password);

        assertEquals(password, user.getPassword());
    }

    // Partición equivalente: el setter sustituye el nombre
    @Property
    public void setterUpdatesFirstName(@ForAll @StringLength(min = 1, max = 60) String firstName) {
        User user = new User();
        user.setFirstName(firstName);

        assertEquals(firstName, user.getFirstName());
    }

    // Partición equivalente: el setter sustituye los apellidos
    @Property
    public void setterUpdatesLastName(@ForAll @StringLength(min = 1, max = 60) String lastName) {
        User user = new User();
        user.setLastName(lastName);

        assertEquals(lastName, user.getLastName());
    }

    // Partición equivalente: el setter sustituye el correo
    @Property
    public void setterUpdatesEmail(@ForAll @StringLength(min = 1, max = 60) String email) {
        User user = new User();
        user.setEmail(email);

        assertEquals(email, user.getEmail());
    }

    // Dato aleatorio: se puede asignar y recuperar el identificador
    @Property
    public void setterUpdatesId(@ForAll @LongRange(min = 1, max = 1000000) long id) {
        User user = new User();
        user.setId(id);

        assertEquals(id, user.getId());
    }

    // Partición válida: el setter permite asignar cualquiera de los roles definidos
    @Property
    public void setterUpdatesRole(@ForAll User.RoleType role) {
        User user = new User();
        user.setRole(role);

        assertEquals(role, user.getRole());
    }
}
