package uz.result.resultbot.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.result.resultbot.bot.UserSession;
import uz.result.resultbot.model.Basket;
import uz.result.resultbot.repository.BasketRepository;

import java.util.HashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BasketService {

    private final BasketRepository basketRepository;

    public Basket save(Basket basket) {
        Optional<Basket> optionalBasket = basketRepository.findByUserId(basket.getUser().getId());
        if (optionalBasket.isPresent()) {
            basket.setId(optionalBasket.get().getId());
            return update(basket);
        }
        return basketRepository.save(basket);
    }

    public Basket update(Basket basket) {
        Basket oldBasket = basketRepository.findById(basket.getId())
                .orElseThrow(() -> new RuntimeException("Basket is not found by id: " + basket.getId()));
        oldBasket.setUser(basket.getUser());
        oldBasket.setService(basket.getService());
        return basketRepository.save(oldBasket);
    }

    public Basket findByChatId(Long chatId) {
        return basketRepository.findByUserChatId(chatId).orElse(null);
    }

    public void updateService(String data, Long chatId) {
        Basket basket = UserSession.getBasket(chatId);
        if (basket.getService() == null) {
            basket.setService(new HashSet<>());
        }
        basket.getService().add(data);
        UserSession.updateBasket(chatId, basket);
    }

    public void deleteByChatId(Long chatId) {
        Optional<Basket> optionalBasket = basketRepository.findByUserChatId(chatId);
        optionalBasket.ifPresent(basket -> basketRepository.deleteByIdCustom(basket.getId()));
    }
}
