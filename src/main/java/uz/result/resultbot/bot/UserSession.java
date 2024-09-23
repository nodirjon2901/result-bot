package uz.result.resultbot.bot;

import uz.result.resultbot.model.Application;
import uz.result.resultbot.model.Basket;
import uz.result.resultbot.model.CommercialOffer;
import uz.result.resultbot.model.UserState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UserSession {

    private static final ConcurrentHashMap<Long, Basket> userBasket = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, Application> userApplication = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, CommercialOffer> userCommercialOffer = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, Integer> userMessageChatId = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, Integer> userWriteMessageChatId = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, List<Integer>> userSpecialMessageIds = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, List<Integer>> userAppMessageIds = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Long, UserState> userStates = new ConcurrentHashMap<>();

    public static UserState updateUserState(Long chatId, UserState userState) {
         userStates.put(chatId, userState);
         return userState;
    }

    public static UserState getUserStateTemporary(Long chatId) {
        return userStates.getOrDefault(chatId, UserState.DEFAULT);
    }

    public static void removeUserState(Long chatId) {
        userStates.remove(chatId);
    }

    public static void clearMessageChatId(Long chatId) {
        removeUserMessageId(chatId);
        removeAppMessageIdList(chatId);
        removeSpecialMessageIdList(chatId);
        removeUserWriteMessageId(chatId);
    }

    public static void updateUserWriteMessageId(Long chatId, Integer messageId) {
        userWriteMessageChatId.put(chatId, messageId);
    }

    public static Integer getUserWriteMessageId(Long chatId) {
        return userWriteMessageChatId.getOrDefault(chatId, null);
    }

    public static void removeUserWriteMessageId(Long chatId) {
        userWriteMessageChatId.remove(chatId);
    }

    public static void updateAppMessageId(Long chatId, Integer messageId) {
        List<Integer> integerList = userAppMessageIds.getOrDefault(chatId, new ArrayList<>());
        integerList.add(messageId);
        userAppMessageIds.put(chatId, integerList);
    }

    public static List<Integer> getAppMessageIds(Long chatId) {
        return userAppMessageIds.get(chatId);
    }

    public static void removeAppMessageIdList(Long chatId) {
        userAppMessageIds.remove(chatId);
    }

    public static void updateSpecialMessageId(Long chatId, Integer messageId) {
        List<Integer> integerList = userSpecialMessageIds.getOrDefault(chatId, new ArrayList<>());
        integerList.add(messageId);
        userSpecialMessageIds.put(chatId, integerList);
    }

    public static List<Integer> getSpecialMessageId(Long chatId) {
        return userSpecialMessageIds.get(chatId);
    }

    public static void removeSpecialMessageIdList(Long chatId) {
        userSpecialMessageIds.remove(chatId);
    }

    public static void updateUserMessageId(Long chatId, Integer messageId) {
        userMessageChatId.put(chatId, messageId);
    }

    public static Integer getUserMessageId(Long chatId) {
        return userMessageChatId.getOrDefault(chatId, null);
    }

    public static void removeUserMessageId(Long chatId) {
        userMessageChatId.remove(chatId);
    }

    public static void updateUserApplication(Long chatId, Application application) {
        userApplication.put(chatId, application);
    }

    public static Application getApplication(Long chatId) {
        return userApplication.getOrDefault(chatId, new Application());
    }

    public static void removeApplication(Long chatId) {
        userApplication.remove(chatId);
    }

    public static void updateBasket(Long chatId, Basket basket) {
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
