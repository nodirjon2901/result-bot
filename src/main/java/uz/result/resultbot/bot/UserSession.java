package uz.result.resultbot.bot;

import uz.result.resultbot.model.Application;
import uz.result.resultbot.model.Basket;
import uz.result.resultbot.model.CommercialOffer;

import java.util.concurrent.ConcurrentHashMap;

public class UserSession {

    private static final ConcurrentHashMap<Long, Basket> userBasket = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, Application> userApplication = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, CommercialOffer> userCommercialOffer = new ConcurrentHashMap<>();



    public static void updateUserApplication(Long chatId, Application application) {
        userApplication.put(chatId, application);
    }

    public static Application getApplication(Long chatId) {
        return userApplication.getOrDefault(chatId, new Application());
    }

    public static void removeApplication(Long chatId) {
        userApplication.remove(chatId);
    }

    public static void updateBasket(Long chatId, Basket basket){
        userBasket.put(chatId, basket);
    }

    public static Basket getBasket(Long chatId) {
        return userBasket.getOrDefault(chatId, new Basket());
    }

    public static void removeBasket(Long chatId) {
        userBasket.remove(chatId);
    }

    public static void updateCommercialOffer(Long chatId, CommercialOffer commercialOffer) {
        userCommercialOffer.put(chatId, commercialOffer);
    }

    public static CommercialOffer getCommercialOffer(Long chatId) {
        return userCommercialOffer.getOrDefault(chatId, new CommercialOffer());
    }

    public static void removeCommercialOffer(Long chatId) {
        userCommercialOffer.remove(chatId);
    }

}
