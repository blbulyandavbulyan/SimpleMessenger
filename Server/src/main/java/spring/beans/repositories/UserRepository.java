package spring.beans.repositories;

import entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);
    //<T> Collection<T> findAll(Class<T> dtoClass);
    <T> Collection<T> findAllBy(Class<T> dtoType);
    void deleteByName(String name);
    boolean existsByName(String name);
//    List<UserDto> getUsers();

}
