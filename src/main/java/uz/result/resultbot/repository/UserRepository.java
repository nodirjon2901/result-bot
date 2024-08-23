package uz.result.resultbot.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.result.resultbot.model.User;
import uz.result.resultbot.model.UserState;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByChatId(Long chatId);

    @Query(value = "select user_state from users where chat_id=:chatId", nativeQuery = true)
    Optional<UserState> findUserStateByChatId(@Param("chatId") Long chatId);

    boolean existsByChatId(Long chatId);

    @Transactional
    @Modifying
    @Query(value = "update users set user_state=:state where chat_id=:chatId", nativeQuery = true)
    void updateState(@Param("chatId") Long chatId, @Param("state") String state);

    @Transactional
    @Modifying
    @Query(value = "update users set language=:language where chat_id=:chatId", nativeQuery = true)
    void updateLanguage(@Param("chatId") Long chatId, @Param("language") String language);
}
