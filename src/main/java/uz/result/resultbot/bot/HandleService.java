package uz.result.resultbot.bot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.result.resultbot.model.*;
import uz.result.resultbot.service.ApplicationService;
import uz.result.resultbot.service.BasketService;
import uz.result.resultbot.service.CommercialOfferService;
import uz.result.resultbot.service.UserService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HandleService {

    private final UserService userService;

    private final BasketService basketService;

    private final MarkupService markupService;

    private final ApplicationService applicationService;

    private final CommercialOfferService commercialService;

    @Value("${group.chatId}")
    private String GROUP_CHAT_ID;

    @SneakyThrows
    public void defaultMessageHandler(Long chatId, String text, TelegramLongPollingBot bot) {
        if (text.equals("/start")) {
            User user = User.builder()
                    .chatId(chatId)
                    .language(Language.RUS)
                    .userState(UserState.START)
                    .build();
            userService.save(user);
            startMessageHandler(chatId, bot);
            return;
        }
        SendMessage sendMessage = new SendMessage(
                chatId.toString(),
                """
                        Boshlash uchun /start ni bosing\s
                        \s
                        Нажмите /start, чтобы начать
                        """);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void startMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage(
                chatId.toString(),
                """
                        Assalomu alaykum. Tilni tanlang\s
                        \s
                        Привет. Выберите язык"""
        );
        sendMessage.setReplyMarkup(markupService.selectLanguageInlineMarkup());
        bot.execute(sendMessage);
    }


    @SneakyThrows
    public void notSupportedMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        String text = "";
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            text = "Iltimos savolingizni faqat yozma ravishda yo'llang!";
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            text = "Пожалуйста, задавайте свой вопрос только в письменном виде!";
        SendMessage sendMessage = new SendMessage(chatId.toString(), text);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void menuMessageHandler(Long chatId, String data, TelegramLongPollingBot bot) {
        userService.changeLanguage(chatId, data);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Marhamat quyidagi funksiyalardan birini tanlang");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Пожалуйста, выберите одну из функций ниже");
        sendMessage.setReplyMarkup(markupService.functionInlineMarkup(chatId));
        userService.updateUserState(chatId, UserState.SELECT_FUNCTION);
        bot.execute(sendMessage);
    }


    @SneakyThrows
    public void backOperationMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        UserState userState = userService.getUserState(chatId);
        if (userState.equals(UserState.SELECT_FUNCTION)) {
            userService.updateUserState(chatId, UserState.START);
            startMessageHandler(chatId, bot);
        } else if (userState.equals(UserState.FUNCTION_SERVICE)) {
            userService.updateUserState(chatId, UserState.SELECT_FUNCTION);
            String data = userService.getLanguage(chatId).get().name();
            menuMessageHandler(chatId, data, bot);
        } else if (userState.equals(UserState.SERVICE_ADVERTISING) || userState.equals(UserState.SERVICE_BOT) || userState.equals(UserState.SERVICE_BRANDING) ||
                userState.equals(UserState.SERVICE_SEO) || userState.equals(UserState.SERVICE_SMM) || userState.equals(UserState.SERVICE_SITE)) {
            userService.updateUserState(chatId, UserState.FUNCTION_SERVICE);
            serviceSelectFunctionMessageHandler(chatId, bot);
        } else if (userState.equals(UserState.APP_SELECTED_SERVICE)) {
            userService.updateUserState(chatId, UserState.APP_SERVICE);
            applicationService.clearServices(chatId);
            String phoneNumber = UserSession.getApplication(chatId).getPhoneNumber();
            appPhoneNumMessageHandler(chatId, phoneNumber, bot);
        } else if (userState.equals(UserState.APP_SEND_USER)) {
            userService.updateUserState(chatId, UserState.APP_SELECTED_SERVICE);
            selectAppServiceMessageHandler(chatId, bot);
        } else if (userState.equals(UserState.FUNCTION_BASKET)) {
            userService.updateUserState(chatId, UserState.SELECT_FUNCTION);
            menuMessageHandler(chatId, userService.getLanguage(chatId).get().name(), bot);
        } else if (userState.equals(UserState.FUNCTION_BASKET_SELECTED_SERVICE)) {
            userService.updateUserState(chatId, UserState.FUNCTION_BASKET);
            basketMessageHandler(chatId, bot);
        } else if (userState.equals(UserState.COMMERCIAL_SELECTED_SERVICE)) {
            userService.updateUserState(chatId, UserState.COMMERCIAL_SERVICE);
            commercialCurrentServiceMessageHandler(chatId, bot);
        } else if (userState.equals(UserState.COMMERCIAL_SEND_USER)) {
            userService.updateUserState(chatId, UserState.COMMERCIAL_SELECTED_SERVICE);
            selectBasketServiceMessageHandler(chatId, bot);
        }
    }

    @SneakyThrows
    public void serviceSelectFunctionMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Bizning xizmatlar");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Наши услуги");
        sendMessage.setReplyMarkup(markupService.serviceInlineMarkup(chatId));
        userService.updateUserState(chatId, UserState.FUNCTION_SERVICE);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void siteServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Foydalanishda qulay va professional ko‘rinishga ega saytlarni ishlab chiqish, bu biznesingizni internetda ajralib turishiga yordam beradi.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Разработка сайтов, которые просты в использовании и выглядят профессионально, помогая бизнесу выделяться в интернете.*");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        userService.updateUserState(chatId, UserState.SERVICE_SITE);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void botServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Mijozlar bilan muloqotni osonlashtiradigan va oddiy jarayonlarni avtomatlashtiradigan Telegram-botlar yaratish.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Создание Telegram-ботов, которые облегчают общение с клиентами и автоматизируют рутинные процессы.*");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        userService.updateUserState(chatId, UserState.SERVICE_BOT);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void smmServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Ijtimoiy tarmoqlarni yaratish va boshqarishda yordam berish, auditoriyani kengaytirish va brend imidjini yaxshilash.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Помощь в создании и управлении социальными сетями, чтобы увеличить аудиторию и улучшить имидж бренда.*");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        userService.updateUserState(chatId, UserState.SERVICE_SMM);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void advertisingServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Kerakli mijozlarni jalb qiladigan va biznesingizga ko‘proq foyda keltiradigan reklamani sozlash.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Настройка рекламы, которая привлекает нужных клиентов и приносит бизнесу больше прибыли.*");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        userService.updateUserState(chatId, UserState.SERVICE_ADVERTISING);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void seoServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Saytingizni qidiruv tizimlarida yuqoriroq bo‘lishi va ko‘proq tashrif buyuruvchilarni jalb qilishi uchun optimallashtirish.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Оптимизация сайта, чтобы он был выше в поисковых системах и привлекал больше посетителей.*");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        userService.updateUserState(chatId, UserState.SERVICE_SEO);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void brandingServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Biznesingiz uchun o‘ziga xos uslub va obraz yaratish, bu esa mijozlar uchun oson tanib olinadigan va jozibali bo‘lishi uchun.*");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Создание уникального стиля и образа для бизнеса, который будет легко узнаваем и привлекателен для клиентов.*");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.serviceButtonInlineKeyboardMarkup(chatId));
        userService.updateUserState(chatId, UserState.SERVICE_BRANDING);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void applicationMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Ism sharifingiz kiriting (F.I.O): ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Введите свое полное имя (Ф.И.O): ");
        userService.updateUserState(chatId, UserState.APP_FULL_NAME);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void appFullNameMessageHandler(Long chatId, String fullName, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Telefon raqam kiriting: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Введите номер телефона: ");
        applicationService.updateFullName(fullName, chatId);
        userService.updateUserState(chatId, UserState.APP_PHONE_NUMBER);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void appPhoneNumMessageHandler(Long chatId, String phoneNum, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Qiziqtirgan xizmatlarni tanlang: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Выберите интересующие вас услуги: ");
        sendMessage.setReplyMarkup(markupService.interestedServiceInlineMarkup(chatId));
        applicationService.updatePhoneNum(phoneNum, chatId);
        userService.updateUserState(chatId, UserState.APP_SERVICE);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void selectAppServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Application application = UserSession.getApplication(chatId);
        if (application.getService() == null || application.getService().isEmpty()) {
            shouldSelectServiceMessageHandler(chatId, bot);
            return;
        }
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Siz tanlagan xizmatlar:*\n\n" + "_" + formattedSelectAppServices(application.getService()) + "_\n");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Услуги, которые вы выбираете:*\n\n" + "_" + formattedSelectAppServices(application.getService()) + "_\n");
        sendMessage.setReplyMarkup(markupService.appSelectedServiceButtonInlineMarkup(chatId));
        sendMessage.setParseMode("Markdown");
        userService.updateUserState(chatId, UserState.APP_SELECTED_SERVICE);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    private void shouldSelectServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Qiziqtirgan xizmatlarni tanlang: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Выберите интересующие вас услуги: ");
        sendMessage.setReplyMarkup(markupService.interestedServiceInlineMarkup(chatId));
        userService.updateUserState(chatId, UserState.APP_SERVICE);
        bot.execute(sendMessage);
    }

    private String formattedSelectAppServices(Set<String> services) {
        StringBuilder text = new StringBuilder();
        for (String service : services) {
            text.append(service).append("\n");
        }
        return text.toString();
    }

    private String formattedAppServices(Set<String> services) {
        StringBuilder text = new StringBuilder();
        for (String service : services) {
            text.append(service).append(", ");
        }
        if (!text.isEmpty()) {
            text.setLength(text.length() - 2);
        }
        return text.toString();
    }


    @SneakyThrows
    public void sendToUserApplicationMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Application application = UserSession.getApplication(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Sizning arizangiz:* \n\n" + formattedApplication(chatId, application) + "\n");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Ваше приложение:* \n\n" + formattedApplication(chatId, application) + "\n");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.sendToUserAppButtonsInlineMarkup(chatId));
        userService.updateUserState(chatId, UserState.APP_SEND_USER);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    private String formattedApplication(Long chatId, Application application) {
        String text = "";
        if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            text = "👤 Ф.И.О: " + application.getFullName() + "\n" +
                    "📞 Номер телефона: " + application.getPhoneNumber() + "\n" +
                    "📈 Интересующая услуга: " + formattedAppServices(application.getService()) + "\n";
        } else if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            text = "👤 F.I.O: " + application.getFullName() + "\n" +
                    "📞 Telefon nomer: " + application.getPhoneNumber() + "\n" +
                    "📈 Qiziqtirgan xizmatlar: " + formattedAppServices(application.getService()) + "\n";
        }
        return text;
    }

    @SneakyThrows
    public void sendToDatabaseApplicationMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Application application = applicationService.setUserInApplication(chatId, userService.findByChatId(chatId));
        Application save = applicationService.save(application);
        sendApplicationToCompanyGroup(bot, save);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Arizangiz qabul qilindi* ✅");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Ваша заявка принята* ✅");
        sendMessage.setParseMode("Markdown");
        bot.execute(sendMessage);
        menuMessageHandler(chatId, userService.getLanguage(chatId).get().name(), bot);
        UserSession.removeApplication(chatId);
    }

    private void sendApplicationToCompanyGroup(TelegramLongPollingBot bot, Application application) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(GROUP_CHAT_ID);
        sendMessage.setText(
                "*Новое заявка \uD83D\uDCCB*\n\n" +
                        "*👤 Ф.И.О:* " + application.getFullName() + "\n" +
                        "*📞 Номер телефона:* " + application.getPhoneNumber() + "\n" +
                        "*📈 Интересующая услуга: *" + formattedAppServices(application.getService()) + "\n\n"
        );
        sendMessage.setParseMode("Markdown");
        bot.execute(sendMessage);


        String jsonBody = createNotionJson(application);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("https://api.notion.com/v1/pages"))
                .header("Authorization", "Bearer " + "secret_lnLNl6lfhuBnnZmYOlg6tUGXUv8VjxmnDCffo0wwKJS")
                .header("Content-Type", "application/json")
                .header("Notion-Version", "2022-06-28")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();


        try {
            client.send(request1, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }


    }




    //

    public String createNotionJson(Application  application) {
        return "{"
                + "\"parent\": { \"database_id\": \"" + "2b43ccc3b4af4e53a8e6802fb3f7fcfb" + "\" },"
                + "\"properties\": {"
                + "\"Name\": {"
                + "\"title\": [{ \"text\": { \"content\": \"" + escapeJson(application.getFullName()) + "\" } }]"
                + "},"
                + "\"Type of Service\": {"
                + "\"rich_text\": [{ \"text\": { \"content\": \"" + escapeJson(application.getService().toString()) + "\" } }]"
                + "},"
                + "\"Phone\": {"
                + "\"phone_number\": \"" + application.getPhoneNumber() + "\""
                + "}"
                + "}"
                + "}";
    }


    public static String escapeJson(String value) {
        if (value == null) {
            return null;
        }
        // JSON maxsus belgilarini o'zlashtirish
        return value.replace("\"", "\\\"")
                .replace("\\", "\\\\")
                .replace("/", "\\/")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    //

    @SneakyThrows
    public void basketOperationMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        UserState userState = userService.getUserState(chatId);
        Basket basket = basketService.findByChatId(chatId);
        if (basket == null)
            basket = UserSession.getBasket(chatId);
        if (basket.getService() == null)
            basket.setService(new HashSet<>());
        String text = "";

        if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            switch (userState) {
                case SERVICE_SEO -> basket.getService().add("SEO");
                case SERVICE_SITE -> basket.getService().add("Saytlar");
                case SERVICE_SMM -> basket.getService().add("SMM");
                case SERVICE_BOT -> basket.getService().add("Telegram-botlar");
                case SERVICE_BRANDING -> basket.getService().add("Brending");
                case SERVICE_ADVERTISING -> basket.getService().add("Reklama boshlash");
            }
            text = "Tanlangan xizmat savatga qo'shildi✅";
        } else if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            switch (userState) {
                case SERVICE_SEO -> basket.getService().add("SEO");
                case SERVICE_SITE -> basket.getService().add("Сайты");
                case SERVICE_SMM -> basket.getService().add("SMM");
                case SERVICE_BOT -> basket.getService().add("Telegram-боты");
                case SERVICE_BRANDING -> basket.getService().add("Брендинг");
                case SERVICE_ADVERTISING -> basket.getService().add("Запуск рекламы");
            }
            text = "Выбранная услуга добавлена в корзину✅";
        }
        basket.setUser(userService.findByChatId(chatId));
        basketService.save(basket);
        UserSession.removeBasket(chatId);
        SendMessage sendMessage = new SendMessage(chatId.toString(), text);
        bot.execute(sendMessage);
        serviceSelectFunctionMessageHandler(chatId, bot);
    }

    @SneakyThrows
    public void basketMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Quyidagi funksiyalardan birini tanlang: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Выберите одну из функций ниже: ");
        sendMessage.setReplyMarkup(markupService.basketFunctionInlineMarkup(chatId));
        userService.updateUserState(chatId, UserState.FUNCTION_BASKET);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void selectServiceListInBasketFunction(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Basket basket = basketService.findByChatId(chatId);
        if (basket == null || (basket.getService() == null || basket.getService().isEmpty())) {
            if (userService.getLanguage(chatId).get().equals(Language.UZB))
                sendMessage.setText("*Tanlangan xizmatlar mavjud emas*‼️");
            else if (userService.getLanguage(chatId).get().equals(Language.RUS))
                sendMessage.setText("*Выбранные услуги недоступны*‼️");
            sendMessage.setParseMode("Markdown");
            bot.execute(sendMessage);
            basketMessageHandler(chatId, bot);
            return;
        }
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Siz tanlagan xizmatlar: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Услуги, которые вы выбираете: ");
        sendMessage.setReplyMarkup(markupService.selectServiceListInBasketFunctionInlineMarkup(chatId, basket));
        userService.updateUserState(chatId, UserState.FUNCTION_BASKET_SELECTED_SERVICE);
        bot.execute(sendMessage);
    }

    public boolean checkData(String data) {
        return data.startsWith("del_");
    }

    public void deleteBasketService(Long chatId, String data, TelegramLongPollingBot bot) {
        Basket basket = basketService.findByChatId(chatId);
        String service = data.substring(4);
        basket.getService().removeIf(basketService -> basketService.equals(service));
        basketService.save(basket);
        selectServiceListInBasketFunction(chatId, bot);
    }

    @SneakyThrows
    public void clearUserBasketMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        Basket basket = basketService.findByChatId(chatId);
        basket.getService().removeAll(basket.getService());
        basketService.save(basket);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Savat tozalandi*‼️");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Корзина очищена*‼️");
        sendMessage.setParseMode("Markdown");
        userService.updateUserState(chatId, UserState.FUNCTION_BASKET);
        bot.execute(sendMessage);
        basketMessageHandler(chatId, bot);
    }

    @SneakyThrows
    public void commercialMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Ism sharifingiz kiriting (F.I.O): ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Введите свое полное имя (Ф.И.O): ");
        userService.updateUserState(chatId, UserState.COMMERCIAL_FULL_NAME);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void commercialFullNameMessageHandler(Long chatId, String fullName, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Telefon raqam kiriting: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Введите номер телефона: ");
        commercialService.updateFullName(fullName, chatId);
        userService.updateUserState(chatId, UserState.COMMERCIAL_PHONE_NUMBER);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void commercialPhoneNumMessageHandler(Long chatId, String phoneNum, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Basket basket = basketService.findByChatId(chatId);
        if (basket != null && (basket.getService() != null && !basket.getService().isEmpty())) {
            oldSelectedCommercialServiceMessageHandler(chatId, bot, basket);
            commercialService.updatePhoneNum(phoneNum, chatId);
            return;
        }
        commercialService.updatePhoneNum(phoneNum, chatId);
        commercialCurrentServiceMessageHandler(chatId, bot);
    }

    @SneakyThrows
    public void commercialCurrentServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("Qiziqtirgan xizmatlarni tanlang: ");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("Выберите интересующие вас услуги: ");
        sendMessage.setReplyMarkup(markupService.interestedServiceInlineMarkup(chatId));
        userService.updateUserState(chatId, UserState.COMMERCIAL_SERVICE);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void oldSelectedCommercialServiceMessageHandler(Long chatId, TelegramLongPollingBot bot, Basket basket) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Siz tanlagan xizmatlar:* " + formatBasketService(basket.getService()));
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Услуги, которые вы выбираете:* " + formatBasketService(basket.getService()));
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.commercialOfferButtonsInlineKeyboardMarkup(chatId));
        userService.updateUserState(chatId, UserState.COMMERCIAL_OLD_SERVICE);
        bot.execute(sendMessage);
    }

    private String formatBasketService(Set<String> services) {
        StringBuilder text = new StringBuilder();
        for (String service : services)
            text.append(service).append(", ");
        if (!text.isEmpty())
            text.setLength(text.length() - 2);
        return text.toString();
    }

    @SneakyThrows
    public void selectBasketServiceMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Basket basket = UserSession.getBasket(chatId);
        if (basket.getService() == null) {
            basket.setService(new HashSet<>());
        }
        Basket dbBasket = basketService.findByChatId(chatId);
        basket.getService().addAll(dbBasket.getService());
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Siz tanlagan xizmatlar:*\n\n" + "_" + formatBasketService(basket.getService()) + "_\n");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Услуги, которые вы выбираете:*\n\n" + "_" + formatBasketService(basket.getService()) + "_\n");
        sendMessage.setReplyMarkup(markupService.appSelectedServiceButtonInlineMarkup(chatId));
        sendMessage.setParseMode("Markdown");
        userService.updateUserState(chatId, UserState.COMMERCIAL_SELECTED_SERVICE);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    public void sendToUserCommercialMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Basket dbBasket = basketService.findByChatId(chatId);
        Basket basket = UserSession.getBasket(chatId);
        if (basket != null) {
            if (basket.getService() != null && !basket.getService().isEmpty()) {
                dbBasket.getService().addAll(basket.getService());
                dbBasket = basketService.update(dbBasket);
            }
        }
        CommercialOffer commercialOffer = UserSession.getCommercialOffer(chatId);
        if (commercialOffer.getService() == null) {
            commercialOffer.setService(new HashSet<>());
        }
        commercialOffer.getService().addAll(dbBasket.getService());
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Tijorat taklifi:* \n\n" + formattedCommercial(chatId, commercialOffer) + "\n");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Коммерческое предложение:* \n\n" + formattedCommercial(chatId, commercialOffer) + "\n");
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(markupService.sendToUserAppButtonsInlineMarkup(chatId));
        userService.updateUserState(chatId, UserState.COMMERCIAL_SEND_USER);
        bot.execute(sendMessage);
    }

    @SneakyThrows
    private String formattedCommercial(Long chatId, CommercialOffer commercialOffer) {
        String text = "";
        if (userService.getLanguage(chatId).get().equals(Language.RUS)) {
            text = "👤 Ф.И.О: " + commercialOffer.getFullName() + "\n" +
                    "📞 Номер телефона: " + commercialOffer.getPhoneNumber() + "\n" +
                    "📈 Интересующая услуга: " + formattedAppServices(commercialOffer.getService()) + "\n";
        } else if (userService.getLanguage(chatId).get().equals(Language.UZB)) {
            text = "👤 F.I.O: " + commercialOffer.getFullName() + "\n" +
                    "📞 Telefon nomer: " + commercialOffer.getPhoneNumber() + "\n" +
                    "📈 Qiziqtirgan xizmatlar: " + formattedAppServices(commercialOffer.getService()) + "\n";
        }
        return text;
    }

    @SneakyThrows
    public void sendToDatabaseCommercialMessageHandler(Long chatId, TelegramLongPollingBot bot) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        CommercialOffer commercialOffer = commercialService.setUserInCommercial(chatId, userService.findByChatId(chatId));
        CommercialOffer save = commercialService.save(commercialOffer);
        sendCommercialOfferToCompanyGroupMessageHandler(bot,save);
        if (userService.getLanguage(chatId).get().equals(Language.UZB))
            sendMessage.setText("*Tijorat taklifi qabul qilindi* ✅");
        else if (userService.getLanguage(chatId).get().equals(Language.RUS))
            sendMessage.setText("*Коммерческое предложение принято* ✅");
        sendMessage.setParseMode("Markdown");
        bot.execute(sendMessage);
        menuMessageHandler(chatId, userService.getLanguage(chatId).get().name(), bot);
        UserSession.removeCommercialOffer(chatId);
        basketService.deleteByChatId(chatId);//Agar user tijorat taklifini yuborib bo'lgach savat avtofat tozalanib ketishi kerak bo'lsa
    }

    private void sendCommercialOfferToCompanyGroupMessageHandler(TelegramLongPollingBot bot, CommercialOffer commercialOffer) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(GROUP_CHAT_ID);
        sendMessage.setText(
                "*Новое коммерческое предложение \uD83D\uDCCB*\n\n" +
                        "*👤 Ф.И.О:* " + commercialOffer.getFullName() + "\n" +
                        "*📞 Номер телефона:* " + commercialOffer.getPhoneNumber() + "\n" +
                        "*📈 Интересующая услуга: *" + formattedAppServices(commercialOffer.getService()) + "\n\n"
        );
        sendMessage.setParseMode("Markdown");
        bot.execute(sendMessage);


    }



}
