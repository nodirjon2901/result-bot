package uz.result.resultbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.result.resultbot.bot.UserSession;
import uz.result.resultbot.model.CommercialOffer;
import uz.result.resultbot.model.User;
import uz.result.resultbot.repository.CommercialOfferRepository;

import java.util.HashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommercialOfferService {

    private final CommercialOfferRepository commercialOfferRepository;

    public CommercialOffer save(CommercialOffer commercialOffer) {
        Optional<CommercialOffer> optionalCommercialOffer = commercialOfferRepository.findByUserId(commercialOffer.getUser().getId());
        if (optionalCommercialOffer.isPresent()) {
            commercialOffer.setId(optionalCommercialOffer.get().getId());
            return update(commercialOffer);
        }
        return commercialOfferRepository.save(commercialOffer);
    }

    public CommercialOffer update(CommercialOffer commercialOffer) {
        CommercialOffer oldCommercial = commercialOfferRepository.findById(commercialOffer.getId())
                .orElseThrow(() -> new RuntimeException("Commercial is not found by id: " + commercialOffer.getId()));
        oldCommercial.setService(commercialOffer.getService());
        oldCommercial.setFullName(commercialOffer.getFullName());
        oldCommercial.setPhoneNumber(commercialOffer.getPhoneNumber());
        oldCommercial.setUser(commercialOffer.getUser());
        return commercialOfferRepository.save(oldCommercial);
    }

    public void updateFullName(String fullName, Long chatId) {
        CommercialOffer commercialOffer = UserSession.getCommercialOffer(chatId);
        commercialOffer.setFullName(fullName);
        UserSession.updateCommercialOffer(chatId, commercialOffer);
    }

    public void updatePhoneNum(String phoneNum, Long chatId) {
        CommercialOffer commercialOffer = UserSession.getCommercialOffer(chatId);
        commercialOffer.setPhoneNumber(phoneNum);
        UserSession.updateCommercialOffer(chatId,commercialOffer);
    }

    public void clearServices(Long chatId) {
        CommercialOffer commercialOffer = UserSession.getCommercialOffer(chatId);
        commercialOffer.setService(new HashSet<>());
        UserSession.updateCommercialOffer(chatId,commercialOffer);
    }

    public CommercialOffer setUserInCommercial(Long chatId, User user) {
        CommercialOffer commercialOffer = UserSession.getCommercialOffer(chatId);
        commercialOffer.setUser(user);
        UserSession.updateCommercialOffer(chatId,commercialOffer);
        return UserSession.getCommercialOffer(chatId);
    }
}
