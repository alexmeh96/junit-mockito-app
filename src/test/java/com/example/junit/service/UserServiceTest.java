package com.example.junit.service;


import com.example.junit.dao.UserDao;
import com.example.junit.dto.User;
import com.example.junit.extension.UserServiceParamResolver;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

// создаётся тестовый класс при выполнении каждого теста(рекомендуется!)
//@TestInstance(TestInstance.Lifecycle.PER_METHOD)
// создаётся только один тестовый класс при для всех тестов
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("user")
// запускать тесты в рандомном порядке
//@TestMethodOrder(MethodOrderer.Random.class)

// предназначен для внедрения в тестовый класс собственного функционала (внедрение зависимосте, изменение жизненного цикла тестов и тд)
// в основном в место него используется штуки из spring test framework
@ExtendWith({
        UserServiceParamResolver.class,
        MockitoExtension.class
})
public class UserServiceTest {

    public static final User IVAN = User.of(1, "Ivan", "123");
    public static final User PETR = User.of(2, "Petr", "111");

    @Captor  //  следит за переданными аргументами в вызове метода мок объекта
    private ArgumentCaptor<Integer> argumentCaptor;

    //  создать mock обьект userDao  (наследуется от класса UserDao и переопределяет его методы (преподчтительно использовать!) )
    @Mock
    //  создать spy обьект userDao  ( создаёт прокси вокруг обьекта UserDao )
//    @Spy
    private UserDao userDao;

    @InjectMocks  //  заинджектить mock и spy обьекты в userService
    private UserService userService;

    @BeforeAll
    static void init() {
        System.out.println("Before all");
    }

//    @BeforeEach
//    void prepare(UserService userService) {
//        System.out.println("Before each: " + this);
////        userService = new UserService();
//        this.userService = userService;
//    }

    @BeforeEach
    void prepare() {
        System.out.println("Before each: " + this);
//        this.userDao = Mockito.mock(UserDao.class);  //  можно использовать вместо @Mock
//        this.userDao = Mockito.spy(new UserDao());  //  можно использовать вместо @Spy
//        this.userService = new UserService(userDao);   //  можно использовать вместо @InjectMocks
    }

    @Test
    void shouldDeleteExistedUser() {
        userService.add(IVAN);

        // верни true, когда мы вызовем у обьекта userDao метод delete и передадим ему IVAN.getId()
        // может применяться только для mock-объектов
        Mockito.doReturn(true).when(userDao).delete(IVAN.getId());
//        Mockito.doReturn(true).when(userDao).delete(Mockito.any());

        //для первого вызова метода delete у обьекта userDao вернётся true, а для последующих false
        // может применяться как у mock-обьектов, так и у spy-объектов
//        Mockito.when(userDao.delete(IVAN.getId()))
//                .thenReturn(true)
//                .thenReturn(false);

        var deleteResult = userService.delete(IVAN.getId());
        System.out.println(userService.delete(IVAN.getId()));
        System.out.println(userService.delete(IVAN.getId()));

        // проверка на то, что ровно три раза вызвался метод delete у объекта userDao
        Mockito.verify(userDao, Mockito.times(3)).delete(IVAN.getId());

        //проверка что метод delete у обьекта userDao был вызван с переданным ему Id равным 1
//        var argumentCaptor = ArgumentCaptor.forClass(Integer.class);  //  можно использовать вместо @Captor
        Mockito.verify(userDao, Mockito.times(3)).delete(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(1);

        assertThat(deleteResult).isTrue();
    }

    @Test
    @DisplayName("users will be empty if user added")
    void usersEmptyIfNoUserAdded() {
        System.out.println("Test 1: " + this);
        var users = userService.getAll();
//        assertFalse(users.isEmpty(), () -> "User list should be empty");
        assertTrue(users.isEmpty());
    }

    @Test
    void usersSizeIfUserAdded() {
        System.out.println("Test 2: " + this);
        userService.add(IVAN, PETR);

        var users = userService.getAll();

        assertThat(users).hasSize(2);
//        assertEquals(2, users.size());
    }

    @Test
    void usersConvertedToMapById() {
        userService.add(IVAN, PETR);

        Map<Integer, User> users = userService.getAllConvertedById();

        assertAll(
                () -> assertThat(users).containsKeys(IVAN.getId(), PETR.getId()),
                () -> assertThat(users).containsValues(IVAN, PETR)
        );
    }

    @AfterEach
    void deleteDataFromDatabase() {
        System.out.println("After each: " + this);
    }

    @AfterAll
    static void closeConnectionPool() {
        System.out.println("After all");
    }

    @Nested
    @DisplayName("test user login functionality")
    @Tag("login")
    @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)  // тайм-аут выполнения каждого теста
    class LoginTest {

        @Test
        @Disabled  // игнорировать тест
        void logicFailIfPasswordIsNotCorrect() {
            userService.add(IVAN);
            var maybeUser = userService.login(IVAN.getUsername(), "dummy");
            assertTrue(maybeUser.isEmpty());
        }

//        @Test
        @RepeatedTest(value = 5)  //повторять тест
        void logicFailIfUserDoesNotExist() {
            userService.add(IVAN);
            var maybeUser = userService.login("dummy", IVAN.getPassword());
            assertTrue(maybeUser.isEmpty());
        }

        @Test
        void checkLoginFunctionalityPerformance() {
            //тест не пройдёт по тайм-ауту большему 200мс
            var maybeUser = assertTimeout(Duration.ofMillis(200L), () -> {
//                Thread.sleep(300L);
                return userService.login("dummy", IVAN.getPassword());
            });

            assertTrue(maybeUser.isEmpty());
        }

        @Test
        void throwExceptionIfUsernameOrPasswordIsNull() {
            assertAll(
                    () -> {
                        var exception = assertThrows(IllegalArgumentException.class, () -> userService.login(null, "dummy"));
                        assertThat(exception.getMessage()).isEqualTo("username or password is null");
                    },
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login("dummy", null))
            );
        }

        @Test
        void loginSuccessIfUserExists() {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login(IVAN.getUsername(), IVAN.getPassword());

            assertThat(maybeUser).isPresent();
            maybeUser.ifPresent(user -> assertThat(user).isEqualTo(IVAN));

//        assertTrue(maybeUser.isPresent());
//        maybeUser.ifPresent(user -> assertEquals(IVAN, user));
        }

        @ParameterizedTest  //  аннотация для указания, что тест будет передавать параметры в тестовый метод
        @MethodSource("com.example.junit.service.UserServiceTest#getArgumentsForLoginTest")  //название метода, который возвращает параметры для тестов
//        @CsvFileSource(resources = "/login-test-data.csv", delimiter = ';', numLinesToSkip = 1)
        void loginParameterizedTest(String username, String password, Optional<User> user) {
            userService.add(IVAN, PETR);

            var maybeUser = userService.login(username, password);
            assertThat(maybeUser).isEqualTo(user);
        }
    }

    static Stream<Arguments> getArgumentsForLoginTest() {
        return Stream.of(
          Arguments.of("Ivan", "123", Optional.of(IVAN)),
          Arguments.of("Petr", "111", Optional.of(PETR)),
          Arguments.of("Petr", "dummy", Optional.empty()),
          Arguments.of("dummy", "123", Optional.empty())
        );
    }


}
