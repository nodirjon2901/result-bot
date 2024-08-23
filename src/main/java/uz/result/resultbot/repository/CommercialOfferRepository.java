package uz.result.resultbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.result.resultbot.model.CommercialOffer;

import java.util.Optional;

@Repository
public interface CommercialOfferRepository extends JpaRepository<CommercialOffer, Long> {

    Optional<CommercialOffer> findByUserChatId(Long chatId);

    Optional<CommercialOffer> findByUserId(Long userId);

}
