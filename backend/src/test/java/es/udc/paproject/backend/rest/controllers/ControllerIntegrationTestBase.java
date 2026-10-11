package es.udc.paproject.backend.rest.controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import es.udc.paproject.backend.model.entities.Movie;
import es.udc.paproject.backend.model.entities.MovieDao;
import es.udc.paproject.backend.model.entities.OrderDao;
import es.udc.paproject.backend.model.entities.Room;
import es.udc.paproject.backend.model.entities.RoomDao;
import es.udc.paproject.backend.model.entities.Session;
import es.udc.paproject.backend.model.entities.SessionDao;
import es.udc.paproject.backend.model.entities.User;
import es.udc.paproject.backend.model.entities.UserDao;
import es.udc.paproject.backend.rest.common.JwtGenerator;
import es.udc.paproject.backend.rest.common.JwtInfo;
import jakarta.persistence.EntityManager;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class ControllerIntegrationTestBase {

    protected static final String PASSWORD = "password";
    protected static final String CREDIT_CARD = "1234567812345678";
    protected static final Long NON_EXISTENT_ID = -1L;
    protected static final String INVALID_TOKEN_HEADER = "Bearer esto.no.es.un.token";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserDao userDao;

    @Autowired
    protected MovieDao movieDao;

    @Autowired
    protected RoomDao roomDao;

    @Autowired
    protected SessionDao sessionDao;

    @Autowired
    protected OrderDao orderDao;

    @Autowired
    protected JwtGenerator jwtGenerator;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected EntityManager entityManager;


    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    protected User createUser(String userName, User.RoleType role) {
        User user = new User(userName, passwordEncoder.encode(PASSWORD), "Name", "LastName", userName + "@test.com");
        user.setRole(role);
        return userDao.save(user);
    }

    protected String bearer(User user) {
        return bearerFor(user.getId(), user.getRole().toString());
    }

    protected String bearerFor(Long userId, String role) {
        return "Bearer " + jwtGenerator.generate(new JwtInfo(userId, role));
    }

    protected Movie createMovie(String title) {
        return movieDao.save(new Movie(title, "Summary of " + title, (short) 120));
    }

    protected Room createRoom(int capacity) {
        return roomDao.save(new Room("Room " + capacity, capacity));
    }

    protected Session createSession(Movie movie, Room room, LocalDateTime date) {
        return sessionDao.save(new Session(movie, room, date, new BigDecimal("8.50")));
    }
}