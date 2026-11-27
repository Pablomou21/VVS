package es.udc.paproject.backend.model.services;

import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import es.udc.paproject.backend.model.entities.User;
import es.udc.paproject.backend.model.entities.UserDao;
import es.udc.paproject.backend.model.exceptions.InstanceNotFoundException;
import net.jqwik.api.Arbitraries;

@ExtendWith(MockitoExtension.class)
public class PermissionCheckerImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private PermissionCheckerImpl permissionChecker;

    private Long randomId() {
        return Arbitraries.longs().between(1, Long.MAX_VALUE).sample();
    }

    // ---------- Partición válida: el usuario existe ----------

    // Dato aleatorio: id entre 1 y Long.MAX_VALUE
    @Test
    public void checkUserReturnsUserFoundByDao() throws InstanceNotFoundException {
        Long id = randomId();
        User user = new User();
        when(userDao.findById(id)).thenReturn(Optional.of(user));

        User result = permissionChecker.checkUser(id);

        assertSame(user, result);
    }

    // Dato aleatorio: id entre 1 y Long.MAX_VALUE
    @Test
    public void checkUserSearchesDaoWithGivenId() throws InstanceNotFoundException {
        Long id = randomId();
        when(userDao.findById(id)).thenReturn(Optional.of(new User()));

        permissionChecker.checkUser(id);

        verify(userDao).findById(id);
    }

    // Valor frontera: id 1, el menor id válido
    @Test
    public void checkUserWithMinimumIdReturnsUser() throws InstanceNotFoundException {
        Long id = 1L;
        User user = new User();
        when(userDao.findById(id)).thenReturn(Optional.of(user));

        User result = permissionChecker.checkUser(id);

        assertSame(user, result);
    }

    // ---------- Partición inválida: el usuario no existe ----------

    // Dato aleatorio: id entre 1 y Long.MAX_VALUE sin usuario
    @Test
    public void checkUserThrowsWhenUserNotFound() {
        Long id = randomId();
        when(userDao.findById(id)).thenReturn(Optional.empty());

        assertThrows(InstanceNotFoundException.class, () -> permissionChecker.checkUser(id));
    }

    // Dato aleatorio: id entre 1 y Long.MAX_VALUE sin usuario
    @Test
    public void notFoundExceptionKeyIsUserId() {
        Long id = randomId();
        when(userDao.findById(id)).thenReturn(Optional.empty());

        InstanceNotFoundException e = catchThrowableOfType(() -> permissionChecker.checkUser(id),
                InstanceNotFoundException.class);

        assertEquals(id, e.getKey());
    }

    // Dato aleatorio: id entre 1 y Long.MAX_VALUE sin usuario
    @Test
    public void notFoundExceptionNameIsUserEntity() {
        Long id = randomId();
        when(userDao.findById(id)).thenReturn(Optional.empty());

        InstanceNotFoundException e = catchThrowableOfType(() -> permissionChecker.checkUser(id),
                InstanceNotFoundException.class);

        assertEquals("project.entities.user", e.getName());
    }

    // Valor especial: id null, el mock devuelve Optional.empty() por defecto
    @Test
    public void checkUserWithNullIdThrowsWhenUserNotFound() {
        assertThrows(InstanceNotFoundException.class, () -> permissionChecker.checkUser(null));
    }

}
