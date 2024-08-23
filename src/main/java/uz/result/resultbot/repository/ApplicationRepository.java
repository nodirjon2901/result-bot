package uz.result.resultbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.result.resultbot.model.Application;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByUserId(Long userId);

    Optional<Application> findByUserChatId(Long chatId);

}
