package es.udc.paproject.backend.model.entities;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class UserDaoTest {

    @Autowired
    private UserDao userDao;

    @Autowired
    private TestEntityManager entityManager;

    private User createUser(String userName) {
        User user = new User(userName, "password", "First", "Last", "user@example.com");
        user.setRole(User.RoleType.USER);
        return userDao.save(user);
    }

    // Partición válida: existsByUserName encuentra un nombre guardado
    @Test
    public void existsByUserNameReturnsTrueForSavedUser() {
        createUser("saved-user");
        entityManager.flush();
        entityManager.clear();

        assertTrue(userDao.existsByUserName("saved-user"));
    }

    // Partición inválida: existsByUserName no encuentra un nombre inexistente
    @Test
    public void existsByUserNameReturnsFalseForUnknownUser() {
        assertFalse(userDao.existsByUserName("unknown-user"));
    }

    // Partición válida: findByUserName devuelve el usuario guardado
    @Test
    public void findByUserNameReturnsSavedUser() {
        createUser("saved-user");
        entityManager.flush();
        entityManager.clear();

        assertEquals("saved-user", userDao.findByUserName("saved-user").orElseThrow().getUserName());
    }

    // Partición inválida: findByUserName devuelve vacío para un nombre inexistente
    @Test
    public void findByUserNameReturnsEmptyForUnknownUser() {
        assertTrue(userDao.findByUserName("unknown-user").isEmpty());
    }
}
