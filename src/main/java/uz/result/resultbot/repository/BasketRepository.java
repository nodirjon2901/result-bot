package uz.result.resultbot.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.result.resultbot.model.Basket;

import java.util.Optional;

@Repository
public interface BasketRepository extends JpaRepository<Basket, Long> {

    Optional<Basket> findByUserId(Long userId);

    Optional<Basket> findByUserChatId(Long chatId);

    @Transactional
    @Modifying
    @Query(value = "delete from basket where id=:id",nativeQuery = true)
    void deleteByIdCustom(@Param("id") Long id);
}
