package uz.result.resultbot.bot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.result.resultbot.model.UserState;
import uz.result.resultbot.service.ApplicationService;
import uz.result.resultbot.service.BasketService;
import uz.result.resultbot.service.CommercialOfferService;
import uz.result.resultbot.service.UserService;

@Component
@RequiredArgsConstructor
public class ResultBot extends TelegramLongPollingBot {

    private final UserService userService;

    private final HandleService handleService;

    private final BasketService basketService;

    private final ApplicationService applicationService;

    private final CommercialOfferService commercialOfferService;


    @Value("${bot.token}")
    private String token;

    @Value("${bot.username}")
    private String username;

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Long chatId = update.getMessage().getChatId();
            Integer messageId = update.getMessage().getMessageId();
            UserState currentState = UserSession.getUserStateTemporary(chatId);

            if (currentState.equals(UserState.COMMERCIAL_FULL_NAME) || currentState.equals(UserState.COMMERCIAL_PHONE_NUMBER)) {
                UserSession.updateSpecialMessageId(chatId, messageId);
            }
            if (currentState.equals(UserState.APP_FULL_NAME) || currentState.equals(UserState.APP_PHONE_NUMBER)) {
                UserSession.updateAppMessageId(chatId, messageId);
            }
            if (!(currentState.equals(UserState.COMMERCIAL_FULL_NAME) || currentState.equals(UserState.COMMERCIAL_PHONE_NUMBER) ||
                    currentState.equals(UserState.APP_FULL_NAME) || currentState.equals(UserState.APP_PHONE_NUMBER))) {
                UserSession.updateUserWriteMessageId(chatId, update.getMessage().getMessageId());
            }

            if (update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                if (userService.existsByChatId(chatId)) {
                    if (text.equals("/start")) {
                        userService.updateUserState(chatId, UserState.START);
                        currentState = UserSession.updateUserState(chatId, UserState.START);
                        UserSession.removeApplication(chatId);
                    }
                }
                switch (currentState) {
                    case DEFAULT -> handleService.defaultMessageHandler(chatId, text, this);
                    case START -> handleService.startMessageHandler(chatId, this);
                    case SELECT_FUNCTION ->
                            handleService.menuMessageHandler(chatId, userService.getLanguage(chatId).get().name(), this);
                    case FUNCTION_SERVICE -> handleService.serviceSelectFunctionMessageHandler(chatId, this);
                    case SERVICE_SITE -> handleService.siteServiceMessageHandler(chatId, this);
                    case SERVICE_BOT -> handleService.botServiceMessageHandler(chatId, this);
                    case SERVICE_SMM -> handleService.smmServiceMessageHandler(chatId, this);
                    case SERVICE_ADVERTISING -> handleService.advertisingServiceMessageHandler(chatId, this);
                    case SERVICE_SEO -> handleService.seoServiceMessageHandler(chatId, this);
                    case SERVICE_BRANDING -> handleService.brandingServiceMessageHandler(chatId, this);
                    case APP_FULL_NAME -> handleService.appFullNameMessageHandler(chatId, text, this);
                    case APP_PHONE_NUMBER -> handleService.appPhoneNumMessageHandler(chatId, text, this);
                    case APP_SERVICE ->
                            handleService.appPhoneNumMessageHandler(chatId, UserSession.getApplication(chatId).getPhoneNumber(), this);
                    case APP_SELECTED_SERVICE -> handleService.selectAppServiceMessageHandler(chatId, this);
                    case APP_SEND_USER -> handleService.sendToUserApplicationMessageHandler(chatId, this);
                    case FUNCTION_BASKET -> handleService.basketMessageHandler(chatId, this);
                    case FUNCTION_BASKET_SELECTED_SERVICE ->
                            handleService.selectServiceListInBasketFunction(chatId, this);
                    case COMMERCIAL_FULL_NAME -> handleService.commercialFullNameMessageHandler(chatId, text, this);
                    case COMMERCIAL_PHONE_NUMBER -> handleService.commercialPhoneNumMessageHandler(chatId, text, this);
                    case COMMERCIAL_OLD_SERVICE ->
                            handleService.oldSelectedCommercialServiceMessageHandler(chatId, this, basketService.findByChatId(chatId));
                    case COMMERCIAL_SERVICE -> handleService.commercialCurrentServiceMessageHandler(chatId, this);
                    case COMMERCIAL_SELECTED_SERVICE -> handleService.selectBasketServiceMessageHandler(chatId, this);
                    case COMMERCIAL_SEND_USER -> handleService.sendToUserCommercialMessageHandler(chatId, this);
                }
            } else
                handleService.notSupportedMessageHandler(chatId, this);
        }
        if (update.hasCallbackQuery()) {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();
            UserState currentState = UserSession.getUserStateTemporary(chatId);

            switch (currentState) {
                case START -> {
                    handleService.menuMessageHandler(chatId, data, this);
                    basketService.deleteByChatId(chatId);
                }
                case SELECT_FUNCTION -> {
                    switch (data) {
                        case "services" -> handleService.serviceSelectFunctionMessageHandler(chatId, this);
                        case "application" -> handleService.applicationMessageHandler(chatId, this);
                        case "basket" -> handleService.basketMessageHandler(chatId, this);
                        case "back" -> handleService.backOperationMessageHandler(chatId, this);
                    }
                }
                case FUNCTION_SERVICE -> {
                    switch (data) {
                        case "site" -> handleService.siteServiceMessageHandler(chatId, this);
                        case "bot" -> handleService.botServiceMessageHandler(chatId, this);
                        case "smm" -> handleService.smmServiceMessageHandler(chatId, this);
                        case "advertising" -> handleService.advertisingServiceMessageHandler(chatId, this);
                        case "seo" -> handleService.seoServiceMessageHandler(chatId, this);
                        case "branding" -> handleService.brandingServiceMessageHandler(chatId, this);
                        case "back" -> handleService.backOperationMessageHandler(chatId, this);
                    }
                }
                case SERVICE_SITE, SERVICE_ADVERTISING, SERVICE_BOT, SERVICE_BRANDING, SERVICE_SEO, SERVICE_SMM -> {
                    switch (data) {
                        case "back" -> handleService.backOperationMessageHandler(chatId, this);
                        case "basket" -> handleService.basketOperationMessageHandler(chatId, this);
                    }
                }
                case APP_SERVICE -> {
                    if (data.equals("next"))
                        handleService.selectAppServiceMessageHandler(chatId, this);
                    else
                        applicationService.updateService(data, chatId);
                }
                case APP_SELECTED_SERVICE -> {
                    switch (data) {
                        case "send" -> handleService.sendToUserApplicationMessageHandler(chatId, this);
                        case "back" -> handleService.backOperationMessageHandler(chatId, this);
                    }
                }
                case APP_SEND_USER -> {
                    switch (data) {
                        case "send" -> handleService.sendToDatabaseApplicationMessageHandler(chatId, this);
                        case "back" -> handleService.backOperationMessageHandler(chatId, this);
                        case "edit" -> {
                            applicationService.clearServices(chatId);
                            handleService.applicationMessageHandler(chatId, this);
                        }
                    }
                }
                case FUNCTION_BASKET -> {
                    switch (data) {
                        case "selected_service_list" -> handleService.selectServiceListInBasketFunction(chatId, this);
                        case "back" -> handleService.backOperationMessageHandler(chatId, this);
                    }
                }
                case FUNCTION_BASKET_SELECTED_SERVICE -> {
                    if (handleService.checkData(data))
                        handleService.deleteBasketService(chatId, data, this);
                    switch (data) {
                        case "clear_basket" -> handleService.clearUserBasketMessageHandler(chatId, this);
                        case "back" -> handleService.backOperationMessageHandler(chatId, this);
                        case "commercial" -> handleService.commercialMessageHandler(chatId, this);
                    }
                }
                case COMMERCIAL_OLD_SERVICE -> {
                    switch (data) {
                        case "add" -> handleService.commercialCurrentServiceMessageHandler(chatId, this);
                        case "send" -> handleService.sendToUserCommercialMessageHandler(chatId, this);
                    }
                }
                case COMMERCIAL_SERVICE -> {
                    if (data.equals("next"))
                        handleService.selectBasketServiceMessageHandler(chatId, this);
                    else
                        basketService.updateService(data, chatId);
                }
                case COMMERCIAL_SELECTED_SERVICE -> {
                    switch (data) {
                        case "back" -> handleService.backOperationMessageHandler(chatId, this);
                        case "send" -> handleService.sendToUserCommercialMessageHandler(chatId, this);
                    }
                }
                case COMMERCIAL_SEND_USER -> {
                    switch (data) {
                        case "back" -> handleService.backOperationMessageHandler(chatId, this);
                        case "send" -> handleService.sendToDatabaseCommercialMessageHandler(chatId, this);
                        case "edit" -> {
                            commercialOfferService.clearServices(chatId);
                            UserSession.removeBasket(chatId);
                            handleService.selectServiceListInBasketFunction(chatId, this);
                        }
                    }

                }
            }
        }

    }


}
